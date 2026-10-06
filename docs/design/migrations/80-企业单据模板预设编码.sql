-- 企业单据模板增加来源预设编码，支持同一 DocumentType 下复制多个行业基础模板。
-- 必须可重复执行；生产执行前按 L3 流程备份并登记迁移账本。

SET @column_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'db_document_template'
    AND COLUMN_NAME = 'preset_code'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE db_document_template ADD COLUMN preset_code VARCHAR(64) NULL COMMENT ''来源预设编码'' AFTER name',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists := (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'db_document_template'
    AND INDEX_NAME = 'idx_document_template_company_preset'
);
SET @ddl := IF(@index_exists = 0,
  'ALTER TABLE db_document_template ADD INDEX idx_document_template_company_preset (com_id, type, preset_code, version)',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
