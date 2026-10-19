-- 2026-10-17 麦鸿丁女士万达中心名宅设计方案：Ready -> Draft
-- 生产库 decoboon；执行前已核对 design_id/企业/客户/房屋唯一匹配。
-- 用户明确要求不备份数据库。

START TRANSACTION;

UPDATE db_design
SET status = 'Draft',
    update_date_time = CAST(DATE_FORMAT(NOW(), '%Y%m%d%H%i%s') AS UNSIGNED),
    update_date = CAST(DATE_FORMAT(NOW(), '%Y%m%d') AS UNSIGNED),
    update_time = CAST(DATE_FORMAT(NOW(), '%H%i%s') AS UNSIGNED),
    update_year = YEAR(NOW()),
    update_month = MONTH(NOW()),
    update_day = DAY(NOW()),
    update_hour = HOUR(NOW()),
    update_minute = MINUTE(NOW()),
    update_second = SECOND(NOW()),
    update_week = WEEKDAY(NOW())
WHERE design_id = 237046223691841536
  AND com_id = 1790066191585976744
  AND customer_id = 236766871070703616
  AND property_id = 236767161878577152
  AND status = 'Ready';

SELECT ROW_COUNT() AS affected_rows;

COMMIT;
