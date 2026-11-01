-- 132 · 客户工作区分页索引
-- 项目经理客户工作区按 (com_id, manager_id) 反查项目关联客户。
-- 只增索引，不改数据；上线前必须备份并按迁移账本执行。

ALTER TABLE db_project
  ADD INDEX idx_project_company_manager (com_id, manager_id, project_id);
