-- ========================================================
-- V4: Otimização de segurança na tabela verification_tokens
-- 1. Remove restrição UNIQUE global no token (evita colisões com 6 dígitos)
-- 2. Adiciona coluna attempts para proteção contra força bruta (OWASP)
-- 3. Cria índice composto de alta performance para busca por token
-- ========================================================

ALTER TABLE verification_tokens DROP INDEX uk_verification_tokens_token;

ALTER TABLE verification_tokens ADD COLUMN attempts INT NOT NULL DEFAULT 0;

CREATE INDEX idx_verification_tokens_token_type ON verification_tokens (token, type);

