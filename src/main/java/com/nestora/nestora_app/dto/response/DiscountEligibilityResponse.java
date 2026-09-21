package com.nestora.nestora_app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiscountEligibilityResponse {
    private Boolean eligible;
    private String couponCode;        // null if not eligible
    private BigDecimal discountPercent; // null if not eligible
    private String message;           // ready-to-display text for the banner
}