package com.nestora.nestora_app.dto.response;


import com.nestora.nestora_app.enums.SubscriptionPlan;
import com.nestora.nestora_app.enums.SubscriptionStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class SubscriptionDetailsResponse {
    private SubscriptionPlan plan;
    private SubscriptionStatus subscriptionStatus;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal monthlyFee;
    private Long daysRemaining;
}
