SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM db_payroll_commission_release WHERE com_id = 8900000000000000001;
DELETE FROM db_payroll_commission_adjustment WHERE com_id = 8900000000000000001;
DELETE FROM db_payroll_commission_item WHERE com_id = 8900000000000000001;
DELETE FROM db_payroll_payslip WHERE com_id = 8900000000000000001;
DELETE FROM db_payroll_item WHERE com_id = 8900000000000000001;
DELETE FROM db_payroll_sheet WHERE com_id = 8900000000000000001;
DELETE FROM db_payroll_attendance_summary WHERE com_id = 8900000000000000001;
DELETE FROM db_payroll_compensation_scheme WHERE com_id = 8900000000000000001;
DELETE FROM db_finance_payment WHERE com_id = 8900000000000000001;
DELETE FROM db_finance_payable_settlement WHERE payable_id IN (SELECT payable_id FROM db_finance_payable WHERE com_id = 8900000000000000001);
DELETE FROM db_finance_payable WHERE com_id = 8900000000000000001;
DELETE FROM db_finance_ledger WHERE com_id = 8900000000000000001;
DELETE FROM db_finance_receipt WHERE com_id = 8900000000000000001;
DELETE FROM db_finance_receivable WHERE com_id = 8900000000000000001;
DELETE FROM db_project WHERE com_id = 8900000000000000001;
DELETE FROM quote WHERE com_id = 8900000000000000001;
DELETE FROM db_property WHERE com_id = 8900000000000000001;
DELETE FROM cust_customer WHERE com_id = 8900000000000000001;
DELETE FROM db_tenant_credentials WHERE emp_id = 8900000000000000101;
DELETE FROM db_tenant_employee WHERE com_id = 8900000000000000001;
DELETE FROM db_tenant_store WHERE company_com_id = 8900000000000000001;
DELETE FROM db_tenant_company WHERE com_id = 8900000000000000001;

SET FOREIGN_KEY_CHECKS = 1;
