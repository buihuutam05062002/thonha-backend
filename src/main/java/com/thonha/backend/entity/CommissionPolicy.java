package com.thonha.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "commission_policy",
        uniqueConstraints = @UniqueConstraint(name = "uk_commission_policy_scope", columnNames = "scope_key"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommissionPolicy {

    public static final String DEFAULT_SCOPE = "DEFAULT";

    public static String scopeOfCategory(Long categoryId) {
        return "CATEGORY:" + categoryId;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    Long id;

    @Column(name = "scope_key", nullable = false, length = 40)
    String scopeKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @ToString.Exclude
    ServiceCategory category;

    @Column(name = "rate_percent", nullable = false, precision = 5, scale = 2)
    BigDecimal ratePercent;

    @Column(nullable = false)
    @Builder.Default
    boolean active = true;

    @Column(name = "updated_by")
    Long updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}
