-- 117 主材下单作业单回滚脚本
-- 适用：仅用于隔离/dev/test 演练或明确回退；执行前必须先备份。
-- 前置：db_material_order_sheet / _item 中不得存在业务数据（回滚会整表删除）；
--       若已有数据，必须先人工导出归档。
-- 注意：回滚后实体字段与库结构将不一致，**必须同时回退代码**（否则 ddl-auto=validate 启动失败）。

-- 4) 既有表撤销加列
ALTER TABLE db_finance_payable
  DROP COLUMN settled_amount,
  DROP COLUMN estimated;

ALTER TABLE db_purchase_order_item
  DROP COLUMN settlement_price,
  DROP COLUMN price_state;

ALTER TABLE db_purchase_order
  DROP COLUMN price_state;

-- 3) 作业单行
DROP TABLE IF EXISTS db_material_order_sheet_item_attachments;
DROP TABLE IF EXISTS db_material_order_sheet_item;

-- 2) 作业单
DROP TABLE IF EXISTS db_material_order_sheet_attachments;
DROP TABLE IF EXISTS db_material_order_sheet;

-- 1) 单据模板
DROP TABLE IF EXISTS db_material_sheet_template;

-- 回滚后请把 scripts/build-release.sh 的 MIGRATION_END 回调到 116。
