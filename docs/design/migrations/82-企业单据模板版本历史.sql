-- 企业单据模板增加当前发布版本和不可变版本历史。
-- 必须可重复执行；生产执行前按 L3 流程备份并登记迁移账本。

SET @column_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'db_document_template' AND COLUMN_NAME = 'published_version'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE db_document_template ADD COLUMN published_version INT NOT NULL DEFAULT 0 COMMENT ''当前发布版本'' AFTER version',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'db_document_template' AND COLUMN_NAME = 'published_by_id'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE db_document_template ADD COLUMN published_by_id BIGINT NULL COMMENT ''当前版本发布人'' AFTER status',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'db_document_template' AND COLUMN_NAME = 'published_at'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE db_document_template ADD COLUMN published_at DATETIME(6) NULL COMMENT ''当前版本发布时间'' AFTER published_by_id',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS db_document_template_version (
    version_id BIGINT NOT NULL,
    com_id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    version INT NOT NULL,
    name VARCHAR(128) NOT NULL,
    preset_code VARCHAR(64) NULL,
    baseline_version INT NOT NULL,
    field_config LONGTEXT NOT NULL,
    style_config LONGTEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    published_by_id BIGINT NULL,
    published_at DATETIME(6) NULL,
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
    PRIMARY KEY (version_id),
    UNIQUE KEY uk_document_template_version (com_id, template_id, version),
    KEY idx_document_template_version_company (com_id, version_id),
    KEY idx_document_template_version_template (template_id, version),
    KEY idx_document_template_version_status (com_id, template_id, status, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
