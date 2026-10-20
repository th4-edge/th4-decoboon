-- 122-收款冲正.sql
-- 目的：错误收款不删除原单，通过独立冲正事实回退应收、资金台账和未发放提成。
-- 适用：MySQL 8.0+。生产执行前必须备份并登记迁移账本。

ALTER TABLE db_finance_receipt
    ADD COLUMN status ENUM('Active','Reversed') NOT NULL DEFAULT 'Active' COMMENT '收款状态' AFTER idempotency_fingerprint,
    ADD COLUMN reversed_at DATETIME(6) NULL COMMENT '冲正时间' AFTER status,
    ADD COLUMN reversed_by_id BIGINT NULL COMMENT '冲正操作人' AFTER reversed_at,
    ADD COLUMN reversal_reason VARCHAR(500) NULL COMMENT '冲正原因快照' AFTER reversed_by_id,
    ADD KEY idx_fin_receipt_company_status (com_id, status, receipt_id),
    ADD CONSTRAINT fk_fin_receipt_reversed_by FOREIGN KEY (reversed_by_id) REFERENCES db_tenant_employee (emp_id);

ALTER TABLE db_finance_ledger
    MODIFY COLUMN source ENUM('Adjustment','Manual','Payment','Receipt','Refund','ReceiptReversal') NOT NULL;

CREATE TABLE IF NOT EXISTS db_finance_receipt_reversal (
    reversal_id BIGINT NOT NULL,
    com_id BIGINT NOT NULL,
    store_id BIGINT NULL,
    team_id BIGINT NULL,
    owner_id BIGINT NOT NULL,
    receipt_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    receivable_id BIGINT NOT NULL,
    amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    reversal_at DATE NOT NULL,
    reason_type ENUM('NotReceived','AmountError','ReceivableError','Duplicate','AccountOrDateError','Other') NOT NULL,
    reason VARCHAR(500) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    idempotency_fingerprint VARCHAR(64) NOT NULL,
    reversal_ledger_id BIGINT NULL,
    replacement_receipt_id BIGINT NULL,
    operator_id BIGINT NOT NULL,
    before_snapshot LONGTEXT NOT NULL,
    after_snapshot LONGTEXT NOT NULL,
    create_date_time BIGINT NULL, create_date INT NULL, create_time INT NULL, create_year INT NULL,
    create_month INT NULL, create_day INT NULL, create_hour INT NULL, create_minute INT NULL,
    create_second INT NULL, create_week TINYINT NULL,
    update_date_time BIGINT NULL, update_date INT NULL, update_time INT NULL, update_year INT NULL,
    update_month INT NULL, update_day INT NULL, update_hour INT NULL, update_minute INT NULL,
    update_second INT NULL, update_week TINYINT NULL,
    PRIMARY KEY (reversal_id),
    UNIQUE KEY uk_fin_receipt_reversal_receipt (receipt_id),
    UNIQUE KEY uk_fin_receipt_reversal_idempotency (com_id, idempotency_key),
    KEY idx_fin_receipt_reversal_company (com_id, reversal_id),
    KEY idx_fin_receipt_reversal_project (com_id, project_id, reversal_id),
    KEY idx_fin_receipt_reversal_store (com_id, store_id, reversal_id),
    KEY idx_fin_receipt_reversal_date (com_id, reversal_at, reversal_id),
    CONSTRAINT fk_fin_receipt_reversal_company FOREIGN KEY (com_id) REFERENCES db_tenant_company (com_id),
    CONSTRAINT fk_fin_receipt_reversal_store FOREIGN KEY (store_id) REFERENCES db_tenant_store (store_id),
    CONSTRAINT fk_fin_receipt_reversal_team FOREIGN KEY (team_id) REFERENCES db_tenant_team (team_id),
    CONSTRAINT fk_fin_receipt_reversal_owner FOREIGN KEY (owner_id) REFERENCES db_tenant_employee (emp_id),
    CONSTRAINT fk_fin_receipt_reversal_receipt FOREIGN KEY (receipt_id) REFERENCES db_finance_receipt (receipt_id),
    CONSTRAINT fk_fin_receipt_reversal_project FOREIGN KEY (project_id) REFERENCES db_project (project_id),
    CONSTRAINT fk_fin_receipt_reversal_customer FOREIGN KEY (customer_id) REFERENCES cust_customer (cust_id),
    CONSTRAINT fk_fin_receipt_reversal_receivable FOREIGN KEY (receivable_id) REFERENCES db_finance_receivable (receivable_id),
    CONSTRAINT fk_fin_receipt_reversal_ledger FOREIGN KEY (reversal_ledger_id) REFERENCES db_finance_ledger (ledger_id),
    CONSTRAINT fk_fin_receipt_reversal_operator FOREIGN KEY (operator_id) REFERENCES db_tenant_employee (emp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收款冲正事实';

ALTER TABLE db_payroll_commission_release
    ADD COLUMN reversed BIT NOT NULL DEFAULT 0 COMMENT '是否已因收款冲正反释放' AFTER remark,
    ADD COLUMN reversed_at DATETIME(6) NULL COMMENT '反释放时间' AFTER reversed,
    ADD COLUMN reversed_by_id BIGINT NULL COMMENT '反释放操作人' AFTER reversed_at,
    ADD COLUMN reverse_reason VARCHAR(500) NULL COMMENT '反释放原因' AFTER reversed_by_id,
    ADD KEY idx_payroll_release_receipt_reversed (com_id, receipt_id, reversed, release_id),
    ADD CONSTRAINT fk_payroll_release_reversed_by FOREIGN KEY (reversed_by_id) REFERENCES db_tenant_employee (emp_id);
