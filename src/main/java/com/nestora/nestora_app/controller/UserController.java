package com.nestora.nestora_app.controller;


import com.nestora.nestora_app.dto.request.*;
import com.nestora.nestora_app.dto.response.*;
import com.nestora.nestora_app.entity.Report;
import com.nestora.nestora_app.entity.User;
import com.nestora.nestora_app.repository.ReportRepository;
import com.nestora.nestora_app.repository.UserRepository;
import com.nestora.nestora_app.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                ApiResponse.success("Profile fetched",
                        userService.getProfile(currentUser))
        );
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Profile updated successfully",
                        userService.updateProfile(currentUser, request))
        );
    }

    @PostMapping(
            value = "/profile/picture",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<UserProfileResponse>> uploadProfilePic(
            @AuthenticationPrincipal User currentUser,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(
                ApiResponse.success("Profile picture updated",
                        userService.uploadProfilePic(currentUser, file))
        );
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        userService.changePassword(currentUser, request))
        );
    }

    // Wishlist
    @PostMapping("/saved-properties/{propertyId}")
    public ResponseEntity<ApiResponse<String>> saveProperty(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long propertyId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        userService.saveProperty(currentUser, propertyId))
        );
    }

    @DeleteMapping("/saved-properties/{propertyId}")
    public ResponseEntity<ApiResponse<String>> removeSavedProperty(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long propertyId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        userService.removeSavedProperty(currentUser, propertyId))
        );
    }

    @GetMapping("/saved-properties")
    public ResponseEntity<ApiResponse<List<SavedPropertyResponse>>> getSavedProperties(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                ApiResponse.success("Saved properties fetched",
                        userService.getSavedProperties(currentUser))
        );
    }

    // Booking
    @PostMapping("/booking-requests")
    public ResponseEntity<ApiResponse<BookingResponse>> sendBookingRequest(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody BookingCreateRequest request) {
        BookingResponse response = userService.sendBookingRequest(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking request sent successfully", response));
    }

    @GetMapping("/booking-requests")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookingRequests(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                ApiResponse.success("Booking requests fetched",
                        userService.getMyBookingRequests(currentUser))
        );
    }

    @DeleteMapping("/booking-requests/{requestId}")
    public ResponseEntity<ApiResponse<String>> cancelBookingRequest(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long requestId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        userService.cancelBookingRequest(currentUser, requestId))
        );
    }

    // Reviews
    @PostMapping("/reviews/{propertyId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> writeReview(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long propertyId,
            @Valid @RequestBody ReviewCreateRequest request) {
        ReviewResponse response = userService.writeReview(
                currentUser, propertyId, request
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review submitted successfully", response));
    }

    @PutMapping("/fcm-token")
    public ResponseEntity<ApiResponse<String>> updateFcmToken(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateFcmTokenRequest request) {

        currentUser.setFcmToken(request.getFcmToken());
        userRepository.save(currentUser);

        return ResponseEntity.ok(ApiResponse.success("FCM token updated"));
    }

    @PostMapping("/report")
    public ResponseEntity<ApiResponse<String>> submitReport(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody ReportRequest request) {

        Report report = Report.builder()
                .reporter(currentUser)
                .type(request.getType())
                .refId(request.getRefId())
                .reason(request.getReason())
                .description(request.getDescription())
                .status("PENDING")
                .build();

        reportRepository.save(report);

        return ResponseEntity.ok(
                ApiResponse.success("Report submitted. Our team will review it.")
        );
    }
}
