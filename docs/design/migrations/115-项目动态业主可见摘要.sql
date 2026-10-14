-- 115-项目动态业主可见摘要
-- 日期：2026-10-15
-- 适用：MySQL 8.0+。存量库执行前请先备份；空库部署请同步使用 01-空库基线.sql。
--
-- 背景：USER-DECISION-20261014-002「服务主体知情权」的另一面——
--   业主有权知道是谁在服务，就同样有权知道**它变了**。
-- 既有机制：project/members/save 的 recordMemberChange 已把成员切换写入
--   db_project_report（type=Notice），但 content 是内部口吻（含操作人），
--   且被显式标记 visibleToCustomer=false。
-- 本次不新建对象：加一个**业主口吻的结构化摘要**列，业主端只读该列，
--   content 保持内部口吻不变，避免把内部动态原文透给业主。

ALTER TABLE db_project_report
    ADD COLUMN customer_summary VARCHAR(255) NULL COMMENT '业主可见摘要（业主口吻，不含内部操作人）';

-- 存量动态：该列为空，业主端沿用既有可见性规则，不回溯改写。

-- 回滚：
-- ALTER TABLE db_project_report DROP COLUMN customer_summary;
