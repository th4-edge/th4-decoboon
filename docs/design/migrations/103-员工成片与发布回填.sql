-- 103-员工成片与发布回填.sql
-- 目的：内容获客 P0-3 员工成片、审核和手动发布回填。
-- 影响表：扩展 db_content_employee_content，新增 db_content_publication。
-- 回滚：
-- DROP TABLE IF EXISTS db_content_publication;
-- ALTER TABLE db_content_employee_content
--   DROP COLUMN selected_asset_ids, DROP COLUMN caption, DROP COLUMN rendered_file_path,
--   DROP COLUMN cover_file_path, DROP COLUMN render_job_id, DROP COLUMN render_error,
--   DROP COLUMN reviewed_by, DROP COLUMN reviewed_at;

ALTER TABLE db_content_employee_content
    ADD COLUMN selected_asset_ids TEXT NULL COMMENT '选中素材ID列表',
    ADD COLUMN caption TEXT NULL COMMENT '发布文案',
    ADD COLUMN rendered_file_path VARCHAR(512) NULL COMMENT '成片文件路径',
    ADD COLUMN cover_file_path VARCHAR(512) NULL COMMENT '封面文件路径',
    ADD COLUMN render_job_id VARCHAR(128) NULL COMMENT '渲染任务编号',
    ADD COLUMN render_error VARCHAR(500) NULL COMMENT '渲染错误',
    ADD COLUMN reviewed_by BIGINT NULL COMMENT '审核人',
    ADD COLUMN reviewed_at DATETIME(6) NULL COMMENT '审核时间',
    ADD KEY idx_content_employee_reviewer (reviewed_by, reviewed_at);

CREATE TABLE db_content_publication (
    publication_id BIGINT NOT NULL COMMENT '发布记录索引',
    com_id BIGINT NOT NULL COMMENT '所属企业',
    employee_content_id BIGINT NOT NULL COMMENT '员工内容',
    task_id BIGINT NOT NULL COMMENT '内容任务',
    emp_id BIGINT NOT NULL COMMENT '发布员工',
    platform VARCHAR(24) NOT NULL COMMENT '发布平台',
    post_url VARCHAR(1000) NULL COMMENT '作品链接',
    platform_post_id VARCHAR(128) NULL COMMENT '平台作品ID',
    published_at DATETIME(6) NULL COMMENT '发布时间',
    backfill_evidence_path VARCHAR(512) NULL COMMENT '回填证据文件',
    status VARCHAR(24) NOT NULL COMMENT '回填状态',
    create_date_time DATETIME(6) NULL,
    create_date DATE NULL,
    create_time TIME NULL,
    create_year INT NULL,
    create_month INT NULL,
    create_day INT NULL,
    create_hour INT NULL,
    create_minute INT NULL,
    create_second INT NULL,
    create_week TINYINT NULL,
    update_date_time DATETIME(6) NULL,
    update_date DATE NULL,
    update_time TIME NULL,
    update_year INT NULL,
    update_month INT NULL,
    update_day INT NULL,
    update_hour INT NULL,
    update_minute INT NULL,
    update_second INT NULL,
    update_week TINYINT NULL,
    PRIMARY KEY (publication_id),
    KEY idx_content_publication_company (com_id, publication_id),
    KEY idx_content_publication_content (employee_content_id, publication_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='内容发布记录';
