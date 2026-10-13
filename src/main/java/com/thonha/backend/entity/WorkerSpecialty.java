package com.thonha.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(
        name = "worker_specialty",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_worker_specialty",
                columnNames = {"worker_profile_id", "service_category_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkerSpecialty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_profile_id", nullable = false)
    @ToString.Exclude
    WorkerProfile workerProfile;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "service_category_id", nullable = false)
    @ToString.Exclude
    ServiceCategory serviceCategory;
}