-- 93-项目预计完工日.sql
-- DEC-048：合同基准完工日锁定在 db_project_contract.plan_end_date；
-- db_project.expected_end_date 承载任务排期驱动的动态“预计完工日”，不得回写合同基准。
-- 注意：本文件尚未对目标 MySQL 或生产库执行。

ALTER TABLE db_project
    ADD COLUMN expected_end_date DATE NULL COMMENT '预计完工日（动态，任务排期驱动）';

UPDATE db_project
SET expected_end_date = plan_end_date
WHERE expected_end_date IS NULL;
