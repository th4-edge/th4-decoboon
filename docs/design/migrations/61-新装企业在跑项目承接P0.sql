-- 新装企业在跑项目承接 P0（DESIGN-CARRYOVER-20260926-001）
--
-- 范围：
-- 1) 新增 carryover 领域，承接在跑项目、确认记录、成本快照、调整记录和附件。
-- 2) 为报价、合同、项目、应收、应付增加业务来源，存量统一回填 New。
-- 3) 不迁移已完工项目、历史已结清流水或任意原表。
--
-- 适用：MySQL 8.0+。执行前备份；生产执行前必须完成结构审计和隔离库演练。

-- 1. 现有业务对象增加来源标记
ALTER TABLE quote
    ADD COLUMN business_origin VARCHAR(16) NOT NULL DEFAULT 'New' COMMENT '业务来源：New/Carryover' AFTER com_id;

ALTER TABLE db_project_contract
    ADD COLUMN business_origin VARCHAR(16) NOT NULL DEFAULT 'New' COMMENT '业务来源：New/Carryover' AFTER com_id;

ALTER TABLE db_project
    ADD COLUMN business_origin VARCHAR(16) NOT NULL DEFAULT 'New' COMMENT '业务来源：New/Carryover' AFTER com_id;

ALTER TABLE db_finance_receivable
    ADD COLUMN business_origin VARCHAR(16) NOT NULL DEFAULT 'New' COMMENT '业务来源：New/Carryover' AFTER com_id;

ALTER TABLE db_finance_payable
    ADD COLUMN business_origin VARCHAR(16) NOT NULL DEFAULT 'New' COMMENT '业务来源：New/Carryover' AFTER com_id;

CREATE INDEX idx_quote_company_origin ON quote (com_id, business_origin, quote_id);
CREATE INDEX idx_contract_company_origin ON db_project_contract (com_id, business_origin, contract_id);
CREATE INDEX idx_project_company_origin ON db_project (com_id, business_origin, project_id);
CREATE INDEX idx_fin_receivable_company_origin ON db_finance_receivable (com_id, business_origin, receivable_id);
CREATE INDEX idx_fin_payable_company_origin ON db_finance_payable (com_id, business_origin, payable_id);

