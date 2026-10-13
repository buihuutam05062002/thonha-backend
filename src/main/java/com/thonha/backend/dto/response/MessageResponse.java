package com.thonha.backend.dto.response;

import com.thonha.backend.entity.Message;
import com.thonha.backend.entity.MessageImage;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class MessageResponse {
    Long id;
    Long chatId;
    Long senderId;
    String senderName;
    String content;
    String messageType;
    List<String> images;
    boolean seen;
    LocalDateTime sentAt;

    public static MessageResponse from(Message m) {
        return MessageResponse.builder()
                .id(m.getId())
                .chatId(m.getChat().getId())
                .senderId(m.getSender().getId())
                .senderName(m.getSender().getFullName())
                .content(m.getContent())
                .messageType(m.getMessageType().name())
                .images(m.getImages().stream().map(MessageImage::getUrl).toList())
                .seen(Boolean.TRUE.equals(m.getSeen()))
                .sentAt(m.getSentAt())
                .build();
    }
}
