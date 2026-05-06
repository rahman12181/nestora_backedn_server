package com.nestora.nestora_app.repository;


import com.nestora.nestora_app.entity.OwnerProfile;
import com.nestora.nestora_app.entity.SubscriptionPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPayment, Long> {
    List<SubscriptionPayment> findByOwnerOrderByCreatedAtDesc(OwnerProfile owner);
}