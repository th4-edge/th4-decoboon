-- 79-凭证补录修订轨迹.sql
-- 目的：工资付款补录发放工人后，记录同一凭证补录前后的工人、操作人、时间和说明。
-- 口径：只记录凭证修订事实，不修改付款金额、应付核销、资金台账或凭证金额。
-- 适用：MySQL 8.0+。存量库执行前先备份；空库部署走 01-空库基线后按迁移账本顺延执行。

create table if not exists db_voucher_revision (
    revision_id bigint not null,
    com_id bigint not null,
    voucher_id bigint not null,
    before_worker_id bigint null comment '补录前工人，新建时为空',
    after_worker_id bigint null comment '补录后工人',
    change_type varchar(32) not null default 'WorkerBackfill' comment '修订类型',
    reason varchar(500) null comment '修订说明',
    operator_id bigint not null comment '操作人',
    changed_at datetime(6) not null comment '变更时间',
    create_date_time bigint null,
    create_date int null,
    create_time int null,
    create_year int null,
    create_month int null,
    create_day int null,
    create_hour int null,
    create_minute int null,
    create_second int null,
    create_week int null,
    update_date_time bigint null,
    update_date int null,
    update_time int null,
    update_year int null,
    update_month int null,
    update_day int null,
    update_hour int null,
    update_minute int null,
    update_second int null,
    update_week int null,
    primary key (revision_id),
    key idx_voucher_revision_company (com_id, revision_id),
    key idx_voucher_revision_voucher (com_id, voucher_id, revision_id),
    key idx_voucher_revision_operator (com_id, operator_id, revision_id),
    constraint fk_voucher_revision_company foreign key (com_id) references db_tenant_company (com_id),
    constraint fk_voucher_revision_voucher foreign key (voucher_id) references db_voucher_record (voucher_id),
    constraint fk_voucher_revision_before_worker foreign key (before_worker_id) references db_worker (worker_id),
    constraint fk_voucher_revision_after_worker foreign key (after_worker_id) references db_worker (worker_id),
    constraint fk_voucher_revision_operator foreign key (operator_id) references db_tenant_employee (emp_id)
) engine=InnoDB default charset=utf8mb4 comment='凭证补录修订轨迹';

-- 回滚（仅在确认无需追溯补录历史时执行）：
-- DROP TABLE db_voucher_revision;
