package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.Transaction;
import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.enums.TransactionType;
import com.isaacggr.finance_app.repository.TransactionRepository;
import com.isaacggr.finance_app.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class BillAlertService {

    private static final Logger log = LoggerFactory.getLogger(BillAlertService.class);
    public static final int DEFAULT_DAYS_AHEAD = 3;

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final EmailService emailService;

    public BillAlertService(
            UserRepository userRepository,
            TransactionRepository transactionRepository,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.emailService = emailService;
    }

    // ==========================================
    // DISPARO AGENDADO DIÁRIO (09:00 AM)
    // ==========================================
    @Scheduled(cron = "${app.reports.bill-alerts.cron:0 0 9 * * ?}")
    @Transactional(readOnly = true)
    public void processDailyBillAlerts() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(DEFAULT_DAYS_AHEAD);

        log.info("⏰ [CRON] Iniciando rotina diária de alertas de contas a vencer ({} a {})", startDate, endDate);

        List<User> eligibleUsers = userRepository.findByEmailVerifiedTrueAndDeletedFalse();
        int totalAlertsSent = 0;

        for (User user : eligibleUsers) {
            try {
                List<Transaction> upcomingBills = transactionRepository.findUpcomingTransactions(
                        user.getId(),
                        TransactionType.EXPENSE,
                        startDate,
                        endDate
                );

                if (!upcomingBills.isEmpty()) {
                    emailService.sendBillAlertEmail(user.getEmail(), user.getName(), upcomingBills, DEFAULT_DAYS_AHEAD);
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

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(effectiveDays);

        List<Transaction> upcomingBills = transactionRepository.findUpcomingTransactions(
                user.getId(),
                TransactionType.EXPENSE,
                startDate,
                endDate
        );

        if (!upcomingBills.isEmpty()) {
            emailService.sendBillAlertEmail(user.getEmail(), user.getName(), upcomingBills, effectiveDays);
        }

        return upcomingBills.size();
    }
}

