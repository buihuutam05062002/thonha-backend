package com.thonha.backend.entity;

import com.thonha.backend.enums.WarrantyResolution;
import com.thonha.backend.enums.WarrantyStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "warranty_claim", indexes = {
        @Index(name = "idx_warranty_worker_status", columnList = "worker_id, status"),
        @Index(name = "idx_warranty_request", columnList = "request_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WarrantyClaim {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    @ToString.Exclude
    RepairRequest request;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    @ToString.Exclude
    WorkerProfile worker;
    
    @Column(nullable = false, columnDefinition = "text")
    String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 20)
    @Builder.Default
    WarrantyStatus status = WarrantyStatus.PENDING;

    @Column(name = "scheduled_at")
    LocalDateTime scheduledAt;

    @Column(name = "schedule_note", length = 500)
    String scheduleNote;

    @Column(name = "reject_reason", length = 500)
    String rejectReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_type", length = 30)
    WarrantyResolution resolutionType;

    @Column(name = "resolution_note", columnDefinition = "text")
    String resolutionNote;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column(name = "responded_at")
    LocalDateTime respondedAt;

    @Column(name = "completed_at")
    LocalDateTime completedAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "warranty_claim_evidence", joinColumns = @JoinColumn(name = "claim_id"))
    @Column(name = "url", nullable = false, length = 500)
    @Builder.Default
    @ToString.Exclude
    List<String> evidenceUrls = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "warranty_claim_result", joinColumns = @JoinColumn(name = "claim_id"))
    @Column(name = "url", nullable = false, length = 500)
    @Builder.Default
    @ToString.Exclude
    List<String> resultUrls = new ArrayList<>();
}
