-- CHG-20261108-002 回滚
-- 将 2026-10 麦鸿工资单恢复到错误确认时的状态：
-- Confirmed + 5 条 Confirmed 工资项 + 5 条 Pending 应付 + 5 条 PendingPayment 工资条 + 5 条 InPayroll 考勤。
-- 仅在财务尚未重新生成工资单时可执行；执行前确认 affected-rows.sql 备份完整。

SET @com_id := 1790066191585976744;
SET @sheet_id := 243634477786464256;
SET @period := '2026-10';
SET @confirmed_by := 1790066191593026092;
SET @confirmed_at := '2026-11-08 15:48:57.605097';

DROP PROCEDURE IF EXISTS tmp_chg_20261108_002_restore_payroll;
DELIMITER //
CREATE PROCEDURE tmp_chg_20261108_002_restore_payroll()
BEGIN
    DECLARE v_check INT DEFAULT 0;

    START TRANSACTION;

    SELECT COUNT(*) INTO v_check
    FROM db_payroll_sheet
    WHERE sheet_id = @sheet_id
      AND com_id = @com_id
      AND period = @period
      AND status = 'Draft'
      AND attendance_complete = 0
      AND total_gross_amount = 0.00
      AND total_deduction_amount = 0.00
      AND total_net_amount = 0.00
      AND confirmed_by IS NULL
      AND confirmed_at IS NULL;
    IF v_check <> 1 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '工资单不是可恢复的已回退草稿状态';
    END IF;

    SELECT COUNT(*) INTO v_check
    FROM db_payroll_item
    WHERE com_id = @com_id AND sheet_id = @sheet_id;
    IF v_check <> 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '工资单已存在工资项，禁止覆盖恢复';
    END IF;

    SELECT COUNT(*) INTO v_check
    FROM db_finance_payable
    WHERE com_id = @com_id
      AND source_type = 'PayrollItem'
      AND source_id IN (243634478642102272,243634478491107328,243634478377861120,243634478965063680,243634478839234560);
    IF v_check <> 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '工资应付已存在，禁止覆盖恢复';
    END IF;

    SELECT COUNT(*) INTO v_check
    FROM db_payroll_payslip
    WHERE com_id = @com_id AND sheet_id = @sheet_id;
    IF v_check <> 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '工资条已存在，禁止覆盖恢复';
    END IF;

    SELECT COUNT(*) INTO v_check
    FROM db_payroll_attendance_summary
    WHERE com_id = @com_id
      AND payroll_month = @period
      AND status = 'Pending'
      AND summary_id IN (239277807299985408,239278284716638208,243567698410209280,243569594407583744,243569648094674944);
    IF v_check <> 5 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '考勤不是预期的5条待确认记录';
    END IF;

    INSERT INTO `db_payroll_item` (`item_id`, `com_id`, `store_id`, `team_id`, `emp_id`, `sheet_id`, `project_id`, `attribution`, `attribution_key`, `base_salary_amount`, `attendance_deduction`, `commission_amount`, `bonus_amount`, `allowance_amount`, `social_insurance_amount`, `tax_amount`, `other_deduction`, `gross_amount`, `deduction_amount`, `net_amount`, `status`, `remark`, `creator_id`, `create_date_time`, `create_date`, `create_time`, `create_year`, `create_month`, `create_day`, `create_hour`, `create_minute`, `create_second`, `create_week`, `update_date_time`, `update_date`, `update_time`, `update_year`, `update_month`, `update_day`, `update_hour`, `update_minute`, `update_second`, `update_week`) VALUES (243634478377861120,1790066191585976744,1790066191588014876,NULL,1790221396230505495,243634477786464256,NULL,'Company','COMPANY',3000.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,3000.00,0.00,3000.00,'Confirmed',NULL,1790221992067828768,20261104151626,20261104,151626,2026,11,4,15,16,26,2,20261108154857,20261108,154857,2026,11,8,15,48,57,6);
