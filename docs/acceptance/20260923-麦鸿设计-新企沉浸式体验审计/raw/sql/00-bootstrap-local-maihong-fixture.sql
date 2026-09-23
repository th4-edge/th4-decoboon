-- 本地沉浸式审计环境恢复：只写 127.0.0.1:3306/decoboon。
-- 依据后端仓库 2026-09-21/2026-09-23 运营记录恢复麦鸿演示租户、门店、员工和本地密码凭证。
-- 不创建合同、收付款、验收或任何业务终态；这些必须通过小程序真实动作产生。
START TRANSACTION;

INSERT INTO db_tenant_company
  (com_id, name, title, addr, color, logo, enable_store, size, grade_config,
   create_date_time, create_date, create_time, create_year, create_month, create_day,
   create_hour, create_minute, create_second, create_week)
VALUES
  (1789903520997907818, '麦鸿设计', '麦鸿设计', NULL, '#1677FF', NULL, b'1', 'A', NULL,
   CAST(DATE_FORMAT(NOW(6), '%Y%m%d%H%i%s') AS UNSIGNED), CAST(DATE_FORMAT(NOW(6), '%Y%m%d') AS UNSIGNED), CAST(DATE_FORMAT(NOW(6), '%H%i%s') AS UNSIGNED),
   YEAR(NOW()), MONTH(NOW()), DAY(NOW()), HOUR(NOW()), MINUTE(NOW()), SECOND(NOW()), WEEKDAY(NOW()));

INSERT INTO db_tenant_store
  (store_id, name, addr, color, default_store, company_com_id,
   create_date_time, create_date, create_time, create_year, create_month, create_day,
   create_hour, create_minute, create_second, create_week)
VALUES
  (1789903521001328357, '总店', NULL, '#1677FF', b'1', 1789903520997907818,
   CAST(DATE_FORMAT(NOW(6), '%Y%m%d%H%i%s') AS UNSIGNED), CAST(DATE_FORMAT(NOW(6), '%Y%m%d') AS UNSIGNED), CAST(DATE_FORMAT(NOW(6), '%H%i%s') AS UNSIGNED),
   YEAR(NOW()), MONTH(NOW()), DAY(NOW()), HOUR(NOW()), MINUTE(NOW()), SECOND(NOW()), WEEKDAY(NOW()));

INSERT INTO db_tenant_employee
  (emp_id, name, tel_no, avatar_url, store_id, team_id, com_id, role, status, joined_at, invite_id,
   create_date_time, create_date, create_time, create_year, create_month, create_day,
   create_hour, create_minute, create_second, create_week)
