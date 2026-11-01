-- SMB P0 acceptance fixture. All IDs are new; no existing project/quote/contract/stage is updated.
-- Source objects are cloned from the existing local test chain only to preserve schema-compatible fields.
-- Before running, verify the target IDs do not exist.
SELECT 'preflight' AS phase,
       (SELECT COUNT(*) FROM quote WHERE quote_id=226702200000000001) AS quote_exists,
       (SELECT COUNT(*) FROM db_project_contract WHERE contract_id=226702200000000002) AS contract_exists,
       (SELECT COUNT(*) FROM db_project WHERE project_id=226702200000000003) AS project_exists,
       (SELECT COUNT(*) FROM db_project_stage WHERE stage_id=226702200000000004) AS stage_exists,
       (SELECT COUNT(*) FROM db_acceptance WHERE acceptance_id=226702200000000005) AS acceptance_exists;

CREATE TEMPORARY TABLE tmp_smb_quote AS SELECT * FROM quote WHERE quote_id=226691266090369024;
UPDATE tmp_smb_quote
   SET quote_id=226702200000000001,
       no='Q-ACCEPT-SMB-P0-001',
       title='SMB P0 独立验收报价',
       status='Accepted',
       customer_id=226701371305361408,
       property_id=226701441081802752,
       design_id=NULL,
       emp_id=1890000000000000205;
INSERT INTO quote SELECT * FROM tmp_smb_quote;
DROP TEMPORARY TABLE tmp_smb_quote;

CREATE TEMPORARY TABLE tmp_smb_contract AS SELECT * FROM db_project_contract WHERE contract_id=226691331676700672;
UPDATE tmp_smb_contract
   SET contract_id=226702200000000002,
       no='C-ACCEPT-SMB-P0-001',
       quote_id=226702200000000001,
       customer_id=226701371305361408,
       property_id=226701441081802752,
       project_id=NULL,
       status='Effective';
INSERT INTO db_project_contract SELECT * FROM tmp_smb_contract;
DROP TEMPORARY TABLE tmp_smb_contract;

CREATE TEMPORARY TABLE tmp_smb_project AS SELECT * FROM db_project WHERE project_id=226691387045707776;
UPDATE tmp_smb_project
   SET project_id=226702200000000003,
       no='P-ACCEPT-SMB-P0-001',
       name='SMB P0 独立验收项目 · 101',
       site_addr='验收小区1栋1单元101',
       quote_id=226702200000000001,
       customer_id=226701371305361408,
       property_id=226701441081802752,
       status='Preparing',
       progress=0,
       actual_end_date=NULL,
       manager_id=1890000000000000206,
       designer_id=1890000000000000205,
       support_id=1890000000000000207;
INSERT INTO db_project SELECT * FROM tmp_smb_project;
DROP TEMPORARY TABLE tmp_smb_project;

UPDATE db_project_contract
   SET project_id=226702200000000003
 WHERE contract_id=226702200000000002;

CREATE TEMPORARY TABLE tmp_smb_stage AS SELECT * FROM db_project_stage WHERE stage_id=226691387070873600;
UPDATE tmp_smb_stage
   SET stage_id=226702200000000004,
       project_id=226702200000000003,
       property_id=226701441081802752,
       status='Todo',
       progress=0,
       actual_start=NULL,
       actual_end=NULL;
INSERT INTO db_project_stage SELECT * FROM tmp_smb_stage;
DROP TEMPORARY TABLE tmp_smb_stage;

INSERT INTO db_acceptance
(acceptance_id,create_date,create_date_time,create_day,create_hour,create_minute,create_month,create_second,create_time,create_week,create_year,
 update_date,update_date_time,update_day,update_hour,update_minute,update_month,update_second,update_time,update_week,update_year,
 conclusion,confirmed_at,status,com_id,project_id,property_id,stage_id,emp_id,confirmed_by,owner_id,store_id,team_id,rectification_due_date,rectification_owner_id)
VALUES
(226702200000000005,20260918,20260918220000,18,22,0,9,0,220000,5,2026,
 20260918,20260918220000,18,22,0,9,0,220000,5,2026,
 NULL,NULL,'Pending',1890000000000000001,226702200000000003,226701441081802752,226702200000000004,1890000000000000206,NULL,1890000000000000204,1890000000000000101,1890000000000000301,NULL,NULL);

-- Make the dedicated measure initially Scheduled so B2 can prove that incomplete measure blocks opening.
UPDATE db_measure SET status='Scheduled', executor_id=NULL WHERE measure_id=226701602042413056;
UPDATE db_property SET latest_measure_at=NULL, status='PendingMeasure' WHERE property_id=226701441081802752;

SELECT 'created' AS phase,
       (SELECT COUNT(*) FROM quote WHERE quote_id=226702200000000001) AS quote_count,
       (SELECT COUNT(*) FROM db_project_contract WHERE contract_id=226702200000000002) AS contract_count,
       (SELECT COUNT(*) FROM db_project WHERE project_id=226702200000000003) AS project_count,
       (SELECT COUNT(*) FROM db_project_stage WHERE stage_id=226702200000000004) AS stage_count,
       (SELECT COUNT(*) FROM db_acceptance WHERE acceptance_id=226702200000000005) AS acceptance_count;
