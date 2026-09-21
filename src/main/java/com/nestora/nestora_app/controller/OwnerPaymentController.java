package com.nestora.nestora_app.controller;

import com.nestora.nestora_app.dto.request.OwnerPayoutUpiRequest;
import com.nestora.nestora_app.dto.response.ApiResponse;
import com.nestora.nestora_app.dto.response.RentPaymentResponse;
import com.nestora.nestora_app.dto.response.PayoutStatusResponse;
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
public class OwnerPaymentController {

    private final RentPaymentService rentPaymentService;

    // 14.5 Set Payout UPI ID — owner must do this ONCE before they can receive rent payments
    @PutMapping("/owner/payout-upi")
    public ResponseEntity<ApiResponse<String>> setPayoutUpi(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody OwnerPayoutUpiRequest request) {

        rentPaymentService.setPayoutUpiId(currentUser, request.getPayoutUpiId());
        return ResponseEntity.ok(ApiResponse.success("Payout UPI ID saved. You'll receive rent payments here automatically."));
    }

    // 14.6 Get My Rent Payments (Owner) — see all payments received for their properties
    @GetMapping("/owner/payments")
    public ResponseEntity<ApiResponse<List<RentPaymentResponse>>> getMyPayments(
            @AuthenticationPrincipal User currentUser) {

        List<RentPaymentResponse> payments = rentPaymentService.getOwnerPayments(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Payments fetched", payments));
    }

    // ✅ 14.7 Get Payout Status — check if owner has UPI set and current payout status
    @GetMapping("/owner/payout-status")
    public ResponseEntity<ApiResponse<PayoutStatusResponse>> getPayoutStatus(
            @AuthenticationPrincipal User currentUser) {

        PayoutStatusResponse response = rentPaymentService.getPayoutStatus(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Payout status fetched", response));
    }
}