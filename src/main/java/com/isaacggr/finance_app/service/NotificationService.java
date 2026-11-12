package com.isaacggr.finance_app.service;

import com.isaacggr.finance_app.domain.entity.Bill;
import com.isaacggr.finance_app.domain.entity.Budget;
import com.isaacggr.finance_app.domain.entity.Goal;
import com.isaacggr.finance_app.domain.entity.Notification;
import com.isaacggr.finance_app.domain.entity.User;
import com.isaacggr.finance_app.domain.entity.UserNotificationPreference;
import com.isaacggr.finance_app.domain.enums.NotificationType;
import com.isaacggr.finance_app.dto.NotificationReadAllResponseDTO;
import com.isaacggr.finance_app.dto.NotificationResponseDTO;
import com.isaacggr.finance_app.dto.PageResponseDTO;
import com.isaacggr.finance_app.dto.UnreadNotificationsCountDTO;
import com.isaacggr.finance_app.exception.NotFoundException;
import com.isaacggr.finance_app.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class NotificationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceService preferenceService;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationPreferenceService preferenceService
    ) {
        this.notificationRepository = notificationRepository;
        this.preferenceService = preferenceService;
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<NotificationResponseDTO> listNotifications(
            UUID userId,
            Boolean unreadOnly,
            Pageable pageable
    ) {
        Pageable effectivePageable = withStableSort(pageable);
        Page<Notification> page = Boolean.TRUE.equals(unreadOnly)
                ? notificationRepository.findByUserIdAndReadFalse(userId, effectivePageable)
                : notificationRepository.findByUserId(userId, effectivePageable);

        return PageResponseDTO.from(page.map(NotificationResponseDTO::fromEntity));
    }

    @Transactional(readOnly = true)
    public UnreadNotificationsCountDTO countUnread(UUID userId) {
        return new UnreadNotificationsCountDTO(notificationRepository.countByUserIdAndReadFalse(userId));
    }

    public NotificationResponseDTO markAsRead(UUID notificationId, UUID userId) {
        Notification notification = findOwnedNotification(notificationId, userId);
        if (!notification.isRead()) {
            notification.markAsRead();
            notification = notificationRepository.save(notification);
        }
        return NotificationResponseDTO.fromEntity(notification);
    }

    public NotificationReadAllResponseDTO markAllAsRead(UUID userId) {
        int updatedCount = notificationRepository.markAllAsReadByUserId(userId, LocalDateTime.now());
        return new NotificationReadAllResponseDTO(updatedCount);
    }

    public void delete(UUID notificationId, UUID userId) {
        Notification notification = findOwnedNotification(notificationId, userId);
        notificationRepository.delete(notification);
    }

    public void notifyBillDueAlert(User user, Bill bill, long dueInDays) {
        if (user == null || user.getId() == null || bill == null || bill.getId() == null) {
            return;
        }
        UserNotificationPreference prefs = preferenceService.getEntityOrDefault(user);
        if (!prefs.isBillAlertsInAppEnabled()) {
            return;
        }

        NotificationType type;
        String title;
        String message;
        String dedupPrefix;
        String formattedDueDate = bill.getDueDate().format(DATE_FORMATTER);
        String formattedAmount = formatCurrency(bill.getAmount());

        if (dueInDays < 0) {
            long overdueDays = Math.abs(dueInDays);
            type = NotificationType.BILL_OVERDUE;
            title = "Conta atrasada: " + bill.getTitle();
            message = "A conta %s (%s) venceu em %s e está atrasada há %d %s."
                    .formatted(bill.getTitle(), formattedAmount, formattedDueDate, overdueDays,
                            overdueDays == 1 ? "dia" : "dias");
            dedupPrefix = "BILL_OVERDUE";
        } else if (dueInDays == 0) {
            type = NotificationType.BILL_DUE_TODAY;
            title = "Conta vence hoje: " + bill.getTitle();
            message = "Sua conta %s no valor de %s vence hoje (%s)."
                    .formatted(bill.getTitle(), formattedAmount, formattedDueDate);
            dedupPrefix = "BILL_DUE_TODAY";
        } else {
            type = NotificationType.BILL_DUE_SOON;
            title = "Conta a vencer em %d %s: %s"
                    .formatted(dueInDays, dueInDays == 1 ? "dia" : "dias", bill.getTitle());
            message = "Sua conta %s no valor de %s vence em %s."
                    .formatted(bill.getTitle(), formattedAmount, formattedDueDate);
            dedupPrefix = "BILL_DUE_SOON";
        }

        String dedupKey = "%s:%s:%s".formatted(dedupPrefix, bill.getDueDate(), bill.getId());
        createIfNotExists(user, type, title, message, "BILL", bill.getId(), dedupKey);
    }

    public void notifyMonthlyReportReady(User user, String periodName, BigDecimal balance) {
        if (user == null || user.getId() == null || periodName == null || periodName.isBlank()) {
            return;
        }
        UserNotificationPreference prefs = preferenceService.getEntityOrDefault(user);
        if (!prefs.isMonthlyReportInAppEnabled()) {
            return;
        }

        String title = "Relatório mensal " + periodName + " disponível";
        String message = "Seu fechamento financeiro de %s foi consolidado com saldo líquido de %s."
                .formatted(periodName, formatCurrency(balance));
        String dedupKey = "MONTHLY_REPORT:" + periodName;

        createIfNotExists(user, NotificationType.MONTHLY_REPORT, title, message, "REPORT", null, dedupKey);
    }

    public void notifyBudgetThreshold(
            User user,
            Budget budget,
            BigDecimal spentAmount,
            BigDecimal percentageUsed,
            boolean exceeded
    ) {
        if (user == null || user.getId() == null || budget == null || budget.getId() == null) {
            return;
        }
        UserNotificationPreference prefs = preferenceService.getEntityOrDefault(user);
        if (!prefs.isBudgetAlertsInAppEnabled()) {
            return;
        }

        String periodLabel = "%02d/%d".formatted(budget.getMonth(), budget.getYear());
        NotificationType type = exceeded ? NotificationType.BUDGET_EXCEEDED : NotificationType.BUDGET_WARNING;
        String title = exceeded
                ? "Orçamento ultrapassado: " + budget.getCategory()
                : "Orçamento em alerta: " + budget.getCategory();
        String message = exceeded
                ? "Você atingiu %s%% do orçamento de %s (%s) — gasto de %s para teto de %s."
                        .formatted(percentageUsed, budget.getCategory(), periodLabel,
                                formatCurrency(spentAmount), formatCurrency(budget.getAmount()))
                : "Você já consumiu %s%% do orçamento de %s (%s): %s de %s."
                        .formatted(percentageUsed, budget.getCategory(), periodLabel,
                                formatCurrency(spentAmount), formatCurrency(budget.getAmount()));

        String dedupKey = "%s:%d-%02d:%s".formatted(type.name(), budget.getYear(), budget.getMonth(), budget.getId());
        createIfNotExists(user, type, title, message, "BUDGET", budget.getId(), dedupKey);
    }

    public void notifyGoalCompleted(User user, Goal goal) {
        if (user == null || user.getId() == null || goal == null || goal.getId() == null) {
            return;
        }
        UserNotificationPreference prefs = preferenceService.getEntityOrDefault(user);
        if (!prefs.isGoalAlertsInAppEnabled()) {
            return;
        }

        String title = "Meta concluída: " + goal.getTitle();
        String message = "Parabéns! Você atingiu o objetivo de %s na meta \"%s\"."
                .formatted(formatCurrency(goal.getTargetAmount()), goal.getTitle());
        String dedupKey = "GOAL_COMPLETED:%s:%s".formatted(goal.getId(), goal.getTargetAmount());

        createIfNotExists(user, NotificationType.GOAL_COMPLETED, title, message, "GOAL", goal.getId(), dedupKey);
    }

    private void createIfNotExists(
            User user,
            NotificationType type,
            String title,
            String message,
            String referenceType,
            UUID referenceId,
            String dedupKey
    ) {
        if (dedupKey != null && notificationRepository.existsByUserIdAndDedupKey(user.getId(), dedupKey)) {
            return;
        }

        Notification notification = new Notification(
                user,
                type,
                title,
                message,
                referenceType,
                referenceId,
                dedupKey
        );
        notificationRepository.save(notification);
    }

    private Notification findOwnedNotification(UUID notificationId, UUID userId) {
        return notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotFoundException("Notificação não encontrada"));
    }

    private Pageable withStableSort(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        }
        if (pageable.isUnpaged()) {
            return pageable;
        }

        List<Sort.Order> orders = new ArrayList<>();
        Sort currentSort = pageable.getSort();
        if (currentSort.isUnsorted()) {
            orders.add(Sort.Order.desc("createdAt"));
        } else {
            currentSort.forEach(orders::add);
        }

        if (currentSort.getOrderFor("id") == null) {
            orders.add(Sort.Order.desc("id"));
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(orders));
    }

    private String formatCurrency(BigDecimal amount) {
        BigDecimal safeAmount = (amount != null) ? amount : BigDecimal.ZERO;
        return NumberFormat.getCurrencyInstance(PT_BR).format(safeAmount);
    }
}
