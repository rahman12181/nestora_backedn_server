package com.nestora.nestora_app.service;

import com.nestora.nestora_app.dto.response.ReferralHistoryItemResponse;
import com.nestora.nestora_app.dto.response.ReferralInfoResponse;
import com.nestora.nestora_app.entity.Referral;
import com.nestora.nestora_app.entity.User;
import com.nestora.nestora_app.enums.ReferralStatus;
import com.nestora.nestora_app.enums.WalletTransactionReason;
import com.nestora.nestora_app.exception.AppException;
import com.nestora.nestora_app.repository.ReferralRepository;
import com.nestora.nestora_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ASSUMPTION: UserRepository has (or you add) a method:
 *     Optional<User> findByDisplayId(String displayId);
 * This is how a referral code (= the referrer's displayId, e.g. "NST-000042") is resolved.
 * If it doesn't exist yet, add this one line to your UserRepository interface.
 *
 * INTEGRATION POINTS — hook these two methods into your existing AuthService:
 *   1. In register(): AFTER the new User is saved (before/while sending OTP),
 *          if (request.getReferralCode() != null && !request.getReferralCode().isBlank()) {
 *              referralService.createPendingReferral(newUser, request.getReferralCode());
 *          }
 *   2. In verifyOtp(): AFTER user.setIsEmailVerified(true) is saved,
 *          referralService.completeReferralReward(newUser);
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReferralService {

    public static final BigDecimal REFERRAL_REWARD_AMOUNT = new BigDecimal("19.00");

    private final ReferralRepository referralRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;

    /**
     * Step 1 — called at registration time. Validates the code and records a PENDING
     * referral. Money is NOT credited yet — that only happens after email verification,
     * so a fake/never-verified signup can never earn a reward.
     */
    @Transactional
    public void createPendingReferral(User newUser, String referralCode) {
        String code = referralCode.trim().toUpperCase();

        User referrer = userRepository.findByDisplayId(code)
                .orElseThrow(() -> new AppException("Invalid referral code", HttpStatus.BAD_REQUEST));

        if (referrer.getId().equals(newUser.getId())) {
            throw new AppException("You cannot refer yourself", HttpStatus.BAD_REQUEST);
        }

        // Safety net: a user can only ever be the "referredUser" once, no matter what.
        // (Also enforced at the DB level by a UNIQUE constraint on referred_user_id.)
        if (referralRepository.existsByReferredUser_Id(newUser.getId())) {
            log.warn("Duplicate referral attempt blocked for user {}", newUser.getId());
            return; // silently ignore instead of failing registration
        }

        Referral referral = Referral.builder()
                .referrer(referrer)
                .referredUser(newUser)
                .referralCode(code)
                .rewardAmount(REFERRAL_REWARD_AMOUNT)
                .status(ReferralStatus.PENDING)
                .build();

        referralRepository.save(referral);
        log.info("Pending referral created: referrer={}, referredUser={}", referrer.getId(), newUser.getId());
    }

    /**
     * Step 2 — called right after the referred user's email OTP is verified.
     * Idempotent: safe to call multiple times, only rewards once.
     */
    @Transactional
    public void completeReferralReward(User referredUser) {
        Optional<Referral> referralOpt = referralRepository.findByReferredUser_Id(referredUser.getId());
        if (referralOpt.isEmpty()) {
            return; // this user did not sign up via a referral code
        }

        Referral referral = referralOpt.get();
        if (referral.getStatus() != ReferralStatus.PENDING) {
            return; // already rewarded (or rejected) — do nothing, prevents double credit
        }

        walletService.credit(
                referral.getReferrer(),
                referral.getRewardAmount(),
                WalletTransactionReason.REFERRAL_BONUS,
                referral.getId(),
                "Referral bonus - " + referredUser.getName() + " joined using your code"
        );

        referral.setStatus(ReferralStatus.REWARDED);
        referral.setRewardedAt(java.time.LocalDateTime.now());
        referralRepository.save(referral);

        log.info("Referral reward credited: referrer={}, amount={}", referral.getReferrer().getId(), referral.getRewardAmount());

        // OPTIONAL: notify the referrer using your existing Notification module (Module 8).
        // notificationService.send(referral.getReferrer(), "🎉 You earned ₹19!",
        //     referredUser.getName() + " joined Nestora using your referral code!", "REFERRAL", referral.getId());
    }

    @Transactional(readOnly = true)
    public ReferralInfoResponse getReferralInfo(User currentUser) {
        List<Referral> referrals = referralRepository.findByReferrer_IdOrderByCreatedAtDesc(currentUser.getId());

        long rewardedCount = referrals.stream().filter(r -> r.getStatus() == ReferralStatus.REWARDED).count();
        BigDecimal totalEarned = referrals.stream()
                .filter(r -> r.getStatus() == ReferralStatus.REWARDED)
                .map(Referral::getRewardAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String code = currentUser.getDisplayId();

        return ReferralInfoResponse.builder()
                .referralCode(code)
                .referralLink("https://nestora.in/download?ref=" + code)
                .shareMessage("🏠 Nestora pe PG/Hostel dhundo — mera referral code use karo: " + code +
                        "\nDownload: https://nestora.in/download?ref=" + code)
                .totalReferred(referrals.size())
                .totalRewarded((int) rewardedCount)
                .totalEarned(totalEarned)
                .walletBalance(currentUser.getWalletBalance() == null ? BigDecimal.ZERO : currentUser.getWalletBalance())
                .rewardPerReferral(REFERRAL_REWARD_AMOUNT)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ReferralHistoryItemResponse> getReferralHistory(User currentUser) {
        return referralRepository.findByReferrer_IdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(r -> ReferralHistoryItemResponse.builder()
                        .referredUserName(r.getReferredUser().getName())
                        .referredUserDisplayId(r.getReferredUser().getDisplayId())
                        .status(r.getStatus().name())
                        .rewardAmount(r.getRewardAmount())
                        .joinedAt(r.getCreatedAt())
                        .rewardedAt(r.getRewardedAt())
                        .build())
                .collect(Collectors.toList());
    }
}