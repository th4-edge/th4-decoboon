-- 113-企业品牌与署名配置
-- 日期：2026-10-14
-- 适用：MySQL 8.0+。存量库执行前请先备份；空库部署请同步使用 01-空库基线.sql。
--
-- 背景：ANS-BIZ-20261010-004 §8「装得清标识默认不显示」+ USER-DECISION-20261014-002
--   「服务主体知情权 / 平台角色为记录存证与保管方」。
-- 品牌位做成配置而非写死，避免裁决变化后返工。
-- 刻意使用 VARCHAR 而非 MySQL 原生 ENUM，规避 111 号（GAP-054）同类「ENUM 缺值导致启动失败」风险。

ALTER TABLE db_tenant_company
    ADD COLUMN brand_primary VARCHAR(16) NOT NULL DEFAULT 'Company'
        COMMENT '叙事主品牌：Company=装企（默认） / Platform=平台',
    ADD COLUMN custodian_credit BIT NOT NULL DEFAULT 0
        COMMENT '是否显示「记录存证与保管 · 装得清」署名（默认关闭）';

-- 存量企业：默认装企主品牌、平台署名关闭，与既有对外口径一致，无需回填。

-- 回滚：
-- ALTER TABLE db_tenant_company DROP COLUMN custodian_credit;
-- ALTER TABLE db_tenant_company DROP COLUMN brand_primary;
