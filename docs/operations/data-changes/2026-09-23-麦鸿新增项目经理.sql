-- 麦鸿设计（com_id=1789903520997907818）新增 2 名项目经理：徐勇、张少康
-- 批次：MH-PM-20260923    目标库：本地开发库 decoboon
-- 说明：本地演示账号，沿用验收手机号段 19900000006/19900000007 与口令 Maihong2026
--       （口令哈希为 BCryptPasswordEncoder 的 $2a$10 结果，与既有 $2b$10 凭证等价可校验）
--       本次为用户明确指示“先不考虑订阅规则”，不执行套餐席位校验（当前 6 人 / 基础版 5 席）
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

INSERT INTO db_tenant_employee
  (emp_id, name, tel_no, avatar_url, store_id, team_id, com_id, role, status, joined_at, invite_id,
   create_date_time, create_date, create_time, create_year, create_month, create_day,
   create_hour, create_minute, create_second, create_week)
VALUES
  (1789903522000000006, '徐勇', '19900000006', NULL, 1789903521001328357, NULL, 1789903520997907818, 'Pm', 'Active', @now, NULL,
   @dateTime, @date, @time, @year, @month, @day, @hour, @minute, @second, @week),
  (1789903522000000007, '张少康', '19900000007', NULL, 1789903521001328357, NULL, 1789903520997907818, 'Pm', 'Active', @now, NULL,
   @dateTime, @date, @time, @year, @month, @day, @hour, @minute, @second, @week);

INSERT INTO db_tenant_credentials
  (cre_id, type, emp_id, phone, status, id_key, id_secret, password_hash, password_updated_at,
   failed_attempts, locked_until, must_change_password, last_time,
   create_date_time, create_date, create_time, create_year, create_month, create_day,
   create_hour, create_minute, create_second, create_week)
VALUES
  (1789905600000000006, 'Password', 1789903522000000006, '19900000006', 'Active', '19900000006', NULL,
   '$2a$10$dH9duwPL.cJeZw5C1ljvLe6IicvztSo23zq7L8ZdfBb.dHm5aZakC', @now, 0, NULL, b'0', NULL,
   @dateTime, @date, @time, @year, @month, @day, @hour, @minute, @second, @week),
  (1789905600000000007, 'Password', 1789903522000000007, '19900000007', 'Active', '19900000007', NULL,
   '$2a$10$nssaWMidwntD0NVwnAPXiOLpJ.00NFeknjNOPVvE9iAZdTlT/NIr6', @now, 0, NULL, b'0', NULL,
   @dateTime, @date, @time, @year, @month, @day, @hour, @minute, @second, @week);

COMMIT;
