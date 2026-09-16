-- 业主闭环：显式微信身份/客户/项目授权、邀请回填和验收操作人。
-- 目标数据库：MySQL 8+；请在发布窗口按顺序执行，不要由应用启动自动执行。

CREATE TABLE IF NOT EXISTS cust_customer_access (
    access_id BIGINT NOT NULL,
    com_id BIGINT NOT NULL,
    cre_id BIGINT NOT NULL,
    cust_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    invite_id BIGINT NULL,
    display_name VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    granted_at DATETIME(6) NULL,
    revoked_at DATETIME(6) NULL,
    revoked_by BIGINT NULL,
    create_date_time DATETIME(6) NULL,
    create_date DATE NULL,
    create_time TIME(6) NULL,
    create_year INT NULL,
    create_month INT NULL,
    create_day INT NULL,
    create_hour INT NULL,
    create_minute INT NULL,
    create_second INT NULL,
    create_week INT NULL,
    update_date_time DATETIME(6) NULL,
    update_date DATE NULL,
    update_time TIME(6) NULL,
    update_year INT NULL,
    update_month INT NULL,
    update_day INT NULL,
    update_hour INT NULL,
    update_minute INT NULL,
    update_second INT NULL,
    update_week INT NULL,
    PRIMARY KEY (access_id),
    UNIQUE KEY uk_customer_access_credential_project (cre_id, project_id),
    KEY idx_customer_access_company (com_id, access_id),
    KEY idx_customer_access_company_project (com_id, project_id, access_id),
    KEY idx_customer_access_company_customer (com_id, cust_id, access_id),
    KEY idx_customer_access_company_credential (com_id, cre_id, access_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 存量邀请先允许 customer_id 为空，完成项目客户回填后再收紧非空。
ALTER TABLE cust_customer_invite
    ADD COLUMN customer_id BIGINT NULL,
    ADD COLUMN accepted_by BIGINT NULL,
    ADD COLUMN cancelled_by BIGINT NULL,
    ADD COLUMN accepted_at DATETIME(6) NULL,
    ADD COLUMN cancelled_at DATETIME(6) NULL,
    ADD COLUMN create_date_time DATETIME(6) NULL,
    ADD COLUMN create_date DATE NULL,
    ADD COLUMN create_time TIME(6) NULL,
    ADD COLUMN create_year INT NULL,
    ADD COLUMN create_month INT NULL,
    ADD COLUMN create_day INT NULL,
    ADD COLUMN create_hour INT NULL,
    ADD COLUMN create_minute INT NULL,
    ADD COLUMN create_second INT NULL,
    ADD COLUMN create_week INT NULL,
    ADD COLUMN update_date_time DATETIME(6) NULL,
    ADD COLUMN update_date DATE NULL,
    ADD COLUMN update_time TIME(6) NULL,
    ADD COLUMN update_year INT NULL,
    ADD COLUMN update_month INT NULL,
    ADD COLUMN update_day INT NULL,
    ADD COLUMN update_hour INT NULL,
    ADD COLUMN update_minute INT NULL,
    ADD COLUMN update_second INT NULL,
    ADD COLUMN update_week INT NULL;

UPDATE cust_customer_invite invite
JOIN db_decoboon_project project
  ON project.project_id = invite.project_id
SET invite.customer_id = project.customer_id
WHERE invite.customer_id IS NULL;

-- 若存在无法关联项目的孤儿邀请，本语句会失败，应人工核验后再收紧。
ALTER TABLE cust_customer_invite
    MODIFY COLUMN customer_id BIGINT NOT NULL,
    ADD KEY idx_customer_invite_company (com_id, invite_id),
    ADD KEY idx_customer_invite_company_project (com_id, project_id, invite_id);

ALTER TABLE db_decoboon_acceptance
    ADD COLUMN confirmed_by BIGINT NULL;

-- 外键在存量数据核验后由 DBA 统一补建；应用服务端已校验以下关系：
-- cust_customer_access.com_id -> db_tenant_company.com_id
-- cust_customer_access.cre_id -> db_tenant_credentials.cre_id
-- cust_customer_access.cust_id -> cust_customer.cust_id
-- cust_customer_access.project_id -> db_decoboon_project.project_id
-- cust_customer_access.invite_id -> cust_customer_invite.invite_id
-- cust_customer_invite.customer_id -> cust_customer.cust_id
-- cust_customer_invite.accepted_by -> db_tenant_credentials.cre_id
-- db_decoboon_acceptance.confirmed_by -> db_tenant_credentials.cre_id
