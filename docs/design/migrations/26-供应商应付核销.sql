-- 供应商应付核销（供应链 P0 / 数据模型）
--
-- 背景：一笔付款需要能冲抵同一供应商的多张应付单；付款与应付因此是多对多关系，
-- 由 db_finance_payable_settlement 记录每次核销的金额与去向。
-- 现状：db_finance_payable 已具备 payee_partner_id（供应商）与 due_date（到期日），本脚本不再重复新增。

-- 1) 付款关联合作方（供应商）
ALTER TABLE db_finance_payment
  ADD COLUMN partner_id BIGINT NULL COMMENT '合作方（供应商）' AFTER payable_id;

ALTER TABLE db_finance_payment
  ADD KEY idx_fin_payment_partner (com_id, partner_id, payment_id);

-- 2) 应付款核销明细表
CREATE TABLE IF NOT EXISTS db_finance_payable_settlement (
  settlement_id BIGINT       NOT NULL COMMENT '核销索引',
  com_id        BIGINT       NOT NULL COMMENT '所属企业',
  store_id      BIGINT       NULL COMMENT '所属门店',
  team_id       BIGINT       NULL COMMENT '所属团队',
  owner_id      BIGINT       NULL COMMENT '负责人',
  payment_id    BIGINT       NOT NULL COMMENT '付款单',
  payable_id    BIGINT       NOT NULL COMMENT '应付单',
  amount        DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '核销金额（元）',
  remark        VARCHAR(255) NULL COMMENT '备注',
  create_date        INT      NULL,
  create_date_time   BIGINT   NULL,
  create_day         INT      NULL,
  create_hour        INT      NULL,
  create_minute      INT      NULL,
  create_month       INT      NULL,
  create_second      INT      NULL,
  create_time        INT      NULL,
  create_week        TINYINT  NULL,
  create_year        INT      NULL,
  update_date        INT      NULL,
  update_date_time   BIGINT   NULL,
  update_day         INT      NULL,
  update_hour        INT      NULL,
  update_minute      INT      NULL,
  update_month       INT      NULL,
  update_second      INT      NULL,
  update_time        INT      NULL,
  update_week        TINYINT  NULL,
  update_year        INT      NULL,
  PRIMARY KEY (settlement_id),
  UNIQUE KEY uk_payable_settle_payment_payable (com_id, payment_id, payable_id),
  KEY idx_settle_company (com_id, settlement_id),
  KEY idx_settle_company_payment (com_id, payment_id, settlement_id),
  KEY idx_settle_company_payable (com_id, payable_id, settlement_id),
  KEY idx_settle_company_store (com_id, store_id, settlement_id),
  KEY idx_settle_company_team (com_id, team_id, settlement_id),
  KEY idx_settle_company_owner (com_id, owner_id, settlement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应付款核销明细';

-- 3) 下一步（账期规则，属 P0 第二小步，届时执行）
--   ALTER TABLE db_finance_payable ADD COLUMN receipt_date DATE NULL, ADD COLUMN settlement_mode VARCHAR(16) NULL;
--   ALTER TABLE db_partner ADD COLUMN settlement_mode VARCHAR(16) NULL, ADD COLUMN cutoff_day INT NULL;
