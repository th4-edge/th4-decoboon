-- 106-区域实测面积周长.sql
-- 目的：区域面积/周长口径改为设计师实测量房后经 CAD 计算录入（BR-040 §4 / DEC-062 / EV-213），
--       服务端不再由开间×进深推算；未录入用 NULL 表达，不能用 0 冒充。
-- 影响表：db_design_space（area/perimeter 允许为空，新增 area_source）。
-- 回滚：docs/design/migrations/rollback/106-区域实测面积周长-rollback.sql
-- 注意：ddl-auto=update 不会修改既有列的可空性与类型，必须显式 ALTER。

ALTER TABLE db_design_space
    MODIFY COLUMN area DECIMAL(12, 2) NULL COMMENT '实测面积',
    MODIFY COLUMN perimeter DECIMAL(12, 2) NULL COMMENT '实测周长',
    ADD COLUMN area_source VARCHAR(16) NULL COMMENT '面积口径来源(Measured/Estimated)';

-- 存量行都是旧实现由长宽推算出来的值：统一标记为估算口径，只用于提示复核，
-- 不作为工程量基数（DesignFormulaRecalcContext 只接受 Measured）。
UPDATE db_design_space
SET area_source = 'Estimated'
WHERE area IS NOT NULL OR perimeter IS NOT NULL;