VALUES
  (1789903521009050562, '张伟',   '18660219918', NULL, 1789903521001328357, NULL, 1789903520997907818, 'Boss',         'Active', NOW(6), NULL,
   CAST(DATE_FORMAT(NOW(6), '%Y%m%d%H%i%s') AS UNSIGNED), CAST(DATE_FORMAT(NOW(6), '%Y%m%d') AS UNSIGNED), CAST(DATE_FORMAT(NOW(6), '%H%i%s') AS UNSIGNED), YEAR(NOW()), MONTH(NOW()), DAY(NOW()), HOUR(NOW()), MINUTE(NOW()), SECOND(NOW()), WEEKDAY(NOW())),
  (1789903522000000001, '王宏文', '19900000001', NULL, 1789903521001328357, NULL, 1789903520997907818, 'StoreManager', 'Active', NOW(6), NULL,
   CAST(DATE_FORMAT(NOW(6), '%Y%m%d%H%i%s') AS UNSIGNED), CAST(DATE_FORMAT(NOW(6), '%Y%m%d') AS UNSIGNED), CAST(DATE_FORMAT(NOW(6), '%H%i%s') AS UNSIGNED), YEAR(NOW()), MONTH(NOW()), DAY(NOW()), HOUR(NOW()), MINUTE(NOW()), SECOND(NOW()), WEEKDAY(NOW())),
  (1789903522000000002, '黄贤杰', '19900000002', NULL, 1789903521001328357, NULL, 1789903520997907818, 'Marketer',     'Active', NOW(6), NULL,
   CAST(DATE_FORMAT(NOW(6), '%Y%m%d%H%i%s') AS UNSIGNED), CAST(DATE_FORMAT(NOW(6), '%Y%m%d') AS UNSIGNED), CAST(DATE_FORMAT(NOW(6), '%H%i%s') AS UNSIGNED), YEAR(NOW()), MONTH(NOW()), DAY(NOW()), HOUR(NOW()), MINUTE(NOW()), SECOND(NOW()), WEEKDAY(NOW())),
  (1789903522000000003, '谢玲玲', '19900000003', NULL, 1789903521001328357, NULL, 1789903520997907818, 'Designer',     'Active', NOW(6), NULL,
   CAST(DATE_FORMAT(NOW(6), '%Y%m%d%H%i%s') AS UNSIGNED), CAST(DATE_FORMAT(NOW(6), '%Y%m%d') AS UNSIGNED), CAST(DATE_FORMAT(NOW(6), '%H%i%s') AS UNSIGNED), YEAR(NOW()), MONTH(NOW()), DAY(NOW()), HOUR(NOW()), MINUTE(NOW()), SECOND(NOW()), WEEKDAY(NOW())),
  (1789903522000000004, '董夏鹏', '19900000004', NULL, 1789903521001328357, NULL, 1789903520997907818, 'Designer',     'Active', NOW(6), NULL,
   CAST(DATE_FORMAT(NOW(6), '%Y%m%d%H%i%s') AS UNSIGNED), CAST(DATE_FORMAT(NOW(6), '%Y%m%d') AS UNSIGNED), CAST(DATE_FORMAT(NOW(6), '%H%i%s') AS UNSIGNED), YEAR(NOW()), MONTH(NOW()), DAY(NOW()), HOUR(NOW()), MINUTE(NOW()), SECOND(NOW()), WEEKDAY(NOW())),
  (1789903522000000005, '吴濛濛', '19900000005', NULL, 1789903521001328357, NULL, 1789903520997907818, 'Finance',      'Active', NOW(6), NULL,
   CAST(DATE_FORMAT(NOW(6), '%Y%m%d%H%i%s') AS UNSIGNED), CAST(DATE_FORMAT(NOW(6), '%Y%m%d') AS UNSIGNED), CAST(DATE_FORMAT(NOW(6), '%H%i%s') AS UNSIGNED), YEAR(NOW()), MONTH(NOW()), DAY(NOW()), HOUR(NOW()), MINUTE(NOW()), SECOND(NOW()), WEEKDAY(NOW())),
  (1789903522000000006, '徐勇',   '19900000006', NULL, 1789903521001328357, NULL, 1789903520997907818, 'Pm',           'Active', NOW(6), NULL,
   CAST(DATE_FORMAT(NOW(6), '%Y%m%d%H%i%s') AS UNSIGNED), CAST(DATE_FORMAT(NOW(6), '%Y%m%d') AS UNSIGNED), CAST(DATE_FORMAT(NOW(6), '%H%i%s') AS UNSIGNED), YEAR(NOW()), MONTH(NOW()), DAY(NOW()), HOUR(NOW()), MINUTE(NOW()), SECOND(NOW()), WEEKDAY(NOW()));

-- 本地验收口令统一为 Maihong2026；保存 BCrypt 哈希，明文不落库。
INSERT INTO db_tenant_credentials
  (cre_id, type, emp_id, phone, status, id_key, id_secret, password_hash, password_updated_at,
   failed_attempts, locked_until, must_change_password, last_time,
   create_date_time, create_date, create_time, create_year, create_month, create_day,
   create_hour, create_minute, create_second, create_week)
