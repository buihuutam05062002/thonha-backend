package com.thonha.backend.dto.request;

/**
 * Payload tin nhắn gửi qua WebSocket: SEND /app/chats/{chatId}/messages.
 * Chỉ có chữ; gửi ảnh vẫn dùng REST POST /api/v1/chats/{chatId}/messages (multipart).
 * Các kiểm tra (độ dài, trống, quyền, trạng thái yêu cầu) nằm hết trong ChatService.sendMessage.
 */
public record ChatWsMessage(String content) {
}
