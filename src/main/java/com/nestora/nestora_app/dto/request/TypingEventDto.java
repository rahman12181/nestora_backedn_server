package com.nestora.nestora_app.dto.request;

import lombok.Data;

@Data
public class TypingEventDto {

    private Long conversationId;

    private Long senderId;

    private Long receiverId;

    private boolean isTyping;

}