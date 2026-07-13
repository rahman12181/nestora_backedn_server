package com.nestora.nestora_app.enums;

public enum ReferralStatus {
    PENDING,   // referred user registered, but not yet OTP-verified
    REWARDED,  // referred user verified email -> referrer credited
    REJECTED   // flagged invalid / fraud by admin (reward reversed if already given)
}