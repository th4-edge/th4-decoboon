# 统一对象视图一期 U5-B2 · 历史折叠与利润/结算节点开发证据

| 项 | 内容 |
| --- | --- |
| 问题 | `PI-20261022-010` 统一对象视图一期实施 |
| 切片 | `U5-B2` 历史/替代/作废节点折叠、利润/结算节点 |
| 日期 | 2026-10-23 |
| 状态 | `DEV_VERIFIED / ACCEPTANCE_PENDING` |
| 边界 | 只扩展后端项目事实索引；不改 App/Web，不新增 DDL、Provider、权限或金额/成本/利润公式 |
| 隔离目录 | `/tmp/u5b2.final.nZfhXR` |

## 一、业务依据

- `ANS-BIZ-20261022-007` / `T12-BIZ-20261022-007`
- `DEC-086 §5`：历史/替代/作废版本同卡折叠，财务事实按事实族聚合
- `BR-052 §4`：同一对象只保留一张当前摘要卡，历史版本不伪装为当前有效事实
- `MET-002` / `BR-003`：成本不完整时利润与毛利率为 `null`；应付、付款不等于成本
- `PI-20261022-017` / `GAP-071` 仍开放：本切片不统一新的三态成本公式

## 二、实现结果

### 2.1 历史折叠

- `BusinessFactNode.current` 作为唯一当前/历史标志，契约要求非空。
- `ProjectFactTraceContract.nodeKeyGroups()` 按稳定 `nodeKey` 聚合当前事实与历史版本。
- 报价：沿设计方案或房屋读取历史报价，已替代/已拒绝及非项目绑定报价输出 `current=false`。
- 设计：沿房屋读取方案版本，归档及非绑定方案输出 `current=false`。
- 主材选材：按合同主材行保留版本历史，每个主材行只将最高版本标记为当前。
- 采购：取消/驳回采购保留为历史摘要，不覆盖当前有效采购。
- 合同变更：已驳回/已取消变更输出 `current=false`。
- 验收：已驳回验收轮次输出 `current=false`，保留历史追溯。
- 收款：有效收款进入当前汇总；已冲正收款进入同一 `nodeKey` 的历史摘要，不参与当前累计。

### 2.2 利润/结算节点

- 仅 BOSS、店长、财务可见。
- 仅在项目存在 `Settlement` 快照时生成 `ProfitSettlement` 节点。
- 直接继承结算快照的 `finalAmount/profitAmount/grossMargin`，不重算利润。
- 结算快照利润为空时输出 `costComplete=false`、利润与毛利率为空，并提示“成本未完整”。
- 没有结算快照时不生成空白利润卡。
- 本切片不修复 `PI-20261022-017` 的三态成本统一实现；不得把该节点解释为 `GAP-071` 已关闭。

## 三、冻结点

- 历史节点必须与当前节点共享 `nodeKey`，但仍保留独立 `nodeId/objectId/version`。
- 历史节点必须 `current=false` 并说明历史/失效原因。
- 普通角色不生成 `Cost/Payable/Payment/ProfitSettlement`。
- 业主不接收成本、利润、毛利率、供应商价或内部结算字段。
- 项目事实链不复制结算全量明细，只输出摘要和统一对象入口。

## 四、验证

```text
ProjectFactTraceContractTest        5/5 PASS
ProjectFactTraceContextTest         7/7 PASS
ProjectFactTracePropertiesTest      1/1 PASS
BusinessFactContractTest            5/5 PASS
合计：18/18 PASS，0 failures / 0 errors / 0 skipped

成本邻接回归：
ProjectCostProjectionTest           6/6 PASS
ProjectCostContextTest             14/14 PASS
ProjectCostUnpricedCompletenessTest 3/3 PASS
合计：23/23 PASS

JDK 编译：PASS
U5-B2 + 成本邻接合计：41/41 PASS，0 failures / 0 errors / 0 skipped

JDK package：PASS（8.13 MB）
```

覆盖：

- 报价/设计历史共享 `nodeKey` 且仅一个当前事实；
- 主材选材按合同行折叠版本；
- 已冲正收款不进入当前累计；
- 利润/结算节点继承结算快照且角色裁剪正确；
- 结算利润为空时 `costComplete=false`、利润和毛利率为空；
- Provider 白名单包含 `profitAmount/grossMargin`，无 DTO/VO。

## 五、未覆盖

- App/Web/业主页面消费与真实渲染；
- 真实 Provider HTTP、跨企业/门店/角色越权负向；
- 历史脏数据兼容和端到端 SQL 预算；
- `PI-20261022-017` 三态成本/毛利统一；
- U5-F 独立验收。

## 六、门禁

```text
U1/U2/U3/U4：PASS / ACCEPTED
U5-A/U5-B1/U5-B2：DEV_VERIFIED / ACCEPTANCE_PENDING
U5-C~F：NOT_STARTED
U6：NOT_STARTED
GAP-065：OPEN
GAP-071：OPEN
PI-20261022-010：DEV_IN_PROGRESS
```
