package com.thonha.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tích hợp SePay (chuyển khoản QR + webhook báo biến động số dư).
 * <p>
 * Mỗi giao dịch chờ thanh toán có một mã riêng = {@code payment-prefix} + id giao dịch (vd "TT15").
 * Khách ghi mã này vào nội dung chuyển khoản; SePay gọi webhook về backend kèm nội dung đó và backend
 * đối chiếu lại. Tiền tố phải khớp với "Cấu trúc mã thanh toán" cấu hình trong trang quản trị SePay.
 */
@Service
public class SePayService {

    @Value("${sepay.bank-account}")
    String bankAccount;

    @Value("${sepay.bank-name}")
    String bankName;

    @Value("${sepay.accountHolder}")
    String accountHolder;

    /** Khóa SePay gửi trong header "Authorization: Apikey <khóa>"; để trống = từ chối mọi webhook. */
    @Value("${sepay.api-key:}")
    String apiKey;

    @Value("${sepay.payment-prefix:TT}")
    String paymentPrefix;

    public String getBankAccount() {
        return bankAccount;
    }

    public String getBankName() {
        return bankName;
    }

    public String getAccountHolder() {
        return accountHolder;
    }

    /** Mã thanh toán khách phải ghi vào nội dung chuyển khoản. */
    public String paymentCode(Long paymentId) {
        return paymentPrefix + paymentId;
    }

    /**
     * Lấy id giao dịch từ mã SePay đã nhận diện (`code`) hoặc từ nội dung chuyển khoản (ngân hàng có thể
     * thêm chữ vào trước/sau, nên tìm theo tiền tố + chuỗi số). Không phân biệt hoa thường.
     */
    public Optional<Long> extractPaymentId(String code, String content) {
        Pattern pattern = Pattern.compile(Pattern.quote(paymentPrefix) + "(\\d{1,15})", Pattern.CASE_INSENSITIVE);
        for (String text : new String[]{code, content}) {
            if (text == null || text.isBlank()) {
                continue;
            }
            Matcher m = pattern.matcher(text);
            if (m.find()) {
                return Optional.of(Long.parseLong(m.group(1)));
            }
        }
        return Optional.empty();
    }

    /** Kiểm tra header "Authorization: Apikey <khóa>" của webhook (so sánh thời gian không đổi). */
    public boolean isValidApiKey(String authorizationHeader) {
        if (apiKey == null || apiKey.isBlank() || authorizationHeader == null) {
            return false;
        }
        String header = authorizationHeader.trim();
        String scheme = "apikey ";
        if (header.length() <= scheme.length() || !header.regionMatches(true, 0, scheme, 0, scheme.length())) {
            return false;
        }
        String given = header.substring(scheme.length()).trim();
        return MessageDigest.isEqual(given.getBytes(StandardCharsets.UTF_8), apiKey.getBytes(StandardCharsets.UTF_8));
    }

    /** URL ảnh QR VietQR có sẵn số tiền và nội dung chuyển khoản. */
    public String buildQrUrl(BigDecimal amount, String des) {
        return "https://qr.sepay.vn/img?"
                + "acc=" + URLEncoder.encode(bankAccount, StandardCharsets.UTF_8)
                + "&bank=" + URLEncoder.encode(bankName, StandardCharsets.UTF_8)
                + "&amount=" + amount.setScale(0, RoundingMode.HALF_UP).toPlainString()
                + "&des=" + URLEncoder.encode(des, StandardCharsets.UTF_8)
                + "&template=compact"
                + "&showinfo=false"
                + "&holder=" + URLEncoder.encode(accountHolder, StandardCharsets.UTF_8);
    }
}
