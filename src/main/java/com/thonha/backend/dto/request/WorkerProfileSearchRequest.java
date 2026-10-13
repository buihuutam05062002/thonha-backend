package com.thonha.backend.dto.request;

import com.thonha.backend.enums.ApprovalStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class WorkerProfileSearchRequest {
    String keyword;
    ApprovalStatus status;
    String city;
}