INSERT INTO db_tenant_company (com_id, title, name, size, enable_store)
VALUES (8900000000000000001, '单BOSS验收企业', 'SINGLEBOSS', 'A', b'0');

INSERT INTO db_tenant_store (store_id, company_com_id, name, default_store)
VALUES (8900000000000000102, 8900000000000000001, '总店', b'1');

INSERT INTO db_tenant_employee (emp_id, name, tel_no, role, status, com_id, store_id, joined_at)
VALUES (8900000000000000101, '单人负责人', '18890000001', 'Boss', 'Active', 8900000000000000001, 8900000000000000102, NOW(6));

INSERT INTO db_tenant_credentials (cre_id, type, id_key, phone, status, emp_id, password_hash, password_updated_at, failed_attempts, must_change_password)
VALUES (8900000000000000201, 'Password', '18890000001', '18890000001', 'Active', 8900000000000000101, '__PASSWORD_HASH__', NOW(6), 0, b'0');

-- 提成链最小主合同项目夹具；仅供本地单 BOSS 验收。
INSERT INTO cust_customer (cust_id, name, tel_no, status, visit_count, wechat_added, com_id, emp_id, store_id)
VALUES (8900000000000000301, '单BOSS验收客户', '18890000002', 'Quoted', 0, b'0', 8900000000000000001, 8900000000000000101, 8900000000000000102);

INSERT INTO db_property (property_id, address, property_no, status, com_id, customer_id, emp_id, store_id)
VALUES (8900000000000000302, '单BOSS验收地址1号', 'SB-PROP-001', 'Quoted', 8900000000000000001, 8900000000000000301, 8900000000000000101, 8900000000000000102);

INSERT INTO quote (quote_id, amount, area, cost, cost_complete, design_fee, discount, management_fee, mode, no, status, subtotal, tax_amount, tax_rate, title, version, com_id, business_origin, emp_id, customer_id, property_id, store_id)
VALUES (8900000000000000303, 100000.00, 100.00, 0.00, b'0', 0.00, 1.0000, 0.00, 'Custom', 'SB-Q-001', 'Accepted', 100000.00, 0.00, 0.0500, '单BOSS验收主合同报价', 1, 8900000000000000001, 'New', 8900000000000000101, 8900000000000000301, 8900000000000000302, 8900000000000000102);

INSERT INTO db_project (project_id, area, contract_amount, cost, name, no, progress, site_addr, status, com_id, business_origin, customer_id, designer_id, manager_id, owner_id, property_id, quote_id, store_id, support_id)
VALUES (8900000000000000304, 100.00, 100000.00, 0.00, '单BOSS验收主合同项目', 'SB-P-001', 0, '单BOSS验收地址1号', 'Preparing', 8900000000000000001, 'New', 8900000000000000301, 8900000000000000101, 8900000000000000101, 8900000000000000101, 8900000000000000302, 8900000000000000303, 8900000000000000102, 8900000000000000101);
