-- 回滚 111：把 db_project_stage.status 恢复为 4 值 ENUM。
-- ⚠️ 前置：若已有阶段处于 NotApplicable，直接收紧 ENUM 会截断/失败；本脚本先把这些行归回 Todo。
--    这一步会丢失「本项不涉及」判定（原因/判定人/时间三列保留，仅状态被覆盖），回滚前请评估。
update db_project_stage set status = 'Todo' where status = 'NotApplicable';

alter table db_project_stage
  modify column status enum('Accepting','Doing','Done','Todo') not null;
