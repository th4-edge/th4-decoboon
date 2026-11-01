# 材料计划 P0 交接单（development）

- 日期：2026-09-23
- Lane：development
- 依据：`docs/product/2026-09-23-材料计划业务梳理与设计.md`（v1.2，DEC-MP-01～10 已裁决）
- 状态：`implementation_done_self_tested_pending_independent_acceptance`

## 1. 本批交付范围（P0）

| 编号 | 内容 | 结果 |
| --- | --- | --- |
| P0-1 | `MaterialPlan` 增字段（来源、选材快照、损耗、扣减引用）+ 状态口径扩展 | 完成 |
| P0-2 | 选材确认 → 计划行 `ReadyToOrder` + 下单门禁（DEC-MP-05） | 完成 |
| P0-3 | `MaterialSupplier` 材料-供应商供应关系 + 匹配 Provider（DEC-MP-03/08） | 完成 |
| P0-4 | 下单即形成应付、到货回填账期（DEC-MP-01） | 完成 |
| P0-5 | 到场确认限定 PM/设计负责人任一方（DEC-MP-07） | 完成 |
| P0-6 | 材料计划工作台 Provider + App 页面（按项目聚合、分组） | 完成 |
| P0-7 | 迁移 51 + 空库基线同步 | 完成 |

未纳入本批（下一批 P1/P2）：
- 采购单明细行 `PurchaseOrderItem`（多材料按供应商合并下单，一条材料拆多供应商）
- 增减项负向扣减计划 + 已下单采购调整 + 已到货退货冲减（DEC-MP-02 已裁决，待实现）
- 主材分类主数据与损耗率继承（DEC-MP-09；本批已留 `lossRate/lossQuantity` 字段与口径，分类主数据待补）
- 施工损耗登记的库存联动回归

## 2. 变更文件

后端（`src/main/java/com/th4/edge/decoboon`）：
- `project/configure/MaterialSourceType.java`（新增）
- `project/configure/MaterialPlanStatus.java`（新增 `ReadyToOrder/PartiallyOrdered/Cancelled`）
- `project/model/MaterialPlan.java`（新增 sourceType/selection/selectedBrand/selectedSpec/selectedModel/materialKey/netQuantity/lossRate/lossQuantity/reduceOfPlan/remark）
- `project/context/MaterialPlanContext.java`（新增：材料标识归一化 + 需求量口径）
- `project/context/ProjectContext.java`（立项生成计划时写入来源/选材快照/净用量并派生）
- `project/context/ContractChangeFulfillmentContext.java`（变更材料计划写来源与派生；可调整状态含 ReadyToOrder）
- `material/service/selections/confirm/support/Confirm.java`（确认后推进计划行 ReadyToOrder 并回填选材结果）
- `project/service/purchases/save/support/Save.java`（选材确认门禁）
- `project/service/purchases/order/support/Order.java`（确认下单形成应付，幂等）
- `project/service/goodsreceipts/save/support/Save.java`（到场角色门禁 + 应付账期回填，不再重复建应付）
- `finance/model/Payable.java`（新增 purchaseOrder 关联）
- `finance/repository/PayableRepository.java`（按采购单/来源单号幂等查询）
- `supply/model/MaterialSupplier.java`（新增）
- `supply/repository/MaterialSupplierRepository.java`（新增）
- `supply/service/materialsupplier/{page,save,match}/**`（新增，路由 `supply/material-supplier/page|save|match`）
- `material/model/MaterialPlanWorkbench(Item).java`（新增）
- `material/context/MaterialPlanWorkbenchContext.java`（新增）
- `material/service/plan/workbench/**`（新增，路由 `material/plan/workbench`）

迁移与基线：
- `docs/design/migrations/51-材料计划与供应商下单.sql`（新增，含回滚注释）
- `docs/design/migrations/01-空库基线.sql`（用仓库 `generate-baseline.sh.template` 重新生成）

