package com.nestora.nestora_app.entity;

import com.nestora.nestora_app.enums.PayoutStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Monthly rent payment — separate from booking-level RentPayment.
 * Ye sirf rental agreement ke monthly rent ke liye hai.
 */
@Entity
@Table(name = "monthly_rent_payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyRentPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_code", unique = true, nullable = false, length = 30)
    private String paymentCode;

    @ManyToOne
    @JoinColumn(name = "invoice_id", nullable = false)
    private RentInvoice invoice;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private OwnerProfile owner;

    // ============ Amount breakdown ============
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;  // invoice amount

    @Column(name = "platform_fee_amount", precision = 10, scale = 2)
    private BigDecimal platformFeeAmount;

    @Column(name = "owner_payout_amount", precision = 10, scale = 2)
    private BigDecimal ownerPayoutAmount;

    // ============ Razorpay (collecting money) ============
    @Column(name = "razorpay_order_id", length = 100)
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id", length = 100)
    private String razorpayPaymentId;

    @Column(name = "razorpay_signature", length = 255)
    private String razorpaySignature;

    @Column(nullable = false, length = 30)
    private String status = "CREATED";  // CREATED, PAID, FAILED, REFUNDED

    // ============ RazorpayX (paying out) ============
    @Enumerated(EnumType.STRING)
    @Column(name = "payout_status", nullable = false)
    private PayoutStatus payoutStatus = PayoutStatus.NOT_STARTED;

    @Column(name = "razorpayx_contact_id", length = 60)
    private String razorpayXContactId;

    @Column(name = "razorpayx_fund_account_id", length = 60)
    private String razorpayXFundAccountId;

    @Column(name = "razorpayx_payout_id", length = 60)
    private String razorpayXPayoutId;

    @Column(name = "payout_transaction_ref", length = 100)
    private String payoutTransactionRef;

    @Column(name = "payout_failure_reason", length = 255)
    private String payoutFailureReason;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "payout_at")
    private LocalDateTime payoutAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = "CREATED";
        if (payoutStatus == null) payoutStatus = PayoutStatus.NOT_STARTED;
    }
}