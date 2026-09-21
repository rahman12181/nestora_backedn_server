package com.nestora.nestora_app.entity;

import com.nestora.nestora_app.enums.SubscriptionPlan;
import com.nestora.nestora_app.enums.SubscriptionStatus;
import com.nestora.nestora_app.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "owner_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "business_name")
    private String businessName;

    @Column(name = "aadhar_number")
    private String aadharNumber;

    @Column(name = "aadhar_doc_url")
    private String aadharDocUrl;

    @Column(name = "pan_number")
    private String panNumber;

    @Column(name = "pan_doc_url")
    private String panDocUrl;

    @Column(name = "address_proof_url")
    private String addressProofUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status")
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @ManyToOne
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_plan")
    private SubscriptionPlan subscriptionPlan = SubscriptionPlan.BASIC;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_status")
    private SubscriptionStatus subscriptionStatus = SubscriptionStatus.ACTIVE;

    @Column(name = "subscription_start")
    private LocalDateTime subscriptionStart;

    @Column(name = "subscription_end")
    private LocalDateTime subscriptionEnd;

    @Column(name = "monthly_fee")
    private BigDecimal monthlyFee;

    // ============================================
    // NEW — Booking Payments: where this owner receives automatic rent payouts
    // ============================================
    @Column(name = "payout_upi_id")
    private String payoutUpiId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}