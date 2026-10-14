package com.thonha.backend.dto.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/** Payload SePay gửi tới webhook (các trường lạ được bỏ qua). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SepayRequest {
    /** ID giao dịch trên SePay - dùng để chống xử lý trùng khi SePay gọi lại. */
    Long id;
    String gateway;
    String transactionDate;
    String accountNumber;
    String subAccount;
    /** Mã thanh toán SePay tự nhận diện theo "Cấu trúc mã thanh toán"; có thể null. */
    String code;
    String content;
    /** "in" = tiền vào, "out" = tiền ra. */
    String transferType;
    String description;
    Long transferAmount;
    String referenceCode;
    Long accumulated;
}
