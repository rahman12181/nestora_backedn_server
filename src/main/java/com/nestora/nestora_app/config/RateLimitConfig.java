package com.nestora.nestora_app.config;


import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitConfig {

    // IP wise bucket store karo
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    // Login — 5 attempts per minute
    public Bucket getLoginBucket(String ip) {
        return buckets.computeIfAbsent(
                "login:" + ip,
                k -> Bucket.builder()
                        .addLimit(Bandwidth.classic(
                                5, Refill.greedy(5, Duration.ofMinutes(1))
                        ))
                        .build()
        );
    }

    // Register — 3 attempts per minute
    public Bucket getRegisterBucket(String ip) {
        return buckets.computeIfAbsent(
                "register:" + ip,
                k -> Bucket.builder()
                        .addLimit(Bandwidth.classic(
                                3, Refill.greedy(3, Duration.ofMinutes(1))
                        ))
                        .build()
        );
    }

    // OTP — 3 attempts per 5 minutes
    public Bucket getOtpBucket(String ip) {
        return buckets.computeIfAbsent(
                "otp:" + ip,
                k -> Bucket.builder()
                        .addLimit(Bandwidth.classic(
                                3, Refill.greedy(3, Duration.ofMinutes(5))
                        ))
                        .build()
        );
    }

    // General API — 100 requests per minute
    public Bucket getGeneralBucket(String ip) {
        return buckets.computeIfAbsent(
                "general:" + ip,
                k -> Bucket.builder()
                        .addLimit(Bandwidth.classic(
                                100, Refill.greedy(100, Duration.ofMinutes(1))
                        ))
                        .build()
        );
    }
}
