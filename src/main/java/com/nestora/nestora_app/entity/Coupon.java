package com.nestora.nestora_app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Generic coupon entity — built reusable so you're not limited to just FIRST20 later.
 * Seed FIRST20 with a single INSERT (see schema_booking_payments.sql):
 *   code=FIRST20, discountPercent=20.00, firstBookingOnly=true, isActive=true
 */
@Entity
@Table(name = "coupons")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code; // e.g. "FIRST20" — always stored/matched in UPPERCASE

    @Column(name = "discount_percent", nullable = false)
    private BigDecimal discountPercent; // e.g. 20.00 = 20%

    @Column(name = "max_discount_amount")
    private BigDecimal maxDiscountAmount; // nullable — cap the discount in rupees, null = no cap

    // if true, this coupon can ONLY be used on a student's first-ever successful rent payment
    @Column(name = "first_booking_only", nullable = false)
    private Boolean firstBookingOnly = true;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "valid_from")
    private LocalDateTime validFrom; // nullable = valid immediately

    @Column(name = "valid_until")
    private LocalDateTime validUntil; // nullable = never expires

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isActive == null) isActive = true;
        if (firstBookingOnly == null) firstBookingOnly = true;
    }
}