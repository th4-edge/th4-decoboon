-- 106 区域实测面积周长回滚脚本
-- 适用：仅用于隔离/dev/test 演练或明确回退；执行前必须先备份。
-- 注意：回滚会把未录入（NULL）的面积/周长写成 0，并恢复 NOT NULL 约束；
--       已录入的实测值保持不变，area_source 标记随列删除。

UPDATE db_design_space SET area = 0 WHERE area IS NULL;
UPDATE db_design_space SET perimeter = 0 WHERE perimeter IS NULL;

ALTER TABLE db_design_space
    DROP COLUMN area_source,
    MODIFY COLUMN area DECIMAL(12, 2) NOT NULL,
    MODIFY COLUMN perimeter DECIMAL(12, 2) NOT NULL;
