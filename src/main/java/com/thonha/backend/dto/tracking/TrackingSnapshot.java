package com.thonha.backend.dto.tracking;

import java.math.BigDecimal;

/** Ảnh chụp trạng thái theo dõi: khách vào trang là có ngay vị trí gần nhất, không phải chờ tin tiếp theo. */
public record TrackingSnapshot(Long requestId, String requestCode, String status,
                               Destination destination, WorkerInfo worker, TrackingEvent lastLocation) {
    public record Destination(Double lat, Double lng, String addressText) {
    }

    public record WorkerInfo(String name, String phoneNumber, String avatarUrl, BigDecimal rating) {
    }
}
