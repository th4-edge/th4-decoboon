-- 132 · 主材采购明细关联作业单行
-- 依据：OBJ-008、BR-045、DEC-072、DEC-073、SCN-021、DEF-20261102-005
-- 目的：采购明细可追溯到来源主材作业单行；到场状态按当前作业单行对应的采购明细累计，
--       不再按 MaterialPlan 下所有历史采购累计，避免首批部分到场被误判为已到场。
-- 适用：MySQL 8.0+。生产执行前必须备份、登记迁移账本并完成隔离库回滚演练。

ALTER TABLE db_purchase_order_item
  ADD COLUMN sheet_item_id BIGINT NULL COMMENT '来源主材作业单行；历史无法唯一判定时为空' AFTER material_plan_id,
  ADD KEY idx_purchase_item_company_sheet_item (com_id, sheet_item_id, purchase_item_id),
  ADD CONSTRAINT fk_purchase_item_sheet_item
    FOREIGN KEY (sheet_item_id) REFERENCES db_material_order_sheet_item (item_id);

-- 仅回填可以唯一判定的历史采购明细：同一企业 + 同一材料计划只对应一条作业单行。
-- 多作业单/多行共用一个材料计划的模糊历史保持 NULL，必须输出清单人工核对，禁止按时间或数量猜配。
UPDATE db_purchase_order_item poi
JOIN (
  SELECT com_id, material_plan_id, MIN(item_id) AS sheet_item_id, COUNT(*) AS line_count
  FROM db_material_order_sheet_item
  GROUP BY com_id, material_plan_id
) uniq
  ON uniq.com_id = poi.com_id
 AND uniq.material_plan_id = poi.material_plan_id
 AND uniq.line_count = 1
SET poi.sheet_item_id = uniq.sheet_item_id
WHERE poi.sheet_item_id IS NULL;

-- 人工核对查询（迁移不自动处理）：
-- SELECT poi.com_id, poi.purchase_item_id, poi.purchase_order_id, poi.material_plan_id,
--        COUNT(DISTINCT mosi.item_id) AS candidate_sheet_lines
-- FROM db_purchase_order_item poi
-- JOIN db_material_order_sheet_item mosi
--   ON mosi.com_id = poi.com_id AND mosi.material_plan_id = poi.material_plan_id
-- WHERE poi.sheet_item_id IS NULL
-- GROUP BY poi.com_id, poi.purchase_item_id, poi.purchase_order_id, poi.material_plan_id
-- HAVING candidate_sheet_lines > 1;
