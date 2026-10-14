-- 114-企业对外信息
-- 日期：2026-10-15
-- 适用：MySQL 8.0+。存量库执行前请先备份；空库部署请同步使用 01-空库基线.sql。
--
-- 背景：USER-DECISION-20261014-002「服务主体知情权」——业主有权知道是哪家企业、
-- 哪些人在为这一户服务。这是知情权，不是宣传（ANS-BIZ-20261014-001）。
-- 字段由企业自行填写并审核发布；平台不代填、不推算、不背书。
-- 注意：客服电话为**企业统一号码**；员工个人手机号不在此表、不得投影给业主。

ALTER TABLE db_tenant_company
    ADD COLUMN service_area VARCHAR(128) NULL COMMENT '服务区域',
    ADD COLUMN service_tel  VARCHAR(32)  NULL COMMENT '企业统一客服电话',
    ADD COLUMN after_sale_tel VARCHAR(32) NULL COMMENT '售后专线',
    ADD COLUMN uscc         VARCHAR(32)  NULL COMMENT '统一社会信用代码',
    ADD COLUMN qualification VARCHAR(128) NULL COMMENT '资质',
    ADD COLUMN intro        VARCHAR(512) NULL COMMENT '一句话简介';

-- 存量企业：各字段留空，业主端按 BR-016 显示「暂不可确认」，不用空值伪装。

-- 回滚：
-- ALTER TABLE db_tenant_company DROP COLUMN intro;
-- ALTER TABLE db_tenant_company DROP COLUMN qualification;
-- ALTER TABLE db_tenant_company DROP COLUMN uscc;
-- ALTER TABLE db_tenant_company DROP COLUMN after_sale_tel;
-- ALTER TABLE db_tenant_company DROP COLUMN service_tel;
-- ALTER TABLE db_tenant_company DROP COLUMN service_area;
