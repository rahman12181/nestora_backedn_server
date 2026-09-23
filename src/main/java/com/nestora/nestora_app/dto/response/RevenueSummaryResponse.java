package com.nestora.nestora_app.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class RevenueSummaryResponse {

    // ============ SUMMARY ============
    private BigDecimal thisMonthTotal;
    private BigDecimal lastMonthTotal;
    private Double growthPercent;

    // ============ COLLECTION ============
    private BigDecimal paidAmount;
    private BigDecimal pendingAmount;
    private Double collectionRate;   // percentage 0-100

    // ============ PAYOUTS ============
    private BigDecimal payoutsCompleted;
    private BigDecimal payoutsPending;

    // ============ YEAR TOTALS ============
    private BigDecimal thisYearTotal;
    private Long totalTransactions;

    // ============ MONTHLY TREND (last 6 months) ============
    private List<MonthlyRevenue> monthlyTrend;

    // ============ DAILY TREND (last 30 days) ============
    private List<DailyRevenue> dailyTrend;

    @Data
    @Builder
    public static class MonthlyRevenue {
        private String month;          // "Jan", "Feb"
        private Integer year;
        private BigDecimal amount;
        private Long transactions;
    }

    @Data
    @Builder
    public static class DailyRevenue {
        private String date;           // "2026-09-15"
        private BigDecimal amount;
    }
}