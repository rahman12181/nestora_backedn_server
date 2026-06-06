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
    private String content;           // Deleted ho to null/placeholder
    private Boolean isRead;
    private LocalDateTime sentAt;

    // 🆕 Naye fields
    private Boolean isDeletedForEveryone;   // true → "This message was deleted" dikhao
    private Boolean isEdited;               // true → "edited" tag dikhao
    private LocalDateTime editedAt;         // Kab edit hua
    private Boolean isMine;                 // true → current user ka message hai
}