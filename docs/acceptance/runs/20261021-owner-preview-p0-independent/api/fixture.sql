-- 独立验收夹具；仅执行于 decoboon_owner_preview_acceptance 隔离克隆库。
-- 不改产品代码、不创建 CustomerAccess、不生成业主 token。
START TRANSACTION;
SET @hash = '$2a$10$dH9duwPL.cJeZw5C1ljvLe6IicvztSo23zq7L8ZdfBb.dHm5aZakC';
SET @now = NOW(6);

-- 同企业第二门店、两个团队：用于跨店/跨团队负向用例。
INSERT INTO db_tenant_store
  (store_id, addr, color, default_store, status, sort, name, company_com_id,
   create_date_time, create_date, create_time, create_year, create_month, create_day,
   create_hour, create_minute, create_second, create_week)
VALUES
  (1902102100000000103, NULL, NULL, b'0', 'Active', 1, '验收二店', 1790066191585976744,
   CAST(DATE_FORMAT(@now, '%Y%m%d%H%i%s') AS UNSIGNED), CAST(DATE_FORMAT(@now, '%Y%m%d') AS UNSIGNED),
   CAST(DATE_FORMAT(@now, '%H%i%s') AS UNSIGNED), YEAR(@now), MONTH(@now), DAY(@now), HOUR(@now), MINUTE(@now), SECOND(@now), WEEKDAY(@now));

-- 团队先由现有 BOSS 占位为负责人，员工建立后再更新为实际负责人。
INSERT INTO db_tenant_team
  (team_id, color, name, com_id, leader_emp_id, store_id,
   create_date_time, create_date, create_time, create_year, create_month, create_day,
   create_hour, create_minute, create_second, create_week)
VALUES
  (1902102100000000101, '#2F7D5A', '业主预览验收团队', 1790066191585976744, 1789938943545297099, 1790066191588014876,
   CAST(DATE_FORMAT(@now, '%Y%m%d%H%i%s') AS UNSIGNED), CAST(DATE_FORMAT(@now, '%Y%m%d') AS UNSIGNED),
   CAST(DATE_FORMAT(@now, '%H%i%s') AS UNSIGNED), YEAR(@now), MONTH(@now), DAY(@now), HOUR(@now), MINUTE(@now), SECOND(@now), WEEKDAY(@now)),
  (1902102100000000102, '#7A7A72', '业主预览异队', 1790066191585976744, 1789938943545297099, 1790066191588014876,
   CAST(DATE_FORMAT(@now, '%Y%m%d%H%i%s') AS UNSIGNED), CAST(DATE_FORMAT(@now, '%Y%m%d') AS UNSIGNED),
   CAST(DATE_FORMAT(@now, '%H%i%s') AS UNSIGNED), YEAR(@now), MONTH(@now), DAY(@now), HOUR(@now), MINUTE(@now), SECOND(@now), WEEKDAY(@now));

INSERT INTO db_tenant_employee
  (emp_id, avatar_url, joined_at, name, role, status, tel_no, com_id, invite_id, store_id, team_id,
   create_date_time, create_date, create_time, create_year, create_month, create_day,
   create_hour, create_minute, create_second, create_week)
