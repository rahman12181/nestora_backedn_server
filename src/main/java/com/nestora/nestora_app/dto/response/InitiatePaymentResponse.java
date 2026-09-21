package com.nestora.nestora_app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InitiatePaymentResponse {
    private Long rentPaymentId;
    private String razorpayOrderId;
    private Long amount;      // in PAISE — pass directly to Razorpay Checkout SDK
    private String currency;  // "INR"
    private String couponApplied; // null if none
    private java.math.BigDecimal discountAmount;
    private java.math.BigDecimal payableAmount; // in rupees, for display
}