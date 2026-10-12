-- 2026-10-13 增减项存量 Approved（旧「待客户确认」）只读清点
-- 依据：DEC-068 / T12-BIZ-20261012-004 / PLAN-20261013-001
-- 目标环境：**生产**（开发/test 环境数据不作为依据，无需清点）。
-- 用途：在生产库执行，产出《收口清单》。**本文件全部为 SELECT，不含任何写操作。**
-- 执行：mysql -h 127.0.0.1 -uroot -p decoboon < 2026-10-13-增减项存量Approved清点.sql   （在生产应用服务器上执行）
-- 前置：已取得生产数据变更授权；建议先 `mysqldump` 备份 db_contract_change / db_project_contract / db_finance_receivable。
-- 说明：表名与列名已按真实 schema 核对（本地开发库执行退出码 0、0 错误）；生产数据量以实际执行结果为准。

-- ============================================================
-- §1 总量与分布（先看规模，决定收口批次）
-- ============================================================
-- [stat:begin]
SELECT status AS 状态,
       COUNT(*) AS 单数,
       IFNULL(SUM(amount), 0) AS 金额合计,
       SUM(customer_confirmed = 1) AS 已标记确认,
       SUM(approved_at IS NULL) AS 缺审批时间,
       SUM(approver_id IS NULL) AS 缺审批人
FROM db_contract_change
GROUP BY status
ORDER BY FIELD(status, 'Pending', 'Approved', 'CustomerConfirmed', 'Rejected', 'Cancelled');
-- [stat:end]

-- ============================================================
-- §2 逐单明细（收口清单；被 2026-10-13-增减项存量Approved清点-只读一键干跑.sh
--    按 [plan-export] 标记抽取并导出为 plan.csv，勿改标记）
-- ============================================================
-- [plan-export:begin]
SELECT c.change_id             AS change_id,
       c.no                    AS change_no,
       c.com_id                AS com_id,
       c.project_id            AS project_id,
       c.contract_id           AS contract_id,
       c.type                  AS 变更类型,
       c.amount                AS 变更金额,
       c.cost_delta            AS 成本差额,
       k.current_amount        AS 收口前合同额,
       k.current_amount + c.amount AS 收口后合同额,
       p.status                AS 项目状态,
       k.status                AS 合同状态,
       (SELECT COUNT(*) FROM db_contract_change_item i WHERE i.change_id = c.change_id) AS 明细条数,
       c.approved_at           AS 审批时间,
       c.approver_id           AS 审批人ID,
       c.customer_confirmed    AS 历史确认标记,
       c.customer_confirmed_at AS 历史确认时间
FROM db_contract_change c
LEFT JOIN db_project_contract k ON k.contract_id = c.contract_id
LEFT JOIN db_project          p ON p.project_id  = c.project_id
WHERE c.status = 'Approved'
ORDER BY c.com_id, c.project_id, c.change_id;
-- [plan-export:end]

-- ============================================================
-- §3 风险筛查 A：收口后合同额会被拖负（BR-041 拒绝条件 → 不可直接收口）
-- ============================================================
-- [risk-negative:begin]
SELECT c.change_id, c.no AS change_no, c.com_id, c.project_id,
       c.amount AS 变更金额, k.current_amount AS 收口前合同额,
       k.current_amount + c.amount AS 收口后合同额
FROM db_contract_change c
JOIN db_project_contract k ON k.contract_id = c.contract_id
WHERE c.status = 'Approved'
  AND k.current_amount + c.amount < 0;
-- [risk-negative:end]

-- ============================================================
-- §4 风险筛查 B：项目已完工/已结算、合同已关闭/终止（业务上不应再收口）
-- ============================================================
-- [risk-closed:begin]
SELECT c.change_id, c.no AS change_no, c.com_id, c.project_id,
       p.status AS 项目状态, k.status AS 合同状态
FROM db_contract_change c
LEFT JOIN db_project          p ON p.project_id  = c.project_id
LEFT JOIN db_project_contract k ON k.contract_id = c.contract_id
WHERE c.status = 'Approved'
  AND (p.status IN ('Finished', 'Settled')       -- 已完工 / 已结算
    OR k.status IN ('Closed', 'Terminated')      -- 已关闭 / 已终止
    OR p.project_id IS NULL OR k.contract_id IS NULL);  -- 关联缺失
-- [risk-closed:end]

-- ============================================================
-- [integrity:begin]
-- §5 关联完整性：是否已存在应收（防“已收口但状态未落”的假象）
-- 注：db_finance_receivable 无 change_id 外键，按名称/来源文本匹配，需在真实数据上先验证一次匹配率。
-- ============================================================
SELECT c.change_id, c.no AS change_no,
       (SELECT COUNT(*) FROM db_finance_receivable r
         WHERE r.project_id = c.project_id
           AND (r.name LIKE CONCAT('%', c.no, '%') OR r.business_origin LIKE CONCAT('%', c.no, '%'))) AS 关联应收条数,
       (SELECT COUNT(*) FROM db_project_task t   WHERE t.project_id = c.project_id) AS 项目任务数,
       (SELECT COUNT(*) FROM db_material_plan m  WHERE m.project_id = c.project_id) AS 项目材料计划数
FROM db_contract_change c
WHERE c.status = 'Approved'
ORDER BY c.change_id;
-- [integrity:end]
