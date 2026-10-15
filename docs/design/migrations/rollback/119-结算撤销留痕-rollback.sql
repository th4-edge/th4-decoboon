-- 119 结算撤销留痕回滚脚本
-- 适用：仅用于隔离/dev/test 演练或明确回退；执行前必须先备份。
-- 前置：若已存在撤销记录（status='Cancelled' 且 cancelled_at 非空），回滚会丢失撤销留痕，需先导出。
-- 注意：回滚后必须同时回退代码，否则 ddl-auto=validate 启动失败。

ALTER TABLE db_material_settlement
  DROP KEY idx_settlement_company_cancelled_by,
  DROP COLUMN cancel_reason,
  DROP COLUMN cancelled_at,
  DROP COLUMN cancelled_by;
