package com.nestora.nestora_app.dto.response;

import com.nestora.nestora_app.enums.PropertyAccessPlan;
import com.nestora.nestora_app.enums.PropertyAccessStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PropertyAccessStatusResponse {
    private Boolean hasActiveSubscription;
    private PropertyAccessPlan plan;
    private Integer durationMonths;
    private PropertyAccessStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long daysRemaining;
    private Boolean isExpiringSoon;   // 7 din ya kam bacha ho toh true
    private BigDecimal amountPaid;
}