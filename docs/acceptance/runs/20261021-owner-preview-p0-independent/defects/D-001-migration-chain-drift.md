# D-001 · 迁移链漂移阻断 123 正式执行

> 后续状态：`RESOLVED_IN_TEST_ENV_PROD_PENDING`。`decoboon-test` 已完成 111→123 修复、重复/回滚/validate、API 与 App 复验；dev 历史漂移保留，生产未触达。证据见 `../migration/24-test-repair-preflight.txt` 至 `../migration/48-final-app-verify.log`。

- 级别：P0（发布阻断）
- 环境：本地隔离克隆 `decoboon_owner_preview_acceptance`（MySQL 8.0.24）
- 复现：
  1. 从本地 dev 全量备份并克隆数据库。
  2. 克隆账本最大版本为 120。
  3. 执行 `run-migrations.sh.template apply 121,122,123`。
- 实际：
  - 121 在第 10 行失败：`ERROR 1060 Duplicate column name 'store_id'`。
  - 克隆中 `db_business_target.store_id` 已被历史 `ddl-auto=update` 提前创建，但 `target_scope_key` 与目标唯一索引不存在。
  - 账本将 121 标记为 `FAILED`，123 未通过正式链路执行。
- 影响：无法证明 123 可在目标库按现有 SOP 连续、可回滚地迁移；阻断发布。
- 证据：
  - `migration/02-apply-121-123.log`
  - `migration/03-121-failed-schema.txt`
  - `migration/10-environment-ledgers-final.txt`
- 交回：研发/运维修复 121 与账本/结构漂移后，从新备份重跑 121→123 正式链。
