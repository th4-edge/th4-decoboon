-- CHG-20261113-001
-- Purpose: restore 18660219918 to the Rural Kitchen Renovation Boss identity.
-- This is a one-shot production data change. It fails closed when the expected
-- employee or credential state has drifted.

START TRANSACTION;

CREATE TEMPORARY TABLE chg_20261113_001_guard (
    ok TINYINT NOT NULL CHECK (ok = 1)
);

SET @credential_id = NULL;
SET @current_emp_id = NULL;
SELECT c.cre_id, c.emp_id
INTO @credential_id, @current_emp_id
FROM db_tenant_credentials c
WHERE c.phone = '18660219918'
  AND c.type = 'Wx'
  AND c.status = 'Active'
  AND c.emp_id = 1789938943545297099
FOR UPDATE;

SET @locked_current_emp_id = NULL;
SELECT emp_id INTO @locked_current_emp_id
FROM db_tenant_employee
WHERE emp_id = 1789938943545297099
FOR UPDATE;

SET @locked_target_emp_id = NULL;
SELECT emp_id INTO @locked_target_emp_id
FROM db_tenant_employee
WHERE emp_id = 1793707837268000001
FOR UPDATE;

INSERT INTO chg_20261113_001_guard (ok)
SELECT CASE
    WHEN @credential_id = 1789938911566015119
     AND @current_emp_id = 1789938943545297099
     AND @locked_current_emp_id = 1789938943545297099
     AND @locked_target_emp_id = 1793707837268000001
     AND EXISTS (
         SELECT 1
         FROM db_tenant_employee
         WHERE emp_id = 1789938943545297099
           AND com_id = 1790066191585976744
           AND store_id = 1790066191588014876
           AND tel_no = '18660219918'
           AND role = 'Boss'
           AND status = 'Active'
     )
     AND EXISTS (
         SELECT 1
         FROM db_tenant_employee
         WHERE emp_id = 1793707837268000001
           AND com_id = 1793516576023939150
           AND store_id = 1793516576031498271
           AND tel_no = '18660219918'
           AND role = 'Boss'
           AND status = 'Suspended'
     )
     AND EXISTS (
         SELECT 1
         FROM db_tenant_store
         WHERE store_id = 1793516576031498271
           AND company_com_id = 1793516576023939150
           AND status = 'Active'
     )
     AND EXISTS (
         SELECT 1
         FROM db_tenant_subscription
         WHERE com_id = 1793516576023939150
           AND status IN ('Active', 'Trial')
           AND service_end_date_time > CAST(DATE_FORMAT(NOW(), '%Y%m%d%H%i%s') AS UNSIGNED)
     )
     AND (
         SELECT COUNT(*)
         FROM db_tenant_employee
         WHERE com_id = 1793516576023939150
           AND status = 'Active'
     ) < (
         SELECT COALESCE(p.max_seats, 0) + s.extra_seats
         FROM db_tenant_subscription s
         JOIN db_tenant_plan p ON p.plan_id = s.plan_id
         WHERE s.com_id = 1793516576023939150
     )
    THEN 1
    ELSE 0
END;

UPDATE db_tenant_employee
SET status = 'Suspended'
WHERE emp_id = 1789938943545297099
  AND com_id = 1790066191585976744
  AND store_id = 1790066191588014876
  AND tel_no = '18660219918'
  AND role = 'Boss'
  AND status = 'Active';
SET @suspend_rows = ROW_COUNT();
UPDATE chg_20261113_001_guard SET ok = @suspend_rows;

UPDATE db_tenant_employee
SET status = 'Active',
    joined_at = NOW(6),
    resigned_at = NULL,
    resign_reason = NULL
WHERE emp_id = 1793707837268000001
  AND com_id = 1793516576023939150
  AND store_id = 1793516576031498271
  AND tel_no = '18660219918'
  AND role = 'Boss'
  AND status = 'Suspended';
