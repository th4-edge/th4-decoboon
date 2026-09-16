-- 报价项目拼音检索（P1）
--
-- 目的：报价项目名称支持拼音首字母检索，输入 QGLG 即可命中「轻钢龙骨石膏板吊平顶」。
-- 生成规则：由应用层 PinyinContext 在保存/更新时写入 name_initials（中文取拼音首字母，字母数字保留大写）。
--
-- 1) 字段与索引（com_id 最左）
ALTER TABLE catalog_item
  ADD COLUMN name_initials VARCHAR(64) NULL COMMENT '名称拼音首字母，如 QGLGSGBDPD' AFTER name;

ALTER TABLE catalog_item
  ADD KEY idx_catalog_item_initials (com_id, name_initials);

-- 2) 存量数据回填
--    拼音无法用纯 SQL 生成，请在应用发布后执行一次性回填（示例思路）：
--      for (CatalogItem item : repository.findByCompany_ComId(comId)) {
--          item.setNameInitials(pinyinContext.initials(item.getName()));
--      }
--    或提供管理端「重建拼音索引」入口，分批（每批 500 条）执行以避免长事务。
