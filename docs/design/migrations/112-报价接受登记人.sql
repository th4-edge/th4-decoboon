-- 112-报价接受登记人
-- 日期：2026-10-14
-- 适用：MySQL 8.0+。存量库执行前请先备份；空库部署请同步使用 01-空库基线.sql。
--
-- 背景：DEC-071 / GAP-045 收敛。报价接受不要求客户本人产生，由设计师按正常流程在既有报价上
-- 直接置 Accepted；系统仅记录置位操作人，不新增门禁校验、不要求来源标记与签认证据。
-- 存量已接受报价保留 accepted_by 为空，不回溯推断。

ALTER TABLE quote
    ADD COLUMN accepted_by BIGINT NULL COMMENT '接受登记人（置位 Accepted 的员工）' AFTER accepted_at,
    ADD KEY idx_quote_com_accepted_by (com_id, accepted_by);

-- 回滚：
-- ALTER TABLE quote DROP KEY idx_quote_com_accepted_by;
-- ALTER TABLE quote DROP COLUMN accepted_by;
