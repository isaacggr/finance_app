package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.Bill;
import com.isaacggr.finance_app.domain.entity.User;
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

    public BillAlertService(
            UserRepository userRepository,
            BillRepository billRepository,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.billRepository = billRepository;
        this.emailService = emailService;
    }

    // ==========================================
    // DISPARO AGENDADO DIÁRIO (09:00 America/Sao_Paulo)
    // Notifica apenas contas pendentes que vencem em exatos 3 dias ou hoje (sem spam diário)
    // ==========================================
    @Scheduled(cron = "${app.reports.bill-alerts.cron:0 0 9 * * ?}", zone = "America/Sao_Paulo")
    @Transactional(readOnly = true)
    public void processDailyBillAlerts() {
        LocalDate today = LocalDate.now(DEFAULT_ZONE);
        LocalDate threeDaysAhead = today.plusDays(DEFAULT_DAYS_AHEAD);
        List<LocalDate> targetDueDates = List.of(today, threeDaysAhead);

        log.info("⏰ [CRON] Iniciando rotina de alertas de contas para {} e {} (America/Sao_Paulo)", today, threeDaysAhead);

        List<User> eligibleUsers = userRepository.findByEmailVerifiedTrueAndDeletedFalse();
        int totalAlertsSent = 0;

        for (User user : eligibleUsers) {
            try {
                List<Bill> billsToAlert = billRepository.findByUserIdAndStatusAndDueDateInOrderByDueDateAscCreatedAtDesc(
                        user.getId(),
                        BillStatus.PENDING,
                        targetDueDates
                );

                if (!billsToAlert.isEmpty()) {
                    emailService.sendBillAlertEmail(user.getEmail(), user.getName(), billsToAlert, DEFAULT_DAYS_AHEAD);
                    totalAlertsSent++;
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
    @Transactional(readOnly = true)
    public int sendAlertsForUser(User user, Integer daysAhead) {
        int effectiveDays = (daysAhead != null && daysAhead > 0 && daysAhead <= 30) ? daysAhead : DEFAULT_DAYS_AHEAD;

        LocalDate today = LocalDate.now(DEFAULT_ZONE);
        LocalDate endDate = today.plusDays(effectiveDays);

        List<Bill> openBills = billRepository.findOpenBillsUpTo(user.getId(), endDate);

        if (!openBills.isEmpty()) {
            emailService.sendBillAlertEmail(user.getEmail(), user.getName(), openBills, effectiveDays);
        }

        return openBills.size();
    }
}
