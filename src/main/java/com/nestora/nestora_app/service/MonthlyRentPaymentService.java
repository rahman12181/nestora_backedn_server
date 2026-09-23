package com.nestora.nestora_app.service;

import com.nestora.nestora_app.dto.response.PayoutResult;
import com.nestora.nestora_app.entity.*;
import com.nestora.nestora_app.enums.PayoutStatus;
import com.nestora.nestora_app.exception.AppException;
import com.nestora.nestora_app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonthlyRentPaymentService {

    private final RentInvoiceRepository invoiceRepository;
    private final MonthlyRentPaymentRepository monthlyPaymentRepo;
    private final SettlementRepository settlementRepository;
    private final RentalAgreementService agreementService;
    private final NotificationService notificationService;
    private final RazorpayOrderService razorpayOrderService;
    private final RazorpayXService razorpayXService;

    @Value("${nestora.platform-fee-percent:10}")
    private BigDecimal platformFeePercent;

    // ============================================
    // INITIATE MONTHLY RENT PAYMENT
    // ============================================
    @Transactional
    public MonthlyRentPayment initiatePayment(User student, Long invoiceId) {

        RentInvoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new AppException(
                        "Invoice not found", HttpStatus.NOT_FOUND));

        if (!invoice.getAgreement().getUser().getId().equals(student.getId())) {
            throw new AppException("Not authorized", HttpStatus.FORBIDDEN);
        }

        if ("PAID".equals(invoice.getStatus())) {
            throw new AppException(
                    "Invoice already paid", HttpStatus.CONFLICT);
        }

        OwnerProfile owner = invoice.getAgreement().getOwner();

        // Check if owner has UPI set
        if (owner.getPayoutUpiId() == null || owner.getPayoutUpiId().isBlank()) {
            throw new AppException(
                    "Owner hasn't set up payment collection yet",
                    HttpStatus.BAD_REQUEST);
        }

        // Calculate fees
        BigDecimal gross = invoice.getAmount();
        BigDecimal platformFee = gross.multiply(platformFeePercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal ownerPayout = gross.subtract(platformFee);

        // Create Razorpay order
        String orderId = razorpayOrderService.createOrder(
                gross, "rent_invoice_" + invoiceId);

        String payCode = "MRP-" + UUID.randomUUID()
                .toString().substring(0, 8).toUpperCase();

        MonthlyRentPayment payment = MonthlyRentPayment.builder()
                .paymentCode(payCode)
                .invoice(invoice)
                .student(student)
                .owner(owner)
                .amount(gross)
                .platformFeeAmount(platformFee)
                .ownerPayoutAmount(ownerPayout)
                .razorpayOrderId(orderId)
                .status("CREATED")
                .payoutStatus(PayoutStatus.NOT_STARTED)
                .build();

        return monthlyPaymentRepo.save(payment);
    }

    // ============================================
    // CONFIRM MONTHLY RENT PAYMENT
    // ============================================
    @Transactional
    public MonthlyRentPayment confirmPayment(User student,
                                             String razorpayOrderId,
                                             String razorpayPaymentId,
                                             String razorpaySignature) {

        MonthlyRentPayment payment = monthlyPaymentRepo
                .findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new AppException(
                        "Payment record not found", HttpStatus.NOT_FOUND));

        if (!payment.getStudent().getId().equals(student.getId())) {
            throw new AppException("Not authorized", HttpStatus.FORBIDDEN);
        }

        if ("PAID".equals(payment.getStatus())) {
            return payment;
        }

        // Verify signature
        boolean valid = razorpayOrderService.verifyPaymentSignature(
                razorpayOrderId, razorpayPaymentId, razorpaySignature);

        if (!valid) {
            payment.setStatus("FAILED");
            monthlyPaymentRepo.save(payment);
            throw new AppException(
                    "Payment verification failed", HttpStatus.BAD_REQUEST);
        }

        // Update payment
        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setRazorpaySignature(razorpaySignature);
        payment.setStatus("PAID");
        payment.setPaidAt(LocalDateTime.now());
        payment = monthlyPaymentRepo.save(payment);

        // Mark invoice paid
        RentInvoice invoice = payment.getInvoice();
        invoice.setStatus("PAID");
        invoice.setPaidAt(LocalDateTime.now());
        invoiceRepository.save(invoice);

        // Log activity
        agreementService.logActivity(student, "RENT_PAID",
                "Rent Paid",
                "₹" + payment.getAmount() + " paid for " +
                        invoice.getInvoiceMonth(),
                "payment", "#22C55E", payment.getId(), "RENT_PAYMENT");

        // Notify owner
        try {
            notificationService.createNotification(
                    payment.getOwner().getUser(),
                    "💰 Rent Received",
                    "₹" + payment.getAmount() + " received from " +
                            student.getName(),
                    com.nestora.nestora_app.enums.NotificationType.PAYMENT,
                    payment.getId()
            );
        } catch (Exception ignored) {}

        // Trigger payout
        triggerOwnerPayout(payment);

        return payment;
    }

    // ============================================
    // TRIGGER OWNER PAYOUT (RazorpayX)
    // ============================================
    private void triggerOwnerPayout(MonthlyRentPayment payment) {
        try {
            OwnerProfile owner = payment.getOwner();
            String contactId = razorpayXService.createContact(owner.getUser());
            String fundAccountId = razorpayXService.createFundAccount(
                    contactId, owner.getPayoutUpiId());

            payment.setRazorpayXContactId(contactId);
            payment.setRazorpayXFundAccountId(fundAccountId);

            PayoutResult result = razorpayXService.initiatePayout(
                    fundAccountId,
                    payment.getOwnerPayoutAmount(),
                    payment.getId());

            payment.setRazorpayXPayoutId(result.getPayoutId());
            payment.setPayoutAt(LocalDateTime.now());

            switch (result.getStatus()) {
                case "processed" -> {
                    payment.setPayoutStatus(PayoutStatus.COMPLETED);
                    payment.setPayoutTransactionRef(result.getUtr());
                }
                case "queued", "pending", "processing" ->
                        payment.setPayoutStatus(PayoutStatus.PROCESSING);
                default -> {
                    payment.setPayoutStatus(PayoutStatus.FAILED);
                    payment.setPayoutFailureReason(
                            "RazorpayX: " + result.getStatus());
                }
            }
            monthlyPaymentRepo.save(payment);

            // Create settlement record
            createSettlement(payment);

        } catch (Exception e) {
            log.error("Payout failed for monthly payment {}", payment.getId(), e);
            payment.setPayoutStatus(PayoutStatus.FAILED);
            payment.setPayoutFailureReason(
                    "Payout failed: " + e.getMessage());
            monthlyPaymentRepo.save(payment);
        }
    }

    // ============================================
    // CREATE SETTLEMENT RECORD
    // ============================================
    private void createSettlement(MonthlyRentPayment payment) {
        try {
            String code = "STL-" + UUID.randomUUID()
                    .toString().substring(0, 8).toUpperCase();

            Settlement settlement = Settlement.builder()
                    .settlementCode(code)
                    .owner(payment.getOwner())
                    .grossAmount(payment.getAmount())
                    .platformFee(payment.getPlatformFeeAmount())
                    .netAmount(payment.getOwnerPayoutAmount())
                    .status("COMPLETED")
                    .completedAt(LocalDateTime.now())
                    .build();

            settlementRepository.save(settlement);
        } catch (Exception e) {
            log.error("Settlement creation failed", e);
        }
    }

    // ============================================
    // READ
    // ============================================
    @Transactional(readOnly = true)
    public List<MonthlyRentPayment> getMyPayments(User student) {
        return monthlyPaymentRepo.findByStudentOrderByCreatedAtDesc(student);
    }

    @Transactional(readOnly = true)
    public List<MonthlyRentPayment> getOwnerPayments(User ownerUser) {
        OwnerProfile owner = monthlyPaymentRepo
                .findByStudentOrderByCreatedAtDesc(ownerUser)
                .stream().findFirst()
                .map(p -> p.getOwner())
                .orElseThrow(() -> new AppException(
                        "Owner profile not found", HttpStatus.NOT_FOUND));
        return monthlyPaymentRepo.findByOwnerOrderByCreatedAtDesc(owner);
    }
}