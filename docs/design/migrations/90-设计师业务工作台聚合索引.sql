-- 90-设计师业务工作台聚合索引.sql
-- 目的：为设计师业务工作台的固定聚合 SQL 补齐联合索引。
-- 适用：MySQL 8.0+。存量库执行前必须备份；空库部署需在下一次基线生成时包含本迁移。
-- 原则：com_id 始终最左；后续列按真实连接/过滤顺序排列，避免宽而无用的索引。

CREATE INDEX idx_cust_customer_company_designer
    ON cust_customer (com_id, designer_id, cust_id);

CREATE INDEX idx_project_company_designer_status
    ON db_project (com_id, designer_id, status, project_id);

CREATE INDEX idx_design_company_property_status
    ON db_design (com_id, property_id, status, design_id);

CREATE INDEX idx_material_plan_company_project_status
    ON db_material_plan (com_id, project_id, status, plan_id);

CREATE INDEX idx_contract_change_company_project_status
    ON db_contract_change (com_id, project_id, status, change_id);

CREATE INDEX idx_acceptance_company_project_status
    ON db_acceptance (com_id, project_id, status, acceptance_id);

CREATE INDEX idx_project_stage_company_project_status
    ON db_project_stage (com_id, project_id, status, stage_id);

CREATE INDEX idx_project_contract_company_customer_status
    ON db_project_contract (com_id, customer_id, status, contract_id);

-- 回滚（仅在确认不再使用设计师业务工作台聚合索引时执行）：
-- DROP INDEX idx_project_contract_company_customer_status ON db_project_contract;
-- DROP INDEX idx_project_stage_company_project_status ON db_project_stage;
-- DROP INDEX idx_acceptance_company_project_status ON db_acceptance;
-- DROP INDEX idx_contract_change_company_project_status ON db_contract_change;
-- DROP INDEX idx_material_plan_company_project_status ON db_material_plan;
-- DROP INDEX idx_design_company_property_status ON db_design;
-- DROP INDEX idx_project_company_designer_status ON db_project;
-- DROP INDEX idx_cust_customer_company_designer ON cust_customer;
