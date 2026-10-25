-- 130 企业费用 D3 回滚
-- 前置：停止费用凭证生成入口；已发行 PDF 和历史业务事实只读保留。

DROP TABLE IF EXISTS db_voucher_record_source_rollback_130;

CREATE TABLE db_voucher_record_source_rollback_130 AS
SELECT voucher_id, com_id, source_type, source_id, source_snapshot
FROM db_voucher_record
WHERE source_type IS NOT NULL OR source_id IS NOT NULL OR source_snapshot IS NOT NULL;

DROP INDEX idx_voucher_company_source ON db_voucher_record;

ALTER TABLE db_voucher_record
    DROP COLUMN source_snapshot,
    DROP COLUMN source_id,
    DROP COLUMN source_type;
