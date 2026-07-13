package com.nestora.nestora_app.repository;

import com.nestora.nestora_app.entity.WithdrawalRequest;
import com.nestora.nestora_app.enums.WithdrawalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {

    List<WithdrawalRequest> findByUser_IdOrderByRequestedAtDesc(Long userId);

    List<WithdrawalRequest> findByStatusOrderByRequestedAtAsc(WithdrawalStatus status);

    // used by the RazorpayX webhook handler to find which withdrawal a payout event belongs to
    Optional<WithdrawalRequest> findByRazorpayXPayoutId(String razorpayXPayoutId);

    // NEW — Admin Payment History screen: full list, newest first, optional status filter
    List<WithdrawalRequest> findAllByOrderByRequestedAtDesc();

    List<WithdrawalRequest> findByStatusOrderByRequestedAtDesc(WithdrawalStatus status);
}