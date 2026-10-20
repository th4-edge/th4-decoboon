-- 122-收款冲正-rollback.sql
-- 仅在确认没有收款冲正事实时执行。
ALTER TABLE db_payroll_commission_release
    DROP FOREIGN KEY fk_payroll_release_reversed_by,
    DROP INDEX idx_payroll_release_receipt_reversed,
    DROP COLUMN reverse_reason,
    DROP COLUMN reversed_by_id,
    DROP COLUMN reversed_at,
    DROP COLUMN reversed;

DROP TABLE IF EXISTS db_finance_receipt_reversal;

ALTER TABLE db_finance_ledger
    MODIFY COLUMN source ENUM('Adjustment','Manual','Payment','Receipt','Refund') NOT NULL;

ALTER TABLE db_finance_receipt
    DROP FOREIGN KEY fk_fin_receipt_reversed_by,
    DROP INDEX idx_fin_receipt_company_status,
    DROP COLUMN reversal_reason,
    DROP COLUMN reversed_by_id,
    DROP COLUMN reversed_at,
    DROP COLUMN status;
