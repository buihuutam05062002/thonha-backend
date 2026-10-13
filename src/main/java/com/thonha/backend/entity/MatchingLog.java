package com.thonha.backend.entity;


import com.thonha.backend.enums.MatchingResult;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "matching_log", uniqueConstraints = @UniqueConstraint(name = "uk_matching_request_worker", columnNames = {"request_id", "worker_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MatchingLog {

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

    @Column(name = "total_score", precision = 7, scale = 4)
    BigDecimal totalScore;

    @Column(name = "distance_score", precision = 7, scale = 4)
    BigDecimal distanceScore;

    @Column(name = "rating_score", precision = 7, scale = 4)
    BigDecimal ratingScore;

    @Column(name = "acceptance_rate_score", precision = 7, scale = 4)
    BigDecimal acceptanceRateScore;

    @Column(name = "workload_score", precision = 7, scale = 4)
    BigDecimal workloadScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    MatchingResult result = MatchingResult.PENDING;

    @CreationTimestamp
    @Column(name = "sent_at", nullable = false, updatable = false)
    LocalDateTime sentAt;

    @Column(name = "responded_at")
    LocalDateTime respondedAt;

    @Column(columnDefinition = "text")
    String note;
}
