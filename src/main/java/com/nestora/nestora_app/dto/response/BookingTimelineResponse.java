package com.nestora.nestora_app.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BookingTimelineResponse {
    private String eventType;
    private String title;
    private String description;
    private LocalDateTime timestamp;
    private String icon;
    private String color;
}