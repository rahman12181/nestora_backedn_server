package com.nestora.nestora_app.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendOtpEmail(String toEmail, String otp, String purpose) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);

            if (purpose.equals("REGISTER")) {
                message.setSubject("Nestora — Verify Your Email");
                message.setText(
                        "Welcome to Nestora!\n\n" +
                                "Your OTP for email verification is:\n\n" +
                                "  " + otp + "\n\n" +
                                "This OTP is valid for 10 minutes.\n" +
                                "Do not share this OTP with anyone.\n\n" +
                                "Team Nestora"
                );
            } else {
                message.setSubject("Nestora — Password Reset OTP");
                message.setText(
                        "Hi,\n\n" +
                                "Your OTP for password reset is:\n\n" +
                                "  " + otp + "\n\n" +
                                "This OTP is valid for 10 minutes.\n" +
                                "If you didn't request this, ignore this email.\n\n" +
                                "Team Nestora"
                );
            }

            mailSender.send(message);
            log.info("OTP email sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send OTP email. Please try again.");
        }
    }

    public void sendSubscriptionReminderEmail(String toEmail,
                                              String name,
                                              int daysLeft,
                                              String plan) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Nestora — Subscription Expiring in " + daysLeft + " Days");
            message.setText(
                    "Hi " + name + ",\n\n" +
                            "Your " + plan + " subscription is expiring in " + daysLeft + " days.\n\n" +
                            "Renew now to keep your property listings visible to students.\n\n" +
                            "Renew here: https://app.nestora.in/subscription\n\n" +
                            "Team Nestora"
            );
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Reminder email failed: {}", e.getMessage());
        }
    }
}
