-- 69-报价工程量公式.sql
-- Web Excel 式报价：为报价行增加独立工程量公式，保留既有 formula 字段与 App 兼容性。
SET @db_name = DATABASE();
SET @has_quantity_formula = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = @db_name AND table_name = 'quote_line' AND column_name = 'quantity_formula'
);
SET @sql = IF(@has_quantity_formula = 0,
  'ALTER TABLE quote_line ADD COLUMN quantity_formula VARCHAR(512) NULL COMMENT ''Web工程量公式（服务端权威计算）'' AFTER formula',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 回滚仅用于隔离环境：
-- ALTER TABLE quote_line DROP COLUMN quantity_formula;
