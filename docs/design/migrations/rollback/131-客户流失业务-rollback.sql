-- 131-客户流失业务回滚脚本
-- 仅用于隔离/dev/test 演练或明确回退；执行前必须先备份。
-- 前置：先确认不存在需要保留的 cust_customer_loss 记录。

DROP TABLE IF EXISTS cust_customer_loss;

-- 先确认不存在 Lost/RestoreLost 状态日志且不存在 201-500 字原因说明，再执行字段回滚。
ALTER TABLE cust_customer_status_log
    MODIFY COLUMN reason_text VARCHAR(200) NULL,
    MODIFY COLUMN change_type VARCHAR(24) NOT NULL;
