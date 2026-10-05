-- 74-期初资金余额.sql
-- 目的：承接在跑项目时登记接管日各资金账户起始余额。
-- 口径：期初余额不计入收入/支出/利润；当前余额 = 期初余额 + 接管后净流量。
-- 唯一性：同一企业 + 同一账户 + 同一接管日只保留一条，可更正。

create table if not exists db_finance_opening_balance (
    opening_id bigint not null,
    com_id bigint not null,
    account varchar(16) not null comment '资金账户',
    amount decimal(14,2) not null default 0 comment '接管日余额，允许负数表示透支',
    as_of_date date not null comment '接管日',
    remark varchar(500) null comment '备注',
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
    primary key (opening_id),
    unique key uk_fin_opening_company_account_date (com_id, account, as_of_date),
    key idx_fin_opening_company_date (com_id, as_of_date, opening_id),
    constraint fk_fin_opening_company foreign key (com_id) references db_tenant_company (com_id),
    constraint fk_fin_opening_creator foreign key (creator_id) references db_tenant_employee (emp_id)
) engine=InnoDB default charset=utf8mb4 comment='期初资金余额';
