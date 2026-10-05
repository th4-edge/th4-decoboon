-- 76-剩余收付款计划.sql
-- 目的：承接在跑项目时，把期初应收/应付拆成未来时间计划。
-- 口径：计划是期初余额的时间分解，不是新增应收应付；计划合计不得超过期初余额，不足部分为未排期。
-- 说明：收款/付款仍核销期初应收/应付，不核销计划项。

create table if not exists carryover_payment_plan (
    plan_id bigint not null,
    com_id bigint not null,
    item_id bigint not null,
    direction varchar(16) not null comment 'Receivable/Payable',
    amount decimal(14,2) not null default 0 comment '计划金额',
    planned_date date null comment '计划日期，为空表示未排期',
    counterparty varchar(128) null comment '往来对象',
    remark varchar(500) null comment '备注',
    sort int not null default 0 comment '排序',
    creator_id bigint null,
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
    primary key (plan_id),
    key idx_carryover_payment_item (com_id, item_id, direction, plan_id),
    key idx_carryover_payment_company (com_id, plan_id),
    constraint fk_carryover_payment_company foreign key (com_id) references db_tenant_company (com_id),
    constraint fk_carryover_payment_item foreign key (item_id) references carryover_project (item_id),
    constraint fk_carryover_payment_creator foreign key (creator_id) references db_tenant_employee (emp_id)
) engine=InnoDB default charset=utf8mb4 comment='剩余收付款计划';
