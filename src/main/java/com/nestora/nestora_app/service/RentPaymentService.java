package com.nestora.nestora_app.service;

import com.nestora.nestora_app.dto.request.InitiatePaymentRequest;
import com.nestora.nestora_app.dto.response.*;
import com.nestora.nestora_app.entity.*;
import com.nestora.nestora_app.enums.PayoutStatus;
import com.nestora.nestora_app.enums.RentPaymentStatus;
import com.nestora.nestora_app.exception.AppException;
import com.nestora.nestora_app.repository.BookingRequestRepository;
import com.nestora.nestora_app.repository.CouponRepository;
import com.nestora.nestora_app.repository.OwnerProfileRepository;
import com.nestora.nestora_app.repository.RentPaymentRepository;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RentPaymentService {

    private static final String DEFAULT_FIRST_BOOKING_COUPON = "FIRST20";

    @Value("${nestora.platform-fee-percent:10}")
    private BigDecimal platformFeePercent;

    private final RentPaymentRepository rentPaymentRepository;
    private final CouponRepository couponRepository;
    private final BookingRequestRepository bookingRequestRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final RazorpayOrderService razorpayOrderService;
    private final RazorpayXService razorpayXService;
    private final RentalAgreementService rentalAgreementService;   // ✅ NEW

    // ================= Discount eligibility =================

    @Transactional(readOnly = true)
    public DiscountEligibilityResponse getDiscountEligibility(User student) {
        boolean eligible = isEligibleForFirstBookingDiscount(student);

        if (!eligible) {
            return DiscountEligibilityResponse.builder()
                    .eligible(false)
                    .message("You've already used your first-booking discount.")
                    .build();
        }

        Optional<Coupon> couponOpt = couponRepository.findByCodeAndIsActiveTrue(DEFAULT_FIRST_BOOKING_COUPON);
        if (couponOpt.isEmpty() || !isCouponCurrentlyValid(couponOpt.get())) {
            return DiscountEligibilityResponse.builder()
                    .eligible(false)
                    .message("No active offers right now.")
                    .build();
        }

        Coupon coupon = couponOpt.get();
        return DiscountEligibilityResponse.builder()
                .eligible(true)
                .couponCode(coupon.getCode())
                .discountPercent(coupon.getDiscountPercent())
                .message(String.format("🎉 %s%% OFF your first booking — applied automatically at payment!",
                        coupon.getDiscountPercent().stripTrailingZeros().toPlainString()))
                .build();
    }

    // ================= Owner sets up payout UPI =================

    @Transactional
    public void setPayoutUpiId(User ownerUser, String upiId) {
        OwnerProfile owner = ownerProfileRepository.findByUser(ownerUser)
                .orElseThrow(() -> new AppException("Owner profile not found. Please apply as owner first.", HttpStatus.NOT_FOUND));
        owner.setPayoutUpiId(upiId);
        ownerProfileRepository.save(owner);
    }

    @Transactional(readOnly = true)
    public PayoutStatusResponse getPayoutStatus(User ownerUser) {
        OwnerProfile owner = ownerProfileRepository.findByUser(ownerUser)
                .orElseThrow(() -> new AppException("Owner profile not found.", HttpStatus.NOT_FOUND));

        String upiId = owner.getPayoutUpiId();
        boolean hasPayoutUpi = upiId != null && !upiId.isBlank();

        PayoutStatus currentStatus = PayoutStatus.NOT_STARTED;
        String lastPayoutDate = null;
        Double lastPayoutAmount = null;
        String message = "No payout initiated yet";

        try {
            List<RentPayment> payments = rentPaymentRepository.findByOwner_IdOrderByCreatedAtDesc(owner.getId());
            if (payments != null && !payments.isEmpty()) {
                RentPayment latestPayment = payments.get(0);
                if (latestPayment.getPayoutStatus() != null) {
                    currentStatus = latestPayment.getPayoutStatus();
                }
                lastPayoutDate = latestPayment.getPaidAt() != null
                        ? latestPayment.getPaidAt().toString()
                        : null;
                lastPayoutAmount = latestPayment.getOwnerPayoutAmount() != null
                        ? latestPayment.getOwnerPayoutAmount().doubleValue()
                        : null;

                switch (currentStatus) {
                    case COMPLETED:
                        message = "Last payout completed successfully ✓";
                        break;
                    case PROCESSING:
                        message = "Payout is being processed... ⏳";
                        break;
                    case FAILED:
                        message = "Payout failed. Please contact support. ⚠️";
                        break;
                    default:
                        message = hasPayoutUpi ? "No payout initiated yet" : "Please set UPI ID first";
                }
            } else {
                if (hasPayoutUpi) {
                    message = "No payments received yet. Payout will be initiated when you receive payments.";
                } else {
                    message = "Please set UPI ID to receive payments";
                }
            }
        } catch (Exception e) {
            log.error("Error fetching payout status for owner {}: {}", owner.getId(), e.getMessage());
            message = "Unable to fetch payout details";
        }

        return PayoutStatusResponse.builder()
                .hasPayoutUpi(hasPayoutUpi)
                .upiId(hasPayoutUpi ? upiId : null)
                .payoutStatus(currentStatus)
                .lastPayoutDate(lastPayoutDate)
                .lastPayoutAmount(lastPayoutAmount)
                .message(message)
                .build();
    }

    // ================= Student: view amount due =================

    @Transactional(readOnly = true)
    public PaymentSummaryResponse getPaymentSummary(User student, Long bookingRequestId) {
        BookingRequest booking = getAcceptedBookingOrThrow(bookingRequestId, student.getId());

        BigDecimal originalAmount = resolveOriginalAmount(booking);

        Optional<RentPayment> existing = rentPaymentRepository.findByBookingRequest_Id(bookingRequestId);
        boolean alreadyPaid = existing.isPresent() && existing.get().getStatus() == RentPaymentStatus.PAID;

        boolean eligible = isEligibleForFirstBookingDiscount(student);
        Coupon coupon = eligible ? couponRepository.findByCodeAndIsActiveTrue(DEFAULT_FIRST_BOOKING_COUPON).orElse(null) : null;

        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal discountPercent = null;
        String couponCode = null;

        if (coupon != null && isCouponCurrentlyValid(coupon)) {
            discountPercent = coupon.getDiscountPercent();
            discountAmount = calculateDiscount(originalAmount, coupon);
            couponCode = coupon.getCode();
        }

        return PaymentSummaryResponse.builder()
                .bookingRequestId(booking.getId())
                .propertyTitle(booking.getProperty().getTitle())
                .roomNumber(booking.getRoom() != null ? booking.getRoom().getRoomNumber() : null)
                .originalAmount(originalAmount)
                .eligibleForFirstBookingDiscount(couponCode != null)
                .availableCouponCode(couponCode)
                .discountPercent(discountPercent)
                .estimatedDiscountAmount(discountAmount)
                .estimatedPayableAmount(originalAmount.subtract(discountAmount))
                .alreadyPaid(alreadyPaid)
                .build();
    }

    // ================= Student: initiate payment =================

    @Transactional
    public InitiatePaymentResponse initiatePayment(User student, Long bookingRequestId, InitiatePaymentRequest request) {
        BookingRequest booking = getAcceptedBookingOrThrow(bookingRequestId, student.getId());

        OwnerProfile owner = booking.getProperty().getOwner();
        if (owner.getPayoutUpiId() == null || owner.getPayoutUpiId().isBlank()) {
            throw new AppException("This owner hasn't set up payment collection yet. Please contact them.", HttpStatus.BAD_REQUEST);
        }

        Optional<RentPayment> existing = rentPaymentRepository.findByBookingRequest_Id(bookingRequestId);
        if (existing.isPresent() && existing.get().getStatus() == RentPaymentStatus.PAID) {
            throw new AppException("This booking has already been paid for.", HttpStatus.CONFLICT);
        }

        BigDecimal originalAmount = resolveOriginalAmount(booking);

        String requestedCode = request != null ? request.getCouponCode() : null;
        String codeToTry = (requestedCode != null && !requestedCode.isBlank())
                ? requestedCode.trim().toUpperCase()
                : (isEligibleForFirstBookingDiscount(student) ? DEFAULT_FIRST_BOOKING_COUPON : null);

        BigDecimal discountAmount = BigDecimal.ZERO;
        String appliedCouponCode = null;

        if (codeToTry != null) {
            Optional<Coupon> couponOpt = couponRepository.findByCodeAndIsActiveTrue(codeToTry);
            if (couponOpt.isPresent() && isCouponCurrentlyValid(couponOpt.get())) {
                Coupon coupon = couponOpt.get();
                boolean firstBookingOk = !coupon.getFirstBookingOnly() || isEligibleForFirstBookingDiscount(student);
                if (firstBookingOk) {
                    discountAmount = calculateDiscount(originalAmount, coupon);
                    appliedCouponCode = coupon.getCode();
                } else if (requestedCode != null) {
                    throw new AppException("This coupon is only valid on your first booking.", HttpStatus.BAD_REQUEST);
                }
            } else if (requestedCode != null) {
                throw new AppException("Invalid or expired coupon code.", HttpStatus.BAD_REQUEST);
            }
        }

        BigDecimal platformFee = originalAmount.multiply(platformFeePercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal studentPayable = originalAmount.subtract(discountAmount);
        BigDecimal ownerPayout = originalAmount.subtract(platformFee);

        RentPayment payment = existing.orElseGet(() -> RentPayment.builder()
                .bookingRequest(booking)
                .student(student)
                .owner(owner)
                .property(booking.getProperty())
                .room(booking.getRoom())
                .build());

        payment.setOriginalAmount(originalAmount);
        payment.setCouponCode(appliedCouponCode);
        payment.setDiscountAmount(discountAmount);
        payment.setPlatformFeeAmount(platformFee);
        payment.setStudentPayableAmount(studentPayable);
        payment.setOwnerPayoutAmount(ownerPayout);
        payment.setStatus(RentPaymentStatus.CREATED);

        String razorpayOrderId = razorpayOrderService.createOrder(studentPayable, "booking_" + bookingRequestId);
        payment.setRazorpayOrderId(razorpayOrderId);

        payment = rentPaymentRepository.save(payment);

        return InitiatePaymentResponse.builder()
                .rentPaymentId(payment.getId())
                .razorpayOrderId(razorpayOrderId)
                .amount(studentPayable.multiply(BigDecimal.valueOf(100)).longValueExact())
                .currency("INR")
                .couponApplied(appliedCouponCode)
                .discountAmount(discountAmount)
                .payableAmount(studentPayable)
                .build();
    }

    // ============================================
    // ✅ ENHANCED — Confirm payment + AUTO-CREATE agreement
    // ============================================
    @Transactional
    public RentPaymentResponse confirmPayment(User student, String razorpayOrderId,
                                              String razorpayPaymentId, String razorpaySignature) {
        RentPayment payment = rentPaymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new AppException("Payment record not found", HttpStatus.NOT_FOUND));

        if (!payment.getStudent().getId().equals(student.getId())) {
            throw new AppException("Not authorized for this payment", HttpStatus.FORBIDDEN);
        }
        if (payment.getStatus() == RentPaymentStatus.PAID) {
            return toResponse(payment);
        }

        boolean validSignature = razorpayOrderService.verifyPaymentSignature(
                razorpayOrderId, razorpayPaymentId, razorpaySignature);
        if (!validSignature) {
            payment.setStatus(RentPaymentStatus.FAILED);
            rentPaymentRepository.save(payment);
            throw new AppException("Payment verification failed. Invalid signature.", HttpStatus.BAD_REQUEST);
        }

        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setStatus(RentPaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment = rentPaymentRepository.save(payment);

        triggerOwnerPayout(payment);

        // ============================================
        // ✅ NEW — Auto-create rental agreement + mark room occupied
        // ============================================
        try {
            rentalAgreementService.createAgreement(payment.getBookingRequest());
            log.info("Rental agreement auto-created for booking {}",
                    payment.getBookingRequest().getId());
        } catch (Exception e) {
            log.error("Failed to auto-create agreement for booking {}: {}",
                    payment.getBookingRequest().getId(), e.getMessage());
            // Don't fail the payment — agreement can be retried
        }

        return toResponse(payment);
    }

    private void triggerOwnerPayout(RentPayment payment) {
        try {
            OwnerProfile owner = payment.getOwner();
            String contactId = razorpayXService.createContact(owner.getUser());
            String fundAccountId = razorpayXService.createFundAccount(contactId, owner.getPayoutUpiId());

            payment.setRazorpayXContactId(contactId);
            payment.setRazorpayXFundAccountId(fundAccountId);

            PayoutResult result = razorpayXService.initiatePayout(fundAccountId, payment.getOwnerPayoutAmount(), payment.getId());
            payment.setRazorpayXPayoutId(result.getPayoutId());
            payment.setPayoutAt(LocalDateTime.now());

            switch (result.getStatus()) {
                case "processed" -> {
                    payment.setPayoutStatus(PayoutStatus.COMPLETED);
                    payment.setPayoutTransactionRef(result.getUtr());
                }
                case "queued", "pending", "processing" -> payment.setPayoutStatus(PayoutStatus.PROCESSING);
                default -> {
                    payment.setPayoutStatus(PayoutStatus.FAILED);
                    payment.setPayoutFailureReason("RazorpayX status: " + result.getStatus());
                }
            }
            rentPaymentRepository.save(payment);
        } catch (Exception e) {
            log.error("Owner payout failed for rentPayment {}: {}", payment.getId(), e.getMessage());
            payment.setPayoutStatus(PayoutStatus.FAILED);
            payment.setPayoutFailureReason("Payout could not be initiated: " + e.getMessage());
            rentPaymentRepository.save(payment);
        }
    }

    // ================= Read endpoints =================

    @Transactional(readOnly = true)
    public List<RentPaymentResponse> getMyPayments(User student) {
        return rentPaymentRepository.findByStudent_IdOrderByCreatedAtDesc(student.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RentPaymentResponse> getOwnerPayments(User ownerUser) {
        OwnerProfile owner = ownerProfileRepository.findByUser(ownerUser)
                .orElseThrow(() -> new AppException("Owner profile not found.", HttpStatus.NOT_FOUND));
        return rentPaymentRepository.findByOwner_IdOrderByCreatedAtDesc(owner.getId())
                .stream().map(this::toStudentAwareResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RentPaymentResponse> getAllPayments(RentPaymentStatus statusFilter) {
        List<RentPayment> payments = statusFilter == null
                ? rentPaymentRepository.findAllByOrderByCreatedAtDesc()
                : rentPaymentRepository.findByStatusOrderByCreatedAtDesc(statusFilter);
        return payments.stream().map(this::toStudentAwareResponse).collect(Collectors.toList());
    }

    // ================= Helpers =================

    private BookingRequest getAcceptedBookingOrThrow(Long bookingRequestId, Long studentId) {
        BookingRequest booking = bookingRequestRepository.findById(bookingRequestId)
                .orElseThrow(() -> new AppException("Booking request not found", HttpStatus.NOT_FOUND));

        if (!booking.getUser().getId().equals(studentId)) {
            throw new AppException("Not authorized for this booking", HttpStatus.FORBIDDEN);
        }
        if (!"ACCEPTED".equals(booking.getStatus().name())) {
            throw new AppException("Payment is only available for accepted bookings", HttpStatus.BAD_REQUEST);
        }
        return booking;
    }

    private BigDecimal resolveOriginalAmount(BookingRequest booking) {
        if (booking.getRoom() != null && booking.getRoom().getMonthlyRent() != null) {
            return booking.getRoom().getMonthlyRent();
        }
        return booking.getProperty().getMonthlyRentMin();
    }

    private boolean isEligibleForFirstBookingDiscount(User student) {
        return !rentPaymentRepository.existsByStudent_IdAndStatus(student.getId(), RentPaymentStatus.PAID);
    }

    private boolean isCouponCurrentlyValid(Coupon coupon) {
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getValidFrom() != null && now.isBefore(coupon.getValidFrom())) return false;
        if (coupon.getValidUntil() != null && now.isAfter(coupon.getValidUntil())) return false;
        return true;
    }

    private BigDecimal calculateDiscount(BigDecimal originalAmount, Coupon coupon) {
        BigDecimal discount = originalAmount.multiply(coupon.getDiscountPercent())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        if (coupon.getMaxDiscountAmount() != null && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
            return coupon.getMaxDiscountAmount();
        }
        return discount;
    }

    private RentPaymentResponse toResponse(RentPayment p) {
        return RentPaymentResponse.builder()
                .rentPaymentId(p.getId())
                .bookingRequestId(p.getBookingRequest().getId())
                .propertyTitle(p.getProperty().getTitle())
                .roomNumber(p.getRoom() != null ? p.getRoom().getRoomNumber() : null)
                .originalAmount(p.getOriginalAmount())
                .couponCode(p.getCouponCode())
                .discountAmount(p.getDiscountAmount())
                .studentPayableAmount(p.getStudentPayableAmount())
                .ownerPayoutAmount(p.getOwnerPayoutAmount())
                .status(p.getStatus().name())
                .razorpayPaymentId(p.getRazorpayPaymentId())
                .payoutStatus(p.getPayoutStatus().name())
                .payoutTransactionRef(p.getPayoutTransactionRef())
                .payoutFailureReason(p.getPayoutFailureReason())
                .createdAt(p.getCreatedAt())
                .paidAt(p.getPaidAt())
                .payoutAt(p.getPayoutAt())
                .build();
    }

    private RentPaymentResponse toStudentAwareResponse(RentPayment p) {
        RentPaymentResponse response = toResponse(p);
        response.setStudentId(p.getStudent().getId());
        response.setStudentName(p.getStudent().getName());
        response.setStudentDisplayId(p.getStudent().getDisplayId());
        return response;
    }
}