-- RUN-20261104-FIN-P0-001 local-fixture setup
-- Adds a store-scoped Finance account in the non-default 滨湖店.
INSERT INTO db_tenant_employee
(emp_id,name,role,status,tel_no,com_id,store_id)
SELECT 199590000000000201,'滨湖审计财务','Finance','Active','13910002010',199500000000000001,199500000000000012
WHERE NOT EXISTS (SELECT 1 FROM db_tenant_employee WHERE emp_id=199590000000000201);
INSERT INTO db_tenant_credentials
(cre_id,id_key,id_secret,password_hash,password_updated_at,phone,status,type,emp_id,failed_attempts,must_change_password)
SELECT 199590000000000301,'fixture-payroll-store-finance-20261104',c.id_secret,c.password_hash,c.password_updated_at,'13910002010','Active','Password',199590000000000201,0,b'0'
FROM db_tenant_credentials c
WHERE c.emp_id=199500000000000104
  AND NOT EXISTS (SELECT 1 FROM db_tenant_credentials WHERE cre_id=199590000000000301);
