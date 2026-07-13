package com.nestora.nestora_app.repository;

import com.nestora.nestora_app.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    Page<WalletTransaction> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}