-- 回滚：2026-10-17-多门店测试数据.sql
-- 仅删除 com_id=2790000000000000001 的测试数据；使用临时存储过程遍历所有带 com_id 的表。
-- 适用：本地 dev/test；不要在含真实业务的其他 com_id 上执行。

set session group_concat_max_len = 1000000;

delimiter //
drop procedure if exists cleanup_multistore_test_data//
create procedure cleanup_multistore_test_data()
begin
  declare done int default false;
  declare tbl varchar(64);
  declare cur cursor for
    select table_name
    from information_schema.columns
    where table_schema = database()
      and column_name = 'com_id';
  declare continue handler for not found set done = true;

  set foreign_key_checks = 0;
  open cur;
  read_loop: loop
    fetch cur into tbl;
    if done then
      leave read_loop;
    end if;
    set @stmt = concat('delete from `', tbl, '` where com_id = 2790000000000000001');
    prepare s from @stmt;
    execute s;
    deallocate prepare s;
  end loop;
  close cur;

  delete from db_tenant_store where company_com_id = 2790000000000000001;
  delete from db_tenant_credentials where phone between '19900001001' and '19900001011';
  delete from db_tenant_company where com_id = 2790000000000000001;

  set foreign_key_checks = 1;
end//
delimiter ;

call cleanup_multistore_test_data();
drop procedure cleanup_multistore_test_data;
