package com.nestora.nestora_app.entity;

import com.nestora.nestora_app.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;      // ✅ Import add karo
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(name = "move_in_date", nullable = false)
    private LocalDate moveInDate;

    @Column(name = "duration_months")
    private Integer durationMonths;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "owner_response", columnDefinition = "TEXT")
    private String ownerResponse;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    // ✅ PAYMENT FIELDS — BigDecimal type (DB DECIMAL hai)
    @Column(name = "is_paid")
    private Boolean isPaid = false;

    @Column(name = "payment_status")
    private String paymentStatus;

    @Column(name = "paid_amount")
    private BigDecimal paidAmount;           // ✅ Double → BigDecimal

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "coupon_code")
    private String couponCode;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;       // ✅ Double → BigDecimal

    @Column(name = "owner_payout_amount")
    private BigDecimal ownerPayoutAmount;    // ✅ Double → BigDecimal

    @Column(name = "payout_status")
    private String payoutStatus;

    @Column(name = "payout_transaction_ref")
    private String payoutTransactionRef;

    @PrePersist
    protected void onCreate() {
        requestedAt = LocalDateTime.now();
        if (isPaid == null) isPaid = false;
    }
}