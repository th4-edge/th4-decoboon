-- 78-收付款幂等统一.sql
-- 目的：收款与付款均以客户端提交意图 ID 建企业内唯一约束，并保存业务指纹用于识别同 key 不同载荷。
-- 适用：MySQL 8.0+。存量库执行前先备份；空库部署走 01-空库基线后按迁移账本顺延执行。

-- 收款：保留已有 idempotency_key，新增业务指纹列。历史行的指纹为空，服务端仍可按业务字段兼容比对。
ALTER TABLE db_finance_receipt
    ADD COLUMN idempotency_fingerprint VARCHAR(64) NULL COMMENT '业务指纹，用于校验同一幂等键是否对应同一笔收款'
    AFTER idempotency_key;

-- 付款：新增客户端提交意图 ID 和业务指纹。存量付款先回填每单稳定键，避免唯一索引建立失败。
ALTER TABLE db_finance_payment
    ADD COLUMN idempotency_key VARCHAR(128) NULL COMMENT '幂等键（客户端提交意图 ID，企业内唯一）'
    AFTER remark,
    ADD COLUMN idempotency_fingerprint VARCHAR(64) NULL COMMENT '业务指纹，用于校验同一幂等键是否对应同一笔付款'
    AFTER idempotency_key;

UPDATE db_finance_payment
   SET idempotency_key = CONCAT('legacy-payment-', payment_id)
 WHERE idempotency_key IS NULL;

CREATE UNIQUE INDEX uk_fin_payment_company_idempotency
    ON db_finance_payment (com_id, idempotency_key);

-- 回滚（仅在确认不会造成重复收付款漏单风险时执行）：
-- DROP INDEX uk_fin_payment_company_idempotency ON db_finance_payment;
-- ALTER TABLE db_finance_payment DROP COLUMN idempotency_fingerprint;
-- ALTER TABLE db_finance_payment DROP COLUMN idempotency_key;
-- ALTER TABLE db_finance_receipt DROP COLUMN idempotency_fingerprint;
