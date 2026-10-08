-- 92-客户休眠状态与风险跟进排除.sql
-- 目的：DEC-042 / BR-036 新增 Dormant 客户状态、休眠原因与状态变更日志。
-- 影响表：cust_customer、cust_customer_status_log。
-- 前置：cust_customer、db_tenant_company、db_tenant_employee 已存在。
-- 回滚：先 drop table cust_customer_status_log，再删除 cust_customer 新增外键/列并将 status 恢复为原枚举。
-- 说明：历史客户不回溯，新增列允许为空。

alter table cust_customer
    modify column status enum ('Finished','Following','Lost','New','PendingMeasure','Quoted','Serving','Signed','Dormant') not null,
    add column status_before_dormant enum ('Finished','Following','Lost','New','PendingMeasure','Quoted','Serving','Signed','Dormant') null comment '休眠前客户状态' after next_follow_at,
    add column dormant_reason varchar(32) null comment '休眠原因编码' after status_before_dormant,
    add column dormant_reason_text varchar(200) null comment '休眠原因说明' after dormant_reason,
    add column dormant_at datetime(6) null comment '休眠时间' after dormant_reason_text,
    add column dormant_by bigint null comment '休眠操作人' after dormant_at,
    add column restored_at datetime(6) null comment '最近恢复时间' after dormant_by,
    add column restored_by bigint null comment '最近恢复操作人' after restored_at;

alter table cust_customer
    add constraint fk_customer_dormant_by foreign key (dormant_by) references db_tenant_employee (emp_id),
    add constraint fk_customer_restored_by foreign key (restored_by) references db_tenant_employee (emp_id);

create table cust_customer_status_log (
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
    from_status enum ('Finished','Following','Lost','New','PendingMeasure','Quoted','Serving','Signed','Dormant') null,
    to_status enum ('Finished','Following','Lost','New','PendingMeasure','Quoted','Serving','Signed','Dormant') not null,
    change_type varchar(24) not null,
    reason_code varchar(32) null,
    reason_text varchar(200) null,
    operator_role enum ('Boss','Designer','Finance','Marketer','Pm','StoreManager','TeamLeader') not null,
    request_id varchar(96) null,
    changed_at datetime(6) not null,
    com_id bigint not null,
    cust_id bigint not null,
    operator_id bigint not null,
    primary key (log_id),
    unique key uk_customer_status_log_request (com_id, request_id),
    key idx_customer_status_log_customer (com_id, cust_id, log_id),
    key idx_customer_status_log_status (com_id, to_status, log_id),
    constraint fk_customer_status_log_company foreign key (com_id) references db_tenant_company (com_id),
    constraint fk_customer_status_log_customer foreign key (cust_id) references cust_customer (cust_id),
    constraint fk_customer_status_log_operator foreign key (operator_id) references db_tenant_employee (emp_id)
) engine=InnoDB;
