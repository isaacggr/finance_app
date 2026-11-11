-- ========================================================
-- V6: Hard Delete em budgets, alinhamento de transactions e retenção de 30 dias em users
-- 1. Remove registros soft-deleted residuais de budgets e dropa a coluna deleted (idempotente).
-- 2. Alinha transactions.category e transactions.type de ENUM legado (Hibernate pré-Flyway)
--    para VARCHAR livre (desbloqueando categorias dinâmicas customizadas).
-- 3. Configura ON DELETE CASCADE nas FKs de budgets e transactions consultando
--    information_schema pelo nome real da constraint (Hibernate ou Flyway).
-- 4. Adiciona deleted_at e índice de retenção na tabela users (idempotente).
-- ========================================================

-- 1. Budgets: limpar residuais e remover coluna deleted apenas se ainda existir
SET @budgets_has_deleted = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'budgets'
      AND COLUMN_NAME = 'deleted'
);

SET @sql = IF(
    @budgets_has_deleted > 0,
    'DELETE FROM budgets WHERE deleted = TRUE',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @budgets_has_deleted > 0,
    'ALTER TABLE budgets DROP COLUMN deleted',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. Budgets FK: garantir ON DELETE CASCADE independente do nome anterior da constraint
SET @budgets_fk_name = (
    SELECT kcu.CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE kcu
    WHERE kcu.TABLE_SCHEMA = DATABASE()
      AND kcu.TABLE_NAME = 'budgets'
      AND kcu.COLUMN_NAME = 'user_id'
      AND kcu.REFERENCED_TABLE_NAME = 'users'
    LIMIT 1
);

SET @budgets_fk_rule = (
    SELECT rc.DELETE_RULE
    FROM information_schema.REFERENTIAL_CONSTRAINTS rc
    WHERE rc.CONSTRAINT_SCHEMA = DATABASE()
      AND rc.TABLE_NAME = 'budgets'
      AND rc.CONSTRAINT_NAME = @budgets_fk_name
    LIMIT 1
);

SET @sql = IF(
    @budgets_fk_name IS NOT NULL
        AND (@budgets_fk_name <> 'fk_budgets_user' OR COALESCE(@budgets_fk_rule, '') <> 'CASCADE'),
    CONCAT('ALTER TABLE budgets DROP FOREIGN KEY `', @budgets_fk_name, '`'),
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @budgets_has_cascade_fk = (
    SELECT COUNT(*)
    FROM information_schema.REFERENTIAL_CONSTRAINTS rc
    WHERE rc.CONSTRAINT_SCHEMA = DATABASE()
      AND rc.TABLE_NAME = 'budgets'
      AND rc.CONSTRAINT_NAME = 'fk_budgets_user'
      AND rc.DELETE_RULE = 'CASCADE'
);

SET @sql = IF(
    @budgets_has_cascade_fk = 0,
    'ALTER TABLE budgets ADD CONSTRAINT fk_budgets_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. Transactions: alinhar category e type para VARCHAR livre (desbloqueia categorias dinâmicas)
SET @tx_category_is_legacy = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'transactions'
      AND COLUMN_NAME = 'category'
      AND (LOWER(DATA_TYPE) <> 'varchar' OR COALESCE(CHARACTER_MAXIMUM_LENGTH, 0) < 30)
);

SET @sql = IF(
    @tx_category_is_legacy > 0,
    'ALTER TABLE transactions MODIFY COLUMN category VARCHAR(30) NOT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @tx_type_is_legacy = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'transactions'
      AND COLUMN_NAME = 'type'
      AND (LOWER(DATA_TYPE) <> 'varchar' OR COALESCE(CHARACTER_MAXIMUM_LENGTH, 0) < 20)
);

SET @sql = IF(
    @tx_type_is_legacy > 0,
    'ALTER TABLE transactions MODIFY COLUMN type VARCHAR(20) NOT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. Transactions FK: garantir ON DELETE CASCADE independente do nome gerado pelo Hibernate ou Flyway
SET @tx_fk_name = (
    SELECT kcu.CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE kcu
    WHERE kcu.TABLE_SCHEMA = DATABASE()
      AND kcu.TABLE_NAME = 'transactions'
      AND kcu.COLUMN_NAME = 'user_id'
      AND kcu.REFERENCED_TABLE_NAME = 'users'
    LIMIT 1
);

SET @tx_fk_rule = (
    SELECT rc.DELETE_RULE
    FROM information_schema.REFERENTIAL_CONSTRAINTS rc
    WHERE rc.CONSTRAINT_SCHEMA = DATABASE()
      AND rc.TABLE_NAME = 'transactions'
      AND rc.CONSTRAINT_NAME = @tx_fk_name
    LIMIT 1
);

SET @sql = IF(
    @tx_fk_name IS NOT NULL
        AND (@tx_fk_name <> 'fk_transactions_user' OR COALESCE(@tx_fk_rule, '') <> 'CASCADE'),
    CONCAT('ALTER TABLE transactions DROP FOREIGN KEY `', @tx_fk_name, '`'),
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @tx_has_cascade_fk = (
    SELECT COUNT(*)
    FROM information_schema.REFERENTIAL_CONSTRAINTS rc
    WHERE rc.CONSTRAINT_SCHEMA = DATABASE()
      AND rc.TABLE_NAME = 'transactions'
      AND rc.CONSTRAINT_NAME = 'fk_transactions_user'
      AND rc.DELETE_RULE = 'CASCADE'
);

SET @sql = IF(
    @tx_has_cascade_fk = 0,
    'ALTER TABLE transactions ADD CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. Users: retenção de 30 dias (deleted_at + backfill + índice)
SET @users_has_deleted_at = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'deleted_at'
);

SET @sql = IF(
    @users_has_deleted_at = 0,
    'ALTER TABLE users ADD COLUMN deleted_at DATETIME(6) NULL AFTER deleted',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE users
   SET deleted_at = COALESCE(updated_at, created_at, NOW(6))
 WHERE deleted = b'1'
   AND deleted_at IS NULL;

SET @users_has_retention_idx = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND INDEX_NAME = 'idx_users_deleted_retention'
);

SET @sql = IF(
    @users_has_retention_idx = 0,
    'CREATE INDEX idx_users_deleted_retention ON users (deleted, deleted_at)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
