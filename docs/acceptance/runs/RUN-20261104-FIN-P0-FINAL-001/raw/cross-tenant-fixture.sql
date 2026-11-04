-- Isolated second-tenant fixture, local only.
INSERT INTO db_tenant_company (com_id,name,title,size,enable_store,custodian_credit)
SELECT 199600000000000001,'隔离测试企业','隔离测试企业 · 财务门禁','A',b'1',b'1'
WHERE NOT EXISTS (SELECT 1 FROM db_tenant_company WHERE com_id=199600000000000001);
INSERT INTO db_tenant_store (store_id,name,addr,status,default_store,sort,company_com_id)
SELECT 199600000000000011,'隔离总店','合肥市隔离测试地址','Active',b'1',0,199600000000000001
WHERE NOT EXISTS (SELECT 1 FROM db_tenant_store WHERE store_id=199600000000000011);
INSERT INTO db_tenant_employee (emp_id,name,role,status,tel_no,com_id,store_id)
SELECT 199600000000000104,'隔离财务','Finance','Active','13910002011',199600000000000001,199600000000000011
WHERE NOT EXISTS (SELECT 1 FROM db_tenant_employee WHERE emp_id=199600000000000104);
INSERT INTO db_tenant_credentials (cre_id,id_key,id_secret,password_hash,password_updated_at,phone,status,type,emp_id,failed_attempts,must_change_password)
SELECT 199600000000000301,'fixture-cross-tenant-finance-20261104',c.id_secret,c.password_hash,c.password_updated_at,'13910002011','Active','Password',199600000000000104,0,b'0'
FROM db_tenant_credentials c
WHERE c.emp_id=199500000000000104
  AND NOT EXISTS (SELECT 1 FROM db_tenant_credentials WHERE cre_id=199600000000000301);
