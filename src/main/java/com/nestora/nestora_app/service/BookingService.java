package com.nestora.nestora_app.service;

import com.nestora.nestora_app.dto.request.BookingFilterRequest;
import com.nestora.nestora_app.dto.request.BookingRespondRequest;
import com.nestora.nestora_app.dto.response.BookingResponse;
import com.nestora.nestora_app.dto.response.BookingTimelineResponse;
import com.nestora.nestora_app.entity.*;
import com.nestora.nestora_app.enums.BookingStatus;
import com.nestora.nestora_app.enums.NotificationType;
import com.nestora.nestora_app.exception.AppException;
import com.nestora.nestora_app.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRequestRepository bookingRequestRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyMediaRepository mediaRepository;
    private final BookingTimelineRepository timelineRepository;
    private final NotificationService notificationService;

    // =============================================
    // GET INCOMING REQUESTS (Owner)
    // =============================================
    public List<BookingResponse> getIncomingRequests(User currentUser) {
        return getIncomingRequests(currentUser, new BookingFilterRequest());
    }

    public List<BookingResponse> getIncomingRequests(User currentUser, BookingFilterRequest filter) {

        OwnerProfile owner = ownerProfileRepository.findByUser(currentUser)
                .orElseThrow(() -> new AppException(
                        "Owner profile not found", HttpStatus.NOT_FOUND));

        List<Property> ownerProperties = propertyRepository.findByOwner(owner);

        if (ownerProperties == null || ownerProperties.isEmpty()) {
            return List.of();
        }

        List<BookingRequest> bookings;

        if (filter.getSearchQuery() != null && !filter.getSearchQuery().trim().isEmpty()) {
            bookings = bookingRequestRepository.searchByPropertyIn(ownerProperties, filter.getSearchQuery().trim());
        } else if (filter.getPropertyId() != null) {
            bookings = bookingRequestRepository.findByPropertyInAndPropertyId(ownerProperties, filter.getPropertyId());
        } else if (filter.getStatus() != null) {
            bookings = bookingRequestRepository.findByPropertyInAndStatus(ownerProperties, filter.getStatus());
        } else {
            bookings = bookingRequestRepository.findByPropertyInWithDetails(ownerProperties);
        }

        LocalDateTime now = LocalDateTime.now();

        if (Boolean.TRUE.equals(filter.getOnlyNew())) {
            LocalDateTime cutoff = now.minusHours(24);
            bookings = bookings.stream()
                    .filter(b -> b.getRequestedAt() != null && b.getRequestedAt().isAfter(cutoff))
                    .collect(Collectors.toList());
        }

        if (Boolean.TRUE.equals(filter.getOnlyUrgent())) {
            LocalDateTime threshold = now.minusHours(48);
            bookings = bookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.PENDING)
                    .filter(b -> b.getRequestedAt() != null && b.getRequestedAt().isBefore(threshold))
                    .collect(Collectors.toList());
        }

        bookings = applySort(bookings, filter.getSortBy());

        return bookings.stream()
                .map(b -> mapToBookingResponse(b, ownerProperties))
                .collect(Collectors.toList());
    }

    // =============================================
    // GET SINGLE BOOKING DETAIL
    // =============================================
    public BookingResponse getBookingDetail(User currentUser, Long requestId) {
        OwnerProfile owner = ownerProfileRepository.findByUser(currentUser)
                .orElseThrow(() -> new AppException("Owner profile not found", HttpStatus.NOT_FOUND));

        BookingRequest booking = bookingRequestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new AppException("Booking request not found", HttpStatus.NOT_FOUND));

        if (!booking.getProperty().getOwner().getId().equals(owner.getId())) {
            throw new AppException("Not authorized", HttpStatus.FORBIDDEN);
        }

        List<Property> ownerProperties = propertyRepository.findByOwner(owner);
        return mapToBookingResponse(booking, ownerProperties);
    }

    // =============================================
    // GET TIMELINE
    // =============================================
    public List<BookingTimelineResponse> getTimeline(User currentUser, Long requestId) {

        OwnerProfile owner = ownerProfileRepository.findByUser(currentUser)
                .orElseThrow(() -> new AppException("Owner profile not found", HttpStatus.NOT_FOUND));

        BookingRequest booking = bookingRequestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new AppException("Booking request not found", HttpStatus.NOT_FOUND));

        if (!booking.getProperty().getOwner().getId().equals(owner.getId())) {
            throw new AppException("Not authorized", HttpStatus.FORBIDDEN);
        }

        List<BookingTimelineEvent> events = timelineRepository
                .findByBookingRequestOrderByCreatedAtAsc(booking);

        if (!events.isEmpty()) {
            return events.stream()
                    .map(e -> BookingTimelineResponse.builder()
                            .eventType(e.getEventType())
                            .title(e.getTitle())
                            .description(e.getDescription())
                            .timestamp(e.getCreatedAt())
                            .icon(getTimelineIcon(e.getEventType()))
                            .color(getTimelineColor(e.getEventType()))
                            .build())
                    .collect(Collectors.toList());
        }

        return buildTimelineFromBooking(booking);
    }

    // =============================================
    // ACCEPT
    // =============================================
    @Transactional
    public BookingResponse acceptRequest(User currentUser, Long requestId, BookingRespondRequest request) {

        OwnerProfile owner = ownerProfileRepository.findByUser(currentUser)
                .orElseThrow(() -> new AppException("Owner profile not found", HttpStatus.NOT_FOUND));

        BookingRequest booking = bookingRequestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new AppException("Booking request not found", HttpStatus.NOT_FOUND));

        if (!booking.getProperty().getOwner().getId().equals(owner.getId())) {
            throw new AppException("Not authorized", HttpStatus.FORBIDDEN);
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new AppException("Only pending requests can be accepted", HttpStatus.BAD_REQUEST);
        }

        booking.setStatus(BookingStatus.ACCEPTED);
        booking.setOwnerResponse(request.getResponse());
        booking.setRespondedAt(LocalDateTime.now());
        bookingRequestRepository.save(booking);

        addTimelineEvent(booking, "ACCEPTED", "Booking Accepted",
                "Owner ne booking accept ki: " + request.getResponse());

        notificationService.createNotification(
                booking.getUser(),
                "Booking Request Accepted! 🎉",
                "Your booking request for " + booking.getProperty().getTitle() +
                        " has been accepted. " + request.getResponse(),
                NotificationType.BOOKING,
                booking.getId()
        );

        List<Property> ownerProperties = propertyRepository.findByOwner(owner);
        return mapToBookingResponse(booking, ownerProperties);
    }

    // =============================================
    // REJECT
    // =============================================
    @Transactional
    public BookingResponse rejectRequest(User currentUser, Long requestId, BookingRespondRequest request) {

        OwnerProfile owner = ownerProfileRepository.findByUser(currentUser)
                .orElseThrow(() -> new AppException("Owner profile not found", HttpStatus.NOT_FOUND));

        BookingRequest booking = bookingRequestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new AppException("Booking request not found", HttpStatus.NOT_FOUND));

        if (!booking.getProperty().getOwner().getId().equals(owner.getId())) {
            throw new AppException("Not authorized", HttpStatus.FORBIDDEN);
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new AppException("Only pending requests can be rejected", HttpStatus.BAD_REQUEST);
        }

        booking.setStatus(BookingStatus.REJECTED);
        booking.setOwnerResponse(request.getResponse());
        booking.setRespondedAt(LocalDateTime.now());
        bookingRequestRepository.save(booking);

        addTimelineEvent(booking, "REJECTED", "Booking Rejected",
                "Owner ne booking reject ki: " + request.getResponse());

        notificationService.createNotification(
                booking.getUser(),
                "Booking Request Update",
                "Your booking request for " + booking.getProperty().getTitle() +
                        " was not accepted. " + request.getResponse(),
                NotificationType.BOOKING,
                booking.getId()
        );

        List<Property> ownerProperties = propertyRepository.findByOwner(owner);
        return mapToBookingResponse(booking, ownerProperties);
    }

    // =============================================
    // UNDO (5 min window)
    // =============================================
    @Transactional
    public BookingResponse undoResponse(User currentUser, Long requestId) {

        OwnerProfile owner = ownerProfileRepository.findByUser(currentUser)
                .orElseThrow(() -> new AppException("Owner profile not found", HttpStatus.NOT_FOUND));

        BookingRequest booking = bookingRequestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new AppException("Booking request not found", HttpStatus.NOT_FOUND));

        if (!booking.getProperty().getOwner().getId().equals(owner.getId())) {
            throw new AppException("Not authorized", HttpStatus.FORBIDDEN);
        }

        if (booking.getRespondedAt() == null ||
                Duration.between(booking.getRespondedAt(), LocalDateTime.now()).toMinutes() > 5) {
            throw new AppException("Undo time expired (5 minutes)", HttpStatus.BAD_REQUEST);
        }

        booking.setStatus(BookingStatus.PENDING);
        booking.setOwnerResponse(null);
        booking.setRespondedAt(null);
        bookingRequestRepository.save(booking);

        addTimelineEvent(booking, "UNDONE", "Response Undone",
                "Owner ne apna response undo kar diya");

        List<Property> ownerProperties = propertyRepository.findByOwner(owner);
        return mapToBookingResponse(booking, ownerProperties);
    }

    // =============================================
    // PRIVATE HELPERS
    // =============================================

    private List<BookingRequest> applySort(List<BookingRequest> bookings, String sortBy) {
        if (sortBy == null) sortBy = "newest";

        switch (sortBy.toLowerCase()) {
            case "oldest":
                return bookings.stream()
                        .sorted(Comparator.comparing(BookingRequest::getRequestedAt,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .collect(Collectors.toList());

            case "moveindate":
                return bookings.stream()
                        .sorted(Comparator.comparing(BookingRequest::getMoveInDate,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .collect(Collectors.toList());

            case "urgent":
                return bookings.stream()
                        .sorted(Comparator.comparing(BookingRequest::getRequestedAt,
                                        Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(b -> b.getStatus() == BookingStatus.PENDING ? 0 : 1))
                        .collect(Collectors.toList());

            case "newest":
            default:
                return bookings.stream()
                        .sorted(Comparator.comparing(BookingRequest::getRequestedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                        .collect(Collectors.toList());
        }
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

    private void addTimelineEvent(BookingRequest booking, String type, String title, String description) {
        try {
            BookingTimelineEvent event = BookingTimelineEvent.builder()
                    .bookingRequest(booking)
                    .eventType(type)
                    .title(title)
                    .description(description)
                    .build();
            timelineRepository.save(event);
        } catch (Exception ignored) {
        }
    }

    private List<BookingTimelineResponse> buildTimelineFromBooking(BookingRequest booking) {
        List<BookingTimelineResponse> timeline = new java.util.ArrayList<>();

        if (booking.getRequestedAt() != null) {
            timeline.add(BookingTimelineResponse.builder()
                    .eventType("REQUESTED")
                    .title("Booking Request Received")
                    .description(booking.getUser() != null
                            ? booking.getUser().getName() + " ne booking request bheji"
                            : "Request received")
                    .timestamp(booking.getRequestedAt())
                    .icon("request")
                    .color("#7C3AED")
                    .build());
        }

        if (booking.getStatus() == BookingStatus.ACCEPTED && booking.getRespondedAt() != null) {
            timeline.add(BookingTimelineResponse.builder()
                    .eventType("ACCEPTED")
                    .title("Booking Accepted")
                    .description(booking.getOwnerResponse() != null ? booking.getOwnerResponse() : "Accepted")
                    .timestamp(booking.getRespondedAt())
                    .icon("check")
                    .color("#22C55E")
                    .build());
        }

        if (booking.getStatus() == BookingStatus.REJECTED && booking.getRespondedAt() != null) {
            timeline.add(BookingTimelineResponse.builder()
                    .eventType("REJECTED")
                    .title("Booking Rejected")
                    .description(booking.getOwnerResponse() != null ? booking.getOwnerResponse() : "Rejected")
                    .timestamp(booking.getRespondedAt())
                    .icon("close")
                    .color("#EF4444")
                    .build());
        }

        return timeline;
    }

    private String getTimelineIcon(String eventType) {
        switch (eventType) {
            case "REQUESTED": return "request";
            case "ACCEPTED": return "check";
            case "REJECTED": return "close";
            case "PAID": return "payment";
            case "MOVED_IN": return "home";
            case "CANCELLED": return "cancel";
            case "UNDONE": return "undo";
            default: return "info";
        }
    }

    private String getTimelineColor(String eventType) {
        switch (eventType) {
            case "REQUESTED": return "#7C3AED";
            case "ACCEPTED": return "#22C55E";
            case "REJECTED": return "#EF4444";
            case "PAID": return "#F59E0B";
            case "MOVED_IN": return "#4ECDC4";
            case "CANCELLED": return "#8A8FA3";
            default: return "#8A8FA3";
        }
    }

    // =============================================
    // MAIN MAPPER
    // =============================================
    private BookingResponse mapToBookingResponse(BookingRequest booking, List<Property> ownerProperties) {

        User student = booking.getUser();
        Property property = booking.getProperty();

        boolean isNew = booking.getRequestedAt() != null &&
                Duration.between(booking.getRequestedAt(), LocalDateTime.now()).toHours() < 24;

        boolean isUrgent = booking.getStatus() == BookingStatus.PENDING &&
                booking.getRequestedAt() != null &&
                Duration.between(booking.getRequestedAt(), LocalDateTime.now()).toHours() >= 48;

        Long studentTotalBookings = 0L;
        Long studentAcceptedBookings = 0L;
        Boolean hasActiveBooking = false;
        Boolean isRepeat = false;

        if (student != null) {
            studentTotalBookings = bookingRequestRepository.countByUser(student);
            studentAcceptedBookings = bookingRequestRepository.countAcceptedByUser(student);
            hasActiveBooking = bookingRequestRepository.hasActiveBookingExcluding(student, booking.getId());
            isRepeat = bookingRequestRepository.hasBookedOwnerBefore(student, ownerProperties, booking.getId());
        }

        Integer availableRooms = 0;
        Boolean isPublished = false;
        if (property != null) {
            availableRooms = property.getAvailableRooms() != null ? property.getAvailableRooms() : 0;
            isPublished = Boolean.TRUE.equals(property.getIsPublished());
        }

        return BookingResponse.builder()
                .requestId(booking.getId())
                .propertyId(property != null ? property.getId() : null)
                .propertyTitle(property != null ? property.getTitle() : null)
                .propertyCity(property != null ? property.getCity() : null)
                .propertyState(property != null ? property.getState() : null)
                .propertyAddress(property != null ? property.getAddressLine() : null)
                .coverImage(property != null ? getCoverImage(property) : null)
                .roomId(booking.getRoom() != null ? booking.getRoom().getId() : null)
                .roomNumber(booking.getRoom() != null ? booking.getRoom().getRoomNumber() : null)
                .roomType(booking.getRoom() != null ? String.valueOf(booking.getRoom().getRoomType()) : null)
                .monthlyRent(booking.getRoom() != null ? booking.getRoom().getMonthlyRent() : null)
                .moveInDate(booking.getMoveInDate())
                .durationMonths(booking.getDurationMonths())
                .message(booking.getMessage())
                .status(booking.getStatus())
                .ownerResponse(booking.getOwnerResponse())
                .requestedAt(booking.getRequestedAt())
                .respondedAt(booking.getRespondedAt())

                .studentId(student != null ? student.getId() : null)
                .studentName(student != null ? student.getName() : "Unknown User")
                .studentDisplayId(student != null ? student.getDisplayId() : null)
                .studentEmail(student != null ? student.getEmail() : null)
                .studentPhone(student != null ? student.getPhone() : null)
                .studentProfilePic(student != null ? student.getProfilePic() : null)
                .studentJoinedAt(student != null ? student.getCreatedAt() : null)
                .studentTotalBookings(studentTotalBookings)
                .studentAcceptedBookings(studentAcceptedBookings)
                .studentHasActiveBooking(hasActiveBooking)
                .isRepeatStudent(isRepeat)

                .isPaid(false)
                .paymentStatus("CREATED")

                .isNew(isNew)
                .isUrgent(isUrgent)
                .hasUnreadMessages(false)
                .daysSinceRequested(booking.getRequestedAt() != null
                        ? Duration.between(booking.getRequestedAt(), LocalDateTime.now()).toDays()
                        : 0)

                .availableRooms(availableRooms)
                .isPropertyPublished(isPublished)

                .build();
    }
}