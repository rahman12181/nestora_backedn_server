package com.nestora.nestora_app.controller;

import com.nestora.nestora_app.dto.request.BookingFilterRequest;
import com.nestora.nestora_app.dto.request.BookingRespondRequest;
import com.nestora.nestora_app.dto.response.ApiResponse;
import com.nestora.nestora_app.dto.response.BookingResponse;
import com.nestora.nestora_app.dto.response.BookingStatsResponse;
import com.nestora.nestora_app.dto.response.BookingTimelineResponse;
import com.nestora.nestora_app.entity.User;
import com.nestora.nestora_app.service.BookingAnalyticsService;
import com.nestora.nestora_app.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/owner/booking-requests")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final BookingAnalyticsService analyticsService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getIncomingRequests(
            @AuthenticationPrincipal User currentUser,
            @ModelAttribute BookingFilterRequest filter) {
        return ResponseEntity.ok(
                ApiResponse.success("Booking requests fetched",
                        bookingService.getIncomingRequests(currentUser, filter))
        );
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingDetail(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long requestId) {
        return ResponseEntity.ok(
                ApiResponse.success("Booking detail fetched",
                        bookingService.getBookingDetail(currentUser, requestId))
        );
    }

    @GetMapping("/{requestId}/timeline")
    public ResponseEntity<ApiResponse<List<BookingTimelineResponse>>> getTimeline(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long requestId) {
        return ResponseEntity.ok(
                ApiResponse.success("Timeline fetched",
                        bookingService.getTimeline(currentUser, requestId))
        );
    }

    @PatchMapping("/{requestId}/accept")
    public ResponseEntity<ApiResponse<BookingResponse>> acceptRequest(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long requestId,
            @Valid @RequestBody BookingRespondRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Booking request accepted",
                        bookingService.acceptRequest(currentUser, requestId, request))
        );
    }

    @PatchMapping("/{requestId}/reject")
    public ResponseEntity<ApiResponse<BookingResponse>> rejectRequest(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long requestId,
            @Valid @RequestBody BookingRespondRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Booking request rejected",
                        bookingService.rejectRequest(currentUser, requestId, request))
        );
    }

    @PatchMapping("/{requestId}/undo")
    public ResponseEntity<ApiResponse<BookingResponse>> undoResponse(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long requestId) {
        return ResponseEntity.ok(
                ApiResponse.success("Response undone",
                        bookingService.undoResponse(currentUser, requestId))
        );
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<BookingStatsResponse>> getStats(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                ApiResponse.success("Stats fetched",
                        analyticsService.getStats(currentUser))
        );
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal User currentUser) {
        BookingStatsResponse stats = analyticsService.getStats(currentUser);
        return ResponseEntity.ok(
                ApiResponse.success("Unread count fetched", stats.getUnreadBookingCount())
        );
    }
}