-- 2026-10-17 多门店测试数据.sql
-- 目的：在本地 dev/test 构建独立多门店测试企业，验证店长本店数据范围、目标/利润、合同变更审批、承接确认和主数据权限。
-- 企业：多店测试装企；门店：总店、滨湖店、高新店；账号密码统一为 123456。
-- 回滚：docs/operations/data-changes/2026-10-17-多门店测试数据-rollback.sql
-- 重跑：先执行回滚脚本，再执行本脚本；正向承接测试会生成新项目/报价/客户，不能只重跑本脚本。

set @pwd_hash = (select password_hash from db_tenant_credentials where phone = '19900000003' limit 1);

insert into db_tenant_company
(com_id, name, title, size, enable_store, addr, service_tel, intro, create_date, create_date_time, create_year, create_month, create_day)
values
(2790000000000000001, '多店测试装企', '多店测试装企', 'B', b'1', '合肥市测试路1号', '19900001001', '仅用于多门店权限与经营数据测试', 20261017, 20261017160000, 2026, 10, 17)
on duplicate key update name=values(name), title=values(title), size=values(size), enable_store=values(enable_store), addr=values(addr), service_tel=values(service_tel), intro=values(intro);

insert into db_tenant_store
(store_id, company_com_id, name, default_store, status, sort, addr, create_date, create_date_time, create_year, create_month, create_day)
values
(2790000000000000101, 2790000000000000001, '总店', b'1', 'Active', 0, '合肥市测试路1号', 20261017, 20261017160000, 2026, 10, 17),
(2790000000000000102, 2790000000000000001, '滨湖店', b'0', 'Active', 10, '合肥市滨湖测试路2号', 20261017, 20261017160000, 2026, 10, 17),
(2790000000000000103, 2790000000000000001, '高新店', b'0', 'Active', 20, '合肥市高新测试路3号', 20261017, 20261017160000, 2026, 10, 17)
on duplicate key update name=values(name), default_store=values(default_store), status=values(status), sort=values(sort), addr=values(addr);

insert into db_tenant_employee
(emp_id, com_id, store_id, name, tel_no, role, status, joined_at, create_date, create_date_time, create_year, create_month, create_day)
values
(2790000000000000201, 2790000000000000001, 2790000000000000101, '多店测试老板', '19900001001', 'Boss', 'Active', now(6), 20261017, 20261017160000, 2026, 10, 17),
(2790000000000000202, 2790000000000000001, 2790000000000000102, '滨湖店长', '19900001002', 'StoreManager', 'Active', now(6), 20261017, 20261017160000, 2026, 10, 17),
(2790000000000000203, 2790000000000000001, 2790000000000000103, '高新店长', '19900001003', 'StoreManager', 'Active', now(6), 20261017, 20261017160000, 2026, 10, 17),
(2790000000000000204, 2790000000000000001, 2790000000000000102, '滨湖财务', '19900001004', 'Finance', 'Active', now(6), 20261017, 20261017160000, 2026, 10, 17),
(2790000000000000205, 2790000000000000001, 2790000000000000103, '高新财务', '19900001005', 'Finance', 'Active', now(6), 20261017, 20261017160000, 2026, 10, 17),
(2790000000000000206, 2790000000000000001, 2790000000000000102, '滨湖设计师', '19900001006', 'Designer', 'Active', now(6), 20261017, 20261017160000, 2026, 10, 17),
(2790000000000000207, 2790000000000000001, 2790000000000000103, '高新设计师', '19900001007', 'Designer', 'Active', now(6), 20261017, 20261017160000, 2026, 10, 17),
(2790000000000000208, 2790000000000000001, 2790000000000000102, '滨湖业务员', '19900001008', 'Marketer', 'Active', now(6), 20261017, 20261017160000, 2026, 10, 17),
(2790000000000000209, 2790000000000000001, 2790000000000000103, '高新业务员', '19900001009', 'Marketer', 'Active', now(6), 20261017, 20261017160000, 2026, 10, 17),
(2790000000000000210, 2790000000000000001, 2790000000000000102, '滨湖项目经理', '19900001010', 'Pm', 'Active', now(6), 20261017, 20261017160000, 2026, 10, 17),
(2790000000000000211, 2790000000000000001, 2790000000000000103, '高新项目经理', '19900001011', 'Pm', 'Active', now(6), 20261017, 20261017160000, 2026, 10, 17)
on duplicate key update store_id=values(store_id), name=values(name), tel_no=values(tel_no), role=values(role), status=values(status);

