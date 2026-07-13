package com.nestora.nestora_app.entity;

import com.nestora.nestora_app.enums.ReferralStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "referrals", uniqueConstraints = {
        // THE most important safety net: one referred user can be linked to
        // exactly one referrer, ever. DB-level guarantee against duplicate claims.
        @UniqueConstraint(name = "uq_referred_user", columnNames = {"referred_user_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Referral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The existing user who shared their code
    @ManyToOne
    @JoinColumn(name = "referrer_id", nullable = false)
    private User referrer;

    // The new user who registered using that code
    @ManyToOne
    @JoinColumn(name = "referred_user_id", nullable = false, unique = true)
    private User referredUser;

    @Column(name = "referral_code", nullable = false, length = 20)
    private String referralCode;

    @Column(name = "reward_amount", nullable = false)
    private BigDecimal rewardAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReferralStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "rewarded_at")
    private LocalDateTime rewardedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = ReferralStatus.PENDING;
    }
}