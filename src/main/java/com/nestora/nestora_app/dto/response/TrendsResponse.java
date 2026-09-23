package com.nestora.nestora_app.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TrendsResponse {

    // ============ BOOKINGS (last 7 days) ============
    private List<DailyPoint> bookingsTrend;
    private Integer totalThisWeek;
    private Integer totalLastWeek;
    private Double growthPercent;

    // ============ VIEWS (last 7 days) ============
    private List<DailyPoint> viewsTrend;
    private Long totalViewsThisWeek;

    @Data
    @Builder
    public static class DailyPoint {
        private String day;         // "Mon", "Tue" or "2026-09-15"
        private String date;        // ISO date
        private Long value;
        private Long accepted;      // for bookings only
        private Long rejected;      // for bookings only
    }
}