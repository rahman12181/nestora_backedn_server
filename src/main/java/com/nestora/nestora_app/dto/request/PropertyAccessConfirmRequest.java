package com.nestora.nestora_app.dto.request;

import com.nestora.nestora_app.enums.PropertyAccessPlan;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PropertyAccessConfirmRequest {

    @NotBlank(message = "Razorpay order ID is required")
    private String razorpayOrderId;

    @NotBlank(message = "Razorpay payment ID is required")
    private String razorpayPaymentId;

    @NotBlank(message = "Razorpay signature is required")
    private String razorpaySignature;
    @NotNull(message = "Plan is required")
    private PropertyAccessPlan plan;
}