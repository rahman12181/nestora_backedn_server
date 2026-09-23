package com.nestora.nestora_app.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nestora.nestora_app.enums.BookingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    // ═══════════════════════════════════════════════════════════
    // BASIC FIELDS
    // ═══════════════════════════════════════════════════════════
    private Long requestId;
    private Long propertyId;
    private String propertyTitle;
    private String propertyCity;
    private String propertyState;
    private String propertyAddress;
    private String coverImage;

    // ═══════════════════════════════════════════════════════════
    // ROOM FIELDS
    // ═══════════════════════════════════════════════════════════
    private Long roomId;
    private String roomNumber;
    private String roomType;
    private Double monthlyRent;

    // ═══════════════════════════════════════════════════════════
    // BOOKING FIELDS
    // ═══════════════════════════════════════════════════════════
    private LocalDate moveInDate;
    private Integer durationMonths;
    private String message;
    private BookingStatus status;
    private String ownerResponse;
    private LocalDateTime requestedAt;
    private LocalDateTime respondedAt;

    // ═══════════════════════════════════════════════════════════
    // STUDENT FIELDS (owner panel ke liye)
    // ═══════════════════════════════════════════════════════════
    private Long studentId;
    private String studentName;
    private String studentDisplayId;
    private String studentEmail;
    private String studentPhone;
    private String studentProfilePic;
    private LocalDateTime studentJoinedAt;
    private Long studentTotalBookings;
    private Long studentAcceptedBookings;
    private Boolean studentHasActiveBooking;
    private Boolean isRepeatStudent;

    // ═══════════════════════════════════════════════════════════
    // PAYMENT FIELDS
    // ═══════════════════════════════════════════════════════════
    @JsonProperty("isPaid")
    private Boolean isPaid;

    private String paymentStatus;
    private BigDecimal paidAmount;
    private LocalDateTime paidAt;
    private String couponCode;
    private BigDecimal discountAmount;
    private BigDecimal ownerPayoutAmount;
    private String payoutStatus;
    private String payoutTransactionRef;

    // ═══════════════════════════════════════════════════════════
    // UI FLAGS
    // ═══════════════════════════════════════════════════════════
    private Boolean isNew;
    private Boolean isUrgent;
    private Boolean hasUnreadMessages;
    private Long conversationId;
    private Long daysSinceRequested; // ✅ Integer → Long (Duration.toDays() long deta hai)

    // ═══════════════════════════════════════════════════════════
    // PROPERTY CONTEXT
    // ═══════════════════════════════════════════════════════════
    private Integer availableRooms;
    private Boolean isPropertyPublished;
}