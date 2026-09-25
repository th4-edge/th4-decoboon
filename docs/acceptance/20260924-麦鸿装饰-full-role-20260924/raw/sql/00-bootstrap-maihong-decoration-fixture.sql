-- 本地 decoboon 全角色审计夹具：仅创建企业、总店、8 名员工和本地密码凭证。
-- 不创建客户、房屋、报价、合同、项目、任务、财务或验收业务终态。
START TRANSACTION;

SET @now := NOW(6);
SET @date := CAST(DATE_FORMAT(@now, '%Y%m%d') AS UNSIGNED);
SET @dateTime := CAST(DATE_FORMAT(@now, '%Y%m%d%H%i%s') AS UNSIGNED);
SET @time := CAST(DATE_FORMAT(@now, '%H%i%s') AS UNSIGNED);
SET @year := YEAR(@now);
SET @month := MONTH(@now);
SET @day := DAY(@now);
SET @hour := HOUR(@now);
SET @minute := MINUTE(@now);
SET @second := SECOND(@now);
SET @week := WEEKDAY(@now);

INSERT INTO db_tenant_company
  (com_id, name, title, addr, color, logo, enable_store, size, grade_config,
   create_date_time, create_date, create_time, create_year, create_month, create_day,
   create_hour, create_minute, create_second, create_week)
VALUES
  (1790000000000000001, '麦鸿装饰', '麦鸿装饰', '安徽省合肥市', '#1677FF', NULL, b'1', 'A', NULL,
   @dateTime, @date, @time, @year, @month, @day, @hour, @minute, @second, @week);

INSERT INTO db_tenant_store
  (store_id, name, addr, color, default_store, company_com_id,
   create_date_time, create_date, create_time, create_year, create_month, create_day,
   create_hour, create_minute, create_second, create_week)
VALUES
  (1790000000000000002, '总店', '安徽省合肥市', '#1677FF', b'1', 1790000000000000001,
   @dateTime, @date, @time, @year, @month, @day, @hour, @minute, @second, @week);

INSERT INTO db_tenant_employee
  (emp_id, name, tel_no, avatar_url, store_id, team_id, com_id, role, status, joined_at, invite_id,
   create_date_time, create_date, create_time, create_year, create_month, create_day,
   create_hour, create_minute, create_second, create_week)
VALUES
  (1790000000000000101, '钱小明', '19900000001', NULL, 1790000000000000002, NULL, 1790000000000000001, 'Boss',         'Active', @now, NULL, @dateTime, @date, @time, @year, @month, @day, @hour, @minute, @second, @week),
  (1790000000000000102, '吴濛濛', '19900000002', NULL, 1790000000000000002, NULL, 1790000000000000001, 'Finance',      'Active', @now, NULL, @dateTime, @date, @time, @year, @month, @day, @hour, @minute, @second, @week),
  (1790000000000000103, '王洪文', '19900000003', NULL, 1790000000000000002, NULL, 1790000000000000001, 'StoreManager', 'Active', @now, NULL, @dateTime, @date, @time, @year, @month, @day, @hour, @minute, @second, @week),
  (1790000000000000104, '黄贤杰', '19900000004', NULL, 1790000000000000002, NULL, 1790000000000000001, 'Marketer',     'Active', @now, NULL, @dateTime, @date, @time, @year, @month, @day, @hour, @minute, @second, @week),
  (1790000000000000105, '董夏鹏', '19900000005', NULL, 1790000000000000002, NULL, 1790000000000000001, 'Designer',     'Active', @now, NULL, @dateTime, @date, @time, @year, @month, @day, @hour, @minute, @second, @week),
  (1790000000000000106, '谢玲玲', '19900000006', NULL, 1790000000000000002, NULL, 1790000000000000001, 'Designer',     'Active', @now, NULL, @dateTime, @date, @time, @year, @month, @day, @hour, @minute, @second, @week),
  (1790000000000000107, '徐勇',   '19900000007', NULL, 1790000000000000002, NULL, 1790000000000000001, 'Pm',           'Active', @now, NULL, @dateTime, @date, @time, @year, @month, @day, @hour, @minute, @second, @week),
  (1790000000000000108, '王昌富', '19900000008', NULL, 1790000000000000002, NULL, 1790000000000000001, 'Pm',           'Active', @now, NULL, @dateTime, @date, @time, @year, @month, @day, @hour, @minute, @second, @week);

