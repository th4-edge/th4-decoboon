-- 企业级客户分级配置。
--
-- 依据 docs/plan/2026-09-18-客户标签画像与分级-P0b后端契约.md 第五节：
-- 分级阈值与预算区间由企业配置，未配置时使用冻结默认值
-- （预算 高≥20万 / 达标10-20万 / 低<10万；分级 A≥75、B 50–74；权重 30/25/20/15/10）。
-- 配置格式：key=value;key=value，可空。

ALTER TABLE db_tenant_company
    ADD COLUMN grade_config VARCHAR(512) NULL COMMENT '客户分级配置：high/standard/a/b/wDemand/wBudget/wUrgency/wEngagement/wRecency';
