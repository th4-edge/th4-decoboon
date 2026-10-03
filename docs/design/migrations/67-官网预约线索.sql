-- 官网预约线索（SITE-LEAD-20260927-001）
-- 范围：仅保存公开官网表单提交的预约意向，不接入租户业务对象和客户主链。
-- 适用：MySQL 8.0+。

CREATE TABLE IF NOT EXISTS db_site_lead (
    lead_id BIGINT NOT NULL COMMENT '线索索引',
    contact_name VARCHAR(80) NOT NULL COMMENT '称呼',
    contact VARCHAR(120) NOT NULL COMMENT '联系方式',
    topic VARCHAR(80) NOT NULL COMMENT '关心的问题',
    note VARCHAR(500) NULL COMMENT '现场补充',
    page_url VARCHAR(500) NULL COMMENT '来源页面',
    source VARCHAR(32) NOT NULL DEFAULT 'website' COMMENT '来源渠道',
    status VARCHAR(16) NOT NULL COMMENT 'New/Contacted/Closed',
    consent BIT(1) NOT NULL DEFAULT b'1' COMMENT '同意沟通',
    create_date INT NULL,
    create_date_time BIGINT NULL,
    create_day INT NULL,
    create_hour INT NULL,
    create_minute INT NULL,
    create_month INT NULL,
    create_second INT NULL,
    create_time INT NULL,
    create_week TINYINT NULL,
    create_year INT NULL,
    PRIMARY KEY (lead_id),
    KEY idx_site_lead_status_time (status, create_date_time, lead_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='官网预约线索';
