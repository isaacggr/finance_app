package com.isaacggr.finance_app.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_notification_preferences", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_notif_prefs_user", columnNames = "user_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserNotificationPreference {

    public static final int DEFAULT_BILL_ALERT_DAYS_BEFORE = 3;
    public static final int MIN_BILL_ALERT_DAYS_BEFORE = 1;
    public static final int MAX_BILL_ALERT_DAYS_BEFORE = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "bill_alerts_email_enabled", nullable = false)
    private boolean billAlertsEmailEnabled = true;

    @Column(name = "bill_alerts_in_app_enabled", nullable = false)
    private boolean billAlertsInAppEnabled = true;

    @Column(name = "bill_alert_days_before", nullable = false)
    private int billAlertDaysBefore = DEFAULT_BILL_ALERT_DAYS_BEFORE;

    @Column(name = "monthly_report_email_enabled", nullable = false)
    private boolean monthlyReportEmailEnabled = true;

    @Column(name = "monthly_report_in_app_enabled", nullable = false)
    private boolean monthlyReportInAppEnabled = true;

    @Column(name = "budget_alerts_in_app_enabled", nullable = false)
    private boolean budgetAlertsInAppEnabled = true;

    @Column(name = "goal_alerts_in_app_enabled", nullable = false)
    private boolean goalAlertsInAppEnabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public UserNotificationPreference(
            User user,
            boolean billAlertsEmailEnabled,
            boolean billAlertsInAppEnabled,
            int billAlertDaysBefore,
            boolean monthlyReportEmailEnabled,
            boolean monthlyReportInAppEnabled,
            boolean budgetAlertsInAppEnabled,
            boolean goalAlertsInAppEnabled
    ) {
        this.user = user;
        this.billAlertsEmailEnabled = billAlertsEmailEnabled;
        this.billAlertsInAppEnabled = billAlertsInAppEnabled;
        this.billAlertDaysBefore = normalizeDaysBefore(billAlertDaysBefore);
        this.monthlyReportEmailEnabled = monthlyReportEmailEnabled;
        this.monthlyReportInAppEnabled = monthlyReportInAppEnabled;
        this.budgetAlertsInAppEnabled = budgetAlertsInAppEnabled;
        this.goalAlertsInAppEnabled = goalAlertsInAppEnabled;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static UserNotificationPreference defaultFor(User user) {
        return new UserNotificationPreference(
                user,
                true,
                true,
                DEFAULT_BILL_ALERT_DAYS_BEFORE,
                true,
                true,
                true,
                true
        );
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void update(
            boolean billAlertsEmailEnabled,
            boolean billAlertsInAppEnabled,
            int billAlertDaysBefore,
            boolean monthlyReportEmailEnabled,
            boolean monthlyReportInAppEnabled,
            boolean budgetAlertsInAppEnabled,
            boolean goalAlertsInAppEnabled
    ) {
        this.billAlertsEmailEnabled = billAlertsEmailEnabled;
        this.billAlertsInAppEnabled = billAlertsInAppEnabled;
        this.billAlertDaysBefore = normalizeDaysBefore(billAlertDaysBefore);
        this.monthlyReportEmailEnabled = monthlyReportEmailEnabled;
        this.monthlyReportInAppEnabled = monthlyReportInAppEnabled;
        this.budgetAlertsInAppEnabled = budgetAlertsInAppEnabled;
        this.goalAlertsInAppEnabled = goalAlertsInAppEnabled;
    }

    public static int normalizeDaysBefore(Integer daysBefore) {
        if (daysBefore == null
                || daysBefore < MIN_BILL_ALERT_DAYS_BEFORE
                || daysBefore > MAX_BILL_ALERT_DAYS_BEFORE) {
            return DEFAULT_BILL_ALERT_DAYS_BEFORE;
        }
        return daysBefore;
    }
}
