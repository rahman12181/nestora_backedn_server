package com.nestora.nestora_app.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OwnerPayoutUpiRequest {

    @NotBlank(message = "UPI ID is required")
    private String payoutUpiId; // e.g. "ownername@okhdfcbank" — where rent payouts will be sent
}