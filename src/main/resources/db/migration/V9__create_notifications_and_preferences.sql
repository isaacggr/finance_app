-- ========================================================
-- V9: Criação das tabelas de preferências de notificação e central in-app
-- Permite configuração granular de alertas por canal (e-mail e sino do app)
-- e histórico idempotente de notificações via dedup_key por usuário.
-- ========================================================

CREATE TABLE IF NOT EXISTS user_notification_preferences (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    bill_alerts_email_enabled BIT(1) NOT NULL DEFAULT b'1',
    bill_alerts_in_app_enabled BIT(1) NOT NULL DEFAULT b'1',
    bill_alert_days_before INT NOT NULL DEFAULT 3,
    monthly_report_email_enabled BIT(1) NOT NULL DEFAULT b'1',
    monthly_report_in_app_enabled BIT(1) NOT NULL DEFAULT b'1',
    budget_alerts_in_app_enabled BIT(1) NOT NULL DEFAULT b'1',
    goal_alerts_in_app_enabled BIT(1) NOT NULL DEFAULT b'1',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_notif_prefs_user UNIQUE (user_id),
    CONSTRAINT fk_user_notif_prefs_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Garante que contas existentes já possuam preferências padrão persistidas
INSERT INTO user_notification_preferences (
    id,
    user_id,
    bill_alerts_email_enabled,
    bill_alerts_in_app_enabled,
    bill_alert_days_before,
    monthly_report_email_enabled,
    monthly_report_in_app_enabled,
    budget_alerts_in_app_enabled,
    goal_alerts_in_app_enabled,
    created_at,
    updated_at
)
SELECT
    UUID_TO_BIN(UUID()),
    u.id,
    b'1',
    b'1',
    3,
    b'1',
    b'1',
    b'1',
    b'1',
    NOW(6),
    NOW(6)
FROM users u
WHERE NOT EXISTS (
    SELECT 1 FROM user_notification_preferences p WHERE p.user_id = u.id
);

CREATE TABLE IF NOT EXISTS notifications (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    type VARCHAR(40) NOT NULL,
    title VARCHAR(120) NOT NULL,
    message VARCHAR(500) NOT NULL,
    reference_type VARCHAR(30) DEFAULT NULL,
    reference_id BINARY(16) DEFAULT NULL,
    dedup_key VARCHAR(120) DEFAULT NULL,
    is_read BIT(1) NOT NULL DEFAULT b'0',
    read_at DATETIME(6) DEFAULT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_notifications_user_read_created (user_id, is_read, created_at),
    CONSTRAINT uk_notifications_user_dedup UNIQUE (user_id, dedup_key),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
