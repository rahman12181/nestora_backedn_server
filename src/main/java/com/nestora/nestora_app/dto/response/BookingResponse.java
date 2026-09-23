package com.nestora.nestora_app.dto.response;

import com.nestora.nestora_app.enums.BookingStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class BookingResponse {

    // ============ CORE BOOKING ============
    private Long requestId;
    private Long propertyId;
    private String propertyTitle;
    private String propertyCity;
    private String propertyState;
    private String propertyAddress;
    private String coverImage;
    private Long roomId;
    private String roomNumber;
    private String roomType;
    private BigDecimal monthlyRent;
    private LocalDate moveInDate;
    private Integer durationMonths;
    private String message;
    private BookingStatus status;
    private String ownerResponse;
    private LocalDateTime requestedAt;
    private LocalDateTime respondedAt;

    // ============ STUDENT DETAILS ============
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

    // ============ PAYMENT (placeholder) ============
    private Boolean isPaid;
    private BigDecimal paidAmount;
    private String paymentStatus;
    private String couponCode;
    private BigDecimal discountAmount;
    private BigDecimal ownerPayoutAmount;
    private String payoutStatus;
    private String payoutTransactionRef;
    private LocalDateTime paidAt;

    // ============ UI FLAGS ============
    private Boolean isNew;
    private Boolean isUrgent;
    private Boolean hasUnreadMessages;
    private String conversationId;
    private Long daysSinceRequested;

    // ============ PROPERTY CONTEXT ============
    private Integer availableRooms;
    private Boolean isPropertyPublished;
}