package com.nestora.nestora_app.service;


import com.nestora.nestora_app.dto.request.PropertyCreateRequest;
import com.nestora.nestora_app.dto.request.PropertyUpdateRequest;
import com.nestora.nestora_app.dto.request.RoomCreateRequest;
import com.nestora.nestora_app.dto.request.RoomStatusRequest;
import com.nestora.nestora_app.dto.request.RoomUpdateRequest;
import com.nestora.nestora_app.dto.response.MediaResponse;
import com.nestora.nestora_app.dto.response.PropertyResponse;
import com.nestora.nestora_app.dto.response.RoomResponse;
import com.nestora.nestora_app.entity.*;
import com.nestora.nestora_app.enums.MediaType;
import com.nestora.nestora_app.enums.OccupancyStatus;
import com.nestora.nestora_app.enums.RoomStatus;
import com.nestora.nestora_app.enums.VerificationStatus;
import com.nestora.nestora_app.exception.AppException;
import com.nestora.nestora_app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.nestora.nestora_app.repository.BookingRequestRepository;
import com.nestora.nestora_app.repository.ReviewRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import com.nestora.nestora_app.dto.response.ApiResponse;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final PropertyAmenityRepository amenityRepository;
    private final PropertyMediaRepository mediaRepository;
    private final RoomRepository roomRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final CloudinaryService cloudinaryService;
    private final BookingRequestRepository bookingRequestRepository;
    private final ReviewRepository reviewRepository;
    private final PropertyAccessSubscriptionService propertyAccessSubscriptionService;
    // =============================================
    // ADD PROPERTY
    // =============================================
    @Transactional
    public PropertyResponse addProperty(User currentUser,
                                        PropertyCreateRequest request) {

        OwnerProfile owner = getVerifiedOwner(currentUser);

        // 🆕 Property Access Subscription check — active subscription honi chahiye
        propertyAccessSubscriptionService.checkPropertyAccessAllowed(owner);

        Property property = Property.builder()
                .owner(owner)
                .title(request.getTitle())
                .description(request.getDescription())
                .propertyType(request.getPropertyType())
                .genderAllowed(request.getGenderAllowed())
                .addressLine(request.getAddressLine())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .monthlyRentMin(request.getMonthlyRentMin())
                .monthlyRentMax(request.getMonthlyRentMax())
                .securityDeposit(request.getSecurityDeposit())
                .isNegotiable(request.getIsNegotiable())
                .isActive(true)
                .isPublished(false)
                .totalRooms(0)
                .availableRooms(0)
                .occupancyStatus(OccupancyStatus.AVAILABLE)
                .build();

        Property saved = propertyRepository.save(property);

        // Amenities save karo
        if (request.getAmenities() != null && !request.getAmenities().isEmpty()) {
            saveAmenities(saved, request.getAmenities());
        }

        return mapToPropertyResponse(saved);
    }

    // =============================================
    // GET MY PROPERTIES
    // =============================================
    public List<PropertyResponse> getMyProperties(User currentUser) {

        OwnerProfile owner = ownerProfileRepository.findByUser(currentUser)
                .orElseThrow(() -> new AppException(
                        "Owner profile not found", HttpStatus.NOT_FOUND
                ));

        return propertyRepository.findByOwner(owner)
                .stream()
                .map(this::mapToPropertyResponse)
                .collect(Collectors.toList());
    }

    // =============================================
    // GET PROPERTY BY ID
    // =============================================
    public PropertyResponse getPropertyById(User currentUser, Long propertyId) {

        Property property = getPropertyOfOwner(currentUser, propertyId);
        return mapToPropertyResponse(property);
    }

    // =============================================
    // UPDATE PROPERTY
    // =============================================
    @Transactional
    public PropertyResponse updateProperty(User currentUser,
                                           Long propertyId,
                                           PropertyUpdateRequest request) {

        Property property = getPropertyOfOwner(currentUser, propertyId);

        if (request.getTitle() != null)
            property.setTitle(request.getTitle());
        if (request.getDescription() != null)
            property.setDescription(request.getDescription());
        if (request.getGenderAllowed() != null)
            property.setGenderAllowed(request.getGenderAllowed());
        if (request.getAddressLine() != null)
            property.setAddressLine(request.getAddressLine());
        if (request.getCity() != null)
            property.setCity(request.getCity());
        if (request.getState() != null)
            property.setState(request.getState());
        if (request.getPincode() != null)
            property.setPincode(request.getPincode());
        if (request.getLatitude() != null)
            property.setLatitude(request.getLatitude());
        if (request.getLongitude() != null)
            property.setLongitude(request.getLongitude());
        if (request.getMonthlyRentMin() != null)
            property.setMonthlyRentMin(request.getMonthlyRentMin());
        if (request.getMonthlyRentMax() != null)
            property.setMonthlyRentMax(request.getMonthlyRentMax());
        if (request.getSecurityDeposit() != null)
            property.setSecurityDeposit(request.getSecurityDeposit());
        if (request.getIsNegotiable() != null)
            property.setIsNegotiable(request.getIsNegotiable());

        // Amenities update karo
        if (request.getAmenities() != null) {
            amenityRepository.deleteByProperty(property);
            saveAmenities(property, request.getAmenities());
        }

        propertyRepository.save(property);
        return mapToPropertyResponse(property);
    }

    // =============================================
    // DELETE PROPERTY
    // =============================================
    @Transactional
    public String deleteProperty(User currentUser, Long propertyId) {

        Property property = getPropertyOfOwner(currentUser, propertyId);
        propertyRepository.delete(property);
        return "Property deleted successfully";
    }

    // =============================================
    // UPLOAD MEDIA
    // =============================================
    @Transactional
    public MediaResponse uploadMedia(User currentUser,
                                     Long propertyId,
                                     MultipartFile file,
                                     String mediaType,
                                     Boolean isPrimary) {

        Property property = getPropertyOfOwner(currentUser, propertyId);

        String url;
        MediaType type = MediaType.valueOf(mediaType.toUpperCase());

        if (type == MediaType.VIDEO) {
            url = cloudinaryService.uploadVideo(file, "property-videos");
        } else {
            url = cloudinaryService.uploadImage(file, "property-images");
        }

        // Agar isPrimary true hai to purana primary remove karo
        if (Boolean.TRUE.equals(isPrimary)) {
            mediaRepository.findByPropertyOrderBySortOrderAsc(property)
                    .forEach(m -> {
                        if (Boolean.TRUE.equals(m.getIsPrimary())) {
                            m.setIsPrimary(false);
                            mediaRepository.save(m);
                        }
                    });
        }

        PropertyMedia media = PropertyMedia.builder()
                .property(property)
                .mediaType(type)
                .url(url)
                .isPrimary(isPrimary != null ? isPrimary : false)
                .sortOrder(0)
                .build();

        PropertyMedia saved = mediaRepository.save(media);

        return mapToMediaResponse(saved);
    }

    // =============================================
    // DELETE MEDIA
    // =============================================
    @Transactional
    public String deleteMedia(User currentUser,
                              Long propertyId,
                              Long mediaId) {

        getPropertyOfOwner(currentUser, propertyId);

        PropertyMedia media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new AppException(
                        "Media not found", HttpStatus.NOT_FOUND
                ));

        cloudinaryService.deleteFile(media.getUrl());
        mediaRepository.delete(media);

        return "Media deleted successfully";
    }

    // =============================================
    // ADD ROOM
    // =============================================
    @Transactional
    public RoomResponse addRoom(User currentUser,
                                Long propertyId,
                                RoomCreateRequest request) {

        Property property = getPropertyOfOwner(currentUser, propertyId);

        Room room = Room.builder()
                .property(property)
                .roomNumber(request.getRoomNumber())
                .roomType(request.getRoomType())
                .floorNumber(request.getFloorNumber())
                .monthlyRent(request.getMonthlyRent())
                .capacity(request.getCapacity())
                .occupiedCount(0)
                .status(RoomStatus.AVAILABLE)
                .hasAc(request.getHasAc())
                .hasAttachedBathroom(request.getHasAttachedBathroom())
                .description(request.getDescription())
                .build();

        roomRepository.save(room);

        // Property room count update karo
        updatePropertyRoomCounts(property);

        return mapToRoomResponse(room);
    }

    // =============================================
    // GET ROOMS
    // =============================================
    public List<RoomResponse> getRooms(User currentUser, Long propertyId) {

        Property property = getPropertyOfOwner(currentUser, propertyId);

        return roomRepository.findByProperty(property)
                .stream()
                .map(this::mapToRoomResponse)
                .collect(Collectors.toList());
    }

    // =============================================
    // UPDATE ROOM
    // =============================================
    @Transactional
    public RoomResponse updateRoom(User currentUser,
                                   Long propertyId,
                                   Long roomId,
                                   RoomUpdateRequest request) {

        getPropertyOfOwner(currentUser, propertyId);

        Room room = getRoomById(roomId);

        if (request.getRoomNumber() != null)
            room.setRoomNumber(request.getRoomNumber());
        if (request.getFloorNumber() != null)
            room.setFloorNumber(request.getFloorNumber());
        if (request.getMonthlyRent() != null)
            room.setMonthlyRent(request.getMonthlyRent());
        if (request.getCapacity() != null)
            room.setCapacity(request.getCapacity());
        if (request.getHasAc() != null)
            room.setHasAc(request.getHasAc());
        if (request.getHasAttachedBathroom() != null)
            room.setHasAttachedBathroom(request.getHasAttachedBathroom());
        if (request.getDescription() != null)
            room.setDescription(request.getDescription());

        roomRepository.save(room);
        return mapToRoomResponse(room);
    }

    // =============================================
    // UPDATE ROOM STATUS
    // =============================================
    @Transactional
    public RoomResponse updateRoomStatus(User currentUser,
                                         Long propertyId,
                                         Long roomId,
                                         RoomStatusRequest request) {

        Property property = getPropertyOfOwner(currentUser, propertyId);
        Room room = getRoomById(roomId);

        room.setStatus(request.getStatus());
        roomRepository.save(room);

        // Property available rooms update karo
        updatePropertyRoomCounts(property);

        return mapToRoomResponse(room);
    }

    // =============================================
    // DELETE ROOM
    // =============================================
    @Transactional
    public String deleteRoom(User currentUser,
                             Long propertyId,
                             Long roomId) {

        Property property = getPropertyOfOwner(currentUser, propertyId);
        Room room = getRoomById(roomId);

        roomRepository.delete(room);
        updatePropertyRoomCounts(property);

        return "Room deleted successfully";
    }

    // =============================================
    // PRIVATE HELPERS
    // =============================================

    private OwnerProfile getVerifiedOwner(User user) {
        OwnerProfile owner = ownerProfileRepository.findByUser(user)
                .orElseThrow(() -> new AppException(
                        "Owner profile not found. Please apply as owner first.",
                        HttpStatus.NOT_FOUND
                ));

        if (owner.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new AppException(
                    "Only verified owners can add properties.",
                    HttpStatus.FORBIDDEN
            );
        }

        return owner;
    }

    private Property getPropertyOfOwner(User user, Long propertyId) {
        OwnerProfile owner = ownerProfileRepository.findByUser(user)
                .orElseThrow(() -> new AppException(
                        "Owner profile not found", HttpStatus.NOT_FOUND
                ));

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new AppException(
                        "Property not found", HttpStatus.NOT_FOUND
                ));

        // Property is owner ki hai?
        if (!property.getOwner().getId().equals(owner.getId())) {
            throw new AppException(
                    "You are not authorized to access this property.",
                    HttpStatus.FORBIDDEN
            );
        }

        return property;
    }

    private Room getRoomById(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(
                        "Room not found", HttpStatus.NOT_FOUND
                ));
    }

    private void saveAmenities(Property property, List<String> amenities) {
        amenities.forEach(amenity -> {
            PropertyAmenity pa = PropertyAmenity.builder()
                    .property(property)
                    .amenity(amenity)
                    .build();
            amenityRepository.save(pa);
        });
    }

    private void updatePropertyRoomCounts(Property property) {
        List<Room> allRooms = roomRepository.findByProperty(property);
        int total = allRooms.size();
        int available = (int) allRooms.stream()
                .filter(r -> r.getStatus() == RoomStatus.AVAILABLE)
                .count();

        property.setTotalRooms(total);
        property.setAvailableRooms(available);

        if (available == 0) {
            property.setOccupancyStatus(OccupancyStatus.FULL);
        } else if (available == total) {
            property.setOccupancyStatus(OccupancyStatus.AVAILABLE);
        } else {
            property.setOccupancyStatus(OccupancyStatus.PARTIAL);
        }

        propertyRepository.save(property);
    }

    private PropertyResponse mapToPropertyResponse(Property property) {

        List<String> amenities = amenityRepository
                .findByProperty(property)
                .stream()
                .map(PropertyAmenity::getAmenity)
                .collect(Collectors.toList());

        List<MediaResponse> media = mediaRepository
                .findByPropertyOrderBySortOrderAsc(property)
                .stream()
                .map(this::mapToMediaResponse)
                .collect(Collectors.toList());

        return PropertyResponse.builder()
                .propertyId(property.getId())
                .title(property.getTitle())
                .description(property.getDescription())
                .propertyType(property.getPropertyType())
                .genderAllowed(property.getGenderAllowed())
                .addressLine(property.getAddressLine())
                .city(property.getCity())
                .state(property.getState())
                .pincode(property.getPincode())
                .latitude(property.getLatitude())
                .longitude(property.getLongitude())
                .monthlyRentMin(property.getMonthlyRentMin())
                .monthlyRentMax(property.getMonthlyRentMax())
                .securityDeposit(property.getSecurityDeposit())
                .isNegotiable(property.getIsNegotiable())
                .totalRooms(property.getTotalRooms())
                .availableRooms(property.getAvailableRooms())
                .occupancyStatus(property.getOccupancyStatus())
                .isPublished(property.getIsPublished())
                .amenities(amenities)
                .media(media)
                .createdAt(property.getCreatedAt())
                .build();
    }

    private MediaResponse mapToMediaResponse(PropertyMedia media) {
        return MediaResponse.builder()
                .mediaId(media.getId())
                .mediaType(media.getMediaType())
                .url(media.getUrl())
                .thumbnailUrl(media.getThumbnailUrl())
                .durationSec(media.getDurationSec())
                .isPrimary(media.getIsPrimary())
                .sortOrder(media.getSortOrder())
                .build();
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

    // =============================================
// ADMIN — Complete Property Detail
// =============================================
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdminPropertyDetail(
            Long propertyId) {

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new AppException(
                        "Property not found", HttpStatus.NOT_FOUND
                ));

        OwnerProfile owner = property.getOwner();
        User ownerUser = owner.getUser();

        // Tab 1 — Overview
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("propertyId", property.getId());
        overview.put("title", property.getTitle());
        overview.put("description", property.getDescription());
        overview.put("propertyType", property.getPropertyType());
        overview.put("genderAllowed", property.getGenderAllowed());
        overview.put("addressLine", property.getAddressLine());
        overview.put("city", property.getCity());
        overview.put("state", property.getState());
        overview.put("pincode", property.getPincode());
        overview.put("latitude", property.getLatitude());
        overview.put("longitude", property.getLongitude());
        overview.put("monthlyRentMin", property.getMonthlyRentMin());
        overview.put("monthlyRentMax", property.getMonthlyRentMax());
        overview.put("securityDeposit", property.getSecurityDeposit());
        overview.put("isNegotiable", property.getIsNegotiable());
        overview.put("totalRooms", property.getTotalRooms());
        overview.put("availableRooms", property.getAvailableRooms());
        overview.put("occupancyStatus", property.getOccupancyStatus());
        overview.put("isPublished", property.getIsPublished());
        overview.put("isFeatured", property.getIsFeatured());
        overview.put("featuredUntil", property.getFeaturedUntil());
        overview.put("viewCount", property.getViewCount());
        overview.put("amenities", amenityRepository.findByProperty(property)
                .stream()
                .map(PropertyAmenity::getAmenity)
                .collect(Collectors.toList()));
        overview.put("createdAt", property.getCreatedAt());

        // Tab 2 — Owner Complete Detail
        Map<String, Object> ownerDetail = new LinkedHashMap<>();
        ownerDetail.put("ownerId", owner.getId());
        ownerDetail.put("userId", ownerUser.getId());
        ownerDetail.put("displayId", ownerUser.getDisplayId());
        ownerDetail.put("name", ownerUser.getName());
        ownerDetail.put("email", ownerUser.getEmail());
        ownerDetail.put("phone", ownerUser.getPhone());
        ownerDetail.put("profilePic", ownerUser.getProfilePic());
        ownerDetail.put("businessName", owner.getBusinessName());
        ownerDetail.put("aadharNumber", owner.getAadharNumber());
        ownerDetail.put("panNumber", owner.getPanNumber());
        ownerDetail.put("aadharDocUrl", owner.getAadharDocUrl());
        ownerDetail.put("panDocUrl", owner.getPanDocUrl());
        ownerDetail.put("addressProofUrl", owner.getAddressProofUrl());
        ownerDetail.put("verificationStatus", owner.getVerificationStatus());
        ownerDetail.put("rejectionReason", owner.getRejectionReason());
        ownerDetail.put("verifiedAt", owner.getVerifiedAt());
        ownerDetail.put("subscriptionPlan", owner.getSubscriptionPlan());
        ownerDetail.put("subscriptionStatus", owner.getSubscriptionStatus());
        ownerDetail.put("subscriptionStart", owner.getSubscriptionStart());
        ownerDetail.put("subscriptionEnd", owner.getSubscriptionEnd());
        ownerDetail.put("monthlyFee", owner.getMonthlyFee());
        ownerDetail.put("totalProperties",
                propertyRepository.findByOwner(owner).size());
        ownerDetail.put("memberSince", ownerUser.getCreatedAt());

        // Tab 3 — Rooms
        List<Map<String, Object>> rooms = roomRepository
                .findByProperty(property)
                .stream()
                .map(room -> {
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("roomId", room.getId());
                    r.put("roomNumber", room.getRoomNumber());
                    r.put("roomType", room.getRoomType());
                    r.put("floorNumber", room.getFloorNumber());
                    r.put("monthlyRent", room.getMonthlyRent());
                    r.put("capacity", room.getCapacity());
                    r.put("occupiedCount", room.getOccupiedCount());
                    r.put("status", room.getStatus());
                    r.put("hasAc", room.getHasAc());
                    r.put("hasAttachedBathroom", room.getHasAttachedBathroom());
                    r.put("description", room.getDescription());
                    return r;
                })
                .collect(Collectors.toList());

        // Tab 4 — Media
        List<Map<String, Object>> media = mediaRepository
                .findByPropertyOrderBySortOrderAsc(property)
                .stream()
                .map(m -> {
                    Map<String, Object> med = new LinkedHashMap<>();
                    med.put("mediaId", m.getId());
                    med.put("mediaType", m.getMediaType());
                    med.put("url", m.getUrl());
                    med.put("thumbnailUrl", m.getThumbnailUrl());
                    med.put("durationSec", m.getDurationSec());
                    med.put("isPrimary", m.getIsPrimary());
                    //  med.put("is360", m.getIs360());
                    med.put("sortOrder", m.getSortOrder());
                    return med;
                })
                .collect(Collectors.toList());

        // Tab 5 — Reviews
        List<Review> reviewList = reviewRepository
                .findByPropertyAndIsVisibleTrue(property);

        double avgRating = reviewList.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);

        List<Map<String, Object>> reviews = reviewList.stream()
                .map(rev -> {
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("reviewId", rev.getId());
                    r.put("userName", rev.getUser().getName());
                    r.put("userEmail", rev.getUser().getEmail());
                    r.put("userDisplayId", rev.getUser().getDisplayId());
                    r.put("rating", rev.getRating());
                    r.put("comment", rev.getComment());
                    r.put("isVisible", rev.getIsVisible());
                    r.put("createdAt", rev.getCreatedAt());
                    return r;
                })
                .collect(Collectors.toList());

        Map<String, Object> reviewsTab = new LinkedHashMap<>();
        reviewsTab.put("averageRating",
                Math.round(avgRating * 10.0) / 10.0);
        reviewsTab.put("totalReviews", reviewList.size());
        reviewsTab.put("reviews", reviews);

        // Booking Stats
        List<com.nestora.nestora_app.entity.BookingRequest> bookings =
                bookingRequestRepository.findByProperty(property);

        Map<String, Object> bookingStats = new LinkedHashMap<>();
        bookingStats.put("totalRequests", bookings.size());
        bookingStats.put("pendingRequests", bookings.stream()
                .filter(b -> b.getStatus() ==
                        com.nestora.nestora_app.enums.BookingStatus.PENDING)
                .count());
        bookingStats.put("acceptedRequests", bookings.stream()
                .filter(b -> b.getStatus() ==
                        com.nestora.nestora_app.enums.BookingStatus.ACCEPTED)
                .count());
        bookingStats.put("rejectedRequests", bookings.stream()
                .filter(b -> b.getStatus() ==
                        com.nestora.nestora_app.enums.BookingStatus.REJECTED)
                .count());

        // Final Response — Sab ek saath
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("overview", overview);
        response.put("owner", ownerDetail);
        response.put("rooms", rooms);
        response.put("media", media);
        response.put("reviews", reviewsTab);
        response.put("bookingStats", bookingStats);

        return ResponseEntity.ok(
                ApiResponse.success("Property detail fetched", response)
        );
    }

    // =============================================
