package com.nestora.nestora_app.dto.response;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MessageResponse {
    private Long messageId;
    private Long conversationId;
    private Long senderId;
    private String senderName;
    private String senderPic;
    private String content;
    private Boolean isRead;
    private LocalDateTime sentAt;
}