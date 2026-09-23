package com.nestora.nestora_app;

import com.nestora.nestora_app.entity.RentInvoice;
import com.nestora.nestora_app.enums.NotificationType;
import com.nestora.nestora_app.repository.RentInvoiceRepository;
import com.nestora.nestora_app.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RentReminderScheduler {

    private final RentInvoiceRepository invoiceRepository;
    private final NotificationService notificationService;

    // ============================================
    // Daily 8 AM — Overdue rent reminders
    // ============================================
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void remindOverdueRent() {
        try {
            List<RentInvoice> overdue = invoiceRepository.findOverdue(LocalDate.now());

            log.info("Found {} overdue invoices", overdue.size());

            for (RentInvoice inv : overdue) {
                try {
                    long days = ChronoUnit.DAYS.between(inv.getDueDate(), LocalDate.now());
                    inv.setDaysOverdue((int) days);
                    inv.setStatus("OVERDUE");
                    invoiceRepository.save(inv);

                    notificationService.createNotification(
                            inv.getAgreement().getUser(),
                            "⚠️ Rent Overdue",
                            "Your rent of ₹" + inv.getAmount() +
                                    " is " + days + " days overdue. Please pay immediately.",
                            NotificationType.PAYMENT,
                            inv.getId()
                    );
                } catch (Exception e) {
                    log.error("Failed to send reminder for invoice {}", inv.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("Overdue rent reminder failed", e);
        }
    }

    // ============================================
    // Daily 9 AM — Upcoming rent reminders (5 days before)
    // ============================================
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void remindUpcomingRent() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate fiveDaysLater = today.plusDays(5);

            List<RentInvoice> upcoming = invoiceRepository
                    .findPendingByDateRange(today, fiveDaysLater);

            log.info("Found {} upcoming invoices for reminder", upcoming.size());

            for (RentInvoice inv : upcoming) {
                try {
                    long days = ChronoUnit.DAYS.between(today, inv.getDueDate());

                    notificationService.createNotification(
                            inv.getAgreement().getUser(),
                            "💰 Rent Due in " + days + " Days",
                            "Your rent of ₹" + inv.getAmount() +
                                    " is due on " + inv.getDueDate() +
                                    ". Pay now to avoid late fees.",
                            NotificationType.PAYMENT,
                            inv.getId()
                    );
                } catch (Exception e) {
                    log.error("Failed to send reminder for invoice {}", inv.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("Upcoming rent reminder failed", e);
        }
    }
}