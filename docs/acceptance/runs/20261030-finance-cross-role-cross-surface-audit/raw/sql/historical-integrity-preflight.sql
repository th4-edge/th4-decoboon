-- 只读预检：执行前后对象与金额必须不变
SET @com_id = 999200000000000001;

SELECT 'duplicate_receipt_ledger' AS check_name,
       source_id,
       COUNT(*) AS ledger_count,
       GROUP_CONCAT(ledger_id ORDER BY ledger_id) AS ledger_ids,
       GROUP_CONCAT(IFNULL(store_id, 'NULL') ORDER BY ledger_id) AS stores
FROM db_finance_ledger
WHERE com_id = @com_id AND source = 'Receipt'
GROUP BY source_id
HAVING COUNT(*) > 1;

SELECT 'payment_missing_ledger' AS check_name,
       p.payment_id, p.payable_id, p.amount, p.store_id
FROM db_finance_payment p
LEFT JOIN db_finance_ledger l
  ON l.com_id = p.com_id AND l.source = 'Payment' AND l.source_id = p.payment_id
WHERE p.com_id = @com_id AND l.ledger_id IS NULL;

SELECT 'payable_payment_mismatch' AS check_name,
       y.payable_id, y.amount, y.paid_amount, y.status,
       COALESCE(SUM(p.amount), 0) AS payment_total
FROM db_finance_payable y
LEFT JOIN db_finance_payment p
  ON p.com_id = y.com_id AND p.payable_id = y.payable_id
WHERE y.com_id = @com_id
GROUP BY y.payable_id, y.amount, y.paid_amount, y.status
HAVING y.paid_amount <> COALESCE(SUM(p.amount), 0);
