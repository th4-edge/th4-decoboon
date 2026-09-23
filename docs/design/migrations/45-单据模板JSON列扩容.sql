-- 45-单据模板JSON列扩容.sql
--
-- 背景：db_document_template.field_config / style_config 在部分环境由 Hibernate
-- `ddl-auto: update` 早期建表生成为 varchar(255)；`update` 不会修改既有列类型，
-- 因此凭证模板字段变多后保存会报
--   Data truncation: Data too long for column 'field_config' at row 1
-- 实体侧已是 @Lob（新库为 longtext），本迁移用于修正既有库，可重复执行。
--
-- 建议执行前先确认当前列类型：
--   show create table db_document_template;
-- 执行后应看到 field_config / style_config 为 longtext。

alter table db_document_template
    modify column field_config longtext not null;

alter table db_document_template
    modify column style_config longtext not null;
