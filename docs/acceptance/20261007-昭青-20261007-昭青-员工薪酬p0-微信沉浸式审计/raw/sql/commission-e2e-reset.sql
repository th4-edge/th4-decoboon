-- 仅清理昭青测试企业的提成 P0 本地测试事实，不触碰工资、其他项目或生产数据。
-- 使用前先备份；执行后应收节点恢复到测试前状态。
START TRANSACTION;

CREATE TEMPORARY TABLE tmp_commission_ids AS
SELECT commission_id
FROM db_payroll_commission_item
WHERE com_id = 1790000000000000001
  AND remark = '提成P0本地测试';

CREATE TEMPORARY TABLE tmp_payable_ids AS
SELECT payable_id
FROM db_finance_payable
WHERE com_id = 1790000000000000001
  AND source_type = 'CommissionItem'
  AND source_id IN (SELECT commission_id FROM tmp_commission_ids);

CREATE TEMPORARY TABLE tmp_payment_ids AS
SELECT payment_id
FROM db_finance_payment
WHERE com_id = 1790000000000000001
  AND payable_id IN (SELECT payable_id FROM tmp_payable_ids);

CREATE TEMPORARY TABLE tmp_receipt_ids AS
SELECT receipt_id
FROM db_finance_receipt
WHERE com_id = 1790000000000000001
  AND project_id = 232968907537776640
  AND (idempotency_key LIKE 'comm-p0-receipt-%'
    OR idempotency_key LIKE 'comm-receipt-%');

DELETE FROM db_finance_ledger_revision
WHERE ledger_id IN (
  SELECT ledger_id FROM db_finance_ledger
  WHERE com_id = 1790000000000000001
    AND ((source = 'Payment' AND source_id IN (SELECT payment_id FROM tmp_payment_ids))
      OR (source = 'Receipt' AND source_id IN (SELECT receipt_id FROM tmp_receipt_ids)))
);

DELETE FROM db_finance_ledger
WHERE com_id = 1790000000000000001
  AND ((source = 'Payment' AND source_id IN (SELECT payment_id FROM tmp_payment_ids))
    OR (source = 'Receipt' AND source_id IN (SELECT receipt_id FROM tmp_receipt_ids)));

DELETE FROM db_finance_payable_settlement
WHERE com_id = 1790000000000000001
  AND payment_id IN (SELECT payment_id FROM tmp_payment_ids);

DELETE FROM db_finance_payment
WHERE com_id = 1790000000000000001
  AND payment_id IN (SELECT payment_id FROM tmp_payment_ids);

DELETE FROM db_payroll_commission_release
WHERE com_id = 1790000000000000001
  AND commission_id IN (SELECT commission_id FROM tmp_commission_ids);

DELETE FROM db_finance_payable
WHERE com_id = 1790000000000000001
  AND payable_id IN (SELECT payable_id FROM tmp_payable_ids);

DELETE FROM db_payroll_commission_item
WHERE com_id = 1790000000000000001
  AND commission_id IN (SELECT commission_id FROM tmp_commission_ids);

DELETE FROM db_finance_receipt
WHERE com_id = 1790000000000000001
  AND receipt_id IN (SELECT receipt_id FROM tmp_receipt_ids);

UPDATE db_finance_receivable
SET received_amount = 0.00,
    status = 'Pending',
    update_date_time = UNIX_TIMESTAMP() * 1000
WHERE com_id = 1790000000000000001
  AND receivable_id IN (232968908057870336, 232968908057870337, 232968908062064640);

COMMIT;
