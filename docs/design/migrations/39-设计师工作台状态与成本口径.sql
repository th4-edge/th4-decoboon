-- 39-设计师工作台状态与成本口径
-- 日期：2026-09-20
-- 适用：MySQL 8.0+。存量库执行前请先备份；空库部署请同步使用 01-空库基线.sql。
--
-- 报价状态时间必须由状态推进写入，不能继续把通用 update_time 当作客户反馈时间。
-- 设计工作台最小事实粒度为“客户 + 房屋”，本次不新增归属字段，复用 quote.property_id。

ALTER TABLE quote
    ADD COLUMN submitted_at DATETIME(6) NULL COMMENT '首次提交客户时间' AFTER status,
    ADD COLUMN rejected_at DATETIME(6) NULL COMMENT '客户最近驳回时间' AFTER submitted_at,
    ADD COLUMN accepted_at DATETIME(6) NULL COMMENT '客户接受时间' AFTER rejected_at,
    ADD COLUMN converted_at DATETIME(6) NULL COMMENT '报价转项目时间' AFTER accepted_at;

-- 存量 Accepted / Converted 报价保留 accepted_at 为空；Provider 会回退到旧 updateTime，
-- 避免误解析不同环境中 audit 时间列的历史存储格式。

-- 回滚：
-- ALTER TABLE quote DROP COLUMN converted_at;
-- ALTER TABLE quote DROP COLUMN accepted_at;
-- ALTER TABLE quote DROP COLUMN rejected_at;
-- ALTER TABLE quote DROP COLUMN submitted_at;
