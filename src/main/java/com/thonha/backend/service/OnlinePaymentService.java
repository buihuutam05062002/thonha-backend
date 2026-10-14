package com.thonha.backend.service;

import com.thonha.backend.common.ApiException;
import com.thonha.backend.common.ErrorCode;
import com.thonha.backend.dto.request.SepayRequest;
import com.thonha.backend.dto.response.PaymentResponse;
import com.thonha.backend.entity.PaymentTransaction;
import com.thonha.backend.entity.Quotation;
import com.thonha.backend.entity.RepairRequest;
import com.thonha.backend.enums.PaymentMethod;
import com.thonha.backend.enums.QuotationStatus;
import com.thonha.backend.enums.RepairStatus;
import com.thonha.backend.enums.TransactionStatus;
import com.thonha.backend.repository.PaymentTransactionRepository;
import com.thonha.backend.repository.QuotationRepository;
import com.thonha.backend.repository.RepairRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Thanh toán online (chuyển khoản QR qua SePay) cho khách hàng.
 * <p>
 * Luồng: khách tạo mã QR cho báo giá đã chấp nhận -> chuyển khoản -> SePay gọi webhook -> giao dịch thành công,
 * ghi có phần thu nhập của thợ vào ví (tiền nằm ở tài khoản nền tảng), thông báo cho khách và thợ.
 * Khi thợ bấm hoàn thành, {@link PaymentTransactionService#settleCompletedRequest} thấy đã có giao dịch
 * thành công nên không tạo thêm giao dịch tiền mặt.
 * <p>
 * Mọi thay đổi trên một đơn đều khóa bản ghi đơn trước ({@code findByIdForUpdate}) để tránh chạy đua với
 * hủy đơn / hoàn thành đơn / webhook gọi lặp.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OnlinePaymentService {

    /** Chỉ cho thanh toán khi đã có thợ phụ trách và đơn chưa kết thúc. */
    private static final Set<RepairStatus> PAYABLE =
            EnumSet.of(RepairStatus.MATCHED, RepairStatus.ON_THE_WAY, RepairStatus.IN_PROGRESS);

    private final PaymentTransactionRepository transactionRepository;
    private final RepairRequestRepository requestRepository;
    private final QuotationRepository quotationRepository;
    private final CommissionPolicyService commissionPolicyService;
    private final WalletService walletService;
    private final NotificationService notificationService;
    private final SePayService sePayService;

    /** Khách tạo (hoặc lấy lại) mã QR chuyển khoản cho báo giá đã chấp nhận của đơn. */
    @Transactional
    public PaymentResponse createBankTransfer(Long customerUserId, Long requestId) {
        RepairRequest request = requestRepository.findByIdForUpdate(requestId)
                .filter(r -> r.getCustomer().getId().equals(customerUserId))
                .orElseThrow(() -> new ApiException(ErrorCode.REQUEST_NOT_FOUND));

        if (request.getWorker() == null || !PAYABLE.contains(request.getStatus())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST,
                    "Chỉ thanh toán online được khi đã có thợ phụ trách và yêu cầu chưa hoàn thành");
        }
        if (transactionRepository.existsByRequestIdAndStatus(requestId, TransactionStatus.SUCCESS)) {
            throw new ApiException(ErrorCode.CONFLICT, "Yêu cầu này đã được thanh toán");
        }

        // Số tiền lấy từ báo giá đã chấp nhận ở phía server, không nhận từ client
        Quotation quotation = quotationRepository
                .findFirstByRequestIdAndStatusOrderByIdDesc(requestId, QuotationStatus.ACCEPTED)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REQUEST,
                        "Bạn cần chấp nhận báo giá của thợ trước khi thanh toán online"));
        BigDecimal amount = toWholeVnd(quotation.getTotalAmount());

        Optional<PaymentTransaction> pending = transactionRepository
                .findFirstByRequestIdAndPaymentMethodAndStatusOrderByIdDesc(
                        requestId, PaymentMethod.BANK_TRANSFER, TransactionStatus.PENDING_PAYMENT);
        if (pending.isPresent() && pending.get().getAmount().compareTo(amount) == 0) {
            return toResponse(pending.get());
        }
        // Báo giá đã đổi: đóng mã cũ, tạo mã mới (mã cũ nếu bị chuyển nhầm sẽ không được tự ghi nhận)
        List<PaymentTransaction> stale = transactionRepository.findByRequestIdAndPaymentMethodAndStatus(
                requestId, PaymentMethod.BANK_TRANSFER, TransactionStatus.PENDING_PAYMENT);
        stale.forEach(t -> t.setStatus(TransactionStatus.CANCELLED));
        transactionRepository.saveAll(stale);

        BigDecimal rate = commissionPolicyService.resolveRatePercent(request.getCategory().getId());
        CommissionCalculator.Result split = CommissionCalculator.calculate(amount, rate);
        PaymentTransaction tx = transactionRepository.save(PaymentTransaction.builder()
                .request(request)
                .amount(amount)
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .commissionRate(rate)
                .commissionAmount(split.commissionAmount())
                .workerAmount(split.workerAmount())
                .status(TransactionStatus.PENDING_PAYMENT)
                .build());
        return toResponse(tx);
    }

    /** Trạng thái thanh toán hiện tại của đơn (null nếu chưa có giao dịch nào đang chờ/đã thành công). */
    @Transactional(readOnly = true)
    public PaymentResponse getForRequest(Long customerUserId, Long requestId) {
        requestRepository.findByIdAndCustomerId(requestId, customerUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.REQUEST_NOT_FOUND));
        Optional<PaymentTransaction> paid =
                transactionRepository.findFirstByRequestIdAndStatus(requestId, TransactionStatus.SUCCESS);
        if (paid.isPresent()) {
            return toResponse(paid.get());
        }
        return transactionRepository
                .findFirstByRequestIdAndPaymentMethodAndStatusOrderByIdDesc(
                        requestId, PaymentMethod.BANK_TRANSFER, TransactionStatus.PENDING_PAYMENT)
                .map(this::toResponse)
                .orElse(null);
    }

    /**
     * Xử lý webhook của SePay (đã xác thực API key ở controller). Trả về nhãn kết quả để ghi log.
     * Chỉ ném exception với lỗi hệ thống (để SePay gọi lại); các trường hợp không khớp chỉ ghi log và bỏ qua,
     * không tự cộng tiền, để người quản trị đối soát thủ công.
     */
    @Transactional
    public String handleSepayWebhook(SepayRequest payload) {
        if (payload == null || payload.getId() == null) {
            return "IGNORED_EMPTY";
        }
        if (!"in".equalsIgnoreCase(payload.getTransferType())) {
            return "IGNORED_NOT_INCOMING";
        }
        Optional<Long> paymentId = sePayService.extractPaymentId(payload.getCode(), payload.getContent());
        if (paymentId.isEmpty()) {
            log.info("Sepay {}: không thấy mã thanh toán trong nội dung, bỏ qua", payload.getId());
            return "IGNORED_NO_CODE";
        }
        Optional<Long> requestId = transactionRepository.findRequestId(paymentId.get());
        if (requestId.isEmpty()) {
            log.warn("Sepay {}: mã thanh toán {} không tồn tại", payload.getId(), paymentId.get());
            return "IGNORED_UNKNOWN_PAYMENT";
        }

        // Khóa đơn trước rồi mới đọc giao dịch: mọi luồng khác (tạo QR, hủy, hoàn thành) cũng khóa đơn trước
        RepairRequest request = requestRepository.findByIdForUpdate(requestId.get()).orElse(null);
        PaymentTransaction tx = transactionRepository.findById(paymentId.get()).orElse(null);
        if (request == null || tx == null) {
            return "IGNORED_UNKNOWN_PAYMENT";
        }

        String gatewayId = String.valueOf(payload.getId());
        if (tx.getStatus() == TransactionStatus.SUCCESS) {
            if (!gatewayId.equals(tx.getGatewayTransactionCode())) {
                log.warn("Sepay {}: giao dịch {} đã thanh toán trước đó, khoản chuyển thêm cần hoàn tiền thủ công",
                        payload.getId(), tx.getId());
            }
            return "DUPLICATE";
        }
        if (transactionRepository.existsByGatewayTransactionCode(gatewayId)) {
            return "DUPLICATE";
        }
        if (tx.getPaymentMethod() != PaymentMethod.BANK_TRANSFER || tx.getStatus() != TransactionStatus.PENDING_PAYMENT) {
            log.warn("Sepay {}: nhận tiền cho giao dịch {} đã đóng (trạng thái {}), cần hoàn tiền thủ công",
                    payload.getId(), tx.getId(), tx.getStatus());
            return "IGNORED_CLOSED";
        }
        BigDecimal paid = BigDecimal.valueOf(payload.getTransferAmount() == null ? 0L : payload.getTransferAmount());
        if (paid.compareTo(tx.getAmount()) != 0) {
            log.warn("Sepay {}: sai số tiền cho giao dịch {} (nhận {}, cần {}), cần đối soát thủ công",
                    payload.getId(), tx.getId(), paid.toPlainString(), tx.getAmount().toPlainString());
            return "AMOUNT_MISMATCH";
        }
        if (request.getWorker() == null || !PAYABLE.contains(request.getStatus())) {
            log.warn("Sepay {}: đơn {} không còn ở trạng thái nhận thanh toán ({}), cần hoàn tiền thủ công",
                    payload.getId(), request.getId(), request.getStatus());
            return "REQUEST_NOT_PAYABLE";
        }

        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setGatewayTransactionCode(gatewayId);
        transactionRepository.save(tx);

        // Giá chốt của đơn = số tiền khách đã trả
        request.setFinalPrice(tx.getAmount());
        request.setUpdatedAt(LocalDateTime.now());
        requestRepository.save(request);

        if (tx.getWorkerAmount() != null && tx.getWorkerAmount().signum() > 0) {
            walletService.creditEarning(request.getWorker().getId(), tx.getWorkerAmount(), request.getId(),
                    request.getRequestCode(), tx.getCommissionRate());
        }
        notificationService.sendPaymentSuccess(request.getCustomer().getId(), request.getId(), tx.getAmount());
        notificationService.sendPaymentReceived(request.getWorker().getUser().getId(), request.getId(), tx.getAmount());
        log.info("Sepay {}: đã ghi nhận thanh toán {} cho đơn {}", payload.getId(), tx.getAmount(), request.getRequestCode());
        return "PAID";
    }

    private static BigDecimal toWholeVnd(BigDecimal amount) {
        try {
            BigDecimal whole = amount.setScale(0, RoundingMode.UNNECESSARY);
            if (whole.signum() <= 0) {
                throw new ApiException(ErrorCode.INVALID_REQUEST, "Số tiền báo giá không hợp lệ");
            }
            return whole;
        } catch (ArithmeticException e) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Số tiền báo giá phải là số đồng nguyên");
        }
    }

    private PaymentResponse toResponse(PaymentTransaction tx) {
        String code = sePayService.paymentCode(tx.getId());
        boolean pending = tx.getStatus() == TransactionStatus.PENDING_PAYMENT;
        return new PaymentResponse(
                tx.getId(),
                tx.getRequest().getId(),
                tx.getAmount(),
                tx.getStatus(),
                tx.getStatus().getDescription(),
                tx.getPaymentMethod(),
                code,
                pending ? sePayService.buildQrUrl(tx.getAmount(), code) : null,
                sePayService.getBankName(),
                sePayService.getBankAccount(),
                sePayService.getAccountHolder(),
                tx.getCreatedAt());
    }
}
