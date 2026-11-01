# 材料计划 P3（合并下单 UI / 往来余额自动抵扣 / 入仓退货统一）交接单（development）

- 日期：2026-09-23
- Lane：development
- 依据：设计稿 v1.2 §5/§6 + DEC-MP-02 延伸、DEC-MP-03
- 状态：`implementation_done_self_tested_pending_independent_acceptance`

## 1. 交付范围

| 编号 | 内容 | 结果 |
| --- | --- | --- |
| P3-1 | 供应商往来余额**自动抵扣**新形成应付（按形成先后 FIFO） | 完成 |
| P3-2 | 入仓退货**统一入口**：退货单生成时按库存批次 FIFO 同步库存退回 | 完成 |
| P3-3 | 合并下单**多选 UI**：工作台多选 → 合并下单页（选供应商/交付方式/逐行数量单价）→ 确认下单 | 完成 |

## 2. 关键口径

**往来余额自动抵扣**
```
确认下单 → 形成应付
  → 查询该供应商往来余额（PartnerCredit）
  → 按形成先后（ID 升序）抵扣应付未付部分
  → 余额不足则全部抵扣；余额多余部分继续留存
  → 全程不修改历史付款流水
```

**入仓退货统一**
- 退货单创建后，若采购为"入仓库"且材料来自报价项目库，则按库存批次（stockId 升序）逐批退回库存；
- 直送工地材料未进库存，不产生库存流水；
- 退货单（`MaterialReturn`）同时是应付/成本冲减凭证与库存退回依据，避免两套入口口径不一。

**合并下单**
- 工作台"多选下单"进入多选态，仅可选可下单状态材料，且**必须同一项目**（跨项目选择会提示）；
- 合并下单页：选择供应商（供应商由财务决定，DEC-MP-03）→ 交付方式 → 逐行数量/单价（成本可见角色默认带出计划成本）→ 合计 → 确认下单；
- Boss/财务确认下单即形成应付（DEC-MP-01）；其他角色提交待审批。

## 3. 变更文件

后端：
- `finance/context/PartnerCreditContext.java`（新增：往来余额 FIFO 抵扣应付）
- `project/service/purchases/order/support/Order.java`（形成应付后自动抵扣往来余额）
- `project/context/MaterialDeductionContext.java`（新增 `postInventoryReturn`：入仓退货同步库存退回）
- 测试：`test/.../finance/PartnerCreditContextTest.java`（新增 3 例）、`MaterialDeductionContextTest`/`ContractChangeFulfillmentContextTest`（构造器同步）

App：
- `subpackages/project/pages/material-batch-order.vue`（新增合并下单页）
- `subpackages/project/pages/material-plan-workbench.vue`（新增多选态、勾选、底部"已选 N 项 / 合并下单"）
- `config/index.ts`、`pages.json`（新增 `materialBatchOrder` 路由）

## 4. 验证证据

| 项 | 命令 | 结果 |
| --- | --- | --- |
| 后端编译 | `mvn -o -DskipTests compile` | BUILD SUCCESS |
| 后端全量测试 | `mvn -o test -Dtest='!BossMeasureWorkspace2AcceptanceTest'` | 264 tests / 0 failures / 0 errors |
| 往来余额专项 | `PartnerCreditContextTest` | 3/0/0（FIFO 抵扣、余额留存、已付清跳过） |
| 扣减专项 | `MaterialDeductionContextTest` | 5/0/0 |
| App 类型检查 | `npm run type-check` | 通过 |
| App 构建 | `npm run build:mp-weixin` | DONE |
| App 分包检查 | `npm run check:subpackages` | 通过（主包 629,140 B，14 分包 / 92 页） |

## 5. 风险与已知问题

1. 迁移仍为 51 → 52 → 53 顺序，必须先于新后端启动执行。
2. 入仓退货按库存批次 FIFO 退回；若库存不足（已被领料），**只退可用部分**，剩余不产生库存流水（退货单仍全额冲减应付/成本）。需业务确认此口径。
3. 合并下单要求同一项目；跨项目合并采购未开放。
4. `PartnerCredit` 目前只被"新应付形成"触发抵扣，尚无财务手工抵扣入口。
5. 并发 Maven 构建仍会互相覆盖 `target/classes` 造成伪失败（ECD-010），重跑即恢复；本批已遇到一次。

## 6. next_action

1. 目标环境执行迁移 51→52→53 → 部署后端 → 回归合并下单、退货、往来余额抵扣、主材分类。
2. acceptance lane 独立验收：合并下单（多选跨项目拒绝、Boss/财务直接确认、明细生成）、往来余额抵扣（FIFO、余额留存、已付清跳过）、入仓退货（库存退回 + 应付冲减一致）。
3. 待确认口径：入仓退货库存不足时是否允许"部分退库 + 全额冲减应付"。
