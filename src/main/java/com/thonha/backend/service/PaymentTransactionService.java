package com.thonha.backend.service;

import com.thonha.backend.common.ApiException;
import com.thonha.backend.common.ErrorCode;
import com.thonha.backend.dto.response.CommissionSettlementResponse;
import com.thonha.backend.dto.response.CommissionSummaryResponse;
import com.thonha.backend.dto.worker.WorkerIncomeResponse;
import com.thonha.backend.entity.PaymentTransaction;
import com.thonha.backend.entity.RepairRequest;
import com.thonha.backend.enums.PaymentMethod;
import com.thonha.backend.enums.TransactionStatus;
import com.thonha.backend.repository.PaymentTransactionRepository;
import com.thonha.backend.repository.RepairRequestRepository;
import com.thonha.backend.dto.response.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentTransactionService {

    private static final LocalDateTime MIN_TIME = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final LocalDateTime MAX_TIME = LocalDateTime.of(2999, 1, 1, 0, 0);
    static final int MAX_INCOME_DAYS = 366;

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final CommissionPolicyService commissionPolicyService;
    private final WalletService walletService;
    private final RepairRequestRepository requestRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public PaymentTransaction settleCompletedRequest(RepairRequest request) {
        Optional<PaymentTransaction> existing =
                paymentTransactionRepository.findFirstByRequestIdAndStatus(request.getId(), TransactionStatus.SUCCESS);
        if (existing.isPresent()) {
            return existing.get();
        }
        if (request.getWorker() == null) {
            throw new IllegalStateException("Đơn " + request.getId() + " chưa có thợ phụ trách");
        }
        BigDecimal price = request.getFinalPrice();
        if (price == null || price.signum() <= 0) {
            throw new ApiException(ErrorCode.FINAL_PRICE_REQUIRED);
        }

        // Đơn chốt bằng tiền mặt thì mã QR chuyển khoản còn chờ không còn hiệu lực (tránh trả hai lần)
        cancelPendingOnline(request.getId());
        BigDecimal rate = commissionPolicyService.resolveRatePercent(request.getCategory().getId());
        CommissionCalculator.Result result = CommissionCalculator.calculate(price, rate);

        PaymentTransaction tx = paymentTransactionRepository.save(PaymentTransaction.builder()
                .request(request)
                .amount(price)
                .paymentMethod(PaymentMethod.CASH)
                .commissionRate(rate)
                .commissionAmount(result.commissionAmount())
                .workerAmount(result.workerAmount())
                .status(TransactionStatus.SUCCESS)
                .build());

        if (result.commissionAmount().signum() > 0) {
            walletService.chargeCommission(request.getWorker().getId(), result.commissionAmount(),
                    request.getId(), request.getRequestCode(), rate);
        }
        return tx;
    }

    /**
     * Khách xem đơn của mình đã được ghi nhận thanh toán chưa (null nếu chưa). Khách thanh toán trực tiếp cho
     * thợ nên không còn mã QR phía khách; giao dịch chỉ xuất hiện sau khi thợ chốt giá và hoàn thành đơn.
     */
    @Transactional(readOnly = true)
    public PaymentResponse getForRequest(Long customerUserId, Long requestId) {
        requestRepository.findByIdAndCustomerId(requestId, customerUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.REQUEST_NOT_FOUND));
        return paymentTransactionRepository.findFirstByRequestIdAndStatus(requestId, TransactionStatus.SUCCESS)
                .map(tx -> new PaymentResponse(tx.getId(), requestId, tx.getAmount(), tx.getStatus(),
                        tx.getStatus().getDescription(), tx.getPaymentMethod(),
                        null, null, null, null, null, tx.getCreatedAt()))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean hasSuccessfulPayment(Long requestId) {
        return paymentTransactionRepository.existsByRequestIdAndStatus(requestId, TransactionStatus.SUCCESS);
    }

    /** Đóng các mã QR chuyển khoản còn chờ của đơn (khi đơn bị hủy hoặc đã chốt thanh toán tiền mặt). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void cancelPendingOnline(Long requestId) {
        List<PaymentTransaction> pending = paymentTransactionRepository.findByRequestIdAndPaymentMethodAndStatus(
                requestId, PaymentMethod.BANK_TRANSFER, TransactionStatus.PENDING_PAYMENT);
        pending.forEach(t -> t.setStatus(TransactionStatus.CANCELLED));
        paymentTransactionRepository.saveAll(pending);
    }

    @Transactional(readOnly = true)
    public Page<CommissionSettlementResponse> search(LocalDate from, LocalDate to, Pageable pageable) {
        return paymentTransactionRepository
                .search(TransactionStatus.SUCCESS, start(from), endExclusive(to), pageable)
                .map(CommissionSettlementResponse::from);
    }

    @Transactional(readOnly = true)
    public CommissionSummaryResponse summarize(LocalDate from, LocalDate to) {
        return toSummary(paymentTransactionRepository.summarize(TransactionStatus.SUCCESS, start(from), endExclusive(to)));
    }


    @Transactional(readOnly = true)
    public WorkerIncomeResponse incomeForWorker(Long workerId, LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Vui lòng chọn khoảng ngày");
        }
        if (to.isBefore(from)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");
        }
        if (to.toEpochDay() - from.toEpochDay() + 1 > MAX_INCOME_DAYS) {
            throw new ApiException(ErrorCode.INVALID_REQUEST,
                    "Khoảng ngày tối đa là " + MAX_INCOME_DAYS + " ngày");
        }
        LocalDateTime start = start(from);
        LocalDateTime end = endExclusive(to);
        CommissionSummaryResponse summary = toSummary(
                paymentTransactionRepository.summarizeByWorker(workerId, TransactionStatus.SUCCESS, start, end));
        List<IncomeDailyAggregator.Entry> entries = paymentTransactionRepository
                .findEntriesByWorker(workerId, TransactionStatus.SUCCESS, start, end).stream()
                .map(r -> new IncomeDailyAggregator.Entry(
                        r.getCreatedAt(), r.getAmount(), r.getCommission(), r.getWorkerAmount()))
                .toList();
        return new WorkerIncomeResponse(from, to, summary, IncomeDailyAggregator.aggregate(from, to, entries));
    }

    private static CommissionSummaryResponse toSummary(PaymentTransactionRepository.SummaryRow row) {
        return new CommissionSummaryResponse(
                row.getOrders() == null ? 0 : row.getOrders(),
                nz(row.getGross()), nz(row.getCommission()), nz(row.getWorkerTotal()));
    }

    private static LocalDateTime start(LocalDate from) {
        return from == null ? MIN_TIME : from.atStartOfDay();
    }

    private static LocalDateTime endExclusive(LocalDate to) {
        return to == null ? MAX_TIME : to.plusDays(1).atStartOfDay();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
