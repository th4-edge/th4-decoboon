-- 版本化迁移账本。所有空库初始化和增量迁移都必须先执行本文件。

CREATE TABLE IF NOT EXISTS db_schema_migration (
    version VARCHAR(64) NOT NULL,
    description VARCHAR(255) NOT NULL,
    checksum CHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    started_at DATETIME(6) NOT NULL,
    finished_at DATETIME(6) NULL,
    applied_by VARCHAR(128) NOT NULL,
    error_message VARCHAR(1000) NULL,
    PRIMARY KEY (version),
    KEY idx_schema_migration_status (status, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