VALUES
  (1902102100000000001, NULL, @now, '验收团队负责人', 'TeamLeader', 'Active', '13910000003', 1790066191585976744, NULL, 1790066191588014876, 1902102100000000101,
   CAST(DATE_FORMAT(@now, '%Y%m%d%H%i%s') AS UNSIGNED), CAST(DATE_FORMAT(@now, '%Y%m%d') AS UNSIGNED),
   CAST(DATE_FORMAT(@now, '%H%i%s') AS UNSIGNED), YEAR(@now), MONTH(@now), DAY(@now), HOUR(@now), MINUTE(@now), SECOND(@now), WEEKDAY(@now)),
  (1902102100000000002, NULL, @now, '异队负责人', 'TeamLeader', 'Active', '13910000010', 1790066191585976744, NULL, 1790066191588014876, 1902102100000000102,
   CAST(DATE_FORMAT(@now, '%Y%m%d%H%i%s') AS UNSIGNED), CAST(DATE_FORMAT(@now, '%Y%m%d') AS UNSIGNED),
   CAST(DATE_FORMAT(@now, '%H%i%s') AS UNSIGNED), YEAR(@now), MONTH(@now), DAY(@now), HOUR(@now), MINUTE(@now), SECOND(@now), WEEKDAY(@now)),
  (1902102100000000003, NULL, @now, '异店店长', 'StoreManager', 'Active', '13910000011', 1790066191585976744, NULL, 1902102100000000103, NULL,
   CAST(DATE_FORMAT(@now, '%Y%m%d%H%i%s') AS UNSIGNED), CAST(DATE_FORMAT(@now, '%Y%m%d') AS UNSIGNED),
   CAST(DATE_FORMAT(@now, '%H%i%s') AS UNSIGNED), YEAR(@now), MONTH(@now), DAY(@now), HOUR(@now), MINUTE(@now), SECOND(@now), WEEKDAY(@now));

UPDATE db_tenant_team SET leader_emp_id = 1902102100000000001 WHERE team_id = 1902102100000000101;
UPDATE db_tenant_team SET leader_emp_id = 1902102100000000002 WHERE team_id = 1902102100000000102;

-- 指定验收项目：指定设计师参与、Pm 作为 support 参与、客户归属主团队。
UPDATE db_tenant_employee SET tel_no='13910000001' WHERE emp_id=1789938943545297099;
UPDATE db_tenant_employee SET tel_no='13910000002' WHERE emp_id=1790221790492843136;
UPDATE db_tenant_employee SET tel_no='13910000004' WHERE emp_id=1790221396230505495;
UPDATE db_tenant_employee SET tel_no='13910000005' WHERE emp_id=1790224826490830054;
UPDATE db_tenant_employee SET tel_no='13910000006' WHERE emp_id=1790221992067828768;
UPDATE db_tenant_employee SET tel_no='13910000007' WHERE emp_id=1790222057930837664;
UPDATE db_tenant_employee SET tel_no='13910000008' WHERE emp_id=1790230148482841967;
UPDATE db_tenant_employee SET tel_no='13910000009' WHERE emp_id=1789989046754654238;

UPDATE db_project SET support_id=1790224826490830054, store_id=1790066191588014876, team_id=1902102100000000101
 WHERE project_id=232781889880981504;
UPDATE cust_customer SET team_id=1902102100000000101, store_id=1790066191588014876, emp_id=1790222057930837664
 WHERE cust_id=232781889855815680;

-- 既有微信凭证仅在本隔离库改为测试密码凭证，便于真实 Provider 登录。
UPDATE db_tenant_credentials SET type='Password', phone='13910000001', id_key='13910000001', id_secret=NULL,
 password_hash=@hash, password_updated_at=@now, failed_attempts=0, locked_until=NULL, must_change_password=b'0', status='Active'
 WHERE emp_id=1789938943545297099;
UPDATE db_tenant_credentials SET type='Password', phone='13910000002', id_key='13910000002', id_secret=NULL,
 password_hash=@hash, password_updated_at=@now, failed_attempts=0, locked_until=NULL, must_change_password=b'0', status='Active'
 WHERE emp_id=1790221790492843136;
UPDATE db_tenant_credentials SET type='Password', phone='13910000004', id_key='13910000004', id_secret=NULL,
 password_hash=@hash, password_updated_at=@now, failed_attempts=0, locked_until=NULL, must_change_password=b'0', status='Active'
 WHERE emp_id=1790221396230505495;
