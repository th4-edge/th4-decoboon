-- 48-量房预约完成时间.sql
-- 用户 2026-09-22 冻结：预约时间使用房屋级 Measure.scheduledAt；
-- 量房完成保留实际完成时间 completedAt；measureTime 仅作兼容投影。
-- 本脚本可重复执行；历史数据优先按 status 和 measure_time 回填。

SET @db_name = DATABASE();

SET @has_scheduled_at = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @db_name AND table_name = 'db_measure' AND column_name = 'scheduled_at'
);
SET @sql = IF(@has_scheduled_at = 0,
    'ALTER TABLE db_measure ADD COLUMN scheduled_at DATETIME(6) NULL COMMENT ''计划上门时间'' AFTER measure_time',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_completed_at = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @db_name AND table_name = 'db_measure' AND column_name = 'completed_at'
);
SET @sql = IF(@has_completed_at = 0,
    'ALTER TABLE db_measure ADD COLUMN completed_at DATETIME(6) NULL COMMENT ''实际完成时间'' AFTER scheduled_at',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE db_measure
SET scheduled_at = measure_time
WHERE scheduled_at IS NULL
  AND measure_time IS NOT NULL
  AND status IN ('Scheduled', 'Cancelled');

UPDATE db_measure m
JOIN (
    SELECT property_id, MIN(measure_time) AS first_scheduled_at
    FROM db_measure
    WHERE status = 'Scheduled'
      AND measure_time IS NOT NULL
    GROUP BY property_id
) s ON s.property_id = m.property_id
SET m.scheduled_at = s.first_scheduled_at
WHERE m.status = 'Completed'
  AND m.scheduled_at IS NULL;

UPDATE db_measure
SET completed_at = measure_time
WHERE completed_at IS NULL
  AND measure_time IS NOT NULL
  AND status = 'Completed';

SET @has_scheduled_index = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = @db_name AND table_name = 'db_measure'
      AND index_name = 'idx_measure_company_scheduled_at'
);
SET @sql = IF(@has_scheduled_index = 0,
    'CREATE INDEX idx_measure_company_scheduled_at ON db_measure (com_id, scheduled_at)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_completed_index = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = @db_name AND table_name = 'db_measure'
      AND index_name = 'idx_measure_company_completed_at'
);
SET @sql = IF(@has_completed_index = 0,
    'CREATE INDEX idx_measure_company_completed_at ON db_measure (com_id, completed_at)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 回滚（人工确认后执行）：
-- DROP INDEX idx_measure_company_completed_at ON db_measure;
-- DROP INDEX idx_measure_company_scheduled_at ON db_measure;
-- ALTER TABLE db_measure DROP COLUMN completed_at;
-- ALTER TABLE db_measure DROP COLUMN scheduled_at;
