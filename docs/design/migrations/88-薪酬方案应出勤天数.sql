-- 88-薪酬方案应出勤天数.sql
-- 目的：月应出勤天数归属员工薪酬方案，考勤保存时按方案写入当月快照，不再由前端默认 22 天。
-- 适用：MySQL 8.0+。生产执行前必须备份并登记迁移账本。

ALTER TABLE db_payroll_compensation_scheme
    ADD COLUMN scheduled_days INT NOT NULL DEFAULT 0 COMMENT '月应出勤天数' AFTER base_salary;

-- 历史方案按最近一条月度考勤快照回填；没有考勤事实的保留 0，编辑方案时必须人工确认。
UPDATE db_payroll_compensation_scheme s
SET s.scheduled_days = COALESCE((
    SELECT a.scheduled_days
    FROM db_payroll_attendance_summary a
    WHERE a.com_id = s.com_id
      AND a.emp_id = s.emp_id
      AND a.scheduled_days > 0
    ORDER BY a.payroll_month DESC
    LIMIT 1
), 0);

-- 回滚：
-- ALTER TABLE db_payroll_compensation_scheme DROP COLUMN scheduled_days;
