package com.thonha.backend.entity;

import com.thonha.backend.enums.CategoryStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "service_category")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    Long id;

    @Column(name = "category_name", nullable = false, unique = true, length = 100)
    String name;

    @Column(length = 500)
    String description;

    @Column(length = 255)
    String icon;

    @Column(name = "default_warranty_days", nullable = false, columnDefinition = "int default 0")
    @Builder.Default
    int defaultWarrantyDays = 30;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    CategoryStatus status = CategoryStatus.ACTIVE;
}