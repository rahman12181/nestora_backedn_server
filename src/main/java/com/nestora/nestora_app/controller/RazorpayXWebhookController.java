package com.nestora.nestora_app.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nestora.nestora_app.service.RazorpayXService;
import com.nestora.nestora_app.service.WithdrawalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Configure this URL in your Razorpay Dashboard -> RazorpayX -> Webhooks:
 *   https://api.nestora.in/webhooks/razorpayx/payout
 * Subscribe to events: payout.processed, payout.failed, payout.reversed, payout.rejected
 *
 * IMPORTANT: this endpoint MUST be public (no JWT auth) — add it to your permitAll()
 * list in SecurityConfig: "/webhooks/**"
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class RazorpayXWebhookController {

    private final RazorpayXService razorpayXService;
    private final WithdrawalService withdrawalService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/webhooks/razorpayx/payout")
    public ResponseEntity<String> handlePayoutWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        if (signature == null || !razorpayXService.verifyWebhookSignature(rawPayload, signature)) {
            log.warn("RazorpayX webhook rejected: invalid or missing signature");
            return ResponseEntity.status(400).body("Invalid signature");
        }

        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            String event = root.get("event").asText(); // e.g. "payout.processed"
            JsonNode payoutEntity = root.path("payload").path("payout").path("entity");

            String payoutId = payoutEntity.get("id").asText();
            String status = payoutEntity.get("status").asText(); // processed / rejected / reversed / etc.
            String utr = payoutEntity.has("utr") && !payoutEntity.get("utr").isNull()
                    ? payoutEntity.get("utr").asText() : null;
            String failureReason = payoutEntity.has("failure_reason") && !payoutEntity.get("failure_reason").isNull()
                    ? payoutEntity.get("failure_reason").asText() : null;

            log.info("RazorpayX webhook received: event={}, payoutId={}, status={}", event, payoutId, status);

            withdrawalService.handlePayoutWebhookEvent(payoutId, status, utr, failureReason);

        } catch (Exception e) {
            log.error("Failed to process RazorpayX webhook payload: {}", e.getMessage());
            // Still return 200 so Razorpay doesn't endlessly retry a malformed payload we can't parse
        }

        // Always respond 200 quickly — Razorpay expects fast acknowledgement
        return ResponseEntity.ok("OK");
    }
}