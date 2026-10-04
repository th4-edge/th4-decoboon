-- 68-手工记账分类与更正审计.sql
-- 目的：
-- 1. 保留 db_finance_ledger.category 文本快照，兼容既有手工记账数据；
-- 2. 新增企业两级经营分类，内置分类首次使用时按企业生成，企业可追加自定义分类；
-- 3. 新增 Boss-only 元数据更正与冲正重记审计。
-- 本文件尚未对生产库执行。

alter table db_finance_ledger
    modify column category varchar(128) not null;

alter table db_finance_ledger
    add column category_id bigint null comment '结构化业务分类',
    add column original_ledger_id bigint null comment '被冲正/更正的原流水',
    add column correction_group_id varchar(64) null comment '冲正重记分组标识';

create index idx_fin_ledger_category on db_finance_ledger (com_id, category_id, ledger_id);
create index idx_fin_ledger_correction on db_finance_ledger (com_id, original_ledger_id, correction_group_id);

create table if not exists db_finance_ledger_category (
    category_id bigint not null,
    com_id bigint not null,
    parent_id bigint null,
    name varchar(64) not null,
    direction varchar(16) not null,
    code varchar(64) null,
    built_in tinyint(1) not null default 0,
    sort_no int not null default 0,
    active tinyint(1) not null default 1,
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
    primary key (category_id),
    unique key uk_fin_category_company_code (com_id, direction, code),
    key idx_fin_category_company_parent (com_id, parent_id, direction, sort_no),
    key idx_fin_category_company (com_id, category_id),
    constraint fk_fin_category_company foreign key (com_id) references db_tenant_company (com_id),
    constraint fk_fin_category_parent foreign key (parent_id) references db_finance_ledger_category (category_id),
    constraint fk_fin_category_creator foreign key (creator_id) references db_tenant_employee (emp_id)
) engine=InnoDB default charset=utf8mb4 comment='手工记账业务分类';

create table if not exists db_finance_ledger_revision (
    revision_id bigint not null,
    com_id bigint not null,
    ledger_id bigint not null,
    type varchar(32) not null,
    operated_by_id bigint not null,
    reason varchar(500) not null,
    before_snapshot longtext not null,
    after_snapshot longtext not null,
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
    key idx_fin_revision_company (com_id, revision_id),
    key idx_fin_revision_ledger (com_id, ledger_id, revision_id),
    constraint fk_fin_revision_company foreign key (com_id) references db_tenant_company (com_id),
    constraint fk_fin_revision_ledger foreign key (ledger_id) references db_finance_ledger (ledger_id),
    constraint fk_fin_revision_operator foreign key (operated_by_id) references db_tenant_employee (emp_id)
) engine=InnoDB default charset=utf8mb4 comment='手工流水更正审计';
