# 材料计划 P1（扣减与退货冲减）交接单（development）

- 日期：2026-09-23
- Lane：development
- 依据：`docs/product/2026-09-23-材料计划业务梳理与设计.md` v1.2 §4.2 / §12 / DEC-MP-02、DEC-MP-10
- 状态：`implementation_done_self_tested_pending_independent_acceptance`

## 1. 交付范围

| 编号 | 内容 | 结果 |
| --- | --- | --- |
| P1-1 | 未下单扣减：直接冲减计划数量，**先冲损耗量再冲净用量**，归零则计划行取消 | 完成 |
| P1-2 | 已下单未到货扣减：调整采购单数量与金额，按差额**下调应付** | 完成 |
| P1-3 | 已到货扣减：**强制走退货单**（DEC-MP-02），生成 `MaterialReturn` 并冲减应付与已确认成本 | 完成 |
| P1-4 | 已付款部分不可直接冲减 → 退货单转 `Pending`（待协商）并记录说明 | 完成 |
| P1-5 | 扣减后计划行状态按下单进度重算（无采购→可下单 / 未覆盖→部分下单 / 已覆盖→已下单 / 归零→已取消） | 完成 |
| P1-6 | `material/return/page` 退货单查询 + App 工作台"退货冲减"区展示 | 完成 |
| P1-7 | 迁移 52 + 空库基线同步 | 完成 |

## 2. 扣减判定顺序（实现口径）

```
扣减需求 quantity
 ├─ 1) 逐张采购单：已到货部分 → 生成退货单 → 冲减应付与计划实际成本 → 采购单到场数量回退
 ├─ 2) 逐张采购单：已下单未到货部分 → 采购单数量/金额下调 → 差额下调应付
 └─ 3) 剩余部分（未下单） → 计划行先冲损耗量、再冲净用量 → 归零则 Cancelled
```

触发入口：
- 增减项 `ContractChangeType.Remove`（主材）→ 对原材料计划行按当前需求数量全额扣减；
- 增减项 `ContractChangeType.Quantity`（主材）且新数量 < 当前数量 → 按差额扣减；
- 数量增加 → 直接增加到净用量并重算需求量。

## 3. 变更文件

后端（`src/main/java/com/th4/edge/decoboon`）：
- `project/configure/MaterialReturnStatus.java`（新增）
- `project/model/MaterialReturn.java`（新增，退货单，项目归属 + `@DataPermission`）
- `project/repository/MaterialReturnRepository.java`（新增）
- `project/context/MaterialDeductionContext.java`（新增：三段式扣减编排 + 应付冲减 + 成本回冲）
- `project/context/ContractChangeFulfillmentContext.java`（Remove / Quantity 减少接入扣减流程）
- `material/service/returnorder/page/**`（新增，路由 `material/return/page`）

迁移与基线：
- `docs/design/migrations/52-材料退货单.sql`（新增，含回滚注释）
- `docs/design/migrations/01-空库基线.sql`（用 `generate-baseline.sh.template` 重新生成）

App（`th4-decoboon-app/src`）：
- `domains/material/model.ts`（新增 `MaterialReturnRecord`）
- `domains/material/api.ts`（新增 `materialReturnApi.list`）
- `subpackages/project/pages/material-plan-workbench.vue`（Boss/财务可见"退货冲减"区）

测试：
- 新增 `test/.../project/context/MaterialDeductionContextTest.java`（5 例：先冲损耗、全部冲减取消、已到货退货冲减、已付款转待协商、已下单未到货调整采购单）
- 更新 `ContractChangeFulfillmentContextTest`（Remove 现走扣减流程，断言由 `Closed` 改为 `Cancelled` + 数量归零）
- 更新 `ProviderSecurityAcceptanceTest`（路由 287→288，protected 275→276）

## 4. 金额口径（强制）

| 场景 | 应付 | 实际成本 |
| --- | --- | --- |
| 未下单扣减 | 无应付，不涉及 | 不涉及 |
| 已下单未到货 | 按差额下调应付金额 | 尚未确认，不涉及 |
| 已到货退货（未付款） | 应付金额下调冲减 | 计划实际成本回冲 |
| 已到货退货（已付款） | **不改应付**，退货单转"待协商"并留说明 | 计划实际成本回冲 |
| 施工/领料损耗 | 不涉及 | 只流转库存，不重复累计（DEC-MP-04） |

## 5. 验证证据

| 项 | 命令 | 结果 |
| --- | --- | --- |
| 后端编译 | `mvn -o -DskipTests compile` | BUILD SUCCESS |
| 后端全量测试 | `mvn -o test -Dtest='!BossMeasureWorkspace2AcceptanceTest'` | 252 tests / 0 failures / 0 errors |
| 扣减专项 | `MaterialDeductionContextTest` | 5/0/0 |
| 变更履约 | `ContractChangeFulfillmentContextTest` | 4/0/0 |
| 路由与安全 | `ProviderSecurityAcceptanceTest` | 3/0/0，路由 288（public 12 + protected 276） |
| App 类型检查 | `npm run type-check` | 通过 |
| App 构建 | `npm run build:mp-weixin` | DONE |
| App 分包检查 | `npm run check:subpackages` | 通过（主包 628,475 B / 90 页） |

## 6. 风险与已知问题

1. **迁移 52 必须在新后端启动前执行**（prod 为 `validate`）。
2. `MaterialReturn` 由变更确认流程自动生成并直接置为 `Confirmed`；**财务线下确认退货实际发生后的人工确认/驳回尚未提供 Provider**（下一批）。
3. 已付款部分的差额目前只转"待协商"标记，未实现"冲抵其他应付 / 转供应商往来余额"（设计 §12 已列为规则，待实现）。
4. 直送工地的材料未进入库存，因此退货只冲减应付与计划成本，不回写库存流水；入仓材料的库存退货仍走既有 `stock/supplier-return`（两者后续需统一入口）。
5. 未新增"负向扣减计划行"实体：`MaterialSelectionContext.listProjectItems` 以 `contractLine` + `putIfAbsent` 关联计划行，新增同合同行的扣减标记行会遮蔽原计划，故扣减审计由**变更单行 + 退货单 + 采购单调整**承载。
6. `BossMeasureWorkspace2AcceptanceTest`（另一会话未跟踪用例）仍为 2 条量房断言失败，与本批无关。

## 7. next_action

1. 目标环境执行迁移 52 → 部署后端 → 回归减项与退货。
2. acceptance lane 补充独立验收：AC-06（未下单减项）、AC-07（已下单减项调整应付）、AC-08（已到货强制退货单）、AC-18（退货冲减），并覆盖已付款待协商、重复确认幂等、跨企业越权。
3. development 下一批：退货单人工确认/驳回 Provider、"冲抵其他应付/供应商往来"、`PurchaseOrderItem` 多供应商拆单、主材分类损耗率继承。
