-- 平台企业入驻服务台账（OPS-ONBOARDING-20260927-001）
-- 范围：平台运营内部服务记录；企业仅保存标量快照，不建立租户企业外键。
-- 适用：MySQL 8.0+。

CREATE TABLE IF NOT EXISTS db_platform_onboarding_service (
    service_id BIGINT NOT NULL COMMENT '服务记录ID',
    org_id BIGINT NOT NULL COMMENT '平台组织ID',
    user_id BIGINT NOT NULL COMMENT '操作人ID',
    target_com_id BIGINT NOT NULL COMMENT '租户企业ID快照，无外键',
    target_company_title VARCHAR(128) NOT NULL COMMENT '企业名称快照',
    service_status VARCHAR(24) NOT NULL COMMENT 'Pending/Contacted/Waiting/Activated/Closed',
    contact_result VARCHAR(500) NULL COMMENT '沟通结果',
    next_action VARCHAR(500) NOT NULL COMMENT '下一步动作',
    next_follow_at DATETIME(6) NULL COMMENT '下次跟进时间',
    remark VARCHAR(1000) NULL COMMENT '备注',
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
    PRIMARY KEY (service_id),
    KEY idx_platform_onboarding_company_time (org_id, target_com_id, create_date_time, service_id),
    KEY idx_platform_onboarding_operator_time (org_id, user_id, create_date_time),
    CONSTRAINT fk_platform_onboarding_org FOREIGN KEY (org_id) REFERENCES db_platform_org (org_id),
    CONSTRAINT fk_platform_onboarding_user FOREIGN KEY (user_id) REFERENCES db_platform_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台企业入驻服务记录';
