-- 回滚：仅删除目标企业 source_type='Migration' 的复制条目及其明细；
-- 不影响目标企业原有条目，也不影响来源企业。
SET @src := 1790066191585976744;
SET @tgt := 1789938943485101774;
START TRANSACTION;
CREATE TEMPORARY TABLE rmap (item_id BIGINT NOT NULL PRIMARY KEY) ENGINE=InnoDB SELECT item_id FROM catalog_item WHERE com_id = @tgt AND source_type = 'Migration';
SELECT CONCAT('rollback_target_items=', COUNT(*)) FROM rmap;
DELETE d FROM `catalog_construction_detail` d JOIN rmap m ON m.item_id = d.item_id;  -- Construction
SELECT CONCAT('deleted_catalog_construction_detail=', ROW_COUNT());
DELETE d FROM `catalog_material_detail` d JOIN rmap m ON m.item_id = d.item_id;  -- Material
SELECT CONCAT('deleted_catalog_material_detail=', ROW_COUNT());
DELETE d FROM `catalog_custom_detail` d JOIN rmap m ON m.item_id = d.item_id;  -- Custom
SELECT CONCAT('deleted_catalog_custom_detail=', ROW_COUNT());
DELETE d FROM `catalog_soft_detail` d JOIN rmap m ON m.item_id = d.item_id;  -- Soft
SELECT CONCAT('deleted_catalog_soft_detail=', ROW_COUNT());
DELETE d FROM `catalog_general_detail` d JOIN rmap m ON m.item_id = d.item_id;  -- General
SELECT CONCAT('deleted_catalog_general_detail=', ROW_COUNT());
DELETE d FROM `catalog_appliance_detail` d JOIN rmap m ON m.item_id = d.item_id;  -- Appliance
SELECT CONCAT('deleted_catalog_appliance_detail=', ROW_COUNT());
DELETE i FROM catalog_item i JOIN rmap m ON m.item_id = i.item_id;
SELECT CONCAT('deleted_items=', ROW_COUNT());
COMMIT;
-- 表级恢复（如需）：mysqldump 备份见 manifest.json；明细表：`catalog_construction_detail`, `catalog_material_detail`, `catalog_custom_detail`, `catalog_soft_detail`, `catalog_general_detail`, `catalog_appliance_detail`
