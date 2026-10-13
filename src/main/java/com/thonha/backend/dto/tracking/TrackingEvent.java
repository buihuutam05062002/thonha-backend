package com.thonha.backend.dto.tracking;

/** Sự kiện vị trí server phát cho khách hàng qua /topic/requests/{id}/location. */
public record TrackingEvent(Long requestId, String status, double lat, double lng, Double heading,
                            Long distanceMeters, Integer etaMinutes, long updatedAt) {
}
