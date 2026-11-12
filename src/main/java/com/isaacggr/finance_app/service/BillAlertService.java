package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.Bill;
import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.entity.UserNotificationPreference;
import com.isaacggr.finance_app.domain.enums.BillStatus;
import com.isaacggr.finance_app.repository.BillRepository;
import com.isaacggr.finance_app.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class BillAlertService {

    private static final Logger log = LoggerFactory.getLogger(BillAlertService.class);
    public static final int DEFAULT_DAYS_AHEAD = 3;
    public static final ZoneId DEFAULT_ZONE = Bill.DEFAULT_ZONE;

    private final UserRepository userRepository;
    private final BillRepository billRepository;
    private final EmailService emailService;
    private final NotificationPreferenceService preferenceService;
    private final NotificationService notificationService;

    public BillAlertService(
            UserRepository userRepository,
            BillRepository billRepository,
            EmailService emailService,
            NotificationPreferenceService preferenceService,
            NotificationService notificationService
    ) {
        this.userRepository = userRepository;
        this.billRepository = billRepository;
        this.emailService = emailService;
        this.preferenceService = preferenceService;
        this.notificationService = notificationService;
    }

    // ==========================================
    // DISPARO AGENDADO DIÁRIO (09:00 America/Sao_Paulo)
    // Respeita preferências do usuário e alerta apenas no dia do vencimento ou N dias antes (padrão 3).
    // ==========================================
    @Scheduled(cron = "${app.reports.bill-alerts.cron:0 0 9 * * ?}", zone = "America/Sao_Paulo")
    @Transactional
    public void processDailyBillAlerts() {
        LocalDate today = LocalDate.now(DEFAULT_ZONE);

        log.info("⏰ [CRON] Iniciando rotina de alertas de contas em {} (America/Sao_Paulo)", today);

        List<User> eligibleUsers = userRepository.findByEmailVerifiedTrueAndDeletedFalse();
        int totalAlertsSent = 0;

        for (User user : eligibleUsers) {
            try {
                UserNotificationPreference prefs = preferenceService.getEntityOrDefault(user);
                if (!prefs.isBillAlertsEmailEnabled() && !prefs.isBillAlertsInAppEnabled()) {
                    continue;
                }

                int daysBefore = prefs.getBillAlertDaysBefore();
                LocalDate targetAhead = today.plusDays(daysBefore);
                List<LocalDate> targetDueDates = List.of(today, targetAhead);

                List<Bill> billsToAlert = billRepository.findByUserIdAndStatusAndDueDateInOrderByDueDateAscCreatedAtDesc(
                        user.getId(),
                        BillStatus.PENDING,
                        targetDueDates
                );

                if (!billsToAlert.isEmpty()) {
                    if (prefs.isBillAlertsEmailEnabled()) {
                        emailService.sendBillAlertEmail(user.getEmail(), user.getName(), billsToAlert, daysBefore);
                        totalAlertsSent++;
                    }
                    if (prefs.isBillAlertsInAppEnabled()) {
                        for (Bill bill : billsToAlert) {
                            notificationService.notifyBillDueAlert(user, bill, bill.getDueInDaysOn(today));
                        }
                    }
                }

                if (prefs.isBillAlertsInAppEnabled()) {
                    List<Bill> overdueBills = billRepository.findOverdueByUserId(user.getId(), today);
                    for (Bill overdueBill : overdueBills) {
                        notificationService.notifyBillDueAlert(user, overdueBill, overdueBill.getDueInDaysOn(today));
                    }
                }
            } catch (Exception ex) {
                log.error("Erro ao processar alertas de contas para o usuário {}: {}", user.getEmail(), ex.getMessage());
            }
        }

        log.info("🏁 [CRON] Rotina de alertas de contas concluída: {} e-mails disparados.", totalAlertsSent);
    }

    // ==========================================
    // DISPARO SOB DEMANDA (Usuário autenticado)
    // ==========================================
    @Transactional
    public int sendAlertsForUser(User user, Integer daysAhead) {
        UserNotificationPreference prefs = preferenceService.getEntityOrDefault(user);
        int preferredDays = prefs.getBillAlertDaysBefore();
        int effectiveDays = (daysAhead != null && daysAhead > 0 && daysAhead <= 30) ? daysAhead : preferredDays;

        LocalDate today = LocalDate.now(DEFAULT_ZONE);
        LocalDate endDate = today.plusDays(effectiveDays);

        List<Bill> openBills = billRepository.findOpenBillsUpTo(user.getId(), endDate);

        if (!openBills.isEmpty()) {
            emailService.sendBillAlertEmail(user.getEmail(), user.getName(), openBills, effectiveDays);
            if (prefs.isBillAlertsInAppEnabled()) {
                for (Bill bill : openBills) {
                    notificationService.notifyBillDueAlert(user, bill, bill.getDueInDaysOn(today));
                }
            }
        }

        return openBills.size();
    }
}
