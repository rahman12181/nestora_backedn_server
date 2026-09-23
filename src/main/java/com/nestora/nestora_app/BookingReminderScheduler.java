package com.nestora.nestora_app;

import com.nestora.nestora_app.entity.BookingRequest;
import com.nestora.nestora_app.enums.NotificationType;
import com.nestora.nestora_app.repository.BookingRequestRepository;
import com.nestora.nestora_app.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingReminderScheduler {

    private final BookingRequestRepository bookingRequestRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 */6 * * *")
    @Transactional
    public void remindPendingBookings() {

        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        List<BookingRequest> oldPending = bookingRequestRepository.findOldPendingBookings(threshold);

        log.info("Found {} old pending bookings for reminder", oldPending.size());

        for (BookingRequest booking : oldPending) {
            try {
                notificationService.createNotification(
                        booking.getProperty().getOwner().getUser(),
                        "⏰ Pending Booking Reminder",
                        "Aapki " + booking.getProperty().getTitle() +
                                " ke liye ek booking request 24+ ghante se pending hai. Please respond.",
                        NotificationType.BOOKING,
                        booking.getId()
                );
            } catch (Exception e) {
                log.error("Failed to send reminder for booking {}", booking.getId(), e);
            }
        }
    }
}