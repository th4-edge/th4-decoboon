-- 回滚：121-门店月度经营目标.sql
-- 执行前提：已删除所有门店级目标（store_id is not null），否则重新建立旧唯一约束会失败。
-- 说明：公司级目标保留；门店级目标在回滚前必须由业务确认导出/清理。

alter table db_business_target drop foreign key fk_business_target_store;

alter table db_business_target
  drop index uk_business_target_scope_month,
  drop index idx_business_target_store;

alter table db_business_target
  drop column target_scope_key,
  drop column store_id;

alter table db_business_target
  add constraint uk_business_target_company_month unique (com_id, target_month);
