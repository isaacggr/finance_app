package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.Bill;
import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.entity.UserNotificationPreference;
import com.isaacggr.finance_app.dto.dashboard.DashboardSummaryDTO;
import com.isaacggr.finance_app.exception.BusinessException;
import com.isaacggr.finance_app.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class MonthlyReportService {

    private static final Logger log = LoggerFactory.getLogger(MonthlyReportService.class);

    private final UserRepository userRepository;
    private final DashboardService dashboardService;
    private final EmailService emailService;
    private final NotificationPreferenceService preferenceService;
    private final NotificationService notificationService;

    public MonthlyReportService(
            UserRepository userRepository,
            DashboardService dashboardService,
            EmailService emailService,
            NotificationPreferenceService preferenceService,
            NotificationService notificationService
    ) {
        this.userRepository = userRepository;
        this.dashboardService = dashboardService;
        this.emailService = emailService;
        this.preferenceService = preferenceService;
        this.notificationService = notificationService;
    }

    // ==========================================
    // DISPARO AGENDADO AUTOMATIZADO
    // Executa no dia 1º de cada mês às 08:00 America/Sao_Paulo
    // ==========================================
    @Scheduled(cron = "${app.reports.monthly.cron:0 0 8 1 * ?}", zone = "America/Sao_Paulo")
    @Transactional
    public void processMonthlyReports() {
        YearMonth previousMonth = YearMonth.now(Bill.DEFAULT_ZONE).minusMonths(1);
        LocalDate startDate = previousMonth.atDay(1);
        LocalDate endDate = previousMonth.atEndOfMonth();
        String periodName = String.format("%02d/%d", previousMonth.getMonthValue(), previousMonth.getYear());

        log.info("🚀 [CRON] Iniciando rotina de relatórios mensais para o período: {}", periodName);

        List<User> eligibleUsers = userRepository.findByEmailVerifiedTrueAndDeletedFalse();
        int totalSent = 0;

        for (User user : eligibleUsers) {
            try {
                UserNotificationPreference prefs = preferenceService.getEntityOrDefault(user);
                if (!prefs.isMonthlyReportEmailEnabled() && !prefs.isMonthlyReportInAppEnabled()) {
                    continue;
                }

                DashboardSummaryDTO summary = dashboardService.getSummary(user.getId(), startDate, endDate);
                if (prefs.isMonthlyReportEmailEnabled()) {
                    emailService.sendMonthlyReportEmail(user.getEmail(), user.getName(), summary, periodName);
                    totalSent++;
                }
                if (prefs.isMonthlyReportInAppEnabled()) {
                    notificationService.notifyMonthlyReportReady(user, periodName, summary.balance());
                }
            } catch (Exception ex) {
                log.error("Erro ao gerar relatório mensal para o usuário {}: {}", user.getEmail(), ex.getMessage());
            }
        }

        log.info("🏁 [CRON] Concluído envio de relatórios mensais: {} e-mails despachados.", totalSent);
    }

    // ==========================================
    // DISPARO SOB DEMANDA (Para testes / usuário autenticado)
    // ==========================================
    @Transactional
    public void sendReportForUser(User user, Integer month, Integer year) {
        YearMonth targetMonth;

        if (month != null) {
            if (month < 1 || month > 12) {
                throw new BusinessException("O mês informado deve estar entre 1 e 12");
            }
            int effectiveYear = (year != null) ? year : LocalDate.now(Bill.DEFAULT_ZONE).getYear();
            targetMonth = YearMonth.of(effectiveYear, month);
        } else {
            // Se nenhum período for passado, assume o mês anterior fechado
            targetMonth = YearMonth.now(Bill.DEFAULT_ZONE).minusMonths(1);
        }

        LocalDate startDate = targetMonth.atDay(1);
        LocalDate endDate = targetMonth.atEndOfMonth();
        String periodName = String.format("%02d/%d", targetMonth.getMonthValue(), targetMonth.getYear());

        DashboardSummaryDTO summary = dashboardService.getSummary(user.getId(), startDate, endDate);
        emailService.sendMonthlyReportEmail(user.getEmail(), user.getName(), summary, periodName);
        notificationService.notifyMonthlyReportReady(user, periodName, summary.balance());
    }
}

