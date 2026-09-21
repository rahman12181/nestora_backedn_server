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
public class PaymentSummaryResponse {

    private Long bookingRequestId;
    private String propertyTitle;
    private String roomNumber;

    private BigDecimal originalAmount;

    private Boolean eligibleForFirstBookingDiscount;
    private String availableCouponCode;      // e.g. "FIRST20" — null if not eligible
    private BigDecimal discountPercent;       // e.g. 20.00
    private BigDecimal estimatedDiscountAmount;
    private BigDecimal estimatedPayableAmount; // originalAmount - estimatedDiscountAmount

    private Boolean alreadyPaid;
}