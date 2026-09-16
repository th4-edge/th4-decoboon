-- 供应商账期规则（供应链 P0-2）
--
-- 目的：应付到期日按供应商账期规则计算，支持两种口径（可逐供应商配置）：
--   1) ReceiptDays  —— 收货日 + 账期天数（默认 30 天）
--   2) MonthlyCutoff —— 对账月结：每月固定截账日（默认 25 日），收货日不超过截账日计入当月账期，
--                        否则计入次月，再顺延账期天数
--
-- 1) 供应商账期配置
ALTER TABLE db_decoboon_partner
  ADD COLUMN settlement_mode VARCHAR(16) NULL COMMENT '账期模式：ReceiptDays/MonthlyCutoff' AFTER settlement_terms,
  ADD COLUMN settlement_days INT NULL COMMENT '账期天数' AFTER settlement_mode,
  ADD COLUMN cutoff_day INT NULL COMMENT '对账截账日（每月几号）' AFTER settlement_days;

-- 2) 应付单：收货日（账期起算）+ 账期模式快照（固化，避免供应商改规则影响历史应付）
ALTER TABLE db_decoboon_finance_payable
  ADD COLUMN receipt_date DATE NULL COMMENT '收货日（账期起算）' AFTER paid_amount,
  ADD COLUMN settlement_mode VARCHAR(16) NULL COMMENT '账期模式快照' AFTER receipt_date;

-- 3) 存量数据建议：将已有 due_date 视为已固化到期日，receipt_date 可空；
--    如需回溯，可由业务确认后按「due_date 反推 receipt_date」批量补录（不建议自动执行）。
