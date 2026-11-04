-- 135 · 月度考勤天数支持小数
-- 用途：实际业务存在半天请假，实际考勤天数必须按小数保存，不能再被 INTEGER 截断。
-- 口径：用户 2026-11-04 明确反馈；应出勤仍为整数，七类实际考勤天数最多保留两位小数。
-- 风险：L3；执行前必须备份 db_payroll_attendance_summary，并在隔离 MySQL 8 环境演练。

ALTER TABLE db_payroll_attendance_summary
    MODIFY COLUMN normal_days DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '正常出勤天数',
    MODIFY COLUMN personal_leave_days DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '事假天数',
    MODIFY COLUMN sick_leave_days DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '病假天数',
    MODIFY COLUMN absence_days DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '旷工天数',
    MODIFY COLUMN external_days DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '外勤天数',
    MODIFY COLUMN annual_leave_days DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '年假天数',
    MODIFY COLUMN comp_leave_days DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '调休天数';
