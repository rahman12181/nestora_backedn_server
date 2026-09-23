package com.nestora.nestora_app.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TodayScheduleResponse {

    private LocalDate date;
    private Integer totalEvents;
    private List<ScheduleItem> events;

    @Data
    @Builder
    public static class ScheduleItem {
        private Long id;
        private String eventType;       // MOVE_IN, VISIT, PAYMENT_DUE, MEETING
        private String title;
        private String description;
        private LocalDateTime scheduledAt;
        private String timeLabel;       // "10:00 AM"
        private String icon;
        private String color;
        private Long referenceId;       // booking ID
        private String referenceType;
    }
}