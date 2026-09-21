package com.nestora.nestora_app.controller;

import com.nestora.nestora_app.dto.request.ConfirmPaymentRequest;
import com.nestora.nestora_app.dto.request.InitiatePaymentRequest;
import com.nestora.nestora_app.dto.response.*;
import com.nestora.nestora_app.entity.User;
import com.nestora.nestora_app.service.RentPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BookingPaymentController {

    private final RentPaymentService rentPaymentService;

    // 14.1 Get Payment Summary — call this BEFORE showing the "Pay Now" button,
    // so you can display the discount + final amount to the student
    @GetMapping("/user/bookings/{bookingRequestId}/payment-summary")
    public ResponseEntity<ApiResponse<PaymentSummaryResponse>> getPaymentSummary(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long bookingRequestId) {

        PaymentSummaryResponse summary = rentPaymentService.getPaymentSummary(currentUser, bookingRequestId);
        return ResponseEntity.ok(ApiResponse.success("Payment summary fetched", summary));
    }

    // 14.2 Initiate Payment — creates Razorpay Order, returns details for Razorpay Checkout
    @PostMapping("/user/bookings/{bookingRequestId}/pay/initiate")
    public ResponseEntity<ApiResponse<InitiatePaymentResponse>> initiatePayment(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long bookingRequestId,
            @RequestBody(required = false) InitiatePaymentRequest request) {

        InitiatePaymentResponse response = rentPaymentService.initiatePayment(currentUser, bookingRequestId, request);
        return ResponseEntity.ok(ApiResponse.success("Payment order created", response));
    }

    // 14.3 Confirm Payment — call after Razorpay Checkout success callback
    @PostMapping("/user/bookings/{bookingRequestId}/pay/confirm")
    public ResponseEntity<ApiResponse<RentPaymentResponse>> confirmPayment(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long bookingRequestId,
            @Valid @RequestBody ConfirmPaymentRequest request) {

        RentPaymentResponse response = rentPaymentService.confirmPayment(
                currentUser, request.getRazorpayOrderId(), request.getRazorpayPaymentId(), request.getRazorpaySignature());

        return ResponseEntity.ok(ApiResponse.success("Payment successful! Booking confirmed.", response));
    }

    // 14.4 Get My Payments — student's own rent payment history
    @GetMapping("/user/payments")
    public ResponseEntity<ApiResponse<List<RentPaymentResponse>>> getMyPayments(
            @AuthenticationPrincipal User currentUser) {

        List<RentPaymentResponse> payments = rentPaymentService.getMyPayments(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Payments fetched", payments));
    }

    // 14.8 Get Discount Eligibility — standalone check, no booking needed.
    // Use this to show/hide the "First booking? Get 20% off!" banner anywhere in the app
    // (search screen, home screen, etc.) with real, accurate data.
    @GetMapping("/user/discount-eligibility")
    public ResponseEntity<ApiResponse<DiscountEligibilityResponse>> getDiscountEligibility(
            @AuthenticationPrincipal User currentUser) {

        DiscountEligibilityResponse response = rentPaymentService.getDiscountEligibility(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Discount eligibility fetched", response));
    }
}