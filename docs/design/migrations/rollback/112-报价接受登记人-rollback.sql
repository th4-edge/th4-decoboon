-- 112 回滚：报价接受登记人
-- 注意：accepted_by 为审计留痕列，回滚会丢失「谁置位了 Accepted」的历史。
ALTER TABLE quote DROP KEY idx_quote_com_accepted_by;
ALTER TABLE quote DROP COLUMN accepted_by;
