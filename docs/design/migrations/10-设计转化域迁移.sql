-- 设计转化域历史迁移草稿（不完整，禁止自动执行）
-- 新建库只使用 00-迁移账本.sql + 01-空库基线.sql；生产库必须使用 ddl-auto=validate。
-- 存量库必须由 DBA 完成结构审计后使用 run-migrations adopt 登记；design、quote、
-- task、material plan 等差异必须另建完整、可回滚、可演练的差异迁移。
-- 本文件仅保留早期三项列修改作为审计线索，不保证目标结构完整，禁止由任何 upgrade
-- 自动路径执行。

-- 1. 报价不再强制依赖套餐，允许 design_id 报价。
ALTER TABLE db_quote
    MODIFY COLUMN gift_id BIGINT NULL;

-- 2. 设计报价生成项目时，施工阶段可按报价分类直接创建，不必强依赖 BIM Modular。
ALTER TABLE db_project_stage
    MODIFY COLUMN mod_id BIGINT NULL;

-- 3. 设计报价施工任务直接引用报价快照，允许没有旧套餐施工主数据。
ALTER TABLE db_project_task
    MODIFY COLUMN const_id BIGINT NULL;

-- 以下对象缺失时不得依赖 Hibernate 自动建表；必须纳入独立完整差异迁移后评审执行：
-- db_design
-- db_design_requirement
-- db_design_space
-- db_design_item
-- db_design_talk
-- db_design_talk_section
-- db_quote.design_id
-- db_quote_item.{design_item_id, space_id, type, category, material_price, manual_price, pricing_mode, amount, formula, sort}
-- db_project_task.quote_item_id
-- db_material_plan.quote_item_id
