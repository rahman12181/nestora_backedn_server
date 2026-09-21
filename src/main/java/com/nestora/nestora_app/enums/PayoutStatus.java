package com.nestora.nestora_app.enums;

public enum PayoutStatus {
    NOT_STARTED,  // payment not yet confirmed, payout hasn't been triggered
    PROCESSING,   // payout triggered, RazorpayX confirming (usually a few seconds)
    COMPLETED,    // money confirmed sent to owner's UPI
    FAILED        // payout failed — needs admin attention (money is NOT lost, sits in
    // your RazorpayX account until manually resolved)
}