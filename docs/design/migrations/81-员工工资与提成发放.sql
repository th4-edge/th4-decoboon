-- 81-员工工资与提成发放.sql
-- 目的：增加员工薪酬、月度考勤、提成计提/释放、工资单、工资条和员工发放所需的领域表与财务扩展。
-- 边界：员工工资与工人工资分链；不复用 PayableType.Salary；员工发放复用 Payable/Payment/Ledger 主干。
-- 适用：MySQL 8.0+。生产执行前必须备份并登记迁移账本；回滚脚本见文件末尾注释。
-- 说明：员工工资条只有实际发放完成后才对本人员工可见（EV-069）。

-- 1. 现有应付：允许企业归属员工工资没有房屋；类型列扩为 VARCHAR 以承载员工工资/提成。
ALTER TABLE db_finance_payable
    MODIFY COLUMN property_id BIGINT NULL COMMENT '所属房屋；企业归属员工工资可为空',
    MODIFY COLUMN type VARCHAR(32) NOT NULL COMMENT '应付类型；含 EmployeeSalary/EmployeeCommission';

ALTER TABLE db_finance_payable
    ADD COLUMN source_type VARCHAR(32) NULL COMMENT '来源类型：PayrollItem/CommissionItem' AFTER source_no,
    ADD COLUMN source_id BIGINT NULL COMMENT '来源业务主键' AFTER source_type;

CREATE UNIQUE INDEX uk_fin_payable_company_source
    ON db_finance_payable (com_id, source_type, source_id);

-- 2. 现有付款：增加员工收款人与员工发放批次。
ALTER TABLE db_finance_payment
    ADD COLUMN payee_emp_id BIGINT NULL COMMENT '员工收款人' AFTER worker_id,
    ADD COLUMN batch_no VARCHAR(64) NULL COMMENT '员工发放批次号' AFTER payee_emp_id;

CREATE INDEX idx_fin_payment_company_employee
    ON db_finance_payment (com_id, payee_emp_id, paid_at, payment_id);
CREATE INDEX idx_fin_payment_company_batch
    ON db_finance_payment (com_id, batch_no, payment_id);

-- 3. 项目结算：员工工资和员工提成独立于工人工资。
ALTER TABLE db_finance_settlement
    ADD COLUMN employee_salary_amount DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '项目归属员工工资' AFTER salary_amount,
    ADD COLUMN employee_commission_amount DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '项目归属员工提成' AFTER employee_salary_amount;

