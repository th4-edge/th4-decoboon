-- 129 企业费用 D2：费用审批到应付关联
-- 口径：DEC-091 / BR-057 / T12-BIZ-20261022-012 / EXPENSE-D2
-- 边界：只增加费用到应付的关系；应付、付款、核销、流水与关闭继续复用既有财务主干。

ALTER TABLE db_finance_expense
    ADD COLUMN payable_id BIGINT NULL COMMENT '审批通过后形成的费用应付' AFTER category_id;

CREATE INDEX idx_fin_expense_company_payable
    ON db_finance_expense (com_id, payable_id);

ALTER TABLE db_finance_expense
    ADD CONSTRAINT fk_fin_expense_payable
        FOREIGN KEY (payable_id) REFERENCES db_finance_payable (payable_id);
