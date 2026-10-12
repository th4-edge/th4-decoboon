-- 110-门店启用停用与排序.sql
-- 目的：P0D-04 / DESIGN-BIZ-20261013-002 §2 —— 门店主数据补「启用/停用」与「排序」两列。
--       ① status：Active(启用) / Disabled(已停用)，默认 Active；停用不删除数据，历史单据门店归属保留、
--          仍计入「全部门店」合计，仅禁止新的归属（服务端 StoreAttributionContext 拦截）。
--       ② sort：整数升序，同值按 store_id 升序；用于门店维度控件与门店列表展示顺序。
-- 影响表：db_tenant_store（新增两列 + 一个联合索引）。
-- 回滚：docs/design/migrations/rollback/110-门店启用停用与排序-rollback.sql
-- 注意：prod 为 `ddl-auto: validate`，发布前必须先执行本迁移，否则启动即失败（对齐 2026-10-11 迁移 106 事故教训）。

alter table db_tenant_store
  add column status varchar(16) not null default 'Active' comment '门店状态(Active=启用/Disabled=已停用，P0D-04)' after default_store,
  add column sort int not null default 0 comment '排序（升序，同值按 store_id，P0D-04）' after status;

-- 存量门店全部置为启用、排序 0（默认值已覆盖，这里显式写一次以便审计核对）
update db_tenant_store set status = 'Active' where status is null or status not in ('Active', 'Disabled');
update db_tenant_store set sort = 0 where sort is null;

-- 注意：门店表的企业外键列名为 `company_com_id`（不是 com_id），索引必须用它，否则 ERROR 1072。
create index idx_store_company_status_sort on db_tenant_store (company_com_id, status, sort, store_id);
