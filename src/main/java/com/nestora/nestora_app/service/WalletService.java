package com.nestora.nestora_app.service;

import com.nestora.nestora_app.dto.response.WalletTransactionResponse;
import com.nestora.nestora_app.entity.User;
import com.nestora.nestora_app.entity.WalletTransaction;
import com.nestora.nestora_app.enums.WalletTransactionReason;
import com.nestora.nestora_app.enums.WalletTransactionType;
import com.nestora.nestora_app.exception.AppException;
import com.nestora.nestora_app.repository.UserRepository;
import com.nestora.nestora_app.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * ASSUMPTION: your User entity has (or you will add) a `walletBalance` BigDecimal field
 * with getWalletBalance()/setWalletBalance() — see the patch instructions in README for
 * the exact 3 lines to add to User.java, plus the DB migration.
 *
 * ASSUMPTION: UserRepository already exists (standard JpaRepository<User, Long>) — this
 * class only calls the standard .save(user), which every JpaRepository has by default.
 */
@Service
@RequiredArgsConstructor
public class WalletService {

    private final UserRepository userRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @Transactional
    public WalletTransaction credit(User user, BigDecimal amount, WalletTransactionReason reason,
                                    Long referenceId, String description) {

        BigDecimal currentBalance = user.getWalletBalance() == null ? BigDecimal.ZERO : user.getWalletBalance();
        BigDecimal newBalance = currentBalance.add(amount);

        user.setWalletBalance(newBalance);
        userRepository.save(user);

        WalletTransaction txn = WalletTransaction.builder()
                .user(user)
                .type(WalletTransactionType.CREDIT)
                .amount(amount)
                .reason(reason)
                .referenceId(referenceId)
                .balanceAfter(newBalance)
                .description(description)
                .build();

        return walletTransactionRepository.save(txn);
    }

    @Transactional
    public WalletTransaction debit(User user, BigDecimal amount, WalletTransactionReason reason,
                                   Long referenceId, String description) {

        BigDecimal currentBalance = user.getWalletBalance() == null ? BigDecimal.ZERO : user.getWalletBalance();

        if (currentBalance.compareTo(amount) < 0) {
            throw new AppException("Insufficient wallet balance", HttpStatus.BAD_REQUEST);
        }

        BigDecimal newBalance = currentBalance.subtract(amount);
        user.setWalletBalance(newBalance);
        userRepository.save(user);

        WalletTransaction txn = WalletTransaction.builder()
                .user(user)
                .type(WalletTransactionType.DEBIT)
                .amount(amount)
                .reason(reason)
                .referenceId(referenceId)
                .balanceAfter(newBalance)
                .description(description)
                .build();

        return walletTransactionRepository.save(txn);
    }

    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> getTransactions(User user, Pageable pageable) {
        return walletTransactionRepository.findByUser_IdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(t -> WalletTransactionResponse.builder()
                        .transactionId(t.getId())
                        .type(t.getType().name())
                        .amount(t.getAmount())
                        .reason(t.getReason().name())
                        .balanceAfter(t.getBalanceAfter())
                        .description(t.getDescription())
                        .createdAt(t.getCreatedAt())
                        .build());
    }
}