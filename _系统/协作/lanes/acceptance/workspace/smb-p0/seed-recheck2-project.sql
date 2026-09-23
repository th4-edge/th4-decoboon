-- Second-round acceptance fixture, new IDs only. It references the independent property 303
-- which is currently Scheduled so B2 can be tested, then completed by Designer for B3/C rechecks.
SELECT 'preflight' phase,
 (SELECT COUNT(*) FROM quote WHERE quote_id=226706000000000001) quote_exists,
 (SELECT COUNT(*) FROM db_project_contract WHERE contract_id=226706000000000002) contract_exists,
 (SELECT COUNT(*) FROM db_project WHERE project_id=226706000000000003) project_exists,
 (SELECT COUNT(*) FROM db_project_stage WHERE stage_id IN (226706000000000004,226706000000000005)) stage_exists;

CREATE TEMPORARY TABLE tmp_recheck2_quote AS SELECT * FROM quote WHERE quote_id=226702200000000001;
UPDATE tmp_recheck2_quote SET quote_id=226706000000000001,no='Q-RECHECK2-SMB-P0',title='SMB P0 第二轮复验报价',status='Accepted',property_id=226704069090082816,design_id=NULL;
INSERT INTO quote SELECT * FROM tmp_recheck2_quote;
DROP TEMPORARY TABLE tmp_recheck2_quote;

CREATE TEMPORARY TABLE tmp_recheck2_contract AS SELECT * FROM db_project_contract WHERE contract_id=226702200000000002;
UPDATE tmp_recheck2_contract SET contract_id=226706000000000002,no='C-RECHECK2-SMB-P0',quote_id=226706000000000001,project_id=NULL,property_id=226704069090082816,status='Effective';
INSERT INTO db_project_contract SELECT * FROM tmp_recheck2_contract;
DROP TEMPORARY TABLE tmp_recheck2_contract;

CREATE TEMPORARY TABLE tmp_recheck2_project AS SELECT * FROM db_project WHERE project_id=226702200000000003;
UPDATE tmp_recheck2_project SET project_id=226706000000000003,no='P-RECHECK2-SMB-P0',name='SMB P0 第二轮复验项目',site_addr='验收小区3栋1单元303',quote_id=226706000000000001,property_id=226704069090082816,status='Preparing',progress=0,actual_end_date=NULL,manager_id=1890000000000000206,designer_id=1890000000000000205,support_id=1890000000000000207;
INSERT INTO db_project SELECT * FROM tmp_recheck2_project;
DROP TEMPORARY TABLE tmp_recheck2_project;
UPDATE db_project_contract SET project_id=226706000000000003 WHERE contract_id=226706000000000002;

CREATE TEMPORARY TABLE tmp_recheck2_stage1 AS SELECT * FROM db_project_stage WHERE stage_id=226702200000000004;
UPDATE tmp_recheck2_stage1 SET stage_id=226706000000000004,project_id=226706000000000003,property_id=226704069090082816,status='Todo',progress=0,actual_start=NULL,actual_end=NULL;
INSERT INTO db_project_stage SELECT * FROM tmp_recheck2_stage1;
DROP TEMPORARY TABLE tmp_recheck2_stage1;

CREATE TEMPORARY TABLE tmp_recheck2_stage2 AS SELECT * FROM db_project_stage WHERE stage_id=226702200000000006;
UPDATE tmp_recheck2_stage2 SET stage_id=226706000000000005,project_id=226706000000000003,property_id=226704069090082816,status='Todo',progress=0,actual_start=NULL,actual_end=NULL,name='C4 并发复验阶段';
INSERT INTO db_project_stage SELECT * FROM tmp_recheck2_stage2;
DROP TEMPORARY TABLE tmp_recheck2_stage2;

SELECT 'created' phase,
 (SELECT COUNT(*) FROM quote WHERE quote_id=226706000000000001) quote_count,
 (SELECT COUNT(*) FROM db_project_contract WHERE contract_id=226706000000000002) contract_count,
 (SELECT COUNT(*) FROM db_project WHERE project_id=226706000000000003) project_count,
 (SELECT COUNT(*) FROM db_project_stage WHERE stage_id IN (226706000000000004,226706000000000005)) stage_count;
