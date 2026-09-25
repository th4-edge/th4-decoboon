-- MH-FULL-ROLE-20260924 本地夹具修复记录（已执行）
-- 目标库：127.0.0.1:3306/decoboon，仅用于本次测试。
USE decoboon;

START TRANSACTION;

-- 1) 清理旧导入脚本分页幂等缺陷造成的 122 条重复报价项。
-- 每组 type/category/name 保留最小 item_id；已确认未被子业务引用。
DROP TABLE IF EXISTS _audit_dup_delete;
CREATE TABLE _audit_dup_delete AS
SELECT c.item_id
FROM catalog_item c
JOIN (
  SELECT type, category, name, MIN(item_id) keep_id, COUNT(*) cnt
  FROM catalog_item
  GROUP BY type, category, name
  HAVING COUNT(*) > 1
) d ON d.type = c.type AND d.category = c.category AND d.name = c.name
WHERE c.item_id <> d.keep_id;
DELETE FROM catalog_construction_detail WHERE item_id IN (SELECT item_id FROM _audit_dup_delete);
DELETE FROM catalog_custom_detail WHERE item_id IN (SELECT item_id FROM _audit_dup_delete);
DELETE FROM catalog_material_detail WHERE item_id IN (SELECT item_id FROM _audit_dup_delete);
DELETE FROM catalog_item WHERE item_id IN (SELECT item_id FROM _audit_dup_delete);
DROP TABLE _audit_dup_delete;

-- 2) 清理套餐模板分项保存失败后留下的不完整空模板。
DELETE FROM pack_section_space_types
WHERE package_section_group_id IN (SELECT group_id FROM pack_section WHERE com_id = 1790000000000000001);
DELETE FROM pack_section WHERE com_id = 1790000000000000001;
DELETE FROM pack_template_criterions
WHERE package_template_id IN (SELECT template_id FROM pack_template WHERE com_id = 1790000000000000001);
DELETE FROM pack_template WHERE com_id = 1790000000000000001;

-- 3) 清理业务员越权新增设计的负向测试记录。
DELETE FROM db_design WHERE design_id = 228677602972270592;

-- 4) 清理 UI 重试产生的无依赖重复客户及通知。
DELETE FROM db_notice
WHERE customer_id IN (228676083493371904, 228676141425098752, 228676261193449472);
DELETE FROM cust_customer
WHERE cust_id IN (228676083493371904, 228676141425098752, 228676261193449472);

COMMIT;

-- 最终期望：catalog_item=550；pack_template=0；保留 UI 客户 cust_id=228676037641240576。
SELECT COUNT(*) AS catalog_count FROM catalog_item WHERE com_id = 1790000000000000001;
SELECT COUNT(*) AS package_count FROM pack_template WHERE com_id = 1790000000000000001;
SELECT COUNT(*) AS ui_customer_count FROM cust_customer WHERE name LIKE 'UI测试%';
