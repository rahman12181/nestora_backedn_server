package com.nestora.nestora_app.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OwnerActivityResponse {

    private List<ActivityItem> activities;

    @Data
    @Builder
    public static class ActivityItem {
        private Long id;
        private String activityType;    // BOOKING, PAYMENT, REVIEW, VIEW, MESSAGE, PROPERTY
        private String title;
        private String description;
        private String icon;
        private String color;
        private Long referenceId;
        private String referenceType;
        private LocalDateTime createdAt;
        private String timeAgo;         // "2h ago", "1d ago"
    }
}