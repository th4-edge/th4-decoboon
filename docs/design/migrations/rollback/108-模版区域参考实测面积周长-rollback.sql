-- 108 模版区域参考实测面积周长回滚脚本
-- 适用：仅用于隔离/dev/test 演练或明确回退；执行前必须先备份。
-- 注意：回滚删除两列即丢弃模版层的参考实测值；已实例化方案的区域数据不受影响。

ALTER TABLE design_template_room
    DROP COLUMN reference_perimeter,
    DROP COLUMN reference_area;
