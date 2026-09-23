package com.nestora.nestora_app.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class UserDashboardSummaryResponse {

    private UserStats stats;
    private PaymentOverview paymentOverview;
    private List<BookingSummary> recentBookings;
    private List<ActivityItem> recentActivity;
    private CurrentTenancy currentTenancy;
    private List<UpcomingPayment> upcomingPayments;

    @Data
    @Builder
    public static class UserStats {
        private Long totalBookings;
        private Long activeBookings;
        private Long completedBookings;
        private Long cancelledBookings;
        private Long savedProperties;
        private BigDecimal walletBalance;
        private Long unreadMessages;
        private Long unreadNotifications;
    }

    @Data
    @Builder
    public static class PaymentOverview {
        private BigDecimal totalPaid;
        private BigDecimal totalPending;
        private BigDecimal thisMonthPaid;
        private BigDecimal lastMonthPaid;
        private Double monthlyGrowthPercent;
        private Integer totalTransactions;
        private List<MonthlySpend> monthlyTrend;  // 6 months
    }

    @Data
    @Builder
    public static class MonthlySpend {
        private String month;
        private Integer year;
        private BigDecimal amount;
        private Integer transactions;
    }

    @Data
    @Builder
    public static class BookingSummary {
        private Long bookingId;
        private String propertyTitle;
        private String propertyCity;
        private String coverImage;
        private String roomNumber;
        private BigDecimal monthlyRent;
        private String status;
        private String moveInDate;
        private String requestedAt;
    }

    @Data
    @Builder
    public static class ActivityItem {
        private Long id;
        private String activityType;
        private String title;
        private String description;
        private String icon;
        private String color;
        private Long referenceId;
        private String referenceType;
        private String createdAt;
        private String timeAgo;
    }

    @Data
    @Builder
    public static class CurrentTenancy {
        private Long agreementId;
        private String agreementCode;
        private String propertyTitle;
        private String propertyCity;
        private String coverImage;
        private String roomNumber;
        private BigDecimal monthlyRent;
        private BigDecimal securityDeposit;
        private String startDate;
        private String nextDueDate;
        private BigDecimal nextRentAmount;
        private String status;
        private Integer daysUntilDue;
        private Boolean rentDue;
    }

    @Data
    @Builder
    public static class UpcomingPayment {
        private Long invoiceId;
        private String invoiceCode;
        private String invoiceMonth;
        private BigDecimal amount;
        private String dueDate;
        private Integer daysUntilDue;
        private String status;
        private Boolean overdue;
    }
}