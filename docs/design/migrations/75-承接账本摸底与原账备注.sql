-- 75-承接账本摸底与原账备注.sql
-- 目的：承接时记录客户账本摸底说明与项目原账口径备注，用于对照和双轨对账。
-- 说明：只做备注，不建立会计科目主数据；不影响任何金额计算。

alter table carryover_batch
    add column bookkeeping_survey_note varchar(2000) null comment '账本摸底说明';

alter table carryover_project
    add column legacy_account_note varchar(1000) null comment '原账口径备注';
