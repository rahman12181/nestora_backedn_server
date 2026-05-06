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

    // =============================================
    // ADD PROPERTY
    // =============================================
    @Transactional
    public PropertyResponse addProperty(User currentUser,
                                        PropertyCreateRequest request) {

        OwnerProfile owner = getVerifiedOwner(currentUser);

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
}
