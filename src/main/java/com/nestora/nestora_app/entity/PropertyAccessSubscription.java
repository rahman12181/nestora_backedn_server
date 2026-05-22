package com.nestora.nestora_app.entity;

import com.nestora.nestora_app.enums.PropertyAccessPlan;
import com.nestora.nestora_app.enums.PropertyAccessStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "property_access_subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyAccessSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private OwnerProfile owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false)
    private PropertyAccessPlan plan;

    @Column(name = "duration_months", nullable = false)
    private Integer durationMonths;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PropertyAccessStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "amount_paid", nullable = false)
    private BigDecimal amountPaid;

    @Column(name = "razorpay_order_id")
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;

    @Column(name = "razorpay_signature", length = 500)
    private String razorpaySignature;

    @Column(name = "warning_sent")
    private Boolean warningSent = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (warningSent == null) warningSent = false;
    }
}