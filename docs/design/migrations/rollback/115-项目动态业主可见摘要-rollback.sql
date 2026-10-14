-- 115 回滚：项目动态业主可见摘要
-- 注意：回滚后成员调整动态将退回 content 内部口吻，业主端会重新看到含操作人的原文。
-- 若此时线上已有成员调整记录，建议先确认这些动态的处置方式，再执行本回滚。
ALTER TABLE db_project_report DROP COLUMN customer_summary;
