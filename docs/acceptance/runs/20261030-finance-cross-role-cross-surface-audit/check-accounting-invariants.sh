#!/usr/bin/env bash
set -euo pipefail

ROOT="/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon"
DB="${FINANCE_AUDIT_DB:-decoboon-test}"
COM_ID="${FINANCE_AUDIT_COM_ID:-999200000000000001}"
CONFIG="$ROOT/src/main/resources/application-dev.yml"

if [[ ! -f "$CONFIG" ]]; then
  echo "missing config: $CONFIG" >&2
  exit 2
fi

PW="$(awk '/^[[:space:]]*password:/{sub(/^[[:space:]]*password:[[:space:]]*/, ""); print; exit}' "$CONFIG")"
export MYSQL_PWD="$PW"

mysql -h127.0.0.1 -uroot -D "$DB" --batch --raw -N -e "
SELECT JSON_PRETTY(JSON_OBJECT(
  'database', DATABASE(),
  'companyId', ${COM_ID},
  'capturedAt', DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'),
  'receivable', JSON_OBJECT(
    'count', (SELECT COUNT(*) FROM db_finance_receivable WHERE com_id = ${COM_ID}),
    'amount', (SELECT COALESCE(SUM(amount), 0) FROM db_finance_receivable WHERE com_id = ${COM_ID}),
    'received', (SELECT COALESCE(SUM(received_amount), 0) FROM db_finance_receivable WHERE com_id = ${COM_ID}),
    'outstanding', (SELECT COALESCE(SUM(GREATEST(amount - received_amount, 0)), 0) FROM db_finance_receivable WHERE com_id = ${COM_ID}),
    'overReceived', (SELECT COUNT(*) FROM db_finance_receivable WHERE com_id = ${COM_ID} AND received_amount > amount),
    'missingProject', (SELECT COUNT(*) FROM db_finance_receivable r LEFT JOIN db_project p ON p.project_id = r.project_id AND p.com_id = r.com_id WHERE r.com_id = ${COM_ID} AND p.project_id IS NULL),
    'storelessCount', (SELECT COUNT(*) FROM db_finance_receivable WHERE com_id = ${COM_ID} AND store_id IS NULL),
    'storelessAmount', (SELECT COALESCE(SUM(amount), 0) FROM db_finance_receivable WHERE com_id = ${COM_ID} AND store_id IS NULL)
  ),
  'receipt', JSON_OBJECT(
    'activeCount', (SELECT COUNT(*) FROM db_finance_receipt WHERE com_id = ${COM_ID} AND status = 'Active'),
    'activeAmount', (SELECT COALESCE(SUM(amount), 0) FROM db_finance_receipt WHERE com_id = ${COM_ID} AND status = 'Active'),
    'missingLedger', (SELECT COUNT(*) FROM db_finance_receipt r WHERE r.com_id = ${COM_ID} AND NOT EXISTS (SELECT 1 FROM db_finance_ledger l WHERE l.com_id = r.com_id AND l.source = 'Receipt' AND l.source_id = r.receipt_id)),
    'duplicateLedgerSources', (SELECT COUNT(*) FROM (SELECT source_id FROM db_finance_ledger WHERE com_id = ${COM_ID} AND source = 'Receipt' GROUP BY source_id HAVING COUNT(*) > 1) d),
    'ledgerAmount', (SELECT COALESCE(SUM(amount), 0) FROM db_finance_ledger WHERE com_id = ${COM_ID} AND source = 'Receipt' AND direction = 'Income' AND voided = 0)
  ),
  'payable', JSON_OBJECT(
    'count', (SELECT COUNT(*) FROM db_finance_payable WHERE com_id = ${COM_ID}),
    'amount', (SELECT COALESCE(SUM(amount), 0) FROM db_finance_payable WHERE com_id = ${COM_ID}),
    'paid', (SELECT COALESCE(SUM(paid_amount), 0) FROM db_finance_payable WHERE com_id = ${COM_ID}),
    'outstanding', (SELECT COALESCE(SUM(GREATEST(amount - paid_amount, 0)), 0) FROM db_finance_payable WHERE com_id = ${COM_ID}),
    'overPaid', (SELECT COUNT(*) FROM db_finance_payable WHERE com_id = ${COM_ID} AND paid_amount > amount),
    'missingProject', (SELECT COUNT(*) FROM db_finance_payable y LEFT JOIN db_project p ON p.project_id = y.project_id AND p.com_id = y.com_id WHERE y.com_id = ${COM_ID} AND y.project_id IS NOT NULL AND p.project_id IS NULL),
    'storelessCount', (SELECT COUNT(*) FROM db_finance_payable WHERE com_id = ${COM_ID} AND store_id IS NULL),
    'storelessAmount', (SELECT COALESCE(SUM(amount), 0) FROM db_finance_payable WHERE com_id = ${COM_ID} AND store_id IS NULL)
  ),
  'payment', JSON_OBJECT(
    'count', (SELECT COUNT(*) FROM db_finance_payment WHERE com_id = ${COM_ID}),
    'amount', (SELECT COALESCE(SUM(amount), 0) FROM db_finance_payment WHERE com_id = ${COM_ID}),
    'missingLedger', (SELECT COUNT(*) FROM db_finance_payment p WHERE p.com_id = ${COM_ID} AND NOT EXISTS (SELECT 1 FROM db_finance_ledger l WHERE l.com_id = p.com_id AND l.source = 'Payment' AND l.source_id = p.payment_id)),
    'duplicateLedgerSources', (SELECT COUNT(*) FROM (SELECT source_id FROM db_finance_ledger WHERE com_id = ${COM_ID} AND source = 'Payment' GROUP BY source_id HAVING COUNT(*) > 1) d),
    'ledgerAmount', (SELECT COALESCE(SUM(amount), 0) FROM db_finance_ledger WHERE com_id = ${COM_ID} AND source = 'Payment' AND direction = 'Expense' AND voided = 0)
  ),
  'ledger', JSON_OBJECT(
    'count', (SELECT COUNT(*) FROM db_finance_ledger WHERE com_id = ${COM_ID} AND voided = 0),
    'income', (SELECT COALESCE(SUM(amount), 0) FROM db_finance_ledger WHERE com_id = ${COM_ID} AND direction = 'Income' AND voided = 0),
    'expense', (SELECT COALESCE(SUM(amount), 0) FROM db_finance_ledger WHERE com_id = ${COM_ID} AND direction = 'Expense' AND voided = 0),
    'net', (SELECT COALESCE(SUM(CASE WHEN direction = 'Income' THEN amount ELSE -amount END), 0) FROM db_finance_ledger WHERE com_id = ${COM_ID} AND voided = 0),
    'manualIncome', (SELECT COALESCE(SUM(amount), 0) FROM db_finance_ledger WHERE com_id = ${COM_ID} AND source = 'Manual' AND direction = 'Income' AND voided = 0),
    'manualExpense', (SELECT COALESCE(SUM(amount), 0) FROM db_finance_ledger WHERE com_id = ${COM_ID} AND source = 'Manual' AND direction = 'Expense' AND voided = 0)
  ),
  'project', JSON_OBJECT(
    'count', (SELECT COUNT(*) FROM db_project WHERE com_id = ${COM_ID}),
    'storelessCount', (SELECT COUNT(*) FROM db_project WHERE com_id = ${COM_ID} AND store_id IS NULL),
    'settlementCount', (SELECT COUNT(*) FROM db_finance_settlement WHERE com_id = ${COM_ID}),
    'costIncompleteSettlements', (SELECT COUNT(*) FROM db_finance_settlement WHERE com_id = ${COM_ID} AND profit_amount IS NULL),
    'incompleteWithProfit', (SELECT COUNT(*) FROM db_finance_settlement WHERE com_id = ${COM_ID} AND profit_amount IS NULL AND gross_margin IS NOT NULL)
  ),
  'expectedLedgerNet', (
    (SELECT COALESCE(SUM(amount), 0) FROM db_finance_receipt WHERE com_id = ${COM_ID} AND status = 'Active')
    + (SELECT COALESCE(SUM(amount), 0) FROM db_finance_ledger WHERE com_id = ${COM_ID} AND source = 'Manual' AND direction = 'Income' AND voided = 0)
    - (SELECT COALESCE(SUM(amount), 0) FROM db_finance_payment WHERE com_id = ${COM_ID})
    - (SELECT COALESCE(SUM(amount), 0) FROM db_finance_ledger WHERE com_id = ${COM_ID} AND source = 'Manual' AND direction = 'Expense' AND voided = 0)
  )
));" | jq '. as $r | . + {invariants: {receiptLedgerDelta: (((.receipt.ledgerAmount - .receipt.activeAmount) * 100 | round) / 100), paymentLedgerDelta: (((.payment.amount - .payment.ledgerAmount) * 100 | round) / 100), payablePaymentDelta: (((.payment.amount - .payable.paid) * 100 | round) / 100), ledgerNetDelta: (((.ledger.net - .expectedLedgerNet) * 100 | round) / 100), receiptSourceDuplicates: .receipt.duplicateLedgerSources, paymentLedgerMissing: .payment.missingLedger, storelessFinancialObjects: (.receivable.storelessCount + .payable.storelessCount + .project.storelessCount), coreBalanced: ((.receipt.ledgerAmount == .receipt.activeAmount) and (.payment.amount == .payment.ledgerAmount) and (.payment.amount == .payable.paid) and (.ledger.net == .expectedLedgerNet) and (.receipt.duplicateLedgerSources == 0) and (.payment.missingLedger == 0))}}'
