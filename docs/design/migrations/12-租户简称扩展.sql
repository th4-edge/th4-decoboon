-- 企业简称从 12 字符扩展为 32 字符，兼容品牌简称和历史数据。
ALTER TABLE db_tenant_company
    MODIFY COLUMN name VARCHAR(32) NOT NULL;
