-- 92 客户休眠状态与风险跟进排除回滚脚本
-- 适用：仅用于隔离/dev/test 演练或明确回退；执行前必须先备份。
-- 前置：cust_customer 中不得存在 status='Dormant' 的数据；如存在必须先人工处理。

DROP TABLE IF EXISTS cust_customer_status_log;

ALTER TABLE cust_customer
    DROP FOREIGN KEY fk_customer_dormant_by,
    DROP FOREIGN KEY fk_customer_restored_by;

ALTER TABLE cust_customer
    DROP COLUMN status_before_dormant,
    DROP COLUMN dormant_reason,
    DROP COLUMN dormant_reason_text,
    DROP COLUMN dormant_at,
    DROP COLUMN dormant_by,
    DROP COLUMN restored_at,
    DROP COLUMN restored_by;

ALTER TABLE cust_customer
    MODIFY COLUMN status enum ('Finished','Following','Lost','New','PendingMeasure','Quoted','Serving','Signed') NOT NULL;
