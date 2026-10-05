-- 71-手工账务作废.sql
-- 目的：企业负责人可对手工记账流水做逻辑作废（不物理删除）。
-- 影响表：db_finance_ledger（资金流水）。
-- 前置：68-手工记账分类与更正审计.sql 已执行。
-- 说明：资金流水仍保留原记录、金额和审计；作废后由应用层过滤，不再进入台账与汇总。

alter table db_finance_ledger
    add column voided tinyint(1) not null default 0 comment '是否作废',
    add column voided_by_id bigint null comment '作废人',
    add column voided_at datetime(3) null comment '作废时间',
    add column void_reason varchar(500) null comment '作废原因';

create index idx_fin_ledger_voided on db_finance_ledger (com_id, source, voided, ledger_id);

alter table db_finance_ledger
    add constraint fk_fin_ledger_voided_by foreign key (voided_by_id) references db_tenant_employee (emp_id);
