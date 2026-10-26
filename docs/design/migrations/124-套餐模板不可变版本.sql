-- 124 套餐模板不可变版本与报价来源版本
-- 口径：DEC-083 / BR-051 / T12-BIZ-20261022-004
-- 策略：每条 pack_template 记录代表一个版本；同一 (com_id, code) 下版本递增。
--       发布版本不可原地修改；is_current_published 标识唯一可承接新报价的发布版本。
-- 兼容：有项目行的存量套餐固化 version=1、Published、current=1；无项目行套餐降为 Draft/current=0；不伪造更早版本。

ALTER TABLE pack_template
    ADD COLUMN code VARCHAR(64) NULL COMMENT '套餐编码（同一编码下版本递增）' AFTER template_id,
    ADD COLUMN version INT NOT NULL DEFAULT 1 COMMENT '套餐版本' AFTER code,
    ADD COLUMN status ENUM('Draft', 'Published', 'Disabled') NOT NULL DEFAULT 'Draft' COMMENT '套餐状态' AFTER version,
    ADD COLUMN is_current_published BIT NOT NULL DEFAULT 0 COMMENT '是否当前发布版本' AFTER status,
    ADD COLUMN current_published_code VARCHAR(64) GENERATED ALWAYS AS (CASE WHEN is_current_published = 1 THEN code ELSE NULL END) STORED COMMENT '当前发布编码唯一约束投影' AFTER is_current_published,
    ADD COLUMN published_at DATETIME(6) NULL COMMENT '发布时间' AFTER is_current_published,
    ADD COLUMN published_by_id BIGINT NULL COMMENT '发布人' AFTER published_at,
    ADD COLUMN snapshot_json LONGTEXT NULL COMMENT '发布快照内容' AFTER published_by_id,
    ADD COLUMN snapshot_hash VARCHAR(64) NULL COMMENT '发布快照SHA-256' AFTER snapshot_json;

UPDATE pack_template
SET code = CONCAT('PACK-', template_id)
WHERE code IS NULL;

UPDATE pack_template
SET status = 'Published',
    is_current_published = 1
WHERE code IS NOT NULL;

-- 无项目行的存量套餐不具备发布条件，降为草稿，避免成为新报价来源。
UPDATE pack_template t
LEFT JOIN (
    SELECT DISTINCT template_id
    FROM pack_line
) l ON l.template_id = t.template_id
SET t.status = 'Draft',
    t.is_current_published = 0
WHERE l.template_id IS NULL;

ALTER TABLE pack_template
    MODIFY COLUMN code VARCHAR(64) NOT NULL COMMENT '套餐编码（同一编码下版本递增）';

CREATE UNIQUE INDEX uk_pack_template_code_version
    ON pack_template (com_id, code, version);
CREATE UNIQUE INDEX uk_pack_template_current_version
    ON pack_template (com_id, current_published_code);
CREATE INDEX idx_pack_template_code_version
    ON pack_template (com_id, code, version DESC);
CREATE INDEX idx_pack_template_current
    ON pack_template (com_id, status, is_current_published, code);
CREATE INDEX idx_pack_template_published_by
    ON pack_template (published_by_id);

ALTER TABLE pack_line
    ADD COLUMN section_name_snapshot VARCHAR(64) NULL COMMENT '分区名称快照' AFTER section_id,
    ADD COLUMN catalog_type_snapshot VARCHAR(20) NULL COMMENT '报价项目类型快照' AFTER catalog_item_id,
    ADD COLUMN catalog_category_snapshot VARCHAR(64) NULL COMMENT '报价项目分类快照' AFTER catalog_type_snapshot,
    ADD COLUMN craft_snapshot LONGTEXT NULL COMMENT '施工工艺快照' AFTER catalog_category_snapshot,
    ADD COLUMN cost_price_snapshot DECIMAL(12, 2) NULL COMMENT '标准成本快照' AFTER craft_snapshot;

