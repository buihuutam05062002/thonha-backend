package com.thonha.backend.entity;


import com.thonha.backend.enums.ApprovalStatus;
import com.thonha.backend.enums.AvailabilityStatus;
import com.thonha.backend.enums.DocumentType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "worker_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
     Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @ToString.Exclude
     User user;

    @Column(name = "residence_city", nullable = false, length = 100)
     String provinceCity;

    @Column(name = "service_area", nullable = false, length = 255)
     String operatingArea;

    @Column(name = "years_of_experience", nullable = false)
     Integer experienceYears;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    @Builder.Default
     ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false, length = 20)
    @Builder.Default
     AvailabilityStatus availabilityStatus = AvailabilityStatus.OFFLINE;

    @Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
    @Builder.Default
     BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "acceptance_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
     BigDecimal acceptanceRate = new BigDecimal("100.00");

    @Column(name = "active_job_count", nullable = false)
    @Builder.Default
     Integer ongoingJobsCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
     LocalDateTime createdAt;

    @Column(name = "reviewed_at")
     LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    @ToString.Exclude
     User reviewedBy;

    @Column(name = "reject_reason", length = 500)
     String rejectReason;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "bank_account_id")
    @ToString.Exclude
     BankAccount bankAccount;


    @OneToMany(mappedBy = "workerProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    @OrderBy("id ASC")
    @Builder.Default
    @ToString.Exclude
     List<WorkerDocument> documents = new ArrayList<>();

    @OneToMany(mappedBy = "workerProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    @Builder.Default
    @ToString.Exclude
     List<WorkerSpecialty> specialties = new ArrayList<>();

    public void addDocument(DocumentType type, String fileUrl) {
        WorkerDocument doc = WorkerDocument.builder()
                .workerProfile(this)
                .type(type)
                .fileUrl(fileUrl)
                .build();
        documents.add(doc);
    }

    public void addSpecialty(ServiceCategory category) {
        boolean exists = specialties.stream()
                .anyMatch(s -> s.getServiceCategory().getId().equals(category.getId()));
        if (exists) {
            return;
        }
        WorkerSpecialty specialty = WorkerSpecialty.builder()
                .workerProfile(this)
                .serviceCategory(category)
                .build();
        specialties.add(specialty);
    }

    public void removeSpecialty(ServiceCategory category) {
        specialties.removeIf(s -> s.getServiceCategory().getId().equals(category.getId()));
    }
}