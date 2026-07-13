package com.nestora.nestora_app.repository;

import com.nestora.nestora_app.entity.Referral;
import com.nestora.nestora_app.enums.ReferralStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReferralRepository extends JpaRepository<Referral, Long> {

    boolean existsByReferredUser_Id(Long referredUserId);

    Optional<Referral> findByReferredUser_Id(Long referredUserId);

    List<Referral> findByReferrer_IdOrderByCreatedAtDesc(Long referrerId);

    long countByReferrer_IdAndStatus(Long referrerId, ReferralStatus status);
}