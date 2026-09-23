-- 57-业主意见投诉.sql
-- 依据用户 2026-09-23 决策：业主端需要形成意见/投诉闭环，反馈必须关联项目、
-- 自动分派项目经理，并保留处理回复、时限、关闭与满意度。
-- 适用：MySQL 8.0+。

CREATE TABLE IF NOT EXISTS cust_customer_feedback (
    feedback_id BIGINT NOT NULL,
    com_id BIGINT NOT NULL,
    store_id BIGINT NULL,
    team_id BIGINT NULL,
    owner_id BIGINT NULL,
    project_id BIGINT NOT NULL,
    property_id BIGINT NOT NULL,
    stage_id BIGINT NULL,
    cust_id BIGINT NOT NULL,
    cre_id BIGINT NOT NULL,
    kind VARCHAR(16) NOT NULL COMMENT 'Opinion/Complaint',
    type VARCHAR(24) NOT NULL COMMENT 'Quality/Design/Material/Schedule/Service/Fee/Safety/Other',
    status VARCHAR(24) NOT NULL COMMENT 'Submitted/Processing/Resolved/Escalated/Closed',
    title VARCHAR(128) NOT NULL,
    content VARCHAR(2048) NOT NULL,
    handler_id BIGINT NULL,
    response VARCHAR(2048) NULL,
    responded_at DATETIME(6) NULL,
    due_at DATETIME(6) NULL,
    closed_at DATETIME(6) NULL,
    satisfaction INT NULL,
    create_date_time BIGINT NULL,
    create_date INT NULL,
    create_time INT NULL,
    create_year INT NULL,
    create_month INT NULL,
    create_day INT NULL,
    create_hour INT NULL,
    create_minute INT NULL,
    create_second INT NULL,
    create_week INT NULL,
    update_date_time BIGINT NULL,
    update_date INT NULL,
    update_time INT NULL,
    update_year INT NULL,
    update_month INT NULL,
    update_day INT NULL,
    update_hour INT NULL,
    update_minute INT NULL,
    update_second INT NULL,
    update_week INT NULL,
    PRIMARY KEY (feedback_id),
    KEY idx_customer_feedback_company (com_id, feedback_id),
    KEY idx_customer_feedback_project (com_id, project_id, feedback_id),
    KEY idx_customer_feedback_status (com_id, status, feedback_id),
    KEY idx_customer_feedback_credential (com_id, cre_id, feedback_id),
    KEY idx_customer_feedback_store (com_id, store_id, feedback_id),
    KEY idx_customer_feedback_team (com_id, team_id, feedback_id),
    KEY idx_customer_feedback_owner (com_id, owner_id, feedback_id),
    CONSTRAINT fk_customer_feedback_company FOREIGN KEY (com_id) REFERENCES db_tenant_company (com_id),
    CONSTRAINT fk_customer_feedback_store FOREIGN KEY (store_id) REFERENCES db_tenant_store (store_id),
    CONSTRAINT fk_customer_feedback_team FOREIGN KEY (team_id) REFERENCES db_tenant_team (team_id),
    CONSTRAINT fk_customer_feedback_owner FOREIGN KEY (owner_id) REFERENCES db_tenant_employee (emp_id),
    CONSTRAINT fk_customer_feedback_project FOREIGN KEY (project_id) REFERENCES db_project (project_id),
    CONSTRAINT fk_customer_feedback_property FOREIGN KEY (property_id) REFERENCES db_property (property_id),
    CONSTRAINT fk_customer_feedback_stage FOREIGN KEY (stage_id) REFERENCES db_project_stage (stage_id),
    CONSTRAINT fk_customer_feedback_customer FOREIGN KEY (cust_id) REFERENCES cust_customer (cust_id),
    CONSTRAINT fk_customer_feedback_credential FOREIGN KEY (cre_id) REFERENCES db_tenant_credentials (cre_id),
    CONSTRAINT fk_customer_feedback_handler FOREIGN KEY (handler_id) REFERENCES db_tenant_employee (emp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='业主意见投诉';

CREATE TABLE IF NOT EXISTS cust_customer_feedback_image (
    feedback_id BIGINT NOT NULL,
    url VARCHAR(512) NOT NULL,
    KEY idx_customer_feedback_image (feedback_id),
    CONSTRAINT fk_customer_feedback_image_feedback FOREIGN KEY (feedback_id)
        REFERENCES cust_customer_feedback (feedback_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='业主意见投诉图片';

-- 回滚（仅在确认新表无业务数据后执行）：
-- DROP TABLE cust_customer_feedback_image;
-- DROP TABLE cust_customer_feedback;
