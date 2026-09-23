-- 50-合并履约专员与财务角色.sql
-- 产品角色统一为：Boss、StoreManager、Designer、Marketer、Pm、Finance、TeamLeader。
-- 履约专员的历史账号、邀请和动态统一迁移为财务，保留原履约协同权限。

update db_tenant_employee
set role = 'Finance'
where role = 'ProjectSupport';

update db_tenant_invite
set role = 'Finance'
where role = 'ProjectSupport';

update db_project_report
set author_role = 'Finance'
where author_role = 'ProjectSupport';

alter table db_tenant_employee
    modify column role enum ('Boss','StoreManager','Designer','Marketer','Pm','Finance','TeamLeader') not null;

alter table db_tenant_invite
    modify column role enum ('Boss','StoreManager','Designer','Marketer','Pm','Finance','TeamLeader') not null;

alter table db_project_report
    modify column author_role enum ('Boss','StoreManager','Designer','Marketer','Pm','Finance','TeamLeader') not null;

-- 回滚（仅回滚代码前，人工确认后执行）：
-- alter table db_tenant_employee
--     modify column role enum ('Boss','Designer','Finance','Marketer','Pm','ProjectSupport','StoreManager','TeamLeader') not null;
-- alter table db_tenant_invite
--     modify column role enum ('Boss','Designer','Finance','Marketer','Pm','ProjectSupport','StoreManager','TeamLeader') not null;
-- alter table db_project_report
--     modify column author_role enum ('Boss','Designer','Finance','Marketer','Pm','ProjectSupport','StoreManager','TeamLeader') not null;
-- update db_tenant_employee set role = 'ProjectSupport' where emp_id in (<迁移前备份的 emp_id 列表>);
-- update db_tenant_invite set role = 'ProjectSupport' where invite_id in (<迁移前备份的 invite_id 列表>);
-- update db_project_report set author_role = 'ProjectSupport' where report_id in (<迁移前备份的 report_id 列表>);
