package com.thonha.backend.dto.tracking;

/** Tin nhắn thợ gửi lên server: vị trí hiện tại (heading = hướng di chuyển theo độ, có thể null). */
public record LocationMessage(Double lat, Double lng, Double heading) {
}
