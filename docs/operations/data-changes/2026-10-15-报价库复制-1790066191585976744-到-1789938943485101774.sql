SET SESSION sql_mode='STRICT_ALL_TABLES';
START TRANSACTION;
SET @src := 1790066191585976744;
SET @tgt := 1789938943485101774;
SET @maint := 1789938943545297099;
SET @base := 236232845046050816;
CREATE TEMPORARY TABLE cmap (old_id BIGINT NOT NULL PRIMARY KEY, new_id BIGINT NOT NULL) ENGINE=InnoDB SELECT i.item_id AS old_id, @base + ROW_NUMBER() OVER (ORDER BY i.item_id) AS new_id FROM catalog_item i WHERE i.com_id = @src AND NOT EXISTS (SELECT 1 FROM catalog_item t WHERE t.com_id = @tgt AND t.type = i.type AND t.code <=> i.code);
SELECT CONCAT('guard_item_conflict=', COUNT(*)) FROM catalog_item WHERE item_id BETWEEN @base AND @base + 100000;
INSERT INTO catalog_item (item_id, active, brand, category, code, cost_price, craft, criterion, description, formula, manual_price, fixed_total_price, material_price, name, name_initials, pricing_mode, series, sort, spec, type, unit, com_id, import_batch_id, maintainer_id, source_type) SELECT m.new_id, i.active, i.brand, i.category, i.code, i.cost_price, i.craft, i.criterion, i.description, i.formula, i.manual_price, i.fixed_total_price, i.material_price, i.name, i.name_initials, i.pricing_mode, i.series, i.sort, i.spec, i.type, i.unit, @tgt, NULL, @maint, 'Migration' FROM catalog_item i JOIN cmap m ON m.old_id = i.item_id;
SELECT CONCAT('copied_items=', ROW_COUNT());
-- 明细：Construction（catalog_construction_detail）
INSERT INTO catalog_construction_detail (detail_id, concealed, craft, criterion, duration_days, measurement_formula, stage, com_id, item_id) SELECT @base + 100000 + ROW_NUMBER() OVER (ORDER BY d.detail_id), d.concealed, d.craft, d.criterion, d.duration_days, d.measurement_formula, d.stage, @tgt, m.new_id FROM catalog_construction_detail d JOIN cmap m ON m.old_id = d.item_id;
SELECT CONCAT('copied_catalog_construction_detail=', ROW_COUNT());
-- 明细：Material（catalog_material_detail）
INSERT INTO catalog_material_detail (detail_id, color, inventory_mode, loss_rate, material, minimum_order, model, com_id, item_id) SELECT @base + 110000 + ROW_NUMBER() OVER (ORDER BY d.detail_id), d.color, d.inventory_mode, d.loss_rate, d.material, d.minimum_order, d.model, @tgt, m.new_id FROM catalog_material_detail d JOIN cmap m ON m.old_id = d.item_id;
SELECT CONCAT('copied_catalog_material_detail=', ROW_COUNT());
-- 明细：Custom（catalog_custom_detail）
INSERT INTO catalog_custom_detail (detail_id, board, cabinet_type, countertop, door, hardware, parametric_formula, com_id, item_id) SELECT @base + 120000 + ROW_NUMBER() OVER (ORDER BY d.detail_id), d.board, d.cabinet_type, d.countertop, d.door, d.hardware, d.parametric_formula, @tgt, m.new_id FROM catalog_custom_detail d JOIN cmap m ON m.old_id = d.item_id;
SELECT CONCAT('copied_catalog_custom_detail=', ROW_COUNT());
-- 明细：Soft（catalog_soft_detail）
INSERT INTO catalog_soft_detail (detail_id, composition, dimensions, installation_mode, lead_time_days, material, style, com_id, item_id) SELECT @base + 130000 + ROW_NUMBER() OVER (ORDER BY d.detail_id), d.composition, d.dimensions, d.installation_mode, d.lead_time_days, d.material, d.style, @tgt, m.new_id FROM catalog_soft_detail d JOIN cmap m ON m.old_id = d.item_id;
SELECT CONCAT('copied_catalog_soft_detail=', ROW_COUNT());
-- 明细：General（catalog_general_detail）
INSERT INTO catalog_general_detail (detail_id, allocation_mode, approval_required, discountable, fee_type, invoice_type, com_id, item_id) SELECT @base + 140000 + ROW_NUMBER() OVER (ORDER BY d.detail_id), d.allocation_mode, d.approval_required, d.discountable, d.fee_type, d.invoice_type, @tgt, m.new_id FROM catalog_general_detail d JOIN cmap m ON m.old_id = d.item_id;
SELECT CONCAT('copied_catalog_general_detail=', ROW_COUNT());
-- 明细：Appliance（catalog_appliance_detail）
INSERT INTO catalog_appliance_detail (detail_id, after_sales, dimensions, model, power, utility_requirement, warranty_months, com_id, item_id) SELECT @base + 150000 + ROW_NUMBER() OVER (ORDER BY d.detail_id), d.after_sales, d.dimensions, d.model, d.power, d.utility_requirement, d.warranty_months, @tgt, m.new_id FROM catalog_appliance_detail d JOIN cmap m ON m.old_id = d.item_id;
SELECT CONCAT('copied_catalog_appliance_detail=', ROW_COUNT());
SELECT CONCAT('skipped_existing=', (SELECT COUNT(*) FROM catalog_item WHERE com_id = @src) - (SELECT COUNT(*) FROM cmap));
COMMIT;
