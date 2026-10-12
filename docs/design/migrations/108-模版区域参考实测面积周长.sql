-- 108-模版区域参考实测面积周长.sql
-- 目的：设计模版区域新增「参考实测面积/周长」（DEC-064 / ANS-BIZ-20261012-001 / T12-BIZ-20261012-001 第 1 期）。
--       参考值只用于**模版内公式校验**与实例化打底；不承载任何房屋的实测事实，
--       不得作为工程量基数或进入报价金额（金额门禁：只有 Measured 来源可作基数）。
-- 影响表：design_template_room（新增 reference_area / reference_perimeter，均可空 = 未填）。
-- 回滚：docs/design/migrations/rollback/108-模版区域参考实测面积周长-rollback.sql
-- 注意：新增可空列不影响既有行；发布前校验不因缺参考值阻断（DEC-058 待补机制不变）。

ALTER TABLE design_template_room
    ADD COLUMN reference_area DECIMAL(12, 3) NULL COMMENT '参考实测面积(㎡,仅模版内公式校验,非工程量基数)',
    ADD COLUMN reference_perimeter DECIMAL(12, 3) NULL COMMENT '参考实测周长(m,仅模版内公式校验,非工程量基数)';
