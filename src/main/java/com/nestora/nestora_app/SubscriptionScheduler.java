package com.nestora.nestora_app;


import com.nestora.nestora_app.entity.OwnerProfile;
import com.nestora.nestora_app.enums.NotificationType;
import com.nestora.nestora_app.enums.SubscriptionStatus;
import com.nestora.nestora_app.repository.OwnerProfileRepository;
import com.nestora.nestora_app.repository.TokenBlacklistRepository;
import com.nestora.nestora_app.service.EmailService;
import com.nestora.nestora_app.service.NotificationService;
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

    // Har roz subah 9 baje check karo
    @Scheduled(cron = "0 0 9 * * *")
    public void checkExpiringSubscriptions() {
        log.info("Checking expiring subscriptions...");

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
                        "Subscription Expiring Soon! ⚠️",
                        "Your " + owner.getSubscriptionPlan().name() +
                                " plan expires in 7 days. Renew now to keep your listings active.",
                        NotificationType.PAYMENT,
                        owner.getId()
                );

                // Email bhi bhejo
                emailService.sendSubscriptionReminderEmail(
                        owner.getUser().getEmail(),
                        owner.getUser().getName(),
                        7,
                        owner.getSubscriptionPlan().name()
                );

                log.info("7 day reminder sent to: {}",
                        owner.getUser().getEmail());
            }

            // 1 din pehle final reminder
            if (daysLeft == 1) {
                notificationService.createNotification(
                        owner.getUser(),
                        "Last Day! Subscription Expires Tomorrow ⚠️",
                        "Your subscription expires tomorrow. Renew now!",
                        NotificationType.PAYMENT,
                        owner.getId()
                );

                log.info("1 day reminder sent to: {}",
                        owner.getUser().getEmail());
            }

            // Expire ho gaya
            if (daysLeft <= 0) {
                owner.setSubscriptionStatus(SubscriptionStatus.EXPIRED);
                ownerProfileRepository.save(owner);

                notificationService.createNotification(
                        owner.getUser(),
                        "Subscription Expired",
                        "Your subscription has expired. Renew to keep listings visible.",
                        NotificationType.PAYMENT,
                        owner.getId()
                );

                log.info("Subscription expired for: {}",
                        owner.getUser().getEmail());
            }
        }
    }

    // Har raat 2 baje expired tokens delete karo
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupExpiredTokens() {
        tokenBlacklistRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("Expired tokens cleaned up");
    }
}
