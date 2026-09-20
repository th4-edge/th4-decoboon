-- 客户标签字典 / 客户标签关联 / 客户画像。
--
-- 依据 docs/plan/2026-09-18-客户标签画像与分级-P0b后端契约.md（B1–B4 已冻结）：
-- 1) cust_tag 为客户标签字典，仅企业负责人与门店店长可维护；
--    scope=System 的字典项由系统事实派生，任何角色不可改名或删除。
-- 2) cust_tag_link 只绑定人工标签；系统标签由服务端按客户事实装配，不落关联表。
-- 3) cust_portrait + cust_portrait_concern 承载 10 个营销画像字段，全部可空。
-- 4) 客户分级为派生值，不落库。
-- 5) 数据范围继承客户经营归属，由 Provider 显式校验，不做冗余快照。

CREATE TABLE IF NOT EXISTS cust_tag (
    tag_id BIGINT NOT NULL COMMENT '标签索引',
    com_id BIGINT NOT NULL COMMENT '所属企业',
    name VARCHAR(20) NOT NULL COMMENT '标签名称',
    category VARCHAR(16) NOT NULL COMMENT '标签分类',
    scope VARCHAR(16) NOT NULL COMMENT '标签来源：System/Manual',
    color VARCHAR(16) NULL COMMENT '标签颜色',
    status VARCHAR(16) NOT NULL COMMENT '标签状态：Enabled/Disabled',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    create_date INT NULL, create_date_time BIGINT NULL, create_day INT NULL,
    create_hour INT NULL, create_minute INT NULL, create_month INT NULL,
    create_second INT NULL, create_time INT NULL, create_week TINYINT NULL,
    create_year INT NULL,
    update_date INT NULL, update_date_time BIGINT NULL, update_day INT NULL,
    update_hour INT NULL, update_minute INT NULL, update_month INT NULL,
    update_second INT NULL, update_time INT NULL, update_week TINYINT NULL,
    update_year INT NULL,
    PRIMARY KEY (tag_id),
    UNIQUE KEY uk_cust_tag_company_name (com_id, name),
    KEY idx_cust_tag_company (com_id, tag_id),
    KEY idx_cust_tag_company_category (com_id, category, status),
    CONSTRAINT fk_cust_tag_company FOREIGN KEY (com_id) REFERENCES db_tenant_company (com_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户标签字典';

CREATE TABLE IF NOT EXISTS cust_tag_link (
    link_id BIGINT NOT NULL COMMENT '关联索引',
    com_id BIGINT NOT NULL COMMENT '所属企业',
    cust_id BIGINT NOT NULL COMMENT '客户',
    tag_id BIGINT NOT NULL COMMENT '标签',
    emp_id BIGINT NOT NULL COMMENT '打标签员工',
    create_date INT NULL, create_date_time BIGINT NULL, create_day INT NULL,
    create_hour INT NULL, create_minute INT NULL, create_month INT NULL,
    create_second INT NULL, create_time INT NULL, create_week TINYINT NULL,
    create_year INT NULL,
    update_date INT NULL, update_date_time BIGINT NULL, update_day INT NULL,
    update_hour INT NULL, update_minute INT NULL, update_month INT NULL,
    update_second INT NULL, update_time INT NULL, update_week TINYINT NULL,
    update_year INT NULL,
    PRIMARY KEY (link_id),
    UNIQUE KEY uk_cust_tag_link_company_customer_tag (com_id, cust_id, tag_id),
    KEY idx_cust_tag_link_company (com_id, link_id),
    KEY idx_cust_tag_link_company_customer (com_id, cust_id, link_id),
    KEY idx_cust_tag_link_company_tag (com_id, tag_id, link_id),
    CONSTRAINT fk_cust_tag_link_company FOREIGN KEY (com_id) REFERENCES db_tenant_company (com_id),
    CONSTRAINT fk_cust_tag_link_customer FOREIGN KEY (cust_id) REFERENCES cust_customer (cust_id),
    CONSTRAINT fk_cust_tag_link_tag FOREIGN KEY (tag_id) REFERENCES cust_tag (tag_id),
    CONSTRAINT fk_cust_tag_link_employee FOREIGN KEY (emp_id) REFERENCES db_tenant_employee (emp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户标签关联';

CREATE TABLE IF NOT EXISTS cust_portrait (
    portrait_id BIGINT NOT NULL COMMENT '画像索引',
    com_id BIGINT NOT NULL COMMENT '所属企业',
    cust_id BIGINT NOT NULL COMMENT '客户',
    decision_role VARCHAR(16) NULL COMMENT '决策角色',
    purpose VARCHAR(24) NULL COMMENT '装修动机',
    house_condition VARCHAR(16) NULL COMMENT '房屋现状',
    start_window VARCHAR(24) NULL COMMENT '计划开工时间',
    style VARCHAR(16) NULL COMMENT '风格偏好',
    eco VARCHAR(16) NULL COMMENT '环保健康要求',
    household VARCHAR(24) NULL COMMENT '居住人口结构',
    payment VARCHAR(16) NULL COMMENT '付款方式偏好',
    competitor VARCHAR(24) NULL COMMENT '竞品对比状态',
    emp_id BIGINT NULL COMMENT '最后修改人',
    create_date INT NULL, create_date_time BIGINT NULL, create_day INT NULL,
    create_hour INT NULL, create_minute INT NULL, create_month INT NULL,
    create_second INT NULL, create_time INT NULL, create_week TINYINT NULL,
    create_year INT NULL,
    update_date INT NULL, update_date_time BIGINT NULL, update_day INT NULL,
    update_hour INT NULL, update_minute INT NULL, update_month INT NULL,
    update_second INT NULL, update_time INT NULL, update_week TINYINT NULL,
    update_year INT NULL,
    PRIMARY KEY (portrait_id),
    UNIQUE KEY uk_cust_portrait_company_customer (com_id, cust_id),
    KEY idx_cust_portrait_company (com_id, portrait_id),
    CONSTRAINT fk_cust_portrait_company FOREIGN KEY (com_id) REFERENCES db_tenant_company (com_id),
    CONSTRAINT fk_cust_portrait_customer FOREIGN KEY (cust_id) REFERENCES cust_customer (cust_id),
    CONSTRAINT fk_cust_portrait_employee FOREIGN KEY (emp_id) REFERENCES db_tenant_employee (emp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户画像';

CREATE TABLE IF NOT EXISTS cust_portrait_concern (
    portrait_id BIGINT NOT NULL COMMENT '画像索引',
    concern VARCHAR(16) NOT NULL COMMENT '主要顾虑'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户画像主要顾虑';
