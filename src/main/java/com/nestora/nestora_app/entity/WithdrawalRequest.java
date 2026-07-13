package com.nestora.nestora_app.entity;

import com.nestora.nestora_app.enums.WithdrawalStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "withdrawal_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "upi_id", nullable = false, length = 100)
    private String upiId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WithdrawalStatus status;

    @Column(name = "admin_note", length = 255)
    private String adminNote;

    // UPI transaction reference (UTR) — now filled AUTOMATICALLY from the RazorpayX
    // payout response, admin does not type this in manually anymore.
    @Column(name = "transaction_ref", length = 100)
    private String transactionRef;

    // ============================================
    // NEW — RazorpayX automatic payout tracking
    // ============================================
    @Column(name = "razorpayx_contact_id", length = 60)
    private String razorpayXContactId;

    @Column(name = "razorpayx_fund_account_id", length = 60)
    private String razorpayXFundAccountId;

    @Column(name = "razorpayx_payout_id", length = 60, unique = true)
    private String razorpayXPayoutId;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @ManyToOne
    @JoinColumn(name = "processed_by")
    private User processedBy; // which admin triggered the payout

    @PrePersist
    protected void onCreate() {
        requestedAt = LocalDateTime.now();
        if (status == null) status = WithdrawalStatus.PENDING;
    }
}