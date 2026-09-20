-- 量房角色分离与收款幂等（P0-01 / P0-05）
--
-- 依据：T06-ANS-BIZ-20260918-004
-- 1) Measure.designer 只表达设计责任事实，未指派设计师时允许为空；
--    实际执行量房的人单独记录在 executor_id，不再与 designer_id 混用。
-- 2) 收款按企业建立业务幂等键唯一约束，重复提交只保留一条收款和一条资金台账。
--
-- 适用：MySQL 8.0+。存量库执行前请先备份；空库部署请同步使用 01-空库基线.sql。

-- 1. 量房：designer_id 允许为空并新增 executor_id
ALTER TABLE db_measure
    MODIFY COLUMN designer_id BIGINT NULL COMMENT '量房设计师（未指派时为空）';

ALTER TABLE db_measure
    ADD COLUMN executor_id BIGINT NULL COMMENT '量房执行人（实际上门执行人）' AFTER designer_id;

CREATE INDEX idx_measure_company_executor ON db_measure (com_id, executor_id, measure_id);

ALTER TABLE db_measure
    ADD CONSTRAINT FK_measure_executor FOREIGN KEY (executor_id) REFERENCES db_tenant_employee (emp_id);

-- 2. 收款：企业内幂等键唯一
ALTER TABLE db_finance_receipt
    ADD COLUMN idempotency_key VARCHAR(128) NULL COMMENT '幂等键（客户端键+业务指纹，企业内唯一）' AFTER remark;

-- 存量收款没有幂等键：按单据回填稳定键，避免唯一索引建立失败。
UPDATE db_finance_receipt
   SET idempotency_key = CONCAT('legacy-', receipt_id)
 WHERE idempotency_key IS NULL;

CREATE UNIQUE INDEX uk_fin_receipt_company_idempotency
    ON db_finance_receipt (com_id, idempotency_key);

-- 回滚（仅在确认无新增收款漏单风险时执行）：
-- DROP INDEX uk_fin_receipt_company_idempotency ON db_finance_receipt;
-- ALTER TABLE db_finance_receipt DROP COLUMN idempotency_key;
-- ALTER TABLE db_measure DROP FOREIGN KEY FK_measure_executor;
-- DROP INDEX idx_measure_company_executor ON db_measure;
-- ALTER TABLE db_measure DROP COLUMN executor_id;
-- ALTER TABLE db_measure MODIFY COLUMN designer_id BIGINT NOT NULL;
