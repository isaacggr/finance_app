-- ========================================================
-- V1: Criação das tabelas iniciais (users e transactions)
-- Padrão Enterprise com chaves primárias UUID (BINARY(16)),
-- integridade referencial e índices compostos de performance.
-- ========================================================

CREATE TABLE IF NOT EXISTS users (
    id BINARY(16) NOT NULL,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(80) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    deleted BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS transactions (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    type VARCHAR(10) NOT NULL,
    category VARCHAR(30) NOT NULL,
    date DATE NOT NULL,
    description VARCHAR(255) DEFAULT NULL,
    deleted BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_transactions_user_date (user_id, date),
    KEY idx_transactions_user_deleted (user_id, deleted),
    CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

