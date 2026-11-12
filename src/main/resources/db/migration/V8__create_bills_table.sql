-- ========================================================
-- V8: Criação da tabela de contas a pagar (bills)
-- Separa compromissos futuros (PENDING / PAID / OVERDUE calculado) das
-- transações já efetivadas e permite vínculo opcional com a transação gerada.
-- ========================================================

CREATE TABLE IF NOT EXISTS bills (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    title VARCHAR(80) NOT NULL,
    description VARCHAR(255) DEFAULT NULL,
    category VARCHAR(30) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    due_date DATE NOT NULL,
    recurrence VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    paid_at DATE DEFAULT NULL,
    paid_transaction_id BINARY(16) DEFAULT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_bills_user_status_due (user_id, status, due_date),
    KEY idx_bills_status_due (status, due_date),
    CONSTRAINT fk_bills_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_bills_paid_transaction FOREIGN KEY (paid_transaction_id) REFERENCES transactions (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
