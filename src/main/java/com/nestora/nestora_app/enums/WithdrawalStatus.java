package com.nestora.nestora_app.enums;

public enum WithdrawalStatus {
    PENDING,   // requested, waiting for admin to pay manually
    APPROVED,  // admin has sent the money via UPI/bank and marked it paid
    REJECTED   // admin rejected -> amount refunded back to wallet
}