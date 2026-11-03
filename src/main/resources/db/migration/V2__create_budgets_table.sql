-- ========================================================
-- V2: Criação da tabela de orçamentos mensais (budgets)
-- Permite ao usuário definir tetos de gastos por categoria
-- para um determinado mês/ano.
-- ========================================================

CREATE TABLE IF NOT EXISTS budgets (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    category VARCHAR(30) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    month INT NOT NULL,
    year INT NOT NULL,
    deleted BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_budgets_user_period (user_id, month, year),
    CONSTRAINT uk_budgets_user_cat_period UNIQUE (user_id, category, month, year),
    CONSTRAINT fk_budgets_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