-- 4. 薪酬方案。
CREATE TABLE IF NOT EXISTS db_payroll_compensation_scheme (
    scheme_id BIGINT NOT NULL,
    com_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    team_id BIGINT NULL,
    emp_id BIGINT NOT NULL,
    base_salary DECIMAL(14,2) NOT NULL DEFAULT 0,
    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    status VARCHAR(16) NOT NULL,
    remark VARCHAR(500) NULL,
    creator_id BIGINT NOT NULL,
    create_date_time BIGINT NULL, create_date INT NULL, create_time INT NULL, create_year INT NULL,
    create_month INT NULL, create_day INT NULL, create_hour INT NULL, create_minute INT NULL,
    create_second INT NULL, create_week TINYINT NULL,
    update_date_time BIGINT NULL, update_date INT NULL, update_time INT NULL, update_year INT NULL,
    update_month INT NULL, update_day INT NULL, update_hour INT NULL, update_minute INT NULL,
    update_second INT NULL, update_week TINYINT NULL,
    PRIMARY KEY (scheme_id),
    UNIQUE KEY uk_payroll_comp_employee_effective (com_id, emp_id, effective_from),
    KEY idx_payroll_comp_company (com_id, scheme_id),
    KEY idx_payroll_comp_employee (com_id, emp_id, scheme_id),
    KEY idx_payroll_comp_store (com_id, store_id, scheme_id),
    KEY idx_payroll_comp_team (com_id, team_id, scheme_id),
    CONSTRAINT fk_payroll_comp_company FOREIGN KEY (com_id) REFERENCES db_tenant_company (com_id),
    CONSTRAINT fk_payroll_comp_store FOREIGN KEY (store_id) REFERENCES db_tenant_store (store_id),
    CONSTRAINT fk_payroll_comp_team FOREIGN KEY (team_id) REFERENCES db_tenant_team (team_id),
    CONSTRAINT fk_payroll_comp_employee FOREIGN KEY (emp_id) REFERENCES db_tenant_employee (emp_id),
    CONSTRAINT fk_payroll_comp_creator FOREIGN KEY (creator_id) REFERENCES db_tenant_employee (emp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工薪酬方案';

-- 5. 月度考勤汇总。
CREATE TABLE IF NOT EXISTS db_payroll_attendance_summary (
    summary_id BIGINT NOT NULL,
    com_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    team_id BIGINT NULL,
    emp_id BIGINT NOT NULL,
    payroll_month VARCHAR(7) NOT NULL,
    scheduled_days INT NOT NULL DEFAULT 0,
    normal_days INT NOT NULL DEFAULT 0,
    personal_leave_days INT NOT NULL DEFAULT 0,
    sick_leave_days INT NOT NULL DEFAULT 0,
    absence_days INT NOT NULL DEFAULT 0,
    external_days INT NOT NULL DEFAULT 0,
    annual_leave_days INT NOT NULL DEFAULT 0,
    comp_leave_days INT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL,
    confirmed_by BIGINT NULL,
    confirmed_at DATETIME(6) NULL,
    remark VARCHAR(500) NULL,
    creator_id BIGINT NOT NULL,
    create_date_time BIGINT NULL, create_date INT NULL, create_time INT NULL, create_year INT NULL,
    create_month INT NULL, create_day INT NULL, create_hour INT NULL, create_minute INT NULL,
    create_second INT NULL, create_week TINYINT NULL,
    update_date_time BIGINT NULL, update_date INT NULL, update_time INT NULL, update_year INT NULL,
    update_month INT NULL, update_day INT NULL, update_hour INT NULL, update_minute INT NULL,
    update_second INT NULL, update_week TINYINT NULL,
    PRIMARY KEY (summary_id),
    UNIQUE KEY uk_payroll_attendance_employee_month (com_id, emp_id, payroll_month),
    KEY idx_payroll_attendance_company (com_id, summary_id),
    KEY idx_payroll_attendance_employee (com_id, emp_id, payroll_month, summary_id),
    KEY idx_payroll_attendance_store (com_id, store_id, payroll_month, summary_id),
    KEY idx_payroll_attendance_team (com_id, team_id, payroll_month, summary_id),
    CONSTRAINT fk_payroll_attendance_company FOREIGN KEY (com_id) REFERENCES db_tenant_company (com_id),
    CONSTRAINT fk_payroll_attendance_store FOREIGN KEY (store_id) REFERENCES db_tenant_store (store_id),
    CONSTRAINT fk_payroll_attendance_team FOREIGN KEY (team_id) REFERENCES db_tenant_team (team_id),
    CONSTRAINT fk_payroll_attendance_employee FOREIGN KEY (emp_id) REFERENCES db_tenant_employee (emp_id),
    CONSTRAINT fk_payroll_attendance_creator FOREIGN KEY (creator_id) REFERENCES db_tenant_employee (emp_id),
    CONSTRAINT fk_payroll_attendance_confirmed FOREIGN KEY (confirmed_by) REFERENCES db_tenant_employee (emp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工月度考勤汇总';

-- 6. 提成明细与释放记录。
CREATE TABLE IF NOT EXISTS db_payroll_commission_item (
    commission_id BIGINT NOT NULL,
    com_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    team_id BIGINT NULL,
    emp_id BIGINT NOT NULL,
    project_id BIGINT NULL,
    payroll_item_id BIGINT NULL,
    contract_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    manual_rate DECIMAL(8,4) NOT NULL DEFAULT 0,
    commission_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    accrued_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    released_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    paid_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    status VARCHAR(24) NOT NULL,
    adjustment_of BIGINT NULL,
    remark VARCHAR(500) NULL,
    creator_id BIGINT NOT NULL,
    create_date_time BIGINT NULL, create_date INT NULL, create_time INT NULL, create_year INT NULL,
    create_month INT NULL, create_day INT NULL, create_hour INT NULL, create_minute INT NULL,
    create_second INT NULL, create_week TINYINT NULL,
    update_date_time BIGINT NULL, update_date INT NULL, update_time INT NULL, update_year INT NULL,
    update_month INT NULL, update_day INT NULL, update_hour INT NULL, update_minute INT NULL,
    update_second INT NULL, update_week TINYINT NULL,
    PRIMARY KEY (commission_id),
    KEY idx_payroll_commission_company (com_id, commission_id),
    KEY idx_payroll_commission_employee (com_id, emp_id, status, commission_id),
    KEY idx_payroll_commission_project (com_id, project_id, commission_id),
    KEY idx_payroll_commission_store (com_id, store_id, commission_id),
    CONSTRAINT fk_payroll_commission_company FOREIGN KEY (com_id) REFERENCES db_tenant_company (com_id),
    CONSTRAINT fk_payroll_commission_store FOREIGN KEY (store_id) REFERENCES db_tenant_store (store_id),
    CONSTRAINT fk_payroll_commission_team FOREIGN KEY (team_id) REFERENCES db_tenant_team (team_id),
    CONSTRAINT fk_payroll_commission_employee FOREIGN KEY (emp_id) REFERENCES db_tenant_employee (emp_id),
    CONSTRAINT fk_payroll_commission_project FOREIGN KEY (project_id) REFERENCES db_project (project_id),
    CONSTRAINT fk_payroll_commission_adjustment FOREIGN KEY (adjustment_of) REFERENCES db_payroll_commission_item (commission_id),
    CONSTRAINT fk_payroll_commission_creator FOREIGN KEY (creator_id) REFERENCES db_tenant_employee (emp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工提成明细';

CREATE TABLE IF NOT EXISTS db_payroll_commission_release (
    release_id BIGINT NOT NULL,
    com_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    team_id BIGINT NULL,
    emp_id BIGINT NOT NULL,
    commission_id BIGINT NOT NULL,
    receipt_id BIGINT NOT NULL,
    amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    released_at DATE NOT NULL,
    remark VARCHAR(500) NULL,
    creator_id BIGINT NOT NULL,
    create_date_time BIGINT NULL, create_date INT NULL, create_time INT NULL, create_year INT NULL,
    create_month INT NULL, create_day INT NULL, create_hour INT NULL, create_minute INT NULL,
    create_second INT NULL, create_week TINYINT NULL,
    update_date_time BIGINT NULL, update_date INT NULL, update_time INT NULL, update_year INT NULL,
    update_month INT NULL, update_day INT NULL, update_hour INT NULL, update_minute INT NULL,
    update_second INT NULL, update_week TINYINT NULL,
    PRIMARY KEY (release_id),
    UNIQUE KEY uk_payroll_release_commission_receipt (com_id, commission_id, receipt_id),
    KEY idx_payroll_release_company (com_id, release_id),
    KEY idx_payroll_release_commission (com_id, commission_id, release_id),
    KEY idx_payroll_release_employee (com_id, emp_id, release_id),
    KEY idx_payroll_release_receipt (com_id, receipt_id, release_id),
    CONSTRAINT fk_payroll_release_company FOREIGN KEY (com_id) REFERENCES db_tenant_company (com_id),
    CONSTRAINT fk_payroll_release_store FOREIGN KEY (store_id) REFERENCES db_tenant_store (store_id),
    CONSTRAINT fk_payroll_release_team FOREIGN KEY (team_id) REFERENCES db_tenant_team (team_id),
    CONSTRAINT fk_payroll_release_employee FOREIGN KEY (emp_id) REFERENCES db_tenant_employee (emp_id),
    CONSTRAINT fk_payroll_release_commission FOREIGN KEY (commission_id) REFERENCES db_payroll_commission_item (commission_id),
    CONSTRAINT fk_payroll_release_receipt FOREIGN KEY (receipt_id) REFERENCES db_finance_receipt (receipt_id),
    CONSTRAINT fk_payroll_release_creator FOREIGN KEY (creator_id) REFERENCES db_tenant_employee (emp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工提成回款释放记录';

-- 7. 工资单、工资项、工资条。
CREATE TABLE IF NOT EXISTS db_payroll_sheet (
    sheet_id BIGINT NOT NULL,
    com_id BIGINT NOT NULL,
    store_id BIGINT NULL,
    team_id BIGINT NULL,
    owner_id BIGINT NOT NULL,
    period VARCHAR(7) NOT NULL,
    status VARCHAR(24) NOT NULL,
    attendance_complete BIT NOT NULL DEFAULT 0,
    commission_complete BIT NOT NULL DEFAULT 0,
    total_gross_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    total_deduction_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    total_net_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    confirmed_by BIGINT NULL,
    confirmed_at DATETIME(6) NULL,
    remark VARCHAR(500) NULL,
    creator_id BIGINT NOT NULL,
    create_date_time BIGINT NULL, create_date INT NULL, create_time INT NULL, create_year INT NULL,
    create_month INT NULL, create_day INT NULL, create_hour INT NULL, create_minute INT NULL,
    create_second INT NULL, create_week TINYINT NULL,
    update_date_time BIGINT NULL, update_date INT NULL, update_time INT NULL, update_year INT NULL,
    update_month INT NULL, update_day INT NULL, update_hour INT NULL, update_minute INT NULL,
    update_second INT NULL, update_week TINYINT NULL,
    PRIMARY KEY (sheet_id),
    UNIQUE KEY uk_payroll_sheet_company_period (com_id, period),
    KEY idx_payroll_sheet_company (com_id, sheet_id),
    KEY idx_payroll_sheet_period (com_id, period, sheet_id),
    KEY idx_payroll_sheet_owner (com_id, owner_id, sheet_id),
    CONSTRAINT fk_payroll_sheet_company FOREIGN KEY (com_id) REFERENCES db_tenant_company (com_id),
    CONSTRAINT fk_payroll_sheet_store FOREIGN KEY (store_id) REFERENCES db_tenant_store (store_id),
    CONSTRAINT fk_payroll_sheet_team FOREIGN KEY (team_id) REFERENCES db_tenant_team (team_id),
    CONSTRAINT fk_payroll_sheet_owner FOREIGN KEY (owner_id) REFERENCES db_tenant_employee (emp_id),
    CONSTRAINT fk_payroll_sheet_creator FOREIGN KEY (creator_id) REFERENCES db_tenant_employee (emp_id),
    CONSTRAINT fk_payroll_sheet_confirmed FOREIGN KEY (confirmed_by) REFERENCES db_tenant_employee (emp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工工资单';

CREATE TABLE IF NOT EXISTS db_payroll_item (
    item_id BIGINT NOT NULL,
    com_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    team_id BIGINT NULL,
    emp_id BIGINT NOT NULL,
    sheet_id BIGINT NOT NULL,
    project_id BIGINT NULL,
    attribution VARCHAR(16) NOT NULL,
    attribution_key VARCHAR(64) NOT NULL,
    base_salary_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    attendance_deduction DECIMAL(14,2) NOT NULL DEFAULT 0,
    commission_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    bonus_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    allowance_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    social_insurance_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    tax_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    other_deduction DECIMAL(14,2) NOT NULL DEFAULT 0,
    gross_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    deduction_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    net_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL,
    remark VARCHAR(500) NULL,
    creator_id BIGINT NOT NULL,
    create_date_time BIGINT NULL, create_date INT NULL, create_time INT NULL, create_year INT NULL,
    create_month INT NULL, create_day INT NULL, create_hour INT NULL, create_minute INT NULL,
    create_second INT NULL, create_week TINYINT NULL,
    update_date_time BIGINT NULL, update_date INT NULL, update_time INT NULL, update_year INT NULL,
    update_month INT NULL, update_day INT NULL, update_hour INT NULL, update_minute INT NULL,
    update_second INT NULL, update_week TINYINT NULL,
    PRIMARY KEY (item_id),
    UNIQUE KEY uk_payroll_item_sheet_employee_attribution (com_id, sheet_id, emp_id, attribution_key),
    KEY idx_payroll_item_company (com_id, item_id),
    KEY idx_payroll_item_sheet (com_id, sheet_id, item_id),
    KEY idx_payroll_item_employee (com_id, emp_id, sheet_id, item_id),
    KEY idx_payroll_item_project (com_id, project_id, item_id),
    KEY idx_payroll_item_store (com_id, store_id, item_id),
    CONSTRAINT fk_payroll_item_company FOREIGN KEY (com_id) REFERENCES db_tenant_company (com_id),
    CONSTRAINT fk_payroll_item_store FOREIGN KEY (store_id) REFERENCES db_tenant_store (store_id),
    CONSTRAINT fk_payroll_item_team FOREIGN KEY (team_id) REFERENCES db_tenant_team (team_id),
    CONSTRAINT fk_payroll_item_employee FOREIGN KEY (emp_id) REFERENCES db_tenant_employee (emp_id),
    CONSTRAINT fk_payroll_item_sheet FOREIGN KEY (sheet_id) REFERENCES db_payroll_sheet (sheet_id),
    CONSTRAINT fk_payroll_item_project FOREIGN KEY (project_id) REFERENCES db_project (project_id),
    CONSTRAINT fk_payroll_item_creator FOREIGN KEY (creator_id) REFERENCES db_tenant_employee (emp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工工资项';

CREATE TABLE IF NOT EXISTS db_payroll_payslip (
    payslip_id BIGINT NOT NULL,
    com_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    team_id BIGINT NULL,
    emp_id BIGINT NOT NULL,
    sheet_id BIGINT NOT NULL,
    period VARCHAR(7) NOT NULL,
    gross_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    deduction_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    net_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
    status VARCHAR(24) NOT NULL,
    published_at DATETIME(6) NULL,
    creator_id BIGINT NOT NULL,
    create_date_time BIGINT NULL, create_date INT NULL, create_time INT NULL, create_year INT NULL,
    create_month INT NULL, create_day INT NULL, create_hour INT NULL, create_minute INT NULL,
    create_second INT NULL, create_week TINYINT NULL,
    update_date_time BIGINT NULL, update_date INT NULL, update_time INT NULL, update_year INT NULL,
    update_month INT NULL, update_day INT NULL, update_hour INT NULL, update_minute INT NULL,
    update_second INT NULL, update_week TINYINT NULL,
    PRIMARY KEY (payslip_id),
    UNIQUE KEY uk_payroll_payslip_sheet_employee (com_id, sheet_id, emp_id),
    KEY idx_payroll_payslip_company (com_id, payslip_id),
    KEY idx_payroll_payslip_sheet (com_id, sheet_id, payslip_id),
    KEY idx_payroll_payslip_employee (com_id, emp_id, period, payslip_id),
    KEY idx_payroll_payslip_published (com_id, emp_id, published_at, payslip_id),
    CONSTRAINT fk_payroll_payslip_company FOREIGN KEY (com_id) REFERENCES db_tenant_company (com_id),
    CONSTRAINT fk_payroll_payslip_store FOREIGN KEY (store_id) REFERENCES db_tenant_store (store_id),
    CONSTRAINT fk_payroll_payslip_team FOREIGN KEY (team_id) REFERENCES db_tenant_team (team_id),
    CONSTRAINT fk_payroll_payslip_employee FOREIGN KEY (emp_id) REFERENCES db_tenant_employee (emp_id),
    CONSTRAINT fk_payroll_payslip_sheet FOREIGN KEY (sheet_id) REFERENCES db_payroll_sheet (sheet_id),
    CONSTRAINT fk_payroll_payslip_creator FOREIGN KEY (creator_id) REFERENCES db_tenant_employee (emp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工工资条';

-- 回滚（仅在确认不保留员工薪酬事实时执行）：
-- DROP TABLE IF EXISTS db_payroll_payslip;
-- DROP TABLE IF EXISTS db_payroll_item;
-- DROP TABLE IF EXISTS db_payroll_sheet;
-- DROP TABLE IF EXISTS db_payroll_commission_release;
-- DROP TABLE IF EXISTS db_payroll_commission_item;
-- DROP TABLE IF EXISTS db_payroll_attendance_summary;
-- DROP TABLE IF EXISTS db_payroll_compensation_scheme;
-- ALTER TABLE db_finance_settlement DROP COLUMN employee_commission_amount, DROP COLUMN employee_salary_amount;
-- ALTER TABLE db_finance_payment DROP INDEX idx_fin_payment_company_batch, DROP INDEX idx_fin_payment_company_employee,
--   DROP COLUMN batch_no, DROP COLUMN payee_emp_id;
-- ALTER TABLE db_finance_payable DROP INDEX uk_fin_payable_company_source,
--   DROP COLUMN source_id, DROP COLUMN source_type;
-- ALTER TABLE db_finance_payable MODIFY COLUMN type ENUM('Expense','Material','Other','Project','Salary','Supplier') NOT NULL;
-- ALTER TABLE db_finance_payable MODIFY COLUMN property_id BIGINT NOT NULL;
