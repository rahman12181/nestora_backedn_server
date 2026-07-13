package com.nestora.nestora_app.enums;

public enum WithdrawalStatus {
    PENDING,     // requested, admin hasn't triggered payout yet
    PROCESSING,  // admin triggered payout, RazorpayX is processing it (async UPI settlement)
    APPROVED,    // payout confirmed successful — money has actually reached the user's UPI
    FAILED,      // payout failed/reversed by RazorpayX — amount auto-refunded to wallet
    REJECTED     // admin manually rejected before payout — amount refunded to wallet
}