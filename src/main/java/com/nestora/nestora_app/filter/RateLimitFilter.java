package com.nestora.nestora_app.filter;


import com.nestora.nestora_app.config.RateLimitConfig;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitConfig rateLimitConfig;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String ip = getClientIP(request);
        String path = request.getRequestURI();

        Bucket bucket;

        if (path.contains("/auth/login")) {
            bucket = rateLimitConfig.getLoginBucket(ip);
        } else if (path.contains("/auth/register")) {
            bucket = rateLimitConfig.getRegisterBucket(ip);
        } else if (path.contains("/auth/verify-otp") ||
                path.contains("/auth/resend-otp")) {
            bucket = rateLimitConfig.getOtpBucket(ip);
        } else {
            bucket = rateLimitConfig.getGeneralBucket(ip);
        }

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Remaining requests header me bhejo
            response.addHeader(
                    "X-Rate-Limit-Remaining",
                    String.valueOf(probe.getRemainingTokens())
            );
            filterChain.doFilter(request, response);
        } else {
            // Too Many Requests
            long waitSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Too many requests. Please wait " +
                            waitSeconds + " seconds.\"}"
            );
            log.warn("Rate limit exceeded for IP: {} on path: {}", ip, path);
        }
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
