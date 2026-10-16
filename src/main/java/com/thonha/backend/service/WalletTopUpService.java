package com.thonha.backend.service;

import com.thonha.backend.common.ApiException;
import com.thonha.backend.common.ErrorCode;
import com.thonha.backend.dto.request.SepayRequest;
import com.thonha.backend.dto.response.TopUpResponse;
import com.thonha.backend.entity.Wallet;
import com.thonha.backend.entity.WalletTopUp;
import com.thonha.backend.entity.WorkerProfile;
import com.thonha.backend.enums.TransactionStatus;
import com.thonha.backend.repository.WalletRepository;
import com.thonha.backend.repository.WalletTopUpRepository;
import com.thonha.backend.repository.WalletTransactionRepository;
import com.thonha.backend.repository.WorkerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Thợ tự nạp tiền vào ví hoa hồng bằng chuyển khoản QR (SePay) — thay cho việc quản trị viên nạp tay.
 * <p>
 * Luồng: thợ tạo lệnh nạp -> quét QR chuyển khoản -> SePay gọi webhook -> cộng tiền vào ví.
 * Khách thanh toán trực tiếp cho thợ (tiền mặt hoặc chuyển khoản riêng), nền tảng không giữ tiền của khách;
 * hoa hồng vẫn được trừ vào ví khi đơn hoàn thành ({@link PaymentTransactionService#settleCompletedRequest}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletTopUpService {
    private final WalletTopUpRepository topUpRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletService walletService;
    private final SePayService sePayService;

    @Value("${app.wallet.topup-min:10000}")
    private BigDecimal minAmount;
    @Value("${app.wallet.topup-max:50000000}")
    private BigDecimal maxAmount;

    /** Thợ tạo (hoặc lấy lại) lệnh nạp đang chờ cùng số tiền. */
    @Transactional
    public TopUpResponse create(Long workerUserId, BigDecimal amount) {
        WorkerProfile profile = workerProfileRepository.findByUserId(workerUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.WORKER_NOT_FOUND));
        BigDecimal whole = toWholeVnd(amount);
        if (whole.compareTo(minAmount) < 0 || whole.compareTo(maxAmount) > 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Số tiền nạp phải từ "
                    + minAmount.toPlainString() + "đ đến " + maxAmount.toPlainString() + "đ");
        }
        Optional<WalletTopUp> pending = topUpRepository.findFirstByWorkerIdAndAmountAndStatusOrderByIdDesc(
                profile.getId(), whole, TransactionStatus.PENDING_PAYMENT);
        if (pending.isPresent()) {
            return toResponse(pending.get());
        }
        WalletTopUp topUp = topUpRepository.save(WalletTopUp.builder()
                .worker(profile)
                .amount(whole)
                .status(TransactionStatus.PENDING_PAYMENT)
                .build());
        return toResponse(topUp);
    }

    /** Thợ xem trạng thái một lệnh nạp của chính mình (giao diện hỏi lại định kỳ cho tới khi tiền về). */
    @Transactional(readOnly = true)
    public TopUpResponse get(Long workerUserId, Long topUpId) {
        WorkerProfile profile = workerProfileRepository.findByUserId(workerUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.WORKER_NOT_FOUND));
        WalletTopUp topUp = topUpRepository.findByIdAndWorkerId(topUpId, profile.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Không tìm thấy lệnh nạp tiền"));
        return toResponse(topUp);
    }

    /**
     * Xử lý webhook SePay (đã xác thực API key ở controller). Trả về nhãn kết quả để ghi log.
     * Tiền đã vào tài khoản nền tảng và mã nạp xác định đúng thợ nên cộng đúng SỐ TIỀN THỰC NHẬN vào ví
     * (kể cả khi thợ sửa số tiền trong app ngân hàng). Mỗi mã giao dịch SePay chỉ được cộng một lần.
     */
    @Transactional
    public String handleSepayWebhook(SepayRequest payload) {
        if (payload == null || payload.getId() == null) {
            return "IGNORED_EMPTY";
        }
        if (!"in".equalsIgnoreCase(payload.getTransferType())) {
            return "IGNORED_NOT_INCOMING";
        }
        Optional<Long> topUpId = sePayService.extractTopUpId(payload.getCode(), payload.getContent());
        if (topUpId.isEmpty()) {
            log.info("Sepay {}: không thấy mã nạp ví trong nội dung, bỏ qua", payload.getId());
            return "IGNORED_NO_CODE";
        }
        WalletTopUp topUp = topUpRepository.findByIdForUpdate(topUpId.get()).orElse(null);
        if (topUp == null) {
            log.warn("Sepay {}: mã nạp ví {} không tồn tại, cần đối soát thủ công", payload.getId(), topUpId.get());
            return "IGNORED_UNKNOWN_TOPUP";
        }
        String gatewayId = String.valueOf(payload.getId());
        // Khớp khóa chống trùng của WalletService.topUp ("TOPUP:" + mã tham chiếu viết hoa)
        String reference = "SEPAY:" + gatewayId;
        if (walletTransactionRepository.existsByIdempotencyKey("TOPUP:" + reference.toUpperCase())) {
            return "DUPLICATE";
        }
        BigDecimal received = BigDecimal.valueOf(payload.getTransferAmount() == null ? 0L : payload.getTransferAmount());
        if (received.signum() <= 0) {
            return "IGNORED_ZERO_AMOUNT";
        }
        if (received.compareTo(topUp.getAmount()) != 0) {
            log.warn("Sepay {}: lệnh nạp {} yêu cầu {} nhưng nhận {}, vẫn ghi có theo số thực nhận",
                    payload.getId(), topUp.getId(), topUp.getAmount().toPlainString(), received.toPlainString());
        }
        String note = "Nạp tiền qua chuyển khoản QR (" + sePayService.topUpCode(topUp.getId()) + ")";
        walletService.topUp(topUp.getWorker().getId(), received, reference, note, null);

        if (topUp.getStatus() != TransactionStatus.SUCCESS) {
            topUp.setStatus(TransactionStatus.SUCCESS);
            topUp.setPaidAmount(received);
            topUp.setGatewayTransactionCode(gatewayId);
            topUp.setPaidAt(LocalDateTime.now());
            topUpRepository.save(topUp);
        }
        log.info("Sepay {}: đã nạp {} vào ví thợ {} (lệnh {})", payload.getId(), received.toPlainString(),
                topUp.getWorker().getId(), topUp.getId());
        return "TOPPED_UP";
    }

    private static BigDecimal toWholeVnd(BigDecimal amount) {
        try {
            return amount.setScale(0, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Số tiền nạp phải là số đồng nguyên");
        }
    }

    private TopUpResponse toResponse(WalletTopUp t) {
        boolean pending = t.getStatus() == TransactionStatus.PENDING_PAYMENT;
        String code = sePayService.topUpCode(t.getId());
        BigDecimal balance = t.getStatus() == TransactionStatus.SUCCESS
                ? walletRepository.findByWorkerId(t.getWorker().getId()).map(Wallet::getBalance).orElse(null)
                : null;
        return new TopUpResponse(
                t.getId(),
                t.getAmount(),
                t.getPaidAmount(),
                t.getStatus(),
                t.getStatus().getDescription(),
                code,
                pending ? sePayService.buildQrUrl(t.getAmount(), code) : null,
                sePayService.getBankName(),
                sePayService.getBankAccount(),
                sePayService.getAccountHolder(),
                balance,
                t.getCreatedAt(),
                t.getPaidAt());
    }
}
