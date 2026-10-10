-- 105 客户跟进图片回滚脚本
-- 适用：仅用于隔离/dev/test 演练或明确回退；执行前必须先备份。
-- 注意：回滚会删除跟进图片对象键和 request_id，不会删除 OSS 对象；OSS 对象按保留策略处理。

DROP TABLE IF EXISTS cust_customer_follow_image;

ALTER TABLE cust_customer_follow
    DROP INDEX uk_customer_follow_company_request,
    DROP COLUMN request_id;
