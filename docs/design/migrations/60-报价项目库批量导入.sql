-- 报价项目库批量导入（PRD-CATALOG-IMPORT-20260926-001）
--
-- 1) 标准成本区分“成本待维护”（NULL）与“明确零成本”（0），不再用 0 冒充完整成本。
-- 2) 固定总价使用独立字段，不再写入材料/人工单价。
-- 3) 新增导入批次与导入行，支持预览暂存、批次追溯、24 小时无引用撤销。
--
-- 适用：MySQL 8.0+。存量库执行前请备份；空库部署同步使用 01-空库基线.sql。

-- 1. 报价项目库主数据：成本可空 + 固定总价 + 导入来源
ALTER TABLE catalog_item
    MODIFY COLUMN cost_price DECIMAL(12, 2) NULL COMMENT '标准成本单价，NULL=待维护，0=明确零成本';

ALTER TABLE catalog_item
    ADD COLUMN fixed_total_price DECIMAL(12, 2) NULL COMMENT '固定总价（仅固定总价计价方式）' AFTER manual_price,
    ADD COLUMN import_batch_id BIGINT NULL COMMENT '导入来源批次' AFTER com_id,
    ADD COLUMN source_type VARCHAR(16) NOT NULL DEFAULT 'Manual' COMMENT '来源：Manual/Import/Migration';

CREATE INDEX idx_catalog_item_import_batch ON catalog_item (com_id, import_batch_id, item_id);

-- 2. 下游固定总价快照与成本待维护传播
ALTER TABLE db_design_item
    MODIFY COLUMN cost_price DECIMAL(12, 2) NULL COMMENT '成本单价，NULL=成本待维护，0=明确零成本';

ALTER TABLE db_design_item
    ADD COLUMN fixed_total_price DECIMAL(12, 2) NULL COMMENT '固定总价快照（仅固定总价计价方式）' AFTER manual_price;

ALTER TABLE quote_line
    ADD COLUMN fixed_total_price DECIMAL(12, 2) NULL COMMENT '固定总价快照（仅固定总价计价方式）' AFTER manual_price;

