package com.nestora.nestora_app.controller;


import com.nestora.nestora_app.dto.request.BookingRespondRequest;
import com.nestora.nestora_app.dto.response.ApiResponse;
import com.nestora.nestora_app.dto.response.BookingResponse;
import com.nestora.nestora_app.entity.User;
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

    @GetMapping
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getIncomingRequests(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                ApiResponse.success("Booking requests fetched",
                        bookingService.getIncomingRequests(currentUser))
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
}
