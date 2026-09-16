-- 成本完整性口径：成本未补齐时，结算快照不得固化为可靠利润。
-- 适用：MySQL 8.0+。

ALTER TABLE db_decoboon_finance_settlement
    MODIFY COLUMN profit_amount DECIMAL(14, 2) NULL COMMENT '项目毛利，成本不完整时为空',
    MODIFY COLUMN gross_margin DECIMAL(8, 2) NULL COMMENT '毛利率，成本不完整时为空';
