package com.nestora.nestora_app.service;


import com.nestora.nestora_app.dto.request.RejectOwnerRequest;
import com.nestora.nestora_app.dto.response.*;
import com.nestora.nestora_app.entity.*;
import com.nestora.nestora_app.enums.VerificationStatus;
import com.nestora.nestora_app.exception.AppException;
import com.nestora.nestora_app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final OwnerProfileRepository ownerProfileRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final SubscriptionPaymentRepository subscriptionPaymentRepository;
    private final PropertyAccessSubscriptionRepository propertyAccessSubscriptionRepository;

    // GET PENDING OWNERS
    public List<AdminOwnerResponse> getPendingOwners() {
        return ownerProfileRepository
                .findByVerificationStatus(VerificationStatus.PENDING)
                .stream()
                .map(this::mapToAdminOwnerResponse)
                .collect(Collectors.toList());
    }


    // GET ALL OWNERS

    public List<AdminOwnerResponse> getAllOwners() {
        return ownerProfileRepository.findAll()
                .stream()
                .map(this::mapToAdminOwnerResponse)
                .collect(Collectors.toList());
    }


    // VERIFY OWNER

    @Transactional
    public String verifyOwner(Long ownerId) {

        OwnerProfile owner = ownerProfileRepository.findById(ownerId)
                .orElseThrow(() -> new AppException(
                        "Owner not found", HttpStatus.NOT_FOUND
                ));

        if (owner.getVerificationStatus() == VerificationStatus.VERIFIED) {
            throw new AppException(
                    "Owner is already verified", HttpStatus.CONFLICT
            );
        }

        owner.setVerificationStatus(VerificationStatus.VERIFIED);
        owner.setVerifiedAt(LocalDateTime.now());
        owner.setRejectionReason(null);
        ownerProfileRepository.save(owner);

        // User ka role OWNER karo
        User user = owner.getUser();
        user.setRole(com.nestora.nestora_app.enums.Role.OWNER);
        userRepository.save(user);

        return "Owner verified successfully. Verified badge assigned.";
    }


    // REJECT OWNER

    @Transactional
    // GET /admin/owners/{ownerId}/detail
    public AdminOwnerResponse getOwnerDetail(Long ownerId) {
        OwnerProfile owner = ownerProfileRepository.findById(ownerId)
                .orElseThrow(() -> new AppException("Owner not found", HttpStatus.NOT_FOUND));

        return AdminOwnerResponse.builder()
                .ownerId(owner.getId())
                .userId(owner.getUser().getId())
                .displayId(owner.getUser().getDisplayId())
                .name(owner.getUser().getName())
                .email(owner.getUser().getEmail())
                .phone(owner.getUser().getPhone())
                .businessName(owner.getBusinessName())
                .aadharNumber(owner.getAadharNumber())
                .panNumber(owner.getPanNumber())
                .aadharDocUrl(owner.getAadharDocUrl())
                .panDocUrl(owner.getPanDocUrl())
                .addressProofUrl(owner.getAddressProofUrl())
                .verificationStatus(owner.getVerificationStatus())
                .rejectionReason(owner.getRejectionReason())
                .subscriptionPlan(owner.getSubscriptionPlan())
                .createdAt(owner.getCreatedAt())
                .build();
    }

    public String rejectOwner(Long ownerId, RejectOwnerRequest request) {

        OwnerProfile owner = ownerProfileRepository.findById(ownerId)
                .orElseThrow(() -> new AppException(
                        "Owner not found", HttpStatus.NOT_FOUND
                ));

        owner.setVerificationStatus(VerificationStatus.REJECTED);
        owner.setRejectionReason(request.getReason());
        owner.setVerifiedAt(null);
        ownerProfileRepository.save(owner);

        return "Owner application rejected.";
    }


    // GET PENDING PROPERTIES

    public List<AdminPropertyResponse> getPendingProperties() {
        return propertyRepository.findAll()
                .stream()
                .filter(p -> !p.getIsPublished() && p.getIsActive())
                .map(this::mapToAdminPropertyResponse)
                .collect(Collectors.toList());
    }


    // PUBLISH PROPERTY

    @Transactional
    public String publishProperty(Long propertyId) {

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new AppException(
                        "Property not found", HttpStatus.NOT_FOUND
                ));

        property.setIsPublished(true);
        propertyRepository.save(property);

        return "Property published successfully. Now visible to users.";
    }


    // UNPUBLISH PROPERTY

    @Transactional
    public String unpublishProperty(Long propertyId) {

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new AppException(
                        "Property not found", HttpStatus.NOT_FOUND
                ));

        property.setIsPublished(false);
        propertyRepository.save(property);

        return "Property unpublished successfully.";
    }


    // GET ALL USERS

    public List<AdminUserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToAdminUserResponse)
                .collect(Collectors.toList());
    }

    // =============================================
    // DEACTIVATE USER
    // =============================================
    @Transactional
    public String deactivateUser(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(
                        "User not found", HttpStatus.NOT_FOUND
                ));

        if (!user.getIsActive()) {
            throw new AppException(
                    "User is already deactivated", HttpStatus.CONFLICT
            );
        }

        user.setIsActive(false);
        userRepository.save(user);

        return "User account deactivated";
    }

    // =============================================
    // ACTIVATE USER
    // =============================================
    @Transactional
    public String activateUser(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(
                        "User not found", HttpStatus.NOT_FOUND
                ));

        if (user.getIsActive()) {
            throw new AppException(
                    "User is already active", HttpStatus.CONFLICT
            );
        }

        user.setIsActive(true);
        userRepository.save(user);

        return "User account activated";
    }

    // =============================================
    // DASHBOARD STATS
    // =============================================
    public DashboardStatsResponse getDashboardStats() {

        long totalUsers = userRepository.count();

        long totalOwners = ownerProfileRepository.count();

        long verifiedOwners = ownerProfileRepository
                .findByVerificationStatus(VerificationStatus.VERIFIED)
                .size();

        long pendingVerifications = ownerProfileRepository
                .findByVerificationStatus(VerificationStatus.PENDING)
                .size();

        long totalProperties = propertyRepository.count();

        long publishedProperties = propertyRepository
                .findByIsPublishedTrueAndIsActiveTrue()
                .size();

        long pendingProperties = totalProperties - publishedProperties;

        // Listing Subscription revenue
        BigDecimal listingRevenue = subscriptionPaymentRepository
                .findAll()
                .stream()
                .filter(p -> "SUCCESS".equals(p.getStatus()))
                .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Property Access Subscription revenue
        BigDecimal propertyAccessRevenue = propertyAccessSubscriptionRepository
                .findAll()
                .stream()
                .filter(p -> p.getAmountPaid() != null)
                .map(PropertyAccessSubscription::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Total = dono ka sum
        BigDecimal totalRevenue = listingRevenue.add(propertyAccessRevenue);

        return DashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalOwners(totalOwners)
                .verifiedOwners(verifiedOwners)
                .pendingVerifications(pendingVerifications)
                .totalProperties(totalProperties)
                .publishedProperties(publishedProperties)
                .pendingProperties(pendingProperties)
                .totalRevenue(totalRevenue)
                .listingSubscriptionRevenue(listingRevenue)
                .propertyAccessRevenue(propertyAccessRevenue)
                .build();
    }

    // =============================================
    // PRIVATE HELPERS
    // =============================================

    private AdminOwnerResponse mapToAdminOwnerResponse(OwnerProfile owner) {
        return AdminOwnerResponse.builder()
                .ownerId(owner.getId())
                .userId(owner.getUser().getId())
                .displayId(owner.getUser().getDisplayId())
                .name(owner.getUser().getName())
                .email(owner.getUser().getEmail())
                .phone(owner.getUser().getPhone())
                .businessName(owner.getBusinessName())
                .aadharNumber(owner.getAadharNumber())
                .panNumber(owner.getPanNumber())
                .aadharDocUrl(owner.getAadharDocUrl())
                .panDocUrl(owner.getPanDocUrl())
                .addressProofUrl(owner.getAddressProofUrl())
                .verificationStatus(owner.getVerificationStatus())
                .rejectionReason(owner.getRejectionReason())
                .subscriptionPlan(owner.getSubscriptionPlan())
                .createdAt(owner.getCreatedAt())
                .build();
    }

    private AdminPropertyResponse mapToAdminPropertyResponse(Property property) {
        return AdminPropertyResponse.builder()
                .propertyId(property.getId())
                .title(property.getTitle())
                .ownerName(property.getOwner().getUser().getName())
                .ownerDisplayId(property.getOwner().getUser().getDisplayId())
                .city(property.getCity())
                .state(property.getState())
                .propertyType(property.getPropertyType())
                .isPublished(property.getIsPublished())
                .createdAt(property.getCreatedAt())
                .build();
    }

    private AdminUserResponse mapToAdminUserResponse(User user) {
        return AdminUserResponse.builder()
                .userId(user.getId())
                .displayId(user.getDisplayId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .isEmailVerified(user.getIsEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}