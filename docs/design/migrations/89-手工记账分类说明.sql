-- 89-手工记账分类说明.sql
-- 目的：为手工记账业务分类增加企业可维护的分类说明。
-- 影响表：db_finance_ledger_category。
-- 前置：68-手工记账分类与更正审计.sql 已执行。
-- 回滚：alter table db_finance_ledger_category drop column description;

alter table db_finance_ledger_category
    add column description varchar(500) null comment '分类说明：适用场景、举例和禁止用途' after name;
