package com.nestora.nestora_app.repository;

import com.nestora.nestora_app.entity.WithdrawalRequest;
import com.nestora.nestora_app.enums.WithdrawalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {

    List<WithdrawalRequest> findByUser_IdOrderByRequestedAtDesc(Long userId);

    List<WithdrawalRequest> findByStatusOrderByRequestedAtAsc(WithdrawalStatus status);
}