-- 95-增减项目标快照与当前有效剩余.sql
-- 目的：DEC-055 / BR-041 增减项三类动作（新增分项 / 减项 / 替换）。
-- 影响表：db_contract_change_item、db_contract_change。
-- 前置：db_contract_change、db_contract_change_item、catalog_item、db_contract_line 已存在。
-- 说明：新列允许为空以兼容历史数据，不自动回填；历史 Price/Quantity 和缺少目标快照的记录保持只读。
-- 注意：本文件尚未对目标 MySQL 或生产库执行；执行前必须完成备份、回滚校验和迁移账本登记。

alter table db_contract_change_item
    add column source_change_item_id bigint null comment '已生效新增/替换目标来源' after contract_line_id,
    add column source_remaining_quantity decimal(12,3) null comment '创建时来源当前有效剩余工程量' after amount_delta,
    add column source_remaining_amount decimal(14,2) null comment '创建时来源当前有效剩余金额' after source_remaining_quantity,
    add column source_unit_price decimal(12,2) null comment '创建时来源当前有效单价' after source_remaining_amount,
    add column source_baseline_time datetime(6) null comment '来源基线时间' after source_unit_price,
    add column target_catalog_item_id bigint null comment '目标报价项目' after source_baseline_time,
    add column target_name varchar(256) null comment '目标名称快照' after target_catalog_item_id,
    add column target_type varchar(20) null comment '目标类型快照' after target_name,
    add column target_category varchar(64) null comment '目标分类快照' after target_type,
    add column target_brand varchar(128) null comment '目标品牌快照' after target_category,
    add column target_series varchar(128) null comment '目标系列快照' after target_brand,
    add column target_spec varchar(256) null comment '目标规格快照' after target_series,
    add column target_unit varchar(32) null comment '目标单位快照' after target_spec,
    add column target_pricing_mode varchar(24) null comment '目标计价方式' after target_unit,
    add column target_material_price decimal(12,2) null comment '目标材料单价' after target_pricing_mode,
    add column target_manual_price decimal(12,2) null comment '目标人工单价' after target_material_price,
    add column target_fixed_total_price decimal(12,2) null comment '目标固定总价' after target_manual_price,
    add column target_formula varchar(256) null comment '目标公式快照' after target_fixed_total_price,
    add column target_craft longtext null comment '目标施工工艺' after target_formula,
    add column target_criterion longtext null comment '目标验收标准' after target_craft,
    add column target_quantity decimal(12,3) null comment '目标工程量' after target_criterion,
    add column target_amount decimal(14,2) null comment '目标金额' after target_quantity,
    add column target_cost_price decimal(12,2) null comment '目标成本单价；null=待维护' after target_amount,
    add column target_cost_amount decimal(14,2) null comment '目标成本金额；null=待维护' after target_cost_price,
    add column target_cost_complete tinyint(1) not null default 0 comment '目标成本是否完整' after target_cost_amount,
    modify column cost_delta decimal(14,2) null comment '成本变化；null=存在成本未完整',
    add key idx_change_item_company_source_line (com_id, contract_line_id, change_item_id),
    add key idx_change_item_company_source_change (com_id, source_change_item_id, change_item_id),
    add key idx_change_item_company_target_catalog (com_id, target_catalog_item_id, change_item_id);

alter table db_contract_change
    modify column cost_delta decimal(14,2) null comment '成本变化；null=存在成本未完整';

-- 回滚：
-- alter table db_contract_change
--     modify column cost_delta decimal(14,2) not null default 0 comment '成本变化';
-- alter table db_contract_change_item
--     drop index idx_change_item_company_target_catalog,
--     drop index idx_change_item_company_source_change,
--     drop index idx_change_item_company_source_line,
--     drop column target_cost_complete,
--     drop column target_cost_amount,
--     drop column target_cost_price,
--     drop column target_amount,
--     drop column target_quantity,
--     drop column target_criterion,
--     drop column target_craft,
--     drop column target_formula,
--     drop column target_fixed_total_price,
--     drop column target_manual_price,
--     drop column target_material_price,
--     drop column target_pricing_mode,
--     drop column target_unit,
--     drop column target_spec,
--     drop column target_series,
--     drop column target_brand,
--     drop column target_category,
--     drop column target_type,
--     drop column target_name,
--     drop column target_catalog_item_id,
--     drop column source_baseline_time,
--     drop column source_unit_price,
--     drop column source_remaining_amount,
--     drop column source_remaining_quantity,
--     drop column source_change_item_id,
--     modify column cost_delta decimal(14,2) not null default 0 comment '成本变化';
