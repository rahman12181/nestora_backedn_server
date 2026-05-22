package com.nestora.nestora_app.repository;

import com.nestora.nestora_app.entity.OwnerProfile;
import com.nestora.nestora_app.entity.PropertyAccessSubscription;
import com.nestora.nestora_app.enums.PropertyAccessStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PropertyAccessSubscriptionRepository
        extends JpaRepository<PropertyAccessSubscription, Long> {

    // Owner ka current active subscription
    Optional<PropertyAccessSubscription> findTopByOwnerAndStatusOrderByEndDateDesc(
            OwnerProfile owner, PropertyAccessStatus status);

    // Owner ki saari subscriptions — history ke liye
    List<PropertyAccessSubscription> findByOwnerOrderByCreatedAtDesc(OwnerProfile owner);

    // Scheduler: woh subscriptions jo aaj expire ho rahi hain
    @Query("SELECT s FROM PropertyAccessSubscription s " +
            "WHERE s.status = 'ACTIVE' AND s.endDate <= :now")
    List<PropertyAccessSubscription> findExpiredSubscriptions(LocalDateTime now);

    // Scheduler: 7 din mein expire hone wali — aur warning abhi tak nahi bheji
    @Query("SELECT s FROM PropertyAccessSubscription s " +
            "WHERE s.status = 'ACTIVE' " +
            "AND s.warningSent = false " +
            "AND s.endDate BETWEEN :now AND :sevenDaysLater")
    List<PropertyAccessSubscription> findSubscriptionsExpiringSoon(
            LocalDateTime now, LocalDateTime sevenDaysLater);

    // Admin: status se filter karke
    List<PropertyAccessSubscription> findByStatusOrderByCreatedAtDesc(
            PropertyAccessStatus status);
}