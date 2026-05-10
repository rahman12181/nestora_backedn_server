package com.nestora.nestora_app.controller;

import com.nestora.nestora_app.dto.request.OwnerApplyRequest;
import com.nestora.nestora_app.dto.request.SubscriptionBuyRequest;
import com.nestora.nestora_app.dto.request.SubscriptionConfirmRequest;
import com.nestora.nestora_app.dto.response.*;
import com.nestora.nestora_app.entity.User;
import com.nestora.nestora_app.service.OwnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/owner")
@RequiredArgsConstructor
public class OwnerController {

    private final OwnerService ownerService;

    @PostMapping(
            value = "/apply",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<String>> applyAsOwner(
            @AuthenticationPrincipal User currentUser,
            @Valid @ModelAttribute OwnerApplyRequest request,
            @RequestPart(value = "aadharDoc", required = false) MultipartFile aadharDoc,
            @RequestPart(value = "panDoc", required = false) MultipartFile panDoc,
            @RequestPart(value = "addressProof", required = false) MultipartFile addressProof) {

        String message = ownerService.applyAsOwner(
                currentUser, request, aadharDoc, panDoc, addressProof
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(message));
    }

    @GetMapping("/my-profile")
    public ResponseEntity<ApiResponse<OwnerProfileResponse>> getMyProfile(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                ApiResponse.success("Profile fetched",
                        ownerService.getMyProfile(currentUser))
        );
    }

    @GetMapping("/verification-status")
    public ResponseEntity<ApiResponse<VerificationStatusResponse>> getVerificationStatus(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                ApiResponse.success("Status fetched",
                        ownerService.getVerificationStatus(currentUser))
        );
    }

    @PostMapping("/subscription/buy")
    public ResponseEntity<ApiResponse<SubscriptionOrderResponse>> buySubscription(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody SubscriptionBuyRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Order created",
                        ownerService.buySubscription(currentUser, request))
        );
    }

    @PostMapping("/subscription/confirm")
    public ResponseEntity<ApiResponse<SubscriptionDetailsResponse>> confirmSubscription(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody SubscriptionConfirmRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Subscription activated",
                        ownerService.confirmSubscription(currentUser, request))
        );
    }

    @GetMapping("/subscription/details")
    public ResponseEntity<ApiResponse<SubscriptionDetailsResponse>> getSubscriptionDetails(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                ApiResponse.success("Subscription details fetched",
                        ownerService.getSubscriptionDetails(currentUser))
        );
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<OwnerDashboardResponse>> getDashboard(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                ApiResponse.success("Dashboard fetched",
                        ownerService.getMyDashboard(currentUser))
        );
    }

    @PostMapping("/properties/{propertyId}/feature")
    public ResponseEntity<ApiResponse<String>> buyFeaturedListing(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long propertyId,
            @RequestParam Integer days) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        ownerService.buyFeaturedListing(currentUser, propertyId, days))
        );
    }
}