UPDATE pack_line l
JOIN pack_section s ON s.group_id = l.section_id
LEFT JOIN catalog_item c ON c.item_id = l.catalog_item_id
SET l.section_name_snapshot = s.name,
    l.catalog_type_snapshot = c.type,
    l.catalog_category_snapshot = c.category,
    l.craft_snapshot = c.craft,
    l.cost_price_snapshot = c.cost_price;

UPDATE pack_template t
SET t.snapshot_json = JSON_OBJECT(
        'templateId', t.template_id,
        'code', t.code,
        'version', t.version,
        'name', t.name,
        'area', t.area,
        'price', t.price,
        'discount', t.discount,
        'cost', t.cost,
        'criterions', COALESCE((
            SELECT JSON_ARRAYAGG(c.criterions)
            FROM pack_template_criterions c
            WHERE c.package_template_id = t.template_id
        ), JSON_ARRAY()),
        'lines', COALESCE((
            SELECT JSON_ARRAYAGG(JSON_OBJECT(
                'itemId', l.item_id,
                'sectionId', l.section_id,
                'sectionName', l.section_name_snapshot,
                'catalogItemId', l.catalog_item_id,
                'catalogType', l.catalog_type_snapshot,
                'catalogCategory', l.catalog_category_snapshot,
                'craft', l.craft_snapshot,
                'costPrice', l.cost_price_snapshot,
                'name', l.name,
                'brand', l.brand,
                'spec', l.spec,
                'unit', l.unit,
                'quantity', l.quantity,
                'quantityFactor', l.quantity_factor,
                'pricingMethod', l.pricing_method,
                'included', l.included,
                'deductible', l.deductible,
                'sort', l.sort,
                'criterions', COALESCE((
                    SELECT JSON_ARRAYAGG(lc.criterions)
                    FROM pack_line_criterions lc
                    WHERE lc.item_id = l.item_id
                ), JSON_ARRAY())
            ))
            FROM pack_line l
            WHERE l.template_id = t.template_id
        ), JSON_ARRAY()),
        'measures', COALESCE((
            SELECT JSON_ARRAYAGG(JSON_OBJECT(
                'itemId', ti.item_id,
                'name', ti.name,
                'unit', ti.unit,
                'priority', ti.priority,
                'price', m.price,
                'standard', m.standard,
                'remark', m.remark
            ))
            FROM pack_template_measures m
            JOIN pack_template_item ti ON ti.item_id = m.item_id
            WHERE m.package_template_id = t.template_id
        ), JSON_ARRAY())
    )
WHERE t.snapshot_json IS NULL;

UPDATE pack_template
SET snapshot_hash = SHA2(snapshot_json, 256)
WHERE snapshot_json IS NOT NULL
  AND snapshot_hash IS NULL;

ALTER TABLE quote
    ADD COLUMN package_template_code VARCHAR(64) NULL COMMENT '套餐编码快照' AFTER package_template_id,
    ADD COLUMN package_template_version INT NULL COMMENT '套餐版本快照' AFTER package_template_code,
    ADD COLUMN package_template_snapshot_hash VARCHAR(64) NULL COMMENT '套餐发布快照哈希' AFTER package_template_version;

UPDATE quote q
JOIN pack_template t ON t.template_id = q.package_template_id
SET q.package_template_code = t.code,
    q.package_template_version = t.version,
    q.package_template_snapshot_hash = t.snapshot_hash
WHERE q.package_template_id IS NOT NULL;

ALTER TABLE quote_line
    ADD COLUMN package_section_name_snapshot VARCHAR(64) NULL COMMENT '套餐分区名称快照' AFTER category;

UPDATE quote_line ql
JOIN pack_line pl ON pl.item_id = ql.package_line_id
SET ql.package_section_name_snapshot = pl.section_name_snapshot
WHERE ql.package_line_id IS NOT NULL;

CREATE INDEX idx_quote_package_version
    ON quote (com_id, package_template_code, package_template_version);
