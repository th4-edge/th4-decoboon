-- 126 企业费用与员工报销 D1 回滚
-- 前置：停止费用 Provider 和 App/Web 写入；确认已导出四表业务数据与附件路径。
-- 本脚本只回滚 D1 新表，不修改历史手工流水、Payable、Payment、Ledger 或 Voucher。

DROP TABLE IF EXISTS db_finance_expense_revision;
DROP TABLE IF EXISTS db_finance_expense_attachment;
DROP TABLE IF EXISTS db_finance_expense;
DROP TABLE IF EXISTS db_finance_expense_category;