// ADMIN — All Properties with Owner Info
// =============================================
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllPropertiesForAdmin() {

        List<Map<String, Object>> properties = propertyRepository.findAll()
                .stream()
                .map(p -> {
                    OwnerProfile owner = p.getOwner();
                    User ownerUser = owner.getUser();

                    // Cover image
                    String coverImage = mediaRepository
                            .findByPropertyOrderBySortOrderAsc(p)
                            .stream()
                            .filter(m -> Boolean.TRUE.equals(m.getIsPrimary()))
                            .findFirst()
                            .map(PropertyMedia::getUrl)
                            .orElse(null);

                    // Reviews count
                    int reviewCount = reviewRepository
                            .findByPropertyAndIsVisibleTrue(p).size();

                    Map<String, Object> prop = new LinkedHashMap<>();
                    prop.put("propertyId", p.getId());
                    prop.put("title", p.getTitle());
                    prop.put("propertyType", p.getPropertyType());
                    prop.put("genderAllowed", p.getGenderAllowed());
                    prop.put("city", p.getCity());
                    prop.put("state", p.getState());
                    prop.put("pincode", p.getPincode());
                    prop.put("monthlyRentMin", p.getMonthlyRentMin());
                    prop.put("monthlyRentMax", p.getMonthlyRentMax());
                    prop.put("totalRooms", p.getTotalRooms());
                    prop.put("availableRooms", p.getAvailableRooms());
                    prop.put("isPublished", p.getIsPublished());
                    prop.put("isFeatured", p.getIsFeatured());
                    prop.put("viewCount", p.getViewCount());
                    prop.put("coverImage", coverImage);
                    prop.put("totalReviews", reviewCount);
                    prop.put("createdAt", p.getCreatedAt());

                    // Owner summary
                    Map<String, Object> ownerSummary = new LinkedHashMap<>();
                    ownerSummary.put("ownerId", owner.getId());
                    ownerSummary.put("userId", ownerUser.getId());
                    ownerSummary.put("name", ownerUser.getName());
                    ownerSummary.put("email", ownerUser.getEmail());
                    ownerSummary.put("phone", ownerUser.getPhone());
                    ownerSummary.put("displayId", ownerUser.getDisplayId());
                    ownerSummary.put("businessName", owner.getBusinessName());
                    ownerSummary.put("verificationStatus",
                            owner.getVerificationStatus());
                    ownerSummary.put("subscriptionPlan",
                            owner.getSubscriptionPlan());
                    ownerSummary.put("subscriptionStatus",
                            owner.getSubscriptionStatus());

                    prop.put("owner", ownerSummary);
                    return prop;
                })
                .sorted((a, b) -> {
                    // Pending pehle
                    boolean aPublished = (Boolean) a.get("isPublished");
                    boolean bPublished = (Boolean) b.get("isPublished");
                    if (!aPublished && bPublished) return -1;
                    if (aPublished && !bPublished) return 1;
                    return 0;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                ApiResponse.success("All properties fetched", properties)
        );
    }
}