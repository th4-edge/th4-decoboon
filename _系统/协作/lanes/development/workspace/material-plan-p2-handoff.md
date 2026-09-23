# 材料计划 P2（冲抵其他应付 / 采购明细拆单 / 主材分类损耗率）交接单（development）

- 日期：2026-09-23
- Lane：development
- 依据：设计稿 v1.2 §6/§9 + DEC-MP-02 延伸、DEC-MP-03、DEC-MP-09
- 状态：`implementation_done_self_tested_pending_independent_acceptance`

## 1. 交付范围

| 编号 | 内容 | 结果 |
| --- | --- | --- |
| P2-1 | 退货余款**冲抵同一供应商其他未结清应付**（按到期日先后） | 完成 |
| P2-2 | 冲抵后仍有余额 → 记入**供应商往来余额**（`PartnerCredit`），不改历史付款流水 | 完成 |
| P2-3 | `PurchaseOrderItem` 采购明细：一张采购单一个供应商、可按供应商合并多条材料 | 完成 |
| P2-4 | 同一条材料可拆给多个供应商（多条明细承载），计划进度由明细实时汇总 | 完成 |
| P2-5 | `project/purchase/batch` 合并下单 Provider（按供应商一次提交多条材料） | 完成 |
| P2-6 | 到场验收按采购明细登记（单明细自动匹配；多明细必须指定 `purchaseItemId`） | 完成 |
| P2-7 | 主材分类 `MaterialCategory` 主数据（默认损耗率），计划行按分类继承 | 完成 |
| P2-8 | 计划行状态统一由 `MaterialPlanStatusContext` 派生（禁用人工写入） | 完成 |
| P2-9 | 迁移 53 + 空库基线同步 | 完成 |

## 2. 关键口径

**退货余款处理顺序（新增）**
```
退货金额
 ├─ 1) 冲减本采购单应付（未付款部分）
 ├─ 2) 剩余 → 冲抵同一供应商其他未结清应付（按到期日先后）
 └─ 3) 仍未冲完 → 记入供应商往来余额（PartnerCredit，供后续抵扣）
 已付款部分不改历史付款流水
```

**采购明细口径**
- 采购单头按供应商聚合，数量/金额/到场量 = 明细汇总（`recomputeHeader`）；
- 计划行下单/到场进度优先取采购明细，无明细时回退采购单单行（历史数据兼容）；
- 迁移 53 会把存量采购单补一条 1:1 明细，保证新旧口径一致。

**主材分类损耗率（DEC-MP-09）**
- 企业维护主材分类（如"瓷砖"）并设置默认损耗率（百分比，默认 0，**不预置行业比例**）；
- 立项/变更生成材料计划行时按 `ContractLine.category`（或 `CatalogItem.category`）继承；
- `quantity = netQuantity + lossQuantity`，`lossQuantity = netQuantity × lossRate%`。

## 3. 变更文件

后端（`src/main/java/com/th4/edge/decoboon`）：
- `project/model/PurchaseOrderItem.java`、`project/repository/PurchaseOrderItemRepository.java`（新增）
- `project/service/purchases/batch/**`（新增，路由 `project/purchase/batch`）
- `project/context/MaterialPlanStatusContext.java`（新增，状态派生统一口径）
- `project/context/MaterialDeductionContext.java`（重写：明细优先扣减 + 冲抵其他应付 + 往来余额）
- `project/model/MaterialReturn.java`（新增）、`project/repository/MaterialReturnRepository.java`
- `project/context/ContractChangeFulfillmentContext.java`（接入分类损耗率）
- `project/model/GoodsReceipt.java`（新增 `purchaseItem`）、`project/service/goodsreceipts/save/**`（明细化到场）
- `project/service/purchases/save/**`（创建 1:1 明细）、`project/service/purchases/order/**`（明细同步下单）
- `project/context/ProjectContext.java`、`project/context/MaterialPlanContext.java`（分类损耗继承）
- `material/model/MaterialCategory.java`、`material/repository/MaterialCategoryRepository.java`、`material/context/MaterialCategoryContext.java`（新增）
- `material/service/category/{page,save}/**`（新增，路由 `material/category/page|save`）
- `material/context/MaterialPlanWorkbenchContext.java`（明细优先汇总）
- `finance/model/PartnerCredit.java`、`finance/repository/PartnerCreditRepository.java`（新增）