INSERT INTO `db_payroll_item` (`item_id`, `com_id`, `store_id`, `team_id`, `emp_id`, `sheet_id`, `project_id`, `attribution`, `attribution_key`, `base_salary_amount`, `attendance_deduction`, `commission_amount`, `bonus_amount`, `allowance_amount`, `social_insurance_amount`, `tax_amount`, `other_deduction`, `gross_amount`, `deduction_amount`, `net_amount`, `status`, `remark`, `creator_id`, `create_date_time`, `create_date`, `create_time`, `create_year`, `create_month`, `create_day`, `create_hour`, `create_minute`, `create_second`, `create_week`, `update_date_time`, `update_date`, `update_time`, `update_year`, `update_month`, `update_day`, `update_hour`, `update_minute`, `update_second`, `update_week`) VALUES (243634478491107328,1790066191585976744,1790066191588014876,NULL,1790221790492843136,243634477786464256,NULL,'Company','COMPANY',5000.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,5000.00,0.00,5000.00,'Confirmed',NULL,1790221992067828768,20261104151626,20261104,151626,2026,11,4,15,16,26,2,20261108154857,20261108,154857,2026,11,8,15,48,57,6);
INSERT INTO `db_payroll_item` (`item_id`, `com_id`, `store_id`, `team_id`, `emp_id`, `sheet_id`, `project_id`, `attribution`, `attribution_key`, `base_salary_amount`, `attendance_deduction`, `commission_amount`, `bonus_amount`, `allowance_amount`, `social_insurance_amount`, `tax_amount`, `other_deduction`, `gross_amount`, `deduction_amount`, `net_amount`, `status`, `remark`, `creator_id`, `create_date_time`, `create_date`, `create_time`, `create_year`, `create_month`, `create_day`, `create_hour`, `create_minute`, `create_second`, `create_week`, `update_date_time`, `update_date`, `update_time`, `update_year`, `update_month`, `update_day`, `update_hour`, `update_minute`, `update_second`, `update_week`) VALUES (243634478642102272,1790066191585976744,1790066191588014876,NULL,1790221992067828768,243634477786464256,NULL,'Company','COMPANY',5000.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,5000.00,0.00,5000.00,'Confirmed',NULL,1790221992067828768,20261104151626,20261104,151626,2026,11,4,15,16,26,2,20261108154857,20261108,154857,2026,11,8,15,48,57,6);
INSERT INTO `db_payroll_item` (`item_id`, `com_id`, `store_id`, `team_id`, `emp_id`, `sheet_id`, `project_id`, `attribution`, `attribution_key`, `base_salary_amount`, `attendance_deduction`, `commission_amount`, `bonus_amount`, `allowance_amount`, `social_insurance_amount`, `tax_amount`, `other_deduction`, `gross_amount`, `deduction_amount`, `net_amount`, `status`, `remark`, `creator_id`, `create_date_time`, `create_date`, `create_time`, `create_year`, `create_month`, `create_day`, `create_hour`, `create_minute`, `create_second`, `create_week`, `update_date_time`, `update_date`, `update_time`, `update_year`, `update_month`, `update_day`, `update_hour`, `update_minute`, `update_second`, `update_week`) VALUES (243634478839234560,1790066191585976744,1790066191588014876,NULL,1790222057930837664,243634477786464256,NULL,'Company','COMPANY',5000.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,5000.00,0.00,5000.00,'Confirmed',NULL,1790221992067828768,20261104151626,20261104,151626,2026,11,4,15,16,26,2,20261108154857,20261108,154857,2026,11,8,15,48,57,6);
INSERT INTO `db_payroll_item` (`item_id`, `com_id`, `store_id`, `team_id`, `emp_id`, `sheet_id`, `project_id`, `attribution`, `attribution_key`, `base_salary_amount`, `attendance_deduction`, `commission_amount`, `bonus_amount`, `allowance_amount`, `social_insurance_amount`, `tax_amount`, `other_deduction`, `gross_amount`, `deduction_amount`, `net_amount`, `status`, `remark`, `creator_id`, `create_date_time`, `create_date`, `create_time`, `create_year`, `create_month`, `create_day`, `create_hour`, `create_minute`, `create_second`, `create_week`, `update_date_time`, `update_date`, `update_time`, `update_year`, `update_month`, `update_day`, `update_hour`, `update_minute`, `update_second`, `update_week`) VALUES (243634478965063680,1790066191585976744,1790066191588014876,NULL,1790230148482841967,243634477786464256,NULL,'Company','COMPANY',3000.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,3000.00,0.00,3000.00,'Confirmed',NULL,1790221992067828768,20261104151626,20261104,151626,2026,11,4,15,16,26,2,20261108154857,20261108,154857,2026,11,8,15,48,57,6);
INSERT INTO `db_finance_payable` (`payable_id`, `create_date`, `create_date_time`, `create_day`, `create_hour`, `create_minute`, `create_month`, `create_second`, `create_time`, `create_week`, `create_year`, `update_date`, `update_date_time`, `update_day`, `update_hour`, `update_minute`, `update_month`, `update_second`, `update_time`, `update_week`, `update_year`, `amount`, `due_date`, `name`, `paid_amount`, `payee_name`, `period`, `receipt_date`, `remark`, `settlement_mode`, `source_no`, `source_type`, `source_id`, `status`, `type`, `urgent`, `com_id`, `business_origin`, `creator_id`, `material_plan_id`, `owner_id`, `payee_emp_id`, `payee_partner_id`, `project_id`, `purchase_order_id`, `property_id`, `store_id`, `team_id`, `estimated`, `settled_amount`) VALUES (245092214223405056,20261108,20261108154857,8,15,48,11,57,154857,6,2026,20261108,20261108154857,8,15,48,11,57,154857,6,2026,5000.00,NULL,'2026-10 员工实发工资',0.00,'吴濛濛','2026-10',NULL,'工资项实发金额',NULL,NULL,'PayrollItem',243634478642102272,'Pending','EmployeeSalary',_binary '\0',1790066191585976744,'New',1790066191593026092,NULL,1790221992067828768,1790221992067828768,NULL,NULL,NULL,NULL,1790066191588014876,NULL,_binary '\0',NULL);
INSERT INTO `db_finance_payable` (`payable_id`, `create_date`, `create_date_time`, `create_day`, `create_hour`, `create_minute`, `create_month`, `create_second`, `create_time`, `create_week`, `create_year`, `update_date`, `update_date_time`, `update_day`, `update_hour`, `update_minute`, `update_month`, `update_second`, `update_time`, `update_week`, `update_year`, `amount`, `due_date`, `name`, `paid_amount`, `payee_name`, `period`, `receipt_date`, `remark`, `settlement_mode`, `source_no`, `source_type`, `source_id`, `status`, `type`, `urgent`, `com_id`, `business_origin`, `creator_id`, `material_plan_id`, `owner_id`, `payee_emp_id`, `payee_partner_id`, `project_id`, `purchase_order_id`, `property_id`, `store_id`, `team_id`, `estimated`, `settled_amount`) VALUES (245092214454091776,20261108,20261108154857,8,15,48,11,57,154857,6,2026,20261108,20261108154857,8,15,48,11,57,154857,6,2026,5000.00,NULL,'2026-10 员工实发工资',0.00,'王宏文','2026-10',NULL,'工资项实发金额',NULL,NULL,'PayrollItem',243634478491107328,'Pending','EmployeeSalary',_binary '\0',1790066191585976744,'New',1790066191593026092,NULL,1790221790492843136,1790221790492843136,NULL,NULL,NULL,NULL,1790066191588014876,NULL,_binary '\0',NULL);
INSERT INTO `db_finance_payable` (`payable_id`, `create_date`, `create_date_time`, `create_day`, `create_hour`, `create_minute`, `create_month`, `create_second`, `create_time`, `create_week`, `create_year`, `update_date`, `update_date_time`, `update_day`, `update_hour`, `update_minute`, `update_month`, `update_second`, `update_time`, `update_week`, `update_year`, `amount`, `due_date`, `name`, `paid_amount`, `payee_name`, `period`, `receipt_date`, `remark`, `settlement_mode`, `source_no`, `source_type`, `source_id`, `status`, `type`, `urgent`, `com_id`, `business_origin`, `creator_id`, `material_plan_id`, `owner_id`, `payee_emp_id`, `payee_partner_id`, `project_id`, `purchase_order_id`, `property_id`, `store_id`, `team_id`, `estimated`, `settled_amount`) VALUES (245092214529589248,20261108,20261108154857,8,15,48,11,57,154857,6,2026,20261108,20261108154857,8,15,48,11,57,154857,6,2026,3000.00,NULL,'2026-10 员工实发工资',0.00,'董夏鹏','2026-10',NULL,'工资项实发金额',NULL,NULL,'PayrollItem',243634478377861120,'Pending','EmployeeSalary',_binary '\0',1790066191585976744,'New',1790066191593026092,NULL,1790221396230505495,1790221396230505495,NULL,NULL,NULL,NULL,1790066191588014876,NULL,_binary '\0',NULL);
INSERT INTO `db_finance_payable` (`payable_id`, `create_date`, `create_date_time`, `create_day`, `create_hour`, `create_minute`, `create_month`, `create_second`, `create_time`, `create_week`, `create_year`, `update_date`, `update_date_time`, `update_day`, `update_hour`, `update_minute`, `update_month`, `update_second`, `update_time`, `update_week`, `update_year`, `amount`, `due_date`, `name`, `paid_amount`, `payee_name`, `period`, `receipt_date`, `remark`, `settlement_mode`, `source_no`, `source_type`, `source_id`, `status`, `type`, `urgent`, `com_id`, `business_origin`, `creator_id`, `material_plan_id`, `owner_id`, `payee_emp_id`, `payee_partner_id`, `project_id`, `purchase_order_id`, `property_id`, `store_id`, `team_id`, `estimated`, `settled_amount`) VALUES (245092214571532288,20261108,20261108154857,8,15,48,11,57,154857,6,2026,20261108,20261108154857,8,15,48,11,57,154857,6,2026,3000.00,NULL,'2026-10 员工实发工资',0.00,'谢玲玲','2026-10',NULL,'工资项实发金额',NULL,NULL,'PayrollItem',243634478965063680,'Pending','EmployeeSalary',_binary '\0',1790066191585976744,'New',1790066191593026092,NULL,1790230148482841967,1790230148482841967,NULL,NULL,NULL,NULL,1790066191588014876,NULL,_binary '\0',NULL);
INSERT INTO `db_finance_payable` (`payable_id`, `create_date`, `create_date_time`, `create_day`, `create_hour`, `create_minute`, `create_month`, `create_second`, `create_time`, `create_week`, `create_year`, `update_date`, `update_date_time`, `update_day`, `update_hour`, `update_minute`, `update_month`, `update_second`, `update_time`, `update_week`, `update_year`, `amount`, `due_date`, `name`, `paid_amount`, `payee_name`, `period`, `receipt_date`, `remark`, `settlement_mode`, `source_no`, `source_type`, `source_id`, `status`, `type`, `urgent`, `com_id`, `business_origin`, `creator_id`, `material_plan_id`, `owner_id`, `payee_emp_id`, `payee_partner_id`, `project_id`, `purchase_order_id`, `property_id`, `store_id`, `team_id`, `estimated`, `settled_amount`) VALUES (245092214605086720,20261108,20261108154857,8,15,48,11,57,154857,6,2026,20261108,20261108154857,8,15,48,11,57,154857,6,2026,5000.00,NULL,'2026-10 员工实发工资',0.00,'黄贤杰','2026-10',NULL,'工资项实发金额',NULL,NULL,'PayrollItem',243634478839234560,'Pending','EmployeeSalary',_binary '\0',1790066191585976744,'New',1790066191593026092,NULL,1790222057930837664,1790222057930837664,NULL,NULL,NULL,NULL,1790066191588014876,NULL,_binary '\0',NULL);
INSERT INTO `db_payroll_payslip` (`payslip_id`, `com_id`, `store_id`, `team_id`, `emp_id`, `sheet_id`, `period`, `gross_amount`, `deduction_amount`, `net_amount`, `status`, `published_at`, `creator_id`, `create_date_time`, `create_date`, `create_time`, `create_year`, `create_month`, `create_day`, `create_hour`, `create_minute`, `create_second`, `create_week`, `update_date_time`, `update_date`, `update_time`, `update_year`, `update_month`, `update_day`, `update_hour`, `update_minute`, `update_second`, `update_week`) VALUES (245092214651224064,1790066191585976744,1790066191588014876,NULL,1790221992067828768,243634477786464256,'2026-10',5000.00,0.00,5000.00,'PendingPayment',NULL,1790066191593026092,20261108154857,20261108,154857,2026,11,8,15,48,57,6,20261108154857,20261108,154857,2026,11,8,15,48,57,6);
INSERT INTO `db_payroll_payslip` (`payslip_id`, `com_id`, `store_id`, `team_id`, `emp_id`, `sheet_id`, `period`, `gross_amount`, `deduction_amount`, `net_amount`, `status`, `published_at`, `creator_id`, `create_date_time`, `create_date`, `create_time`, `create_year`, `create_month`, `create_day`, `create_hour`, `create_minute`, `create_second`, `create_week`, `update_date_time`, `update_date`, `update_time`, `update_year`, `update_month`, `update_day`, `update_hour`, `update_minute`, `update_second`, `update_week`) VALUES (245092214722527232,1790066191585976744,1790066191588014876,NULL,1790221790492843136,243634477786464256,'2026-10',5000.00,0.00,5000.00,'PendingPayment',NULL,1790066191593026092,20261108154857,20261108,154857,2026,11,8,15,48,57,6,20261108154857,20261108,154857,2026,11,8,15,48,57,6);
INSERT INTO `db_payroll_payslip` (`payslip_id`, `com_id`, `store_id`, `team_id`, `emp_id`, `sheet_id`, `period`, `gross_amount`, `deduction_amount`, `net_amount`, `status`, `published_at`, `creator_id`, `create_date_time`, `create_date`, `create_time`, `create_year`, `create_month`, `create_day`, `create_hour`, `create_minute`, `create_second`, `create_week`, `update_date_time`, `update_date`, `update_time`, `update_year`, `update_month`, `update_day`, `update_hour`, `update_minute`, `update_second`, `update_week`) VALUES (245092214751887360,1790066191585976744,1790066191588014876,NULL,1790221396230505495,243634477786464256,'2026-10',3000.00,0.00,3000.00,'PendingPayment',NULL,1790066191593026092,20261108154857,20261108,154857,2026,11,8,15,48,57,6,20261108154857,20261108,154857,2026,11,8,15,48,57,6);
INSERT INTO `db_payroll_payslip` (`payslip_id`, `com_id`, `store_id`, `team_id`, `emp_id`, `sheet_id`, `period`, `gross_amount`, `deduction_amount`, `net_amount`, `status`, `published_at`, `creator_id`, `create_date_time`, `create_date`, `create_time`, `create_year`, `create_month`, `create_day`, `create_hour`, `create_minute`, `create_second`, `create_week`, `update_date_time`, `update_date`, `update_time`, `update_year`, `update_month`, `update_day`, `update_hour`, `update_minute`, `update_second`, `update_week`) VALUES (245092214785441792,1790066191585976744,1790066191588014876,NULL,1790230148482841967,243634477786464256,'2026-10',3000.00,0.00,3000.00,'PendingPayment',NULL,1790066191593026092,20261108154857,20261108,154857,2026,11,8,15,48,57,6,20261108154857,20261108,154857,2026,11,8,15,48,57,6);
INSERT INTO `db_payroll_payslip` (`payslip_id`, `com_id`, `store_id`, `team_id`, `emp_id`, `sheet_id`, `period`, `gross_amount`, `deduction_amount`, `net_amount`, `status`, `published_at`, `creator_id`, `create_date_time`, `create_date`, `create_time`, `create_year`, `create_month`, `create_day`, `create_hour`, `create_minute`, `create_second`, `create_week`, `update_date_time`, `update_date`, `update_time`, `update_year`, `update_month`, `update_day`, `update_hour`, `update_minute`, `update_second`, `update_week`) VALUES (245092214810607616,1790066191585976744,1790066191588014876,NULL,1790222057930837664,243634477786464256,'2026-10',5000.00,0.00,5000.00,'PendingPayment',NULL,1790066191593026092,20261108154857,20261108,154857,2026,11,8,15,48,57,6,20261108154857,20261108,154857,2026,11,8,15,48,57,6);

    SELECT COUNT(*) INTO v_check
    FROM db_payroll_item
    WHERE com_id = @com_id AND sheet_id = @sheet_id AND status = 'Confirmed';
    IF v_check <> 5 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '恢复工资项失败';
    END IF;

    SELECT COUNT(*) INTO v_check
    FROM db_finance_payable
    WHERE com_id = @com_id
      AND source_type = 'PayrollItem'
      AND source_id IN (243634478642102272,243634478491107328,243634478377861120,243634478965063680,243634478839234560)
      AND status = 'Pending'
      AND paid_amount = 0;
    IF v_check <> 5 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '恢复工资应付失败';
    END IF;

    SELECT COUNT(*) INTO v_check
    FROM db_payroll_payslip
    WHERE com_id = @com_id AND sheet_id = @sheet_id AND status = 'PendingPayment' AND published_at IS NULL;
    IF v_check <> 5 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '恢复工资条失败';
    END IF;

    UPDATE db_payroll_attendance_summary
    SET status = 'InPayroll', confirmed_by = 1790221992067828768, confirmed_at = '2026-11-04 15:16:20.414920', remark = '',
        update_date_time = 20261104151626, update_date = 20261104, update_time = 151626,
        update_year = 2026, update_month = 11, update_day = 4, update_hour = 15, update_minute = 16, update_second = 26, update_week = 2
    WHERE summary_id = 239277807299985408 AND status = 'Pending';

    UPDATE db_payroll_attendance_summary
    SET status = 'InPayroll', confirmed_by = 1790221992067828768, confirmed_at = '2026-11-04 15:16:21.199605', remark = '',
        update_date_time = 20261104151626, update_date = 20261104, update_time = 151626,
        update_year = 2026, update_month = 11, update_day = 4, update_hour = 15, update_minute = 16, update_second = 26, update_week = 2
    WHERE summary_id = 239278284716638208 AND status = 'Pending';

    UPDATE db_payroll_attendance_summary
    SET status = 'InPayroll', confirmed_by = 1790221992067828768, confirmed_at = '2026-11-04 14:11:50.531408', remark = NULL,
        update_date_time = 20261104151626, update_date = 20261104, update_time = 151626,
        update_year = 2026, update_month = 11, update_day = 4, update_hour = 15, update_minute = 16, update_second = 26, update_week = 2
    WHERE summary_id = 243567698410209280 AND status = 'Pending';

    UPDATE db_payroll_attendance_summary
    SET status = 'InPayroll', confirmed_by = 1790221992067828768, confirmed_at = '2026-11-04 15:16:18.790729', remark = NULL,
        update_date_time = 20261104151626, update_date = 20261104, update_time = 151626,
        update_year = 2026, update_month = 11, update_day = 4, update_hour = 15, update_minute = 16, update_second = 26, update_week = 2
    WHERE summary_id = 243569594407583744 AND status = 'Pending';

    UPDATE db_payroll_attendance_summary
    SET status = 'InPayroll', confirmed_by = 1790221992067828768, confirmed_at = '2026-11-04 14:11:48.361138', remark = NULL,
        update_date_time = 20261104151626, update_date = 20261104, update_time = 151626,
        update_year = 2026, update_month = 11, update_day = 4, update_hour = 15, update_minute = 16, update_second = 26, update_week = 2
    WHERE summary_id = 243569648094674944 AND status = 'Pending';

    SELECT COUNT(*) INTO v_check
    FROM db_payroll_attendance_summary
    WHERE com_id = @com_id
      AND payroll_month = @period
      AND status = 'InPayroll'
      AND summary_id IN (239277807299985408,239278284716638208,243567698410209280,243569594407583744,243569648094674944);
    IF v_check <> 5 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '恢复考勤锁定状态失败';
    END IF;
    SET @updated_attendances := v_check;

    UPDATE db_payroll_sheet
    SET status = 'Confirmed', attendance_complete = 1, commission_complete = 0,
        total_gross_amount = 21000.00, total_deduction_amount = 0.00, total_net_amount = 21000.00,
        confirmed_by = @confirmed_by, confirmed_at = @confirmed_at, remark = NULL,
        update_date_time = 20261108154857, update_date = 20261108, update_time = 154857,
        update_year = 2026, update_month = 11, update_day = 8, update_hour = 15, update_minute = 48, update_second = 57, update_week = 6
    WHERE sheet_id = @sheet_id AND status = 'Draft';
    SET v_check = ROW_COUNT();
    IF v_check <> 1 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '恢复工资单头失败';
    END IF;

    SELECT 'CHG_20261108_002_RESTORED' AS result, @updated_attendances AS attendances_updated;

    COMMIT;
END//
DELIMITER ;

SELECT GET_LOCK('decoboon:chg:20261108:002', 10) AS lock_acquired;
CALL tmp_chg_20261108_002_restore_payroll();
DROP PROCEDURE IF EXISTS tmp_chg_20261108_002_restore_payroll;
SELECT RELEASE_LOCK('decoboon:chg:20261108:002') AS lock_released;
