package com.nestora.nestora_app;

import com.nestora.nestora_app.entity.OwnerProfile;
import com.nestora.nestora_app.entity.PropertyAccessSubscription;
import com.nestora.nestora_app.enums.NotificationType;
import com.nestora.nestora_app.enums.PropertyAccessStatus;
import com.nestora.nestora_app.enums.SubscriptionStatus;
import com.nestora.nestora_app.repository.OwnerProfileRepository;
import com.nestora.nestora_app.repository.PropertyAccessSubscriptionRepository;
import com.nestora.nestora_app.repository.TokenBlacklistRepository;
import com.nestora.nestora_app.service.EmailService;
import com.nestora.nestora_app.service.NotificationService;
import com.nestora.nestora_app.service.PropertyAccessSubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionScheduler {

    private final OwnerProfileRepository ownerProfileRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final TokenBlacklistRepository tokenBlacklistRepository;

    // 🆕 Property Access ke liye
    private final PropertyAccessSubscriptionRepository propertyAccessRepository;
    private final PropertyAccessSubscriptionService propertyAccessService;

    // =============================================
    // Existing — Listing Subscription (BASIC/STANDARD/etc.)
    // Har roz subah 9 baje
    // =============================================
    @Scheduled(cron = "0 0 9 * * *")
    public void checkExpiringListingSubscriptions() {
        log.info("Checking expiring listing subscriptions...");

        List<OwnerProfile> owners = ownerProfileRepository.findAll();

        for (OwnerProfile owner : owners) {
            if (owner.getSubscriptionEnd() == null) continue;
            if (owner.getSubscriptionStatus() != SubscriptionStatus.ACTIVE) continue;

            long daysLeft = ChronoUnit.DAYS.between(
                    LocalDateTime.now(),
                    owner.getSubscriptionEnd()
            );

            // 7 din pehle remind karo
            if (daysLeft == 7) {
                notificationService.createNotification(
                        owner.getUser(),
                        "Listing Subscription Expiring Soon! ⚠️",
                        "Your " + owner.getSubscriptionPlan().name() +
                                " listing plan expires in 7 days. Renew now.",
                        NotificationType.PAYMENT,
                        owner.getId()
                );

                emailService.sendSubscriptionReminderEmail(
                        owner.getUser().getEmail(),
                        owner.getUser().getName(),
                        7,
                        owner.getSubscriptionPlan().name()
                );

                log.info("Listing 7-day reminder sent to: {}", owner.getUser().getEmail());
            }

            // 1 din pehle
            if (daysLeft == 1) {
                notificationService.createNotification(
                        owner.getUser(),
                        "Last Day! Listing Subscription Expires Tomorrow ⚠️",
                        "Your listing subscription expires tomorrow. Renew now!",
                        NotificationType.PAYMENT,
                        owner.getId()
                );
            }

            // Expire ho gaya
            if (daysLeft <= 0) {
                owner.setSubscriptionStatus(SubscriptionStatus.EXPIRED);
                ownerProfileRepository.save(owner);

                notificationService.createNotification(
                        owner.getUser(),
                        "Listing Subscription Expired",
                        "Your listing subscription has expired. Renew to keep listings ranked.",
                        NotificationType.PAYMENT,
                        owner.getId()
                );

                log.info("Listing subscription expired for: {}", owner.getUser().getEmail());
            }
        }
    }

    // =============================================
    // 🆕 NEW — Property Access Subscription
    // Har roz subah 8 baje
    // =============================================
    @Scheduled(cron = "0 0 8 * * *")
    public void checkPropertyAccessSubscriptions() {
        log.info("Checking property access subscriptions...");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysLater = now.plusDays(7);

        // 1. Jo subscriptions expire ho chuki hain — hide properties
        List<PropertyAccessSubscription> expired =
                propertyAccessRepository.findExpiredSubscriptions(now);

        for (PropertyAccessSubscription sub : expired) {
            try {
                propertyAccessService.expireSubscriptionAndHideProperties(sub);
            } catch (Exception e) {
                log.error("Error expiring subscription {}: {}", sub.getId(), e.getMessage());
            }
        }

        log.info("Expired {} property access subscriptions", expired.size());

        // 2. Jo 7 din mein expire hone wali hain — warning bhejo
        List<PropertyAccessSubscription> expiringSoon =
                propertyAccessRepository.findSubscriptionsExpiringSoon(now, sevenDaysLater);

        for (PropertyAccessSubscription sub : expiringSoon) {
            try {
                long daysLeft = ChronoUnit.DAYS.between(now, sub.getEndDate());
                propertyAccessService.sendWarningNotification(sub, daysLeft);
            } catch (Exception e) {
                log.error("Error sending warning for subscription {}: {}",
                        sub.getId(), e.getMessage());
            }
        }

        log.info("Sent {} property access warning notifications", expiringSoon.size());
    }

    // =============================================
    // Har raat 2 baje — expired tokens cleanup
    // =============================================
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupExpiredTokens() {
        tokenBlacklistRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("Expired tokens cleaned up");
    }
}