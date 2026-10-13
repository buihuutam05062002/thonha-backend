package com.thonha.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class ChatResponse {
    Long id;
    Long requestId;
    String requestCode;
    Long otherUserId;
    String otherUserName;
    String otherUserAvatarUrl;
    boolean canSend;
    long unreadCount;
    MessageResponse lastMessage;
}
