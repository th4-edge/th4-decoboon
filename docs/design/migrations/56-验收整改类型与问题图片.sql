-- 56-验收整改类型与问题图片.sql
-- 用户 2026-09-23 裁决：
-- 1. 业主提交整改要求时支持上传多张问题图片；
-- 2. 业主和业务人员不能直接设置整改截止日期；
-- 3. 后端按整改类型固定计算截止日期：施工整改 3 天、设计整改 5 天；
-- 4. 整改责任人固定为当前项目项目经理。
-- 适用：MySQL 8.0+。

SET @db_name = DATABASE();

-- 加列前查 information_schema，允许迁移在部分执行失败后安全重试。
SET @has_acceptance_rectification_type = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @db_name
      AND table_name = 'db_acceptance'
      AND column_name = 'rectification_type'
);
SET @sql = IF(@has_acceptance_rectification_type = 0,
    'ALTER TABLE db_acceptance ADD COLUMN rectification_type VARCHAR(24) NULL COMMENT ''整改类型（Construction=施工整改 3 天，Design=设计整改 5 天）''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_round_rectification_type = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @db_name
      AND table_name = 'db_acceptance_round'
      AND column_name = 'rectification_type'
);
SET @sql = IF(@has_round_rectification_type = 0,
    'ALTER TABLE db_acceptance_round ADD COLUMN rectification_type VARCHAR(24) NULL COMMENT ''整改类型（Construction=施工整改 3 天，Design=设计整改 5 天）''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS db_acceptance_rectification_issue_image (
    acceptance_id BIGINT NOT NULL COMMENT '阶段验收单',
    url VARCHAR(512) NOT NULL COMMENT '业主提出的整改问题图片',
    KEY idx_acceptance_rectification_issue_image (acceptance_id),
    CONSTRAINT fk_acceptance_rectification_issue_image_acceptance
        FOREIGN KEY (acceptance_id) REFERENCES db_acceptance (acceptance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='业主整改问题图片';

CREATE TABLE IF NOT EXISTS db_acceptance_round_rectification_issue_image (
    round_id BIGINT NOT NULL COMMENT '验收轮次',
    url VARCHAR(512) NOT NULL COMMENT '本轮业主提出的整改问题图片',
    KEY idx_acceptance_round_rectification_issue_image (round_id),
    CONSTRAINT fk_acceptance_round_rectification_issue_image_round
        FOREIGN KEY (round_id) REFERENCES db_acceptance_round (round_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='验收轮次整改问题图片';

-- 存量整改数据按施工整改回填，保留原截止日期，不重算历史事实。
UPDATE db_acceptance
SET rectification_type = 'Construction'
WHERE rectification_type IS NULL
  AND (rectification_owner_id IS NOT NULL OR rectification_due_date IS NOT NULL);

UPDATE db_acceptance_round
SET rectification_type = 'Construction'
WHERE rectification_type IS NULL
  AND (rectification_owner_id IS NOT NULL OR rectification_due_date IS NOT NULL);

-- 回滚（仅在确认新字段无业务数据后执行）：
-- DROP TABLE db_acceptance_round_rectification_issue_image;
-- DROP TABLE db_acceptance_rectification_issue_image;
-- ALTER TABLE db_acceptance_round DROP COLUMN rectification_type;
-- ALTER TABLE db_acceptance DROP COLUMN rectification_type;
