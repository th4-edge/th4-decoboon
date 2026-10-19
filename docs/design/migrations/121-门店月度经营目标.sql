-- 121-门店月度经营目标.sql
-- 目的：DEC-076 / BR-049 —— 月度经营目标增加门店维度。
--       公司目标 store_id = NULL；门店目标 store_id = 具体门店。
--       用生成列 target_scope_key = coalesce(store_id, 0) 保证：
--         同一企业 + 同一月份 + 同一范围（公司或具体门店）唯一。
-- 影响表：db_business_target（新增 store_id、生成列、唯一索引与门店索引）。
-- 回滚：docs/design/migrations/rollback/121-门店月度经营目标-rollback.sql
-- 注意：prod 为 ddl-auto: validate，发布前必须先执行本迁移。

alter table db_business_target
  add column store_id bigint null comment '门店目标归属；NULL=公司级目标（DEC-076/BR-049）' after com_id,
  add column target_scope_key bigint generated always as (coalesce(store_id, 0)) stored comment '目标范围键：0=公司级，其他=门店ID' after store_id;

-- 旧唯一约束只覆盖 com_id + target_month，会阻止门店目标与公司目标并存，必须替换。
alter table db_business_target
  drop index uk_business_target_company_month,
  add unique index uk_business_target_scope_month (com_id, target_month, target_scope_key),
  add index idx_business_target_store (com_id, store_id, target_month);

alter table db_business_target
  add constraint fk_business_target_store foreign key (store_id) references db_tenant_store (store_id);
