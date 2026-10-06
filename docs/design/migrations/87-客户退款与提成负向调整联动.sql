-- 87-客户退款与提成负向调整联动.sql
-- 目的：客户退款/退单形成独立资金事实，并自动触发原项目已发提成的负向调整。
-- 边界：退款不反改原收款、应收和已发提成；有效回款按收款减退款净额计算。
-- 适用：MySQL 8.0+。生产执行前必须备份并登记迁移账本。

ALTER TABLE db_finance_ledger
    MODIFY COLUMN source ENUM('Adjustment','Manual','Payment','Receipt','Refund') NOT NULL;

CREATE TABLE IF NOT EXISTS db_finance_refund (
    refund_id BIGINT NOT NULL,
    com_id BIGINT NOT NULL,
    store_id BIGINT NULL,
    team_id BIGINT NULL,
    owner_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    receipt_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    account VARCHAR(16) NOT NULL,
    refund_at DATE NOT NULL,
    reason VARCHAR(500) NULL,
    idempotency_key VARCHAR(128) NULL,
    idempotency_fingerprint VARCHAR(64) NULL,
    creator_id BIGINT NOT NULL,
    create_date_time BIGINT NULL, create_date INT NULL, create_time INT NULL, create_year INT NULL,
    create_month INT NULL, create_day INT NULL, create_hour INT NULL, create_minute INT NULL,
    create_second INT NULL, create_week TINYINT NULL,
    update_date_time BIGINT NULL, update_date INT NULL, update_time INT NULL, update_year INT NULL,
    update_month INT NULL, update_day INT NULL, update_hour INT NULL, update_minute INT NULL,
    update_second INT NULL, update_week TINYINT NULL,
    PRIMARY KEY (refund_id),
    UNIQUE KEY uk_fin_refund_company_idempotency (com_id, idempotency_key),
    KEY idx_fin_refund_company (com_id, refund_id),
    KEY idx_fin_refund_project (com_id, project_id, refund_id),
    KEY idx_fin_refund_receipt (com_id, receipt_id, refund_id),
    KEY idx_fin_refund_customer (com_id, customer_id, refund_id),
    KEY idx_fin_refund_store (com_id, store_id, refund_id),
    CONSTRAINT fk_fin_refund_company FOREIGN KEY (com_id) REFERENCES db_tenant_company (com_id),
    CONSTRAINT fk_fin_refund_store FOREIGN KEY (store_id) REFERENCES db_tenant_store (store_id),
    CONSTRAINT fk_fin_refund_team FOREIGN KEY (team_id) REFERENCES db_tenant_team (team_id),
    CONSTRAINT fk_fin_refund_owner FOREIGN KEY (owner_id) REFERENCES db_tenant_employee (emp_id),
    CONSTRAINT fk_fin_refund_project FOREIGN KEY (project_id) REFERENCES db_project (project_id),
    CONSTRAINT fk_fin_refund_receipt FOREIGN KEY (receipt_id) REFERENCES db_finance_receipt (receipt_id),
    CONSTRAINT fk_fin_refund_customer FOREIGN KEY (customer_id) REFERENCES cust_customer (cust_id),
    CONSTRAINT fk_fin_refund_creator FOREIGN KEY (creator_id) REFERENCES db_tenant_employee (emp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户退款';

ALTER TABLE db_payroll_commission_adjustment
    ADD COLUMN refund_id BIGINT NULL COMMENT '触发退款事实' AFTER original_commission_id,
    ADD UNIQUE KEY uk_payroll_adjustment_commission_refund (com_id, original_commission_id, refund_id),
    ADD CONSTRAINT fk_payroll_adjustment_refund FOREIGN KEY (refund_id) REFERENCES db_finance_refund (refund_id);

-- 回滚（仅在确认不保留退款事实时执行）：
-- ALTER TABLE db_payroll_commission_adjustment DROP FOREIGN KEY fk_payroll_adjustment_refund;
-- ALTER TABLE db_payroll_commission_adjustment DROP INDEX uk_payroll_adjustment_commission_refund,
--   DROP COLUMN refund_id;
-- DROP TABLE IF EXISTS db_finance_refund;
-- ALTER TABLE db_finance_ledger MODIFY COLUMN source ENUM('Adjustment','Manual','Payment','Receipt') NOT NULL;
