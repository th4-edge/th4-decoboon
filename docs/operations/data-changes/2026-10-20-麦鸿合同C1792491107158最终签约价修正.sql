-- 生产数据修正：麦鸿装饰合同 C1792491107158 最终签约价由 112016.75 修正为 75500.00
-- 原因：App 合同页从计划选择页返回时用报价默认值覆盖了负责人录入的最终签约价。
-- 范围：合同签约价/当前合同额、项目合同额、付款计划总额与节点金额、合同节点应收金额。
-- 前置：执行前已确认各期应收 received_amount 均为 0.00；合同行报价快照与 baseline_amount 保持不变。

SET NAMES utf8mb4;
SET @contract_id = 238242788695605248;
SET @project_id = 238250397133701120;
SET @plan_id = 238250300270444544;
SET @old_total = 112016.75;
SET @new_total = 75500.00;

CREATE TABLE IF NOT EXISTS bak_20261020_contract_238242788695605248 AS
SELECT * FROM db_project_contract WHERE contract_id = @contract_id;
CREATE TABLE IF NOT EXISTS bak_20261020_project_238250397133701120 AS
SELECT * FROM db_project WHERE project_id = @project_id;
CREATE TABLE IF NOT EXISTS bak_20261020_payment_plan_238250300270444544 AS
SELECT * FROM db_contract_payment_plan WHERE plan_id = @plan_id;
CREATE TABLE IF NOT EXISTS bak_20261020_payment_node_238250300270444544 AS
SELECT * FROM db_contract_payment_node WHERE plan_id = @plan_id;
CREATE TABLE IF NOT EXISTS bak_20261020_receivable_238250397133701120 AS
SELECT * FROM db_finance_receivable WHERE project_id = @project_id;

START TRANSACTION;

UPDATE db_project_contract
SET signed_amount = @new_total,
    current_amount = @new_total
WHERE contract_id = @contract_id
  AND signed_amount = @old_total
  AND current_amount = @old_total;

UPDATE db_project
SET contract_amount = @new_total
WHERE project_id = @project_id;

UPDATE db_contract_payment_plan
SET total_amount = @new_total
WHERE plan_id = @plan_id;

UPDATE db_contract_payment_node
SET amount = CASE code
    WHEN 'PAY_NODE_1' THEN 22650.00
    WHEN 'PAY_NODE_2' THEN 18875.00
    WHEN 'PAY_NODE_3' THEN 18875.00
    WHEN 'PAY_NODE_4' THEN 12835.00
    WHEN 'PAY_NODE_5' THEN 2265.00
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

SELECT contract_id, no, baseline_amount, signed_amount, current_amount, status
FROM db_project_contract WHERE contract_id = @contract_id;
SELECT project_id, no, contract_amount FROM db_project WHERE project_id = @project_id;
SELECT plan_id, total_amount FROM db_contract_payment_plan WHERE plan_id = @plan_id;
SELECT code, ratio, amount FROM db_contract_payment_node WHERE plan_id = @plan_id ORDER BY sort;
SELECT receivable_id, name, amount, received_amount, status FROM db_finance_receivable
WHERE project_id = @project_id ORDER BY receivable_id;
SELECT SUM(n.amount) AS payment_node_total FROM db_contract_payment_node n WHERE n.plan_id = @plan_id;
SELECT SUM(r.amount) AS receivable_total FROM db_finance_receivable r WHERE r.project_id = @project_id;
