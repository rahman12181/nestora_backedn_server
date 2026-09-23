package com.nestora.nestora_app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "settlements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "settlement_code", unique = true, nullable = false, length = 30)
    private String settlementCode;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private OwnerProfile owner;

    @ManyToOne
    @JoinColumn(name = "rent_payment_id")
    private RentPayment rentPayment;

    @Column(name = "gross_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "platform_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal platformFee;

    @Column(name = "net_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal netAmount;

    @Column(nullable = false, length = 30)
    private String status = "PENDING";

    @Column(name = "payout_reference", length = 100)
    private String payoutReference;

    @Column(name = "razorpay_transfer_id", length = 100)
    private String razorpayTransferId;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "initiated_at")
    private LocalDateTime initiatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}