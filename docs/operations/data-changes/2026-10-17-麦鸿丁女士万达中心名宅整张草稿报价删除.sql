-- 2026-10-17 删除麦鸿丁女士万达中心名宅整张草稿报价
-- quote_id=237069407250022400 / design_id=237046223691841536
-- 执行前已确认：Draft、Custom、无项目/合同等下游引用。
-- 行级回滚备份：
-- /opt/backups/th4-decoboon/quote-delete-20261017-134159-maihong-ding-wanda

START TRANSACTION;

DELETE FROM db_notice
 WHERE quote_id = 237069407250022400;

DELETE qlc
  FROM quote_line_components qlc
  JOIN quote_line ql ON qlc.line_id = ql.item_id
 WHERE ql.quote_id = 237069407250022400;

DELETE FROM quote_line
 WHERE quote_id = 237069407250022400;

DELETE FROM quote
 WHERE quote_id = 237069407250022400
   AND status = 'Draft'
   AND design_id = 237046223691841536;

COMMIT;
