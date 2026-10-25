-- 129 企业费用 D2 回滚
-- 前置：停止费用付款入口，保留应付、付款、核销和流水历史事实。

ALTER TABLE db_finance_expense
    DROP FOREIGN KEY fk_fin_expense_payable;

DROP INDEX idx_fin_expense_company_payable ON db_finance_expense;

ALTER TABLE db_finance_expense
    DROP COLUMN payable_id;
