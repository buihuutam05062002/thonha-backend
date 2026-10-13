package com.thonha.backend.entity;


import com.thonha.backend.enums.DiscountType;
import com.thonha.backend.enums.PromotionStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "promotion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    Long id;

    @Column(nullable = false, unique = true, length = 50)
    String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 15, scale = 2)
    BigDecimal discountValue;

    @Column(nullable = false)
    Integer quantity;

    @Column(name = "used_quantity", nullable = false)
    @Builder.Default
    Integer usedQuantity = 0;

    @Column(name = "start_date", nullable = false)
    LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    PromotionStatus status = PromotionStatus.ACTIVE;

    public boolean isUsable(LocalDate today) {
        return status == PromotionStatus.ACTIVE
                && usedQuantity < quantity
                && !today.isBefore(startDate)
                && !today.isAfter(endDate);
    }
}