insert into db_tenant_credentials
(cre_id, id_key, phone, type, status, password_hash, password_updated_at, emp_id, create_date, create_date_time, create_year, create_month, create_day)
values
(2790000000000000301, 'dev-multistore-19900001001', '19900001001', 'Password', 'Active', @pwd_hash, now(6), 2790000000000000201, 20261017, 20261017160000, 2026, 10, 17),
(2790000000000000302, 'dev-multistore-19900001002', '19900001002', 'Password', 'Active', @pwd_hash, now(6), 2790000000000000202, 20261017, 20261017160000, 2026, 10, 17),
(2790000000000000303, 'dev-multistore-19900001003', '19900001003', 'Password', 'Active', @pwd_hash, now(6), 2790000000000000203, 20261017, 20261017160000, 2026, 10, 17),
(2790000000000000304, 'dev-multistore-19900001004', '19900001004', 'Password', 'Active', @pwd_hash, now(6), 2790000000000000204, 20261017, 20261017160000, 2026, 10, 17),
(2790000000000000305, 'dev-multistore-19900001005', '19900001005', 'Password', 'Active', @pwd_hash, now(6), 2790000000000000205, 20261017, 20261017160000, 2026, 10, 17),
(2790000000000000306, 'dev-multistore-19900001006', '19900001006', 'Password', 'Active', @pwd_hash, now(6), 2790000000000000206, 20261017, 20261017160000, 2026, 10, 17),
(2790000000000000307, 'dev-multistore-19900001007', '19900001007', 'Password', 'Active', @pwd_hash, now(6), 2790000000000000207, 20261017, 20261017160000, 2026, 10, 17),
(2790000000000000308, 'dev-multistore-19900001008', '19900001008', 'Password', 'Active', @pwd_hash, now(6), 2790000000000000208, 20261017, 20261017160000, 2026, 10, 17),
(2790000000000000309, 'dev-multistore-19900001009', '19900001009', 'Password', 'Active', @pwd_hash, now(6), 2790000000000000209, 20261017, 20261017160000, 2026, 10, 17),
(2790000000000000310, 'dev-multistore-19900001010', '19900001010', 'Password', 'Active', @pwd_hash, now(6), 2790000000000000210, 20261017, 20261017160000, 2026, 10, 17),
(2790000000000000311, 'dev-multistore-19900001011', '19900001011', 'Password', 'Active', @pwd_hash, now(6), 2790000000000000211, 20261017, 20261017160000, 2026, 10, 17)
on duplicate key update phone=values(phone), emp_id=values(emp_id), password_hash=values(password_hash), status='Active';

insert into db_tenant_subscription
(sub_id, com_id, plan_id, plan_version_id, status, trial_activated, extra_seats, extra_stores,
 service_end_date, service_end_date_time, service_end_year, service_end_month, service_end_day,
 service_end_hour, service_end_minute, service_end_second, service_end_time, service_end_week,
 create_date, create_date_time, create_year, create_month, create_day)
values
(2790000000000000401, 2790000000000000001, 1790208367910210092, 1790208367913526699, 'Active', b'1', 0, 0,
 20271231, 20271231235959, 2027, 12, 31, 23, 59, 59, 235959, 5,
 20261017, 20261017160000, 2026, 10, 17)
on duplicate key update plan_id=values(plan_id), plan_version_id=values(plan_version_id), status='Active', trial_activated=b'1',
 service_end_date=values(service_end_date), service_end_date_time=values(service_end_date_time),
 service_end_year=values(service_end_year), service_end_month=values(service_end_month), service_end_day=values(service_end_day),
 service_end_hour=values(service_end_hour), service_end_minute=values(service_end_minute), service_end_second=values(service_end_second),
 service_end_time=values(service_end_time), service_end_week=values(service_end_week);

