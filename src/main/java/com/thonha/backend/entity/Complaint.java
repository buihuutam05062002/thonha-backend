package com.thonha.backend.entity;


import com.thonha.backend.enums.ComplaintReason;
import com.thonha.backend.enums.ComplaintStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "complaint")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    @ToString.Exclude
    RepairRequest request;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "complainant_id", nullable = false)
    @ToString.Exclude
    User complainant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by_id")
    @ToString.Exclude
    User handledBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    ComplaintReason reason;

    @Column(columnDefinition = "text")
    String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    ComplaintStatus status = ComplaintStatus.PENDING;

    @Column(columnDefinition = "text")
    String resolution;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    LocalDateTime resolvedAt;

    @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    List<ComplaintImage> images = new ArrayList<>();

    public void addImage(String url) {
        images.add(ComplaintImage.builder().complaint(this).url(url).build());
    }
}
