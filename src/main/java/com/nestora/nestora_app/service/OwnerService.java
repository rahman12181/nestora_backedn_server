package com.nestora.nestora_app.service;

import com.nestora.nestora_app.dto.request.OwnerApplyRequest;
import com.nestora.nestora_app.dto.request.SubscriptionBuyRequest;
import com.nestora.nestora_app.dto.request.SubscriptionConfirmRequest;
import com.nestora.nestora_app.dto.response.OwnerDashboardResponse;
import com.nestora.nestora_app.dto.response.OwnerProfileResponse;
import com.nestora.nestora_app.dto.response.SubscriptionDetailsResponse;
import com.nestora.nestora_app.dto.response.SubscriptionOrderResponse;
import com.nestora.nestora_app.dto.response.VerificationStatusResponse;
import com.nestora.nestora_app.entity.OwnerProfile;
import com.nestora.nestora_app.entity.Property;
import com.nestora.nestora_app.entity.Review;
import com.nestora.nestora_app.entity.Room;
import com.nestora.nestora_app.entity.SubscriptionPayment;
import com.nestora.nestora_app.entity.User;
import com.nestora.nestora_app.enums.BookingStatus;
import com.nestora.nestora_app.enums.NotificationType;
import com.nestora.nestora_app.enums.Role;
import com.nestora.nestora_app.enums.RoomStatus;
import com.nestora.nestora_app.enums.SubscriptionPlan;
import com.nestora.nestora_app.enums.SubscriptionStatus;
import com.nestora.nestora_app.enums.VerificationStatus;
import com.nestora.nestora_app.exception.AppException;
import com.nestora.nestora_app.repository.BookingRequestRepository;
import com.nestora.nestora_app.repository.OwnerProfileRepository;
import com.nestora.nestora_app.repository.PropertyRepository;
import com.nestora.nestora_app.repository.ReviewRepository;
import com.nestora.nestora_app.repository.RoomRepository;
import com.nestora.nestora_app.repository.SubscriptionPaymentRepository;
import com.nestora.nestora_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OwnerService {

    private final OwnerProfileRepository ownerProfileRepository;
    private final SubscriptionPaymentRepository subscriptionPaymentRepository;
    private final CloudinaryService cloudinaryService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final RazorpayService razorpayService;
    private final PropertyRepository propertyRepository;
    private final RoomRepository roomRepository;
    private final BookingRequestRepository bookingRequestRepository;
    private final ReviewRepository reviewRepository;

    // =============================================
    // APPLY AS OWNER
    // =============================================
    @Transactional
    public String applyAsOwner(User currentUser,
                               OwnerApplyRequest request,
                               MultipartFile aadharDoc,
                               MultipartFile panDoc,
                               MultipartFile addressProof) {

        if (ownerProfileRepository.existsByUser(currentUser)) {

            OwnerProfile existing = ownerProfileRepository
                    .findByUser(currentUser)
                    .orElseThrow(() -> new AppException(
                            "Owner profile not found", HttpStatus.NOT_FOUND
                    ));

            if (existing.getVerificationStatus() == VerificationStatus.VERIFIED) {
                throw new AppException(
                        "You are already a verified owner.",
                        HttpStatus.CONFLICT
                );
            }

            if (existing.getVerificationStatus() == VerificationStatus.PENDING) {
                throw new AppException(
                        "Application already submitted. Please wait for admin review.",
                        HttpStatus.CONFLICT
                );
            }

            // REJECTED tha — reapply
            if (aadharDoc != null && !aadharDoc.isEmpty()) {
                existing.setAadharDocUrl(
                        cloudinaryService.uploadDocument(aadharDoc, "owner-docs")
                );
            }
            if (panDoc != null && !panDoc.isEmpty()) {
                existing.setPanDocUrl(
                        cloudinaryService.uploadDocument(panDoc, "owner-docs")
                );
            }
            if (addressProof != null && !addressProof.isEmpty()) {
                existing.setAddressProofUrl(
                        cloudinaryService.uploadDocument(addressProof, "owner-docs")
                );
            }

            existing.setBusinessName(request.getBusinessName());
            existing.setAadharNumber(request.getAadharNumber());
            existing.setPanNumber(request.getPanNumber());
            existing.setVerificationStatus(VerificationStatus.PENDING);
            existing.setRejectionReason(null);
            ownerProfileRepository.save(existing);

            notifyAdmins(
                    "Owner Re-Application",
                    currentUser.getName() +
                            " has re-applied to become an owner. Please review.",
                    existing.getId()
            );

            return "Re-application submitted. Admin will review within 24-48 hours.";
        }

        // New application
        String aadharUrl = (aadharDoc != null && !aadharDoc.isEmpty())
                ? cloudinaryService.uploadDocument(aadharDoc, "owner-docs") : null;

        String panUrl = (panDoc != null && !panDoc.isEmpty())
                ? cloudinaryService.uploadDocument(panDoc, "owner-docs") : null;

        String addressUrl = (addressProof != null && !addressProof.isEmpty())
                ? cloudinaryService.uploadDocument(addressProof, "owner-docs") : null;

        OwnerProfile ownerProfile = OwnerProfile.builder()
                .user(currentUser)
                .businessName(request.getBusinessName())
                .aadharNumber(request.getAadharNumber())
                .aadharDocUrl(aadharUrl)
                .panNumber(request.getPanNumber())
                .panDocUrl(panUrl)
                .addressProofUrl(addressUrl)
                .verificationStatus(VerificationStatus.PENDING)
                .subscriptionPlan(SubscriptionPlan.BASIC)
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .build();

        OwnerProfile saved = ownerProfileRepository.save(ownerProfile);

        notifyAdmins(
                "New Owner Application",
                currentUser.getName() +
                        " has applied to become an owner. Please review.",
                saved.getId()
        );

        return "Owner application submitted. Admin will verify within 24-48 hours.";
    }

    // =============================================
    // GET MY PROFILE
    // =============================================
    public OwnerProfileResponse getMyProfile(User currentUser) {
        OwnerProfile owner = getOwnerProfileByUser(currentUser);
        return OwnerProfileResponse.builder()
                .ownerId(owner.getId())
                .userId(currentUser.getId())
                .displayId(currentUser.getDisplayId())
                .name(currentUser.getName())
                .email(currentUser.getEmail())
                .phone(currentUser.getPhone())
                .businessName(owner.getBusinessName())
                .aadharNumber(maskAadhar(owner.getAadharNumber()))
                .panNumber(owner.getPanNumber())
                .aadharDocUrl(owner.getAadharDocUrl())
                .panDocUrl(owner.getPanDocUrl())
                .addressProofUrl(owner.getAddressProofUrl())
                .verificationStatus(owner.getVerificationStatus())
                .rejectionReason(owner.getRejectionReason())
                .verifiedAt(owner.getVerifiedAt())
                .subscriptionPlan(owner.getSubscriptionPlan())
                .subscriptionStatus(owner.getSubscriptionStatus())
                .subscriptionStart(owner.getSubscriptionStart())
                .subscriptionEnd(owner.getSubscriptionEnd())
                .monthlyFee(owner.getMonthlyFee())
                .createdAt(owner.getCreatedAt())
                .build();
    }

    // =============================================
    // GET VERIFICATION STATUS
    // =============================================
    public VerificationStatusResponse getVerificationStatus(User currentUser) {
        OwnerProfile owner = getOwnerProfileByUser(currentUser);
        return VerificationStatusResponse.builder()
                .verificationStatus(owner.getVerificationStatus())
                .rejectionReason(owner.getRejectionReason())
                .verifiedAt(owner.getVerifiedAt())
                .build();
    }

    // =============================================
    // BUY SUBSCRIPTION — Real Razorpay
    // =============================================
    public SubscriptionOrderResponse buySubscription(User currentUser,
                                                     SubscriptionBuyRequest request) {
        OwnerProfile owner = getOwnerProfileByUser(currentUser);

        if (owner.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new AppException(
                    "Only verified owners can buy subscription.",
                    HttpStatus.FORBIDDEN
            );
        }

        BigDecimal amount = getPlanPrice(request.getPlan());
        long amountInPaise = amount.multiply(BigDecimal.valueOf(100)).longValue();

        // Real Razorpay order create karo
        String orderId = razorpayService.createOrder(
                amountInPaise,
                "NST-" + currentUser.getId() + "-" + System.currentTimeMillis()
        );

        SubscriptionPayment payment = SubscriptionPayment.builder()
                .owner(owner)
                .razorpayOrderId(orderId)
                .amount(amount)
                .plan(request.getPlan())
                .status("PENDING")
                .build();

        subscriptionPaymentRepository.save(payment);

        return SubscriptionOrderResponse.builder()
                .razorpayOrderId(orderId)
                .amount(amountInPaise)
                .currency("INR")
                .plan(request.getPlan())
                .build();
    }

    // =============================================
    // CONFIRM SUBSCRIPTION — Signature verify
    // =============================================
    @Transactional
    public SubscriptionDetailsResponse confirmSubscription(
            User currentUser,
            SubscriptionConfirmRequest request) {

        OwnerProfile owner = getOwnerProfileByUser(currentUser);

        // Real Razorpay signature verify karo
        boolean isValid = razorpayService.verifyPayment(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        if (!isValid) {
            throw new AppException(
                    "Payment verification failed. Invalid signature.",
                    HttpStatus.BAD_REQUEST
            );
        }

        BigDecimal amount = getPlanPrice(request.getPlan());

        SubscriptionPayment payment = SubscriptionPayment.builder()
                .owner(owner)
                .razorpayOrderId(request.getRazorpayOrderId())
                .razorpayPaymentId(request.getRazorpayPaymentId())
                .amount(amount)
                .plan(request.getPlan())
                .status("SUCCESS")
                .paymentDate(LocalDateTime.now())
                .periodStart(LocalDateTime.now())
                .periodEnd(LocalDateTime.now().plusMonths(1))
                .build();

        subscriptionPaymentRepository.save(payment);

        owner.setSubscriptionPlan(request.getPlan());
        owner.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        owner.setSubscriptionStart(LocalDateTime.now());
        owner.setSubscriptionEnd(LocalDateTime.now().plusMonths(1));
        owner.setMonthlyFee(amount);
        ownerProfileRepository.save(owner);

        notificationService.createNotification(
                currentUser,
                "Subscription Activated! 🎉",
                "Your " + request.getPlan().name() + " plan is now active.",
                NotificationType.PAYMENT,
                owner.getId()
        );

        return buildSubscriptionDetails(owner);
    }

    // =============================================
    // GET SUBSCRIPTION DETAILS
    // =============================================
    public SubscriptionDetailsResponse getSubscriptionDetails(User currentUser) {
        OwnerProfile owner = getOwnerProfileByUser(currentUser);
        return buildSubscriptionDetails(owner);
    }

    // =============================================
    // OWNER DASHBOARD STATS
    // =============================================
    public OwnerDashboardResponse getMyDashboard(User currentUser) {
        OwnerProfile owner = getOwnerProfileByUser(currentUser);

        List<Property> properties = propertyRepository.findByOwner(owner);

        long totalProperties = properties.size();
        long publishedProperties = properties.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsPublished()))
                .count();

        long totalViews = properties.stream()
                .mapToLong(p -> p.getViewCount() != null ? p.getViewCount() : 0)
                .sum();

        // Rooms
        List<Room> allRooms = properties.stream()
                .flatMap(p -> roomRepository.findByProperty(p).stream())
                .collect(Collectors.toList());

        long totalRooms = allRooms.size();
        long availableRooms = allRooms.stream()
                .filter(r -> r.getStatus() == RoomStatus.AVAILABLE)
                .count();
        long occupiedRooms = allRooms.stream()
                .filter(r -> r.getStatus() == RoomStatus.OCCUPIED)
                .count();

        // Bookings
        List<com.nestora.nestora_app.entity.BookingRequest> bookings =
                bookingRequestRepository.findByPropertyIn(properties);

        long totalBookings = bookings.size();
        long pendingBookings = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING)
                .count();
        long acceptedBookings = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.ACCEPTED)
                .count();
        long rejectedBookings = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.REJECTED)
                .count();

        // Average rating
        double avgRating = properties.stream()
                .flatMap(p -> reviewRepository
                        .findByPropertyAndIsVisibleTrue(p).stream())
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);

        return OwnerDashboardResponse.builder()
                .totalProperties(totalProperties)
                .publishedProperties(publishedProperties)
                .totalRooms(totalRooms)
                .availableRooms(availableRooms)
                .occupiedRooms(occupiedRooms)
                .totalBookingRequests(totalBookings)
                .pendingRequests(pendingBookings)
                .acceptedRequests(acceptedBookings)
                .rejectedRequests(rejectedBookings)
                .totalViews(totalViews)
                .averageRating(Math.round(avgRating * 10.0) / 10.0)
                .build();
    }

    // =============================================
    // BUY FEATURED LISTING
    // =============================================
    @Transactional
    public String buyFeaturedListing(User currentUser,
                                     Long propertyId,
                                     Integer days) {

        getOwnerProfileByUser(currentUser);

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new AppException(
                        "Property not found", HttpStatus.NOT_FOUND
                ));

        property.setIsFeatured(true);
        property.setFeaturedUntil(LocalDateTime.now().plusDays(days));
        propertyRepository.save(property);

        return "Featured listing activated for " + days + " days";
    }

    // =============================================
    // PRIVATE HELPERS
    // =============================================

    public OwnerProfile getOwnerProfileByUser(User user) {
        return ownerProfileRepository.findByUser(user)
                .orElseThrow(() -> new AppException(
                        "Owner profile not found. Please apply as owner first.",
                        HttpStatus.NOT_FOUND
                ));
    }

    private BigDecimal getPlanPrice(SubscriptionPlan plan) {
        return switch (plan) {
            case BASIC      -> BigDecimal.valueOf(399);
            case STANDARD   -> BigDecimal.valueOf(599);
            case PREMIUM    -> BigDecimal.valueOf(799);
            case ENTERPRISE -> BigDecimal.valueOf(999);
        };
    }

    private SubscriptionDetailsResponse buildSubscriptionDetails(OwnerProfile owner) {
        long daysRemaining = 0;
        if (owner.getSubscriptionEnd() != null) {
            daysRemaining = ChronoUnit.DAYS.between(
                    LocalDateTime.now(),
                    owner.getSubscriptionEnd()
            );
            if (daysRemaining < 0) daysRemaining = 0;
        }

        return SubscriptionDetailsResponse.builder()
                .plan(owner.getSubscriptionPlan())
                .subscriptionStatus(owner.getSubscriptionStatus())
                .startDate(owner.getSubscriptionStart())
                .endDate(owner.getSubscriptionEnd())
                .monthlyFee(owner.getMonthlyFee())
                .daysRemaining(daysRemaining)
                .build();
    }

    private void notifyAdmins(String title, String body, Long refId) {
        userRepository.findAll()
                .stream()
                .filter(u -> u.getRole() == Role.ADMIN)
                .forEach(admin ->
                        notificationService.createNotification(
                                admin,
                                title,
                                body,
                                NotificationType.VERIFICATION,
                                refId
                        )
                );
    }

    private String maskAadhar(String aadhar) {
        if (aadhar == null) return null;
        return "XXXX XXXX " + aadhar.substring(aadhar.length() - 4);
    }
}