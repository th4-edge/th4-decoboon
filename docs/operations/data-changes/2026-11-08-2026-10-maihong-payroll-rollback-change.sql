-- CHG-20261108-002
-- 麦鸿装饰 2026-10 工资单错误确认回退。
-- 目标：保留工资单头但恢复为 Draft；删除本次确认生成的工资项、应付、工资条；
--       考勤从 InPayroll 退回 Pending，清除确认章，供财务重新核算。
-- 执行前必须完成相关表备份，并确认以下常量与变更记录一致。

SET @com_id := 1790066191585976744;
SET @sheet_id := 243634477786464256;
SET @period := '2026-10';
SET @confirmed_by := 1790066191593026092;
SET @confirmed_at := '2026-11-08 15:48:57.605097';
SET @reason := 'CHG-20261108-002：2026-10工资单在财务复核前被企业负责人从草稿直接确认，回退为草稿并解锁考勤重新核算';

DROP PROCEDURE IF EXISTS tmp_chg_20261108_002_rollback_payroll;
DELIMITER //
CREATE PROCEDURE tmp_chg_20261108_002_rollback_payroll()
BEGIN
    DECLARE v_check INT DEFAULT 0;
    DECLARE v_rows INT DEFAULT 0;
    DECLARE v_now DATETIME(6);
    DECLARE v_create_date INT;
    DECLARE v_create_date_time BIGINT;
    DECLARE v_create_time INT;
    DECLARE v_create_year INT;
    DECLARE v_create_month INT;
    DECLARE v_create_day INT;
    DECLARE v_create_hour INT;
    DECLARE v_create_minute INT;
    DECLARE v_create_second INT;
    DECLARE v_create_week INT;

    START TRANSACTION;

    SELECT COUNT(*) INTO v_check
    FROM db_payroll_sheet
    WHERE sheet_id = @sheet_id
      AND com_id = @com_id
      AND period = @period
      AND status = 'Confirmed'
      AND attendance_complete = 1
      AND total_gross_amount = 21000.00
      AND total_deduction_amount = 0.00
      AND total_net_amount = 21000.00
      AND confirmed_by = @confirmed_by
      AND confirmed_at = @confirmed_at;
    IF v_check <> 1 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '工资单头与已确认前置事实不一致';
    END IF;

    SELECT COUNT(*) INTO v_check
    FROM db_payroll_item
    WHERE com_id = @com_id
      AND sheet_id = @sheet_id
      AND status = 'Confirmed';
    IF v_check <> 5 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '已确认工资项数量不是预期的5条';
    END IF;

    SELECT COUNT(*) INTO v_check
    FROM db_finance_payable
    WHERE com_id = @com_id
      AND source_type = 'PayrollItem'
      AND source_id IN (
          SELECT item_id FROM db_payroll_item WHERE com_id = @com_id AND sheet_id = @sheet_id
      )
      AND status = 'Pending'
      AND paid_amount = 0;
    IF v_check <> 5 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '工资应付数量或状态不是预期的5条未付款记录';
    END IF;

    SELECT COUNT(*) INTO v_check
    FROM db_finance_payment p
    JOIN db_finance_payable b ON b.payable_id = p.payable_id
    WHERE b.com_id = @com_id
      AND b.source_type = 'PayrollItem'
      AND b.source_id IN (
          SELECT item_id FROM db_payroll_item WHERE com_id = @com_id AND sheet_id = @sheet_id
      );
    IF v_check <> 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '工资应付已存在付款事实，禁止回退';
    END IF;

    SELECT COUNT(*) INTO v_check
    FROM db_finance_payable_settlement s
    JOIN db_finance_payable b ON b.payable_id = s.payable_id
    WHERE b.com_id = @com_id
      AND b.source_type = 'PayrollItem'
      AND b.source_id IN (
          SELECT item_id FROM db_payroll_item WHERE com_id = @com_id AND sheet_id = @sheet_id
      );
    IF v_check <> 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '工资应付已存在核销事实，禁止回退';
    END IF;

    SELECT COUNT(*) INTO v_check
    FROM db_finance_expense e
    JOIN db_finance_payable b ON b.payable_id = e.payable_id
    WHERE b.com_id = @com_id
      AND b.source_type = 'PayrollItem'
      AND b.source_id IN (
          SELECT item_id FROM db_payroll_item WHERE com_id = @com_id AND sheet_id = @sheet_id
      );
    IF v_check <> 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '工资应付已关联费用事实，禁止回退';
    END IF;

    SELECT COUNT(*) INTO v_check
    FROM db_payroll_payslip
    WHERE com_id = @com_id
      AND sheet_id = @sheet_id
      AND period = @period
      AND status = 'PendingPayment'
      AND published_at IS NULL;
    IF v_check <> 5 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '工资条数量、状态或发布状态不符合回退前置';
    END IF;

    SELECT COUNT(*) INTO v_check
    FROM db_payroll_commission_item
    WHERE com_id = @com_id
      AND payroll_item_id IN (
          SELECT item_id FROM db_payroll_item WHERE com_id = @com_id AND sheet_id = @sheet_id
      );
    IF v_check <> 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '工资项已关联提成条目，禁止回退';
    END IF;

    SELECT COUNT(*) INTO v_check
    FROM db_payroll_commission_adjustment
    WHERE com_id = @com_id
      AND payroll_item_id IN (
          SELECT item_id FROM db_payroll_item WHERE com_id = @com_id AND sheet_id = @sheet_id
      );
    IF v_check <> 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '工资项已关联提成调整，禁止回退';
    END IF;

    SELECT COUNT(*) INTO v_check
    FROM db_payroll_attendance_summary a
    WHERE a.com_id = @com_id
      AND a.payroll_month = @period
      AND a.status = 'InPayroll'
      AND a.emp_id IN (
          SELECT emp_id FROM db_payroll_item WHERE com_id = @com_id AND sheet_id = @sheet_id
      );
    IF v_check <> 5 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '考勤锁定记录不是预期的5条';
    END IF;

    SET v_now := NOW(6);
    SET v_create_date := CAST(DATE_FORMAT(v_now, '%Y%m%d') AS UNSIGNED);
    SET v_create_date_time := CAST(DATE_FORMAT(v_now, '%Y%m%d%H%i%s') AS UNSIGNED);
    SET v_create_time := CAST(DATE_FORMAT(v_now, '%H%i%s') AS UNSIGNED);
    SET v_create_year := YEAR(v_now);
    SET v_create_month := MONTH(v_now);
    SET v_create_day := DAY(v_now);
    SET v_create_hour := HOUR(v_now);
    SET v_create_minute := MINUTE(v_now);
    SET v_create_second := SECOND(v_now);
    SET v_create_week := WEEKDAY(v_now);

    UPDATE db_payroll_attendance_summary
    SET status = 'Pending',
        confirmed_by = NULL,
        confirmed_at = NULL,
        update_date = v_create_date,
        update_date_time = v_create_date_time,
        update_year = v_create_year,
        update_month = v_create_month,
        update_day = v_create_day,
        update_hour = v_create_hour,
        update_minute = v_create_minute,
        update_second = v_create_second,
        update_week = v_create_week,
        update_time = v_create_time
    WHERE com_id = @com_id
      AND payroll_month = @period
      AND status = 'InPayroll'
      AND emp_id IN (
          SELECT emp_id FROM db_payroll_item WHERE com_id = @com_id AND sheet_id = @sheet_id
      );
    SET v_rows = ROW_COUNT();
    IF v_rows <> 5 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '考勤退回待确认失败';
    END IF;

    DELETE FROM db_payroll_payslip
    WHERE com_id = @com_id
      AND sheet_id = @sheet_id
      AND period = @period
      AND status = 'PendingPayment'
      AND published_at IS NULL;
    SET v_rows := ROW_COUNT();
    IF v_rows <> 5 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '删除工资条数量不符合预期';
    END IF;

    DELETE b FROM db_finance_payable b
    WHERE b.com_id = @com_id
      AND b.source_type = 'PayrollItem'
      AND b.source_id IN (
          SELECT item_id FROM db_payroll_item WHERE com_id = @com_id AND sheet_id = @sheet_id
      )
      AND b.status = 'Pending'
      AND b.paid_amount = 0;
    SET v_rows := ROW_COUNT();
    IF v_rows <> 5 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '删除工资应付数量不符合预期';
    END IF;

    DELETE FROM db_payroll_item
    WHERE com_id = @com_id
      AND sheet_id = @sheet_id
      AND status = 'Confirmed';
    SET v_rows := ROW_COUNT();
    IF v_rows <> 5 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '删除工资项数量不符合预期';
    END IF;

    UPDATE db_payroll_sheet
    SET status = 'Draft',
        attendance_complete = 0,
        commission_complete = 0,
        total_gross_amount = 0.00,
        total_deduction_amount = 0.00,
        total_net_amount = 0.00,
        confirmed_by = NULL,
        confirmed_at = NULL,
        remark = @reason,
        update_date = v_create_date,
        update_date_time = v_create_date_time,
        update_year = v_create_year,
        update_month = v_create_month,
        update_day = v_create_day,
        update_hour = v_create_hour,
        update_minute = v_create_minute,
        update_second = v_create_second,
        update_week = v_create_week,
        update_time = v_create_time
    WHERE sheet_id = @sheet_id
      AND com_id = @com_id
      AND status = 'Confirmed';
    SET v_rows = ROW_COUNT();
    IF v_rows <> 1 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '工资单回退草稿失败';
    END IF;

    SELECT
        'CHG_20261108_002_PAYROLL_ROLLBACK_APPLIED' AS result,
        @sheet_id AS sheet_id,
        @period AS period,
        'Draft' AS sheet_status,
        5 AS deleted_items,
        5 AS deleted_payables,
        5 AS deleted_payslips,
        5 AS unlocked_attendances;

    COMMIT;
END//
DELIMITER ;

SELECT GET_LOCK('decoboon:chg:20261108:002', 10) AS lock_acquired;
CALL tmp_chg_20261108_002_rollback_payroll();
DROP PROCEDURE IF EXISTS tmp_chg_20261108_002_rollback_payroll;
SELECT RELEASE_LOCK('decoboon:chg:20261108:002') AS lock_released;
