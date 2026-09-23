package com.nestora.nestora_app.service;

import com.nestora.nestora_app.dto.response.BookingStatsResponse;
import com.nestora.nestora_app.entity.OwnerProfile;
import com.nestora.nestora_app.entity.Property;
import com.nestora.nestora_app.entity.User;
import com.nestora.nestora_app.enums.BookingStatus;
import com.nestora.nestora_app.exception.AppException;
import com.nestora.nestora_app.repository.BookingRequestRepository;
import com.nestora.nestora_app.repository.OwnerProfileRepository;
import com.nestora.nestora_app.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingAnalyticsService {

    private final BookingRequestRepository bookingRequestRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final PropertyRepository propertyRepository;

    public BookingStatsResponse getStats(User currentUser) {

        OwnerProfile owner = ownerProfileRepository.findByUser(currentUser)
                .orElseThrow(() -> new AppException(
                        "Owner profile not found", HttpStatus.NOT_FOUND));

        List<Property> properties = propertyRepository.findByOwner(owner);

        if (properties == null || properties.isEmpty()) {
            return emptyStats();
        }

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfThisWeek = LocalDate.now()
                .with(java.time.DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime startOfLastWeek = startOfThisWeek.minusWeeks(1);
        LocalDateTime endOfLastWeek = startOfThisWeek.minusSeconds(1);
        LocalDateTime startOfThisMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        Long total = bookingRequestRepository.countByProperties(properties);
        Long pending = bookingRequestRepository.countByPropertiesAndStatus(properties, BookingStatus.PENDING);
        Long accepted = bookingRequestRepository.countByPropertiesAndStatus(properties, BookingStatus.ACCEPTED);
        Long rejected = bookingRequestRepository.countByPropertiesAndStatus(properties, BookingStatus.REJECTED);

        Long todayNew = bookingRequestRepository.countRecent(properties, startOfToday);
        Long todayAccepted = bookingRequestRepository.countAcceptedSince(properties, startOfToday);
        Long todayRejected = bookingRequestRepository.countByPropertiesAndStatusBetween(
                properties, BookingStatus.REJECTED, startOfToday, LocalDateTime.now());

        Long urgent = bookingRequestRepository.countUrgentPending(properties, LocalDateTime.now().minusHours(48));

        Long thisWeek = bookingRequestRepository.countRecent(properties, startOfThisWeek);
        Long lastWeek = bookingRequestRepository.countByPropertiesAndStatusBetween(
                properties, null, startOfLastWeek, endOfLastWeek);

        Long responded = accepted + rejected;
        Double acceptanceRate = responded > 0 ? (accepted * 100.0) / responded : 0.0;

        List<Long> propertyIds = properties.stream().map(Property::getId).collect(Collectors.toList());
        Double avgHours = bookingRequestRepository.avgResponseHoursByPropertyIds(propertyIds);
        if (avgHours == null) avgHours = 0.0;

        Double weeklyGrowth = lastWeek != null && lastWeek > 0
                ? ((thisWeek - lastWeek) * 100.0) / lastWeek
                : (thisWeek > 0 ? 100.0 : 0.0);

        return BookingStatsResponse.builder()
                .todayNew(todayNew)
                .todayAccepted(todayAccepted)
                .todayRejected(todayRejected)
                .totalPending(pending)
                .urgentPending(urgent)
                .totalBookings(total)
                .totalAccepted(accepted)
                .totalRejected(rejected)
                .acceptanceRate(Math.round(acceptanceRate * 10) / 10.0)
                .avgResponseHours(Math.round(avgHours * 10) / 10.0)
                .avgResponseMinutes((int) Math.round(avgHours * 60))   // ✅ (int) cast
                .thisWeekBookings(thisWeek)
                .lastWeekBookings(lastWeek)
                .weeklyGrowthPercent(Math.round(weeklyGrowth * 10) / 10.0)
                .thisMonthBookings(bookingRequestRepository.countRecent(properties, startOfThisMonth))
                .unreadBookingCount(pending)
                .build();
    }

    private BookingStatsResponse emptyStats() {
        return BookingStatsResponse.builder()
                .todayNew(0L).todayAccepted(0L).todayRejected(0L)
                .totalPending(0L).urgentPending(0L)
                .totalBookings(0L).totalAccepted(0L).totalRejected(0L)
                .acceptanceRate(0.0)
                .avgResponseHours(0.0)
                .avgResponseMinutes(0)   // ✅ int 0
                .thisWeekBookings(0L).lastWeekBookings(0L).weeklyGrowthPercent(0.0)
                .thisMonthBookings(0L).unreadBookingCount(0L)
                .build();
    }
}