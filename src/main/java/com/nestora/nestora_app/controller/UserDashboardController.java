package com.nestora.nestora_app.controller;

import com.nestora.nestora_app.dto.response.ApiResponse;
import com.nestora.nestora_app.dto.response.UserDashboardSummaryResponse;
import com.nestora.nestora_app.entity.User;
import com.nestora.nestora_app.service.UserDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/dashboard")
@RequiredArgsConstructor
public class UserDashboardController {

    private final UserDashboardService userDashboardService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<UserDashboardSummaryResponse>> getSummary(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(
                "Dashboard summary fetched",
                userDashboardService.getSummary(currentUser)));
    }
}