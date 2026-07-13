package com.nestora.nestora_app.service;

import com.nestora.nestora_app.dto.response.WithdrawalResponse;
import com.nestora.nestora_app.entity.User;
import com.nestora.nestora_app.entity.WithdrawalRequest;
import com.nestora.nestora_app.enums.WalletTransactionReason;
import com.nestora.nestora_app.enums.WithdrawalStatus;
import com.nestora.nestora_app.exception.AppException;
import com.nestora.nestora_app.repository.WithdrawalRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final WalletService walletService;

    /**
     * User requests a withdrawal. Amount is deducted from wallet IMMEDIATELY (put "on hold")
     * so the same balance can't be requested twice while the first request is still pending.
     * If admin rejects later, the amount is refunded back to the wallet.
     */
    @Transactional
    public WithdrawalResponse requestWithdrawal(User currentUser, BigDecimal amount, String upiId) {

        WithdrawalRequest withdrawal = WithdrawalRequest.builder()
                .user(currentUser)
                .amount(amount)
                .upiId(upiId)
                .status(WithdrawalStatus.PENDING)
                .build();

        withdrawal = withdrawalRequestRepository.save(withdrawal);

        // This throws AppException("Insufficient wallet balance", 400) if balance is too low
        walletService.debit(
                currentUser,
                amount,
                WalletTransactionReason.WITHDRAWAL_HOLD,
                withdrawal.getId(),
                "Withdrawal requested to UPI: " + upiId
        );

        return toResponse(withdrawal);
    }

    @Transactional(readOnly = true)
    public List<WithdrawalResponse> getMyWithdrawals(User currentUser) {
        return withdrawalRequestRepository.findByUser_IdOrderByRequestedAtDesc(currentUser.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WithdrawalResponse> getPendingWithdrawals() {
        return withdrawalRequestRepository.findByStatusOrderByRequestedAtAsc(WithdrawalStatus.PENDING)
                .stream()
                .map(this::toAdminResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public WithdrawalResponse approveWithdrawal(User admin, Long withdrawalId, String transactionRef) {
        WithdrawalRequest withdrawal = withdrawalRequestRepository.findById(withdrawalId)
                .orElseThrow(() -> new AppException("Withdrawal request not found", HttpStatus.NOT_FOUND));

        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new AppException("Only pending requests can be approved", HttpStatus.BAD_REQUEST);
        }

        withdrawal.setStatus(WithdrawalStatus.APPROVED);
        withdrawal.setTransactionRef(transactionRef);
        withdrawal.setProcessedAt(java.time.LocalDateTime.now());
        withdrawal.setProcessedBy(admin);

        withdrawal = withdrawalRequestRepository.save(withdrawal);

        // OPTIONAL: notify user via Module 8 —
        // notificationService.send(withdrawal.getUser(), "💸 Payment sent!",
        //     "₹" + withdrawal.getAmount() + " has been sent to " + withdrawal.getUpiId(), "PAYMENT", withdrawal.getId());

        return toResponse(withdrawal);
    }

    @Transactional
    public WithdrawalResponse rejectWithdrawal(User admin, Long withdrawalId, String reason) {
        WithdrawalRequest withdrawal = withdrawalRequestRepository.findById(withdrawalId)
                .orElseThrow(() -> new AppException("Withdrawal request not found", HttpStatus.NOT_FOUND));

        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new AppException("Only pending requests can be rejected", HttpStatus.BAD_REQUEST);
        }

        withdrawal.setStatus(WithdrawalStatus.REJECTED);
        withdrawal.setAdminNote(reason);
        withdrawal.setProcessedAt(java.time.LocalDateTime.now());
        withdrawal.setProcessedBy(admin);
        withdrawal = withdrawalRequestRepository.save(withdrawal);

        // Refund the held amount back to the user's wallet
        walletService.credit(
                withdrawal.getUser(),
                withdrawal.getAmount(),
                WalletTransactionReason.WITHDRAWAL_REJECTED_REFUND,
                withdrawal.getId(),
                "Withdrawal rejected: " + reason + " — amount refunded"
        );

        return toResponse(withdrawal);
    }

    private WithdrawalResponse toResponse(WithdrawalRequest w) {
        return WithdrawalResponse.builder()
                .withdrawalId(w.getId())
                .amount(w.getAmount())
                .upiId(w.getUpiId())
                .status(w.getStatus().name())
                .transactionRef(w.getTransactionRef())
                .adminNote(w.getAdminNote())
                .requestedAt(w.getRequestedAt())
                .processedAt(w.getProcessedAt())
                .build();
    }

    private WithdrawalResponse toAdminResponse(WithdrawalRequest w) {
        WithdrawalResponse response = toResponse(w);
        response.setUserId(w.getUser().getId());
        response.setUserName(w.getUser().getName());
        response.setUserDisplayId(w.getUser().getDisplayId());
        return response;
    }
}