package com.thonha.backend.entity;

import com.thonha.backend.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lệnh nạp tiền vào ví của thợ qua chuyển khoản QR (SePay).
 * <p>
 * Thợ tạo lệnh -> nhận QR có sẵn số tiền + nội dung "NAP{id}" -> chuyển khoản -> SePay gọi webhook ->
 * backend cộng tiền vào ví và chuyển lệnh sang SUCCESS. {@code amount} là số tiền thợ yêu cầu,
 * {@code paidAmount} là số tiền thực nhận (ghi có vào ví theo số thực nhận).
 */
@Entity
@Table(name = "wallet_topup", indexes = @Index(name = "idx_topup_worker", columnList = "worker_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WalletTopUp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    @ToString.Exclude
    WorkerProfile worker;

    @Column(nullable = false, precision = 15, scale = 2)
    BigDecimal amount;

    @Column(name = "paid_amount", precision = 15, scale = 2)
    BigDecimal paidAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    TransactionStatus status = TransactionStatus.PENDING_PAYMENT;

    /** Mã giao dịch phía SePay của lần chuyển khoản đã ghi nhận (để đối soát). */
    @Column(name = "gateway_transaction_code", length = 100)
    String gatewayTransactionCode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column(name = "paid_at")
    LocalDateTime paidAt;
}
