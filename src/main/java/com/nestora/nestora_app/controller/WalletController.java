package com.nestora.nestora_app.controller;

import com.nestora.nestora_app.dto.request.WithdrawRequest;
import com.nestora.nestora_app.dto.response.ApiResponse;
import com.nestora.nestora_app.dto.response.WalletTransactionResponse;
import com.nestora.nestora_app.dto.response.WithdrawalResponse;
import com.nestora.nestora_app.entity.User;
import com.nestora.nestora_app.service.WalletService;
import com.nestora.nestora_app.service.WithdrawalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final WithdrawalService withdrawalService;

    // 13.3 Get Wallet Transaction History (paginated ledger)
    @GetMapping("/user/wallet/transactions")
    public ResponseEntity<ApiResponse<Page<WalletTransactionResponse>>> getTransactions(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<WalletTransactionResponse> transactions = walletService.getTransactions(currentUser, pageable);
        return ResponseEntity.ok(ApiResponse.success("Wallet transactions fetched", transactions));
    }

    // 13.4 Request Withdrawal
    @PostMapping("/user/wallet/withdraw")
    public ResponseEntity<ApiResponse<WithdrawalResponse>> requestWithdrawal(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody WithdrawRequest request) {

        WithdrawalResponse response = withdrawalService.requestWithdrawal(
                currentUser, request.getAmount(), request.getUpiId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Withdrawal request submitted. We'll process it within 24-48 hours.", response));
    }

    // 13.5 Get My Withdrawal Requests
    @GetMapping("/user/wallet/withdrawals")
    public ResponseEntity<ApiResponse<List<WithdrawalResponse>>> getMyWithdrawals(
            @AuthenticationPrincipal User currentUser) {

        List<WithdrawalResponse> withdrawals = withdrawalService.getMyWithdrawals(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Withdrawal requests fetched", withdrawals));
    }
}