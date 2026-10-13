package com.thonha.backend.controller;

import com.thonha.backend.common.ApiException;
import com.thonha.backend.dto.request.ChatWsMessage;
import com.thonha.backend.dto.response.MessageResponse;
import com.thonha.backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/**
 * Gửi tin nhắn chữ qua WebSocket: SEND /app/chats/{chatId}/messages  với body {"content": "..."}.
 * Controller chỉ chuyển tiếp sang {@link ChatService#sendMessage} nên mọi kiểm tra nghiệp vụ vẫn ở một chỗ.
 *
 * - Người nhận được đẩy tin mới ở /topic/user/{userId}/chat (do ChatService phát, sau khi commit).
 * - Người gửi nhận lại tin đã lưu ở /user/queue/chat (xác nhận, có id/sentAt thật).
 * - Lỗi trả riêng cho người gửi ở /user/queue/errors theo định dạng {code, message}.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWsController {

    private final ChatService chatService;

    @MessageMapping("/chats/{chatId}/messages")
    @SendToUser("/queue/chat")
    public MessageResponse send(@DestinationVariable("chatId") Long chatId,
                                @Payload(required = false) ChatWsMessage msg,
                                Principal principal) {
        String content = msg == null ? null : msg.content();
        return chatService.sendMessage(chatId, Long.valueOf(principal.getName()), content, null);
    }

    @MessageExceptionHandler(ApiException.class)
    @SendToUser("/queue/errors")
    public Map<String, String> onApiError(ApiException e) {
        return Map.of("code", e.getCode(), "message", e.getMessage());
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public Map<String, String> onError(Exception e) {
        log.warn("Lỗi khi xử lý tin nhắn chat qua WebSocket", e);
        return Map.of("code", "INTERNAL_ERROR", "message", "Không gửi được tin nhắn, vui lòng thử lại");
    }
}
