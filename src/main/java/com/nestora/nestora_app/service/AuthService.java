package com.nestora.nestora_app.service;

import com.nestora.nestora_app.dto.request.ForgotPasswordRequest;
import com.nestora.nestora_app.dto.request.LoginRequest;
import com.nestora.nestora_app.dto.request.RegisterRequest;
import com.nestora.nestora_app.dto.request.ResetPasswordRequest;
import com.nestora.nestora_app.dto.request.VerifyOtpRequest;
import com.nestora.nestora_app.dto.response.AuthResponse;
import com.nestora.nestora_app.entity.OtpVerification;
import com.nestora.nestora_app.entity.OtpVerification.OtpType;
import com.nestora.nestora_app.entity.User;
import com.nestora.nestora_app.enums.Role;
import com.nestora.nestora_app.exception.AppException;
import com.nestora.nestora_app.repository.OtpVerificationRepository;
import com.nestora.nestora_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final OtpVerificationRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final SmsService smsService;

    @Value("${otp.expiry.minutes}")
    private int otpExpiryMinutes;

    // =============================================
    // REGISTER
    // =============================================
    @Transactional
    public String register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException("Email already registered", HttpStatus.CONFLICT);
        }

        if (request.getPhone() != null &&
                userRepository.existsByPhone(request.getPhone())) {
            throw new AppException(
                    "Phone number already registered", HttpStatus.CONFLICT
            );
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail().toLowerCase().trim())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.STUDENT)
                .isActive(false)
                .isEmailVerified(false)
                .build();

        User saved = userRepository.save(user);

        saved.setDisplayId(generateDisplayId(saved.getId()));
        userRepository.save(saved);

        sendOtp(
                saved.getEmail(),
                saved.getPhone(),
                OtpType.REGISTER
        );

        return "OTP sent to " + request.getEmail() +
                ". Please verify to complete registration.";
    }

    // =============================================
    // VERIFY OTP - ✅ FIXED: userId added
    // =============================================
    @Transactional
    public AuthResponse verifyRegisterOtp(VerifyOtpRequest request) {

        String email = request.getEmail().toLowerCase().trim();

        validateOtp(email, request.getOtp(), OtpType.REGISTER);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(
                        "User not found", HttpStatus.NOT_FOUND
                ));

        user.setIsActive(true);
        user.setIsEmailVerified(true);
        userRepository.save(user);

        // ✅ FIXED: userId pass karo
        String accessToken = jwtService.generateAccessToken(user, user.getId());
        String refreshToken = jwtService.generateRefreshToken(user, user.getId());

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    // =============================================
    // LOGIN - ✅ FIXED: userId added
    // =============================================
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(
                        "Invalid email or password", HttpStatus.UNAUTHORIZED
                ));

        if (user.getAccountLockedUntil() != null &&
                user.getAccountLockedUntil().isAfter(LocalDateTime.now())) {

            long minutesLeft = ChronoUnit.MINUTES.between(
                    LocalDateTime.now(), user.getAccountLockedUntil()
            );

            throw new AppException(
                    "Account locked due to too many failed attempts. " +
                            "Try again after " + minutesLeft + " minutes.",
                    HttpStatus.FORBIDDEN
            );
        }

        if (!user.getIsActive()) {
            throw new AppException(
                    "Account not verified. Please verify your email first.",
                    HttpStatus.FORBIDDEN
            );
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            int attempts = (user.getFailedLoginAttempts() == null)
                    ? 0 : user.getFailedLoginAttempts();
            attempts++;
            user.setFailedLoginAttempts(attempts);

            if (attempts >= 5) {
                user.setAccountLockedUntil(
                        LocalDateTime.now().plusMinutes(30)
                );
                user.setFailedLoginAttempts(0);
                userRepository.save(user);
                throw new AppException(
                        "Too many failed attempts. Account locked for 30 minutes.",
                        HttpStatus.FORBIDDEN
                );
            }

            userRepository.save(user);
            throw new AppException(
                    "Invalid email or password. " +
                            (5 - attempts) + " attempts remaining.",
                    HttpStatus.UNAUTHORIZED
            );
        }

        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);

        // ✅ FIXED: userId pass karo
        String accessToken = jwtService.generateAccessToken(user, user.getId());
        String refreshToken = jwtService.generateRefreshToken(user, user.getId());

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    // =============================================
    // FORGOT PASSWORD
    // =============================================
    @Transactional
    public String forgotPassword(ForgotPasswordRequest request) {

        String email = request.getEmail().toLowerCase().trim();

        if (!userRepository.existsByEmail(email)) {
            return "If this email is registered, you will receive an OTP.";
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(
                        "User not found", HttpStatus.NOT_FOUND
                ));

        sendOtp(email, user.getPhone(), OtpType.FORGOT_PASSWORD);

        return "OTP sent to your email for password reset.";
    }

    // =============================================
    // RESET PASSWORD
    // =============================================
    @Transactional
    public String resetPassword(ResetPasswordRequest request) {

        String email = request.getEmail().toLowerCase().trim();

        validateOtp(email, request.getOtp(), OtpType.FORGOT_PASSWORD);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(
                        "User not found", HttpStatus.NOT_FOUND
                ));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return "Password reset successfully. Please login with your new password.";
    }

    // =============================================
    // REFRESH TOKEN - ✅ FIXED: userId added
    // =============================================
    public AuthResponse refreshToken(String refreshToken) {

        String email = jwtService.extractUsername(refreshToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(
                        "User not found", HttpStatus.NOT_FOUND
                ));

        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new AppException(
                    "Invalid or expired refresh token",
                    HttpStatus.UNAUTHORIZED
            );
        }

        // ✅ FIXED: userId pass karo
        String newAccessToken = jwtService.generateAccessToken(user, user.getId());
        String newRefreshToken = jwtService.generateRefreshToken(user, user.getId());

        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    // =============================================
    // RESEND OTP
    // =============================================
    @Transactional
    public String resendOtp(ForgotPasswordRequest request, String type) {

        String email = request.getEmail().toLowerCase().trim();

        if (!userRepository.existsByEmail(email)) {
            throw new AppException("Email not registered", HttpStatus.NOT_FOUND);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(
                        "User not found", HttpStatus.NOT_FOUND
                ));

        OtpType otpType = type.equals("REGISTER") ?
                OtpType.REGISTER : OtpType.FORGOT_PASSWORD;

        sendOtp(email, user.getPhone(), otpType);

        return "OTP resent successfully.";
    }

    // =============================================
    // PRIVATE HELPERS
    // =============================================

    private void sendOtp(String email, String phone, OtpType type) {

        otpRepository.deleteAllByEmailAndType(email, type);

        String otp = generateOtp();

        OtpVerification otpVerification = OtpVerification.builder()
                .email(email)
                .otp(otp)
                .type(type)
                .isUsed(false)
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .build();

        otpRepository.save(otpVerification);

        emailService.sendOtpEmail(email, otp, type.name());

        if (phone != null && !phone.isEmpty()) {
            smsService.sendOtpSms(phone, otp);
        }
    }

    private void validateOtp(String email, String otp, OtpType type) {

        OtpVerification otpRecord = otpRepository
                .findTopByEmailAndTypeAndIsUsedFalseOrderByCreatedAtDesc(
                        email, type
                )
                .orElseThrow(() -> new AppException(
                        "OTP not found. Please request a new OTP.",
                        HttpStatus.BAD_REQUEST
                ));

        if (otpRecord.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(
                    "OTP has expired. Please request a new OTP.",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (!otpRecord.getOtp().equals(otp)) {
            throw new AppException(
                    "Invalid OTP. Please try again.",
                    HttpStatus.BAD_REQUEST
            );
        }

        otpRecord.setIsUsed(true);
        otpRepository.save(otpRecord);
    }

    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    private String generateDisplayId(Long userId) {
        return "NST-" + String.format("%06d", userId);
    }

    private AuthResponse buildAuthResponse(User user,
                                           String accessToken,
                                           String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .role(user.getRole().name())
                .userId(user.getId())
                .displayId(user.getDisplayId())
                .name(user.getName())
                .email(user.getEmail())
                .isEmailVerified(user.getIsEmailVerified())
                .build();
    }
}