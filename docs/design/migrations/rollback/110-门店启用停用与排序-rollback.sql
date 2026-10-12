-- 回滚：110-门店启用停用与排序.sql
-- 说明：仅去列与索引；门店历史业务数据不受影响（停用只影响新建归属，删除列后门店恢复为「无状态」= 等效全部启用）。
-- 执行前提：确认没有任何代码/报表仍在读取 db_tenant_store.status / sort（即已回滚到迁移前版本）。

drop index idx_store_company_status_sort on db_tenant_store;

alter table db_tenant_store
  drop column sort,
  drop column status;
