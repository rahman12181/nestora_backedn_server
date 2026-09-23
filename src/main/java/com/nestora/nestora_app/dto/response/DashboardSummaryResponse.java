package com.nestora.nestora_app.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardSummaryResponse {

    private RevenueSummaryResponse revenue;
    private ActionRequiredResponse actionsRequired;
    private OwnerActivityResponse recentActivity;
    private OccupancyResponse occupancy;
    private TrendsResponse trends;
    private TopPropertyResponse topProperty;
    private TodayScheduleResponse todaySchedule;

    // Unread counts
    private Long unreadMessages;
    private Long unreadNotifications;
}