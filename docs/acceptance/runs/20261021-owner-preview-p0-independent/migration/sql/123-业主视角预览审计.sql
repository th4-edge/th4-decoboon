-- 123-业主视角预览审计.sql
-- 目的：记录员工使用自身身份打开业主视角只读预览的最小安全审计。
-- 影响表：新增 db_project_owner_preview_log，不改现有业务表。
-- 前置：db_tenant_company、db_tenant_employee、db_project 已存在。
-- 回滚：docs/design/migrations/rollback/123-业主视角预览审计-rollback.sql。
-- 边界：不记录业主点击、停留、访问频率、页面正文或业主已读；本表不作为业务状态。

create table db_project_owner_preview_log (
    log_id bigint not null,
    create_date integer,
    create_date_time bigint,
    create_day integer,
    create_hour integer,
    create_minute integer,
    create_month integer,
    create_second integer,
    create_time integer,
    create_week tinyint,
    create_year integer,
    update_date integer,
    update_date_time bigint,
    update_day integer,
    update_hour integer,
    update_minute integer,
    update_month integer,
    update_second integer,
    update_time integer,
    update_week tinyint,
    update_year integer,
    com_id bigint not null,
    project_id bigint null,
    requested_project_id bigint not null,
    employee_id bigint not null,
    employee_name varchar(15) not null,
    employee_role varchar(24) not null,
    source varchar(16) not null,
    result varchar(16) not null,
    opened_at datetime(6) not null,
    request_id varchar(128) null,
    primary key (log_id),
    key idx_owner_preview_company_project (com_id, project_id, log_id),
    key idx_owner_preview_company_requested (com_id, requested_project_id, log_id),
    key idx_owner_preview_company_employee (com_id, employee_id, log_id),
    key idx_owner_preview_company_opened (com_id, opened_at),
    constraint fk_owner_preview_company foreign key (com_id) references db_tenant_company (com_id),
    constraint fk_owner_preview_project foreign key (project_id) references db_project (project_id),
    constraint fk_owner_preview_employee foreign key (employee_id) references db_tenant_employee (emp_id)
) engine=InnoDB default charset=utf8mb4 comment='业主视角预览最小安全审计';
