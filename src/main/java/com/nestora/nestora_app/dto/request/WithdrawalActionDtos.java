package com.nestora.nestora_app.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

public class WithdrawalActionDtos {

    @Data
    public static class ApproveWithdrawalRequest {
        @NotBlank(message = "Transaction reference is required")
        private String transactionRef; // UPI/bank ref number after manual payment
    }

    @Data
    public static class RejectWithdrawalRequest {
        @NotBlank(message = "Rejection reason is required")
        private String reason;
    }
}