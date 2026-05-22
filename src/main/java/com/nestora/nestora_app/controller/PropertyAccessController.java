package com.nestora.nestora_app.controller;

import com.nestora.nestora_app.dto.request.PropertyAccessBuyRequest;
import com.nestora.nestora_app.dto.request.PropertyAccessConfirmRequest;
import com.nestora.nestora_app.dto.response.*;
import com.nestora.nestora_app.entity.User;
import com.nestora.nestora_app.service.PropertyAccessSubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/owner/property-access")
@RequiredArgsConstructor
public class PropertyAccessController {

    private final PropertyAccessSubscriptionService propertyAccessService;

    // POST /owner/property-access/buy
    @PostMapping("/buy")
    public ResponseEntity<ApiResponse<PropertyAccessOrderResponse>> buySubscription(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody PropertyAccessBuyRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Order created. Complete payment to activate property access.",
                        propertyAccessService.buySubscription(currentUser, request))
        );
    }

    // POST /owner/property-access/confirm
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<PropertyAccessConfirmResponse>> confirmSubscription(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody PropertyAccessConfirmRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Property access subscription activated! now you can add the propertieswh.",
                        propertyAccessService.confirmSubscription(currentUser, request))
        );
    }

    // GET /owner/property-access/status
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<PropertyAccessStatusResponse>> getStatus(
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Property access subscription details fetched",
                        propertyAccessService.getStatus(currentUser))
        );
    }

    // GET /owner/property-access/history
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<PropertyAccessHistoryResponse>>> getHistory(
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Subscription history fetched",
                        propertyAccessService.getHistory(currentUser))
        );
    }

    // POST /owner/property-access/renew
    @PostMapping("/renew")
    public ResponseEntity<ApiResponse<PropertyAccessOrderResponse>> renewSubscription(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody PropertyAccessBuyRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Renewal order created. Complete payment to extend your property access.",
                        propertyAccessService.renewSubscription(currentUser, request))
        );
    }
}