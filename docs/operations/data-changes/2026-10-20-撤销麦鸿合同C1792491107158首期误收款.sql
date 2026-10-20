-- 生产数据修正：撤销误登记的首期收款 22,650.00
-- 合同：C1792491107158 / contract_id=238242788695605248
-- 项目：P1792492921137 / project_id=238250397133701120
-- 收款：receipt_id=238255379182518272
-- 台账：ledger_id=238255379203489792
-- 应收：receivable_id=238250400770162688
-- 前置核对：无提成释放、无凭证、无退款、无承接项目锁定。

SET NAMES utf8mb4;
SET @receipt_id = 238255379182518272;
SET @ledger_id = 238255379203489792;
SET @receivable_id = 238250400770162688;
SET @project_id = 238250397133701120;
SET @amount = 22650.00;

CREATE TABLE IF NOT EXISTS bak_20261020_receipt_238255379182518272 AS
SELECT * FROM db_finance_receipt WHERE receipt_id = @receipt_id;
CREATE TABLE IF NOT EXISTS bak_20261020_ledger_238255379203489792 AS
SELECT * FROM db_finance_ledger WHERE ledger_id = @ledger_id;
CREATE TABLE IF NOT EXISTS bak_20261020_receivable_238250400770162688 AS
SELECT * FROM db_finance_receivable WHERE receivable_id = @receivable_id;

START TRANSACTION;

DELETE FROM db_finance_ledger
WHERE ledger_id = @ledger_id
  AND source = 'Receipt'
  AND source_id = @receipt_id
  AND amount = @amount;

DELETE FROM db_finance_receipt
WHERE receipt_id = @receipt_id
  AND project_id = @project_id
  AND receivable_id = @receivable_id
  AND amount = @amount;

UPDATE db_finance_receivable
SET received_amount = 0.00,
    status = 'Pending'
WHERE receivable_id = @receivable_id
  AND project_id = @project_id
  AND amount = @amount
  AND received_amount = @amount;

COMMIT;

SELECT 'receipt_remaining', COUNT(*) FROM db_finance_receipt WHERE receipt_id = @receipt_id;
SELECT 'ledger_remaining', COUNT(*) FROM db_finance_ledger WHERE ledger_id = @ledger_id;
SELECT 'receivable', receivable_id, name, amount, received_amount, status FROM db_finance_receivable WHERE receivable_id = @receivable_id;
SELECT 'project_receipts', COUNT(*), COALESCE(SUM(amount), 0.00) FROM db_finance_receipt WHERE project_id = @project_id;
SELECT 'project_receivable', COUNT(*), SUM(amount), SUM(received_amount) FROM db_finance_receivable WHERE project_id = @project_id;
SELECT 'project_ledger_income', COUNT(*), COALESCE(SUM(amount), 0.00) FROM db_finance_ledger WHERE project_id = @project_id AND direction = 'Income' AND source = 'Receipt';
