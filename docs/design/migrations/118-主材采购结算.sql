-- 118-主材采购结算
-- 日期：2026-10-15
-- 适用：MySQL 8.0+。存量库执行前请先备份；空库部署请同步使用 01-空库基线.sql。
--
-- 依据（业务事实源：装得清业务智能体）：
--   · BR-045 采购价、无价下单与结算价成本口径规则（approved）
--   · DEC-072 第 1/3/4/5 项（暂估应付；成本悬挂期不设上限；成本偏差为正式能力；结算价不沉淀）
--   · DEC-073（逐单结算与月结并存；互斥；逐单优先；月结兜底）
--   · SOL-014 / SCN-021（主材下单作业单方案与场景）
--
-- 语义：
--   · 结算价 = 实际成本的唯一计价来源；确认结算后回填采购明细 settlement_price，
--     把暂估应付修正为确定金额（estimated=0、settled_amount），并把「结算价 − 采购价」差额补记到实际成本。
--   · **互斥**：同一采购明细只允许被一种结算方式计入一次 → 唯一约束 uk_settlement_item_purchase_item 兜底。
--
-- 回滚：见 docs/design/migrations/rollback/118-主材采购结算-rollback.sql

-- 1) 结算单
CREATE TABLE IF NOT EXISTS db_material_settlement (
  settlement_id     BIGINT        NOT NULL COMMENT '结算单索引',
  create_date       INT           NULL,
  create_date_time  BIGINT        NULL,
  create_day        INT           NULL,
  create_hour       INT           NULL,
  create_minute     INT           NULL,
  create_month      INT           NULL,
  create_second     INT           NULL,
  create_time       INT           NULL,
  create_week       TINYINT       NULL,
  create_year       INT           NULL,
  update_date       INT           NULL,
  update_date_time  BIGINT        NULL,
  update_day        INT           NULL,
  update_hour       INT           NULL,
  update_minute     INT           NULL,
  update_month      INT           NULL,
  update_second     INT           NULL,
  update_time       INT           NULL,
  update_week       TINYINT       NULL,
  update_year       INT           NULL,
  no                VARCHAR(64)   NOT NULL COMMENT '结算单号',
  scope             ENUM('Monthly','PerPurchaseOrder') NOT NULL COMMENT '结算方式',
  period            VARCHAR(32)   NULL COMMENT '结算期间（月结使用，如 2026-10）',
  settled_at        DATE          NULL COMMENT '结算日期',
  total_amount      DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '结算金额（Σ 结算价 × 数量）',
  status            ENUM('Cancelled','Confirmed') NOT NULL COMMENT '结算单状态',
  remark            VARCHAR(500)  NULL COMMENT '备注',
  com_id            BIGINT        NOT NULL COMMENT '所属企业',
  confirmer_id      BIGINT        NOT NULL COMMENT '确认人',
  owner_id          BIGINT        NULL COMMENT '经营负责人',
  project_id        BIGINT        NULL COMMENT '所属项目（月结跨多项目时为空）',
  store_id          BIGINT        NULL COMMENT '所属门店',
  supplier_id       BIGINT        NOT NULL COMMENT '供应商',
  team_id           BIGINT        NULL COMMENT '所属团队',
  PRIMARY KEY (settlement_id),
  UNIQUE KEY uk_settlement_company_no (com_id, no),
  KEY idx_settlement_company (com_id, settlement_id),
  KEY idx_settlement_company_supplier (com_id, supplier_id, settlement_id),
  KEY idx_settlement_company_scope_period (com_id, scope, period),
  KEY idx_settlement_company_store (com_id, store_id, settlement_id),
  KEY idx_settlement_company_team (com_id, team_id, settlement_id),
  KEY idx_settlement_company_owner (com_id, owner_id, settlement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主材采购结算单';

-- 2) 结算明细（互斥约束：同一采购明细只能结算一次）
CREATE TABLE IF NOT EXISTS db_material_settlement_item (
  settlement_item_id BIGINT        NOT NULL COMMENT '结算明细索引',
  create_date        INT           NULL,
  create_date_time   BIGINT        NULL,
  create_day         INT           NULL,
  create_hour        INT           NULL,
  create_minute      INT           NULL,
  create_month       INT           NULL,
  create_second      INT           NULL,
  create_time        INT           NULL,
  create_week        TINYINT       NULL,
  create_year        INT           NULL,
  update_date        INT           NULL,
  update_date_time   BIGINT        NULL,
  update_day         INT           NULL,
  update_hour        INT           NULL,
  update_minute      INT           NULL,
  update_month       INT           NULL,
  update_second      INT           NULL,
  update_time        INT           NULL,
  update_week        TINYINT       NULL,
  update_year        INT           NULL,
  quantity           DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '结算数量',
  settlement_price   DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '结算价（实际成本唯一计价来源）',
  amount             DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '结算金额',
  remark             VARCHAR(500)  NULL COMMENT '备注',
  com_id             BIGINT        NOT NULL COMMENT '所属企业',
  purchase_item_id   BIGINT        NOT NULL COMMENT '采购明细',
  settlement_id      BIGINT        NOT NULL COMMENT '所属结算单',
  PRIMARY KEY (settlement_item_id),
  UNIQUE KEY uk_settlement_item_purchase_item (com_id, purchase_item_id),
  KEY idx_settlement_item_company (com_id, settlement_item_id),
  KEY idx_settlement_item_company_settlement (com_id, settlement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主材采购结算明细';
