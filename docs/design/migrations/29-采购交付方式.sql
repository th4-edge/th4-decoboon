-- 采购交付方式：默认供应商直送工地，只有明确入仓的采购才进入库存。
-- 适用：MySQL 8.0+。

ALTER TABLE db_decoboon_purchase_order
    ADD COLUMN delivery_mode VARCHAR(16) NOT NULL DEFAULT 'Direct' COMMENT '交付方式：Direct=直送工地，Warehouse=入仓库'
    AFTER supplier_name;

UPDATE db_decoboon_purchase_order
SET delivery_mode = 'Direct'
WHERE delivery_mode IS NULL OR delivery_mode = '';
