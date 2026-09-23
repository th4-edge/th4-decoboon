-- 58-设计方案状态闭环回填.sql
-- 依据用户 2026-09-23 反馈：报价已完成、合同已签约、项目已施工，业主端设计方案却一直显示“待谈单”。
-- 根因：设计方案状态只有设计师手动推进（Draft -> Ready -> Presented -> Selected），
--       客户接受报价、合同签约/生效、项目立项等商务主链动作没有同步回写设计方案状态。
-- 代码修复：新增 DesignLifecycleContext，在报价接受、合同签约/生效、项目立项时把
--           Ready/Presented 方案推进为 Selected（详见 DesignLifecycleClosureTest）。
-- 本迁移仅回填历史脏数据，幂等可重复执行；只推进 Ready/Presented，绝不改动 Draft/Selected/Archived。
-- 适用：MySQL 8.0+。

-- 1) 客户已接受或已转项目的报价，其关联设计方案必须进入“客户选中”。
UPDATE db_design d
JOIN quote q ON q.design_id = d.design_id
SET d.status = 'Selected'
WHERE q.status IN ('Accepted', 'Converted')
  AND d.status IN ('Ready', 'Presented');

-- 2) 已生成合同的历史报价（含直接签约补录），其关联设计方案必须进入“客户选中”。
UPDATE db_design d
JOIN quote q ON q.design_id = d.design_id
JOIN db_project_contract c ON c.quote_id = q.quote_id
SET d.status = 'Selected'
WHERE d.status IN ('Ready', 'Presented');

-- 3) 已立项施工的历史报价，其关联设计方案必须进入“客户选中”。
UPDATE db_design d
JOIN quote q ON q.design_id = d.design_id
JOIN db_project p ON p.quote_id = q.quote_id
SET d.status = 'Selected'
WHERE d.status IN ('Ready', 'Presented');
