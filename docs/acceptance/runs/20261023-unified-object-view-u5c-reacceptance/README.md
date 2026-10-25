# 统一对象视图一期 U5-C · 修复后独立复验

| 项 | 内容 |
| --- | --- |
| 问题 | `PI-20261022-010` 统一对象视图一期实施 |
| 验收切片 | `U5-C` App 项目详情 PHASE 视图（修复后复验） |
| 日期 | 2026-10-23 |
| 角色 | 独立验收方（只读审查，不修改实现） |
| 前置 | U1/U2/U3/U4 ACCEPTED；U5-A/U5-B1/U5-B2 `DEV_VERIFIED` |
| 最终结论 | **PASS / U5-C ACCEPTED**；P0/P1/P2=`0/0/0` |
| U5-D 门禁 | `ALLOWED_PREPARE_AFTER_RECONCILIATION` / 未启动 |
| U6 | 未启动 |
| 历史报告 | 首轮 U5-C FAIL 报告保留，不覆盖 |

## 一、结论

U5-C 首轮两个 P1 已关闭：

1. **P1-01 成本根字段泄露已关闭。** `project/project/info` 的 Project/Quote 根白名单已移除 `cost`；成本只能通过授权事实/财务投影返回。
2. **P1-02 前端重排/兜底已关闭。** App 不再排序服务端事实节点；缺失或未知 stage/current/version 不再默认填充，改为抛出契约错误，由页面显示“暂不可确认”并提供重试路径。

固定 8 阶段、当前/异常展开、同 nodeKey 历史折叠、来源缺口、返回锚点和节点路由均通过复验。U5-C 可标记 `ACCEPTED`。

本轮只接受 U5-C；U5-A/U5-B1/U5-B2 仍保留 `DEV_VERIFIED` 原状态，U5-D~F 与 U6 未启动。

## 二、全新独立快照

```text
后端：/tmp/u5c-reaccept-backend
App ：/tmp/u5c-reaccept-app
```

- 从三仓 detached `HEAD` 重新建立，不复用开发副本作为验收证据。
- U5-C scoped 文件：39 个（后端 35、App 4）。
- 清单 SHA-256：`da8ce198c70e4429f1eb48b6acc376985351c99ff6e06f651a4f395af8894675`。
- 与当前工作区 39 个文件逐一比较：0 mismatch。
- U5C-owned SQL/migration：无。
- Web 不在 U5-C 范围；U5-D 未纳入。

## 三、命令与结果

### 3.1 后端

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-22.jdk/Contents/Home \
mvn -o -q clean test \
  -Dtest=ProjectFactTraceContractTest,ProjectFactTraceContextTest,ProjectFactTracePropertiesTest,BusinessFactContractTest,U5CFinalAcceptanceProbeTest,U5CProviderBoundaryProbeTest

JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-22.jdk/Contents/Home \
mvn -o -q -Dmaven.test.skip=true package
```

结果：

- 6 个测试类：**22/22 PASS**，0 failures / 0 errors / 0 skipped。
- `ProjectFactTraceContractTest` 5/5。
- `ProjectFactTraceContextTest` 7/7。
- `ProjectFactTracePropertiesTest` 2/2。
- `BusinessFactContractTest` 5/5。
- `U5CFinalAcceptanceProbeTest` 1/1：Project/Quote 根白名单不含成本字段。
- `U5CProviderBoundaryProbeTest` 2/2：数据范围前置门禁、7 角色成本参数矩阵。
- JDK 22 package：PASS，约 7.6 MB JAR。

### 3.2 App

```text
npm run type-check          PASS
npm run build:mp-weixin     PASS
npm run check:subpackages   PASS
npm run check:navigation    PASS
```

构建结果：

```text
主包 1,010,856 B
最大分包 716,177 B
总包 3,826,690 B
主包 14 页 / 17 个分包 / 170 页
```

### 3.3 独立 UI/同源探针

```text
EIGHT_STAGE_ORDER=PASS
NODE_KEY_HISTORY_FOLD=PASS
CURRENT_ATTENTION_EXPAND=PASS
MISSING_TRACE_AND_GAP_STATE=PASS
RETURN_ANCHOR_WIRING=PASS
FACT_ROUTE_MAPPING=PASS
FACT_NODE_CONTRACT_MAPPING=PASS
SERVER_STABLE_ORDER_CONSUMED=PASS
NO_FRONTEND_CONTRACT_FALLBACK=PASS
```

### 3.4 无关失败隔离

- `npm run check:ui`：FAIL，命中 `src/App.vue:62` 直接调用 `uni.showModal`，非 U5-C 文件。
- `npm run check:owner-preview`：FAIL，TeamLeader 缺少 `view:owner-preview` 的既有权限差异，非 U5-C 文件。
- 两项均未用于证明 U5-C PASS，也不计入 U5-C 自有缺陷。

## 四、P1 闭环

| 原问题 | 复验结果 | 证据 |
| --- | --- | --- |
| P1-01 项目详情根响应含成本 | **CLOSED** | Info Project/Quote 白名单无 cost；`ProjectFactTracePropertiesTest` 2/2、独立成本探针 1/1 |
| P1-02 App 重排/默认兜底 | **CLOSED** | `ProjectFactPhase` 直接使用服务端 nodes；API 严格校验 nodeId/nodeKey/stage/factType/projectId/version/current/visibility；UI/同源探针全部 PASS |

### 4.1 成本边界

- `project/project/info` 不再返回 Project.cost 或 Quote.cost。
- 授权成本仍通过 BOSS/店长/财务路径下的 Cost/ProfitSettlement 事实投影返回；设计师/PM/Marketer/TeamLeader 不请求内部成本事实。
- 页面隐藏不作为权限控制，服务端白名单是最终边界。

### 4.2 同源排序与缺失值

- App 不再对 `trace.nodes` 二次排序。
- 分组按服务端 nodes 顺序进行；同 nodeKey 当前主卡、历史卡内折叠。
- 缺失/未知 stage、current、version、nodeKey、factType、projectId、visibility、缺口 reason/required 不再补默认值；契约异常进入页面“项目事实暂不可确认”与重试路径。

## 五、逐项判定

| 验收项 | 结论 |
| --- | --- |
| 固定 8 阶段与当前/异常展开 | PASS |
| 同 nodeKey 历史折叠 | PASS（静态/契约） |
| 来源缺口与无 trace 失败态 | PASS |
| 返回锚点与来源路由 | PASS（静态） |
| `project/project/info` 与 App 同源 | PASS |
| 成本/成本完整性角色边界 | PASS |
| 跨企业/非参与项目前置门禁 | PASS（静态/单元）；真实 HTTP 留 U6 |
| 无项目/无 factTrace 边界 | PASS |
| App type-check/build/分包/导航 | PASS |
| U5-C 自有 DDL/金额/状态变化 | 无 |

## 六、剩余风险

1. 真实 App 页面、真机、多机型、弱网和跨企业/门店/非参与项目 HTTP 负向仍未执行，留 U6。
2. 返回锚点在冷启动或阶段重挂载后仍可能因阶段折叠而无法定位，需 U6 真机验证。
3. 来源缺口当前只有原因和“必须补齐/可选”提示，没有直接修复 CTA。
4. `GAP-053` 真实非空区域数据和 `GAP-071` 成本三态口径仍开放。
5. U5-D 仅允许重新对账和准备，不得直接启动或宣称完成。

## 七、门禁结论

```text
U1/U2/U3/U4：ACCEPTED
U5-A/U5-B1/U5-B2：DEV_VERIFIED / ACCEPTANCE_PENDING
U5-C：ACCEPTED
U5-D~F：ALLOWED_PREPARE_AFTER_RECONCILIATION / NOT_STARTED
U6：NOT_STARTED
PI-20261022-010：DEV_IN_PROGRESS
GAP-065：OPEN
GAP-071：OPEN
```

首轮 U5-C FAIL 报告保留为历史；本报告是 U5-C 最新独立验收事实源。
