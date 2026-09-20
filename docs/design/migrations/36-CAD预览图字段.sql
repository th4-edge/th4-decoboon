-- 36-CAD 预览图字段.sql
-- 设计方案表增加 CAD 预览图 URL 和转换状态字段
-- 对应前端 A+ 方案：后端预渲染 PNG + 前端图片预览

ALTER TABLE db_design
    ADD COLUMN cad_preview_url VARCHAR(512) COMMENT 'CAD 预览图' AFTER cad_url,
    ADD COLUMN cad_convert_status VARCHAR(16) COMMENT 'CAD 转换状态' AFTER cad_preview_url;
