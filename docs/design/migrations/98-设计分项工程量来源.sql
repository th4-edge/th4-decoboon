-- 98-设计分项工程量来源.sql
-- 目的：区分公式自动工程量与设计师手工工程量，空间尺寸变化只重算公式量，避免覆盖手工量。
-- 影响表：db_design_item。
-- 回滚：ALTER TABLE db_design_item DROP COLUMN quantity_source;

ALTER TABLE db_design_item
    ADD COLUMN quantity_source varchar(16) NOT NULL DEFAULT 'Formula'
        COMMENT '工程量来源：Formula 公式自动量 / Manual 手工量'
        AFTER quantity;

-- 存量有公式的分项保持 Formula；没有公式的分项视为手工量。
UPDATE db_design_item
SET quantity_source = 'Manual'
WHERE formula IS NULL OR TRIM(formula) = '';

CREATE INDEX idx_design_item_company_design_source
    ON db_design_item (com_id, design_id, quantity_source, item_id);
