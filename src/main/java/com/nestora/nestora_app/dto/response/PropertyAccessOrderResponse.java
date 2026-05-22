package com.nestora.nestora_app.dto.response;

import com.nestora.nestora_app.enums.PropertyAccessPlan;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PropertyAccessOrderResponse {
    private String razorpayOrderId;
    private Long amount;          // paise mein
    private String currency;
    private PropertyAccessPlan plan;
    private Integer durationMonths;
    private BigDecimal planPrice;
}