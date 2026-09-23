-- 49-主材选材分项结果.sql
-- 合同 Material 行作为项目主材分项清单；db_material_selection 存该分项的历次结果版本。
-- selection_json 为界面回填与导出快照，不参与报价、成本、材料数量或采购金额计算。

alter table db_material_selection
    add column source_contract_line_id bigint null after project_id,
    add column selection_json longtext null after content;

alter table db_material_selection
    drop index uk_material_selection_version;

alter table db_material_selection
    add unique key uk_material_selection_item_version (com_id, source_contract_line_id, version),
    add key idx_material_selection_company_item (com_id, source_contract_line_id, selection_id);

alter table db_material_selection
    add constraint fk_material_selection_contract_line
        foreign key (source_contract_line_id) references db_contract_line (line_id);

-- 回滚（人工确认后执行）：
-- alter table db_material_selection drop foreign key fk_material_selection_contract_line;
-- alter table db_material_selection drop index idx_material_selection_company_item;
-- alter table db_material_selection drop index uk_material_selection_item_version;
-- alter table db_material_selection add unique key uk_material_selection_version (com_id, property_id, contract_id, version);
-- alter table db_material_selection drop column selection_json;
-- alter table db_material_selection drop column source_contract_line_id;
