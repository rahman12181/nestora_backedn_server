package com.nestora.nestora_app.service;

import com.nestora.nestora_app.dto.response.PropertySearchResponse;
import com.nestora.nestora_app.dto.response.ReviewResponse;
import com.nestora.nestora_app.dto.response.RoomResponse;
import com.nestora.nestora_app.entity.*;
import com.nestora.nestora_app.enums.GenderAllowed;
import com.nestora.nestora_app.enums.PropertyType;
import com.nestora.nestora_app.enums.VerificationStatus;
import com.nestora.nestora_app.exception.AppException;
import com.nestora.nestora_app.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final PropertyRepository propertyRepository;
    private final PropertyAmenityRepository amenityRepository;
    private final PropertyMediaRepository mediaRepository;
    private final ReviewRepository reviewRepository;
    private final RoomRepository roomRepository;
    private final OwnerProfileRepository ownerProfileRepository;

    // =============================================
    // SEARCH PROPERTIES
    // =============================================
    public List<PropertySearchResponse> searchProperties(
            String city,
            String type,
            String gender,
            BigDecimal minRent,
            BigDecimal maxRent,
            String pincode,
            Double lat,
            Double lng,
            Double radius,
            String sort) {

        PropertyType propertyType = null;
        if (type != null && !type.isEmpty()) {
            try {
                propertyType = PropertyType.valueOf(type.toUpperCase());
            } catch (Exception ignored) {}
        }

        GenderAllowed genderAllowed = null;
        if (gender != null && !gender.isEmpty()) {
            try {
                genderAllowed = GenderAllowed.valueOf(gender.toUpperCase());
            } catch (Exception ignored) {}
        }

        List<Property> properties = propertyRepository.searchProperties(
                city, propertyType, genderAllowed, minRent, maxRent, pincode
        );

        // Location based filter
        if (lat != null && lng != null && radius != null) {
            properties = properties.stream()
                    .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
                    .filter(p -> {
                        double distance = calculateDistance(
                                lat, lng,
                                p.getLatitude().doubleValue(),
                                p.getLongitude().doubleValue()
                        );
                        return distance <= radius;
                    })
                    .collect(Collectors.toList());
        }

        List<PropertySearchResponse> responses = properties.stream()
                .map(p -> mapToSearchResponse(p, lat, lng))
                .collect(Collectors.toList());

        // Sorting
        if (sort != null && !sort.isEmpty()) {
            switch (sort.toLowerCase()) {
                case "rent_asc" ->
                        responses.sort(Comparator.comparing(
                                r -> r.getMonthlyRentMin() != null ?
                                        r.getMonthlyRentMin() : BigDecimal.ZERO
                        ));
                case "rent_desc" ->
                        responses.sort(Comparator.comparing(
                                (PropertySearchResponse r) ->
                                        r.getMonthlyRentMin() != null ?
                                                r.getMonthlyRentMin() : BigDecimal.ZERO
                        ).reversed());
                case "nearest" -> {
                    if (lat != null && lng != null) {
                        responses.sort(Comparator.comparing(
                                r -> r.getDistanceKm() != null ?
                                        r.getDistanceKm() : Double.MAX_VALUE
                        ));
                    }
                }
                case "rating" ->
                        responses.sort(Comparator.comparing(
                                (PropertySearchResponse r) ->
                                        r.getAverageRating() != null ?
                                                r.getAverageRating() : 0.0
                        ).reversed());
            }
        }

        // Featured properties hamesha pehle dikhao
        responses.sort((a, b) -> {
            boolean aFeatured = Boolean.TRUE.equals(a.getIsFeatured());
            boolean bFeatured = Boolean.TRUE.equals(b.getIsFeatured());
            if (aFeatured && !bFeatured) return -1;
            if (!aFeatured && bFeatured) return 1;
            return 0;
        });

        return responses;
    }

    // =============================================
    // GET PROPERTY DETAIL (Public)
    // =============================================
    @Transactional
    public PropertySearchResponse getPropertyDetail(Long propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new AppException(
                        "Property not found", HttpStatus.NOT_FOUND
                ));

        if (!property.getIsPublished()) {
            throw new AppException("Property not available", HttpStatus.NOT_FOUND);
        }

        // View count increment
        property.setViewCount(
                property.getViewCount() != null ?
                        property.getViewCount() + 1 : 1
        );
        propertyRepository.save(property);

        return mapToSearchResponse(property, null, null);
    }

    // =============================================
    // GET PROPERTY ROOMS (Public)
    // =============================================
    public List<RoomResponse> getPropertyRooms(Long propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new AppException(
                        "Property not found", HttpStatus.NOT_FOUND
                ));

        return roomRepository.findByProperty(property)
                .stream()
                .map(this::mapToRoomResponse)
                .collect(Collectors.toList());
    }

    // =============================================
    // GET PROPERTY REVIEWS (Public)
    // =============================================
    public List<ReviewResponse> getPropertyReviews(Long propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new AppException(
                        "Property not found", HttpStatus.NOT_FOUND
                ));

        return reviewRepository.findByPropertyAndIsVisibleTrue(property)
                .stream()
                .map(this::mapToReviewResponse)
                .collect(Collectors.toList());
    }

    // =============================================
    // PRIVATE HELPERS
    // =============================================

    private PropertySearchResponse mapToSearchResponse(Property p,
                                                       Double userLat,
                                                       Double userLng) {
        // Cover image
        String coverImage = mediaRepository
                .findByPropertyOrderBySortOrderAsc(p)
                .stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsPrimary()))
                .findFirst()
                .map(PropertyMedia::getUrl)
                .orElse(null);

        // Amenities
        List<String> amenities = amenityRepository
                .findByProperty(p)
                .stream()
                .map(PropertyAmenity::getAmenity)
                .collect(Collectors.toList());

        // Reviews
        List<Review> reviews = reviewRepository
                .findByPropertyAndIsVisibleTrue(p);

        double avgRating = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);

        // Owner verified?
        OwnerProfile owner = ownerProfileRepository
                .findByUser(p.getOwner().getUser())
                .orElse(null);

        boolean isVerified = owner != null &&
                owner.getVerificationStatus() == VerificationStatus.VERIFIED;

        // Distance
        Double distance = null;
        if (userLat != null && userLng != null &&
                p.getLatitude() != null && p.getLongitude() != null) {
            distance = calculateDistance(
                    userLat, userLng,
                    p.getLatitude().doubleValue(),
                    p.getLongitude().doubleValue()
            );
            distance = Math.round(distance * 100.0) / 100.0;
        }

        return PropertySearchResponse.builder()
                .propertyId(p.getId())
                .title(p.getTitle())
                .propertyType(p.getPropertyType())
                .genderAllowed(p.getGenderAllowed())
                .addressLine(p.getAddressLine())
                .city(p.getCity())
                .state(p.getState())
                .pincode(p.getPincode())
                .monthlyRentMin(p.getMonthlyRentMin())
                .monthlyRentMax(p.getMonthlyRentMax())
                .securityDeposit(p.getSecurityDeposit())
                .isNegotiable(p.getIsNegotiable())
                .totalRooms(p.getTotalRooms())
                .availableRooms(p.getAvailableRooms())
                .occupancyStatus(p.getOccupancyStatus())
                .coverImage(coverImage)
                .amenities(amenities)
                .isVerifiedOwner(isVerified)
                .distanceKm(distance)
                .averageRating(Math.round(avgRating * 10.0) / 10.0)
                .totalReviews(reviews.size())
                .isFeatured(Boolean.TRUE.equals(p.getIsFeatured()))   // NEW
                .viewCount(p.getViewCount() != null ? p.getViewCount() : 0L) // NEW
                .build();
    }

    // Haversine formula
    private double calculateDistance(double lat1, double lon1,
                                     double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2)
                * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private RoomResponse mapToRoomResponse(Room room) {
        return RoomResponse.builder()
                .roomId(room.getId())
                .roomNumber(room.getRoomNumber())
                .roomType(room.getRoomType())
                .floorNumber(room.getFloorNumber())
                .monthlyRent(room.getMonthlyRent())
                .capacity(room.getCapacity())
                .occupiedCount(room.getOccupiedCount())
                .status(room.getStatus())
                .hasAc(room.getHasAc())
                .hasAttachedBathroom(room.getHasAttachedBathroom())
                .description(room.getDescription())
                .build();
    }

    private ReviewResponse mapToReviewResponse(Review review) {
        return ReviewResponse.builder()
                .reviewId(review.getId())
                .userName(review.getUser().getName())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}