package com.nestora.nestora_app.service;

import com.nestora.nestora_app.dto.request.PropertyAccessBuyRequest;
import com.nestora.nestora_app.dto.request.PropertyAccessConfirmRequest;
import com.nestora.nestora_app.dto.response.PropertyAccessConfirmResponse;
import com.nestora.nestora_app.dto.response.PropertyAccessHistoryResponse;
import com.nestora.nestora_app.dto.response.PropertyAccessOrderResponse;
import com.nestora.nestora_app.dto.response.PropertyAccessStatusResponse;
import com.nestora.nestora_app.entity.OwnerProfile;
import com.nestora.nestora_app.entity.Property;
import com.nestora.nestora_app.entity.PropertyAccessSubscription;
import com.nestora.nestora_app.entity.User;
import com.nestora.nestora_app.enums.NotificationType;
import com.nestora.nestora_app.enums.PropertyAccessPlan;
import com.nestora.nestora_app.enums.PropertyAccessStatus;
import com.nestora.nestora_app.enums.VerificationStatus;
import com.nestora.nestora_app.exception.AppException;
import com.nestora.nestora_app.repository.OwnerProfileRepository;
import com.nestora.nestora_app.repository.PropertyAccessSubscriptionRepository;
import com.nestora.nestora_app.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyAccessSubscriptionService {

    private final PropertyAccessSubscriptionRepository propertyAccessRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final PropertyRepository propertyRepository;
    private final RazorpayService razorpayService;
    private final NotificationService notificationService;

    // =============================================
    // BUY — Razorpay Order Create
    // =============================================
    public PropertyAccessOrderResponse buySubscription(User currentUser,
                                                       PropertyAccessBuyRequest request) {

        OwnerProfile owner = getVerifiedOwner(currentUser);

        PropertyAccessPlan plan = request.getPlan();
        long amountInPaise = plan.getPrice()
                .multiply(java.math.BigDecimal.valueOf(100))
                .longValue();

        String orderId = razorpayService.createOrder(
                amountInPaise,
                "PA-" + currentUser.getId() + "-" + System.currentTimeMillis()
        );

        return PropertyAccessOrderResponse.builder()
                .razorpayOrderId(orderId)
                .amount(amountInPaise)
                .currency("INR")
                .plan(plan)
                .durationMonths(plan.getDurationMonths())
                .planPrice(plan.getPrice())
                .build();
    }

    // =============================================
    // CONFIRM — Payment verify + Subscription activate
    // =============================================
    @Transactional
    public PropertyAccessConfirmResponse confirmSubscription(User currentUser,
                                                             PropertyAccessConfirmRequest request) {

        OwnerProfile owner = getVerifiedOwner(currentUser);

        // Razorpay signature verify karo
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

        PropertyAccessPlan plan = request.getPlan();
        LocalDateTime now = LocalDateTime.now();

        // Agar pehle se active subscription hai — toh uske khatam hone ke baad start karo
        Optional<PropertyAccessSubscription> existingActive = propertyAccessRepository
                .findTopByOwnerAndStatusOrderByEndDateDesc(owner, PropertyAccessStatus.ACTIVE);

        LocalDateTime startDate;
        if (existingActive.isPresent() && existingActive.get().getEndDate().isAfter(now)) {
            // Active subscription hai — extension: current end ke baad se shuru
            startDate = existingActive.get().getEndDate();
        } else {
            // Koi active nahi / expired — abhi se shuru
            startDate = now;
        }

        LocalDateTime endDate = startDate.plusMonths(plan.getDurationMonths());

        PropertyAccessSubscription subscription = PropertyAccessSubscription.builder()
                .owner(owner)
                .plan(plan)
                .durationMonths(plan.getDurationMonths())
                .status(PropertyAccessStatus.ACTIVE)
                .startDate(startDate)
                .endDate(endDate)
                .amountPaid(plan.getPrice())
                .razorpayOrderId(request.getRazorpayOrderId())
                .razorpayPaymentId(request.getRazorpayPaymentId())
                .razorpaySignature(request.getRazorpaySignature())
                .warningSent(false)
                .build();

        propertyAccessRepository.save(subscription);

        // Agar pehle properties hidden thi — wapas publish karo
        restoreOwnerProperties(owner);

        // Notification bhejo
        notificationService.createNotification(
                currentUser,
                "Property Access Activated! 🎉",
                "Aapka " + plan.getDurationMonths() + " month Property Access Subscription "
                        + "activate ho gaya. Ab aap properties add kar sakte hain.",
                NotificationType.PAYMENT,
                subscription.getId()
        );

        long daysRemaining = ChronoUnit.DAYS.between(now, endDate);

        return PropertyAccessConfirmResponse.builder()
                .plan(plan)
                .durationMonths(plan.getDurationMonths())
                .status(PropertyAccessStatus.ACTIVE)
                .startDate(startDate)
                .endDate(endDate)
                .daysRemaining(daysRemaining)
                .amountPaid(plan.getPrice())
                .paymentId(request.getRazorpayPaymentId())
                .build();
    }

    // =============================================
    // STATUS — Current subscription ki details
    // =============================================
    public PropertyAccessStatusResponse getStatus(User currentUser) {

        OwnerProfile owner = getVerifiedOwner(currentUser);

        Optional<PropertyAccessSubscription> activeOpt = propertyAccessRepository
                .findTopByOwnerAndStatusOrderByEndDateDesc(owner, PropertyAccessStatus.ACTIVE);

        if (activeOpt.isEmpty()) {
            // Koi active subscription nahi
            return PropertyAccessStatusResponse.builder()
                    .hasActiveSubscription(false)
                    .plan(null)
                    .status(PropertyAccessStatus.EXPIRED)
                    .startDate(null)
                    .endDate(null)
                    .daysRemaining(0L)
                    .isExpiringSoon(false)
                    .amountPaid(null)
                    .build();
        }

        PropertyAccessSubscription sub = activeOpt.get();
        long daysRemaining = ChronoUnit.DAYS.between(LocalDateTime.now(), sub.getEndDate());
        if (daysRemaining < 0) daysRemaining = 0;

        return PropertyAccessStatusResponse.builder()
                .hasActiveSubscription(true)
                .plan(sub.getPlan())
                .durationMonths(sub.getDurationMonths())
                .status(sub.getStatus())
                .startDate(sub.getStartDate())
                .endDate(sub.getEndDate())
                .daysRemaining(daysRemaining)
                .isExpiringSoon(daysRemaining <= 7)
                .amountPaid(sub.getAmountPaid())
                .build();
    }

    // =============================================
    // HISTORY — Purani saari subscriptions
    // =============================================
    public List<PropertyAccessHistoryResponse> getHistory(User currentUser) {

        OwnerProfile owner = getVerifiedOwner(currentUser);

        return propertyAccessRepository
                .findByOwnerOrderByCreatedAtDesc(owner)
                .stream()
                .map(this::mapToHistoryResponse)
                .collect(Collectors.toList());
    }

    // =============================================
    // RENEW — Same as buy (reuse buySubscription)
    // Confirm ke liye /property-access/confirm hi use hoga
    // =============================================
    public PropertyAccessOrderResponse renewSubscription(User currentUser,
                                                         PropertyAccessBuyRequest request) {
        // Renew = buy jaisa hi hai — same method reuse
        return buySubscription(currentUser, request);
    }

    // =============================================
    // CHECK — Kya owner property add kar sakta hai?
    // PropertyService use karta hai
    // =============================================
    public void checkPropertyAccessAllowed(OwnerProfile owner) {
        Optional<PropertyAccessSubscription> activeOpt = propertyAccessRepository
                .findTopByOwnerAndStatusOrderByEndDateDesc(owner, PropertyAccessStatus.ACTIVE);

        if (activeOpt.isEmpty()) {
            throw new AppException(
                    "Property add karne ke liye active Property Access Subscription zaruri hai. " +
                            "Subscription kharido: POST /owner/property-access/buy",
                    HttpStatus.FORBIDDEN
            );
        }

        PropertyAccessSubscription sub = activeOpt.get();
        if (sub.getEndDate().isBefore(LocalDateTime.now())) {
            throw new AppException(
                    "Aapki Property Access Subscription expire ho gayi hai. " +
                            "Renew karo: POST /owner/property-access/renew",
                    HttpStatus.FORBIDDEN
            );
        }
    }

    // =============================================
    // ADMIN — Saari subscriptions list
    // =============================================
    public List<PropertyAccessHistoryResponse> getAllSubscriptionsForAdmin(
            PropertyAccessStatus status) {

        List<PropertyAccessSubscription> list;

        if (status != null) {
            list = propertyAccessRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            list = propertyAccessRepository.findAll();
        }

        return list.stream()
                .map(this::mapToHistoryResponse)
                .collect(Collectors.toList());
    }

    // =============================================
    // ADMIN — Force Expire
    // =============================================
    @Transactional
    public String forceExpireSubscription(Long subscriptionId, String reason) {

        PropertyAccessSubscription sub = propertyAccessRepository.findById(subscriptionId)
                .orElseThrow(() -> new AppException(
                        "Subscription not found", HttpStatus.NOT_FOUND
                ));

        sub.setStatus(PropertyAccessStatus.CANCELLED);
        propertyAccessRepository.save(sub);

        // Owner ki properties hide karo
        hideOwnerProperties(sub.getOwner());

        // Owner ko notify karo
        notificationService.createNotification(
                sub.getOwner().getUser(),
                "❌ Subscription Cancelled",
                "Aapki Property Access Subscription cancel kar di gayi hai. " +
                        "Reason: " + reason + ". Aapki properties hide kar di gayi hain.",
                NotificationType.PAYMENT,
                subscriptionId
        );

        log.info("Force expired subscription {} for owner {} — reason: {}",
                subscriptionId, sub.getOwner().getId(), reason);

        return "Subscription force expired. Owner ki properties hide kar di gayi hain.";
    }

    // =============================================
    // SCHEDULER use karta hai — Expire + Hide
    // =============================================
    @Transactional
    public void expireSubscriptionAndHideProperties(PropertyAccessSubscription sub) {
        sub.setStatus(PropertyAccessStatus.EXPIRED);
        propertyAccessRepository.save(sub);

        hideOwnerProperties(sub.getOwner());

        notificationService.createNotification(
                sub.getOwner().getUser(),
                "❌ Property Access Subscription Expire!",
                "Aapki Property Access Subscription aaj expire ho gayi. " +
                        "Properties users ko nahi dikh rahi. Abhi renew karo: " +
                        "POST /owner/property-access/renew",
                NotificationType.PAYMENT,
                sub.getId()
        );

        log.info("Property Access Subscription expired for owner: {}",
                sub.getOwner().getUser().getEmail());
    }

    // =============================================
    // SCHEDULER use karta hai — Warning notification
    // =============================================
    @Transactional
    public void sendWarningNotification(PropertyAccessSubscription sub, long daysLeft) {
        sub.setWarningSent(true);
        propertyAccessRepository.save(sub);

        notificationService.createNotification(
                sub.getOwner().getUser(),
                "⚠️ Subscription Expire Ho Raha Hai!",
                "Aapki Property Access Subscription " + daysLeft +
                        " din mein expire hogi. Renew karo warna aapki properties " +
                        "users ko nahi dikhegi.",
                NotificationType.PAYMENT,
                sub.getId()
        );

        log.info("7-day warning sent to: {}",
                sub.getOwner().getUser().getEmail());
    }

    // =============================================
    // PRIVATE HELPERS
    // =============================================

    private OwnerProfile getVerifiedOwner(User user) {
        OwnerProfile owner = ownerProfileRepository.findByUser(user)
                .orElseThrow(() -> new AppException(
                        "Owner profile not found. Please apply as owner first.",
                        HttpStatus.NOT_FOUND
                ));

        if (owner.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new AppException(
                    "Only verified owners can buy property access subscription.",
                    HttpStatus.FORBIDDEN
            );
        }

        return owner;
    }

    private void hideOwnerProperties(OwnerProfile owner) {
        List<Property> properties = propertyRepository.findByOwner(owner);
        properties.forEach(p -> {
            p.setIsPublished(false);
            propertyRepository.save(p);
        });
        log.info("Hidden {} properties for owner: {}",
                properties.size(), owner.getUser().getEmail());
    }

    private void restoreOwnerProperties(OwnerProfile owner) {
        // Sirf woh properties restore karo jo pehle published thi
        // Note: isActive = true matlab owner ne kabhi publish kiya tha
        // Admin dobara publish karega ya owner ka existing published state restore
        List<Property> properties = propertyRepository.findByOwner(owner);
        properties.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .forEach(p -> {
                    p.setIsPublished(true);
                    propertyRepository.save(p);
                });
        log.info("Restored {} properties for owner: {}",
                properties.size(), owner.getUser().getEmail());
    }

    private PropertyAccessHistoryResponse mapToHistoryResponse(
            PropertyAccessSubscription sub) {
        return PropertyAccessHistoryResponse.builder()
                .subscriptionId(sub.getId())
                .plan(sub.getPlan())
                .durationMonths(sub.getDurationMonths())
                .status(sub.getStatus())
                .startDate(sub.getStartDate())
                .endDate(sub.getEndDate())
                .amountPaid(sub.getAmountPaid())
                .paymentId(sub.getRazorpayPaymentId())
                .purchasedAt(sub.getCreatedAt())
                .build();
    }
}