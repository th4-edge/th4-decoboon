-- 107-方案面积对齐实测口径.sql
-- 目的：db_design.area 语义改为「区域实测面积合计」（BR-040 §4 / DEC-062 / EV-213）。
--       迁移 106 只标记了区域口径，未重算方案面积；存量 db_design.area 仍可能是旧实现的推算汇总，
--       而该字段是混合报价（HybridQuoteBuilder）的面积比例基数，沿用推算值会让金额建立在估算面积上。
-- 影响表：db_design（area）。
-- 回滚：docs/design/migrations/rollback/107-方案面积对齐实测口径-rollback.sql
-- 注意：执行前必须备份 db_design（见 docs/acceptance/runs/20261010-area-measure-migration/）。

UPDATE db_design d
SET d.area = COALESCE((
        SELECT SUM(s.area)
        FROM db_design_space s
        WHERE s.design_id = d.design_id
          AND s.area_source = 'Measured'
    ), 0);
