-- 35-量房关联表与字段补全
-- 日期：2026-09-19
-- 适用：MySQL 8.0+。存量库执行前请先备份；空库部署请使用 01-空库基线.sql。
--
-- 背景：前端录入量房记录支持现场照片多图上传，后端 Measure 实体使用
-- @ElementCollection 关联表存储 sitePhotos / structureImages / attachments。
-- JPA 未配置 ddl-auto，存量库若缺少以下关联表或字段，保存时会报
-- "Table doesn't exist" 或 "Unknown column" 错误。
-- 本迁移以幂等方式补全所有量房记录相关的关联表和字段。

-- 1. 现场照片关联表（01-空库基线已含，存量库可能缺失）
CREATE TABLE IF NOT EXISTS db_measure_site_photos (
    measure_id BIGINT NOT NULL COMMENT '量房记录',
    url VARCHAR(512) NOT NULL COMMENT '现场照片存储路径',
    KEY idx_measure_site_photo (measure_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='量房现场照片';

-- 2. 结构图与原始图纸关联表（25-阶段排期与验收证据已含，存量库可能缺失）
CREATE TABLE IF NOT EXISTS db_measure_structure_images (
    measure_id BIGINT NOT NULL COMMENT '量房记录',
    url VARCHAR(512) NOT NULL COMMENT '结构图或原始图纸地址',
    KEY idx_measure_structure_image (measure_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='量房结构图与原始图纸';

-- 3. 量房附件关联表（25-阶段排期与验收证据已含，存量库可能缺失）
CREATE TABLE IF NOT EXISTS db_measure_attachments (
    measure_id BIGINT NOT NULL COMMENT '量房记录',
    url VARCHAR(512) NOT NULL COMMENT '量房附件地址',
    KEY idx_measure_attachment (measure_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='量房附件';

-- 4. 量房主表补充 executor_id 字段（34-量房角色分离已含，存量库可能缺失）
-- MySQL 不支持 ADD COLUMN IF NOT EXISTS，使用存储过程幂等添加
DROP PROCEDURE IF EXISTS add_column_if_not_exists;
DELIMITER //
CREATE PROCEDURE add_column_if_not_exists(IN tbl_name VARCHAR(64), IN col_name VARCHAR(64), IN col_def TEXT)
BEGIN
    IF NOT EXISTS (
        SELECT * FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tbl_name AND COLUMN_NAME = col_name
    ) THEN
        SET @sql = CONCAT('ALTER TABLE ', tbl_name, ' ADD COLUMN ', col_def);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL add_column_if_not_exists(
    'db_measure',
    'executor_id',
    'executor_id BIGINT NULL COMMENT ''量房执行人（实际上门执行量房的员工，与设计师事实分离）'' AFTER designer_id'
);

DROP PROCEDURE IF EXISTS add_column_if_not_exists;

-- 5. 外键约束（关联表 → 量房主表），已存在则跳过
-- 注意：如果存量库已有数据但关联表 measure_id 存在无效值，外键创建会失败。
-- 请先执行：DELETE FROM db_measure_site_photos WHERE measure_id NOT IN (SELECT measure_id FROM db_measure);
-- 对 structure_images 和 attachments 同理。

-- 回滚（仅在确认无需量房照片功能时执行）：
-- DROP TABLE IF EXISTS db_measure_attachments;
-- DROP TABLE IF EXISTS db_measure_structure_images;
-- DROP TABLE IF EXISTS db_measure_site_photos;
-- ALTER TABLE db_measure DROP COLUMN executor_id;
