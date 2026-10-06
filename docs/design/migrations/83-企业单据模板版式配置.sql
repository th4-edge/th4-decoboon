-- 企业单据模板和不可变版本增加受控版式配置。
-- 必须可重复执行；生产执行前按 L3 流程备份并登记迁移账本。

SET @column_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'db_document_template' AND COLUMN_NAME = 'layout_config'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE db_document_template ADD COLUMN layout_config LONGTEXT NULL COMMENT ''受控版式JSON'' AFTER style_config',
  'ALTER TABLE db_document_template MODIFY COLUMN layout_config LONGTEXT NULL COMMENT ''受控版式JSON''');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'db_document_template_version' AND COLUMN_NAME = 'layout_config'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE db_document_template_version ADD COLUMN layout_config LONGTEXT NULL COMMENT ''受控版式JSON'' AFTER style_config',
  'ALTER TABLE db_document_template_version MODIFY COLUMN layout_config LONGTEXT NULL COMMENT ''受控版式JSON''');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
