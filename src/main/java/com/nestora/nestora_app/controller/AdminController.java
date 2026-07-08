package com.nestora.nestora_app.controller;


import com.nestora.nestora_app.dto.request.RejectOwnerRequest;
import com.nestora.nestora_app.dto.response.*;
import com.nestora.nestora_app.entity.Report;
import com.nestora.nestora_app.exception.AppException;
import com.nestora.nestora_app.repository.ReportRepository;
import com.nestora.nestora_app.service.AdminService;
import com.nestora.nestora_app.service.PropertyAccessSubscriptionService;
import com.nestora.nestora_app.enums.PropertyAccessStatus;
import com.nestora.nestora_app.dto.response.PropertyAccessHistoryResponse;
import com.nestora.nestora_app.service.PropertyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;
import com.nestora.nestora_app.repository.PropertyRepository;
import java.util.stream.Collectors;
import com.nestora.nestora_app.service.PropertyService;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final PropertyAccessSubscriptionService propertyAccessService;
    private final ReportRepository reportRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyService propertyService;

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

    // GET /admin/owners/{ownerId}/detail
    @GetMapping("/owners/{ownerId}/detail")
    public ResponseEntity<ApiResponse<AdminOwnerResponse>> getOwnerDetail(
            @PathVariable Long ownerId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Owner detail fetched",
                adminService.getOwnerDetail(ownerId)));
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

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<List<Report>>> getPendingReports() {
        return ResponseEntity.ok(
                ApiResponse.success("Reports fetched",
                        reportRepository.findByStatus("PENDING"))
        );
    }

    @PatchMapping("/reports/{reportId}/resolve")
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

    @GetMapping("/properties/{propertyId}/detail")
    public ResponseEntity<?> getPropertyDetail(
            @PathVariable Long propertyId) {
        return propertyService.getAdminPropertyDetail(propertyId);
    }

    @GetMapping("/properties/all")
    public ResponseEntity<?> getAllProperties() {
        return propertyService.getAllPropertiesForAdmin();
    }

    // =============================================
    // 🆕 Admin — Saari Property Access Subscriptions dekho
    // GET /admin/property-access-subscriptions?status=ACTIVE
    // =============================================
    @GetMapping("/property-access-subscriptions")
    public ResponseEntity<ApiResponse<List<PropertyAccessHistoryResponse>>> getAllPropertyAccessSubscriptions(
            @RequestParam(required = false) PropertyAccessStatus status) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Property access subscriptions fetched",
                        propertyAccessService.getAllSubscriptionsForAdmin(status))
        );
    }

    // =============================================
    // 🆕 Admin — Force expire karo subscription
    // PATCH /admin/property-access-subscriptions/{subscriptionId}/expire
    // =============================================
    @PatchMapping("/property-access-subscriptions/{subscriptionId}/expire")
    public ResponseEntity<ApiResponse<String>> forceExpireSubscription(
            @PathVariable Long subscriptionId,
            @RequestBody(required = false) java.util.Map<String, String> body) {

        String reason = (body != null && body.containsKey("reason"))
                ? body.get("reason")
                : "Admin action";

        return ResponseEntity.ok(
                ApiResponse.success(
                        propertyAccessService.forceExpireSubscription(subscriptionId, reason))
        );
    }

    /*// All Properties (Admin)
    @GetMapping("/properties/all")
    public ResponseEntity<ApiResponse<List<AdminPropertyResponse>>> getAllProperties() {
        List<AdminPropertyResponse> properties = propertyRepository.findAll()
                .stream()
                .map(p -> AdminPropertyResponse.builder()
                        .propertyId(p.getId())
                        .title(p.getTitle())
                        .ownerName(p.getOwner().getUser().getName())
                        .ownerDisplayId(p.getOwner().getUser().getDisplayId())
                        .city(p.getCity())
                        .state(p.getState())
                        .propertyType(p.getPropertyType())
                        .isPublished(p.getIsPublished())
                        .createdAt(p.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                ApiResponse.success("All properties fetched", properties)
        );
    }*/
}