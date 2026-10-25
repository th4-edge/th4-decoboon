-- 仅用于 decoboon_owner_preview_chain 隔离验收库。
-- 前置审计：db_business_target 0 行；store_id 已由历史 ddl-auto 创建；
-- 缺 target_scope_key、目标唯一索引、门店索引和正式 FK。
ALTER TABLE db_business_target DROP FOREIGN KEY FKi800pmwva8rjxx702dsrqecv;
ALTER TABLE db_business_target DROP INDEX FKi800pmwva8rjxx702dsrqecv;

ALTER TABLE db_business_target
  ADD COLUMN target_scope_key BIGINT GENERATED ALWAYS AS (COALESCE(store_id, 0)) STORED
    COMMENT '目标范围键：0=公司级，其他=门店ID' AFTER store_id;

ALTER TABLE db_business_target
  DROP INDEX uk_business_target_company_month,
  ADD UNIQUE INDEX uk_business_target_scope_month (com_id, target_month, target_scope_key),
  ADD INDEX idx_business_target_store (com_id, store_id, target_month);

ALTER TABLE db_business_target
  ADD CONSTRAINT fk_business_target_store FOREIGN KEY (store_id) REFERENCES db_tenant_store (store_id);
