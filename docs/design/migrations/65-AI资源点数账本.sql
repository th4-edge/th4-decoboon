-- 65 AI资源点数账本
-- 依据：ANS-BIZ-20260926-004、DEC-012、BR-011。
-- A0.2 只建立内部点数账户和账本机制；真实费率、套餐点数和资源包价格暂缓到 A0.4。

CREATE TABLE IF NOT EXISTS `db_ai_resource_account` (
  `account_id` BIGINT NOT NULL COMMENT '账户索引（雪花）',
  `com_id` BIGINT NOT NULL COMMENT '所属企业，唯一收费主体',
  `total_granted` BIGINT NOT NULL DEFAULT 0 COMMENT '累计发放点数',
  `available_units` BIGINT NOT NULL DEFAULT 0 COMMENT '可用点数',
  `reserved_units` BIGINT NOT NULL DEFAULT 0 COMMENT '预占点数',
  `consumed_units` BIGINT NOT NULL DEFAULT 0 COMMENT '累计消耗点数',
  `refunded_units` BIGINT NOT NULL DEFAULT 0 COMMENT '累计返还点数',
  `adjusted_units` BIGINT NOT NULL DEFAULT 0 COMMENT '累计调整点数',
  `balance_version` BIGINT NOT NULL DEFAULT 0 COMMENT '余额版本',
  `status` VARCHAR(16) NOT NULL DEFAULT 'Active' COMMENT 'Active/Frozen',
  `create_date_time` BIGINT NULL, `create_date` INT NULL, `create_time` INT NULL,
  `create_year` INT NULL, `create_month` INT NULL, `create_day` INT NULL,
  `create_hour` INT NULL, `create_minute` INT NULL, `create_second` INT NULL, `create_week` TINYINT NULL,
  `update_date_time` BIGINT NULL, `update_date` INT NULL, `update_time` INT NULL,
  `update_year` INT NULL, `update_month` INT NULL, `update_day` INT NULL,
  `update_hour` INT NULL, `update_minute` INT NULL, `update_second` INT NULL, `update_week` TINYINT NULL,
  PRIMARY KEY (`account_id`),
  UNIQUE KEY `uk_ai_resource_account_company` (`com_id`),
  KEY `idx_ai_resource_account_company` (`com_id`, `account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业AI资源账户';

CREATE TABLE IF NOT EXISTS `db_ai_rate_card_version` (
  `rate_card_id` BIGINT NOT NULL COMMENT '费率版本索引（雪花）',
  `capability_code` VARCHAR(64) NOT NULL COMMENT 'AI能力编码',
  `unit_type` VARCHAR(32) NOT NULL COMMENT '原生资源单位',
  `units_per_point` DECIMAL(18,6) NOT NULL COMMENT '每点资源单位',
  `model_multiplier` DECIMAL(10,4) NOT NULL DEFAULT 1 COMMENT '模型倍率',
  `quality_multiplier` DECIMAL(10,4) NOT NULL DEFAULT 1 COMMENT '质量档位倍率',
  `version_no` INT NOT NULL COMMENT '版本号',
  `status` VARCHAR(16) NOT NULL DEFAULT 'Draft' COMMENT 'Draft/Active/Disabled',
  `effective_from` DATETIME(6) NULL COMMENT '生效时间',
  `effective_to` DATETIME(6) NULL COMMENT '失效时间',
  `approved_by` BIGINT NULL COMMENT '审批人',
  `create_date_time` BIGINT NULL, `create_date` INT NULL, `create_time` INT NULL,
  `create_year` INT NULL, `create_month` INT NULL, `create_day` INT NULL,
  `create_hour` INT NULL, `create_minute` INT NULL, `create_second` INT NULL, `create_week` TINYINT NULL,
  `update_date_time` BIGINT NULL, `update_date` INT NULL, `update_time` INT NULL,
  `update_year` INT NULL, `update_month` INT NULL, `update_day` INT NULL,
  `update_hour` INT NULL, `update_minute` INT NULL, `update_second` INT NULL, `update_week` TINYINT NULL,
  PRIMARY KEY (`rate_card_id`),
  UNIQUE KEY `uk_ai_rate_card_version` (`capability_code`, `version_no`),
  KEY `idx_ai_rate_card_active` (`capability_code`, `status`, `version_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI费率版本';

CREATE TABLE IF NOT EXISTS `db_ai_resource_ledger` (
  `ledger_id` BIGINT NOT NULL COMMENT '账本索引（雪花）',
  `com_id` BIGINT NOT NULL COMMENT '所属企业',
  `allocation_store_id` BIGINT NULL COMMENT '分摊门店；空为企业公共',
  `call_id` BIGINT NULL COMMENT '关联AI调用',
  `type` VARCHAR(16) NOT NULL COMMENT 'Grant/Reserve/Consume/Release/Refund/Expire/Adjust',
  `change_units` BIGINT NOT NULL COMMENT '可用点数变动',
  `balance_before` BIGINT NOT NULL COMMENT '变动前可用点数',
  `balance_after` BIGINT NOT NULL COMMENT '变动后可用点数',
  `rate_card_version` VARCHAR(64) NULL COMMENT '费率版本快照',
  `reason` VARCHAR(300) NULL COMMENT '变动原因',
  `operator_id` BIGINT NULL COMMENT '操作人',
  `occurred_at` DATETIME(6) NOT NULL COMMENT '发生时间',
  `create_date_time` BIGINT NULL, `create_date` INT NULL, `create_time` INT NULL,
  `create_year` INT NULL, `create_month` INT NULL, `create_day` INT NULL,
  `create_hour` INT NULL, `create_minute` INT NULL, `create_second` INT NULL, `create_week` TINYINT NULL,
  `update_date_time` BIGINT NULL, `update_date` INT NULL, `update_time` INT NULL,
  `update_year` INT NULL, `update_month` INT NULL, `update_day` INT NULL,
  `update_hour` INT NULL, `update_minute` INT NULL, `update_second` INT NULL, `update_week` TINYINT NULL,
  PRIMARY KEY (`ledger_id`),
  KEY `idx_ai_ledger_company_time` (`com_id`, `occurred_at`, `ledger_id`),
  KEY `idx_ai_ledger_company_call` (`com_id`, `call_id`, `ledger_id`),
  KEY `idx_ai_ledger_company_store` (`com_id`, `allocation_store_id`, `occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI点数账本（append-only）';
