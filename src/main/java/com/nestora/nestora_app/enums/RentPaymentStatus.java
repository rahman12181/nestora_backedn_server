package com.nestora.nestora_app.enums;

public enum RentPaymentStatus {
    CREATED,  // Razorpay order created, waiting for student to pay
    PAID,     // payment confirmed successful
    FAILED,   // payment failed/cancelled by student
    REFUNDED  // admin refunded (manual process, not automated in v1)
}