package com.thonha.backend.service;

import com.thonha.backend.common.ApiException;
import com.thonha.backend.common.ErrorCode;
import com.thonha.backend.dto.response.WalletResponse;
import com.thonha.backend.dto.response.WalletTransactionResponse;
import com.thonha.backend.entity.Wallet;
import com.thonha.backend.entity.WalletTransaction;
import com.thonha.backend.entity.WorkerProfile;
import com.thonha.backend.enums.WalletTransactionType;
import com.thonha.backend.repository.WalletRepository;
import com.thonha.backend.repository.WalletTransactionRepository;
import com.thonha.backend.repository.WorkerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;


@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final WorkerProfileRepository workerProfileRepository;

    @Value("${app.wallet.min-balance:0}")
    private BigDecimal minBalanceToReceiveJobs;

    public BigDecimal getMinBalanceToReceiveJobs() {
        return minBalanceToReceiveJobs;
    }


    @Transactional
    public Wallet ensureWallet(Long workerProfileId) {
        return walletRepository.findByWorkerIdForUpdate(workerProfileId)
                .orElseGet(() -> createWallet(workerProfileId));
    }

    private Wallet createWallet(Long workerProfileId) {
        WorkerProfile profile = workerProfileRepository.findByIdForUpdate(workerProfileId)
                .orElseThrow(() -> new ApiException(ErrorCode.WORKER_NOT_FOUND));
        return walletRepository.findByWorkerIdForUpdate(workerProfileId)
                .orElseGet(() -> walletRepository.save(Wallet.builder().worker(profile).build()));
    }


    @Transactional(readOnly = true)
    public WalletResponse getMyWallet(Long workerUserId) {
        WorkerProfile profile = workerProfileRepository.findByUserId(workerUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.WORKER_NOT_FOUND));
        return walletRepository.findByWorkerId(profile.getId())
                .map(this::toResponse)
                .orElseGet(() -> new WalletResponse(null, profile.getId(), profile.getUser().getFullName(),
                        profile.getUser().getPhoneNumber(), BigDecimal.ZERO, minBalanceToReceiveJobs,
                        BigDecimal.ZERO.compareTo(minBalanceToReceiveJobs) >= 0, null));
    }

    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> getMyTransactions(Long workerUserId, Pageable pageable) {
        WorkerProfile profile = workerProfileRepository.findByUserId(workerUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.WORKER_NOT_FOUND));
        return getTransactions(profile.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public WalletResponse getWallet(Long workerProfileId) {
        return walletRepository.findByWorkerId(workerProfileId)
                .map(this::toResponse)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Thợ này chưa có ví"));
    }

    @Transactional(readOnly = true)
    public Page<WalletResponse> searchWallets(String keyword, boolean negativeOnly, Pageable pageable) {
        return walletRepository.search(blankToNull(keyword), negativeOnly, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> getTransactions(Long workerProfileId, Pageable pageable) {
        Wallet wallet = walletRepository.findByWorkerId(workerProfileId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Thợ này chưa có ví"));
        return transactionRepository.findByWalletIdOrderByIdDesc(wallet.getId(), pageable)
                .map(WalletTransactionResponse::from);
    }


    @Transactional
    public WalletTransactionResponse topUp(Long workerProfileId, BigDecimal amount, String referenceCode,
                                           String note, Long adminUserId) {
        Wallet wallet = ensureWallet(workerProfileId);
        String ref = blankToNull(referenceCode);
        String key = ref == null ? null : "TOPUP:" + ref.toUpperCase(Locale.ROOT);
        if (key != null && transactionRepository.existsByIdempotencyKey(key)) {
            throw new ApiException(ErrorCode.DUPLICATE_ENTRY, "Mã tham chiếu này đã được ghi nhận trước đó");
        }
        wallet.credit(amount);
        return WalletTransactionResponse.from(
                post(wallet, WalletTransactionType.TOPUP, amount, null, key, blankToNull(note), adminUserId));
    }

    @Transactional
    public WalletTransactionResponse adjust(Long workerProfileId, WalletTransactionType type, BigDecimal amount,
                                            String reason, Long adminUserId) {
        if (type != WalletTransactionType.ADJUSTMENT_CREDIT && type != WalletTransactionType.ADJUSTMENT_DEBIT) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Loại điều chỉnh không hợp lệ");
        }
        Wallet wallet = ensureWallet(workerProfileId);
        BigDecimal signed;
        if (type == WalletTransactionType.ADJUSTMENT_CREDIT) {
            wallet.credit(amount);
            signed = amount;
        } else {
            wallet.charge(amount);
            signed = amount.negate();
        }
        return WalletTransactionResponse.from(post(wallet, type, signed, null, null, reason.trim(), adminUserId));
    }


    @Transactional(propagation = Propagation.MANDATORY)
    public void chargeCommission(Long workerProfileId, BigDecimal commission, Long requestId,
                                 String requestCode, BigDecimal ratePercent) {
        Wallet wallet = ensureWallet(workerProfileId);
        String key = "COMMISSION:" + requestId;
        if (transactionRepository.existsByIdempotencyKey(key)) {
            log.warn("Đơn {} đã được trừ hoa hồng trước đó, bỏ qua", requestId);
            return;
        }
        wallet.charge(commission);
        String note = "Hoa hồng đơn " + requestCode + " (" + ratePercent.stripTrailingZeros().toPlainString() + "%)";
        post(wallet, WalletTransactionType.COMMISSION, commission.negate(), requestId, key, note, null);
    }

    /**
     * Ghi có phần thu nhập của thợ khi khách đã chuyển khoản vào tài khoản của nền tảng
     * (tiền nằm ở nền tảng nên nền tảng "nợ" thợ phần còn lại sau hoa hồng). Mỗi đơn chỉ ghi một lần.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void creditEarning(Long workerProfileId, BigDecimal workerAmount, Long requestId,
                              String requestCode, BigDecimal ratePercent) {
        Wallet wallet = ensureWallet(workerProfileId);
        String key = "ONLINE_PAYMENT:" + requestId;
        if (transactionRepository.existsByIdempotencyKey(key)) {
            log.warn("Đơn {} đã được ghi thu nhập thanh toán online trước đó, bỏ qua", requestId);
            return;
        }
        wallet.credit(workerAmount);
        String note = "Thu nhập đơn " + requestCode + " (chuyển khoản, đã trừ hoa hồng "
                + ratePercent.stripTrailingZeros().toPlainString() + "%)";
        post(wallet, WalletTransactionType.JOB_EARNING, workerAmount, requestId, key, note, null);
    }

    @Transactional(readOnly = true)
    public void assertCanTakeJobs(Long workerProfileId) {
        walletRepository.findByWorkerId(workerProfileId)
                .filter(w -> w.getBalance().compareTo(minBalanceToReceiveJobs) < 0)
                .ifPresent(w -> {
                    throw new ApiException(ErrorCode.WALLET_BALANCE_TOO_LOW);
                });
    }

    private WalletTransaction post(Wallet wallet, WalletTransactionType type, BigDecimal signedAmount,
                                   Long requestId, String idempotencyKey, String note, Long createdBy) {
        try {
            return transactionRepository.saveAndFlush(WalletTransaction.builder()
                    .wallet(wallet)
                    .type(type)
                    .amount(signedAmount)
                    .balanceAfter(wallet.getBalance())
                    .requestId(requestId)
                    .idempotencyKey(idempotencyKey)
                    .note(note)
                    .createdBy(createdBy)
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.DUPLICATE_ENTRY, "Giao dịch này đã được ghi nhận trước đó");
        }
    }

    private WalletResponse toResponse(Wallet w) {
        WorkerProfile profile = w.getWorker();
        return new WalletResponse(w.getId(), profile.getId(), profile.getUser().getFullName(),
                profile.getUser().getPhoneNumber(), w.getBalance(), minBalanceToReceiveJobs,
                w.getBalance().compareTo(minBalanceToReceiveJobs) >= 0, w.getUpdatedAt());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
