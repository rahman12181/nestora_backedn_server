package com.nestora.nestora_app.dto.request;


import com.nestora.nestora_app.enums.SubscriptionPlan;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubscriptionBuyRequest {

    @NotNull(message = "Plan is required")
    private SubscriptionPlan plan;
}
