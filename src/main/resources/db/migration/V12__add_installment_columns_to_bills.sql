-- ========================================================
-- V12: Adiciona colunas de parcelamento na tabela bills de forma idempotente
-- ========================================================

DROP PROCEDURE IF EXISTS upgrade_bills_installments;

DELIMITER $$
CREATE PROCEDURE upgrade_bills_installments()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bills' AND COLUMN_NAME = 'installment_group_id'
    ) THEN
        ALTER TABLE bills
            ADD COLUMN installment_group_id BINARY(16) DEFAULT NULL AFTER user_id,
            ADD COLUMN installment_number INT DEFAULT NULL AFTER installment_group_id,
            ADD COLUMN total_installments INT DEFAULT NULL AFTER installment_number,
            ADD INDEX idx_bills_installment_group (installment_group_id),
            ADD CONSTRAINT fk_bills_installment_group FOREIGN KEY (installment_group_id) REFERENCES installment_groups (id) ON DELETE CASCADE;
    END IF;
END $$
DELIMITER ;

CALL upgrade_bills_installments();

DROP PROCEDURE IF EXISTS upgrade_bills_installments;
