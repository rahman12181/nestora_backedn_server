package com.nestora.nestora_app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RentPaymentResponse {

    private Long rentPaymentId;
    private Long bookingRequestId;
    private String propertyTitle;
    private String roomNumber;

    private BigDecimal originalAmount;
    private String couponCode;
    private BigDecimal discountAmount;
    private BigDecimal studentPayableAmount;
    private BigDecimal ownerPayoutAmount;

    private String status;         // CREATED / PAID / FAILED / REFUNDED
    private String razorpayPaymentId;

    private String payoutStatus;   // NOT_STARTED / PROCESSING / COMPLETED / FAILED
    private String payoutTransactionRef;
    private String payoutFailureReason;

    // populated only in owner/admin views
    private Long studentId;
    private String studentName;
    private String studentDisplayId;

    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime payoutAt;
}