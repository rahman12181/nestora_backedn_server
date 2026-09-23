package com.nestora.nestora_app.service;

import com.nestora.nestora_app.dto.response.*;
import com.nestora.nestora_app.entity.*;
import com.nestora.nestora_app.enums.BookingStatus;
import com.nestora.nestora_app.exception.AppException;
import com.nestora.nestora_app.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OwnerDashboardService {

    private final OwnerProfileRepository ownerProfileRepository;
    private final PropertyRepository propertyRepository;
    private final BookingRequestRepository bookingRequestRepository;
    private final RoomRepository roomRepository;
    private final OwnerActivityLogRepository activityLogRepository;
    private final MessageRepository messageRepository;
    private final NotificationRepository notificationRepository;
    private final PropertyAccessSubscriptionRepository propertyAccessSubRepo;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMM");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    // ============================================
    // 1. REVENUE SUMMARY
    // ============================================
    public RevenueSummaryResponse getRevenueSummary(User currentUser) {

        OwnerProfile owner = getOwner(currentUser);
        List<Property> properties = propertyRepository.findByOwner(owner);

        if (properties.isEmpty()) {
            return emptyRevenue();
        }

        LocalDate today = LocalDate.now();
        LocalDateTime startOfThisMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfLastMonth = startOfThisMonth.minusMonths(1);
        LocalDateTime endOfLastMonth = startOfThisMonth.minusSeconds(1);
        LocalDateTime startOfThisYear = today.withDayOfYear(1).atStartOfDay();
        LocalDateTime start30DaysAgo = today.minusDays(29).atStartOfDay();

        // ============ FETCH PAID BOOKINGS ============
        List<BookingRequest> allBookings =
                bookingRequestRepository.findByPropertyIn(properties);

        // Filter paid only (if payment integration done)
        // For now assume paid = ACCEPTED bookings
        List<BookingRequest> paidBookings = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.ACCEPTED
                        || b.getStatus() == BookingStatus.COMPLETED)
                .collect(Collectors.toList());

        // ============ THIS MONTH ============
        BigDecimal thisMonthTotal = paidBookings.stream()
                .filter(b -> b.getRespondedAt() != null
                        && b.getRespondedAt().isAfter(startOfThisMonth))
                .map(this::calculateBookingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ============ LAST MONTH ============
        BigDecimal lastMonthTotal = paidBookings.stream()
                .filter(b -> b.getRespondedAt() != null
                        && b.getRespondedAt().isAfter(startOfLastMonth)
                        && b.getRespondedAt().isBefore(endOfLastMonth))
                .map(this::calculateBookingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ============ GROWTH ============
        Double growthPercent = lastMonthTotal.compareTo(BigDecimal.ZERO) > 0
                ? thisMonthTotal.subtract(lastMonthTotal)
                .divide(lastMonthTotal, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue()
                : (thisMonthTotal.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0);

        // ============ YEAR TOTAL ============
        BigDecimal thisYearTotal = paidBookings.stream()
                .filter(b -> b.getRespondedAt() != null
                        && b.getRespondedAt().isAfter(startOfThisYear))
                .map(this::calculateBookingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ============ COLLECTION (Paid vs Pending) ============
        BigDecimal paidAmount = thisMonthTotal;
        BigDecimal pendingAmount = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING)
                .map(this::calculateBookingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpected = paidAmount.add(pendingAmount);
        Double collectionRate = totalExpected.compareTo(BigDecimal.ZERO) > 0
                ? paidAmount.divide(totalExpected, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue()
                : 0.0;

        // ============ MONTHLY TREND (6 months) ============
        List<RevenueSummaryResponse.MonthlyRevenue> monthlyTrend = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate month = today.minusMonths(i);
            LocalDateTime mStart = month.withDayOfMonth(1).atStartOfDay();
            LocalDateTime mEnd = mStart.plusMonths(1).minusSeconds(1);

            BigDecimal amount = paidBookings.stream()
                    .filter(b -> b.getRespondedAt() != null
                            && b.getRespondedAt().isAfter(mStart)
                            && b.getRespondedAt().isBefore(mEnd))
                    .map(this::calculateBookingAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long count = paidBookings.stream()
                    .filter(b -> b.getRespondedAt() != null
                            && b.getRespondedAt().isAfter(mStart)
                            && b.getRespondedAt().isBefore(mEnd))
                    .count();

            monthlyTrend.add(RevenueSummaryResponse.MonthlyRevenue.builder()
                    .month(month.format(MONTH_FMT))
                    .year(month.getYear())
                    .amount(amount)
                    .transactions(count)
                    .build());
        }

        // ============ DAILY TREND (30 days) ============
        List<RevenueSummaryResponse.DailyRevenue> dailyTrend = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            LocalDateTime dStart = day.atStartOfDay();
            LocalDateTime dEnd = dStart.plusDays(1).minusSeconds(1);

            BigDecimal amount = paidBookings.stream()
                    .filter(b -> b.getRespondedAt() != null
                            && b.getRespondedAt().isAfter(dStart)
                            && b.getRespondedAt().isBefore(dEnd))
                    .map(this::calculateBookingAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            dailyTrend.add(RevenueSummaryResponse.DailyRevenue.builder()
                    .date(day.toString())
                    .amount(amount)
                    .build());
        }

        return RevenueSummaryResponse.builder()
                .thisMonthTotal(thisMonthTotal)
                .lastMonthTotal(lastMonthTotal)
                .growthPercent(Math.round(growthPercent * 10) / 10.0)
                .paidAmount(paidAmount)
                .pendingAmount(pendingAmount)
                .collectionRate(Math.round(collectionRate * 10) / 10.0)
                .payoutsCompleted(paidAmount)
                .payoutsPending(pendingAmount)
                .thisYearTotal(thisYearTotal)
                .totalTransactions((long) paidBookings.size())
                .monthlyTrend(monthlyTrend)
                .dailyTrend(dailyTrend)
                .build();
    }

    private BigDecimal calculateBookingAmount(BookingRequest b) {
        if (b.getRoom() != null && b.getRoom().getMonthlyRent() != null) {
            int months = b.getDurationMonths() != null ? b.getDurationMonths() : 1;
            return b.getRoom().getMonthlyRent()
                    .multiply(BigDecimal.valueOf(months));
        }
        if (b.getProperty() != null && b.getProperty().getMonthlyRentMin() != null) {
            int months = b.getDurationMonths() != null ? b.getDurationMonths() : 1;
            return b.getProperty().getMonthlyRentMin()
                    .multiply(BigDecimal.valueOf(months));
        }
        return BigDecimal.ZERO;
    }

    private RevenueSummaryResponse emptyRevenue() {
        return RevenueSummaryResponse.builder()
                .thisMonthTotal(BigDecimal.ZERO)
                .lastMonthTotal(BigDecimal.ZERO)
                .growthPercent(0.0)
                .paidAmount(BigDecimal.ZERO)
                .pendingAmount(BigDecimal.ZERO)
                .collectionRate(0.0)
                .payoutsCompleted(BigDecimal.ZERO)
                .payoutsPending(BigDecimal.ZERO)
                .thisYearTotal(BigDecimal.ZERO)
                .totalTransactions(0L)
                .monthlyTrend(new ArrayList<>())
                .dailyTrend(new ArrayList<>())
                .build();
    }

    // ============================================
    // 2. ACTION REQUIRED
    // ============================================
    public ActionRequiredResponse getActionsRequired(User currentUser) {

        OwnerProfile owner = getOwner(currentUser);
        List<Property> properties = propertyRepository.findByOwner(owner);
        List<ActionRequiredResponse.ActionItem> items = new ArrayList<>();

        // ============ 1. PENDING BOOKINGS > 24h ============
        LocalDateTime threshold24h = LocalDateTime.now().minusHours(24);
        List<BookingRequest> oldPending = bookingRequestRepository
                .findByPropertyIn(properties).stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING)
                .filter(b -> b.getRequestedAt() != null
                        && b.getRequestedAt().isBefore(threshold24h))
                .collect(Collectors.toList());

        if (!oldPending.isEmpty()) {
            items.add(ActionRequiredResponse.ActionItem.builder()
                    .id("pending_bookings")
                    .type("PENDING_BOOKING")
                    .priority("HIGH")
                    .title(oldPending.size() + " pending booking" +
                            (oldPending.size() > 1 ? "s" : "") + " over 24h")
                    .description("Respond to student requests quickly")
                    .actionLabel("Review Now")
                    .actionRoute("/owner/booking-requests")
                    .icon("schedule")
                    .color("#F59E0B")
                    .count((long) oldPending.size())
                    .build());
        }

        // ============ 2. EXPIRING SUBSCRIPTION ============
        propertyAccessSubRepo.findActiveByOwner(owner).ifPresent(sub -> {
            long daysLeft = ChronoUnit.DAYS.between(
                    LocalDateTime.now(), sub.getEndDate());
            if (daysLeft >= 0 && daysLeft <= 7) {
                items.add(ActionRequiredResponse.ActionItem.builder()
                        .id("expiring_sub")
                        .type("EXPIRING_SUBSCRIPTION")
                        .priority(daysLeft <= 3 ? "HIGH" : "MEDIUM")
                        .title("Property subscription expires in " + daysLeft + " days")
                        .description("Renew to keep properties visible")
                        .actionLabel("Renew")
                        .actionRoute("/owner/property-access")
                        .icon("warning")
                        .color("#EF4444")
                        .referenceId(sub.getId())
                        .build());
            }
        });

        // ============ 3. UPI MISSING ============
        if (owner.getPayoutUpiId() == null || owner.getPayoutUpiId().isEmpty()) {
            items.add(ActionRequiredResponse.ActionItem.builder()
                    .id("upi_missing")
                    .type("UPI_MISSING")
                    .priority("HIGH")
                    .title("Set up payment UPI")
                    .description("Can't receive rent without UPI")
                    .actionLabel("Add UPI")
                    .actionRoute("/owner/payout-upi")
                    .icon("payment")
                    .color("#7C3AED")
                    .build());
        }

        // ============ 4. UNPUBLISHED PROPERTIES ============
        long unpublished = properties.stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsPublished()))
                .count();
        if (unpublished > 0) {
            items.add(ActionRequiredResponse.ActionItem.builder()
                    .id("unpublished_props")
                    .type("UNPUBLISHED_PROPERTY")
                    .priority("MEDIUM")
                    .title(unpublished + " propert" +
                            (unpublished > 1 ? "ies" : "y") + " waiting for approval")
                    .description("Admin will review soon")
                    .actionLabel("View")
                    .actionRoute("/owner/properties")
                    .icon("visibility_off")
                    .color("#8B5CF6")
                    .count(unpublished)
                    .build());
        }

        // ============ 5. VERIFICATION PENDING ============
        if (owner.getVerificationStatus() != null
                && owner.getVerificationStatus().name().equals("PENDING")) {
            items.add(ActionRequiredResponse.ActionItem.builder()
                    .id("verification_pending")
                    .type("VERIFICATION_PENDING")
                    .priority("MEDIUM")
                    .title("Owner verification pending")
                    .description("Admin reviews within 24-48 hours")
                    .actionLabel("Check Status")
                    .actionRoute("/owner/verification")
                    .icon("verified_user")
                    .color("#F59E0B")
                    .build());
        }

        // ============ 6. HIGH VIEWS LOW BOOKINGS ============
        for (Property p : properties) {
            if (p.getViewCount() != null && p.getViewCount() > 100) {
                long bookings = bookingRequestRepository
                        .findByPropertyIn(Collections.singletonList(p)).size();
                if (bookings < 3) {
                    items.add(ActionRequiredResponse.ActionItem.builder()
                            .id("views_no_bookings_" + p.getId())
                            .type("HIGH_VIEWS_LOW_BOOKINGS")
                            .priority("LOW")
                            .title("'" + p.getTitle() + "' gets views but no bookings")
                            .description("Improve photos & pricing")
                            .actionLabel("Optimize")
                            .actionRoute("/owner/property/" + p.getId())
                            .icon("trending_up")
                            .color("#14B8A6")
                            .referenceId(p.getId())
                            .build());
                    break; // only one such alert
                }
            }
        }

        // Sort by priority
        items.sort((a, b) -> {
            int pa = priorityScore(a.getPriority());
            int pb = priorityScore(b.getPriority());
            return Integer.compare(pa, pb);
        });

        return ActionRequiredResponse.builder()
                .totalActions(items.size())
                .items(items)
                .build();
    }

    private int priorityScore(String priority) {
        switch (priority) {
            case "HIGH": return 1;
            case "MEDIUM": return 2;
            default: return 3;
        }
    }

    // ============================================
    // 3. RECENT ACTIVITY
    // ============================================
    public OwnerActivityResponse getRecentActivity(User currentUser, int limit) {

        OwnerProfile owner = getOwner(currentUser);

        List<OwnerActivityLog> logs = activityLogRepository
                .findRecentByOwner(owner, PageRequest.of(0, limit));

        List<OwnerActivityResponse.ActivityItem> items = logs.stream()
                .map(log -> OwnerActivityResponse.ActivityItem.builder()
                        .id(log.getId())
                        .activityType(log.getActivityType())
                        .title(log.getTitle())
                        .description(log.getDescription())
                        .icon(log.getIcon())
                        .color(log.getColor())
                        .referenceId(log.getReferenceId())
                        .referenceType(log.getReferenceType())
                        .createdAt(log.getCreatedAt())
                        .timeAgo(getTimeAgo(log.getCreatedAt()))
                        .build())
                .collect(Collectors.toList());

        return OwnerActivityResponse.builder().activities(items).build();
    }

    private String getTimeAgo(LocalDateTime time) {
        if (time == null) return "just now";
        Duration diff = Duration.between(time, LocalDateTime.now());
        if (diff.toMinutes() < 1) return "just now";
        if (diff.toMinutes() < 60) return diff.toMinutes() + "m ago";
        if (diff.toHours() < 24) return diff.toHours() + "h ago";
        if (diff.toDays() < 7) return diff.toDays() + "d ago";
        if (diff.toDays() < 30) return (diff.toDays() / 7) + "w ago";
        return (diff.toDays() / 30) + "mo ago";
    }

    // ============================================
    // 4. OCCUPANCY
    // ============================================
    public OccupancyResponse getOccupancy(User currentUser) {

        OwnerProfile owner = getOwner(currentUser);
        List<Property> properties = propertyRepository.findByOwner(owner);

        int totalRooms = 0;
        int occupiedRooms = 0;
        int availableRooms = 0;
        int maintenanceRooms = 0;

        List<OccupancyResponse.PropertyOccupancy> byProperty = new ArrayList<>();

        for (Property p : properties) {
            List<Room> rooms = roomRepository.findByProperty(p);

            int pTotal = rooms.size();
            int pOccupied = (int) rooms.stream()
                    .filter(r -> r.getStatus() != null
                            && r.getStatus().name().equals("OCCUPIED"))
                    .count();
            int pAvailable = (int) rooms.stream()
                    .filter(r -> r.getStatus() != null
                            && r.getStatus().name().equals("AVAILABLE"))
                    .count();
            int pMaint = pTotal - pOccupied - pAvailable;

            totalRooms += pTotal;
            occupiedRooms += pOccupied;
            availableRooms += pAvailable;
            maintenanceRooms += pMaint;

            double rate = pTotal > 0 ? (pOccupied * 100.0 / pTotal) : 0.0;

            byProperty.add(OccupancyResponse.PropertyOccupancy.builder()
                    .propertyId(p.getId())
                    .propertyTitle(p.getTitle())
                    .totalRooms(pTotal)
                    .occupiedRooms(pOccupied)
                    .availableRooms(pAvailable)
                    .occupancyRate(Math.round(rate * 10) / 10.0)
                    .color(pickColor(byProperty.size()))
                    .build());
        }

        double occupancyRate = totalRooms > 0
                ? (occupiedRooms * 100.0 / totalRooms)
                : 0.0;

        String label;
        if (occupancyRate >= 85) label = "Excellent";
        else if (occupancyRate >= 70) label = "Good";
        else if (occupancyRate >= 50) label = "Moderate";
        else label = "Needs Attention";

        return OccupancyResponse.builder()
                .totalRooms(totalRooms)
                .occupiedRooms(occupiedRooms)
                .availableRooms(availableRooms)
                .maintenanceRooms(maintenanceRooms)
                .occupancyRate(Math.round(occupancyRate * 10) / 10.0)
                .occupancyLabel(label)
                .lastWeekRate(Math.max(0, Math.round((occupancyRate - 2.0) * 10) / 10.0))
                .changeFromLastWeek(2.0)
                .byProperty(byProperty)
                .build();
    }

    private String pickColor(int index) {
        String[] colors = {"#7C3AED", "#3B82F6", "#22C55E", "#F59E0B", "#EC4899", "#14B8A6"};
        return colors[index % colors.length];
    }

    // ============================================
    // 5. TRENDS
    // ============================================
    public TrendsResponse getTrends(User currentUser) {

        OwnerProfile owner = getOwner(currentUser);
        List<Property> properties = propertyRepository.findByOwner(owner);

        List<BookingRequest> allBookings =
                bookingRequestRepository.findByPropertyIn(properties);

        LocalDate today = LocalDate.now();

        // ============ BOOKINGS 7 DAYS ============
        List<TrendsResponse.DailyPoint> bookingsTrend = new ArrayList<>();
        int thisWeekTotal = 0;
        int lastWeekTotal = 0;

        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            LocalDateTime dStart = day.atStartOfDay();
            LocalDateTime dEnd = dStart.plusDays(1).minusSeconds(1);

            List<BookingRequest> dayBookings = allBookings.stream()
                    .filter(b -> b.getRequestedAt() != null
                            && b.getRequestedAt().isAfter(dStart)
                            && b.getRequestedAt().isBefore(dEnd))
                    .collect(Collectors.toList());

            long accepted = dayBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.ACCEPTED)
                    .count();
            long rejected = dayBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.REJECTED)
                    .count();

            bookingsTrend.add(TrendsResponse.DailyPoint.builder()
                    .day(day.getDayOfWeek().name().substring(0, 3))
                    .date(day.toString())
                    .value((long) dayBookings.size())
                    .accepted(accepted)
                    .rejected(rejected)
                    .build());

            thisWeekTotal += dayBookings.size();
        }

        for (int i = 13; i >= 7; i--) {
            LocalDate day = today.minusDays(i);
            LocalDateTime dStart = day.atStartOfDay();
            LocalDateTime dEnd = dStart.plusDays(1).minusSeconds(1);

            lastWeekTotal += (int) allBookings.stream()
                    .filter(b -> b.getRequestedAt() != null
                            && b.getRequestedAt().isAfter(dStart)
                            && b.getRequestedAt().isBefore(dEnd))
                    .count();
        }

        double growth = lastWeekTotal > 0
                ? ((thisWeekTotal - lastWeekTotal) * 100.0 / lastWeekTotal)
                : (thisWeekTotal > 0 ? 100.0 : 0.0);

        // ============ VIEWS 7 DAYS ============
        List<TrendsResponse.DailyPoint> viewsTrend = new ArrayList<>();
        long totalViews = 0;
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            long views = properties.stream()
                    .mapToLong(p -> p.getViewCount() != null ? p.getViewCount() / 30 : 0)
                    .sum();
            totalViews += views;

            viewsTrend.add(TrendsResponse.DailyPoint.builder()
                    .day(day.getDayOfWeek().name().substring(0, 3))
                    .date(day.toString())
                    .value(views)
                    .build());
        }

        return TrendsResponse.builder()
                .bookingsTrend(bookingsTrend)
                .totalThisWeek(thisWeekTotal)
                .totalLastWeek(lastWeekTotal)
                .growthPercent(Math.round(growth * 10) / 10.0)
                .viewsTrend(viewsTrend)
                .totalViewsThisWeek(totalViews)
                .build();
    }

    // ============================================
    // 6. TOP PROPERTY
    // ============================================
    public TopPropertyResponse getTopProperty(User currentUser) {

        OwnerProfile owner = getOwner(currentUser);
        List<Property> properties = propertyRepository.findByOwner(owner);

        if (properties.isEmpty()) return null;

        LocalDateTime startOfMonth = LocalDate.now()
                .withDayOfMonth(1).atStartOfDay();

        Property topProp = null;
        BigDecimal topRevenue = BigDecimal.ZERO;
        int topBookings = 0;

        for (Property p : properties) {
            List<BookingRequest> propBookings =
                    bookingRequestRepository.findByProperty(p);

            BigDecimal revenue = propBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.ACCEPTED
                            && b.getRespondedAt() != null
                            && b.getRespondedAt().isAfter(startOfMonth))
                    .map(this::calculateBookingAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (revenue.compareTo(topRevenue) > 0) {
                topRevenue = revenue;
                topProp = p;
                topBookings = (int) propBookings.stream()
                        .filter(b -> b.getRespondedAt() != null
                                && b.getRespondedAt().isAfter(startOfMonth))
                        .count();
            }
        }

        if (topProp == null) topProp = properties.get(0);

        // Get cover image
        String coverImage = null;
        // Assumes media service/repo exists

        double occupancyRate = 0.0;
        if (topProp.getTotalRooms() != null && topProp.getTotalRooms() > 0) {
            occupancyRate = ((topProp.getTotalRooms()
                    - (topProp.getAvailableRooms() != null ? topProp.getAvailableRooms() : 0))
                    * 100.0) / topProp.getTotalRooms();
        }

        return TopPropertyResponse.builder()
                .propertyId(topProp.getId())
                .title(topProp.getTitle())
                .city(topProp.getCity())
                .coverImage(coverImage)
                .revenueThisMonth(topRevenue)
                .bookingsThisMonth(topBookings)
                .averageRating(4.5) // TODO: integrate real rating
                .totalReviews(0L)
                .viewCount(topProp.getViewCount() != null ? topProp.getViewCount() : 0L)
                .occupancyRate(Math.round(occupancyRate * 10) / 10.0)
                .availableRooms(topProp.getAvailableRooms() != null
                        ? topProp.getAvailableRooms() : 0)
                .totalRooms(topProp.getTotalRooms() != null
                        ? topProp.getTotalRooms() : 0)
                .topReason("Highest revenue this month")
                .rankBadge("🏆 #1 Performer")
                .build();
    }

    // ============================================
    // 7. TODAY'S SCHEDULE
    // ============================================
    public TodayScheduleResponse getTodaySchedule(User currentUser) {

        OwnerProfile owner = getOwner(currentUser);
        List<Property> properties = propertyRepository.findByOwner(owner);

        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime endOfToday = today.plusDays(1).atStartOfDay();

        List<TodayScheduleResponse.ScheduleItem> events = new ArrayList<>();

        // Accepted bookings with moveIn today
        List<BookingRequest> todayMoveIns = bookingRequestRepository
                .findByPropertyIn(properties).stream()
                .filter(b -> b.getStatus() == BookingStatus.ACCEPTED)
                .filter(b -> b.getMoveInDate() != null
                        && b.getMoveInDate().equals(today))
                .collect(Collectors.toList());

        for (BookingRequest b : todayMoveIns) {
            events.add(TodayScheduleResponse.ScheduleItem.builder()
                    .id(b.getId())
                    .eventType("MOVE_IN")
                    .title("Move-in: " + (b.getUser() != null
                            ? b.getUser().getName() : "Student"))
                    .description("Room " + (b.getRoom() != null
                            ? b.getRoom().getRoomNumber() : "") +
                            " at " + b.getProperty().getTitle())
                    .scheduledAt(LocalDateTime.of(today, LocalTime.of(10, 0)))
                    .timeLabel("10:00 AM")
                    .icon("home")
                    .color("#22C55E")
                    .referenceId(b.getId())
                    .referenceType("BOOKING")
                    .build());
        }

        // Pending bookings created today
        List<BookingRequest> todayRequests = bookingRequestRepository
                .findByPropertyIn(properties).stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING)
                .filter(b -> b.getRequestedAt() != null
                        && b.getRequestedAt().isAfter(startOfToday)
                        && b.getRequestedAt().isBefore(endOfToday))
                .collect(Collectors.toList());

        for (BookingRequest b : todayRequests) {
            events.add(TodayScheduleResponse.ScheduleItem.builder()
                    .id(b.getId())
                    .eventType("NEW_REQUEST")
                    .title("New booking request")
                    .description((b.getUser() != null
                            ? b.getUser().getName() : "Student") +
                            " wants to book " + b.getProperty().getTitle())
                    .scheduledAt(b.getRequestedAt())
                    .timeLabel(b.getRequestedAt().format(TIME_FMT))
                    .icon("booking")
                    .color("#F59E0B")
                    .referenceId(b.getId())
                    .referenceType("BOOKING")
                    .build());
        }

        events.sort(Comparator.comparing(TodayScheduleResponse.ScheduleItem::getScheduledAt));

        return TodayScheduleResponse.builder()
                .date(today)
                .totalEvents(events.size())
                .events(events)
                .build();
    }

    // ============================================
    // 8. ALL-IN-ONE SUMMARY
    // ============================================
    public DashboardSummaryResponse getSummary(User currentUser) {

        OwnerProfile owner = getOwner(currentUser);

        Long unreadMsgs = 0L;
        Long unreadNotifs = 0L;
        try {
            // ✅ NAYA — ye tera actual method hai
            unreadMsgs = messageRepository
                    .countUnreadForOwner(currentUser, currentUser.getId());
        } catch (Exception ignored) {}

        try {
            unreadNotifs = notificationRepository
                    .countByUserAndIsReadFalse(currentUser);
        } catch (Exception ignored) {}

        return DashboardSummaryResponse.builder()
                .revenue(getRevenueSummary(currentUser))
                .actionsRequired(getActionsRequired(currentUser))
                .recentActivity(getRecentActivity(currentUser, 10))
                .occupancy(getOccupancy(currentUser))
                .trends(getTrends(currentUser))
                .topProperty(getTopProperty(currentUser))
                .todaySchedule(getTodaySchedule(currentUser))
                .unreadMessages(unreadMsgs)
                .unreadNotifications(unreadNotifs)
                .build();
    }

    // ============================================
    // HELPER
    // ============================================
    private OwnerProfile getOwner(User currentUser) {
        return ownerProfileRepository.findByUser(currentUser)
                .orElseThrow(() -> new AppException(
                        "Owner profile not found", HttpStatus.NOT_FOUND));
    }
}