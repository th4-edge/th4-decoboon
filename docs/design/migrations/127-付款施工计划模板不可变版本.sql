-- 127 付款/施工计划模板不可变版本与显式平台升级
-- 口径：DEC-083 §4、BR-051、DEC-048、T12-BIZ-20261022-004
-- 策略：每条企业计划模板记录代表一个版本；同一 (com_id, code) 下版本递增。
--       仅 is_current_enabled=1 的版本可用于新合同；历史版本由合同快照继续读取。

ALTER TABLE db_company_payment_plan
    ADD COLUMN is_current_enabled BIT NOT NULL DEFAULT 0 COMMENT '是否当前启用版本' AFTER is_default,
    ADD COLUMN current_enabled_code VARCHAR(64) GENERATED ALWAYS AS (CASE WHEN is_current_enabled = 1 THEN code ELSE NULL END) STORED COMMENT '当前启用编码唯一约束投影' AFTER is_current_enabled;

ALTER TABLE db_company_construction_plan
    ADD COLUMN is_current_enabled BIT NOT NULL DEFAULT 0 COMMENT '是否当前启用版本' AFTER is_default,
    ADD COLUMN current_enabled_code VARCHAR(64) GENERATED ALWAYS AS (CASE WHEN is_current_enabled = 1 THEN code ELSE NULL END) STORED COMMENT '当前启用编码唯一约束投影' AFTER is_current_enabled;

-- 有节点且已启用的存量模板成为当前启用版本；无节点/非启用模板保留草稿/停用。
UPDATE db_company_payment_plan p
SET p.is_current_enabled = CASE
    WHEN p.status = 'Enabled' AND EXISTS (
        SELECT 1 FROM db_company_payment_node n WHERE n.plan_id = p.template_id
    ) THEN 1 ELSE 0 END;

UPDATE db_company_construction_plan p
SET p.is_current_enabled = CASE
    WHEN p.status = 'Enabled' AND EXISTS (
        SELECT 1 FROM db_company_construction_stage s WHERE s.plan_id = p.template_id
    ) THEN 1 ELSE 0 END;

-- 不能保留无明细的已启用模板。
UPDATE db_company_payment_plan p
SET p.status = 'Draft', p.is_default = 0, p.is_current_enabled = 0
WHERE p.status = 'Enabled' AND p.is_current_enabled = 0;

UPDATE db_company_construction_plan p
SET p.status = 'Draft', p.is_default = 0, p.is_current_enabled = 0
WHERE p.status = 'Enabled' AND p.is_current_enabled = 0;

ALTER TABLE db_company_payment_plan
    DROP INDEX uk_company_payment_plan_code,
    ADD UNIQUE KEY uk_company_payment_plan_code_version (com_id, code, version),
    ADD UNIQUE KEY uk_company_payment_plan_current_version (com_id, current_enabled_code),
    ADD KEY idx_company_payment_plan_code_version (com_id, code, version DESC),
    ADD KEY idx_company_payment_plan_current (com_id, status, is_current_enabled, code);

ALTER TABLE db_company_construction_plan
    DROP INDEX uk_company_construction_plan_code,
    ADD UNIQUE KEY uk_company_construction_plan_code_version (com_id, code, version),
    ADD UNIQUE KEY uk_company_construction_plan_current_version (com_id, current_enabled_code),
    ADD KEY idx_company_construction_plan_code_version (com_id, code, version DESC),
    ADD KEY idx_company_construction_plan_current (com_id, status, is_current_enabled, code);


ALTER TABLE db_contract_construction_plan
    ADD COLUMN source_total_days INT NULL COMMENT '模板声明标准总工期快照' AFTER source_version;
