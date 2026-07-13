package com.nestora.nestora_app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RazorpayX credentials — set these in application.properties (or better, as
 * environment variables in production, never commit real keys to git):
 *
 *   razorpayx.key-id=rzp_live_xxxxxxxxxxxx
 *   razorpayx.key-secret=xxxxxxxxxxxxxxxxxxxx
 *   razorpayx.account-number=xxxxxxxxxxxx      (your RazorpayX current account number)
 *   razorpayx.webhook-secret=xxxxxxxxxxxxxxxx  (set this same secret in Razorpay Dashboard webhook config)
 *
 * You get all 4 values only AFTER RazorpayX approves your business/current account —
 * this is a business KYC step on Razorpay's dashboard, not something done in code.
 */
@Configuration
public class RazorpayXConfig {

    @Value("${razorpayx.key-id:}")
    private String keyId;

    @Value("${razorpayx.key-secret:}")
    private String keySecret;

    @Value("${razorpayx.account-number:}")
    private String accountNumber;

    @Value("${razorpayx.webhook-secret:}")
    private String webhookSecret;

    public String getKeyId() { return keyId; }
    public String getKeySecret() { return keySecret; }
    public String getAccountNumber() { return accountNumber; }
    public String getWebhookSecret() { return webhookSecret; }

    @Bean
    public RestTemplate razorpayXRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(15000);
        return new RestTemplate(factory);
    }
}