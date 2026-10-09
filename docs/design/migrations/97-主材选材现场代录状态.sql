-- 97-主材选材现场代录状态.sql
-- 依据 DEC-056 / BR-009（2026-10-09 用户裁决）：
--   主材选材由「草稿/待客户确认/已确认」收敛为「已选材（现场确认）」单一状态；保存即已选材。
--   采购门禁判定载体改写，但门禁本身不变（进入采购后锁定，换材走增减项）。
--
-- 背景：46 号迁移声明 status 为 varchar(24)，但部分环境经 ddl-auto 落成
--   enum('Confirmed','Draft','PendingConfirm')，写入 'Selected' 会 Data truncated。此处对齐为 varchar。
--
-- 存量数据：用户 2026-10-09 明确「目前不存在，如果存在就删除重做」。
--   因此本脚本不迁移旧状态语义，直接删除旧状态记录（旧值仅 Draft/PendingConfirm/Confirmed）。
--   执行前必须备份 db_material_selection 与引用它的 db_material_plan.selection_id。
--
-- 回滚：用备份恢复 db_material_selection 行；如需恢复旧枚举：
--   alter table db_material_selection modify status enum('Confirmed','Draft','PendingConfirm') not null;

alter table db_material_selection modify column status varchar(24) not null;

-- 解除材料计划对旧状态选材的外键引用（selection_id 可空）
update db_material_plan p
  join db_material_selection s on s.selection_id = p.selection_id
  set p.selection_id = null
  where s.status in ('Draft', 'PendingConfirm', 'Confirmed');

delete a from db_material_selection_attachments a
  join db_material_selection s on s.selection_id = a.selection_id
  where s.status in ('Draft', 'PendingConfirm', 'Confirmed');

delete from db_material_selection
  where status in ('Draft', 'PendingConfirm', 'Confirmed');
