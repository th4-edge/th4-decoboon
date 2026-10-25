# 迁移 123 验收证据

## 目标与环境

- 本机 MySQL：`127.0.0.1:3306`，MySQL `8.0.24`
- 原始 dev：`decoboon`，只读克隆源，未执行 121/122/123
- 目标 test：`decoboon-test`，已完成 111→123 修复与复验
- 正式演练目标（隔离克隆）：`decoboon_owner_preview_acceptance`
- SQL 独立冒烟库：`decoboon_owner_preview_migration_smoke`
- 未连接生产库

## 备份

备份文件已移出代码仓库，避免业务数据进入 Git；位置与校验见 `11-backup-location.txt`。

- dev 全量备份：`/Users/zhangwei/.codex/evidence-backups/20261021-owner-preview-p0/20261021-024605-owner-preview-acceptance-dev-decoboon-full.sql.gz`
- SHA256：`1e2823483bf714fdda0737c29ced4ad462c47f0ce555c925a1ae4f2a4d846bfd`
- 文件大小：307843 B
- SQL 冒烟库备份：`/Users/zhangwei/.codex/evidence-backups/20261021-owner-preview-p0/20261021-owner-preview-migration-smoke-before.sql.gz`
- SHA256：`27ceae3e49fb8cae7b024f5bf6bfe2ff047e7908884c53520eadaa452cde2416`

## 目标测试环境修复：PASS（2026-10-21 后续复验）

- 目标：`decoboon-test`
- 修复前全量备份：
  `/Users/zhangwei/.codex/evidence-backups/20261021-owner-preview-p0/20261021-033439-decoboon-test-before-111-ledger-112-123.sql.gz`
- SHA256：`c8f9ad8e369dcb3c9a29b6911c6b0d2665db26e214792e87fefce9d62a0a8d08`
- 审计：111 枚举结构已存在但账本缺失；112–122 关键结构均不存在
- 处置：111 结构审计后补账本；112→123 使用正式 `run-migrations.sh.template` 连续执行
- 结果：112、113、114、115、116、117、118、119、120、121、122、123 全部 SUCCESS
- 123 重复执行：SKIP
- 123 回滚：表删除；重放：SUCCESS，3 外键
- `ddl-auto=validate` 启动：PASS
- API：59/59 PASS
- App：15/15 PASS（五类入口、财务/业务员隐藏、三 Tab、固定水印）

证据：`24-test-repair-preflight.txt` 至 `48-final-app-verify.log`。

## 初轮正式链演练结果：BLOCKED（历史记录）

- 克隆账本最大版本：120
- 执行：`run-migrations.sh.template apply 121,122,123`
- 121 失败：`Duplicate column name 'store_id'`
- 原因：历史 `ddl-auto=update` 已提前创建 `store_id`，但缺 `target_scope_key` 和目标索引；账本没有对应 121 SUCCESS
- 账本结果：121 `FAILED`，123 未进入正式链路
- 备份恢复路径：使用上述 dev 全量 gzip 在隔离库重建

## SQL 独立冒烟：PASS

在最小隔离 schema（含 `db_schema_migration` 和三张 FK 目标空表）中：

1. `apply 123`：SUCCESS
2. 重复 `apply 123`：SKIP
3. 执行 rollback
4. 人工清除 smoke 库 123 账本行
5. 重放 `apply 123`：SUCCESS

已验证：

- 表 `db_project_owner_preview_log`
- 主键 `PRIMARY(log_id)`
- 4 个业务索引
- 3 个外键：company / project / employee
- 账本 SUCCESS、checksum、started_at、finished_at、actor

## 实际克隆兼容性：PASS（仅 SQL 兼容）

在 `decoboon_owner_preview_acceptance` 中直接执行 123 与回滚脚本：

- rollback 后表数：0
- 重放后表数：1
- 外键数：3
- 索引行数：14

该结果不等同于正式账本链通过。

## 隔离链修复验证（D-001 预演）

- 新隔离库：`decoboon_owner_preview_chain`
- 从 dev 全量备份重建
- 121 按结构审计做等价修复，并以 DBA 审计说明登记 SUCCESS
- 122、123 使用正式 `run-migrations.sh.template` 执行成功
- 123 重复执行：SKIP
- 123 回滚后重放：表 0 → 1，账本 SUCCESS
- `ddl-auto=validate` 启动：PASS

这只验证“修复后可恢复”的技术路径，不代表原始 dev/test 已修复。

## 关键证据

- `00-clone-preflight.txt`
- `sql/123-业主视角预览审计.sql`
- `sql/123-业主视角预览审计-rollback.sql`
- `12-ledger-123-snapshots.tsv`
- `01-clone-verification.txt`
- `02-apply-121-123.log`
- `03-121-failed-schema.txt`
- `04-api-sandbox-direct-123.txt`
- `05-sql-smoke-apply-1.log`
- `06-sql-smoke-repeat-skip.log`
- `07-sql-smoke-rollback-reapply.log`
- `08-sql-smoke-structure-ledger.txt`
- `09-actual-clone-rollback-reapply.txt`
- `10-environment-ledgers-final.txt`
