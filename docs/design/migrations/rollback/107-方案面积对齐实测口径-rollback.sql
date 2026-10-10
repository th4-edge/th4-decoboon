-- 107 方案面积对齐实测口径 回滚脚本
-- 适用：仅用于隔离/dev/test 演练或明确回退；执行前必须先备份。
-- 注意：无法精确还原迁移前的值——旧值可能是创建时的客户/房屋面积，也可能是推算汇总。
--       这里按「全部区域面积合计」近似恢复旧口径；“从未保存过区域”的方案会得到 0，需用备份还原。

UPDATE db_design d
SET d.area = COALESCE((
        SELECT SUM(s.area)
        FROM db_design_space s
        WHERE s.design_id = d.design_id
    ), 0);
