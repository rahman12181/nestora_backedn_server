package com.nestora.nestora_app.dto.response;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ConversationResponse {
    private Long conversationId;
    private Long otherUserId;
    private String otherUserName;
    private String otherUserPic;
    private String propertyTitle;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private Long unreadCount;
}