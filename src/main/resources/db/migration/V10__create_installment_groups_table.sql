-- ========================================================
-- V10: Criação da tabela de compras parceladas (installment_groups)
-- e adição dos campos de parcelamento nas tabelas bills e transactions.
-- Suporta agrupamento de parcelas, integridade referencial com users (ON DELETE CASCADE)
-- e exclusão em cascata das contas/transações vinculadas ao grupo.
-- ========================================================

CREATE TABLE IF NOT EXISTS installment_groups (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    description VARCHAR(255) NOT NULL,
    total_amount DECIMAL(15, 2) NOT NULL,
    total_installments INT NOT NULL,
    category VARCHAR(30) NOT NULL,
    start_date DATE NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_installment_groups_user (user_id),
    CONSTRAINT fk_installment_groups_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Adiciona campos de parcelamento na tabela bills (idempotente)
-- --------------------------------------------------------
SET @bills_has_installment_group_id = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'bills'
      AND COLUMN_NAME = 'installment_group_id'
);

SET @sql = IF(
    @bills_has_installment_group_id = 0,
    'ALTER TABLE bills ADD COLUMN installment_group_id BINARY(16) DEFAULT NULL AFTER user_id',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @bills_has_installment_number = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'bills'
      AND COLUMN_NAME = 'installment_number'
);

SET @sql = IF(
    @bills_has_installment_number = 0,
    'ALTER TABLE bills ADD COLUMN installment_number INT DEFAULT NULL AFTER installment_group_id',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @bills_has_total_installments = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'bills'
      AND COLUMN_NAME = 'total_installments'
);

SET @sql = IF(
    @bills_has_total_installments = 0,
    'ALTER TABLE bills ADD COLUMN total_installments INT DEFAULT NULL AFTER installment_number',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Garante índice para consultas de parcelas por grupo em bills
SET @bills_has_installment_idx = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'bills'
      AND INDEX_NAME = 'idx_bills_installment_group'
);

SET @sql = IF(
    @bills_has_installment_idx = 0,
    'CREATE INDEX idx_bills_installment_group ON bills (installment_group_id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Garante foreign key entre bills e installment_groups com ON DELETE CASCADE
SET @bills_has_installment_fk = (
    SELECT COUNT(*)
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'bills'
      AND CONSTRAINT_NAME = 'fk_bills_installment_group'
);

SET @sql = IF(
    @bills_has_installment_fk = 0,
    'ALTER TABLE bills ADD CONSTRAINT fk_bills_installment_group FOREIGN KEY (installment_group_id) REFERENCES installment_groups (id) ON DELETE CASCADE',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- --------------------------------------------------------
-- Adiciona campos de parcelamento na tabela transactions (idempotente)
-- --------------------------------------------------------
SET @tx_has_installment_group_id = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'transactions'
      AND COLUMN_NAME = 'installment_group_id'
);

SET @sql = IF(
    @tx_has_installment_group_id = 0,
    'ALTER TABLE transactions ADD COLUMN installment_group_id BINARY(16) DEFAULT NULL AFTER user_id',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @tx_has_installment_number = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'transactions'
      AND COLUMN_NAME = 'installment_number'
);

SET @sql = IF(
    @tx_has_installment_number = 0,
    'ALTER TABLE transactions ADD COLUMN installment_number INT DEFAULT NULL AFTER installment_group_id',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @tx_has_total_installments = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'transactions'
      AND COLUMN_NAME = 'total_installments'
);

SET @sql = IF(
    @tx_has_total_installments = 0,
    'ALTER TABLE transactions ADD COLUMN total_installments INT DEFAULT NULL AFTER installment_number',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Garante índice para consultas de parcelas por grupo em transactions
SET @tx_has_installment_idx = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'transactions'
      AND INDEX_NAME = 'idx_transactions_installment_group'
);

SET @sql = IF(
    @tx_has_installment_idx = 0,
    'CREATE INDEX idx_transactions_installment_group ON transactions (installment_group_id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Garante foreign key entre transactions e installment_groups com ON DELETE CASCADE
SET @tx_has_installment_fk = (
    SELECT COUNT(*)
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'transactions'
      AND CONSTRAINT_NAME = 'fk_transactions_installment_group'
);

SET @sql = IF(
    @tx_has_installment_fk = 0,
    'ALTER TABLE transactions ADD CONSTRAINT fk_transactions_installment_group FOREIGN KEY (installment_group_id) REFERENCES installment_groups (id) ON DELETE CASCADE',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
