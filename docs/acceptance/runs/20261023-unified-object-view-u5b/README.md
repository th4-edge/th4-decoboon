# 统一对象视图一期 U5-B1 · 项目事实当前聚合开发证据

| 项 | 内容 |
| --- | --- |
| 问题 | `PI-20261022-010` 统一对象视图一期实施 |
| 切片 | `U5-B1` 当前项目事实聚合与 `project/project/info` 接线 |
| 日期 | 2026-10-23 |
| 状态 | `DEV_VERIFIED / ACCEPTANCE_PENDING` |
| 边界 | 只做后端当前事实索引；不改 App/Web，不做历史版本折叠和利润/结算节点 |
| 隔离目录 | `/tmp/u5b2.ABonH4` |

## 一、实现结果

新增 `ProjectFactTraceContext`：

- 来源：客户、房屋、项目来源；
- 售前：最近已完成量房、绑定设计、绑定报价；
- 签约：报价接受、合同；
- 履约：项目、阶段、任务、日报、主材选材、材料计划、采购、到货；
- 变更：合同变更；
- 验收：阶段验收；
- 财务：应收、收款；BOSS/店长/财务额外包含应付、付款、成本；
- 售后：实际完工交付、业主意见。

已接入：

- `project/project/info` 返回瞬态 `Project.factTrace`；
- Provider 属性白名单包含 `ProjectFactTrace`、`BusinessFactNode`、来源缺口；
- 成本节点仅在 BOSS/店长/财务投影中生成。

## 二、冻结点

- 未经证实的事实不生成占位对象，只进入 `sourceGaps`；
- 量房只取 `Completed`，按完成时间倒序、measureId 倒序；
- 设计和报价沿项目/合同绑定对象读取；
- 承接来源固定显示“项目承接（接管基线）”；
- 事实节点保留原对象权威金额/数量/进度/成本完整性；
- 节点统一 `projectId`、版本、来源、导航和返回锚点；
- 有原始事实时，不伪造空节点或金额 0。

## 三、验证

```text
ProjectFactTraceContractTest
ProjectFactTraceContextTest
ProjectFactTracePropertiesTest
BusinessFactContractTest
合计：12/12 PASS，0 failures / 0 errors / 0 skipped

JDK 22 package：PASS
```

覆盖：

- 当前来源/售前/签约/履约/验收/财务/售后节点；
- 缺失量房/设计进入 sourceGaps；
- 普通角色不生成 Cost/Payable/Payment；
- 成本授权投影只生成一次 Cost 节点；
- 节点 projectId、nodeKey、returnPath、returnAnchor；
- Provider 白名单与事实节点字段。

## 四、未覆盖

- 历史版本、替代/作废节点折叠；
- ProfitSettlement 利润/结算节点（需统一财务口径）；
- App/Web/业主页面接线；
- 真实 Provider HTTP、真实页面、跨企业/门店权限负向；
- U5 独立验收。

## 五、门禁

```text
U1/U2/U3/U4：PASS / ACCEPTED
U5-A：DEV_VERIFIED
U5-B1：DEV_VERIFIED / ACCEPTANCE_PENDING
U5-B2：NOT_STARTED（历史/利润结算）
U5-C~F：NOT_STARTED
U6：未启动
GAP-065：OPEN
```
