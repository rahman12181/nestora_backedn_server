package com.nestora.nestora_app.controller;


import com.nestora.nestora_app.dto.request.RejectOwnerRequest;
import com.nestora.nestora_app.dto.response.*;
import com.nestora.nestora_app.entity.Report;
import com.nestora.nestora_app.exception.AppException;
import com.nestora.nestora_app.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // =============================================
    // OWNER MANAGEMENT
    // =============================================

    @GetMapping("/owners/pending")
    public ResponseEntity<ApiResponse<List<AdminOwnerResponse>>> getPendingOwners() {
        return ResponseEntity.ok(
                ApiResponse.success("Pending owners fetched",
                        adminService.getPendingOwners())
        );
    }

    @GetMapping("/owners/all")
    public ResponseEntity<ApiResponse<List<AdminOwnerResponse>>> getAllOwners() {
        return ResponseEntity.ok(
                ApiResponse.success("All owners fetched",
                        adminService.getAllOwners())
        );
    }

    @PatchMapping("/owners/{ownerId}/verify")
    public ResponseEntity<ApiResponse<String>> verifyOwner(
            @PathVariable Long ownerId) {

        String message = adminService.verifyOwner(ownerId);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    @PatchMapping("/owners/{ownerId}/reject")
    public ResponseEntity<ApiResponse<String>> rejectOwner(
            @PathVariable Long ownerId,
            @Valid @RequestBody RejectOwnerRequest request) {

        String message = adminService.rejectOwner(ownerId, request);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    // =============================================
    // PROPERTY MANAGEMENT
    // =============================================

    @GetMapping("/properties/pending")
    public ResponseEntity<ApiResponse<List<AdminPropertyResponse>>> getPendingProperties() {
        return ResponseEntity.ok(
                ApiResponse.success("Pending properties fetched",
                        adminService.getPendingProperties())
        );
    }

    @PatchMapping("/properties/{propertyId}/publish")
    public ResponseEntity<ApiResponse<String>> publishProperty(
            @PathVariable Long propertyId) {

        String message = adminService.publishProperty(propertyId);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    @PatchMapping("/properties/{propertyId}/unpublish")
    public ResponseEntity<ApiResponse<String>> unpublishProperty(
            @PathVariable Long propertyId) {

        String message = adminService.unpublishProperty(propertyId);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    // =============================================
    // USER MANAGEMENT
    // =============================================

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<AdminUserResponse>>> getAllUsers() {
        return ResponseEntity.ok(
                ApiResponse.success("Users fetched",
                        adminService.getAllUsers())
        );
    }

    @PatchMapping("/users/{userId}/deactivate")
    public ResponseEntity<ApiResponse<String>> deactivateUser(
            @PathVariable Long userId) {

        String message = adminService.deactivateUser(userId);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    @PatchMapping("/users/{userId}/activate")
    public ResponseEntity<ApiResponse<String>> activateUser(
            @PathVariable Long userId) {

        String message = adminService.activateUser(userId);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    // =============================================
    // DASHBOARD
    // =============================================

    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats() {
        return ResponseEntity.ok(
                ApiResponse.success("Dashboard stats fetched",
                        adminService.getDashboardStats())
        );
    }

    @GetMapping("/admin/reports")
    public ResponseEntity<ApiResponse<List<Report>>> getPendingReports() {
        return ResponseEntity.ok(
                ApiResponse.success("Reports fetched",
                        reportRepository.findByStatus("PENDING"))
        );
    }

    @PatchMapping("/admin/reports/{reportId}/resolve")
    public ResponseEntity<ApiResponse<String>> resolveReport(
            @PathVariable Long reportId) {

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(
                        "Report not found", HttpStatus.NOT_FOUND
                ));

        report.setStatus("RESOLVED");
        reportRepository.save(report);

        return ResponseEntity.ok(ApiResponse.success("Report resolved"));
    }
}
