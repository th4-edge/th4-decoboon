-- 回滚：将 C1792491107158 恢复为本次修正前的 112016.75 及原应收计划。
SET NAMES utf8mb4;
SET @contract_id = 238242788695605248;
SET @project_id = 238250397133701120;
SET @plan_id = 238250300270444544;
SET @old_total = 112016.75;

START TRANSACTION;
UPDATE db_project_contract
SET signed_amount = @old_total,
    current_amount = @old_total
WHERE contract_id = @contract_id;
UPDATE db_project
SET contract_amount = @old_total
WHERE project_id = @project_id;
UPDATE db_contract_payment_plan
SET total_amount = @old_total
WHERE plan_id = @plan_id;
UPDATE db_contract_payment_node
SET amount = CASE code
    WHEN 'PAY_NODE_1' THEN 33605.03
    WHEN 'PAY_NODE_2' THEN 28004.19
    WHEN 'PAY_NODE_3' THEN 28004.19
    WHEN 'PAY_NODE_4' THEN 19042.85
    WHEN 'PAY_NODE_5' THEN 3360.49
    ELSE amount
END
WHERE plan_id = @plan_id;
UPDATE db_finance_receivable r
JOIN db_contract_payment_node n ON n.receivable_id = r.receivable_id
SET r.amount = n.amount
WHERE r.project_id = @project_id
  AND n.plan_id = @plan_id
  AND r.received_amount = 0.00;
COMMIT;
