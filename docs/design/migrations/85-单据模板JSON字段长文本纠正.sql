-- 纠正历史 db_document_template / db_document_template_version 的 JSON 存储类型。
-- Hibernate 可能将 @Lob String 在 MySQL 建为 TINYTEXT；字段配置和高级设计器配置会超过 255 字节。
-- 本迁移必须可重复执行；生产执行前按 L3 流程备份并登记迁移账本。

SET @ddl := (
  SELECT IF(COLUMN_TYPE = 'longtext',
    'SELECT 1',
    'ALTER TABLE db_document_template MODIFY COLUMN field_config LONGTEXT NOT NULL COMMENT ''字段配置JSON''')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'db_document_template' AND COLUMN_NAME = 'field_config'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := (
  SELECT IF(COLUMN_TYPE = 'longtext',
    'SELECT 1',
    'ALTER TABLE db_document_template MODIFY COLUMN style_config LONGTEXT NOT NULL COMMENT ''受控样式JSON''')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'db_document_template' AND COLUMN_NAME = 'style_config'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := (
  SELECT IF(COLUMN_TYPE = 'longtext',
    'SELECT 1',
    'ALTER TABLE db_document_template MODIFY COLUMN layout_config LONGTEXT NULL COMMENT ''受控版式JSON''')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'db_document_template' AND COLUMN_NAME = 'layout_config'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := (
  SELECT IF(COLUMN_TYPE = 'longtext',
    'SELECT 1',
    'ALTER TABLE db_document_template MODIFY COLUMN designer_config LONGTEXT NULL COMMENT ''凭证设计器JSON''')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'db_document_template' AND COLUMN_NAME = 'designer_config'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := (
  SELECT IF(COLUMN_TYPE = 'longtext',
    'SELECT 1',
    'ALTER TABLE db_document_template_version MODIFY COLUMN field_config LONGTEXT NOT NULL COMMENT ''字段配置JSON''')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'db_document_template_version' AND COLUMN_NAME = 'field_config'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := (
  SELECT IF(COLUMN_TYPE = 'longtext',
    'SELECT 1',
    'ALTER TABLE db_document_template_version MODIFY COLUMN style_config LONGTEXT NOT NULL COMMENT ''受控样式JSON''')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'db_document_template_version' AND COLUMN_NAME = 'style_config'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := (
  SELECT IF(COLUMN_TYPE = 'longtext',
    'SELECT 1',
    'ALTER TABLE db_document_template_version MODIFY COLUMN layout_config LONGTEXT NULL COMMENT ''受控版式JSON''')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'db_document_template_version' AND COLUMN_NAME = 'layout_config'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := (
  SELECT IF(COLUMN_TYPE = 'longtext',
    'SELECT 1',
    'ALTER TABLE db_document_template_version MODIFY COLUMN designer_config LONGTEXT NULL COMMENT ''凭证设计器JSON''')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'db_document_template_version' AND COLUMN_NAME = 'designer_config'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
