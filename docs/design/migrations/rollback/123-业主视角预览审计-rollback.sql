-- 回滚：123-业主视角预览审计.sql
-- 说明：仅删除本功能新增的独立审计表，不改动任何现有业务表或业务状态。
drop table if exists db_project_owner_preview_log;
