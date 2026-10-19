-- 120 设计分项工程量公式拆分回滚脚本
-- 适用：仅用于隔离/dev/test 演练或明确回退；执行前必须先备份。
-- 回滚前确认应用已停止写入 quantity_formula，否则会丢失新的工程量公式数据。

ALTER TABLE db_design_item
    DROP COLUMN quantity_formula;
