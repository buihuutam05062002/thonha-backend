package com.thonha.backend.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SepayRequest {
    String subAccount;
    String code;
    String content;
    String transferType;
    String description;
    Integer transferAmount;
    String referenceCode;
    Integer accumulated;
    Integer id;
}