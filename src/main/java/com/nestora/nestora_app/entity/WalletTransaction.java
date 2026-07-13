package com.nestora.nestora_app.entity;

import com.nestora.nestora_app.enums.WalletTransactionReason;
import com.nestora.nestora_app.enums.WalletTransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transactions", indexes = {
        @Index(name = "idx_wallet_txn_user", columnList = "user_id, created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private WalletTransactionType type;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false)
    private WalletTransactionReason reason;

    // links back to the Referral.id or WithdrawalRequest.id that caused this entry
    @Column(name = "reference_id")
    private Long referenceId;

    // wallet balance AFTER this transaction was applied — for audit trail
    @Column(name = "balance_after", nullable = false)
    private BigDecimal balanceAfter;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}