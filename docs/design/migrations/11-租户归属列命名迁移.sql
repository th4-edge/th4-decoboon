-- 修正租户员工/团队归属列命名，使物理列与 com_id/store_id 权限索引一致。
-- 适用：仍使用 company_com_id/store_store_id 旧列名的环境；全新库无需执行。

ALTER TABLE db_tenant_employee
    RENAME COLUMN company_com_id TO com_id,
    RENAME COLUMN store_store_id TO store_id;

ALTER TABLE db_tenant_team
    RENAME COLUMN company_com_id TO com_id,
    RENAME COLUMN store_store_id TO store_id;
