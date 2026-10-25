-- 130 企业费用 D3：费用付款凭证来源快照
-- 口径：DEC-091 §8 / BR-057 §7 / T12-BIZ-20261022-012 / EXPENSE-D3
-- 边界：只增加凭证来源定位和费用快照；模板版本、PDF路径和文件哈希复用现有列。

ALTER TABLE db_voucher_record
    ADD COLUMN source_type VARCHAR(32) NULL COMMENT '来源类型；费用凭证为 Expense' AFTER project_id;

ALTER TABLE db_voucher_record
    ADD COLUMN source_id BIGINT NULL COMMENT '来源业务主键；费用凭证为 expense_id' AFTER source_type;

ALTER TABLE db_voucher_record
    ADD COLUMN source_snapshot LONGTEXT NULL COMMENT '发行时费用来源快照JSON' AFTER source_id;

CREATE INDEX idx_voucher_company_source
    ON db_voucher_record (com_id, source_type, source_id);
