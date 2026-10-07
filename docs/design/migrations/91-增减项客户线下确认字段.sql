-- 91-增减项客户线下确认字段.sql
-- 目的：DEC-041 / BR-035 增减项客户线下确认——客户找设计师线下确认后形成变更单，需记录确认人与确认方式。
-- 影响表：db_contract_change。
-- 前置：db_contract_change 已存在（合同变更基础表）。
-- 回滚：alter table db_contract_change drop column customer_confirm_method, drop column customer_confirmed_by;
-- 说明：customer_confirmed_at 已存在，本次仅补充确认人/确认方式；历史数据允许为空，不回溯改写。

alter table db_contract_change
    add column customer_confirmed_by varchar(64) null comment '客户确认人' after customer_confirmed_at,
    add column customer_confirm_method varchar(32) null comment '客户确认方式' after customer_confirmed_by;
