-- 134 rollback · 主材采购明细关联作业单行
-- 前置：确认不存在依赖 sheet_item_id 的生产业务逻辑，并与代码同步回退。
ALTER TABLE db_purchase_order_item
  DROP FOREIGN KEY fk_purchase_item_sheet_item,
  DROP INDEX idx_purchase_item_company_sheet_item,
  DROP COLUMN sheet_item_id;
