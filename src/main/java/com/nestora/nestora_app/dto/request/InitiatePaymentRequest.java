package com.nestora.nestora_app.dto.request;

import lombok.Data;

@Data
public class InitiatePaymentRequest {
    // optional — e.g. "FIRST20". If omitted, backend still auto-applies FIRST20 when eligible
    // (see README) — pass it explicitly if you want the student to type it in themselves.
    private String couponCode;
}