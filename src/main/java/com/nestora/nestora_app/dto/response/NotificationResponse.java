package com.nestora.nestora_app.dto.response;


import com.nestora.nestora_app.enums.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long notificationId;
    private String title;
    private String body;
    private NotificationType type;
    private Long refId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
