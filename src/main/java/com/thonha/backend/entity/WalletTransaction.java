package com.thonha.backend.entity;

import com.thonha.backend.enums.WalletTransactionType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Immutable
@Table(name = "wallet_transaction",
        uniqueConstraints = @UniqueConstraint(name = "uk_wallet_tx_idempotency", columnNames = "idempotency_key"),
        indexes = @Index(name = "idx_wallet_tx_wallet_created", columnList = "wallet_id, created_at"))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    @ToString.Exclude
    Wallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    WalletTransactionType type;

    @Column(nullable = false, precision = 15, scale = 2)
    BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 15, scale = 2)
    BigDecimal balanceAfter;

    @Column(name = "request_id")
    Long requestId;

    @Column(name = "idempotency_key", length = 100)
    String idempotencyKey;

    @Column(length = 500)
    String note;

    @Column(name = "created_by")
    Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;
}