App（`th4-decoboon-app/src`）：
- `domains/material/model.ts`（工作台/供应关系类型、状态枚举扩展）
- `domains/material/api.ts`（状态映射扩展、`materialPlanWorkbenchApi`、`materialSupplierApi`）
- `subpackages/project/pages/material-plan-workbench.vue`（新增工作台页面）
- `subpackages/project/pages/material-plan.vue`（状态展示补 readyToOrder/partiallyOrdered）
- `subpackages/supply/pages/purchase-edit.vue`（可下单状态含 readyToOrder；Boss/财务创建即确认下单）
- `subpackages/supply/pages/index.vue`（新增"材料计划工作台"入口）
- `config/index.ts`、`pages.json`（新增 `materialPlanWorkbench` 路由）

测试调整（构造器/契约变更导致，必要同步）：
- `test/.../project/context/ContractChangeFulfillmentContextTest.java`
- `test/.../material/MaterialSelectionFlowTest.java`
- `test/.../project/PurchaseCreationPolicyTest.java`
- `test/.../project/PurchaseDeliveryModeTest.java`（fixture 补 `project.setManager`，因到场角色门禁）
- `test/.../security/ProviderSecurityAcceptanceTest.java`（路由 283→287，protected 271→275）

## 3. 验证证据

| 项 | 命令 | 结果 |
| --- | --- | --- |
| 后端编译 | `mvn -o -DskipTests compile` | BUILD SUCCESS |
| 后端全量测试 | `mvn -o test -Dtest='!BossMeasureWorkspace2AcceptanceTest'` | 247 tests / 0 failures / 0 errors |
| 路由与安全 | `ProviderSecurityAcceptanceTest` | 3/0/0，路由 287（public 12 + protected 275） |
| 材料/采购相关 | `ContractChangeFulfillmentContextTest`、`MaterialSelectionFlowTest`、`PurchaseCreationPolicyTest`、`PurchaseDeliveryModeTest` | 4+7+3+4 全通过 |
| App 类型检查 | `npm run type-check` | 通过 |
| App 构建 | `npm run build:mp-weixin` | DONE，构建成功 |
| App 分包检查 | `npm run check:subpackages` | 通过（主包 627,943 B / 90 页） |

## 4. 风险与已知问题

1. **迁移 51 必须先执行再启动新后端**（prod 为 `ddl-auto: validate`）。
2. **空库基线已整体重新生成**：会把其他在途会话缺失的表（验收轮次、付款计划、单据模板、凭证、工人、选材等）一并纳入。基线执行后的 `db_schema_migration` 标记口径需随之复核。
3. `BossMeasureWorkspace2AcceptanceTest`（另一会话新增的未跟踪用例）2 个量房日期窗口断言失败，与本批材料改动无关；已排除后全量 247 通过。
4. 应付口径变化：历史采购单若无应付，到场时按采购金额补建（兼容路径），避免重复应付（同时按 `purchase_order_id` 与 `source_no` 幂等）。
5. `PurchaseOrderItem` 未落地，当前仍是"一采购单一条材料"；按供应商合并下单与一条材料多供应商拆分待 P1。
6. 扣减项仍沿用原逻辑（未下单可直接改/关闭）；`ChangeReduce` 负向计划与退货冲减待 P1（DEC-MP-02 约束已写入设计稿）。
7. 损耗率 `lossRate` 字段与口径已落地，但"主材分类维护损耗率并自动继承"待 P1 实现；当前默认 0，可由财务在下单前调整。

## 5. 下一步（next_action）

1. 目标环境按序执行迁移 51 → 部署后端 → 回归材料计划工作台。
2. acceptance lane 按设计稿 §13 的 AC-01～AC-23 做独立验收（重点 AC-01 选材门禁、AC-02 下单生成应付、AC-05 到货回填账期、AC-09 到场角色、AC-11 越权、AC-16～19 损耗）。
3. development 下一批：`PurchaseOrderItem` + 增减项扣减/退货冲减 + 主材分类损耗率。
