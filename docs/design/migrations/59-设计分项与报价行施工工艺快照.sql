-- 59-设计分项与报价行施工工艺快照.sql
-- 依据用户 2026-09-24 决策：施工工艺必须能被员工端与业主端同时看到，
-- 并且报价一旦生成就冻结当时的工艺口径，不随材料库主数据回改。
--
-- 变更：
--   1) db_design_item 增加 craft（longtext）——设计分项生成时从材料库 CatalogItem.craft 冻结；
--   2) quote_line 增加 craft（longtext）——报价行生成时从设计分项/材料库冻结。
-- 类型与 catalog_item.craft 对齐（longtext）；ddl-auto:update 不会修改既有列类型，长文本必须显式 alter（ECD-009）。
-- 幂等：加列前查 information_schema；回填只写 NULL/空串，绝不覆盖已有快照。
-- 适用：MySQL 8.0+。

SET @db_name = DATABASE();

-- 1) db_design_item.craft
SET @has_design_item_craft = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @db_name AND table_name = 'db_design_item' AND column_name = 'craft'
);
SET @sql = IF(@has_design_item_craft = 0,
    'ALTER TABLE db_design_item ADD COLUMN craft LONGTEXT NULL COMMENT ''施工工艺快照（来自材料库）'' AFTER criterion',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) quote_line.craft
SET @has_quote_line_craft = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @db_name AND table_name = 'quote_line' AND column_name = 'craft'
);
SET @sql = IF(@has_quote_line_craft = 0,
    'ALTER TABLE quote_line ADD COLUMN craft LONGTEXT NULL COMMENT ''施工工艺快照（生成报价时冻结）'' AFTER formula',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) 历史设计分项回填：直接取材料库施工工艺
UPDATE db_design_item di
JOIN catalog_item ci ON ci.item_id = di.catalog_item_id
SET di.craft = ci.craft
WHERE (di.craft IS NULL OR di.craft = '')
  AND ci.craft IS NOT NULL
  AND ci.craft <> '';

-- 4) 历史报价行回填：优先设计分项快照（上一步刚回填完），否则取材料库
UPDATE quote_line ql
JOIN db_design_item di ON di.item_id = ql.design_item_id
SET ql.craft = di.craft
WHERE (ql.craft IS NULL OR ql.craft = '')
  AND di.craft IS NOT NULL
  AND di.craft <> '';

UPDATE quote_line ql
JOIN catalog_item ci ON ci.item_id = ql.catalog_item_id
SET ql.craft = ci.craft
WHERE (ql.craft IS NULL OR ql.craft = '')
  AND ci.craft IS NOT NULL
  AND ci.craft <> '';

-- 回滚（人工确认后执行）：
-- ALTER TABLE quote_line DROP COLUMN craft;
-- ALTER TABLE db_design_item DROP COLUMN craft;
