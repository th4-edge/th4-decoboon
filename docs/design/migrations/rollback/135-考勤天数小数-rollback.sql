-- 135 rollback · 月度考勤天数恢复整数
-- 安全门禁：存在小数考勤数据时直接报错并停止，禁止通过隐式截断丢数据。
DELIMITER $$
DROP PROCEDURE IF EXISTS rollback_135_attendance_decimal_days$$
CREATE PROCEDURE rollback_135_attendance_decimal_days()
BEGIN
    DECLARE fractional_rows BIGINT DEFAULT 0;

    SELECT COUNT(*) INTO fractional_rows
    FROM db_payroll_attendance_summary
    WHERE normal_days <> TRUNCATE(normal_days, 0)
       OR personal_leave_days <> TRUNCATE(personal_leave_days, 0)
       OR sick_leave_days <> TRUNCATE(sick_leave_days, 0)
       OR absence_days <> TRUNCATE(absence_days, 0)
       OR external_days <> TRUNCATE(external_days, 0)
       OR annual_leave_days <> TRUNCATE(annual_leave_days, 0)
       OR comp_leave_days <> TRUNCATE(comp_leave_days, 0);

    IF fractional_rows > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '135 rollback blocked: fractional attendance days exist';
    END IF;

    ALTER TABLE db_payroll_attendance_summary
        MODIFY COLUMN normal_days INTEGER NOT NULL DEFAULT 0 COMMENT '正常出勤天数',
        MODIFY COLUMN personal_leave_days INTEGER NOT NULL DEFAULT 0 COMMENT '事假天数',
        MODIFY COLUMN sick_leave_days INTEGER NOT NULL DEFAULT 0 COMMENT '病假天数',
        MODIFY COLUMN absence_days INTEGER NOT NULL DEFAULT 0 COMMENT '旷工天数',
        MODIFY COLUMN external_days INTEGER NOT NULL DEFAULT 0 COMMENT '外勤天数',
        MODIFY COLUMN annual_leave_days INTEGER NOT NULL DEFAULT 0 COMMENT '年假天数',
        MODIFY COLUMN comp_leave_days INTEGER NOT NULL DEFAULT 0 COMMENT '调休天数';
END$$
DELIMITER ;

CALL rollback_135_attendance_decimal_days();
DROP PROCEDURE rollback_135_attendance_decimal_days;
