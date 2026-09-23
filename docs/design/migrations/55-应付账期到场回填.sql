-- 55-应付账期到场回填.sql
-- 依据 DEC-MP-01 / BR-003：下单即形成应付，但账期起算日与到期日应在材料到场确认后按供应商账期回填。
-- 适用：MySQL 8.0+
-- 修复：旧基线把 due_date 定义为 NOT NULL，导致项目采购“确认下单”创建应付时因 dueDate=NULL 失败。

ALTER TABLE db_finance_payable
  MODIFY COLUMN due_date DATE NULL COMMENT '约定付款日（到场确认后按供应商账期回填）';

-- 回滚（仅在确认不存在 due_date IS NULL 的数据后执行）：
-- ALTER TABLE db_finance_payable MODIFY COLUMN due_date DATE NOT NULL;
