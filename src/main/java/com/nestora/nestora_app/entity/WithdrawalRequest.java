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

    // UPI/bank transaction reference number the admin enters after manually paying
    @Column(name = "transaction_ref", length = 100)
    private String transactionRef;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @ManyToOne
    @JoinColumn(name = "processed_by")
    private User processedBy; // which admin approved/rejected this

    @PrePersist
    protected void onCreate() {
        requestedAt = LocalDateTime.now();
        if (status == null) status = WithdrawalStatus.PENDING;
    }
}