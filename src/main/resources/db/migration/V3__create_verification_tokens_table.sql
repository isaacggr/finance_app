-- ========================================================
-- V3: Verificação de e-mail e tokens temporários
-- Adiciona a coluna email_verified na tabela users e cria
-- a tabela verification_tokens para ativação e reset de senha.
-- ========================================================

ALTER TABLE users ADD COLUMN email_verified BIT(1) NOT NULL DEFAULT b'1';

CREATE TABLE IF NOT EXISTS verification_tokens (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    token VARCHAR(100) NOT NULL,
    type VARCHAR(30) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_verification_tokens_token UNIQUE (token),
    KEY idx_verification_tokens_user_type (user_id, type),
    KEY idx_verification_tokens_expires (expires_at),
    CONSTRAINT fk_verification_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

