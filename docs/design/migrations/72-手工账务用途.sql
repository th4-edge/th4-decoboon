-- 72-手工账务用途.sql
-- 目的：手工记账回归文本分类，新增“账务用途”枚举，支撑禁止清单与项目成本/收入口径。
-- 影响表：db_finance_ledger（资金流水）。
-- 说明：自动流水 purpose 为空；历史手工流水 purpose 为空，视为“其他”，暂不批量处理。

alter table db_finance_ledger
    add column purpose varchar(24) null comment '手工账务用途：项目零星成本/企业费用/其他收入/资金往来/其他';

create index idx_fin_ledger_manual_purpose
    on db_finance_ledger (com_id, source, purpose, project_id, ledger_id);
