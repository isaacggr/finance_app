-- ========================================================
-- V13: Criação de índices compostos de alta performance
-- Acelera o cálculo dos relatórios mensais automáticos por e-mail,
-- consultas agregadas do dashboard financeiro e verificação de orçamentos por categoria.
-- ========================================================

-- Acelera agregação de receitas/despesas por período no relatório mensal e dashboard
CREATE INDEX idx_transactions_user_type_date ON transactions (user_id, type, date);

-- Acelera cálculo de consumo de orçamento por categoria e período
CREATE INDEX idx_transactions_user_type_cat_date ON transactions (user_id, type, category, date);
