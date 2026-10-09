-- 99-设计分项验收标准长文本.sql
-- 目的：报价项目库 criterion 可能超过 512 字符，模版实例化写入 db_design_item.criterion 时会被截断报错。
-- 影响表：db_design_item。
-- 回滚：ALTER TABLE db_design_item MODIFY COLUMN criterion VARCHAR(512) NULL COMMENT '工艺说明';

ALTER TABLE db_design_item
    MODIFY COLUMN criterion LONGTEXT NULL COMMENT '工艺说明';
