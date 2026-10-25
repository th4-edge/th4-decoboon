-- 125 项目阶段验收模式快照
-- 口径：DEC-087 / BR-053 / T12-BIZ-20261022-008 / PROJ-STATE-D1
-- 目的：把合同施工阶段快照的 acceptance_mode 复制到执行阶段，供阶段详情/列表回读。
-- 兼容：历史阶段仅按 db_contract_construction_stage.project_stage_id 可靠回填；无法关联的保持 NULL，页面显示“待核验”，不得猜测。
-- 回滚：docs/design/migrations/rollback/125-项目阶段验收模式-rollback.sql。

ALTER TABLE db_project_stage
    ADD COLUMN acceptance_mode ENUM('None', 'ConstructionDone', 'AcceptanceDone') NULL
        COMMENT '阶段验收模式（合同施工计划快照继承；历史缺失待核验）'
        AFTER weight;

UPDATE db_project_stage p
JOIN db_contract_construction_stage c
  ON c.project_stage_id = p.stage_id
SET p.acceptance_mode = c.acceptance_mode
WHERE p.acceptance_mode IS NULL;