-- 客户/房屋
insert into cust_customer
(cust_id, com_id, store_id, name, tel_no, addr, community, status, emp_id, designer_id, source, wechat_added, visit_count, create_date, create_date_time, create_year, create_month, create_day)
values
(2790000000000000501, 2790000000000000001, 2790000000000000102, '滨湖测试客户', '13900001101', '滨湖测试路2号1栋101', '滨湖测试小区', 'Serving', 2790000000000000208, 2790000000000000206, 'Other', b'1', 1, 20261017, 20261017160000, 2026, 10, 17),
(2790000000000000502, 2790000000000000001, 2790000000000000103, '高新测试客户', '13900001102', '高新测试路3号2栋202', '高新测试小区', 'Serving', 2790000000000000209, 2790000000000000207, 'Other', b'1', 1, 20261017, 20261017160000, 2026, 10, 17)
on duplicate key update store_id=values(store_id), name=values(name), tel_no=values(tel_no), addr=values(addr), community=values(community), status=values(status), emp_id=values(emp_id), designer_id=values(designer_id);

insert into db_property
(property_id, com_id, store_id, customer_id, emp_id, designer_id, property_no, address, community, building, room_no, area, layout, status, create_date, create_date_time, create_year, create_month, create_day)
values
(2790000000000000601, 2790000000000000001, 2790000000000000102, 2790000000000000501, 2790000000000000208, 2790000000000000206, 'MT-A-101', '滨湖测试路2号1栋101', '滨湖测试小区', '1栋', '101', 100.00, '3室2厅', 'Serving', 20261017, 20261017160000, 2026, 10, 17),
(2790000000000000602, 2790000000000000001, 2790000000000000103, 2790000000000000502, 2790000000000000209, 2790000000000000207, 'MT-B-202', '高新测试路3号2栋202', '高新测试小区', '2栋', '202', 120.00, '3室2厅', 'Serving', 20261017, 20261017160000, 2026, 10, 17)
on duplicate key update store_id=values(store_id), customer_id=values(customer_id), emp_id=values(emp_id), designer_id=values(designer_id), address=values(address), community=values(community), area=values(area), status=values(status);

-- 报价/项目/合同
insert into quote
(quote_id, com_id, store_id, customer_id, property_id, emp_id, no, title, mode, status, version, amount, area, cost, cost_complete, design_fee, discount, management_fee, subtotal, tax_amount, tax_rate, business_origin, create_date, create_date_time, create_year, create_month, create_day)
values
(2790000000000000701, 2790000000000000001, 2790000000000000102, 2790000000000000501, 2790000000000000601, 2790000000000000206, 'MT-Q-A-001', '滨湖测试报价', 'Custom', 'Converted', 1, 100000.00, 100.00, 60000.00, b'1', 0.00, 0.0000, 0.00, 100000.00, 0.00, 0.0000, 'New', 20261017, 20261017160000, 2026, 10, 17),
(2790000000000000702, 2790000000000000001, 2790000000000000103, 2790000000000000502, 2790000000000000602, 2790000000000000207, 'MT-Q-B-001', '高新测试报价', 'Custom', 'Converted', 1, 120000.00, 120.00, 40000.00, b'1', 0.00, 0.0000, 0.00, 120000.00, 0.00, 0.0000, 'New', 20261017, 20261017160000, 2026, 10, 17)
on duplicate key update store_id=values(store_id), customer_id=values(customer_id), property_id=values(property_id), emp_id=values(emp_id), amount=values(amount), area=values(area), cost=values(cost), cost_complete=values(cost_complete), status=values(status);

