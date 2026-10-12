-- 109 量房原始区域与方案区域来源回滚脚本
-- 适用：仅用于隔离/dev/test 演练或明确回退；执行前必须先备份。
-- 注意：回滚删除量房原始区域整表（丢失原始结构实测事实），并移除方案区域的来源列；
--       已实例化方案的 area/perimeter/area_source 保持不变。

drop table if exists db_measure_region;

ALTER TABLE db_design_space
    DROP COLUMN source_measure_region_id,
    DROP COLUMN value_origin;
