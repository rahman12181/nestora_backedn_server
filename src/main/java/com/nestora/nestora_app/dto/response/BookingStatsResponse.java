package com.nestora.nestora_app.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BookingStatsResponse {
    private Long todayNew;
    private Long todayAccepted;
    private Long todayRejected;
    private Long totalPending;
    private Long urgentPending;
    private Long totalBookings;
    private Long totalAccepted;
    private Long totalRejected;
    private Double acceptanceRate;
    private Double avgResponseHours;
    private Integer avgResponseMinutes;   // ✅ Integer
    private Long thisWeekBookings;
    private Long lastWeekBookings;
    private Double weeklyGrowthPercent;
    private Long thisMonthBookings;
    private Long unreadBookingCount;
}