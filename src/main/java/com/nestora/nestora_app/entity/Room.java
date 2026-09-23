package com.nestora.nestora_app.entity;

import com.nestora.nestora_app.enums.RoomStatus;
import com.nestora.nestora_app.enums.RoomType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "room_number", length = 20)
    private String roomNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false)
    private RoomType roomType;

    @Column(name = "floor_number")
    private Integer floorNumber;

    @Column(name = "monthly_rent", nullable = false)
    private BigDecimal monthlyRent;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "occupied_count")
    private Integer occupiedCount = 0;

    @Enumerated(EnumType.STRING)
    private RoomStatus status = RoomStatus.AVAILABLE;

    @Column(name = "has_ac")
    private Boolean hasAc = false;

    @Column(name = "has_attached_bathroom")
    private Boolean hasAttachedBathroom = false;

    @Column(columnDefinition = "TEXT")
    private String description;

    // ============================================
    // ✅ NEW — Current Tenant Information
    // ============================================
    @Column(name = "current_user_id")
    private Long currentUserId;

    @Column(name = "current_agreement_id")
    private Long currentAgreementId;

    @Column(name = "occupied_since")
    private LocalDateTime occupiedSince;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ============================================
    // ✅ Helper Methods
    // ============================================
    public boolean isOccupied() {
        return status == RoomStatus.OCCUPIED;
    }

    public boolean hasTenant() {
        return currentUserId != null;
    }

    public void assignTenant(Long userId, Long agreementId) {
        this.currentUserId = userId;
        this.currentAgreementId = agreementId;
        this.occupiedSince = LocalDateTime.now();
        this.status = RoomStatus.OCCUPIED;
        if (this.occupiedCount == null) {
            this.occupiedCount = 0;
        }
        this.occupiedCount = this.occupiedCount + 1;
    }

    public void releaseTenant() {
        this.currentUserId = null;
        this.currentAgreementId = null;
        this.occupiedSince = null;
        this.status = RoomStatus.AVAILABLE;
        if (this.occupiedCount != null && this.occupiedCount > 0) {
            this.occupiedCount = this.occupiedCount - 1;
        }
    }
}