-- 2. 承接任务
CREATE TABLE IF NOT EXISTS carryover_batch (
    batch_id BIGINT NOT NULL COMMENT '承接任务索引',
    com_id BIGINT NOT NULL COMMENT '所属企业',
    created_by BIGINT NOT NULL COMMENT '发起人',
    as_of_date DATE NOT NULL COMMENT '接管日期',
    status VARCHAR(16) NOT NULL COMMENT 'Active/Completed/Cancelled',
    scope_confirmed_at DATETIME(6) NULL COMMENT '范围确认时间',
    completed_at DATETIME(6) NULL COMMENT '完成时间',
    version INT NOT NULL DEFAULT 1 COMMENT '任务版本',
    create_date INT NULL, create_date_time BIGINT NULL, create_day INT NULL, create_hour INT NULL,
    create_minute INT NULL, create_month INT NULL, create_second INT NULL, create_time INT NULL,
    create_week TINYINT NULL, create_year INT NULL,
    update_date INT NULL, update_date_time BIGINT NULL, update_day INT NULL, update_hour INT NULL,
    update_minute INT NULL, update_month INT NULL, update_second INT NULL, update_time INT NULL,
    update_week TINYINT NULL, update_year INT NULL,
    PRIMARY KEY (batch_id),
    KEY idx_carryover_batch_company (com_id, batch_id),
    KEY idx_carryover_batch_company_status (com_id, status, batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='在跑项目承接任务';

-- 3. 单项目承接
CREATE TABLE IF NOT EXISTS carryover_project (
    item_id BIGINT NOT NULL COMMENT '承接项目索引',
    com_id BIGINT NOT NULL COMMENT '所属企业',
    store_id BIGINT NULL COMMENT '所属门店',
    team_id BIGINT NULL COMMENT '所属团队',
    owner_id BIGINT NULL COMMENT '经营负责人',
    batch_id BIGINT NOT NULL COMMENT '所属承接任务',
    status VARCHAR(16) NOT NULL COMMENT 'Draft/Ready/Confirmed/Applied/Locked/Cancelled',
    version INT NOT NULL DEFAULT 1 COMMENT '草稿版本',
    customer_name VARCHAR(64) NULL COMMENT '客户姓名',
    customer_phone VARCHAR(32) NULL COMMENT '客户手机号',
    community VARCHAR(128) NULL COMMENT '小区',
    address VARCHAR(256) NULL COMMENT '房屋地址',
    area DECIMAL(10,2) NULL COMMENT '面积',
    manager_id BIGINT NULL COMMENT '项目经理',
    designer_id BIGINT NULL COMMENT '设计师',
    contract_no VARCHAR(64) NULL COMMENT '合同编号',
    signed_amount DECIMAL(14,2) NULL COMMENT '原始签约金额',
    current_amount DECIMAL(14,2) NULL COMMENT '当前合同金额',
    signed_at DATE NULL COMMENT '签约日期',
    start_date DATE NULL COMMENT '开工日期',
    plan_end_date DATE NULL COMMENT '计划完工日期',
    project_name VARCHAR(128) NULL COMMENT '项目名称',
    project_status VARCHAR(16) NULL COMMENT '映射后的项目状态',
    current_stage_name VARCHAR(128) NULL COMMENT '当前施工阶段',
    progress INT NOT NULL DEFAULT 0 COMMENT '当前进度',
    remaining_work VARCHAR(1000) NULL COMMENT '剩余工作摘要',
    current_issue VARCHAR(1000) NULL COMMENT '当前问题摘要',
    receivable_outstanding DECIMAL(14,2) NULL COMMENT '期初未收金额',
    receivable_due_date DATE NULL COMMENT '期初应收到期日',
    receivable_source_no VARCHAR(64) NULL COMMENT '期初应收原单号',
    receivable_confirmed BIT(1) NOT NULL DEFAULT b'0' COMMENT '期初应收已确认',
    payable_outstanding DECIMAL(14,2) NULL COMMENT '期初未付金额',
    payable_due_date DATE NULL COMMENT '期初应付到期日',
    payable_payee_name VARCHAR(128) NULL COMMENT '期初应付收款方',
    payable_source_no VARCHAR(64) NULL COMMENT '期初应付原单号',
    payable_confirmed BIT(1) NOT NULL DEFAULT b'0' COMMENT '期初应付已确认',
    cost_total DECIMAL(14,2) NULL COMMENT '接管前成本总额',
    cost_status VARCHAR(16) NOT NULL DEFAULT 'Unknown' COMMENT 'Complete/TotalOnly/Unknown',
    cost_missing_note VARCHAR(500) NULL COMMENT '接管前成本缺失说明',
    legacy_id VARCHAR(128) NULL COMMENT '原系统项目 ID',
    legacy_project_no VARCHAR(64) NULL COMMENT '原系统项目编号',
    quote_id BIGINT NULL COMMENT '接管基线报价',
    contract_id BIGINT NULL COMMENT '合同基线',
    project_id BIGINT NULL COMMENT '正式项目',
    receivable_id BIGINT NULL COMMENT '期初应收',
    payable_id BIGINT NULL COMMENT '期初应付',
    applied_at DATETIME(6) NULL COMMENT '承接完成时间',
    cancelled_at DATETIME(6) NULL COMMENT '取消时间',
    cancel_reason VARCHAR(500) NULL COMMENT '取消原因',
    locked_at DATETIME(6) NULL COMMENT '锁定时间',
    first_business_at DATETIME(6) NULL COMMENT '首笔业务发生时间',
    first_business_type VARCHAR(24) NULL COMMENT '首笔业务类型',
    locked_by BIGINT NULL COMMENT '锁定操作人',
    create_date INT NULL, create_date_time BIGINT NULL, create_day INT NULL, create_hour INT NULL,
    create_minute INT NULL, create_month INT NULL, create_second INT NULL, create_time INT NULL,
    create_week TINYINT NULL, create_year INT NULL,
    update_date INT NULL, update_date_time BIGINT NULL, update_day INT NULL, update_hour INT NULL,
    update_minute INT NULL, update_month INT NULL, update_second INT NULL, update_time INT NULL,
    update_week TINYINT NULL, update_year INT NULL,
    PRIMARY KEY (item_id),
    KEY idx_carryover_project_company (com_id, item_id),
    KEY idx_carryover_project_batch (com_id, batch_id, item_id),
    KEY idx_carryover_project_status (com_id, status, item_id),
    KEY idx_carryover_project_store (com_id, store_id, item_id),
    KEY idx_carryover_project_team (com_id, team_id, item_id),
    KEY idx_carryover_project_owner (com_id, owner_id, item_id),
    KEY idx_carryover_project_quote (quote_id),
    KEY idx_carryover_project_contract (contract_id),
    KEY idx_carryover_project_result (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单项目业务承接';

-- 4. 分角色确认
CREATE TABLE IF NOT EXISTS carryover_confirmation (
    confirmation_id BIGINT NOT NULL COMMENT '确认记录索引',
    com_id BIGINT NOT NULL COMMENT '所属企业',
    item_id BIGINT NOT NULL COMMENT '所属承接项目',
    required_role VARCHAR(16) NOT NULL COMMENT 'Boss/Finance/Pm/Designer',
    confirmed_by BIGINT NOT NULL COMMENT '实际确认人',
    proxy BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否代确认',
    confirmed_version INT NOT NULL COMMENT '确认的草稿版本',
    confirmed_at DATETIME(6) NOT NULL COMMENT '确认时间',
    summary VARCHAR(500) NULL COMMENT '确认摘要',
    create_date INT NULL, create_date_time BIGINT NULL, create_day INT NULL, create_hour INT NULL,
    create_minute INT NULL, create_month INT NULL, create_second INT NULL, create_time INT NULL,
    create_week TINYINT NULL, create_year INT NULL,
    update_date INT NULL, update_date_time BIGINT NULL, update_day INT NULL, update_hour INT NULL,
    update_minute INT NULL, update_month INT NULL, update_second INT NULL, update_time INT NULL,
    update_week TINYINT NULL, update_year INT NULL,
    PRIMARY KEY (confirmation_id),
    UNIQUE KEY uk_carryover_confirmation_role_version (com_id, item_id, required_role, confirmed_version),
    KEY idx_carryover_confirmation_item (com_id, item_id, required_role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='承接项目确认记录';

-- 5. 接管前成本快照
CREATE TABLE IF NOT EXISTS carryover_cost_snapshot (
    snapshot_id BIGINT NOT NULL COMMENT '成本快照索引',
    com_id BIGINT NOT NULL COMMENT '所属企业',
    item_id BIGINT NOT NULL COMMENT '所属承接项目',
    project_id BIGINT NOT NULL COMMENT '正式项目',
    total_cost DECIMAL(14,2) NULL COMMENT '接管前成本总额',
    cost_status VARCHAR(16) NOT NULL COMMENT 'Complete/TotalOnly/Unknown',
    as_of_date DATE NOT NULL COMMENT '成本截止日',
    missing_note VARCHAR(500) NULL COMMENT '缺失说明',
    confirmed_by BIGINT NULL COMMENT '确认人',
    confirmed_at DATETIME(6) NULL COMMENT '确认时间',
    create_date INT NULL, create_date_time BIGINT NULL, create_day INT NULL, create_hour INT NULL,
    create_minute INT NULL, create_month INT NULL, create_second INT NULL, create_time INT NULL,
    create_week TINYINT NULL, create_year INT NULL,
    update_date INT NULL, update_date_time BIGINT NULL, update_day INT NULL, update_hour INT NULL,
    update_minute INT NULL, update_month INT NULL, update_second INT NULL, update_time INT NULL,
    update_week TINYINT NULL, update_year INT NULL,
    PRIMARY KEY (snapshot_id),
    UNIQUE KEY uk_carryover_cost_item (item_id),
    UNIQUE KEY uk_carryover_cost_project (project_id),
    KEY idx_carryover_cost_item (com_id, item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接管前成本快照';

-- 6. 锁定后调整记录
CREATE TABLE IF NOT EXISTS carryover_adjustment (
    adjustment_id BIGINT NOT NULL COMMENT '调整索引',
    com_id BIGINT NOT NULL COMMENT '所属企业',
    item_id BIGINT NOT NULL COMMENT '所属承接项目',
    type VARCHAR(16) NOT NULL COMMENT 'Receivable/Payable/Cost/Other',
    before_amount DECIMAL(14,2) NULL COMMENT '调整前金额',
    delta_amount DECIMAL(14,2) NOT NULL COMMENT '调整差额',
    after_amount DECIMAL(14,2) NULL COMMENT '调整后金额',
    reason VARCHAR(500) NOT NULL COMMENT '调整原因',
    created_by BIGINT NOT NULL COMMENT '操作人',
    create_date INT NULL, create_date_time BIGINT NULL, create_day INT NULL, create_hour INT NULL,
    create_minute INT NULL, create_month INT NULL, create_second INT NULL, create_time INT NULL,
    create_week TINYINT NULL, create_year INT NULL,
    update_date INT NULL, update_date_time BIGINT NULL, update_day INT NULL, update_hour INT NULL,
    update_minute INT NULL, update_month INT NULL, update_second INT NULL, update_time INT NULL,
    update_week TINYINT NULL, update_year INT NULL,
    PRIMARY KEY (adjustment_id),
    KEY idx_carryover_adjustment_item (com_id, item_id, adjustment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='承接调整记录';

-- 7. 附件
CREATE TABLE IF NOT EXISTS carryover_attachment (
    attachment_id BIGINT NOT NULL COMMENT '附件索引',
    com_id BIGINT NOT NULL COMMENT '所属企业',
    item_id BIGINT NOT NULL COMMENT '所属承接项目',
    category VARCHAR(16) NOT NULL COMMENT 'Contract/Quote/Site/Finance/Other',
    file_url VARCHAR(512) NOT NULL COMMENT '文件地址',
    original_name VARCHAR(256) NULL COMMENT '原文件名',
    uploaded_by BIGINT NOT NULL COMMENT '上传人',
    create_date INT NULL, create_date_time BIGINT NULL, create_day INT NULL, create_hour INT NULL,
    create_minute INT NULL, create_month INT NULL, create_second INT NULL, create_time INT NULL,
    create_week TINYINT NULL, create_year INT NULL,
    update_date INT NULL, update_date_time BIGINT NULL, update_day INT NULL, update_hour INT NULL,
    update_minute INT NULL, update_month INT NULL, update_second INT NULL, update_time INT NULL,
    update_week TINYINT NULL, update_year INT NULL,
    PRIMARY KEY (attachment_id),
    KEY idx_carryover_attachment_item (com_id, item_id, attachment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='承接项目附件';

-- 8. 外键
ALTER TABLE carryover_batch
    ADD CONSTRAINT fk_carryover_batch_company FOREIGN KEY (com_id) REFERENCES db_tenant_company (com_id),
    ADD CONSTRAINT fk_carryover_batch_creator FOREIGN KEY (created_by) REFERENCES db_tenant_employee (emp_id);

ALTER TABLE carryover_project
    ADD CONSTRAINT fk_carryover_project_company FOREIGN KEY (com_id) REFERENCES db_tenant_company (com_id),
    ADD CONSTRAINT fk_carryover_project_store FOREIGN KEY (store_id) REFERENCES db_tenant_store (store_id),
    ADD CONSTRAINT fk_carryover_project_team FOREIGN KEY (team_id) REFERENCES db_tenant_team (team_id),
    ADD CONSTRAINT fk_carryover_project_owner FOREIGN KEY (owner_id) REFERENCES db_tenant_employee (emp_id),
    ADD CONSTRAINT fk_carryover_project_batch FOREIGN KEY (batch_id) REFERENCES carryover_batch (batch_id),
    ADD CONSTRAINT fk_carryover_project_manager FOREIGN KEY (manager_id) REFERENCES db_tenant_employee (emp_id),
    ADD CONSTRAINT fk_carryover_project_designer FOREIGN KEY (designer_id) REFERENCES db_tenant_employee (emp_id),
    ADD CONSTRAINT fk_carryover_project_quote FOREIGN KEY (quote_id) REFERENCES quote (quote_id),
    ADD CONSTRAINT fk_carryover_project_contract FOREIGN KEY (contract_id) REFERENCES db_project_contract (contract_id),
    ADD CONSTRAINT fk_carryover_project_result FOREIGN KEY (project_id) REFERENCES db_project (project_id),
    ADD CONSTRAINT fk_carryover_project_receivable FOREIGN KEY (receivable_id) REFERENCES db_finance_receivable (receivable_id),
    ADD CONSTRAINT fk_carryover_project_payable FOREIGN KEY (payable_id) REFERENCES db_finance_payable (payable_id),
    ADD CONSTRAINT fk_carryover_project_locked_by FOREIGN KEY (locked_by) REFERENCES db_tenant_employee (emp_id);

ALTER TABLE carryover_confirmation
    ADD CONSTRAINT fk_carryover_confirmation_company FOREIGN KEY (com_id) REFERENCES db_tenant_company (com_id),
    ADD CONSTRAINT fk_carryover_confirmation_item FOREIGN KEY (item_id) REFERENCES carryover_project (item_id),
    ADD CONSTRAINT fk_carryover_confirmation_employee FOREIGN KEY (confirmed_by) REFERENCES db_tenant_employee (emp_id);

ALTER TABLE carryover_cost_snapshot
    ADD CONSTRAINT fk_carryover_cost_company FOREIGN KEY (com_id) REFERENCES db_tenant_company (com_id),
    ADD CONSTRAINT fk_carryover_cost_item FOREIGN KEY (item_id) REFERENCES carryover_project (item_id),
    ADD CONSTRAINT fk_carryover_cost_project FOREIGN KEY (project_id) REFERENCES db_project (project_id),
    ADD CONSTRAINT fk_carryover_cost_employee FOREIGN KEY (confirmed_by) REFERENCES db_tenant_employee (emp_id);

ALTER TABLE carryover_adjustment
    ADD CONSTRAINT fk_carryover_adjustment_company FOREIGN KEY (com_id) REFERENCES db_tenant_company (com_id),
    ADD CONSTRAINT fk_carryover_adjustment_item FOREIGN KEY (item_id) REFERENCES carryover_project (item_id),
    ADD CONSTRAINT fk_carryover_adjustment_employee FOREIGN KEY (created_by) REFERENCES db_tenant_employee (emp_id);

ALTER TABLE carryover_attachment
    ADD CONSTRAINT fk_carryover_attachment_company FOREIGN KEY (com_id) REFERENCES db_tenant_company (com_id),
    ADD CONSTRAINT fk_carryover_attachment_item FOREIGN KEY (item_id) REFERENCES carryover_project (item_id),
    ADD CONSTRAINT fk_carryover_attachment_employee FOREIGN KEY (uploaded_by) REFERENCES db_tenant_employee (emp_id);

-- 回滚（仅在确认无承接业务数据后手工执行）
-- ALTER TABLE carryover_attachment DROP FOREIGN KEY fk_carryover_attachment_employee;
-- ALTER TABLE carryover_attachment DROP FOREIGN KEY fk_carryover_attachment_item;
-- ALTER TABLE carryover_attachment DROP FOREIGN KEY fk_carryover_attachment_company;
-- ALTER TABLE carryover_adjustment DROP FOREIGN KEY fk_carryover_adjustment_employee;
-- ALTER TABLE carryover_adjustment DROP FOREIGN KEY fk_carryover_adjustment_item;
-- ALTER TABLE carryover_adjustment DROP FOREIGN KEY fk_carryover_adjustment_company;
-- ALTER TABLE carryover_cost_snapshot DROP FOREIGN KEY fk_carryover_cost_employee;
-- ALTER TABLE carryover_cost_snapshot DROP FOREIGN KEY fk_carryover_cost_project;
-- ALTER TABLE carryover_cost_snapshot DROP FOREIGN KEY fk_carryover_cost_item;
-- ALTER TABLE carryover_cost_snapshot DROP FOREIGN KEY fk_carryover_cost_company;
-- ALTER TABLE carryover_confirmation DROP FOREIGN KEY fk_carryover_confirmation_employee;
-- ALTER TABLE carryover_confirmation DROP FOREIGN KEY fk_carryover_confirmation_item;
-- ALTER TABLE carryover_confirmation DROP FOREIGN KEY fk_carryover_confirmation_company;
-- ALTER TABLE carryover_project DROP FOREIGN KEY fk_carryover_project_locked_by;
-- ALTER TABLE carryover_project DROP FOREIGN KEY fk_carryover_project_payable;
-- ALTER TABLE carryover_project DROP FOREIGN KEY fk_carryover_project_receivable;
-- ALTER TABLE carryover_project DROP FOREIGN KEY fk_carryover_project_result;
-- ALTER TABLE carryover_project DROP FOREIGN KEY fk_carryover_project_contract;
-- ALTER TABLE carryover_project DROP FOREIGN KEY fk_carryover_project_quote;
-- ALTER TABLE carryover_project DROP FOREIGN KEY fk_carryover_project_designer;
-- ALTER TABLE carryover_project DROP FOREIGN KEY fk_carryover_project_manager;
-- ALTER TABLE carryover_project DROP FOREIGN KEY fk_carryover_project_batch;
-- ALTER TABLE carryover_project DROP FOREIGN KEY fk_carryover_project_owner;
-- ALTER TABLE carryover_project DROP FOREIGN KEY fk_carryover_project_team;
-- ALTER TABLE carryover_project DROP FOREIGN KEY fk_carryover_project_store;
-- ALTER TABLE carryover_project DROP FOREIGN KEY fk_carryover_project_company;
-- ALTER TABLE carryover_batch DROP FOREIGN KEY fk_carryover_batch_creator;
-- ALTER TABLE carryover_batch DROP FOREIGN KEY fk_carryover_batch_company;
-- DROP TABLE IF EXISTS carryover_attachment;
-- DROP TABLE IF EXISTS carryover_adjustment;
-- DROP TABLE IF EXISTS carryover_cost_snapshot;
-- DROP TABLE IF EXISTS carryover_confirmation;
-- DROP TABLE IF EXISTS carryover_project;
-- DROP TABLE IF EXISTS carryover_batch;
-- DROP INDEX idx_fin_payable_company_origin ON db_finance_payable;
-- DROP INDEX idx_fin_receivable_company_origin ON db_finance_receivable;
-- DROP INDEX idx_project_company_origin ON db_project;
-- DROP INDEX idx_contract_company_origin ON db_project_contract;
-- DROP INDEX idx_quote_company_origin ON quote;
-- ALTER TABLE db_finance_payable DROP COLUMN business_origin;
-- ALTER TABLE db_finance_receivable DROP COLUMN business_origin;
-- ALTER TABLE db_project DROP COLUMN business_origin;
-- ALTER TABLE db_project_contract DROP COLUMN business_origin;
-- ALTER TABLE quote DROP COLUMN business_origin;
