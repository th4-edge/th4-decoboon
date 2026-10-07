-- 回滚：恢复 2026-10-07 删除的张伟员工薪酬记录。
-- 执行顺序必须先工资单、再工资项、再薪酬方案，满足外键依赖。
-- 仅在确认需要恢复且对应主键不存在时执行。

INSERT INTO db_payroll_sheet (
    sheet_id, com_id, store_id, team_id, owner_id, period, status,
    attendance_complete, commission_complete, total_gross_amount, total_deduction_amount,
    total_net_amount, confirmed_by, confirmed_at, remark, creator_id,
    create_date_time, create_date, create_time, create_year, create_month, create_day,
    create_hour, create_minute, create_second, create_week,
    update_date_time, update_date, update_time, update_year, update_month, update_day,
    update_hour, update_minute, update_second, update_week
) VALUES (
    233340455780089856, 1790066191585976744, 1790066191588014876, NULL,
    1789938943545297099, '2026-10', 'Draft', b'0', b'1', 3000.00, 0.00, 3000.00,
    NULL, NULL, NULL, 1789938943545297099,
    20261007053139, 20261007, 53139, 2026, 10, 7, 5, 31, 39, 2,
    20261007111231, 20261007, 111231, 2026, 10, 7, 11, 12, 31, 2
);

INSERT INTO db_payroll_item (
    item_id, com_id, store_id, team_id, emp_id, sheet_id, project_id,
    attribution, attribution_key, base_salary_amount, attendance_deduction,
    commission_amount, bonus_amount, allowance_amount, social_insurance_amount,
    tax_amount, other_deduction, gross_amount, deduction_amount, net_amount,
    status, remark, creator_id, create_date_time, create_date, create_time,
    create_year, create_month, create_day, create_hour, create_minute, create_second,
    create_week, update_date_time, update_date, update_time, update_year, update_month,
    update_day, update_hour, update_minute, update_second, update_week
) VALUES (
    233426237433839616, 1790066191585976744, 1790066191588014876, NULL,
    1789938943545297099, 233340455780089856, NULL, 'Company', 'COMPANY',
    3000.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 3000.00, 0.00, 3000.00,
    'Draft', NULL, 1790066191593026092,
    20261007111231, 20261007, 111231, 2026, 10, 7, 11, 12, 31, 2,
    20261007111231, 20261007, 111231, 2026, 10, 7, 11, 12, 31, 2
);

INSERT INTO db_payroll_compensation_scheme (
    scheme_id, com_id, store_id, team_id, emp_id, base_salary, scheduled_days,
    effective_from, effective_to, status, remark, creator_id,
    create_date_time, create_date, create_time, create_year, create_month, create_day,
    create_hour, create_minute, create_second, create_week,
    update_date_time, update_date, update_time, update_year, update_month, update_day,
    update_hour, update_minute, update_second, update_week
) VALUES (
    233353300731232256, 1790066191585976744, 1790066191588014876, NULL,
    1789938943545297099, 3000.00, 26, '2026-10-07', NULL, 'Active', '',
    1789938943545297099,
    20261007062242, 20261007, 62242, 2026, 10, 7, 6, 22, 42, 2,
    20261007062242, 20261007, 62242, 2026, 10, 7, 6, 22, 42, 2
);
