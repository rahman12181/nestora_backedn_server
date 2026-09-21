package com.nestora.nestora_app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Standard Razorpay Payment Gateway credentials — used to COLLECT rent payments from
 * students. These are DIFFERENT keys from RazorpayX (RazorpayXConfig) which is used to
 * PAY OUT to owners. If you already configured Razorpay for owner subscriptions
 * (Module 3/11), you likely already have these same values somewhere — just reuse them:
 *
 *   razorpay.payment.key-id=rzp_live_xxxxxxxxxxxx
 *   razorpay.payment.key-secret=xxxxxxxxxxxxxxxxxxxx
 */
@Configuration
public class RazorpayPaymentConfig {

    @Value("${razorpay.key.id:}")
    private String keyId;

    @Value("${razorpay.key.secret:}")
    private String keySecret;

    public String getKeyId() { return keyId; }
    public String getKeySecret() { return keySecret; }

    @Bean
    public RestTemplate razorpayPaymentRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(15000);
        return new RestTemplate(factory);
    }
}