-- 回滚：恢复误登记收款及其收入台账，并恢复首期应收已收金额。
SET NAMES utf8mb4;
SET @receipt_id = 238255379182518272;
SET @ledger_id = 238255379203489792;
SET @receivable_id = 238250400770162688;
SET @amount = 22650.00;

START TRANSACTION;

UPDATE db_finance_receivable
SET received_amount = @amount,
    status = 'Received'
WHERE receivable_id = @receivable_id;

INSERT INTO db_finance_receipt
SELECT * FROM bak_20261020_receipt_238255379182518272
WHERE receipt_id = @receipt_id
  AND NOT EXISTS (SELECT 1 FROM db_finance_receipt WHERE receipt_id = @receipt_id);

INSERT INTO db_finance_ledger
SELECT * FROM bak_20261020_ledger_238255379203489792
WHERE ledger_id = @ledger_id
  AND NOT EXISTS (SELECT 1 FROM db_finance_ledger WHERE ledger_id = @ledger_id);

COMMIT;
