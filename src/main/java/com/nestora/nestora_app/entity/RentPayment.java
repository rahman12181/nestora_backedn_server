package com.nestora.nestora_app.entity;

import com.nestora.nestora_app.enums.PayoutStatus;
import com.nestora.nestora_app.enums.RentPaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ASSUMPTION: your booking request entity is called `BookingRequest` (per your Module 2.9/7
 * API docs — fields like requestId, property, room, status). Adjust the import/type below
 * if your actual class is named differently.
 */
@Entity
@Table(name = "rent_payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "booking_request_id", nullable = false, unique = true)
    private BookingRequest bookingRequest;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private OwnerProfile owner;

    @ManyToOne
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room; // nullable — booking may not always be tied to a specific room

    // ============ Amount breakdown ============
    @Column(name = "original_amount", nullable = false)
    private BigDecimal originalAmount; // full rent/deposit amount before any discount

    @Column(name = "coupon_code", length = 30)
    private String couponCode; // e.g. "FIRST20", null if no coupon used

    @Column(name = "discount_amount", nullable = false)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "platform_fee_amount", nullable = false)
    private BigDecimal platformFeeAmount; // platform's commission, calculated on originalAmount

    @Column(name = "student_payable_amount", nullable = false)
    private BigDecimal studentPayableAmount; // = originalAmount - discountAmount (what student actually pays)

    @Column(name = "owner_payout_amount", nullable = false)
    private BigDecimal ownerPayoutAmount; // = originalAmount - platformFeeAmount (owner always gets full rent minus platform's normal commission — discount does NOT reduce owner's share)

    // ============ Razorpay (collecting money FROM student) ============
    @Column(name = "razorpay_order_id", length = 60)
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id", length = 60)
    private String razorpayPaymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RentPaymentStatus status;

    // ============ RazorpayX (paying OUT to owner) ============
    @Enumerated(EnumType.STRING)
    @Column(name = "payout_status", nullable = false)
    private PayoutStatus payoutStatus;

    @Column(name = "razorpayx_contact_id", length = 60)
    private String razorpayXContactId;

    @Column(name = "razorpayx_fund_account_id", length = 60)
    private String razorpayXFundAccountId;

    @Column(name = "razorpayx_payout_id", length = 60, unique = true)
    private String razorpayXPayoutId;

    @Column(name = "payout_transaction_ref", length = 100)
    private String payoutTransactionRef; // UTR

    @Column(name = "payout_failure_reason", length = 255)
    private String payoutFailureReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "payout_at")
    private LocalDateTime payoutAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = RentPaymentStatus.CREATED;
        if (payoutStatus == null) payoutStatus = PayoutStatus.NOT_STARTED;
        if (discountAmount == null) discountAmount = BigDecimal.ZERO;
    }
}