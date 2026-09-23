package com.nestora.nestora_app.service;

import com.nestora.nestora_app.entity.*;
import com.nestora.nestora_app.enums.RoomStatus;
import com.nestora.nestora_app.exception.AppException;
import com.nestora.nestora_app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RentalAgreementService {

    private final RentalAgreementRepository agreementRepository;
    private final RentInvoiceRepository invoiceRepository;
    private final RentPaymentRepository paymentRepository;
    private final SettlementRepository settlementRepository;
    private final BookingRequestRepository bookingRepository;
    private final UserActivityLogRepository activityLogRepository;
    private final RoomRepository roomRepository;                    // ✅ NEW
    private final NotificationService notificationService;

    private static final DateTimeFormatter MONTH_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM");

    // ============================================
    // CREATE AGREEMENT (Called after booking payment success)
    // ✅ ENHANCED — Mark room occupied
    // ============================================
    @Transactional
    public RentalAgreement createAgreement(BookingRequest booking) {

        // Check existing
        if (agreementRepository.findActiveByUser(booking.getUser()).isPresent()) {
            log.info("Agreement already exists for user {}", booking.getUser().getId());
            return agreementRepository.findActiveByUser(booking.getUser()).get();
        }

        // Validate booking has room
        if (booking.getRoom() == null) {
            throw new AppException("Booking must have a room assigned", HttpStatus.BAD_REQUEST);
        }

        String code = "AGR-" + UUID.randomUUID()
                .toString().substring(0, 8).toUpperCase();

        RentalAgreement agreement = RentalAgreement.builder()
                .agreementCode(code)
                .bookingRequest(booking)
                .user(booking.getUser())
                .owner(booking.getProperty().getOwner())
                .property(booking.getProperty())
                .room(booking.getRoom())
                .monthlyRent(booking.getRoom().getMonthlyRent() != null
                        ? booking.getRoom().getMonthlyRent()
                        : booking.getProperty().getMonthlyRentMin())
                .securityDeposit(booking.getProperty().getSecurityDeposit() != null
                        ? booking.getProperty().getSecurityDeposit()
                        : BigDecimal.ZERO)
                .advancePaid(BigDecimal.ZERO)
                .startDate(booking.getMoveInDate() != null
                        ? booking.getMoveInDate()
                        : LocalDate.now())
                .rentDueDay(5)
                .status("ACTIVE")
                .build();

        RentalAgreement saved = agreementRepository.save(agreement);

        // ============================================
        // ✅ NEW — Mark room as OCCUPIED for this user
        // ============================================
        try {
            Room room = booking.getRoom();
            room.setStatus(RoomStatus.OCCUPIED);
            room.setCurrentUserId(booking.getUser().getId());
            room.setCurrentAgreementId(saved.getId());
            room.setOccupiedSince(LocalDateTime.now());

            if (room.getOccupiedCount() == null) {
                room.setOccupiedCount(1);
            } else {
                room.setOccupiedCount(room.getOccupiedCount() + 1);
            }

            roomRepository.save(room);

            log.info("Room {} assigned to user {} (agreement: {})",
                    room.getRoomNumber(), booking.getUser().getId(), code);
        } catch (Exception e) {
            log.error("Failed to mark room occupied: {}", e.getMessage());
        }

        // Generate invoices for next 12 months
        generateInvoices(saved, 12);

        // Log activity
        logActivity(saved.getUser(), "AGREEMENT_CREATED",
                "Rental Agreement Created",
                "You can now pay rent for " + saved.getProperty().getTitle(),
                "contract", "#7C3AED", saved.getId(), "AGREEMENT");

        log.info("Agreement {} created successfully for booking {}",
                code, booking.getId());

        return saved;
    }

    // ============================================
    // GENERATE MONTHLY INVOICES
    // ============================================
    @Transactional
    public void generateInvoices(RentalAgreement agreement, int months) {
        LocalDate start = agreement.getStartDate();

        for (int i = 0; i < months; i++) {
            LocalDate monthDate = start.plusMonths(i);
            String month = monthDate.format(MONTH_FMT);

            LocalDate dueDate = monthDate.withDayOfMonth(
                    Math.min(agreement.getRentDueDay(), monthDate.lengthOfMonth()));

            String invoiceCode = "INV-" + agreement.getAgreementCode()
                    + "-" + month;

            RentInvoice invoice = RentInvoice.builder()
                    .invoiceCode(invoiceCode)
                    .agreement(agreement)
                    .invoiceMonth(month)
                    .dueDate(dueDate)
                    .amount(agreement.getMonthlyRent())
                    .lateFee(BigDecimal.ZERO)
                    .status("PENDING")
                    .daysOverdue(0)
                    .build();

            invoiceRepository.save(invoice);
        }

        log.info("Generated {} invoices for agreement {}", months, agreement.getAgreementCode());
    }

    // ============================================
    // GET USER AGREEMENTS
    // ============================================
    public List<RentalAgreement> getUserAgreements(User user) {
        return agreementRepository.findByUserOrderByStartDateDesc(user);
    }

    public RentalAgreement getAgreementDetail(User user, Long id) {
        RentalAgreement ag = agreementRepository.findById(id)
                .orElseThrow(() -> new AppException(
                        "Agreement not found", HttpStatus.NOT_FOUND));

        if (!ag.getUser().getId().equals(user.getId())) {
            throw new AppException("Not authorized", HttpStatus.FORBIDDEN);
        }
        return ag;
    }

    // ============================================
    // GET AGREEMENT INVOICES
    // ============================================
    public List<RentInvoice> getUserInvoices(User user, Long agreementId) {
        RentalAgreement ag = getAgreementDetail(user, agreementId);
        return invoiceRepository.findByAgreementOrderByDueDateAsc(ag);
    }

    // ============================================
    // ✅ ENHANCED — TERMINATE AGREEMENT + Release room
    // ============================================
    @Transactional
    public RentalAgreement terminateAgreement(User user, Long id, String reason) {
        RentalAgreement ag = getAgreementDetail(user, id);

        if (!"ACTIVE".equals(ag.getStatus())) {
            throw new AppException("Only active agreements can be terminated",
                    HttpStatus.BAD_REQUEST);
        }

        ag.setStatus("TERMINATED");
        ag.setTerminationReason(reason);
        ag.setTerminatedAt(LocalDateTime.now());
        ag.setEndDate(LocalDate.now());

        agreementRepository.save(ag);

        // ============================================
        // ✅ NEW — Release room (mark available)
        // ============================================
        try {
            Room room = ag.getRoom();
            if (room != null) {
                room.setStatus(RoomStatus.AVAILABLE);
                room.setCurrentUserId(null);
                room.setCurrentAgreementId(null);
                room.setOccupiedSince(null);

                if (room.getOccupiedCount() != null && room.getOccupiedCount() > 0) {
                    room.setOccupiedCount(room.getOccupiedCount() - 1);
                }

                roomRepository.save(room);

                log.info("Room {} released from user {}",
                        room.getRoomNumber(), user.getId());
            }
        } catch (Exception e) {
            log.error("Failed to release room: {}", e.getMessage());
        }

        // Log activity
        logActivity(user, "AGREEMENT_TERMINATED",
                "Rental Agreement Ended",
                "Agreement " + ag.getAgreementCode() + " terminated: " + reason,
                "cancel", "#EF4444", ag.getId(), "AGREEMENT");

        return ag;
    }

    // ============================================
    // HELPER — LOG ACTIVITY
    // ============================================
    public void logActivity(User user, String type, String title,
                            String desc, String icon, String color,
                            Long refId, String refType) {
        try {
            UserActivityLog logEntry = UserActivityLog.builder()
                    .user(user)
                    .activityType(type)
                    .title(title)
                    .description(desc)
                    .icon(icon)
                    .color(color)
                    .referenceId(refId)
                    .referenceType(refType)
                    .build();
            activityLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Failed to log activity: {}", e.getMessage());
        }
    }
}