-- 仅本地测试凭证；口令为 Maihong2026，使用 BCrypt 哈希保存，明文不入库。
INSERT INTO db_tenant_credentials
  (cre_id, type, emp_id, phone, status, id_key, id_secret, password_hash, password_updated_at,
   failed_attempts, locked_until, must_change_password, last_time,
   create_date_time, create_date, create_time, create_year, create_month, create_day,
   create_hour, create_minute, create_second, create_week)
VALUES
  (1790000000000000201, 'Password', 1790000000000000101, '19900000001', 'Active', '19900000001', NULL, '$2a$10$dH9duwPL.cJeZw5C1ljvLe6IicvztSo23zq7L8ZdfBb.dHm5aZakC', @now, 0, NULL, b'0', NULL, @dateTime, @date, @time, @year, @month, @day, @hour, @minute, @second, @week),
  (1790000000000000202, 'Password', 1790000000000000102, '19900000002', 'Active', '19900000002', NULL, '$2a$10$dH9duwPL.cJeZw5C1ljvLe6IicvztSo23zq7L8ZdfBb.dHm5aZakC', @now, 0, NULL, b'0', NULL, @dateTime, @date, @time, @year, @month, @day, @hour, @minute, @second, @week),
  (1790000000000000203, 'Password', 1790000000000000103, '19900000003', 'Active', '19900000003', NULL, '$2a$10$dH9duwPL.cJeZw5C1ljvLe6IicvztSo23zq7L8ZdfBb.dHm5aZakC', @now, 0, NULL, b'0', NULL, @dateTime, @date, @time, @year, @month, @day, @hour, @minute, @second, @week),
  (1790000000000000204, 'Password', 1790000000000000104, '19900000004', 'Active', '19900000004', NULL, '$2a$10$dH9duwPL.cJeZw5C1ljvLe6IicvztSo23zq7L8ZdfBb.dHm5aZakC', @now, 0, NULL, b'0', NULL, @dateTime, @date, @time, @year, @month, @day, @hour, @minute, @second, @week),
  (1790000000000000205, 'Password', 1790000000000000105, '19900000005', 'Active', '19900000005', NULL, '$2a$10$dH9duwPL.cJeZw5C1ljvLe6IicvztSo23zq7L8ZdfBb.dHm5aZakC', @now, 0, NULL, b'0', NULL, @dateTime, @date, @time, @year, @month, @day, @hour, @minute, @second, @week),
  (1790000000000000206, 'Password', 1790000000000000106, '19900000006', 'Active', '19900000006', NULL, '$2a$10$dH9duwPL.cJeZw5C1ljvLe6IicvztSo23zq7L8ZdfBb.dHm5aZakC', @now, 0, NULL, b'0', NULL, @dateTime, @date, @time, @year, @month, @day, @hour, @minute, @second, @week),
  (1790000000000000207, 'Password', 1790000000000000107, '19900000007', 'Active', '19900000007', NULL, '$2a$10$dH9duwPL.cJeZw5C1ljvLe6IicvztSo23zq7L8ZdfBb.dHm5aZakC', @now, 0, NULL, b'0', NULL, @dateTime, @date, @time, @year, @month, @day, @hour, @minute, @second, @week),
  (1790000000000000208, 'Password', 1790000000000000108, '19900000008', 'Active', '19900000008', NULL, '$2a$10$dH9duwPL.cJeZw5C1ljvLe6IicvztSo23zq7L8ZdfBb.dHm5aZakC', @now, 0, NULL, b'0', NULL, @dateTime, @date, @time, @year, @month, @day, @hour, @minute, @second, @week);

COMMIT;
