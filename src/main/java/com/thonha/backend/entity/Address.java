package com.thonha.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Table(name = "address")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    User user;

    @Column(length = 100)
    String label;

    @Column(name = "full_address", nullable = false, length = 500)
    String fullAddress;

    @Column(precision = 10, scale = 7)
    BigDecimal lat;

    @Column(precision = 10, scale = 7)
    BigDecimal lng;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    Boolean defaultAddress = false;
}