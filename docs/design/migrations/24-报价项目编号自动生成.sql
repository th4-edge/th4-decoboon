-- 报价项目编号自动生成（CatalogItem.code）
--
-- 背景：编号由后端按「类型前缀 + 企业内 4 位序号」生成，前端可不传。
--   施工 SG-0001 / 主材 ZC-0001 / 软装 RZ-0001 / 电器 DQ-0001 / 综合 ZH-0001 / 全屋定制 DZ-0001
-- 并发保障：应用层事务内取最大值 +1；本脚本补充唯一约束兜底。
--
-- ⚠️ 执行前检查：历史数据是否存在同企业同类型重复编号（含空串）
--   SELECT com_id, type, code, COUNT(*) c FROM catalog_item
--    WHERE code IS NOT NULL AND code <> '' GROUP BY com_id, type, code HAVING c > 1;
--   若有重复，先人工整理（或在编号后追加 -OLD 保留历史可追溯）。
--
-- 空字符串视为"未填写"，需先规范为 NULL，避免唯一约束把多条空串判为冲突：
UPDATE catalog_item SET code = NULL WHERE code = '';

ALTER TABLE catalog_item
  ADD UNIQUE KEY uk_catalog_item_code (com_id, type, code);

-- 备注：MySQL 唯一索引允许多个 NULL，因此未填写编号的存量数据不会互相冲突。
