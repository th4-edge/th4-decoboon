-- 方案原始结构图改为多图。
--
-- 依据用户决策：原始结构图支持多图、文件可编辑。
-- 保留既有 db_design.original_plan_url（单图）用于历史数据读取，
-- 新增 db_design_original_plan_images 承载多图；前端读取时合并两者。

CREATE TABLE IF NOT EXISTS db_design_original_plan_images (
    design_id BIGINT NOT NULL COMMENT '设计方案',
    url VARCHAR(512) NOT NULL COMMENT '原始结构图地址'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='方案原始结构图（多图）';
