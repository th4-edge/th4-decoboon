-- 120-设计分项工程量公式拆分.sql
-- 目的：把设计分项的两个公式概念拆开（DEC-063 / GAP-042）：
--   formula            = 报价项目库带入的「计价公式说明」，例如 (材料单价) × 工程量；
--   quantity_formula   = 工程量自动计算公式，只允许 A/P/WP/CP 等专业变量。
-- 影响表：db_design_item（新增可空列）。
-- 回滚：docs/design/migrations/rollback/120-设计分项工程量公式拆分-rollback.sql
-- 注意：ddl-auto=update 不会补列；必须显式执行本迁移。

ALTER TABLE db_design_item
    ADD COLUMN quantity_formula VARCHAR(512) NULL COMMENT '工程量公式（专业变量）' AFTER formula;

-- 只迁移已经是专业变量语法的历史公式；中文计价说明不进入 quantity_formula。
-- 生产当前没有专业变量公式的存量设计分项，本更新预计 0 行；dev/test 若有可平滑承接。
UPDATE db_design_item
SET quantity_formula = TRIM(formula)
WHERE quantity_source = 'Formula'
  AND formula IS NOT NULL
  AND TRIM(formula) <> ''
  AND formula REGEXP '^[=A-Za-z0-9_+*/() .,#-]+$';

-- 拆分前实例化的「待补尺寸」把标记写在 formula；迁移到 quantity_formula。
-- formula 原值保留，便于回滚；新保存路径会用报价项目库的计价说明覆盖它。
UPDATE db_design_item
SET quantity_formula = TRIM(formula)
WHERE LEFT(formula, 20) = '__PENDING_MEASURE__|'
  AND (quantity_formula IS NULL OR TRIM(quantity_formula) = '');
