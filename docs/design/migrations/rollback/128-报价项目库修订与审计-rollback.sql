-- 回滚 128 报价项目库修订与审计
-- 前置：后端已回退；完成数据库备份。审计和报价来源修订先归档，再移除新增字段。

DROP TABLE IF EXISTS quote_line_catalog_source_rollback_128;
CREATE TABLE quote_line_catalog_source_rollback_128 AS
SELECT item_id, com_id, catalog_source_id, catalog_source_code, catalog_source_revision
FROM quote_line;

DROP INDEX idx_quote_line_catalog_source ON quote_line;
ALTER TABLE quote_line
    DROP COLUMN catalog_source_revision,
    DROP COLUMN catalog_source_code,
    DROP COLUMN catalog_source_id;

RENAME TABLE catalog_item_revision TO catalog_item_revision_rollback_128;

ALTER TABLE catalog_item
    DROP COLUMN revision;
