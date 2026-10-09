-- 96-阶段本项不涉及.sql
-- GAP-039 / DEC-057：空阶段区分「本项不涉及」与「应有任务但缺失」。
-- 本项不涉及由 PM/Boss/店长判定并留痕（原因/判定人/时间），从项目进度分母与阶段权重剔除。
-- db_project_stage.status 为 VARCHAR(16)，新增 StageStatus.NotApplicable 无需改列类型。
-- 注意：本文件尚未对目标 MySQL 或生产库执行。

ALTER TABLE db_project_stage
    ADD COLUMN not_applicable_reason VARCHAR(255) NULL COMMENT '本项不涉及判定原因',
    ADD COLUMN not_applicable_by BIGINT NULL COMMENT '本项不涉及判定人',
    ADD COLUMN not_applicable_at DATETIME NULL COMMENT '本项不涉及判定时间';

-- 回滚：
-- ALTER TABLE db_project_stage
--     DROP COLUMN not_applicable_reason,
--     DROP COLUMN not_applicable_by,
--     DROP COLUMN not_applicable_at;
