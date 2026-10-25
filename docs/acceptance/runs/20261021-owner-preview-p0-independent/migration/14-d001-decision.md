# D-001 结构审计结论与处置建议

## 本地审计

### dev `decoboon`

- 账本连续到 120
- `db_business_target.store_id` 已存在
- 缺少 `target_scope_key`
- 仍是旧唯一键 `uk_business_target_company_month(com_id,target_month)`
- 缺少 `idx_business_target_store`、`fk_business_target_store`
- 122 收款冲正结构不存在
- 123 预览审计表不存在

结论：121 属于历史 `ddl-auto=update` 造成的“部分结构已创建、账本未登记”漂移，不能直接按原 SQL 重放。

### test `decoboon-test`

- 账本连续到 110
- 121/122/123 均未登记
- `store_id` 等 121 结构也不存在

结论：test 不能直接作为 123 的目标验证环境，必须先补齐 111→122 的受控迁移链。

## 处置建议

1. 不修改已发布/已引用迁移文件的语义；先完成 DBA 结构审计。
2. dev：按审计结果补齐 121 缺失结构，再以受控方式登记 121 SUCCESS；随后执行 122、123。
3. test：从明确的基线恢复或按 111→122 顺序补齐，禁止只手工插入 123 账本。
4. prod：本次未连接、未审计、未修改；发布前必须另行按 L3 流程备份、审计和确认。
5. 修复后重跑：
   - 121→122→123 连续 SUCCESS
   - 123 重跑 SKIP
   - rollback 演练
   - JPA schema parity
   - 预览审计表与索引/外键核对

## 当前状态

D-001 仍为 **BLOCKED**。本轮没有修改 121/122/123 DDL，也没有修改原始 dev/test 账本。

## 后续执行结果（2026-10-21）

- `decoboon-test` 已按上述路径完成修复：
  - 111 结构审计后补账本
  - 112→123 正式执行，全部 SUCCESS
  - 123 重复执行 SKIP
  - 123 回滚后重放 PASS
  - `ddl-auto=validate` 启动 PASS
  - API 59/59、App 15/15 PASS
- `decoboon` 仍保留历史 121 部分漂移，未继续修复。
- 生产库未连接、未审计、未迁移。