迁移与基线：
- `docs/design/migrations/53-采购明细与主材分类.sql`（新增：`db_purchase_order_item` + 存量补明细、`db_material_category`、`db_finance_partner_credit`、`db_goods_receipt.purchase_item_id`）
- `docs/design/migrations/51-材料计划与供应商下单.sql`、`52-材料退货单.sql`（同批已交付）
- `docs/design/migrations/01-空库基线.sql`（重新生成）

App（`th4-decoboon-app/src`）：
- `domains/material/model.ts`（`MaterialCategory`）
- `domains/material/api.ts`（`materialCategoryApi`、`materialBatchPurchaseApi`）
- `subpackages/project/pages/material-category.vue`（新增主材分类与损耗率维护页）
- `subpackages/project/pages/material-plan-workbench.vue`（"主材分类损耗"入口）
- `config/index.ts`、`pages.json`（新增 `materialCategory` 路由）

测试：
- `MaterialDeductionContextTest` 扩到 5 例（含"已付清 → 冲抵其他应付 + 转往来余额"）
- 新增/更新 `ContractChangeFulfillmentContextTest`、`PurchaseDeliveryModeTest`、`PurchaseCreationPolicyTest`、`MaterialSelectionFlowTest`、`ProviderSecurityAcceptanceTest`（路由 288→293，protected 276→281）

## 4. 验证证据

| 项 | 命令 | 结果 |
| --- | --- | --- |
| 后端编译 | `mvn -o -DskipTests compile` | BUILD SUCCESS |
| 后端全量测试 | `mvn -o test -Dtest='!BossMeasureWorkspace2AcceptanceTest'` | 258 tests / 0 failures / 0 errors |
| 扣减专项 | `MaterialDeductionContextTest` | 5/0/0 |
| 变更履约 | `ContractChangeFulfillmentContextTest` | 4/0/0 |
| 交付方式 | `PurchaseDeliveryModeTest` | 4/0/0 |
| 路由与安全 | `ProviderSecurityAcceptanceTest` | 3/0/0，路由 293（public 12 + protected 281） |
| App 类型检查 | `npm run type-check` | 通过 |
| App 构建 | `npm run build:mp-weixin` | DONE |
| App 分包检查 | `npm run check:subpackages` | 通过（主包 628,941 B，14 分包 / 91 页） |

## 5. 风险与已知问题

1. **迁移 53 必须在新后端启动前执行**；迁移包含存量数据补明细（幂等，`WHERE NOT EXISTS`）。
2. 迁移 51/52/53 需按序执行；空库基线已整体重新生成（含其他在途会话此前缺失的表）。
3. **合并下单的多选 UI 尚未接入 App**：`project/purchase/batch` 与 `materialBatchPurchaseApi` 已就绪，工作台当前仍是逐条下单入口；下一批补多选 + 选择供应商的合并下单交互。
4. 供应商往来余额目前**只记录不自动抵扣**（后续采购/付款时的抵扣动作待实现）。
5. 主材分类按"名称"与 `ContractLine.category` 匹配；若企业分类名与合同分类名不一致，需要人工在分类页按合同用词命名，或后续补映射。
6. 入仓材料的退货仍走既有 `stock/supplier-return`，与新的 `MaterialReturn` 未统一入口。
7. `BossMeasureWorkspace2AcceptanceTest`（另一会话未跟踪用例）仍为量房断言失败，与本批无关。
8. 并发 Maven 构建期间出现过 `target/classes` 被其他会话覆盖导致的伪失败（ECD-010 同源），重跑即恢复。

## 6. next_action

1. 目标环境按 51 → 52 → 53 顺序执行迁移 → 部署后端 → 回归材料计划、退货、合并下单、主材分类。
2. acceptance lane 独立验收：AC-16/17（损耗继承与成本）、AC-18（退货单冲减）、新增用例覆盖"冲抵其他应付 / 往来余额 / 合并下单 / 一条材料拆多供应商 / 多明细到场指定"。
3. development 下一批：合并下单多选 UI、供应商往来余额自动抵扣、入仓退货入口统一。
