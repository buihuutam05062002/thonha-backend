package com.thonha.backend.dto.response;

import com.thonha.backend.enums.PriorityLevel;
import com.thonha.backend.enums.RepairStatus;
import com.thonha.backend.entity.RequestAttachment;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class RepairRequestResponse {
     Long id;
     String requestCode;
     Long customerId;
     String customerName;
     Long categoryId;
     String categoryName;
     AddressResponse address;
     String addressText;
     Double lat;
     Double lng;
     String description;
     PriorityLevel priorityLevel;
     RepairStatus status;
     BigDecimal finalPrice;
     Long workerId;
     String workerName;
     LocalDateTime createdAt;
     LocalDateTime updatedAt;
     List<RequestAttachmentResponse> attachments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestAttachmentResponse {
        private Long id;
        private String type;
        private String url;
        private Integer sortOrder;
    }

    public static RepairRequestResponse from(com.thonha.backend.entity.RepairRequest r) {
        if (r == null) return null;
        return RepairRequestResponse.builder()
                .id(r.getId())
                .requestCode(r.getRequestCode())
                .customerId(r.getCustomer() != null ? r.getCustomer().getId() : null)
                .customerName(r.getCustomer() != null ? r.getCustomer().getFullName() : null)
                .categoryId(r.getCategory() != null ? r.getCategory().getId() : null)
                .categoryName(r.getCategory() != null ? r.getCategory().getName() : null)
                .address(com.thonha.backend.dto.response.AddressResponse.from(r.getAddress()))
                .addressText(r.getAddressText())
                .lat(r.getLat() != null ? r.getLat().doubleValue() : null)
                .lng(r.getLng() != null ? r.getLng().doubleValue() : null)
                .description(r.getDescription())
                .priorityLevel(r.getPriorityLevel())
                .status(r.getStatus())
                .finalPrice(r.getFinalPrice())
                .workerId(r.getWorker() != null ? r.getWorker().getId() : null)
                .workerName(r.getWorker() != null && r.getWorker().getUser() != null ? r.getWorker().getUser().getFullName() : null)
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .attachments(r.getAttachments() != null ? r.getAttachments().stream()
                        .map(a -> RequestAttachmentResponse.builder()
                                .id(a.getId())
                                .type(a.getType())
                                .url(a.getUrl())
                                .sortOrder(a.getSortOrder())
                                .build()).toList() : List.of())
                .build();
    }
}