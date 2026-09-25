-- 接管后剩余施工计划 P0
-- 1) 项目与接管基线阶段记录不可覆盖的进度起点。
-- 2) 承接项目记录计划状态、版本和确认人。
-- 3) 新增计划确认和调整留痕表。
-- 适用：MySQL 8.0+；执行前备份。

ALTER TABLE db_project
    ADD COLUMN baseline_progress INT NULL COMMENT '接管时项目进度基线' AFTER progress;

ALTER TABLE db_project_stage
    ADD COLUMN baseline_progress INT NULL COMMENT '接管时阶段进度基线' AFTER progress;

ALTER TABLE carryover_project
    ADD COLUMN plan_status VARCHAR(16) NOT NULL DEFAULT 'NotStarted' COMMENT '接管后计划：NotStarted/Draft/Confirmed/Locked' AFTER first_business_type,
    ADD COLUMN plan_version INT NOT NULL DEFAULT 0 COMMENT '接管后计划版本' AFTER plan_status,
    ADD COLUMN plan_confirmed_at DATETIME(6) NULL COMMENT '接管后计划确认时间' AFTER plan_version,
    ADD COLUMN plan_confirmed_by BIGINT NULL COMMENT '接管后计划确认人' AFTER plan_confirmed_at;

UPDATE db_project project
JOIN carryover_project item ON item.project_id = project.project_id
SET project.baseline_progress = item.progress
WHERE project.business_origin = 'Carryover'
  AND project.baseline_progress IS NULL;

UPDATE db_project_stage stage
JOIN carryover_project item ON item.project_id = stage.project_id
SET stage.baseline_progress = item.progress
WHERE stage.stage_code = 'CARRYOVER_BASELINE'
  AND stage.baseline_progress IS NULL;

CREATE TABLE IF NOT EXISTS carryover_plan_confirmation (
    confirmation_id BIGINT NOT NULL COMMENT '计划确认索引',
    com_id BIGINT NOT NULL COMMENT '所属企业',
    item_id BIGINT NOT NULL COMMENT '所属承接项目',
    project_id BIGINT NOT NULL COMMENT '正式项目',
    required_role VARCHAR(16) NOT NULL COMMENT '应确认角色',
    confirmed_by BIGINT NOT NULL COMMENT '实际确认人',
    proxy BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否代确认',
    plan_version INT NOT NULL COMMENT '计划版本',
    confirmed_at DATETIME(6) NOT NULL COMMENT '确认时间',
    summary VARCHAR(500) NULL COMMENT '确认摘要',
    create_date INT NULL, create_date_time BIGINT NULL, create_day INT NULL, create_hour INT NULL,
    create_minute INT NULL, create_month INT NULL, create_second INT NULL, create_time INT NULL,
    create_week TINYINT NULL, create_year INT NULL,
    update_date INT NULL, update_date_time BIGINT NULL, update_day INT NULL, update_hour INT NULL,
    update_minute INT NULL, update_month INT NULL, update_second INT NULL, update_time INT NULL,
    update_week TINYINT NULL, update_year INT NULL,
    PRIMARY KEY (confirmation_id),
    UNIQUE KEY uk_carryover_plan_confirmation_version (com_id, item_id, plan_version),
    KEY idx_carryover_plan_confirmation_item (com_id, item_id, plan_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接管后施工计划确认';

CREATE TABLE IF NOT EXISTS carryover_plan_change (
    change_id BIGINT NOT NULL COMMENT '调整记录索引',
    com_id BIGINT NOT NULL COMMENT '所属企业',
    item_id BIGINT NOT NULL COMMENT '所属承接项目',
    project_id BIGINT NOT NULL COMMENT '正式项目',
    stage_id BIGINT NULL COMMENT '涉及施工阶段',
    stage_name VARCHAR(128) NULL COMMENT '阶段名称快照',
    action VARCHAR(24) NOT NULL COMMENT '计划动作',
    before_snapshot VARCHAR(2000) NULL COMMENT '调整前快照',
    after_snapshot VARCHAR(2000) NULL COMMENT '调整后快照',
    reason VARCHAR(500) NULL COMMENT '调整原因',
    created_by BIGINT NOT NULL COMMENT '操作人',
    create_date INT NULL, create_date_time BIGINT NULL, create_day INT NULL, create_hour INT NULL,
    create_minute INT NULL, create_month INT NULL, create_second INT NULL, create_time INT NULL,
    create_week TINYINT NULL, create_year INT NULL,
    update_date INT NULL, update_date_time BIGINT NULL, update_day INT NULL, update_hour INT NULL,
    update_minute INT NULL, update_month INT NULL, update_second INT NULL, update_time INT NULL,
    update_week TINYINT NULL, update_year INT NULL,
    PRIMARY KEY (change_id),
    KEY idx_carryover_plan_change_item (com_id, item_id, change_id),
    KEY idx_carryover_plan_change_project (com_id, project_id, change_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接管后施工计划调整记录';

ALTER TABLE carryover_project
    ADD CONSTRAINT fk_carryover_project_plan_confirmed_by
        FOREIGN KEY (plan_confirmed_by) REFERENCES db_tenant_employee (emp_id);

ALTER TABLE carryover_plan_confirmation
    ADD CONSTRAINT fk_carryover_plan_confirmation_company
        FOREIGN KEY (com_id) REFERENCES db_tenant_company (com_id),
    ADD CONSTRAINT fk_carryover_plan_confirmation_item
        FOREIGN KEY (item_id) REFERENCES carryover_project (item_id),
    ADD CONSTRAINT fk_carryover_plan_confirmation_project
        FOREIGN KEY (project_id) REFERENCES db_project (project_id),
    ADD CONSTRAINT fk_carryover_plan_confirmation_employee
        FOREIGN KEY (confirmed_by) REFERENCES db_tenant_employee (emp_id);

ALTER TABLE carryover_plan_change
    ADD CONSTRAINT fk_carryover_plan_change_company
        FOREIGN KEY (com_id) REFERENCES db_tenant_company (com_id),
    ADD CONSTRAINT fk_carryover_plan_change_item
        FOREIGN KEY (item_id) REFERENCES carryover_project (item_id),
    ADD CONSTRAINT fk_carryover_plan_change_project
        FOREIGN KEY (project_id) REFERENCES db_project (project_id),
    ADD CONSTRAINT fk_carryover_plan_change_stage
        FOREIGN KEY (stage_id) REFERENCES db_project_stage (stage_id),
    ADD CONSTRAINT fk_carryover_plan_change_employee
        FOREIGN KEY (created_by) REFERENCES db_tenant_employee (emp_id);