-- 报价行：让合成项目的成本/金额可计算（不是完整报价明细，仅用于门店利润口径测试）
insert into quote_line
(item_id, com_id, store_id, quote_id, property_id, emp_id, name, category, type, unit, role, quantity, material_price, manual_price, price, amount, cost, actual_cost, actual_labor_cost, actual_material_cost, actual_other_cost, discount_amount, pricing_mode, formula, difference_status, difference_type, included, sort, create_date, create_date_time, create_year, create_month, create_day)
values
(2790000000000001401, 2790000000000000001, 2790000000000000102, 2790000000000000701, 2790000000000000601, 2790000000000000206, '滨湖测试施工项', '施工', 'Construction', 'Item', 'Base', 1, 0.00, 100000.00, 100000.00, 100000.00, 60000.00, 0.00, 0.00, 0.00, 0.00, 0.00, 'FixedTotal', '固定总价', 'None', 'None', b'1', 0, 20261017, 20261017160000, 2026, 10, 17),
(2790000000000001402, 2790000000000000001, 2790000000000000103, 2790000000000000702, 2790000000000000602, 2790000000000000207, '高新测试施工项', '施工', 'Construction', 'Item', 'Base', 1, 0.00, 120000.00, 120000.00, 120000.00, 40000.00, 0.00, 0.00, 0.00, 0.00, 0.00, 'FixedTotal', '固定总价', 'None', 'None', b'1', 0, 20261017, 20261017160000, 2026, 10, 17)
on duplicate key update store_id=values(store_id), quote_id=values(quote_id), property_id=values(property_id), emp_id=values(emp_id), amount=values(amount), cost=values(cost), price=values(price), manual_price=values(manual_price), material_price=values(material_price), pricing_mode=values(pricing_mode), included=b'1';

insert into db_project
(project_id, com_id, store_id, customer_id, property_id, quote_id, no, name, area, contract_amount, cost, status, business_origin, owner_id, designer_id, manager_id, progress, site_addr, start_date, plan_end_date, create_date, create_date_time, create_year, create_month, create_day)
values
(2790000000000000801, 2790000000000000001, 2790000000000000102, 2790000000000000501, 2790000000000000601, 2790000000000000701, 'MT-P-A-001', '滨湖测试项目', 100.00, 100000.00, 60000.00, 'Ongoing', 'New', 2790000000000000208, 2790000000000000206, 2790000000000000210, 30, '滨湖测试路2号1栋101', '2026-10-01', '2027-01-31', 20261017, 20261017160000, 2026, 10, 17),
(2790000000000000802, 2790000000000000001, 2790000000000000103, 2790000000000000502, 2790000000000000602, 2790000000000000702, 'MT-P-B-001', '高新测试项目', 120.00, 120000.00, 40000.00, 'Ongoing', 'New', 2790000000000000209, 2790000000000000207, 2790000000000000211, 30, '高新测试路3号2栋202', '2026-10-01', '2027-01-31', 20261017, 20261017160000, 2026, 10, 17)
on duplicate key update store_id=values(store_id), customer_id=values(customer_id), property_id=values(property_id), quote_id=values(quote_id), name=values(name), contract_amount=values(contract_amount), cost=values(cost), status=values(status), owner_id=values(owner_id), designer_id=values(designer_id), manager_id=values(manager_id);

insert into db_project_contract
(contract_id, com_id, store_id, customer_id, property_id, quote_id, project_id, creator_id, owner_id, no, baseline_amount, current_amount, signed_amount, status, business_origin, signed_at, start_date, plan_end_date, payment_terms, create_date, create_date_time, create_year, create_month, create_day)
values
(2790000000000000901, 2790000000000000001, 2790000000000000102, 2790000000000000501, 2790000000000000601, 2790000000000000701, 2790000000000000801, 2790000000000000206, 2790000000000000208, 'MT-C-A-001', 100000.00, 100000.00, 100000.00, 'Effective', 'New', '2026-10-01', '2026-10-01', '2027-01-31', '测试合同', 20261017, 20261017160000, 2026, 10, 17),
(2790000000000000902, 2790000000000000001, 2790000000000000103, 2790000000000000502, 2790000000000000602, 2790000000000000702, 2790000000000000802, 2790000000000000207, 2790000000000000209, 'MT-C-B-001', 120000.00, 120000.00, 120000.00, 'Effective', 'New', '2026-10-01', '2026-10-01', '2027-01-31', '测试合同', 20261017, 20261017160000, 2026, 10, 17)
on duplicate key update store_id=values(store_id), customer_id=values(customer_id), property_id=values(property_id), quote_id=values(quote_id), project_id=values(project_id), baseline_amount=values(baseline_amount), current_amount=values(current_amount), signed_amount=values(signed_amount), status=values(status), owner_id=values(owner_id);

