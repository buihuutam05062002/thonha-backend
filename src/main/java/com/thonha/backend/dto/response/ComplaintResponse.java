package com.thonha.backend.dto.response;

import com.thonha.backend.entity.Complaint;
import com.thonha.backend.entity.ComplaintImage;
import com.thonha.backend.entity.RepairRequest;
import com.thonha.backend.entity.User;
import com.thonha.backend.enums.ComplaintReason;
import com.thonha.backend.enums.ComplaintStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ComplaintResponse {
    Long id;
    Long requestId;
    String requestCode;

    Long complainantId;
    String complainantName;
    String complainantRole;

    Long againstId;
    String againstName;

    ComplaintReason reason;
    String reasonLabel;
    String description;

    ComplaintStatus status;
    String statusLabel;
    String resolution;

    List<String> images;

    LocalDateTime createdAt;
    LocalDateTime resolvedAt;

    Long handledById;
    String handledByName;


    public static ComplaintResponse from(Complaint c, boolean withImages) {
        RepairRequest r = c.getRequest();
        User complainant = c.getComplainant();
        User customer = r.getCustomer();
        User workerUser = r.getWorker() != null ? r.getWorker().getUser() : null;

        boolean byCustomer = customer.getId().equals(complainant.getId());
        User against = byCustomer ? workerUser : customer;
        User handler = c.getHandledBy();

        return ComplaintResponse.builder()
                .id(c.getId())
                .requestId(r.getId())
                .requestCode(r.getRequestCode())
                .complainantId(complainant.getId())
                .complainantName(complainant.getFullName())
                .complainantRole(byCustomer ? "CUSTOMER" : "WORKER")
                .againstId(against != null ? against.getId() : null)
                .againstName(against != null ? against.getFullName() : null)
                .reason(c.getReason())
                .reasonLabel(c.getReason().getDescription())
                .description(c.getDescription())
                .status(c.getStatus())
                .statusLabel(c.getStatus().getDescription())
                .resolution(c.getResolution())
                .images(withImages
                        ? c.getImages().stream().map(ComplaintImage::getUrl).toList()
                        : List.of())
                .createdAt(c.getCreatedAt())
                .resolvedAt(c.getResolvedAt())
                .handledById(handler != null ? handler.getId() : null)
                .handledByName(handler != null ? handler.getFullName() : null)
                .build();
    }
}
