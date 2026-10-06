-- 86-员工提成负向调整.sql
-- 目的：退款、离职、跨期产生的已发提成超额，形成独立负向调整，下期工资抵扣。
-- 边界：不修改已计提、已释放、已发放的原提成；调整金额不得超过原提成累计已发金额。
-- 适用：MySQL 8.0+。生产执行前必须备份并登记迁移账本。

CREATE TABLE IF NOT EXISTS db_payroll_commission_adjustment (
    adjustment_id BIGINT NOT NULL,
    com_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    team_id BIGINT NULL,
    emp_id BIGINT NOT NULL,
    project_id BIGINT NULL,
    original_commission_id BIGINT NOT NULL,
    idempotency_key VARCHAR(128) NULL COMMENT '企业内幂等键',
    idempotency_fingerprint VARCHAR(64) NULL COMMENT '业务指纹',
    payroll_item_id BIGINT NULL,
    type VARCHAR(24) NOT NULL COMMENT 'Refund/Resignation/CrossPeriod',
    status VARCHAR(24) NOT NULL COMMENT 'Pending/Included/Cancelled',
    refund_amount DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '本次退款/退单金额，仅 Refund 使用',
    deduction_amount DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '应抵扣金额，正数表示下期扣减',
    applied_amount DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '已进入工资单的抵扣金额',
    occurred_at DATE NOT NULL,
    remark VARCHAR(500) NULL,
    creator_id BIGINT NOT NULL,
    create_date_time BIGINT NULL, create_date INT NULL, create_time INT NULL, create_year INT NULL,
    create_month INT NULL, create_day INT NULL, create_hour INT NULL, create_minute INT NULL,
    create_second INT NULL, create_week TINYINT NULL,
    update_date_time BIGINT NULL, update_date INT NULL, update_time INT NULL, update_year INT NULL,
    update_month INT NULL, update_day INT NULL, update_hour INT NULL, update_minute INT NULL,
    update_second INT NULL, update_week TINYINT NULL,
    PRIMARY KEY (adjustment_id),
    UNIQUE KEY uk_payroll_adjustment_company_idempotency (com_id, idempotency_key),
    KEY idx_payroll_adjustment_company (com_id, adjustment_id),
    KEY idx_payroll_adjustment_employee (com_id, emp_id, status, adjustment_id),
    KEY idx_payroll_adjustment_original (com_id, original_commission_id, adjustment_id),
    KEY idx_payroll_adjustment_project (com_id, project_id, adjustment_id),
    KEY idx_payroll_adjustment_payroll_item (com_id, payroll_item_id, adjustment_id),
    KEY idx_payroll_adjustment_store (com_id, store_id, adjustment_id),
    CONSTRAINT fk_payroll_adjustment_company FOREIGN KEY (com_id) REFERENCES db_tenant_company (com_id),
    CONSTRAINT fk_payroll_adjustment_store FOREIGN KEY (store_id) REFERENCES db_tenant_store (store_id),
    CONSTRAINT fk_payroll_adjustment_team FOREIGN KEY (team_id) REFERENCES db_tenant_team (team_id),
    CONSTRAINT fk_payroll_adjustment_employee FOREIGN KEY (emp_id) REFERENCES db_tenant_employee (emp_id),
    CONSTRAINT fk_payroll_adjustment_project FOREIGN KEY (project_id) REFERENCES db_project (project_id),
    CONSTRAINT fk_payroll_adjustment_original FOREIGN KEY (original_commission_id) REFERENCES db_payroll_commission_item (commission_id),
    CONSTRAINT fk_payroll_adjustment_payroll_item FOREIGN KEY (payroll_item_id) REFERENCES db_payroll_item (item_id),
    CONSTRAINT fk_payroll_adjustment_creator FOREIGN KEY (creator_id) REFERENCES db_tenant_employee (emp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工提成负向调整';

CREATE INDEX idx_payroll_adjustment_status_period
    ON db_payroll_commission_adjustment (com_id, status, occurred_at, adjustment_id);

-- 回滚（仅在确认不保留负向调整事实时执行）：
-- DROP TABLE IF EXISTS db_payroll_commission_adjustment;