SET @resume_rows = ROW_COUNT();
UPDATE chg_20261113_001_guard SET ok = @resume_rows;

UPDATE db_tenant_credentials
SET emp_id = 1793707837268000001
WHERE cre_id = 1789938911566015119
  AND phone = '18660219918'
  AND type = 'Wx'
  AND status = 'Active'
  AND emp_id = 1789938943545297099;
SET @credential_rows = ROW_COUNT();
UPDATE chg_20261113_001_guard SET ok = @credential_rows;

SET @changed_at = NOW(6);
SET @create_date = CAST(DATE_FORMAT(@changed_at, '%Y%m%d') AS UNSIGNED);
SET @create_time = CAST(DATE_FORMAT(@changed_at, '%H%i%s') AS UNSIGNED);
SET @create_date_time = CAST(DATE_FORMAT(@changed_at, '%Y%m%d%H%i%s') AS UNSIGNED);
SET @create_year = YEAR(@changed_at);
SET @create_month = MONTH(@changed_at);
SET @create_day = DAY(@changed_at);
SET @create_hour = HOUR(@changed_at);
SET @create_minute = MINUTE(@changed_at);
SET @create_second = SECOND(@changed_at);
SET @create_week = WEEK(@changed_at, 0);
SET @reason = '用户要求将 18660219918 / 张伟恢复为农村厨房改造企业负责人';

SELECT COALESCE(MAX(log_id), 0) INTO @max_member_log_id
FROM db_company_member_log;

INSERT INTO db_company_member_log (
    log_id, action, after_snapshot, before_snapshot,
    create_date, create_date_time, create_day, create_hour, create_minute,
    create_month, create_second, create_time, create_week, create_year,
    reason, com_id, emp_id, operator_id, user_id
) VALUES (
    @max_member_log_id + 1,
    'Suspend',
    '{"role":"Boss","empId":1789938943545297099,"status":"Suspended","companyId":1790066191585976744}',
    '{"role":"Boss","empId":1789938943545297099,"status":"Active","companyId":1790066191585976744}',
    @create_date, @create_date_time, @create_day, @create_hour, @create_minute,
    @create_month, @create_second, @create_time, @create_week, @create_year,
    @reason, 1790066191585976744, 1789938943545297099, NULL, 1789938911566015119
);

INSERT INTO db_company_member_log (
    log_id, action, after_snapshot, before_snapshot,
    create_date, create_date_time, create_day, create_hour, create_minute,
    create_month, create_second, create_time, create_week, create_year,
    reason, com_id, emp_id, operator_id, user_id
) VALUES (
    @max_member_log_id + 2,
    'Resume',
    '{"role":"Boss","empId":1793707837268000001,"status":"Active","companyId":1793516576023939150}',
    '{"role":"Boss","empId":1793707837268000001,"status":"Suspended","companyId":1793516576023939150}',
    @create_date, @create_date_time, @create_day, @create_hour, @create_minute,
    @create_month, @create_second, @create_time, @create_week, @create_year,
    @reason, 1793516576023939150, 1793707837268000001, NULL, 1789938911566015119
);

SELECT COALESCE(MAX(log_id), 0) INTO @max_security_log_id
FROM db_user_security_log;

INSERT INTO db_user_security_log (
    log_id, action, com_id, create_date, create_date_time, create_day,
    create_hour, create_minute, create_month, create_second, create_time,
    create_week, create_year, emp_id, ip, open_id, phone, remark, result,
    user_agent, user_id
) VALUES (
    @max_security_log_id + 1,
    'CompanySwitch',
    1793516576023939150,
    @create_date, @create_date_time, @create_day,
    @create_hour, @create_minute, @create_month, @create_second, @create_time,
    @create_week, @create_year,
    1793707837268000001,
    NULL,
    (SELECT id_key FROM db_tenant_credentials WHERE cre_id = 1789938911566015119),
    '18660219918',
    @reason,
    'Success',
    NULL,
    1789938911566015119
);

COMMIT;
