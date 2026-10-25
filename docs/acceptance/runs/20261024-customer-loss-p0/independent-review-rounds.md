# 客户流失 P0 独立验收轮次

## Round 1 — BLOCK

- P0：空库基线后状态日志 `change_type` 仍为四值 ENUM，无法写 `Lost/RestoreLost`。
- P1：部分房屋流失无恢复入口；锁释放早于事务提交；requestId 重试不复用；500 字说明超过状态日志列宽；scope 非必填；客户 360 无单房流失。
- 结论：禁止生产迁移与发布。

## Round 2 — BLOCK

- 已确认迁移、scope、requestId、EntityGraph、部分恢复入口和锁后释放修复。
- 剩余 P1：事务在拿锁前已建立快照，Update 的一级缓存/REPEATABLE READ 仍可能读到旧状态。
- 结论：继续阻断。

## Round 3 — PASS

- Save/Update 改为：事务外获取 Redisson 锁 → TransactionTemplate 新事务 → 提交/回滚 → 释放锁。
- Update 锁前只做无状态定位，锁后在新事务中重读。
- 独立定向测试：29/29 PASS。
- P0：0；P1：0。
- P2 不影响代码放行：App 少量字段差异、前端枚举副本、历史页未接入、列表入口、证据归档等。
- 结论：代码独立验收 PASS。生产迁移、部署和正式发布仍需另行确认。
