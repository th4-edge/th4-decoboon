-- 46-主材选材记录.sql
-- 依据 SCN-006 / BR-004 / DEC-003：签约后主材选材以文本保存，按版本保留历史，不回写报价、合同、成本、材料计划和采购事实。
-- 选材文本为 longtext，禁止依赖 ddl-auto:update 改写既有列类型（ECD-009）。

create table if not exists db_material_selection (
    selection_id bigint not null,
    com_id bigint not null,
    store_id bigint null,
    team_id bigint null,
    owner_id bigint null,
    customer_id bigint not null,
    property_id bigint not null,
    contract_id bigint null,
    project_id bigint null,
    version int not null default 1,
    status varchar(24) not null,
    title varchar(128) null,
    content longtext not null,
    designer_id bigint null,
    confirmer_name varchar(64) null,
    confirmed_by_id bigint null,
    confirmed_at datetime(6) null,
    remark varchar(500) null,
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
    primary key (selection_id),
    unique key uk_material_selection_version (com_id, property_id, contract_id, version),
    key idx_material_selection_company (com_id, selection_id),
    key idx_material_selection_company_property (com_id, property_id, selection_id),
    key idx_material_selection_company_contract (com_id, contract_id, selection_id),
    key idx_material_selection_company_project (com_id, project_id, selection_id),
    key idx_material_selection_company_store (com_id, store_id, selection_id),
    key idx_material_selection_company_team (com_id, team_id, selection_id),
    key idx_material_selection_company_owner (com_id, owner_id, selection_id),
    constraint fk_material_selection_company foreign key (com_id) references db_tenant_company (com_id)
) engine=InnoDB default charset=utf8mb4;

create table if not exists db_material_selection_attachments (
    selection_id bigint not null,
    url varchar(512) null,
    key idx_material_selection_attachments (selection_id),
    constraint fk_material_selection_attachments_selection
        foreign key (selection_id) references db_material_selection (selection_id)
) engine=InnoDB default charset=utf8mb4;

-- 回滚（人工确认后执行）：
-- DROP TABLE IF EXISTS db_material_selection_attachments;
-- DROP TABLE IF EXISTS db_material_selection;
