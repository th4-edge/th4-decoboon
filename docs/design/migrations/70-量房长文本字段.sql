-- ECD-009：Measure 的四个 @Lob 字段在空库基线中历史建为 tinytext，
-- ddl-auto:update 不会把既有 tinytext 升级为 longtext。
-- 本迁移必须幂等，生产执行前按发布规范完成备份和隔离 MySQL 8 演练。
ALTER TABLE db_measure
    MODIFY COLUMN original_structure LONGTEXT NULL,
    MODIFY COLUMN dimensions LONGTEXT NULL,
    MODIFY COLUMN customer_requirements LONGTEXT NULL,
    MODIFY COLUMN designer_conclusion LONGTEXT NULL;
