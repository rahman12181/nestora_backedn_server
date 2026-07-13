package com.nestora.nestora_app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nestora.nestora_app.config.RazorpayXConfig;
import com.nestora.nestora_app.dto.response.PayoutResult;
import com.nestora.nestora_app.entity.User;
import com.nestora.nestora_app.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Talks to the RazorpayX API to actually move money to a user's UPI ID.
 * Docs: https://razorpay.com/docs/x/payout-links/ and /v1/payouts API reference.
 *
 * Flow for every withdrawal: Contact -> Fund Account (their UPI VPA) -> Payout.
 * A fresh Contact + Fund Account is created per withdrawal for simplicity — RazorpayX
 * allows this without issue; it just means you'll see one contact per payout in their
 * dashboard rather than one contact per user. Fine for this scale.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RazorpayXService {

    private static final String BASE_URL = "https://api.razorpay.com/v1";

    private final RazorpayXConfig config;
    private final RestTemplate razorpayXRestTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private HttpHeaders authHeaders() {
        String credentials = config.getKeyId() + ":" + config.getKeySecret();
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encoded);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /** Step 1 — create a RazorpayX Contact representing the person being paid. */
    public String createContact(User user) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", user.getName());
        body.put("email", user.getEmail());
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            body.put("contact", user.getPhone());
        }
        body.put("type", "customer");
        body.put("reference_id", "user_" + user.getId());

        JsonNode response = post("/contacts", body, "Failed to create payout contact");
        return response.get("id").asText();
    }

    /** Step 2 — attach their UPI ID (VPA) as a Fund Account under that Contact. */
    public String createFundAccount(String contactId, String upiId) {
        Map<String, Object> vpa = new HashMap<>();
        vpa.put("address", upiId);

        Map<String, Object> body = new HashMap<>();
        body.put("contact_id", contactId);
        body.put("account_type", "vpa");
        body.put("vpa", vpa);

        JsonNode response = post("/fund_accounts", body, "Failed to link UPI ID for payout");
        return response.get("id").asText();
    }

    /**
     * Step 3 — actually send the money. `amount` is in rupees; RazorpayX wants paise.
     * `referenceId` should be your internal WithdrawalRequest.id so it shows up in
     * Razorpay's dashboard and can be reconciled later.
     */
    public PayoutResult initiatePayout(String fundAccountId, BigDecimal amount, Long withdrawalRequestId) {
        long amountInPaise = amount.multiply(BigDecimal.valueOf(100)).longValueExact();

        Map<String, Object> body = new HashMap<>();
        body.put("account_number", config.getAccountNumber());
        body.put("fund_account_id", fundAccountId);
        body.put("amount", amountInPaise);
        body.put("currency", "INR");
        body.put("mode", "UPI");
        body.put("purpose", "payout");
        body.put("queue_if_low_balance", true);
        body.put("reference_id", "withdrawal_" + withdrawalRequestId);
        body.put("narration", "Nestora Refer & Earn payout");

        JsonNode response = post("/payouts", body, "Payout request failed. Please try again or contact support.");

        String payoutId = response.get("id").asText();
        String status = response.get("status").asText(); // queued / pending / processing / processed / rejected / cancelled
        String utr = response.has("utr") && !response.get("utr").isNull() ? response.get("utr").asText() : null;

        return PayoutResult.builder()
                .payoutId(payoutId)
                .status(status)
                .utr(utr)
                .build();
    }

    private JsonNode post(String path, Map<String, Object> body, String errorMessage) {
        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, authHeaders());
            ResponseEntity<String> response = razorpayXRestTemplate.postForEntity(
                    BASE_URL + path, entity, String.class);
            return objectMapper.readTree(response.getBody());
        } catch (RestClientException e) {
            log.error("RazorpayX API call to {} failed: {}", path, e.getMessage());
            throw new AppException(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("Failed to parse RazorpayX response from {}: {}", path, e.getMessage());
            throw new AppException(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** Verifies the X-Razorpay-Signature header on incoming webhook calls. */
    public boolean verifyWebhookSignature(String payload, String signatureHeader) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
                    config.getWebhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString().equals(signatureHeader);
        } catch (Exception e) {
            log.error("Webhook signature verification failed: {}", e.getMessage());
            return false;
        }
    }
}