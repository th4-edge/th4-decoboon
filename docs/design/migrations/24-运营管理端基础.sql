-- 运营管理端独立组织与用户迁移。
-- 若本机或测试库曾执行过旧版“Company.platform + Employee 外键”预发布迁移：
-- 1. 先将旧 db_platform_operation_log 导出留档；
-- 2. 执行 ALTER TABLE db_tenant_company DROP COLUMN platform;
-- 3. 执行 DROP TABLE db_platform_operation_log;
-- 4. 再执行本文件创建独立平台组织表。
-- 生产正式发布不得保留旧融合表或 company.platform 字段。
-- 设计原则：
-- 1. 平台运营组织、部门、用户、审计全部使用独立表。
-- 2. 平台表不建立任何指向 db_tenant_company、db_tenant_employee 的外键。
-- 3. 租户企业ID、订阅ID仅作为审计标量快照保存，不做数据库关联。
-- 4. 平台认证独立于租户 Credentials/Employee/Company。

CREATE TABLE IF NOT EXISTS db_platform_org (
    org_id BIGINT NOT NULL COMMENT '平台组织索引',
    code VARCHAR(32) NOT NULL COMMENT '平台组织编码',
    name VARCHAR(128) NOT NULL COMMENT '平台组织名称',
    status VARCHAR(16) NOT NULL COMMENT 'Active/Suspended',
    create_date INT,
    create_date_time BIGINT,
    create_day INT,
    create_hour INT,
    create_minute INT,
    create_month INT,
    create_second INT,
    create_time INT,
    create_week TINYINT,
    create_year INT,
    PRIMARY KEY (org_id),
    UNIQUE KEY uk_platform_org_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台独立运营组织';

CREATE TABLE IF NOT EXISTS db_platform_department (
    dept_id BIGINT NOT NULL COMMENT '平台部门索引',
    org_id BIGINT NOT NULL COMMENT '平台组织',
    parent_id BIGINT NULL COMMENT '上级平台部门',
    code VARCHAR(32) NOT NULL COMMENT '平台部门编码',
    name VARCHAR(64) NOT NULL COMMENT '平台部门名称',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    status VARCHAR(16) NOT NULL COMMENT 'Active/Suspended',
    create_date INT,
    create_date_time BIGINT,
    create_day INT,
    create_hour INT,
    create_minute INT,
    create_month INT,
    create_second INT,
    create_time INT,
    create_week TINYINT,
    create_year INT,
    PRIMARY KEY (dept_id),
    UNIQUE KEY uk_platform_dept_org_code (org_id, code),
    KEY idx_platform_dept_org (org_id, dept_id),
    KEY idx_platform_dept_parent (org_id, parent_id, dept_id),
    CONSTRAINT fk_platform_dept_org FOREIGN KEY (org_id) REFERENCES db_platform_org (org_id),
    CONSTRAINT fk_platform_dept_parent FOREIGN KEY (parent_id) REFERENCES db_platform_department (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台组织部门';

CREATE TABLE IF NOT EXISTS db_platform_user (
    user_id BIGINT NOT NULL COMMENT '平台用户索引',
    org_id BIGINT NOT NULL COMMENT '平台组织',
    dept_id BIGINT NULL COMMENT '平台部门',
    name VARCHAR(32) NOT NULL COMMENT '姓名',
    phone VARCHAR(20) NOT NULL COMMENT '手机号',
    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt密码哈希',
    role VARCHAR(32) NOT NULL COMMENT '平台岗位',
    status VARCHAR(16) NOT NULL COMMENT 'Active/Suspended/Locked',
    last_login_at DATETIME(6) NULL COMMENT '最近登录时间',
    create_date INT,
    create_date_time BIGINT,
    create_day INT,
    create_hour INT,
    create_minute INT,
    create_month INT,
    create_second INT,
    create_time INT,
    create_week TINYINT,
    create_year INT,
    update_date INT,
    update_date_time BIGINT,
    update_day INT,
    update_hour INT,
    update_minute INT,
    update_month INT,
    update_second INT,
    update_time INT,
    update_week TINYINT,
    update_year INT,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_platform_user_phone (phone),
    KEY idx_platform_user_org (org_id, user_id),
    KEY idx_platform_user_department (org_id, dept_id, user_id),
    KEY idx_platform_user_phone (phone, status),
    CONSTRAINT fk_platform_user_org FOREIGN KEY (org_id) REFERENCES db_platform_org (org_id),
    CONSTRAINT fk_platform_user_dept FOREIGN KEY (dept_id) REFERENCES db_platform_department (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台独立运营用户';

CREATE TABLE IF NOT EXISTS db_platform_operation_log (
    log_id BIGINT NOT NULL COMMENT '审计索引',
    org_id BIGINT NOT NULL COMMENT '平台组织',
    user_id BIGINT NOT NULL COMMENT '平台操作人',
    action VARCHAR(24) NOT NULL COMMENT '平台操作动作',
    target_type VARCHAR(32) NOT NULL COMMENT '目标类型',
    target_key VARCHAR(64) NOT NULL COMMENT '目标标识',
    target_com_id BIGINT NULL COMMENT '租户企业ID快照，无外键',
    target_company_title VARCHAR(128) NULL COMMENT '租户企业名称快照',
    target_sub_id BIGINT NULL COMMENT '租户订阅ID快照，无外键',
    summary VARCHAR(256) NOT NULL COMMENT '操作摘要',
    before_value VARCHAR(2048) NULL COMMENT '变更前',
    after_value VARCHAR(2048) NULL COMMENT '变更后',
    reason VARCHAR(500) NOT NULL COMMENT '操作原因',
    create_date INT,
    create_date_time BIGINT,
    create_day INT,
    create_hour INT,
    create_minute INT,
    create_month INT,
    create_second INT,
    create_time INT,
    create_week TINYINT,
    create_year INT,
    PRIMARY KEY (log_id),
    KEY idx_platform_log_org_time (org_id, create_date_time, log_id),
    KEY idx_platform_log_operator_time (user_id, create_date_time, log_id),
    KEY idx_platform_log_target_company (target_com_id, log_id),
    CONSTRAINT fk_platform_log_org FOREIGN KEY (org_id) REFERENCES db_platform_org (org_id),
    CONSTRAINT fk_platform_log_operator FOREIGN KEY (user_id) REFERENCES db_platform_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台独立人工操作审计';

-- 说明：本迁移不修改 db_tenant_company，不向平台表和租户表之间增加任何外键。
