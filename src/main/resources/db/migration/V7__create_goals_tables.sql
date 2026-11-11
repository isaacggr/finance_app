-- ========================================================
-- V7: Criação das tabelas de metas financeiras (goals e goal_contributions)
-- Armazena metas por usuário com flag is_primary (máx. 1 ativa por usuário via regra transacional)
-- e histórico auditável de aportes com exclusão em cascata (ON DELETE CASCADE).
-- ========================================================

CREATE TABLE IF NOT EXISTS goals (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    title VARCHAR(80) NOT NULL,
    description VARCHAR(255) DEFAULT NULL,
    target_amount DECIMAL(15, 2) NOT NULL,
    current_amount DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    deadline DATE DEFAULT NULL,
    is_primary BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_goals_user_primary (user_id, is_primary),
    CONSTRAINT fk_goals_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS goal_contributions (
    id BINARY(16) NOT NULL,
    goal_id BINARY(16) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    contribution_date DATE NOT NULL,
    note VARCHAR(255) DEFAULT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_goal_contributions_goal_date (goal_id, contribution_date),
    CONSTRAINT fk_goal_contributions_goal FOREIGN KEY (goal_id) REFERENCES goals (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
