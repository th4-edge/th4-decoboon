-- 2026-11-04 工资项存在考勤状态回填草案（HIGH RISK，未执行）
-- 执行前必须完成三表备份、用户确认，并先运行同名 Markdown 中的只读核对。

SELECT
    s.period,
    s.status AS sheet_status,
    COUNT(DISTINCT a.summary_id) AS affected_attendances
FROM db_payroll_attendance_summary a
JOIN db_payroll_sheet s
  ON s.com_id = a.com_id
 AND s.period = a.payroll_month
WHERE a.status = 'Confirmed'
  AND EXISTS (
      SELECT 1
      FROM db_payroll_item i
      WHERE i.com_id = a.com_id
        AND i.sheet_id = s.sheet_id
        AND i.emp_id = a.emp_id
  )
GROUP BY s.period, s.status
ORDER BY s.period, s.status;

CREATE TABLE bak_payroll_attendance_inpayroll_20261104 AS
SELECT DISTINCT
    a.summary_id,
    a.com_id,
    a.emp_id,
    a.payroll_month,
    a.status AS before_status
FROM db_payroll_attendance_summary a
JOIN db_payroll_sheet s
  ON s.com_id = a.com_id
 AND s.period = a.payroll_month
WHERE a.status = 'Confirmed'
  AND EXISTS (
      SELECT 1
      FROM db_payroll_item i
      WHERE i.com_id = a.com_id
        AND i.sheet_id = s.sheet_id
        AND i.emp_id = a.emp_id
  );

UPDATE db_payroll_attendance_summary a
JOIN bak_payroll_attendance_inpayroll_20261104 b
  ON b.summary_id = a.summary_id
SET a.status = 'InPayroll'
WHERE a.status = 'Confirmed';

SELECT
    a.summary_id,
    a.emp_id,
    a.payroll_month,
    a.status
FROM db_payroll_attendance_summary a
JOIN bak_payroll_attendance_inpayroll_20261104 b
  ON b.summary_id = a.summary_id
WHERE a.status <> 'InPayroll';
