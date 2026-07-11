package com.nestora.nestora_app.service;


import com.nestora.nestora_app.dto.request.*;
import com.nestora.nestora_app.dto.response.*;
import com.nestora.nestora_app.entity.*;
import com.nestora.nestora_app.enums.BookingStatus;
import com.nestora.nestora_app.enums.VerificationStatus;
import com.nestora.nestora_app.exception.AppException;
import com.nestora.nestora_app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.nestora.nestora_app.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final SavedPropertyRepository savedPropertyRepository;
    private final BookingRequestRepository bookingRequestRepository;
    private final ReviewRepository reviewRepository;
    private final PropertyRepository propertyRepository;
    private final RoomRepository roomRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final PropertyMediaRepository mediaRepository;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;
    private final NotificationService notificationService;

    // =============================================
    // GET PROFILE
    // =============================================
    public UserProfileResponse getProfile(User currentUser) {
        return UserProfileResponse.builder()
                .userId(currentUser.getId())
                .displayId(currentUser.getDisplayId())
                .name(currentUser.getName())
                .email(currentUser.getEmail())
                .phone(currentUser.getPhone())
                .profilePic(currentUser.getProfilePic())
                .role(currentUser.getRole())
                .isEmailVerified(currentUser.getIsEmailVerified())
                .createdAt(currentUser.getCreatedAt())
                .build();
    }

    // =============================================
    // UPDATE PROFILE
    // =============================================
    @Transactional
    public UserProfileResponse updateProfile(User currentUser,
                                             UpdateProfileRequest request) {
        if (request.getName() != null) {
            currentUser.setName(request.getName());
        }

        if (request.getPhone() != null) {
            // Phone already kisi aur ka to nahi?
            if (userRepository.existsByPhone(request.getPhone()) &&
                    !request.getPhone().equals(currentUser.getPhone())) {
                throw new AppException(
                        "Phone number already in use", HttpStatus.CONFLICT
                );
            }
            currentUser.setPhone(request.getPhone());
        }

        userRepository.save(currentUser);
        return getProfile(currentUser);
    }

    // =============================================
    // UPLOAD PROFILE PIC
    // =============================================
    @Transactional
    public UserProfileResponse uploadProfilePic(User currentUser,
                                                MultipartFile file) {
        String url = cloudinaryService.uploadImage(file, "profile-pics");
        currentUser.setProfilePic(url);
        userRepository.save(currentUser);
        return getProfile(currentUser);
    }

    // =============================================
    // CHANGE PASSWORD
    // =============================================
    @Transactional
    public String changePassword(User currentUser,
                                 ChangePasswordRequest request) {

        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                currentUser.getPasswordHash())) {
            throw new AppException(
                    "Current password is incorrect", HttpStatus.BAD_REQUEST
            );
        }

        if (passwordEncoder.matches(
                request.getNewPassword(),
                currentUser.getPasswordHash())) {
            throw new AppException(
                    "New password cannot be same as current password",
                    HttpStatus.BAD_REQUEST
            );
        }

        currentUser.setPasswordHash(
                passwordEncoder.encode(request.getNewPassword())
        );
        userRepository.save(currentUser);

        return "Password changed successfully";
    }

    // =============================================
    // SAVE PROPERTY (Wishlist)
    // =============================================
    @Transactional
    public String saveProperty(User currentUser, Long propertyId) {

        Property property = getPublishedProperty(propertyId);

        if (savedPropertyRepository.existsByUserAndProperty(currentUser, property)) {
            throw new AppException(
                    "Property already in wishlist", HttpStatus.CONFLICT
            );
        }

        SavedProperty saved = SavedProperty.builder()
                .user(currentUser)
                .property(property)
                .build();

        savedPropertyRepository.save(saved);
        return "Property saved to wishlist";
    }

    // =============================================
    // REMOVE SAVED PROPERTY
    // =============================================
    @Transactional
    public String removeSavedProperty(User currentUser, Long propertyId) {

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new AppException(
                        "Property not found", HttpStatus.NOT_FOUND
                ));

        SavedProperty saved = savedPropertyRepository
                .findByUserAndProperty(currentUser, property)
                .orElseThrow(() -> new AppException(
                        "Property not in wishlist", HttpStatus.NOT_FOUND
                ));

        savedPropertyRepository.delete(saved);
        return "Property removed from wishlist";
    }

    // =============================================
    // GET SAVED PROPERTIES
    // =============================================
    public List<SavedPropertyResponse> getSavedProperties(User currentUser) {
        return savedPropertyRepository.findByUser(currentUser)
                .stream()
                .map(this::mapToSavedPropertyResponse)
                .collect(Collectors.toList());
    }

    // =============================================
    // SEND BOOKING REQUEST
    // =============================================
    @Transactional
    public BookingResponse sendBookingRequest(User currentUser,
                                              BookingCreateRequest request) {

        Property property = getPublishedProperty(request.getPropertyId());

        Room room = null;
        if (request.getRoomId() != null) {
            room = roomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new AppException(
                            "Room not found", HttpStatus.NOT_FOUND
                    ));
        }

        BookingRequest booking = BookingRequest.builder()
                .user(currentUser)
                .property(property)
                .room(room)
                .moveInDate(request.getMoveInDate())
                .durationMonths(request.getDurationMonths())
                .message(request.getMessage())
                .status(BookingStatus.PENDING)
                .build();

        BookingRequest saved = bookingRequestRepository.save(booking);

        // ✅ ADDED — Owner ko notify karo
        User ownerUser = property.getOwner().getUser();
        notificationService.createNotification(
                ownerUser,
                "New Booking Request 📩",
                currentUser.getName() + " has requested to book " +
                        property.getTitle(),
                NotificationType.BOOKING,
                saved.getId()
        );

        return mapToBookingResponse(saved);
    }

    // =============================================
    // GET MY BOOKING REQUESTS
    // =============================================
    public List<BookingResponse> getMyBookingRequests(User currentUser) {
        return bookingRequestRepository.findByUser(currentUser)
                .stream()
                .map(this::mapToBookingResponse)
                .collect(Collectors.toList());
    }

    // =============================================
    // CANCEL BOOKING REQUEST
    // =============================================
    @Transactional
    public String cancelBookingRequest(User currentUser, Long requestId) {

        BookingRequest booking = bookingRequestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(
                        "Booking request not found", HttpStatus.NOT_FOUND
                ));

        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw new AppException(
                    "Not authorized to cancel this request", HttpStatus.FORBIDDEN
            );
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new AppException(
                    "Only pending requests can be cancelled", HttpStatus.BAD_REQUEST
            );
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRequestRepository.save(booking);

        return "Booking request cancelled";
    }

    // =============================================
    // WRITE REVIEW
    // =============================================
    @Transactional
    public ReviewResponse writeReview(User currentUser,
                                      Long propertyId,
                                      ReviewCreateRequest request) {

        Property property = getPublishedProperty(propertyId);

        if (reviewRepository.existsByUserAndProperty(currentUser, property)) {
            throw new AppException(
                    "You have already reviewed this property", HttpStatus.CONFLICT
            );
        }

        Review review = Review.builder()
                .user(currentUser)
                .property(property)
                .rating(request.getRating())
                .comment(request.getComment())
                .isVisible(true)
                .build();

        Review saved = reviewRepository.save(review);
        return mapToReviewResponse(saved);
    }

    // =============================================
    // PRIVATE HELPERS
    // =============================================

    private Property getPublishedProperty(Long propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new AppException(
                        "Property not found", HttpStatus.NOT_FOUND
                ));

        if (!property.getIsPublished()) {
            throw new AppException(
                    "Property is not available", HttpStatus.NOT_FOUND
            );
        }

        return property;
    }

    private String getCoverImage(Property property) {
        return mediaRepository
                .findByPropertyOrderBySortOrderAsc(property)
                .stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsPrimary()))
                .findFirst()
                .map(PropertyMedia::getUrl)
                .orElse(null);
    }

    private SavedPropertyResponse mapToSavedPropertyResponse(SavedProperty saved) {
        Property property = saved.getProperty();
        OwnerProfile owner = ownerProfileRepository
                .findByUser(property.getOwner().getUser())
                .orElse(null);

        return SavedPropertyResponse.builder()
                .savedId(saved.getId())
                .propertyId(property.getId())
                .title(property.getTitle())
                .propertyType(property.getPropertyType())
                .genderAllowed(property.getGenderAllowed())
                .city(property.getCity())
                .state(property.getState())
                .monthlyRentMin(property.getMonthlyRentMin())
                .monthlyRentMax(property.getMonthlyRentMax())
                .coverImage(getCoverImage(property))
                .availableRooms(property.getAvailableRooms())
                .isVerifiedOwner(owner != null &&
                        owner.getVerificationStatus() == VerificationStatus.VERIFIED)
                .savedAt(saved.getSavedAt())
                .build();
    }

    private BookingResponse mapToBookingResponse(BookingRequest booking) {
        return BookingResponse.builder()
                .requestId(booking.getId())
                .propertyId(booking.getProperty().getId())
                .propertyTitle(booking.getProperty().getTitle())
                .propertyCity(booking.getProperty().getCity())
                .coverImage(getCoverImage(booking.getProperty()))
                .roomId(booking.getRoom() != null ?
                        booking.getRoom().getId() : null)
                .roomNumber(booking.getRoom() != null ?
                        booking.getRoom().getRoomNumber() : null)
                .moveInDate(booking.getMoveInDate())
                .durationMonths(booking.getDurationMonths())
                .message(booking.getMessage())
                .status(booking.getStatus())
                .ownerResponse(booking.getOwnerResponse())
                .requestedAt(booking.getRequestedAt())
                .respondedAt(booking.getRespondedAt())
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
