-- 109 房屋区域实测档案与方案区域来源回滚脚本
-- 适用：仅用于隔离/dev/test 演练或明确回退；执行前必须先备份。
-- 注意：回滚删除房屋区域实测档案整表（会丢失区域实测事实），并移除方案区域的值来源与留痕列；
--       已实例化方案的 area/perimeter/area_source 保持不变。

drop table if exists db_property_region;

ALTER TABLE db_design_space
    DROP COLUMN override_at,
    DROP COLUMN override_by_id,
    DROP COLUMN inherited_perimeter,
    DROP COLUMN inherited_area,
    DROP COLUMN house_region_id,
    DROP COLUMN value_origin;
