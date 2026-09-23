// UserSupportController.java
package com.nestora.nestora_app.controller;

import com.nestora.nestora_app.dto.response.ApiResponse;
import com.nestora.nestora_app.entity.*;
import com.nestora.nestora_app.service.UserSupportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/support")
@RequiredArgsConstructor
public class UserSupportController {

    private final UserSupportService service;

    @PostMapping("/tickets")
    public ResponseEntity<ApiResponse<UserSupportTicket>> create(
            @AuthenticationPrincipal User user,
            @RequestBody CreateTicket req) {
        return ResponseEntity.ok(ApiResponse.success(
                "Ticket created",
                service.create(user, req.category, req.subject,
                        req.description, req.referenceId, req.referenceType)));
    }

    @GetMapping("/tickets")
    public ResponseEntity<ApiResponse<List<UserSupportTicket>>> getMine(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(
                "Tickets fetched",
                service.getMyTickets(user)));
    }

    public static class CreateTicket {
        public String category;
        public String subject;
        public String description;
        public Long referenceId;
        public String referenceType;
    }
}