-- 门店差异合同变更：Pending，店长可拒绝本店、不可拒绝他店
insert into db_contract_change
(change_id, com_id, store_id, contract_id, project_id, property_id, requester_id, owner_id, no, reason, type, status, amount, cost_delta, customer_confirmed, create_date, create_date_time, create_year, create_month, create_day)
values
(2790000000000001001, 2790000000000000001, 2790000000000000102, 2790000000000000901, 2790000000000000801, 2790000000000000601, 2790000000000000206, 2790000000000000208, 'MT-CHG-A-001', '滨湖店测试变更', 'Add', 'Pending', 1000.00, 500.00, b'0', 20261017, 20261017160000, 2026, 10, 17),
(2790000000000001002, 2790000000000000001, 2790000000000000103, 2790000000000000902, 2790000000000000802, 2790000000000000602, 2790000000000000207, 2790000000000000209, 'MT-CHG-B-001', '高新店测试变更', 'Add', 'Pending', 2000.00, 800.00, b'0', 20261017, 20261017160000, 2026, 10, 17),
(2790000000000001003, 2790000000000000001, 2790000000000000102, 2790000000000000901, 2790000000000000801, 2790000000000000601, 2790000000000000206, 2790000000000000208, 'MT-CHG-A-002', 'A店审批通过正向', 'Add', 'Pending', 1500.00, 500.00, b'0', 20261017, 20261017170000, 2026, 10, 17)
on duplicate key update store_id=values(store_id), contract_id=values(contract_id), project_id=values(project_id), property_id=values(property_id), requester_id=values(requester_id), owner_id=values(owner_id), reason=values(reason), status='Pending', amount=values(amount), cost_delta=values(cost_delta), customer_confirmed=b'0';

-- 门店经营利润：A 店 项目毛利 40,000 + 其他收入 10,000 - 企业费用 4,000 = 46,000；B 店 80,000 + 20,000 - 5,000 = 95,000
insert into db_finance_ledger
(ledger_id, com_id, store_id, creator_id, owner_id, direction, purpose, source, amount, category, summary, occurred_at, account, voided, create_date, create_date_time, create_year, create_month, create_day)
values
(2790000000000002001, 2790000000000000001, 2790000000000000102, 2790000000000000204, 2790000000000000204, 'Income', 'OtherIncome', 'Manual', 10000.00, '其他收入', '滨湖店测试其他收入', '2026-10-17', 'CompanyBank', b'0', 20261017, 20261017160000, 2026, 10, 17),
(2790000000000002002, 2790000000000000001, 2790000000000000102, 2790000000000000204, 2790000000000000204, 'Expense', 'CompanyExpense', 'Manual', 4000.00, '企业费用', '滨湖店测试企业费用', '2026-10-17', 'CompanyBank', b'0', 20261017, 20261017160000, 2026, 10, 17),
(2790000000000002003, 2790000000000000001, 2790000000000000103, 2790000000000000205, 2790000000000000205, 'Income', 'OtherIncome', 'Manual', 20000.00, '其他收入', '高新店测试其他收入', '2026-10-17', 'CompanyBank', b'0', 20261017, 20261017160000, 2026, 10, 17),
(2790000000000002004, 2790000000000000001, 2790000000000000103, 2790000000000000205, 2790000000000000205, 'Expense', 'CompanyExpense', 'Manual', 5000.00, '企业费用', '高新店测试企业费用', '2026-10-17', 'CompanyBank', b'0', 20261017, 20261017160000, 2026, 10, 17)
on duplicate key update store_id=values(store_id), direction=values(direction), purpose=values(purpose), amount=values(amount), category=values(category), summary=values(summary), occurred_at=values(occurred_at), voided=b'0';

