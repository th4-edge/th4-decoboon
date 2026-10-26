-- 128 报价项目库 revision、不可变变更审计与报价来源修订
-- 口径：DEC-083 §2、BR-051、DEC-079、T12-BIZ-20261022-004
-- 策略：CatalogItem 保持 Active/Inactive；每次有效变更递增 revision 并写不可变审计；
--       报价行保存 catalogSourceId/code/revision 与既有业务值快照。

ALTER TABLE catalog_item
    ADD COLUMN revision INT NOT NULL DEFAULT 1 COMMENT '有效修订号' AFTER sort;

CREATE TABLE catalog_item_revision (
    revision_id BIGINT NOT NULL,
    create_date INTEGER,
    create_date_time BIGINT,
    create_day INTEGER,
    create_hour INTEGER,
    create_minute INTEGER,
    create_month INTEGER,
    create_second INTEGER,
    create_time INTEGER,
    create_week TINYINT,
    create_year INTEGER,
    update_date INTEGER,
    update_date_time BIGINT,
    update_day INTEGER,
    update_hour INTEGER,
    update_minute INTEGER,
    update_month INTEGER,
    update_second INTEGER,
    update_time INTEGER,
    update_week TINYINT,
    update_year INTEGER,
    com_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    item_code VARCHAR(64),
    revision INT NOT NULL,
    change_type ENUM('Create','Update','Disable','Enable','Delete') NOT NULL,
    snapshot_json LONGTEXT NOT NULL,
    snapshot_hash VARCHAR(64) NOT NULL,
    changed_by_id BIGINT,
    changed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (revision_id),
    UNIQUE KEY uk_catalog_revision_item_revision (com_id, item_id, revision),
    KEY idx_catalog_revision_company_item (com_id, item_id, revision),
    KEY idx_catalog_revision_company_time (com_id, changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报价项目不可变修订审计';

INSERT INTO catalog_item_revision (
    revision_id, com_id, item_id, item_code, revision, change_type, snapshot_json, snapshot_hash,
    changed_by_id, changed_at
)
SELECT
    item_id,
    com_id,
    item_id,
    code,
    1,
    'Create',
    JSON_OBJECT(
        'itemId', item_id,
        'type', type,
        'code', code,
        'category', category,
        'brand', brand,
        'series', series,
        'spec', spec,
        'name', name,
        'unit', unit,
        'pricingMode', pricing_mode,
        'materialPrice', material_price,
        'manualPrice', manual_price,
        'fixedTotalPrice', fixed_total_price,
        'costPrice', cost_price,
        'formula', formula,
        'craft', craft,
        'criterion', criterion,
        'description', description,
        'active', active,
        'sort', sort,
        'sourceType', source_type,
        'revision', 1
    ),
    SHA2(JSON_OBJECT(
        'itemId', item_id,
        'type', type,
        'code', code,
        'category', category,
        'brand', brand,
        'series', series,
        'spec', spec,
        'name', name,
        'unit', unit,
        'pricingMode', pricing_mode,
        'materialPrice', material_price,
        'manualPrice', manual_price,
        'fixedTotalPrice', fixed_total_price,
        'costPrice', cost_price,
        'formula', formula,
        'craft', craft,
        'criterion', criterion,
        'description', description,
        'active', active,
        'sort', sort,
        'sourceType', source_type,
        'revision', 1
    ), 256),
    maintainer_id,
    NOW(6)
FROM catalog_item;

ALTER TABLE quote_line
    ADD COLUMN catalog_source_id BIGINT NULL COMMENT '报价项目来源索引' AFTER catalog_item_id,
    ADD COLUMN catalog_source_code VARCHAR(64) NULL COMMENT '报价项目来源编码' AFTER catalog_source_id,
    ADD COLUMN catalog_source_revision INT NULL COMMENT '报价项目来源修订号' AFTER catalog_source_code;

UPDATE quote_line ql
JOIN catalog_item ci ON ci.item_id = ql.catalog_item_id
SET ql.catalog_source_id = ci.item_id,
    ql.catalog_source_code = ci.code,
    ql.catalog_source_revision = ci.revision
WHERE ql.catalog_item_id IS NOT NULL;

CREATE INDEX idx_quote_line_catalog_source
    ON quote_line (com_id, catalog_source_id, catalog_source_revision);
