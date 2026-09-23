package com.nestora.nestora_app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "rent_invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_code", unique = true, nullable = false, length = 30)
    private String invoiceCode;

    @ManyToOne
    @JoinColumn(name = "agreement_id", nullable = false)
    private RentalAgreement agreement;

    @Column(name = "invoice_month", nullable = false, length = 10)
    private String invoiceMonth;  // "2026-10"

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "late_fee", precision = 10, scale = 2)
    private BigDecimal lateFee = BigDecimal.ZERO;

    @Column(nullable = false, length = 30)
    private String status = "PENDING";  // PENDING, PAID, OVERDUE, CANCELLED

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "days_overdue")
    private Integer daysOverdue = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}