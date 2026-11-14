-- CHG-20261113-001 rollback
-- Restores the pre-change state captured in:
-- /opt/backups/th4-decoboon/20261113-175541-18660219918-rural-kitchen-boss

START TRANSACTION;

UPDATE db_tenant_employee
SET status = 'Active'
WHERE emp_id = 1789938943545297099
  AND com_id = 1790066191585976744
  AND store_id = 1790066191588014876
  AND role = 'Boss'
  AND status = 'Suspended';

UPDATE db_tenant_employee
SET status = 'Suspended',
    joined_at = '2026-11-03 20:15:15.044061',
    resigned_at = NULL,
    resign_reason = NULL
WHERE emp_id = 1793707837268000001
  AND com_id = 1793516576023939150
  AND store_id = 1793516576031498271
  AND role = 'Boss'
  AND status = 'Active';

UPDATE db_tenant_credentials
SET emp_id = 1789938943545297099
WHERE cre_id = 1789938911566015119
  AND phone = '18660219918'
  AND type = 'Wx'
  AND emp_id = 1793707837268000001;

DELETE FROM db_company_member_log
WHERE log_id IN (1795649600000000005, 1795649600000000006)
  AND reason = '用户要求将 18660219918 / 张伟恢复为农村厨房改造企业负责人';

DELETE FROM db_user_security_log
WHERE log_id = 1794563067229083495
  AND action = 'CompanySwitch'
  AND remark = '用户要求将 18660219918 / 张伟恢复为农村厨房改造企业负责人';

COMMIT;