UPDATE db_tenant_credentials SET type='Password', phone='13910000005', id_key='13910000005', id_secret=NULL,
 password_hash=@hash, password_updated_at=@now, failed_attempts=0, locked_until=NULL, must_change_password=b'0', status='Active'
 WHERE emp_id=1790224826490830054;
UPDATE db_tenant_credentials SET type='Password', phone='13910000006', id_key='13910000006', id_secret=NULL,
 password_hash=@hash, password_updated_at=@now, failed_attempts=0, locked_until=NULL, must_change_password=b'0', status='Active'
 WHERE emp_id=1790221992067828768;
UPDATE db_tenant_credentials SET type='Password', phone='13910000007', id_key='13910000007', id_secret=NULL,
 password_hash=@hash, password_updated_at=@now, failed_attempts=0, locked_until=NULL, must_change_password=b'0', status='Active'
 WHERE emp_id=1790222057930837664;
UPDATE db_tenant_credentials SET type='Password', phone='13910000008', id_key='13910000008', id_secret=NULL,
 password_hash=@hash, password_updated_at=@now, failed_attempts=0, locked_until=NULL, must_change_password=b'0', status='Active'
 WHERE emp_id=1790230148482841967;

INSERT INTO db_tenant_credentials
  (cre_id, type, emp_id, phone, status, id_key, id_secret, password_hash, password_updated_at,
   failed_attempts, locked_until, must_change_password, last_time,
   create_date_time, create_date, create_time, create_year, create_month, create_day,
   create_hour, create_minute, create_second, create_week)
VALUES
  (1902102100000000201, 'Password', 1902102100000000001, '13910000003', 'Active', '13910000003', NULL, @hash, @now, 0, NULL, b'0', NULL,
   CAST(DATE_FORMAT(@now, '%Y%m%d%H%i%s') AS UNSIGNED), CAST(DATE_FORMAT(@now, '%Y%m%d') AS UNSIGNED),
   CAST(DATE_FORMAT(@now, '%H%i%s') AS UNSIGNED), YEAR(@now), MONTH(@now), DAY(@now), HOUR(@now), MINUTE(@now), SECOND(@now), WEEKDAY(@now)),
  (1902102100000000202, 'Password', 1902102100000000002, '13910000010', 'Active', '13910000010', NULL, @hash, @now, 0, NULL, b'0', NULL,
   CAST(DATE_FORMAT(@now, '%Y%m%d%H%i%s') AS UNSIGNED), CAST(DATE_FORMAT(@now, '%Y%m%d') AS UNSIGNED),
   CAST(DATE_FORMAT(@now, '%H%i%s') AS UNSIGNED), YEAR(@now), MONTH(@now), DAY(@now), HOUR(@now), MINUTE(@now), SECOND(@now), WEEKDAY(@now)),
  (1902102100000000203, 'Password', 1902102100000000003, '13910000011', 'Active', '13910000011', NULL, @hash, @now, 0, NULL, b'0', NULL,
   CAST(DATE_FORMAT(@now, '%Y%m%d%H%i%s') AS UNSIGNED), CAST(DATE_FORMAT(@now, '%Y%m%d') AS UNSIGNED),
   CAST(DATE_FORMAT(@now, '%H%i%s') AS UNSIGNED), YEAR(@now), MONTH(@now), DAY(@now), HOUR(@now), MINUTE(@now), SECOND(@now), WEEKDAY(@now)),
  (1902102100000000204, 'Password', 1789989046754654238, '13910000009', 'Active', '13910000009', NULL, @hash, @now, 0, NULL, b'0', NULL,
   CAST(DATE_FORMAT(@now, '%Y%m%d%H%i%s') AS UNSIGNED), CAST(DATE_FORMAT(@now, '%Y%m%d') AS UNSIGNED),
   CAST(DATE_FORMAT(@now, '%H%i%s') AS UNSIGNED), YEAR(@now), MONTH(@now), DAY(@now), HOUR(@now), MINUTE(@now), SECOND(@now), WEEKDAY(@now));

COMMIT;
