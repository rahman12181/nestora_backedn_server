package com.nestora.nestora_app.dto.response;


import com.nestora.nestora_app.enums.SubscriptionPlan;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubscriptionOrderResponse {
    private String razorpayOrderId;
    private Long amount;
    private String currency;
    private SubscriptionPlan plan;
}
