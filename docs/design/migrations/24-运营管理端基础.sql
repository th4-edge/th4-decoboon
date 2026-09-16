-- 运营管理端基础迁移（存量库执行；新空库已同步至 01-空库基线.sql）
-- 1. 标记平台运营企业，禁止将此标记授予普通客户企业。
-- 2. 新增人工操作审计表，所有订阅调整必须写入。

ALTER TABLE db_tenant_company
    ADD COLUMN platform BIT NOT NULL DEFAULT b'0' COMMENT '平台运营企业标记';

CREATE TABLE db_platform_operation_log (
    log_id BIGINT NOT NULL COMMENT '审计索引',
    action ENUM('Freeze','PlanChange','Renew','SeatAdjust','StatusChange','StoreAdjust','Unfreeze') NOT NULL COMMENT '操作动作',
    after_value VARCHAR(2048) COMMENT '变更后',
    before_value VARCHAR(2048) COMMENT '变更前',
    create_date INT COMMENT '创建日',
    create_date_time BIGINT COMMENT '创建时间戳',
    create_day INT COMMENT '创建日索引',
    create_hour INT COMMENT '创建小时',
    create_minute INT COMMENT '创建分钟',
    create_month INT COMMENT '创建月',
    create_second INT COMMENT '创建秒',
    create_time INT COMMENT '创建时分秒',
    create_week TINYINT COMMENT '创建星期',
    create_year INT COMMENT '创建年',
    reason VARCHAR(500) NOT NULL COMMENT '操作原因',
    summary VARCHAR(256) NOT NULL COMMENT '操作摘要',
    target_key VARCHAR(64) NOT NULL COMMENT '目标标识',
    target_type VARCHAR(32) NOT NULL COMMENT '目标类型',
    com_id BIGINT NOT NULL COMMENT '目标企业',
    emp_id BIGINT NOT NULL COMMENT '操作人',
    PRIMARY KEY (log_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台运营人工操作审计';

CREATE INDEX idx_platform_log_company ON db_platform_operation_log (com_id, log_id);
CREATE INDEX idx_platform_log_company_time ON db_platform_operation_log (com_id, create_date_time, log_id);
CREATE INDEX idx_platform_log_operator_time ON db_platform_operation_log (emp_id, create_date_time, log_id);

ALTER TABLE db_platform_operation_log
    ADD CONSTRAINT fk_platform_log_company FOREIGN KEY (com_id) REFERENCES db_tenant_company (com_id),
    ADD CONSTRAINT fk_platform_log_operator FOREIGN KEY (emp_id) REFERENCES db_tenant_employee (emp_id);

-- 上线后手工执行并核对：
-- UPDATE db_tenant_company SET platform = b'1' WHERE com_id = <平台运营企业ID>;
-- 然后在该企业下创建 Boss 员工和 BCrypt 密码凭证。
