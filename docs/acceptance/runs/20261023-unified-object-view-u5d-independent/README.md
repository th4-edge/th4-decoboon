# 统一对象视图一期 U5-D · Web 项目执行 PHASE 视图独立验收

| 项 | 内容 |
| --- | --- |
| 问题 | `PI-20261022-010` 统一对象视图一期实施 |
| 验收切片 | `U5-D` Web 项目执行工作面 PHASE 视图 |
| 日期 | 2026-10-23 |
| 角色 | 独立验收方（只读审查，不修改实现） |
| 前置 | U1/U2/U3/U4/U5-C ACCEPTED；U5-A/U5-B1/U5-B2 `DEV_VERIFIED` |
| 业务依据 | `DEC-086 §5`、`BR-052 §4`、`T12-BIZ-20261022-007` |
| 最终结论 | **FAIL**；P0=0、P1=1、P2=1 |
| U5-D 状态 | `ACCEPTANCE_REJECTED`，不得标记 ACCEPTED |
| U5-E/F、U6 | 未启动；U5-E/F 保持 BLOCKED |

## 一、结论

U5-D 的数据映射、8 阶段 PHASE、施工执行双视图、历史折叠、缺口/异常、服务端顺序、构建和 source guard 均已实现并通过复跑。

但独立反向检查发现两个未闭合问题：

1. **P1-01 历史节点返回锚点失效。** 页面为当前主卡设置了 `fact-anchor-${primary.nodeId}`，但历史节点没有相同的 DOM id；点击历史节点的“查看历史来源”后，返回锚点指向历史 nodeId，页面只能展开历史列表，无法滚动定位历史节点。
2. **P2-01 部分材料事实没有统一来源入口。** `MaterialPlan`、`Purchase`、`GoodsReceipt` 节点在 Web 当前没有对应 source route，违反准备单“材料入口一致”的退出条件；如果当前没有合法目标页，应在准备单/验收口径中明确“摘要 only”，不能静默缺失。

因此 U5-D **FAIL / ACCEPTANCE_REJECTED**，不得进入 U5-E。

## 二、独立快照

```text
/tmp/u5d-web-acceptance
```

- 基于 Web detached `HEAD`，仅覆盖 5 个 U5-D-owned 文件。
- manifest SHA-256：`7d3c3723c613fd44f4fd4a74dfa3d1aba1ed0201b6e19e677a81f0f0e6a7b254`。
- 与当前工作区 U5-D-owned 文件比较：0 mismatch。
- U5-D-owned SQL/migration：无。

## 三、命令与结果

```text
npm run type-check                                  PASS
npm run check:client-copy                           PASS
npm run check:actions                               PASS
node scripts/verify-project-fact-phase.mjs          PASS
npm run build                                       PASS
```

构建仅有既有 Sass deprecation warning，不是 U5-D 失败。

独立反向探针：

```text
PRIMARY_GROUP_ANCHOR=true
HISTORY_NODE_ANCHOR=false
MaterialPlan_ROUTE=MISSING
Purchase_ROUTE=MISSING
GoodsReceipt_ROUTE=MISSING
```

## 四、逐项判定

| 验收项 | 结论 | 证据 |
| --- | --- | --- |
| Web ProjectBrief/API 严格映射 factTrace | PASS | 缺失/未知 stage/current/version 不默认；项目绑定校验 |
| 8 阶段与当前/异常展开 | PASS | PHASE 面板和 source guard |
| 同 nodeKey 当前主卡 + 历史折叠 | PASS（视觉/结构） | 当前主卡和历史列表已渲染 |
| 历史节点返回锚点 | **FAIL（P1-01）** | 历史项没有 `:id="anchorId(history.nodeId)"` |
| 来源缺口/无 trace 失败态 | PASS | WorkbenchState + required/reason |
| 服务端顺序直连、禁止重排 | PASS | `serverNodes` 直接消费 |
| 当前角色来源路由 | PARTIAL | 设计/报价/合同/验收/财务/反馈路由存在；材料计划/采购/到货缺失 |
| 项目执行原工作台不回归 | PASS | 施工执行视图、任务、日报、dialog 保留 |
| Web type-check/client-copy/actions/build | PASS | 见命令结果 |
| U5-D-owned DDL/权限/金额/状态变更 | PASS | 无 DDL；无后端修改 |
| 真实页面/真机/跨企业 HTTP 负向 | 未覆盖 | 留 U5-F/U6 |

## 五、阻断项与最小修复

### P1-01 · 历史节点返回锚点不存在

证据：

- `src/views/workbench/ProjectExecutionView.vue` 打开节点时使用 `factAnchorId(node.nodeId)`，历史节点也传入同一个 `anchor`。
- `src/components/project/ProjectFactPhasePanel.vue:279` 仅给当前主卡绑定 `anchorId(group.primary.nodeId)`。
- `src/components/project/ProjectFactPhasePanel.vue:314` 的历史项没有绑定 `anchorId(history.nodeId)`。

最小修复：

1. 给历史项绑定 `:id="anchorId(history.nodeId)"`，或让锚点统一落到可见的历史容器。
2. 历史项增加 `scroll-margin-top`。
3. source guard 增加“每个当前与历史 nodeId 都有对应 DOM 锚点”的检查。
4. 补真实浏览器返回锚点回归。

### P2-01 · 材料计划/采购/到货来源入口缺失

证据：

- `ProjectExecutionView.factRouteTarget` 没有 `MaterialPlan`、`Purchase`、`GoodsReceipt` 分支。
- 当前 Web 没有这些节点的精确来源页；不能伪造跳转。

最小修复：

1. 若已有统一材料/采购/到货详情页，接入该路由。
2. 若尚无页面，将准备单退出条件明确为“摘要 only”，并在事实卡上显示“暂无 Web 来源入口”，不得静默表现为可点击。
3. 增加 source route coverage 断言。

## 六、剩余风险

- 真实登录、浏览器历史返回、刷新、多分辨率和弱网未验证，留 U5-F/U6。
- 量房来源目前只能进入量房工作列表，尚无单条记录详情页。
- 返回锚点在冷启动/阶段重挂载后仍需真实验证。
- `GAP-053`、`GAP-071` 仍开放。

## 七、门禁

```text
U1/U2/U3/U4/U5-C：ACCEPTED
U5-A/U5-B1/U5-B2：DEV_VERIFIED
U5-D：FAIL / ACCEPTANCE_REJECTED
U5-E~F：BLOCKED / NOT_STARTED
U6：NOT_STARTED
PI-20261022-010：DEV_IN_PROGRESS
GAP-065：OPEN
GAP-071：OPEN
```

不得进入 U5-E，不得把 Web 构建通过写成 U5-D 验收通过。
