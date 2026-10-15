-- 118 主材采购结算回滚脚本
-- 适用：仅用于隔离/dev/test 演练或明确回退；执行前必须先备份。
-- 前置：db_material_settlement / _item 中的数据若已生效，回滚会丢失结算事实，必须先导出归档。
-- 注意：回滚后采购明细 settlement_price、应付 estimated/settled_amount 的既有值不会自动还原，
--       如需还原请用结算前备份；且必须同时回退代码（否则 ddl-auto=validate 启动失败）。

DROP TABLE IF EXISTS db_material_settlement_item;
DROP TABLE IF EXISTS db_material_settlement;

-- 回滚后请确认 scripts/build-release.sh 的 MIGRATION_END 与已执行账本一致。
