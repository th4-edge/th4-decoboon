-- 111-阶段状态补充本项不涉及.sql
-- 目的：修复 GAP-054 —— DEC-057 / GAP-039 引入 StageStatus.NotApplicable（「本项不涉及」）后，
--       db_project_stage.status 的 ENUM 未同步扩展；合同生效建阶段时，凡「模板阶段在本项目没有
--       合同施工行」者由 ProjectContext 自动判为 NotApplicable 并写库 → MySQL 报
--       `Data truncated for column 'status'` → 生效整事务回滚（业务侧只看到「未定义错误信息」）。
--       触发面：任何存在"无合同施工行的阶段"的合同在生效时必然失败（新装企首份合同极易触发）。
-- 影响表：db_project_stage —— 仅扩展 status 的取值集合，不改列类型语义、不迁移数据。
-- 回滚：docs/design/migrations/rollback/111-阶段状态补充本项不涉及-rollback.sql
-- 注意：prod 为 `ddl-auto: validate`；执行后必须重启服务并确认启动通过（对齐 2026-10-12 迁移事故教训）。
-- 关联：docs/design/migrations/96-阶段本项不涉及.sql（其注释误判 status 为 VARCHAR(16)，故漏了本处扩展）。

alter table db_project_stage
  modify column status enum('Accepting','Doing','Done','Todo','NotApplicable') not null;

-- 核对（应返回含 NotApplicable 的 5 值）：
-- select column_type from information_schema.columns
--   where table_schema = database() and table_name = 'db_project_stage' and column_name = 'status';
