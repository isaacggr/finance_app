-- ========================================================
-- V5: Remoção da exclusão lógica na tabela transactions (Hard Delete definitivo)
-- Remove o índice composto com deleted e a coluna deleted.
-- ========================================================

ALTER TABLE transactions DROP INDEX idx_transactions_user_deleted;

ALTER TABLE transactions DROP COLUMN deleted;

