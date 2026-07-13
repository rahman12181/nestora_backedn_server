package com.nestora.nestora_app.controller;

import com.nestora.nestora_app.dto.response.ApiResponse;
import com.nestora.nestora_app.dto.response.ReferralHistoryItemResponse;
import com.nestora.nestora_app.dto.response.ReferralInfoResponse;
import com.nestora.nestora_app.entity.User;
import com.nestora.nestora_app.service.ReferralService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralService referralService;

    // 13.1 Get My Referral Info (code, link, stats, wallet balance)
    @GetMapping("/user/referral/info")
    public ResponseEntity<ApiResponse<ReferralInfoResponse>> getReferralInfo(
            @AuthenticationPrincipal User currentUser) {

        ReferralInfoResponse info = referralService.getReferralInfo(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Referral info fetched", info));
    }

    // 13.2 Get My Referral History (who I referred, and their reward status)
    @GetMapping("/user/referral/history")
    public ResponseEntity<ApiResponse<List<ReferralHistoryItemResponse>>> getReferralHistory(
            @AuthenticationPrincipal User currentUser) {

        List<ReferralHistoryItemResponse> history = referralService.getReferralHistory(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Referral history fetched", history));
    }
}