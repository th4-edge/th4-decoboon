-- 企业单据模板和不可变版本增加凭证设计器配置。
-- 必须可重复执行；生产执行前按 L3 流程备份并登记迁移账本。

SET @column_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'db_document_template' AND COLUMN_NAME = 'designer_config'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE db_document_template ADD COLUMN designer_config LONGTEXT NULL COMMENT ''凭证设计器JSON'' AFTER layout_config',
  'ALTER TABLE db_document_template MODIFY COLUMN designer_config LONGTEXT NULL COMMENT ''凭证设计器JSON''');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'db_document_template_version' AND COLUMN_NAME = 'designer_config'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE db_document_template_version ADD COLUMN designer_config LONGTEXT NULL COMMENT ''凭证设计器JSON'' AFTER layout_config',
  'ALTER TABLE db_document_template_version MODIFY COLUMN designer_config LONGTEXT NULL COMMENT ''凭证设计器JSON''');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
