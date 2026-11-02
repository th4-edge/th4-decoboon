-- 麦鸿装饰：删除已停用套餐“59800整装套餐 V1”
-- template_id=242847258880507904, com_id=1790066191585976744
-- 执行前已确认报价、报价行、材料计划、项目任务引用均为 0。
-- 回滚：/opt/backups/th4-decoboon/pre-delete-package-242847258880507904-20261102-142422/targeted-rows.sql

START TRANSACTION;

DELETE FROM pack_line_criterions
 WHERE item_id IN (
   SELECT item_id FROM pack_line
    WHERE template_id = 242847258880507904
      AND com_id = 1790066191585976744
 );

DELETE FROM pack_template_measures
 WHERE package_template_id = 242847258880507904;

DELETE FROM pack_template_criterions
 WHERE package_template_id = 242847258880507904;

DELETE FROM pack_line
 WHERE template_id = 242847258880507904
   AND com_id = 1790066191585976744;

DELETE FROM pack_template
 WHERE template_id = 242847258880507904
   AND com_id = 1790066191585976744
   AND name = '59800整装套餐'
   AND version = 1
   AND status = 'Disabled';

COMMIT;
