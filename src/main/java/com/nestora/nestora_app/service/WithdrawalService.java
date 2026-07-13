package com.nestora.nestora_app.service;

import com.nestora.nestora_app.dto.response.PayoutResult;
import com.nestora.nestora_app.dto.response.WithdrawalResponse;
import com.nestora.nestora_app.entity.User;
import com.nestora.nestora_app.entity.WithdrawalRequest;
import com.nestora.nestora_app.enums.WalletTransactionReason;
import com.nestora.nestora_app.enums.WithdrawalStatus;
import com.nestora.nestora_app.exception.AppException;
import com.nestora.nestora_app.repository.WithdrawalRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawalService {

    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final WalletService walletService;
    private final RazorpayXService razorpayXService;

    /**
     * User requests a withdrawal. Amount is deducted from wallet IMMEDIATELY (put "on hold")
     * so the same balance can't be requested twice while the first request is still pending.
     * If the payout later fails or is rejected, the amount is refunded back to the wallet.
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

    /**
     * ADMIN — Payment History screen. Returns ALL withdrawal requests (any status),
     * newest first. Pass a status to filter (e.g. only APPROVED to see completed payouts,
     * or only FAILED to see ones needing attention).
     */
    @Transactional(readOnly = true)
    public List<WithdrawalResponse> getAllWithdrawals(WithdrawalStatus statusFilter) {
        List<WithdrawalRequest> withdrawals = (statusFilter == null)
                ? withdrawalRequestRepository.findAllByOrderByRequestedAtDesc()
                : withdrawalRequestRepository.findByStatusOrderByRequestedAtDesc(statusFilter);

        return withdrawals.stream()
                .map(this::toAdminResponse)
                .collect(Collectors.toList());
    }

    /**
     * ADMIN clicks "Approve" — this now ACTUALLY sends the money via RazorpayX UPI payout.
     * No manual UPI app, no manually typing a UTR — the reference number comes back
     * automatically from Razorpay once the transfer is confirmed.
     */
    @Transactional
    public WithdrawalResponse approveWithdrawal(User admin, Long withdrawalId) {
        WithdrawalRequest withdrawal = withdrawalRequestRepository.findById(withdrawalId)
                .orElseThrow(() -> new AppException("Withdrawal request not found", HttpStatus.NOT_FOUND));

        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new AppException("Only pending requests can be approved", HttpStatus.BAD_REQUEST);
        }

        // Step 1 + 2: create a RazorpayX contact + fund account (UPI VPA) for this payout
        String contactId = razorpayXService.createContact(withdrawal.getUser());
        String fundAccountId = razorpayXService.createFundAccount(contactId, withdrawal.getUpiId());

        withdrawal.setRazorpayXContactId(contactId);
        withdrawal.setRazorpayXFundAccountId(fundAccountId);

        // Step 3: fire the actual payout
        PayoutResult result = razorpayXService.initiatePayout(fundAccountId, withdrawal.getAmount(), withdrawal.getId());

        withdrawal.setRazorpayXPayoutId(result.getPayoutId());
        withdrawal.setProcessedBy(admin);
        withdrawal.setProcessedAt(LocalDateTime.now());

        applyPayoutResultToWithdrawal(withdrawal, result);

        withdrawal = withdrawalRequestRepository.save(withdrawal);
        log.info("Payout triggered for withdrawal {}: razorpayx status={}", withdrawal.getId(), result.getStatus());

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
        withdrawal.setProcessedAt(LocalDateTime.now());
        withdrawal.setProcessedBy(admin);
        withdrawal = withdrawalRequestRepository.save(withdrawal);

        walletService.credit(
                withdrawal.getUser(),
                withdrawal.getAmount(),
                WalletTransactionReason.WITHDRAWAL_REJECTED_REFUND,
                withdrawal.getId(),
                "Withdrawal rejected: " + reason + " — amount refunded"
        );

        return toResponse(withdrawal);
    }

    /**
     * Called by the RazorpayX webhook when a payout's status changes asynchronously
     * (e.g. it was "queued"/"processing" at approve-time and only confirms later).
     * Runs in its OWN transaction (REQUIRES_NEW) — webhook delivery/retry from Razorpay
     * must never be affected by unrelated transaction state in the caller.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePayoutWebhookEvent(String payoutId, String eventStatus, String utr, String failureReason) {
        WithdrawalRequest withdrawal = withdrawalRequestRepository
                .findByRazorpayXPayoutId(payoutId)
                .orElse(null);

        if (withdrawal == null) {
            log.warn("Webhook received for unknown payout_id: {}", payoutId);
            return;
        }

        // Idempotency: if we already marked this as APPROVED or FAILED, don't process again
        if (withdrawal.getStatus() == WithdrawalStatus.APPROVED || withdrawal.getStatus() == WithdrawalStatus.FAILED) {
            log.info("Webhook for payout {} ignored — already in terminal state {}", payoutId, withdrawal.getStatus());
            return;
        }

        PayoutResult result = PayoutResult.builder()
                .payoutId(payoutId)
                .status(eventStatus)
                .utr(utr)
                .failureReason(failureReason)
                .build();

        applyPayoutResultToWithdrawal(withdrawal, result);
        withdrawalRequestRepository.save(withdrawal);
        log.info("Webhook updated withdrawal {} to status {}", withdrawal.getId(), withdrawal.getStatus());
    }

    /** Maps a RazorpayX payout status onto our WithdrawalStatus, refunding the wallet on failure. */
    private void applyPayoutResultToWithdrawal(WithdrawalRequest withdrawal, PayoutResult result) {
        switch (result.getStatus()) {
            case "processed" -> {
                withdrawal.setStatus(WithdrawalStatus.APPROVED);
                withdrawal.setTransactionRef(result.getUtr());
            }
            case "queued", "pending", "processing" -> {
                withdrawal.setStatus(WithdrawalStatus.PROCESSING);
            }
            case "rejected", "cancelled", "reversed" -> {
                withdrawal.setStatus(WithdrawalStatus.FAILED);
                withdrawal.setFailureReason(result.getFailureReason() != null
                        ? result.getFailureReason() : "Payout was " + result.getStatus() + " by RazorpayX");

                // money never reached the user — refund it back to their wallet automatically
                walletService.credit(
                        withdrawal.getUser(),
                        withdrawal.getAmount(),
                        WalletTransactionReason.WITHDRAWAL_REJECTED_REFUND,
                        withdrawal.getId(),
                        "Payout failed (" + result.getStatus() + ") — amount refunded automatically"
                );

                // OPTIONAL: notify user via Module 8 —
                // notificationService.send(withdrawal.getUser(), "Payout failed",
                //     "Your withdrawal failed and ₹" + withdrawal.getAmount() + " was refunded to your wallet.",
                //     "PAYMENT", withdrawal.getId());
            }
            default -> log.warn("Unrecognized RazorpayX payout status: {}", result.getStatus());
        }
    }

    private WithdrawalResponse toResponse(WithdrawalRequest w) {
        return WithdrawalResponse.builder()
                .withdrawalId(w.getId())
                .amount(w.getAmount())
                .upiId(w.getUpiId())
                .status(w.getStatus().name())
                .transactionRef(w.getTransactionRef())
                .failureReason(w.getFailureReason())
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