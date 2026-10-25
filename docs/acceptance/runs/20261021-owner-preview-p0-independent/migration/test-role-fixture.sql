-- 仅用于 decoboon-test / ZDQ造数M 专用测试企业。
START TRANSACTION;
SET @now=NOW(6);
SET @hash=(SELECT password_hash FROM db_tenant_credentials WHERE phone='13910000100' LIMIT 1);

INSERT INTO db_tenant_team
 (team_id,color,name,com_id,leader_emp_id,store_id,create_date_time,create_date,create_time,create_year,create_month,create_day,create_hour,create_minute,create_second,create_week)
VALUES
 (999200000000000901,'#2F7D5A','A店业主预览验收团队',999200000000000001,999200000000000025,999200000000000011,
  CAST(DATE_FORMAT(@now,'%Y%m%d%H%i%s') AS UNSIGNED),CAST(DATE_FORMAT(@now,'%Y%m%d') AS UNSIGNED),CAST(DATE_FORMAT(@now,'%H%i%s') AS UNSIGNED),YEAR(@now),MONTH(@now),DAY(@now),HOUR(@now),MINUTE(@now),SECOND(@now),WEEKDAY(@now)),
 (999200000000000902,'#7A7A72','B店异队',999200000000000001,999200000000000025,999200000000000012,
  CAST(DATE_FORMAT(@now,'%Y%m%d%H%i%s') AS UNSIGNED),CAST(DATE_FORMAT(@now,'%Y%m%d') AS UNSIGNED),CAST(DATE_FORMAT(@now,'%H%i%s') AS UNSIGNED),YEAR(@now),MONTH(@now),DAY(@now),HOUR(@now),MINUTE(@now),SECOND(@now),WEEKDAY(@now));

INSERT INTO db_tenant_employee
 (emp_id,avatar_url,joined_at,name,role,status,tel_no,com_id,invite_id,store_id,team_id,create_date_time,create_date,create_time,create_year,create_month,create_day,create_hour,create_minute,create_second,create_week)
VALUES
 (999200000000000041,NULL,@now,'A店验收团队长','TeamLeader','Active','13910000109',999200000000000001,NULL,999200000000000011,999200000000000901,
  CAST(DATE_FORMAT(@now,'%Y%m%d%H%i%s') AS UNSIGNED),CAST(DATE_FORMAT(@now,'%Y%m%d') AS UNSIGNED),CAST(DATE_FORMAT(@now,'%H%i%s') AS UNSIGNED),YEAR(@now),MONTH(@now),DAY(@now),HOUR(@now),MINUTE(@now),SECOND(@now),WEEKDAY(@now)),
 (999200000000000042,NULL,@now,'B店异队团队长','TeamLeader','Active','13910000119',999200000000000001,NULL,999200000000000012,999200000000000902,
  CAST(DATE_FORMAT(@now,'%Y%m%d%H%i%s') AS UNSIGNED),CAST(DATE_FORMAT(@now,'%Y%m%d') AS UNSIGNED),CAST(DATE_FORMAT(@now,'%H%i%s') AS UNSIGNED),YEAR(@now),MONTH(@now),DAY(@now),HOUR(@now),MINUTE(@now),SECOND(@now),WEEKDAY(@now));

UPDATE db_tenant_team SET leader_emp_id=999200000000000041 WHERE team_id=999200000000000901;
UPDATE db_tenant_team SET leader_emp_id=999200000000000042 WHERE team_id=999200000000000902;

INSERT INTO db_tenant_credentials
 (cre_id,type,emp_id,phone,status,id_key,id_secret,password_hash,password_updated_at,failed_attempts,locked_until,must_change_password,last_time,create_date_time,create_date,create_time,create_year,create_month,create_day,create_hour,create_minute,create_second,create_week)
VALUES
 (999200000000000821,'Password',999200000000000041,'13910000109','Active','13910000109',NULL,@hash,@now,0,NULL,b'0',NULL,
  CAST(DATE_FORMAT(@now,'%Y%m%d%H%i%s') AS UNSIGNED),CAST(DATE_FORMAT(@now,'%Y%m%d') AS UNSIGNED),CAST(DATE_FORMAT(@now,'%H%i%s') AS UNSIGNED),YEAR(@now),MONTH(@now),DAY(@now),HOUR(@now),MINUTE(@now),SECOND(@now),WEEKDAY(@now)),
 (999200000000000822,'Password',999200000000000042,'13910000119','Active','13910000119',NULL,@hash,@now,0,NULL,b'0',NULL,
  CAST(DATE_FORMAT(@now,'%Y%m%d%H%i%s') AS UNSIGNED),CAST(DATE_FORMAT(@now,'%Y%m%d') AS UNSIGNED),CAST(DATE_FORMAT(@now,'%H%i%s') AS UNSIGNED),YEAR(@now),MONTH(@now),DAY(@now),HOUR(@now),MINUTE(@now),SECOND(@now),WEEKDAY(@now));

UPDATE db_project SET team_id=999200000000000901, support_id=999200000000000029 WHERE project_id=235520576473530368;
UPDATE cust_customer SET team_id=999200000000000901 WHERE cust_id=235520573667540992;
COMMIT;
