-- 119-结算撤销留痕
-- 日期：2026-10-15
-- 适用：MySQL 8.0+。存量库执行前请先备份；空库部署请同步使用 01-空库基线.sql。
--
-- 依据：用户 2026-10-15 裁决「结算撤销本轮做」+ DEC-073（结算价一经确认冻结；再调整走新结算版本）。
-- 语义：撤销是**纠错**动作——结算头保留并置 Cancelled（记录撤销人/时间/原因，审计可追溯），
--       结算明细删除以释放「同一采购明细只能结算一次」的唯一约束，允许重新结算；
--       同时按原结算价冲回采购明细结算价、价格状态、应付（回暂估）与已补记的实际成本，
--       使项目成本重新判为不完整（利润为 null）。
--
-- 回滚：见 docs/design/migrations/rollback/119-结算撤销留痕-rollback.sql

ALTER TABLE db_material_settlement
  ADD COLUMN cancelled_by  BIGINT       NULL COMMENT '撤销人',
  ADD COLUMN cancelled_at  DATETIME     NULL COMMENT '撤销时间',
  ADD COLUMN cancel_reason VARCHAR(500) NULL COMMENT '撤销原因',
  ADD KEY idx_settlement_company_cancelled_by (com_id, cancelled_by, settlement_id);
