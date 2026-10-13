package com.thonha.backend.dto.response;

import com.thonha.backend.entity.Address;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class AddressResponse {
    Long id;
    String label;
    String fullAddress;
    Double lat;
    Double lng;
    Boolean defaultAddress;

    public static AddressResponse from(Address a) {
        if (a == null) return null;
        return AddressResponse.builder()
                .id(a.getId())
                .label(a.getLabel())
                .fullAddress(a.getFullAddress())
                .lat(a.getLat() != null ? a.getLat().doubleValue() : null)
                .lng(a.getLng() != null ? a.getLng().doubleValue() : null)
                .defaultAddress(a.getDefaultAddress())
                .build();
    }
}