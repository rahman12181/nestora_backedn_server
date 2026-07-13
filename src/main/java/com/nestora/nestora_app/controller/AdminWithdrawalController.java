package com.nestora.nestora_app.controller;

import com.nestora.nestora_app.dto.request.WithdrawalActionDtos.RejectWithdrawalRequest;
import com.nestora.nestora_app.dto.response.ApiResponse;
import com.nestora.nestora_app.dto.response.WithdrawalResponse;
import com.nestora.nestora_app.entity.User;
import com.nestora.nestora_app.enums.WithdrawalStatus;
import com.nestora.nestora_app.service.WithdrawalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminWithdrawalController {

    private final WithdrawalService withdrawalService;

    // 13.6 Get Pending Withdrawal Requests (Admin)
    @GetMapping("/admin/withdrawals/pending")
    public ResponseEntity<ApiResponse<List<WithdrawalResponse>>> getPendingWithdrawals() {
        List<WithdrawalResponse> withdrawals = withdrawalService.getPendingWithdrawals();
        return ResponseEntity.ok(ApiResponse.success("Pending withdrawals fetched", withdrawals));
    }

    // 13.9 Get Full Payment History (Admin) — every withdrawal ever made, newest first.
    // Optional ?status=APPROVED / PENDING / PROCESSING / FAILED / REJECTED to filter.
    @GetMapping("/admin/withdrawals/all")
    public ResponseEntity<ApiResponse<List<WithdrawalResponse>>> getAllWithdrawals(
            @RequestParam(required = false) WithdrawalStatus status) {

        List<WithdrawalResponse> withdrawals = withdrawalService.getAllWithdrawals(status);
        return ResponseEntity.ok(ApiResponse.success("Payment history fetched", withdrawals));
    }

    // 13.7 Approve Withdrawal — NOW triggers a REAL automatic UPI payout via RazorpayX.
    // No request body needed anymore — no manual UTR entry, Razorpay handles the transfer
    // and reports back the actual UTR (either immediately, or later via webhook).
    @PatchMapping("/admin/withdrawals/{withdrawalId}/approve")
    public ResponseEntity<ApiResponse<WithdrawalResponse>> approveWithdrawal(
            @AuthenticationPrincipal User admin,
            @PathVariable Long withdrawalId) {

        WithdrawalResponse response = withdrawalService.approveWithdrawal(admin, withdrawalId);

        String message = "APPROVED".equals(response.getStatus())
                ? "Payout successful! Money sent to user's UPI."
                : "Payout initiated — processing, will confirm shortly.";

        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    // 13.8 Reject Withdrawal — refunds the amount back to the user's wallet
    @PatchMapping("/admin/withdrawals/{withdrawalId}/reject")
    public ResponseEntity<ApiResponse<WithdrawalResponse>> rejectWithdrawal(
            @AuthenticationPrincipal User admin,
            @PathVariable Long withdrawalId,
            @Valid @RequestBody RejectWithdrawalRequest request) {

        WithdrawalResponse response = withdrawalService.rejectWithdrawal(
                admin, withdrawalId, request.getReason());

        return ResponseEntity.ok(ApiResponse.success("Withdrawal rejected. Amount refunded to user's wallet.", response));
    }
}