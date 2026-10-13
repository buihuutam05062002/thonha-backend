package com.thonha.backend.dto.tracking;

/** Đơn đang thực hiện của thợ (đã ghép / đang di chuyển). */
public record ActiveJob(Long id, String requestCode, String status, String category,
                        String description, String addressText, Double lat, Double lng) {
}
