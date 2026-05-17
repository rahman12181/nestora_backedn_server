package com.nestora.nestora_app.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@Slf4j
public class SmsService {

    @Value("${msg91.auth.key:test_key}")
    private String authKey;

    @Value("${msg91.template.id:test_template}")
    private String templateId;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.msg91.com")
            .build();

    public void sendOtpSms(String phone, String otp) {
        if (phone == null || phone.isEmpty()) return;

        try {
            String response = webClient.post()
                    .uri("/api/v5/otp")
                    .header("authkey", authKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(Map.of(
                            "template_id", templateId,
                            "mobile", "91" + phone,
                            "otp", otp
                    ))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("SMS sent to {}: {}", phone, response);

        } catch (Exception e) {
            // SMS fail hone pe app crash nahi hoga
            log.error("SMS send failed to {}: {}", phone, e.getMessage());
        }
    }
}