-- 3. 导入批次
CREATE TABLE IF NOT EXISTS catalog_import_batch (
    batch_id BIGINT NOT NULL COMMENT '批次索引',
    com_id BIGINT NOT NULL COMMENT '所属企业',
    created_by BIGINT NOT NULL COMMENT '操作人（BOSS）',
    batch_no VARCHAR(48) NOT NULL COMMENT '批次号',
    template_version VARCHAR(24) NOT NULL COMMENT '模板版本',
    validation_version VARCHAR(24) NOT NULL COMMENT '校验版本（预览与确认必须一致）',
    original_filename VARCHAR(256) NULL COMMENT '原始文件名',
    source_path VARCHAR(512) NOT NULL COMMENT '上传文件对象键',
    content_type VARCHAR(128) NULL COMMENT '文件类型',
    file_size BIGINT NULL COMMENT '文件大小（字节）',
    file_sha256 VARCHAR(64) NOT NULL COMMENT '文件指纹',
    status VARCHAR(24) NOT NULL COMMENT 'UPLOADED/PARSED/PENDING_CONFIRM/IMPORTING/IMPORTED/PARTIALLY_IMPORTED/FAILED/EXPIRED/REVOKED',
    duplicate_policy VARCHAR(16) NOT NULL DEFAULT 'Skip' COMMENT '疑似重复策略 Skip/Import',
    total_rows INT NOT NULL DEFAULT 0,
    ready_rows INT NOT NULL DEFAULT 0,
    duplicate_rows INT NOT NULL DEFAULT 0,
    suspected_duplicate_rows INT NOT NULL DEFAULT 0,
    error_rows INT NOT NULL DEFAULT 0,
    cost_pending_rows INT NOT NULL DEFAULT 0,
    imported_rows INT NOT NULL DEFAULT 0,
    skipped_rows INT NOT NULL DEFAULT 0,
    partial_import BIT(1) NOT NULL DEFAULT b'0',
    expire_at DATETIME(6) NULL,
    parsed_at DATETIME(6) NULL,
    confirmed_at DATETIME(6) NULL,
    finished_at DATETIME(6) NULL,
    revoked_at DATETIME(6) NULL,
    message LONGTEXT NULL,
    create_date INT NULL, create_date_time BIGINT NULL, create_day INT NULL, create_hour INT NULL,
    create_minute INT NULL, create_month INT NULL, create_second INT NULL, create_time INT NULL,
    create_week TINYINT NULL, create_year INT NULL,
    update_date INT NULL, update_date_time BIGINT NULL, update_day INT NULL, update_hour INT NULL,
    update_minute INT NULL, update_month INT NULL, update_second INT NULL, update_time INT NULL,
    update_week TINYINT NULL, update_year INT NULL,
    PRIMARY KEY (batch_id),
    KEY idx_catalog_import_batch_company (com_id, batch_id),
    KEY idx_catalog_import_batch_company_status (com_id, status, batch_id),
    KEY idx_catalog_import_batch_company_file (com_id, file_sha256, batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报价项目库导入批次';

-- 4. 导入行（预览、重复项、错误清单与导入来源）
CREATE TABLE IF NOT EXISTS catalog_import_row (
    row_id BIGINT NOT NULL COMMENT '行索引',
    com_id BIGINT NOT NULL COMMENT '所属企业',
    batch_id BIGINT NOT NULL COMMENT '所属批次',
    row_no INT NOT NULL COMMENT 'Excel/CSV 行号（含表头）',
    status VARCHAR(24) NOT NULL COMMENT 'READY/DUPLICATE/SUSPECTED_DUPLICATE/ERROR/IMPORTED/SKIPPED',
    error_field VARCHAR(64) NULL,
    error_code VARCHAR(64) NULL,
    error_reason VARCHAR(512) NULL,
    raw_data LONGTEXT NULL COMMENT '行原始数据快照',
    item_name VARCHAR(256) NULL,
    item_type VARCHAR(20) NULL,
    unit VARCHAR(32) NULL,
    pricing_mode VARCHAR(24) NULL,
    sale_amount DECIMAL(12, 2) NULL,
    cost_pending BIT(1) NULL,
    item_id BIGINT NULL COMMENT '导入成功后关联项目',
    create_date INT NULL, create_date_time BIGINT NULL, create_day INT NULL, create_hour INT NULL,
    create_minute INT NULL, create_month INT NULL, create_second INT NULL, create_time INT NULL,
    create_week TINYINT NULL, create_year INT NULL,
    update_date INT NULL, update_date_time BIGINT NULL, update_day INT NULL, update_hour INT NULL,
    update_minute INT NULL, update_month INT NULL, update_second INT NULL, update_time INT NULL,
    update_week TINYINT NULL, update_year INT NULL,
    PRIMARY KEY (row_id),
    UNIQUE KEY uk_catalog_import_row_batch_no (com_id, batch_id, row_no),
    KEY idx_catalog_import_row_company (com_id, row_id),
    KEY idx_catalog_import_row_batch_status (com_id, batch_id, status, row_no),
    KEY idx_catalog_import_row_item (com_id, item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报价项目库导入行';

-- 5. 外键（存量库如已由 ddl-auto=update 建过同义外键，可跳过）
ALTER TABLE catalog_import_batch
    ADD CONSTRAINT FK_catalog_import_batch_company FOREIGN KEY (com_id) REFERENCES db_tenant_company (com_id),
    ADD CONSTRAINT FK_catalog_import_batch_creator FOREIGN KEY (created_by) REFERENCES db_tenant_employee (emp_id);

ALTER TABLE catalog_import_row
    ADD CONSTRAINT FK_catalog_import_row_company FOREIGN KEY (com_id) REFERENCES db_tenant_company (com_id),
    ADD CONSTRAINT FK_catalog_import_row_batch FOREIGN KEY (batch_id) REFERENCES catalog_import_batch (batch_id),
    ADD CONSTRAINT FK_catalog_import_row_item FOREIGN KEY (item_id) REFERENCES catalog_item (item_id);

-- 回滚（确认无导入批次需要保留时执行）
-- DROP TABLE catalog_import_row;
-- DROP TABLE catalog_import_batch;
-- ALTER TABLE quote_line DROP COLUMN fixed_total_price;
-- ALTER TABLE db_design_item DROP COLUMN fixed_total_price;
-- ALTER TABLE db_design_item MODIFY COLUMN cost_price DECIMAL(12,2) NOT NULL;
-- DROP INDEX idx_catalog_item_import_batch ON catalog_item;
-- ALTER TABLE catalog_item DROP COLUMN source_type, DROP COLUMN import_batch_id, DROP COLUMN fixed_total_price;
-- ALTER TABLE catalog_item MODIFY COLUMN cost_price DECIMAL(12,2) NOT NULL;
