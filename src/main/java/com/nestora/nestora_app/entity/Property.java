package com.nestora.nestora_app.entity;

import com.nestora.nestora_app.enums.GenderAllowed;
import com.nestora.nestora_app.enums.OccupancyStatus;
import com.nestora.nestora_app.enums.PropertyType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "properties")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private OwnerProfile owner;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_type", nullable = false)
    private PropertyType propertyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender_allowed", nullable = false)
    private GenderAllowed genderAllowed;

    @Column(name = "address_line", nullable = false)
    private String addressLine;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(nullable = false, length = 10)
    private String pincode;

    private BigDecimal latitude;
    private BigDecimal longitude;

    @Column(name = "monthly_rent_min")
    private BigDecimal monthlyRentMin;

    @Column(name = "monthly_rent_max")
    private BigDecimal monthlyRentMax;

    @Column(name = "security_deposit")
    private BigDecimal securityDeposit;

    @Column(name = "is_negotiable")
    private Boolean isNegotiable = false;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_published")
    private Boolean isPublished = false;

    @Column(name = "total_rooms")
    private Integer totalRooms = 0;

    @Column(name = "available_rooms")
    private Integer availableRooms = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "occupancy_status")
    private OccupancyStatus occupancyStatus = OccupancyStatus.AVAILABLE;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}