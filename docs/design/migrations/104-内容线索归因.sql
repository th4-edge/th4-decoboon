-- 104-内容线索归因.sql
-- 目的：内容获客 P0-4 内容线索触点与经营看板。
-- 影响表：新增 db_content_lead_touch。
-- 回滚：DROP TABLE IF EXISTS db_content_lead_touch;

CREATE TABLE db_content_lead_touch (
    touch_id BIGINT NOT NULL COMMENT '触点索引',
    com_id BIGINT NOT NULL COMMENT '所属企业',
    customer_id BIGINT NOT NULL COMMENT '客户',
    employee_content_id BIGINT NOT NULL COMMENT '员工内容',
    publication_id BIGINT NULL COMMENT '发布记录',
    task_id BIGINT NOT NULL COMMENT '内容任务',
    emp_id BIGINT NOT NULL COMMENT '归因员工',
    attribution_type VARCHAR(24) NOT NULL COMMENT '归因方式',
    confidence VARCHAR(16) NOT NULL COMMENT '归因置信度',
    is_primary TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否主归因',
    evidence VARCHAR(512) NULL COMMENT '归因证据',
    touched_at DATETIME(6) NOT NULL COMMENT '触点时间',
    create_date_time BIGINT NULL,
    create_date INT NULL,
    create_time INT NULL,
    create_year INT NULL,
    create_month INT NULL,
    create_day INT NULL,
    create_hour INT NULL,
    create_minute INT NULL,
    create_second INT NULL,
    create_week TINYINT NULL,
    update_date_time BIGINT NULL,
    update_date INT NULL,
    update_time INT NULL,
    update_year INT NULL,
    update_month INT NULL,
    update_day INT NULL,
    update_hour INT NULL,
    update_minute INT NULL,
    update_second INT NULL,
    update_week TINYINT NULL,
    PRIMARY KEY (touch_id),
    UNIQUE KEY uk_content_touch_customer_content (customer_id, employee_content_id),
    KEY idx_content_touch_company (com_id, touch_id),
    KEY idx_content_touch_customer (customer_id, touch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='内容线索触点';
