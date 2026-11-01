# CHG-20261102-009 · 主材采购明细关联作业单行并修正到场状态

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261102-009 |
| 类型 | 缺陷 / 数据模型修复 |
| 风险级别 | L3 |
| 状态 | 待发布 |
| 负责人 | codex |
| 日期 | 2026-11-02 |
| 关联需求/事故/业务裁决 | `DEF-20261102-005`、`OBJ-008`、`BR-045`、`DEC-072`、`DEC-073`、`RUN-20261102-MAT-X2-001` |

## 2. 目标与范围

- 目标：采购明细必须能追溯到来源作业单行；部分到场只推进对应作业单，不混入同一材料计划下其他历史采购的到场量。
- 范围内：`db_purchase_order_item` 增加可空 `sheet_item_id`；采购下单写入关联；到场状态按该关联的采购明细累计推导；定向回归与隔离库迁移验证。
- 非范围：生产迁移、生产发布、App 身份伪造、异常退换、结算价口径调整。
- 受影响端：后端、数据库；Web/App 仅回读既有事实。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：是。
- 业务事实来源与裁决编号：`OBJ-008`、`BR-045`、`DEC-072`、`DEC-073`、`SCN-021`。
- 关键规则：1 张作业单 → N 张采购单；每张采购单必须可追溯到作业单及其行；到场量少于已下单量为 `PartiallyArrived`，达到后才为 `Arrived`；到场不新增应付。
- 尚存 GAP 或假设：历史采购明细无法唯一判定作业单行时保持空关联，必须出清单人工核对，禁止按时间或数量猜配。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | 同一 MaterialPlan 下已有历史采购20㎡，新作业单下单10㎡ | 新作业单 Submitted | 新采购明细关联新作业单行 | 定向单测 + DB |
| 2 | Pm 首批到场4㎡ | 新采购10㎡ | 新作业单 `PartiallyArrived`，采购 `PartiallyReceived(4/10)` | 页面 + Provider + DB |
| 3 | Pm 尾量到场6㎡ | 首批已登记 | 新作业单与采购均为全部到场 | 页面 + Provider + DB |
| 4 | 历史其他作业单 | 同 MaterialPlan 有20㎡已完成采购 | 不因本次4㎡被推进或混淆 | Provider + DB |
| 5 | 应付与成本 | 采购有价¥88/㎡，计划另有历史20㎡ | 新应付唯一¥880；计划累计实际成本=30×88=2640 | Provider + DB |
| 6 | 超量到场 | 剩余6㎡ | 7㎡拒绝且无部分写入 | 页面 + DB |
| 7 | App 回读 | 授权微信身份 | 与 Web/Provider 同源 | 身份不可用时 `BLOCKED_IDENTITY` |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | dev（本地） | 未提交；baseline `256cc587` + patch `hotfix-source-patch` | `PurchaseOrderItem`、`MaterialSheetContext`、goods receipt、migration 132 |
| th4-decoboon-web | 不修改 | `ec3da3b` | 仅页面复验 |
| th4-decoboon-app | 不修改 | `f2fb91e` | 身份可用后回读 |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| 定向单测 | `MaterialSheetOrderFlowTest`、`MaterialGoodsReceiptFlowTest`、`MigrationNumberingTest` | PASS 24/24 | `docs/acceptance/runs/20261102-mat-x2-arrival-hotfix/README.md` |
| 全量测试 | `mvn test`（origin/prod 449f086c + hotfix） | PASS 971/0/0/0 | `docs/acceptance/runs/20261102-mat-x2-arrival-hotfix/full-test.log` |
| 迁移编号 | `MigrationNumberingTest` | PASS | `33-targeted-tests.txt` |
| 隔离库迁移 | 备份 + `132` + 回滚演练 | PASS；回滚后列/索引为0 | `01-migration-132-applied.txt`、`02-migration-132-rollback-drill.txt` |
| 页面复验 | 首批4㎡→超量7㎡拒绝→尾量6㎡→刷新 | Web/Provider/DB PASS；App BLOCKED_IDENTITY | `RUN-20261102-MAT-X2F-RETEST-001` |

## 7. 发布

| 字段 | 内容 |
| --- | --- |
| Release ID | 不适用，尚未发布 |
| 制品路径/SHA-256 | 干净 baseline+补丁 bootJar `132a4cbc...`；build.tar `08a4b30...`；build.zip `17b9faf...` |
| 生产 release/版本 | 不适用 |
| 上一版本/回滚点 | 生产回滚点待发布前确认 |
| 健康检查 | 不适用 |
| 发布后观察 | 不适用 |

- 数据库迁移：`132-主材采购明细关联作业单行.sql`；生产执行前必须备份、隔离演练并取得用户确认。
- 配置/密钥变化：无。

## 8. 回滚

- 回滚条件：迁移或代码导致启动失败、采购/到场主链异常、历史数据不可读。
- 回滚命令/步骤：执行 `docs/design/migrations/rollback/132-主材采购明细关联作业单行-rollback.sql`，同步回退代码。
- 回滚验证：应用启动、采购/到场读取、作业单状态与应付回读。
- 实际是否回滚：隔离克隆库完成迁移→回滚演练；生产未执行。

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集。
- 人工耗时：未采集。
- 一次通过：否；首次修正确认后追加同一MaterialPlan累计成本修复，复验通过。
- 返工原因：`DEF-20261102-005` 暴露采购明细缺少作业单行来源。
- 最终结论：origin/prod 基线的代码、迁移、全量测试和本机构建已通过，待用户确认推送、生产迁移和部署。

## 10. 复盘与沉淀

- 是否出现重复问题：是，`DEC-073` 已要求采购可追溯作业单行，但实现只关联 MaterialPlan，形成跨单据状态误判。
- 是否需要新增门禁：是，补“同一 MaterialPlan 多作业单/历史采购”到场状态负向回归。
- 候选 `EXP/ANT/PATH`：到场状态只能按当前作业单行对应的采购明细累计，禁止按 MaterialPlan 全历史汇总。
- 后续动作与负责人：先收口132/133等未提交前置迁移形成干净发布基线并提交本补丁；处理2个既有statistic全量测试失败；App身份可用后补跨端回读；生产迁移/发布另行确认。
