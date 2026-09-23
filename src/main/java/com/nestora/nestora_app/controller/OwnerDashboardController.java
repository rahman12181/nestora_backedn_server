package com.nestora.nestora_app.controller;

import com.nestora.nestora_app.dto.response.*;
import com.nestora.nestora_app.entity.User;
import com.nestora.nestora_app.service.OwnerDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/owner/dashboard")
@RequiredArgsConstructor
public class OwnerDashboardController {

    private final OwnerDashboardService dashboardService;

    // ============================================
    // 1. REVENUE
    // ============================================
    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<RevenueSummaryResponse>> getRevenue(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                "Revenue summary fetched",
                dashboardService.getRevenueSummary(currentUser)));
    }

    // ============================================
    // 2. ACTIONS REQUIRED
    // ============================================
    @GetMapping("/actions-required")
    public ResponseEntity<ApiResponse<ActionRequiredResponse>> getActions(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                "Actions fetched",
                dashboardService.getActionsRequired(currentUser)));
    }

    // ============================================
    // 3. RECENT ACTIVITY
    // ============================================
    @GetMapping("/activity")
    public ResponseEntity<ApiResponse<OwnerActivityResponse>> getActivity(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                "Activity fetched",
                dashboardService.getRecentActivity(currentUser, limit)));
    }

    // ============================================
    // 4. OCCUPANCY
    // ============================================
    @GetMapping("/occupancy")
    public ResponseEntity<ApiResponse<OccupancyResponse>> getOccupancy(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                "Occupancy fetched",
                dashboardService.getOccupancy(currentUser)));
    }

    // ============================================
    // 5. TRENDS
    // ============================================
    @GetMapping("/trends")
    public ResponseEntity<ApiResponse<TrendsResponse>> getTrends(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                "Trends fetched",
                dashboardService.getTrends(currentUser)));
    }

    // ============================================
    // 6. TOP PROPERTY
    // ============================================
    @GetMapping("/top-property")
    public ResponseEntity<ApiResponse<TopPropertyResponse>> getTopProperty(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                "Top property fetched",
                dashboardService.getTopProperty(currentUser)));
    }

    // ============================================
    // 7. TODAY'S SCHEDULE
    // ============================================
    @GetMapping("/today-schedule")
    public ResponseEntity<ApiResponse<TodayScheduleResponse>> getTodaySchedule(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                "Today schedule fetched",
                dashboardService.getTodaySchedule(currentUser)));
    }

    // ============================================
    // 8. ALL-IN-ONE SUMMARY ⭐
    // ============================================
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                "Dashboard summary fetched",
                dashboardService.getSummary(currentUser)));
    }
}