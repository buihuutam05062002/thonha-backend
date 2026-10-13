package com.thonha.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class ChatEvent {
    String type;
    Long chatId;
    Long requestId;
    MessageResponse message;
}
