-- 40-合同最终签约价格
-- 日期：2026-09-20
-- 适用：MySQL 8.0+。存量库执行前请先备份；空库部署请同步使用 01-空库基线.sql。
--
-- 金额语义：
--   baseline_amount = 客户确认时的报价快照，只用于追溯，不随签约或变更改写；
--   signed_amount   = 签约时确认的最终价格，签约后冻结；
--   current_amount  = 最终签约价格叠加已确认合同变更后的当前金额。

ALTER TABLE db_project_contract
    ADD COLUMN signed_amount DECIMAL(14,2) NULL COMMENT '最终签约价格（签约后冻结）' AFTER baseline_amount;

-- 历史已签约合同没有录入最终价，按当时使用的报价基线回填；变更后的 current_amount 不回写 signed_amount。
UPDATE db_project_contract
   SET signed_amount = baseline_amount
 WHERE signed_at IS NOT NULL
   AND signed_amount IS NULL;

-- 回滚：
-- ALTER TABLE db_project_contract DROP COLUMN signed_amount;
