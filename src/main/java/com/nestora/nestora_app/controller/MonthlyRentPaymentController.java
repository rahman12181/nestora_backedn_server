package com.nestora.nestora_app.controller;

import com.nestora.nestora_app.dto.response.ApiResponse;
import com.nestora.nestora_app.entity.MonthlyRentPayment;
import com.nestora.nestora_app.entity.User;
import com.nestora.nestora_app.service.MonthlyRentPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/monthly-rent")
@RequiredArgsConstructor
public class MonthlyRentPaymentController {

    private final MonthlyRentPaymentService monthlyRentService;

    // Initiate monthly rent payment
    @PostMapping("/initiate/{invoiceId}")
    public ResponseEntity<ApiResponse<MonthlyRentPayment>> initiate(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long invoiceId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Payment initiated",
                monthlyRentService.initiatePayment(currentUser, invoiceId)));
    }

    // Confirm payment
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<MonthlyRentPayment>> confirm(
            @AuthenticationPrincipal User currentUser,
            @RequestBody ConfirmPaymentRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                "Payment confirmed",
                monthlyRentService.confirmPayment(
                        currentUser,
                        req.razorpayOrderId,
                        req.razorpayPaymentId,
                        req.razorpaySignature)));
    }

    // Get my payments
    @GetMapping("/my-payments")
    public ResponseEntity<ApiResponse<List<MonthlyRentPayment>>> getMyPayments(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                "Payments fetched",
                monthlyRentService.getMyPayments(currentUser)));
    }

    public static class ConfirmPaymentRequest {
        public String razorpayOrderId;
        public String razorpayPaymentId;
        public String razorpaySignature;
    }
}