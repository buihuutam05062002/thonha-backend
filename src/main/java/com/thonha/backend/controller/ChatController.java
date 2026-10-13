package com.thonha.backend.controller;

import com.thonha.backend.common.ApiResponse;
import com.thonha.backend.dto.response.ChatResponse;
import com.thonha.backend.dto.response.MessageResponse;
import com.thonha.backend.security.CurrentUserProvider;
import com.thonha.backend.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chats")
public class ChatController {

    private final ChatService chatService;
    private final CurrentUserProvider currentUserProvider;

    public ChatController(ChatService chatService, CurrentUserProvider currentUserProvider) {
        this.chatService = chatService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/by-request/{requestId}")
    public ResponseEntity<ApiResponse<ChatResponse>> byRequest(@PathVariable Long requestId) {
        return ResponseEntity.ok(ApiResponse.success(
                chatService.getOrCreateByRequest(requestId, currentUserProvider.requireUserId())));
    }

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> messages(
            @PathVariable Long chatId,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "30") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                chatService.getMessages(chatId, currentUserProvider.requireUserId(), beforeId, size)));
    }

    @PostMapping(value = "/{chatId}/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MessageResponse>> send(
            @PathVariable Long chatId,
            @RequestParam(value = "content", required = false) String content,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return ResponseEntity.status(201).body(ApiResponse.success(
                chatService.sendMessage(chatId, currentUserProvider.requireUserId(), content, files)));
    }

    @GetMapping("/{chatId}/unseen-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unseenCount(@PathVariable Long chatId) {
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("count", chatService.unseenCount(chatId, currentUserProvider.requireUserId()))));
    }

    @PatchMapping("/{chatId}/seen")
    public ResponseEntity<ApiResponse<Void>> seen(@PathVariable Long chatId) {
        chatService.markSeen(chatId, currentUserProvider.requireUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
