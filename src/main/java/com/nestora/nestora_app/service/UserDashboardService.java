package com.nestora.nestora_app.service;

import com.nestora.nestora_app.dto.response.UserDashboardSummaryResponse;
import com.nestora.nestora_app.entity.*;
import com.nestora.nestora_app.enums.BookingStatus;
import com.nestora.nestora_app.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDashboardService {

    private final UserRepository userRepository;
    private final BookingRequestRepository bookingRequestRepository;
    private final PropertyRepository propertyRepository;
    private final RentalAgreementRepository rentalAgreementRepository;
    private final RentInvoiceRepository rentInvoiceRepository;

    // ✅ FIX: Use MonthlyRentPaymentRepository (not RentPaymentRepository)
    private final MonthlyRentPaymentRepository monthlyRentPaymentRepository;

    private final SavedPropertyRepository savedPropertyRepository;
    private final UserActivityLogRepository userActivityLogRepository;
    private final MessageRepository messageRepository;
    private final NotificationRepository notificationRepository;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMM");

    public UserDashboardSummaryResponse getSummary(User currentUser) {
        return UserDashboardSummaryResponse.builder()
                .stats(getStats(currentUser))
                .paymentOverview(getPaymentOverview(currentUser))
                .recentBookings(getRecentBookings(currentUser))
                .recentActivity(getRecentActivity(currentUser, 10))
                .currentTenancy(getCurrentTenancy(currentUser))
                .upcomingPayments(getUpcomingPayments(currentUser))
                .build();
    }

    // ============================================
    // STATS
    // ============================================
    private UserDashboardSummaryResponse.UserStats getStats(User user) {
        List<BookingRequest> bookings = bookingRequestRepository.findByUser(user);

        long total = bookings.size();
        long active = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.ACCEPTED)
                .count();
        long completed = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .count();
        long cancelled = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CANCELLED)
                .count();

        Long savedCount = (long) savedPropertyRepository.findByUser(user).size();

        Long unreadMsgs = 0L;
        Long unreadNotifs = 0L;
        try {
            unreadMsgs = messageRepository.countUnreadForUser(user, user.getId());
        } catch (Exception ignored) {}
        try {
            unreadNotifs = notificationRepository.countByUserAndIsReadFalse(user);
        } catch (Exception ignored) {}

        return UserDashboardSummaryResponse.UserStats.builder()
                .totalBookings(total)
                .activeBookings(active)
                .completedBookings(completed)
                .cancelledBookings(cancelled)
                .savedProperties(savedCount)
                .walletBalance(user.getWalletBalance() != null
                        ? user.getWalletBalance() : BigDecimal.ZERO)
                .unreadMessages(unreadMsgs)
                .unreadNotifications(unreadNotifs)
                .build();
    }

    // ============================================
    // PAYMENT OVERVIEW — ✅ FIXED
    // ============================================
    private UserDashboardSummaryResponse.PaymentOverview getPaymentOverview(User user) {

        // ✅ FIX: Use MonthlyRentPaymentRepository
        List<MonthlyRentPayment> payments = monthlyRentPaymentRepository
                .findByStudentOrderByCreatedAtDesc(user);

        BigDecimal totalPaid = payments.stream()
                .filter(p -> "PAID".equals(p.getStatus()))
                .map(MonthlyRentPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<RentInvoice> pending = rentInvoiceRepository.findPendingByUser(user);
        BigDecimal totalPending = pending.stream()
                .map(RentInvoice::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate today = LocalDate.now();
        LocalDateTime startOfThisMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfLastMonth = startOfThisMonth.minusMonths(1);
        LocalDateTime endOfLastMonth = startOfThisMonth.minusSeconds(1);

        BigDecimal thisMonthPaid = payments.stream()
                .filter(p -> "PAID".equals(p.getStatus())
                        && p.getPaidAt() != null
                        && p.getPaidAt().isAfter(startOfThisMonth))
                .map(MonthlyRentPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal lastMonthPaid = payments.stream()
                .filter(p -> "PAID".equals(p.getStatus())
                        && p.getPaidAt() != null
                        && p.getPaidAt().isAfter(startOfLastMonth)
                        && p.getPaidAt().isBefore(endOfLastMonth))
                .map(MonthlyRentPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Double growth = lastMonthPaid.compareTo(BigDecimal.ZERO) > 0
                ? thisMonthPaid.subtract(lastMonthPaid)
                .divide(lastMonthPaid, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue()
                : (thisMonthPaid.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0);

        // Monthly trend (6 months)
        List<UserDashboardSummaryResponse.MonthlySpend> trend = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate month = today.minusMonths(i);
            LocalDateTime mStart = month.withDayOfMonth(1).atStartOfDay();
            LocalDateTime mEnd = mStart.plusMonths(1).minusSeconds(1);

            BigDecimal amt = payments.stream()
                    .filter(p -> "PAID".equals(p.getStatus())
                            && p.getPaidAt() != null
                            && p.getPaidAt().isAfter(mStart)
                            && p.getPaidAt().isBefore(mEnd))
                    .map(MonthlyRentPayment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long count = payments.stream()
                    .filter(p -> "PAID".equals(p.getStatus())
                            && p.getPaidAt() != null
                            && p.getPaidAt().isAfter(mStart)
                            && p.getPaidAt().isBefore(mEnd))
                    .count();

            trend.add(UserDashboardSummaryResponse.MonthlySpend.builder()
                    .month(month.format(MONTH_FMT))
                    .year(month.getYear())
                    .amount(amt)
                    .transactions((int) count)
                    .build());
        }

        return UserDashboardSummaryResponse.PaymentOverview.builder()
                .totalPaid(totalPaid)
                .totalPending(totalPending)
                .thisMonthPaid(thisMonthPaid)
                .lastMonthPaid(lastMonthPaid)
                .monthlyGrowthPercent(Math.round(growth * 10) / 10.0)
                .totalTransactions((int) payments.stream()
                        .filter(p -> "PAID".equals(p.getStatus())).count())
                .monthlyTrend(trend)
                .build();
    }

    // ============================================
    // RECENT BOOKINGS
    // ============================================
    private List<UserDashboardSummaryResponse.BookingSummary> getRecentBookings(User user) {
        List<BookingRequest> bookings = bookingRequestRepository.findByUser(user);

        return bookings.stream()
                .sorted(Comparator.comparing(BookingRequest::getRequestedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(3)
                .map(b -> UserDashboardSummaryResponse.BookingSummary.builder()
                        .bookingId(b.getId())
                        .propertyTitle(b.getProperty() != null
                                ? b.getProperty().getTitle() : null)
                        .propertyCity(b.getProperty() != null
                                ? b.getProperty().getCity() : null)
                        .roomNumber(b.getRoom() != null
                                ? b.getRoom().getRoomNumber() : null)
                        .monthlyRent(b.getRoom() != null
                                ? b.getRoom().getMonthlyRent() : null)
                        .status(b.getStatus() != null
                                ? b.getStatus().name() : "PENDING")
                        .moveInDate(b.getMoveInDate() != null
                                ? b.getMoveInDate().toString() : null)
                        .requestedAt(b.getRequestedAt() != null
                                ? b.getRequestedAt().toString() : null)
                        .build())
                .collect(Collectors.toList());
    }

    // ============================================
    // RECENT ACTIVITY
    // ============================================
    private List<UserDashboardSummaryResponse.ActivityItem> getRecentActivity(
            User user, int limit) {
        List<UserActivityLog> logs = userActivityLogRepository
                .findRecentByUser(user, PageRequest.of(0, limit));

        return logs.stream()
                .map(log -> UserDashboardSummaryResponse.ActivityItem.builder()
                        .id(log.getId())
                        .activityType(log.getActivityType())
                        .title(log.getTitle())
                        .description(log.getDescription())
                        .icon(log.getIcon())
                        .color(log.getColor())
                        .referenceId(log.getReferenceId())
                        .referenceType(log.getReferenceType())
                        .createdAt(log.getCreatedAt() != null
                                ? log.getCreatedAt().toString() : null)
                        .timeAgo(timeAgo(log.getCreatedAt()))
                        .build())
                .collect(Collectors.toList());
    }

    // ============================================
    // CURRENT TENANCY
    // ============================================
    private UserDashboardSummaryResponse.CurrentTenancy getCurrentTenancy(User user) {
        Optional<RentalAgreement> opt = rentalAgreementRepository
                .findActiveByUser(user);

        if (opt.isEmpty()) return null;

        RentalAgreement ag = opt.get();

        Optional<RentInvoice> nextInvoice = rentInvoiceRepository
                .findNextPendingByAgreement(ag);

        String nextDueDate = null;
        BigDecimal nextRentAmount = null;
        Integer daysUntilDue = null;
        Boolean rentDue = false;

        if (nextInvoice.isPresent()) {
            RentInvoice inv = nextInvoice.get();
            nextDueDate = inv.getDueDate() != null ? inv.getDueDate().toString() : null;
            nextRentAmount = inv.getAmount();
            if (inv.getDueDate() != null) {
                long days = Duration.between(
                        LocalDate.now().atStartOfDay(),
                        inv.getDueDate().atStartOfDay()).toDays();
                daysUntilDue = (int) days;
                rentDue = days <= 5;
            }
        }

        return UserDashboardSummaryResponse.CurrentTenancy.builder()
                .agreementId(ag.getId())
                .agreementCode(ag.getAgreementCode())
                .propertyTitle(ag.getProperty() != null
                        ? ag.getProperty().getTitle() : null)
                .propertyCity(ag.getProperty() != null
                        ? ag.getProperty().getCity() : null)
                .roomNumber(ag.getRoom() != null
                        ? ag.getRoom().getRoomNumber() : null)
                .monthlyRent(ag.getMonthlyRent())
                .securityDeposit(ag.getSecurityDeposit())
                .startDate(ag.getStartDate() != null
                        ? ag.getStartDate().toString() : null)
                .nextDueDate(nextDueDate)
                .nextRentAmount(nextRentAmount)
                .status(ag.getStatus())
                .daysUntilDue(daysUntilDue)
                .rentDue(rentDue)
                .build();
    }

    // ============================================
    // UPCOMING PAYMENTS
    // ============================================
    private List<UserDashboardSummaryResponse.UpcomingPayment> getUpcomingPayments(
            User user) {
        List<RentInvoice> upcoming = rentInvoiceRepository
                .findUpcomingByUser(user,
                        LocalDate.now(),
                        LocalDate.now().plusDays(30));

        return upcoming.stream()
                .map(inv -> {
                    long days = Duration.between(
                            LocalDate.now().atStartOfDay(),
                            inv.getDueDate().atStartOfDay()).toDays();
                    return UserDashboardSummaryResponse.UpcomingPayment.builder()
                            .invoiceId(inv.getId())
                            .invoiceCode(inv.getInvoiceCode())
                            .invoiceMonth(inv.getInvoiceMonth())
                            .amount(inv.getAmount())
                            .dueDate(inv.getDueDate().toString())
                            .daysUntilDue((int) days)
                            .status(inv.getStatus())
                            .overdue(days < 0)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private String timeAgo(LocalDateTime time) {
        if (time == null) return "just now";
        Duration d = Duration.between(time, LocalDateTime.now());
        if (d.toMinutes() < 1) return "just now";
        if (d.toMinutes() < 60) return d.toMinutes() + "m ago";
        if (d.toHours() < 24) return d.toHours() + "h ago";
        if (d.toDays() < 7) return d.toDays() + "d ago";
        if (d.toDays() < 30) return (d.toDays() / 7) + "w ago";
        return (d.toDays() / 30) + "mo ago";
    }
}