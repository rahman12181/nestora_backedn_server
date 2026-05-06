package com.nestora.nestora_app.service;

import com.nestora.nestora_app.dto.request.*;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    @Value("${otp.expiry.minutes}")
    private int otpExpiryMinutes;

    //register
    @Transactional
    public String register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException("Email already registered", HttpStatus.CONFLICT);
        }

        if (request.getPhone() != null &&
                userRepository.existsByPhone(request.getPhone())) {
            throw new AppException("Phone number already registered", HttpStatus.CONFLICT);
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

        // CHANGE: request.getEmail() ki jagah saved.getEmail() use karo
        sendOtp(saved.getEmail(), OtpType.REGISTER);

        return "OTP sent to " + request.getEmail() + ". Please verify to complete registration.";
    }

    //verify otp
    @Transactional
    public AuthResponse verifyRegisterOtp(VerifyOtpRequest request) {

        String email = request.getEmail().toLowerCase().trim();

        validateOtp(email, request.getOtp(), OtpType.REGISTER);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        user.setIsActive(true);
        user.setIsEmailVerified(true); // NAYA — yeh add karo
        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    //login
    public AuthResponse login(LoginRequest request) {

        // Pehle check karo user active hai ya nahi
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(
                        "Invalid email or password", HttpStatus.UNAUTHORIZED
                ));

        if (!user.getIsActive()) {
            throw new AppException(
                    "Account not verified. Please verify your email first.",
                    HttpStatus.FORBIDDEN
            );
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    //forget password
    @Transactional
    public String forgotPassword(ForgotPasswordRequest request) {

        String email = request.getEmail().toLowerCase().trim();

        // Email exist karta hai?
        if (!userRepository.existsByEmail(email)) {
            // Security best practice — same message dono cases me
            return "If this email is registered, you will receive an OTP.";
        }

        sendOtp(email, OtpType.FORGOT_PASSWORD);

        return "OTP sent to your email for password reset.";
    }

    //reset password
    @Transactional
    public String resetPassword(ResetPasswordRequest request) {

        String email = request.getEmail().toLowerCase().trim();

        // OTP validate karo
        validateOtp(email, request.getOtp(), OtpType.FORGOT_PASSWORD);

        // Password update karo
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return "Password reset successfully. Please login with your new password.";
    }

    //refresh token
    public AuthResponse refreshToken(String refreshToken) {

        String email = jwtService.extractUsername(refreshToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new AppException(
                    "Invalid or expired refresh token", HttpStatus.UNAUTHORIZED
            );
        }

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    //resend otp
    @Transactional
    public String resendOtp(ForgotPasswordRequest request, String type) {

        String email = request.getEmail().toLowerCase().trim();

        if (!userRepository.existsByEmail(email)) {
            throw new AppException("Email not registered", HttpStatus.NOT_FOUND);
        }

        OtpType otpType = type.equals("REGISTER") ?
                OtpType.REGISTER : OtpType.FORGOT_PASSWORD;

        sendOtp(email, otpType);

        return "OTP resent successfully.";
    }



    private void sendOtp(String email, OtpType type) {
        // Purane OTP delete karo
        otpRepository.deleteAllByEmailAndType(email, type);

        // Naya OTP generate karo
        String otp = generateOtp();

        // DB me save karo
        OtpVerification otpVerification = OtpVerification.builder()
                .email(email)
                .otp(otp)
                .type(type)
                .isUsed(false)
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .build();

        otpRepository.save(otpVerification);

        //send email
        emailService.sendOtpEmail(email, otp, type.name());
    }

    private void validateOtp(String email, String otp, OtpType type) {

        OtpVerification otpRecord = otpRepository
                .findTopByEmailAndTypeAndIsUsedFalseOrderByCreatedAtDesc(email, type)
                .orElseThrow(() -> new AppException(
                        "OTP not found. Please request a new OTP.", HttpStatus.BAD_REQUEST
                ));

        // Expire ho gaya?
        if (otpRecord.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(
                    "OTP has expired. Please request a new OTP.", HttpStatus.BAD_REQUEST
            );
        }

        // Wrong OTP?
        if (!otpRecord.getOtp().equals(otp)) {
            throw new AppException("Invalid OTP. Please try again.", HttpStatus.BAD_REQUEST);
        }

        // Mark as used
        otpRecord.setIsUsed(true);
        otpRepository.save(otpRecord);
    }

    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
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
                .displayId(user.getDisplayId())            // NAYA
                .name(user.getName())
                .email(user.getEmail())
                .isEmailVerified(user.getIsEmailVerified()) // NAYA
                .build();
    }
    private String generateDisplayId(Long userId) {
        return "NST-" + String.format("%06d", userId);
    }
}