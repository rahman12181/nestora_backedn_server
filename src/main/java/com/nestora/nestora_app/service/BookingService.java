package com.nestora.nestora_app.service;

import com.nestora.nestora_app.dto.request.BookingRespondRequest;
import com.nestora.nestora_app.dto.response.BookingResponse;
import com.nestora.nestora_app.entity.*;
import com.nestora.nestora_app.enums.BookingStatus;
import com.nestora.nestora_app.enums.NotificationType;
import com.nestora.nestora_app.exception.AppException;
import com.nestora.nestora_app.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRequestRepository bookingRequestRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final PropertyMediaRepository mediaRepository;
    private final NotificationService notificationService;

    // =============================================
    // GET INCOMING REQUESTS (Owner)
    // =============================================
    public List<BookingResponse> getIncomingRequests(User currentUser) {

        OwnerProfile owner = ownerProfileRepository.findByUser(currentUser)
                .orElseThrow(() -> new AppException(
                        "Owner profile not found", HttpStatus.NOT_FOUND
                ));

        List<Property> ownerProperties = bookingRequestRepository.findAll()
                .stream()
                .map(BookingRequest::getProperty)
                .filter(p -> p.getOwner().getId().equals(owner.getId()))
                .distinct()
                .collect(Collectors.toList());

        return bookingRequestRepository.findByPropertyIn(ownerProperties)
                .stream()
                .map(this::mapToBookingResponse)
                .collect(Collectors.toList());
    }

    // =============================================
    // ACCEPT BOOKING REQUEST
    // =============================================
    @Transactional
    public BookingResponse acceptRequest(User currentUser,
                                         Long requestId,
                                         BookingRespondRequest request) {

        BookingRequest booking = getOwnerBookingRequest(currentUser, requestId);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new AppException(
                    "Only pending requests can be accepted",
                    HttpStatus.BAD_REQUEST
            );
        }

        booking.setStatus(BookingStatus.ACCEPTED);
        booking.setOwnerResponse(request.getResponse());
        booking.setRespondedAt(LocalDateTime.now());
        bookingRequestRepository.save(booking);

        // User ko notification bhejo
        notificationService.createNotification(
                booking.getUser(),
                "Booking Request Accepted! 🎉",
                "Your booking request for " +
                        booking.getProperty().getTitle() +
                        " has been accepted. " + request.getResponse(),
                NotificationType.BOOKING,
                booking.getId()
        );

        return mapToBookingResponse(booking);
    }

    // =============================================
    // REJECT BOOKING REQUEST
    // =============================================
    @Transactional
    public BookingResponse rejectRequest(User currentUser,
                                         Long requestId,
                                         BookingRespondRequest request) {

        BookingRequest booking = getOwnerBookingRequest(currentUser, requestId);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new AppException(
                    "Only pending requests can be rejected",
                    HttpStatus.BAD_REQUEST
            );
        }

        booking.setStatus(BookingStatus.REJECTED);
        booking.setOwnerResponse(request.getResponse());
        booking.setRespondedAt(LocalDateTime.now());
        bookingRequestRepository.save(booking);

        // User ko notification bhejo
        notificationService.createNotification(
                booking.getUser(),
                "Booking Request Update",
                "Your booking request for " +
                        booking.getProperty().getTitle() +
                        " was not accepted. " + request.getResponse(),
                NotificationType.BOOKING,
                booking.getId()
        );

        return mapToBookingResponse(booking);
    }

    // =============================================
    // PRIVATE HELPERS
    // =============================================

    private BookingRequest getOwnerBookingRequest(User currentUser,
                                                  Long requestId) {
        OwnerProfile owner = ownerProfileRepository.findByUser(currentUser)
                .orElseThrow(() -> new AppException(
                        "Owner profile not found", HttpStatus.NOT_FOUND
                ));

        BookingRequest booking = bookingRequestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(
                        "Booking request not found", HttpStatus.NOT_FOUND
                ));

        if (!booking.getProperty().getOwner().getId().equals(owner.getId())) {
            throw new AppException(
                    "Not authorized to respond to this request",
                    HttpStatus.FORBIDDEN
            );
        }

        return booking;
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
}