VALUES
  (1789905600000000001, 'Password', 1789903521009050562, '18660219918', 'Active', '18660219918', NULL, '$2a$10$dH9duwPL.cJeZw5C1ljvLe6IicvztSo23zq7L8ZdfBb.dHm5aZakC', NOW(6), 0, NULL, b'0', NULL,
   CAST(DATE_FORMAT(NOW(6), '%Y%m%d%H%i%s') AS UNSIGNED), CAST(DATE_FORMAT(NOW(6), '%Y%m%d') AS UNSIGNED), CAST(DATE_FORMAT(NOW(6), '%H%i%s') AS UNSIGNED), YEAR(NOW()), MONTH(NOW()), DAY(NOW()), HOUR(NOW()), MINUTE(NOW()), SECOND(NOW()), WEEKDAY(NOW())),
  (1789905600000000002, 'Password', 1789903522000000001, '19900000001', 'Active', '19900000001', NULL, '$2a$10$dH9duwPL.cJeZw5C1ljvLe6IicvztSo23zq7L8ZdfBb.dHm5aZakC', NOW(6), 0, NULL, b'0', NULL,
   CAST(DATE_FORMAT(NOW(6), '%Y%m%d%H%i%s') AS UNSIGNED), CAST(DATE_FORMAT(NOW(6), '%Y%m%d') AS UNSIGNED), CAST(DATE_FORMAT(NOW(6), '%H%i%s') AS UNSIGNED), YEAR(NOW()), MONTH(NOW()), DAY(NOW()), HOUR(NOW()), MINUTE(NOW()), SECOND(NOW()), WEEKDAY(NOW())),
  (1789905600000000003, 'Password', 1789903522000000002, '19900000002', 'Active', '19900000002', NULL, '$2a$10$dH9duwPL.cJeZw5C1ljvLe6IicvztSo23zq7L8ZdfBb.dHm5aZakC', NOW(6), 0, NULL, b'0', NULL,
   CAST(DATE_FORMAT(NOW(6), '%Y%m%d%H%i%s') AS UNSIGNED), CAST(DATE_FORMAT(NOW(6), '%Y%m%d') AS UNSIGNED), CAST(DATE_FORMAT(NOW(6), '%H%i%s') AS UNSIGNED), YEAR(NOW()), MONTH(NOW()), DAY(NOW()), HOUR(NOW()), MINUTE(NOW()), SECOND(NOW()), WEEKDAY(NOW())),
  (1789905600000000004, 'Password', 1789903522000000003, '19900000003', 'Active', '19900000003', NULL, '$2a$10$dH9duwPL.cJeZw5C1ljvLe6IicvztSo23zq7L8ZdfBb.dHm5aZakC', NOW(6), 0, NULL, b'0', NULL,
   CAST(DATE_FORMAT(NOW(6), '%Y%m%d%H%i%s') AS UNSIGNED), CAST(DATE_FORMAT(NOW(6), '%Y%m%d') AS UNSIGNED), CAST(DATE_FORMAT(NOW(6), '%H%i%s') AS UNSIGNED), YEAR(NOW()), MONTH(NOW()), DAY(NOW()), HOUR(NOW()), MINUTE(NOW()), SECOND(NOW()), WEEKDAY(NOW())),
  (1789905600000000005, 'Password', 1789903522000000004, '19900000004', 'Active', '19900000004', NULL, '$2a$10$dH9duwPL.cJeZw5C1ljvLe6IicvztSo23zq7L8ZdfBb.dHm5aZakC', NOW(6), 0, NULL, b'0', NULL,
   CAST(DATE_FORMAT(NOW(6), '%Y%m%d%H%i%s') AS UNSIGNED), CAST(DATE_FORMAT(NOW(6), '%Y%m%d') AS UNSIGNED), CAST(DATE_FORMAT(NOW(6), '%H%i%s') AS UNSIGNED), YEAR(NOW()), MONTH(NOW()), DAY(NOW()), HOUR(NOW()), MINUTE(NOW()), SECOND(NOW()), WEEKDAY(NOW())),
  (1789905600000000006, 'Password', 1789903522000000005, '19900000005', 'Active', '19900000005', NULL, '$2a$10$dH9duwPL.cJeZw5C1ljvLe6IicvztSo23zq7L8ZdfBb.dHm5aZakC', NOW(6), 0, NULL, b'0', NULL,
   CAST(DATE_FORMAT(NOW(6), '%Y%m%d%H%i%s') AS UNSIGNED), CAST(DATE_FORMAT(NOW(6), '%Y%m%d') AS UNSIGNED), CAST(DATE_FORMAT(NOW(6), '%H%i%s') AS UNSIGNED), YEAR(NOW()), MONTH(NOW()), DAY(NOW()), HOUR(NOW()), MINUTE(NOW()), SECOND(NOW()), WEEKDAY(NOW())),
  (1789905600000000007, 'Password', 1789903522000000006, '19900000006', 'Active', '19900000006', NULL, '$2a$10$dH9duwPL.cJeZw5C1ljvLe6IicvztSo23zq7L8ZdfBb.dHm5aZakC', NOW(6), 0, NULL, b'0', NULL,
   CAST(DATE_FORMAT(NOW(6), '%Y%m%d%H%i%s') AS UNSIGNED), CAST(DATE_FORMAT(NOW(6), '%Y%m%d') AS UNSIGNED), CAST(DATE_FORMAT(NOW(6), '%H%i%s') AS UNSIGNED), YEAR(NOW()), MONTH(NOW()), DAY(NOW()), HOUR(NOW()), MINUTE(NOW()), SECOND(NOW()), WEEKDAY(NOW()));

COMMIT;
