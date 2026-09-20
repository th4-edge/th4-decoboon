-- 阶段排期、任务/阶段验收证据、整改责任与量房附件迁移。
-- 适用：MySQL 8.0+。
-- 说明：
-- 1. 阶段计划日期继续使用 db_project_stage 现有字段，不新增表。
-- 2. 验收整改只保存责任员工与截止日期，问题文本继续使用 db_acceptance_issue。
-- 3. 量房现场照片继续使用 db_measure_site_photos，本迁移新增结构图和附件集合表。

ALTER TABLE db_acceptance
    ADD COLUMN rectification_owner_id BIGINT NULL COMMENT '整改责任人',
    ADD COLUMN rectification_due_date DATE NULL COMMENT '整改截止日期';

ALTER TABLE db_acceptance
    ADD KEY idx_acceptance_rectification_owner (rectification_owner_id, rectification_due_date, acceptance_id);

CREATE TABLE IF NOT EXISTS db_measure_structure_images (
    measure_id BIGINT NOT NULL COMMENT '量房记录',
    url VARCHAR(512) NOT NULL COMMENT '结构图或原始图纸地址',
    KEY idx_measure_structure_image (measure_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='量房结构图与原始图纸';

CREATE TABLE IF NOT EXISTS db_measure_attachments (
    measure_id BIGINT NOT NULL COMMENT '量房记录',
    url VARCHAR(512) NOT NULL COMMENT '量房附件地址',
    KEY idx_measure_attachment (measure_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='量房附件';
