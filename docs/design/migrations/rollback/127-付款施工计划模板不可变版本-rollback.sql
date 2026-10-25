-- 回滚 127 付款/施工计划模板不可变版本
-- 前置：后端已回退，并已完成数据库备份。
-- 多版本处理：先按 code 保留 currentEnabled 版本；无 current 时保留最高版本。
--             其余版本及其节点/阶段复制到 rollback_127 归档表后再删除，避免静默丢失。

DROP TABLE IF EXISTS db_contract_construction_plan_rollback_127;
DROP TABLE IF EXISTS db_company_payment_node_rollback_127;
DROP TABLE IF EXISTS db_company_payment_plan_rollback_127;
DROP TABLE IF EXISTS db_company_construction_stage_rollback_127;
DROP TABLE IF EXISTS db_company_construction_plan_rollback_127;

CREATE TABLE db_contract_construction_plan_rollback_127 AS
SELECT * FROM db_contract_construction_plan WHERE 1 = 0;
INSERT INTO db_contract_construction_plan_rollback_127
SELECT * FROM db_contract_construction_plan;

CREATE TABLE db_company_payment_plan_rollback_127 AS SELECT * FROM db_company_payment_plan WHERE 1 = 0;
CREATE TABLE db_company_payment_node_rollback_127 AS SELECT * FROM db_company_payment_node WHERE 1 = 0;
CREATE TABLE db_company_construction_plan_rollback_127 AS SELECT * FROM db_company_construction_plan WHERE 1 = 0;
CREATE TABLE db_company_construction_stage_rollback_127 AS SELECT * FROM db_company_construction_stage WHERE 1 = 0;

INSERT INTO db_company_payment_plan_rollback_127
SELECT p.*
FROM db_company_payment_plan p
WHERE p.template_id NOT IN (
    SELECT keep_id FROM (
        SELECT p2.template_id AS keep_id
        FROM db_company_payment_plan p2
        WHERE NOT EXISTS (
            SELECT 1
            FROM db_company_payment_plan p3
            WHERE p3.com_id = p2.com_id
              AND p3.code = p2.code
              AND (
                  (p3.is_current_enabled = 1 AND p2.is_current_enabled = 0)
                  OR (p3.is_current_enabled = p2.is_current_enabled AND p3.version > p2.version)
              )
        )
    ) kept
);

INSERT INTO db_company_payment_node_rollback_127
SELECT n.*
FROM db_company_payment_node n
JOIN db_company_payment_plan_rollback_127 archived ON archived.template_id = n.plan_id;

DELETE n
FROM db_company_payment_node n
JOIN db_company_payment_plan_rollback_127 archived ON archived.template_id = n.plan_id;

DELETE p
FROM db_company_payment_plan p
JOIN db_company_payment_plan_rollback_127 archived ON archived.template_id = p.template_id;

INSERT INTO db_company_construction_plan_rollback_127
SELECT p.*
FROM db_company_construction_plan p
WHERE p.template_id NOT IN (
    SELECT keep_id FROM (
        SELECT p2.template_id AS keep_id
        FROM db_company_construction_plan p2
        WHERE NOT EXISTS (
            SELECT 1
            FROM db_company_construction_plan p3
            WHERE p3.com_id = p2.com_id
              AND p3.code = p2.code
              AND (
                  (p3.is_current_enabled = 1 AND p2.is_current_enabled = 0)
                  OR (p3.is_current_enabled = p2.is_current_enabled AND p3.version > p2.version)
              )
        )
    ) kept
);

INSERT INTO db_company_construction_stage_rollback_127
SELECT s.*
FROM db_company_construction_stage s
JOIN db_company_construction_plan_rollback_127 archived ON archived.template_id = s.plan_id;

DELETE s
FROM db_company_construction_stage s
JOIN db_company_construction_plan_rollback_127 archived ON archived.template_id = s.plan_id;

DELETE p
FROM db_company_construction_plan p
JOIN db_company_construction_plan_rollback_127 archived ON archived.template_id = p.template_id;

ALTER TABLE db_contract_construction_plan
    DROP COLUMN source_total_days;

ALTER TABLE db_company_payment_plan
    DROP INDEX idx_company_payment_plan_current,
    DROP INDEX idx_company_payment_plan_code_version,
    DROP INDEX uk_company_payment_plan_current_version,
    DROP INDEX uk_company_payment_plan_code_version,
    ADD UNIQUE KEY uk_company_payment_plan_code (com_id, code),
    DROP COLUMN current_enabled_code,
    DROP COLUMN is_current_enabled;

ALTER TABLE db_company_construction_plan
    DROP INDEX idx_company_construction_plan_current,
    DROP INDEX idx_company_construction_plan_code_version,
    DROP INDEX uk_company_construction_plan_current_version,
    DROP INDEX uk_company_construction_plan_code_version,
    ADD UNIQUE KEY uk_company_construction_plan_code (com_id, code),
    DROP COLUMN current_enabled_code,
    DROP COLUMN is_current_enabled;