-- 承接批次与两店待确认项目
insert into carryover_batch
(batch_id, com_id, created_by, as_of_date, status, version, bookkeeping_survey_note, create_date, create_date_time, create_year, create_month, create_day)
values
(2790000000000001101, 2790000000000000001, 2790000000000000201, '2026-10-17', 'Active', 1, '多门店测试承接批次', 20261017, 20261017160000, 2026, 10, 17)
on duplicate key update created_by=values(created_by), as_of_date=values(as_of_date), status='Active', bookkeeping_survey_note=values(bookkeeping_survey_note);

insert into carryover_project
(item_id, com_id, store_id, owner_id, batch_id, status, version, customer_name, customer_phone, community, address, area, manager_id, designer_id, contract_no, signed_amount, current_amount, signed_at, start_date, plan_end_date, project_name, project_status, current_stage_name, progress, remaining_work, current_issue, receivable_outstanding, receivable_due_date, receivable_confirmed, payable_outstanding, payable_due_date, payable_confirmed, cost_total, cost_status, plan_status, plan_version, create_date, create_date_time, create_year, create_month, create_day)
values
(2790000000000001201, 2790000000000000001, 2790000000000000102, 2790000000000000208, 2790000000000001101, 'Ready', 1, '滨湖承接客户', '13900001201', '滨湖测试小区', '滨湖测试路2号3栋301', 100.00, 2790000000000000210, 2790000000000000206, 'MT-CO-A-001', 100000.00, 100000.00, '2026-09-01', '2026-09-01', '2027-01-31', '滨湖承接测试项目', 'Ongoing', '水电阶段', 20, '继续施工并完成后续阶段', null, 30000.00, '2026-10-31', b'1', 10000.00, '2026-10-31', b'1', 50000.00, 'Complete', 'NotStarted', 0, 20261017, 20261017160000, 2026, 10, 17),
(2790000000000001202, 2790000000000000001, 2790000000000000103, 2790000000000000209, 2790000000000001101, 'Ready', 1, '高新承接客户', '13900001202', '高新测试小区', '高新测试路3号4栋402', 120.00, 2790000000000000211, 2790000000000000207, 'MT-CO-B-001', 120000.00, 120000.00, '2026-09-01', '2026-09-01', '2027-01-31', '高新承接测试项目', 'Ongoing', '泥木阶段', 30, '继续施工并完成后续阶段', null, 40000.00, '2026-10-31', b'1', 15000.00, '2026-10-31', b'1', 70000.00, 'Complete', 'NotStarted', 0, 20261017, 20261017160000, 2026, 10, 17)
on duplicate key update store_id=values(store_id), owner_id=values(owner_id), batch_id=values(batch_id), status='Ready', customer_name=values(customer_name), customer_phone=values(customer_phone), community=values(community), address=values(address), area=values(area), manager_id=values(manager_id), designer_id=values(designer_id), current_amount=values(current_amount), receivable_outstanding=values(receivable_outstanding), receivable_confirmed=b'1', payable_outstanding=values(payable_outstanding), payable_confirmed=b'1', cost_total=values(cost_total), cost_status=values(cost_status);

-- 两店月度目标（店长本店目标；公司目标不预置，留给 BOSS 测试）
insert into db_business_target
(target_id, com_id, store_id, target_month, signed_amount, received_amount, gross_profit, active_project_count, create_date, create_date_time, create_year, create_month, create_day)
values
(2790000000000001301, 2790000000000000001, 2790000000000000102, '2026-10-01', 300000.00, 200000.00, 50000.00, 2, 20261017, 20261017160000, 2026, 10, 17),
(2790000000000001302, 2790000000000000001, 2790000000000000103, '2026-10-01', 500000.00, 400000.00, 90000.00, 3, 20261017, 20261017160000, 2026, 10, 17)
on duplicate key update store_id=values(store_id), target_month=values(target_month), signed_amount=values(signed_amount), received_amount=values(received_amount), gross_profit=values(gross_profit), active_project_count=values(active_project_count);
