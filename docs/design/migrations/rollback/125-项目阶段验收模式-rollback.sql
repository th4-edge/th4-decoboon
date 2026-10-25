-- 回滚 125 项目阶段验收模式
-- 前置：后端已回退到不读取 ProjectStage.acceptanceMode 的版本。
-- 说明：仅移除由合同阶段快照继承的展示字段，不改阶段状态、任务、验收或金额事实。

ALTER TABLE db_project_stage
    DROP COLUMN acceptance_mode;
