package com.nestora.nestora_app.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@Slf4j
public class SmsService {

    @Value("${msg91.auth.key}")
    private String authKey;

    @Value("${msg91.template.id}")
    private String templateId;

    @Value("${msg91.sender.id}")
    private String senderId;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.msg91.com")
            .build();

    public void sendOtpSms(String phone, String otp) {
        try {
            // MSG91 OTP API
            String response = webClient.post()
                    .uri("/api/v5/otp")
                    .header("authkey", authKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(Map.of(
                            "template_id", templateId,
                            "mobile", "91" + phone, // India code
                            "otp", otp
                    ))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("SMS sent to {}: {}", phone, response);

        } catch (Exception e) {
            log.error("SMS send failed to {}: {}", phone, e.getMessage());
            // SMS fail hone pe app crash mat karo
        }
    }
}