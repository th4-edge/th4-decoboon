-- 回滚 124 套餐模板不可变版本
-- 前置：后端已回退到不读取版本字段的版本；确认不存在仅依赖多版本的新业务数据。
-- 注意：回滚会移除发布快照与套餐来源版本字段；执行前必须完成数据库备份。

DROP INDEX idx_quote_package_version ON quote;
ALTER TABLE quote_line
    DROP COLUMN package_section_name_snapshot;
ALTER TABLE quote
    DROP COLUMN package_template_snapshot_hash,
    DROP COLUMN package_template_version,
    DROP COLUMN package_template_code;

ALTER TABLE pack_line
    DROP COLUMN cost_price_snapshot,
    DROP COLUMN craft_snapshot,
    DROP COLUMN catalog_category_snapshot,
    DROP COLUMN catalog_type_snapshot,
    DROP COLUMN section_name_snapshot;

DROP INDEX uk_pack_template_current_version ON pack_template;
DROP INDEX idx_pack_template_published_by ON pack_template;
DROP INDEX idx_pack_template_current ON pack_template;
DROP INDEX idx_pack_template_code_version ON pack_template;
DROP INDEX uk_pack_template_code_version ON pack_template;

ALTER TABLE pack_template
    DROP COLUMN snapshot_hash,
    DROP COLUMN snapshot_json,
    DROP COLUMN published_by_id,
    DROP COLUMN published_at,
    DROP COLUMN current_published_code,
    DROP COLUMN is_current_published,
    DROP COLUMN status,
    DROP COLUMN version,
    DROP COLUMN code;
