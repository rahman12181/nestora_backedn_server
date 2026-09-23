package com.nestora.nestora_app.controller;

import com.nestora.nestora_app.dto.response.ApiResponse;
import com.nestora.nestora_app.entity.*;
import com.nestora.nestora_app.exception.AppException;
import com.nestora.nestora_app.service.RentalAgreementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/rental-agreements")
@RequiredArgsConstructor
public class RentalAgreementController {

    private final RentalAgreementService agreementService;

    // ============================================
    // GET all my agreements
    // ============================================
    @GetMapping
    public ResponseEntity<ApiResponse<List<RentalAgreement>>> getMyAgreements(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(
                "Agreements fetched",
                agreementService.getUserAgreements(user)));
    }

    // ============================================
    // GET single agreement detail
    // ============================================
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RentalAgreement>> getDetail(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Agreement fetched",
                agreementService.getAgreementDetail(user, id)));
    }

    // ============================================
    // GET invoices for agreement
    // ============================================
    @GetMapping("/{id}/invoices")
    public ResponseEntity<ApiResponse<List<RentInvoice>>> getInvoices(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Invoices fetched",
                agreementService.getUserInvoices(user, id)));
    }

    // ============================================
    // POST terminate agreement
    // ============================================
    @PostMapping("/{id}/terminate")
    public ResponseEntity<ApiResponse<RentalAgreement>> terminate(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody TerminateRequest req) {

        if (req.reason == null || req.reason.trim().isEmpty()) {
            throw new AppException("Termination reason is required", HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.ok(ApiResponse.success(
                "Agreement terminated",
                agreementService.terminateAgreement(user, id, req.reason)));
    }

    public static class TerminateRequest {
        public String reason;
    }
}