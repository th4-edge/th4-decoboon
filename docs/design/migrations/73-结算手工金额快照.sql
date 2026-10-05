-- 73-结算手工金额快照.sql
-- 目的：项目结算快照记录手工关联项目的其他收入与项目零星成本，保持结算与项目财务报告同口径。
-- 影响表：db_finance_settlement。
-- 说明：历史结算快照默认补 0；后续结算重新生成时写入实际手工金额。

alter table db_finance_settlement
    add column manual_income_amount decimal(14,2) not null default 0 comment '手工关联项目其他收入',
    add column manual_cost_amount decimal(14,2) not null default 0 comment '手工项目零星成本';
