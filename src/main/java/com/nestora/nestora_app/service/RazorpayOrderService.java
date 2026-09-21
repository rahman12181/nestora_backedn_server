package com.nestora.nestora_app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nestora.nestora_app.config.RazorpayPaymentConfig;
import com.nestora.nestora_app.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles the "collect money FROM student" side using standard Razorpay Orders
 * (same product you already use for owner subscriptions in Module 3/11 — this is a
 * separate, self-contained implementation for rent payments so it doesn't depend on
 * whatever existing Razorpay service class you may already have).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RazorpayOrderService {

    private static final String BASE_URL = "https://api.razorpay.com/v1";

    private final RazorpayPaymentConfig config;
    private final RestTemplate razorpayPaymentRestTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private HttpHeaders authHeaders() {
        String credentials = config.getKeyId() + ":" + config.getKeySecret();
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encoded);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /** Creates a Razorpay Order for the given rupee amount. Returns the order_id. */
    public String createOrder(BigDecimal amountInRupees, String receiptId) {
        long amountInPaise = amountInRupees.multiply(BigDecimal.valueOf(100)).longValueExact();

        Map<String, Object> body = new HashMap<>();
        body.put("amount", amountInPaise);
        body.put("currency", "INR");
        body.put("receipt", receiptId);

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, authHeaders());
            ResponseEntity<String> response = razorpayPaymentRestTemplate.postForEntity(
                    BASE_URL + "/orders", entity, String.class);
            JsonNode json = objectMapper.readTree(response.getBody());
            return json.get("id").asText();
        } catch (RestClientException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage());
            throw new AppException("Failed to create payment order. Please try again.", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("Failed to parse Razorpay order response: {}", e.getMessage());
            throw new AppException("Failed to create payment order. Please try again.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Verifies the payment signature Razorpay Checkout returns after a successful payment.
     * Formula (per Razorpay docs): HMAC-SHA256(order_id + "|" + payment_id, key_secret)
     * must equal the signature sent back.
     */
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(config.getKeySecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));

            return hex.toString().equals(signature);
        } catch (Exception e) {
            log.error("Payment signature verification failed: {}", e.getMessage());
            return false;
        }
    }
}