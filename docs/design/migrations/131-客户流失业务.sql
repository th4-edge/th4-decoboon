-- 131-客户流失业务.sql
-- 目的：记录签约前客户整体或单套房屋的流失事实，支持阶段、原因、操作人、恢复与幂等追溯。
-- 影响表：cust_customer_loss（新增）。
-- 前置：cust_customer、db_property、db_tenant_company、db_tenant_employee 已存在。
-- 说明：不复制、不删除、不回写量房/设计/报价/合同历史；客户状态仍使用 CustStatus.Lost。
-- 回滚：docs/design/migrations/rollback/131-客户流失业务-rollback.sql。

-- 状态日志原表在不同历史库中可能仍是四值 ENUM；本迁移统一改为 VARCHAR，
-- 同时把原因说明扩展到 500 字，避免流失原因或恢复说明超过 200 字时整体事务回滚。
alter table cust_customer_status_log
    modify column change_type varchar(24) not null comment '变更类型',
    modify column reason_text varchar(500) null comment '原因说明';

create table cust_customer_loss (
    loss_id bigint not null,
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
    loss_scope enum ('Customer','Property') not null,
    loss_stage enum ('Design','FollowUp','Measure','Negotiation','Signing') not null,
    from_status enum ('Dormant','Finished','Following','Lost','New','PendingMeasure','Quoted','Serving','Signed'),
    reason_code enum ('ChoseOtherCompany','DesignUnsatisfied','Other','PriceBudgetMismatch','ProjectCancelled','ScheduleMismatch','SelfOrRelative','ServiceTrust') not null,
    reason_text varchar(500),
    competitor_name varchar(100),
    lost_at datetime(6) not null,
    operator_role enum ('Boss','Designer','Finance','Marketer','Pm','StoreManager','TeamLeader') not null,
    request_id varchar(96) not null,
    restored_at datetime(6),
    restore_status enum ('Dormant','Finished','Following','Lost','New','PendingMeasure','Quoted','Serving','Signed'),
    restore_note varchar(500),
    restore_request_id varchar(96),
    com_id bigint not null,
    cust_id bigint not null,
    property_id bigint,
    lost_by bigint not null,
    restored_by bigint,
    primary key (loss_id),
    unique key uk_customer_loss_request (com_id, request_id),
    key idx_customer_loss_company (com_id, loss_id),
    key idx_customer_loss_customer_active (com_id, cust_id, restored_at, loss_id),
    key idx_customer_loss_property_active (com_id, property_id, restored_at, loss_id),
    key idx_customer_loss_stage_reason (com_id, loss_stage, reason_code, lost_at),
    constraint fk_customer_loss_company foreign key (com_id) references db_tenant_company (com_id),
    constraint fk_customer_loss_customer foreign key (cust_id) references cust_customer (cust_id),
    constraint fk_customer_loss_property foreign key (property_id) references db_property (property_id),
    constraint fk_customer_loss_operator foreign key (lost_by) references db_tenant_employee (emp_id),
    constraint fk_customer_loss_restored_by foreign key (restored_by) references db_tenant_employee (emp_id)
) engine=InnoDB;
