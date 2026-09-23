package com.nestora.nestora_app.repository;

import com.nestora.nestora_app.entity.BookingRequest;
import com.nestora.nestora_app.entity.BookingTimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingTimelineRepository extends JpaRepository<BookingTimelineEvent, Long> {
    List<BookingTimelineEvent> findByBookingRequestOrderByCreatedAtAsc(BookingRequest bookingRequest);
}