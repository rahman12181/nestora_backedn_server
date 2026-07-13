package com.nestora.nestora_app.enums;

public enum WalletTransactionReason {
    REFERRAL_BONUS,             // credit - earned from referring a friend
    WITHDRAWAL_HOLD,            // debit - user requested withdrawal (funds held pending admin payout)
    WITHDRAWAL_REJECTED_REFUND, // credit - admin rejected withdrawal, amount refunded to wallet
    ADMIN_ADJUSTMENT            // credit/debit - manual correction by admin
}