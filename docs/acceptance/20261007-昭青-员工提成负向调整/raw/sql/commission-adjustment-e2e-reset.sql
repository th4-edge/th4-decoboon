-- 仅清理昭青测试企业 2026-12 负向调整 E2E 草稿与测试调整。
START TRANSACTION;

CREATE TEMPORARY TABLE tmp_test_refund_ids AS
SELECT refund_id FROM db_finance_refund
WHERE com_id = 1790000000000000001
  AND reason IN ('客户退款联动E2E');

DELETE FROM db_payroll_commission_adjustment
WHERE com_id = 1790000000000000001
  AND (remark IN ('提成退款负向调整E2E', '提成手工超额负例E2E', '客户退款自动负向调整')
    OR refund_id IN (SELECT refund_id FROM tmp_test_refund_ids));

DELETE FROM db_finance_ledger
WHERE com_id = 1790000000000000001
  AND source = 'Refund'
  AND source_id IN (SELECT refund_id FROM tmp_test_refund_ids);

DELETE FROM db_finance_refund
WHERE com_id = 1790000000000000001
  AND refund_id IN (SELECT refund_id FROM tmp_test_refund_ids);

DELETE FROM db_payroll_item
WHERE com_id = 1790000000000000001
  AND sheet_id IN (
    SELECT sheet_id FROM db_payroll_sheet
    WHERE com_id = 1790000000000000001 AND period = '2026-12'
  );

DELETE FROM db_payroll_sheet
WHERE com_id = 1790000000000000001
  AND period = '2026-12';

COMMIT;
