-- 105-客户跟进图片.sql
-- 目的：客户跟进支持多张私有 OSS 图片，并增加保存幂等号。
-- 影响表：新增 cust_customer_follow_image；cust_customer_follow 增加 request_id 和唯一索引。
-- 回滚：先删除唯一索引和 request_id，再删除 cust_customer_follow_image。

CREATE TABLE cust_customer_follow_image (
    follow_id BIGINT NOT NULL COMMENT '客户跟进',
    url VARCHAR(512) NOT NULL COMMENT 'OSS对象键',
    sort_no INT NOT NULL COMMENT '图片顺序',
    PRIMARY KEY (follow_id, sort_no),
    UNIQUE KEY uk_customer_follow_image_url (url),
    CONSTRAINT fk_customer_follow_image_follow
        FOREIGN KEY (follow_id) REFERENCES cust_customer_follow (follow_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='客户跟进图片';

ALTER TABLE cust_customer_follow
    ADD COLUMN request_id VARCHAR(64) NULL COMMENT '请求幂等号',
    ADD UNIQUE KEY uk_customer_follow_company_request (com_id, request_id);
