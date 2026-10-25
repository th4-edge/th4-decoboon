# 统一对象视图一期 U5-C · App 项目详情 PHASE 视图独立验收

| 项 | 内容 |
| --- | --- |
| 问题 | `PI-20261022-010` 统一对象视图一期实施 |
| 验收切片 | `U5-C` App 项目详情 PHASE 视图 |
| 日期 | 2026-10-23 |
| 角色 | 独立验收方（只读审查，不修改实现） |
| 前置 | U1/U2/U3/U4 ACCEPTED；U5-A/U5-B1/U5-B2 仅 `DEV_VERIFIED` |
| 业务依据 | `DEC-086 §5`、`BR-052 §4`、`T12-BIZ-20261022-007` |
| 最终结论 | **FAIL**；P0=0、P1=2、P2=0 |
| U5-C 状态 | `ACCEPTANCE_REJECTED`，不得标记 ACCEPTED |
| U5-D 门禁 | **BLOCKED** |
| 问题状态 | `DEV_IN_PROGRESS`；不得关闭 PI-010 |

## 一、结论

U5-C 的 8 阶段 PHASE、当前/异常展开、同 nodeKey 历史折叠、来源缺口和返回锚点主体已实现，App type-check、小程序构建、分包和导航检查通过。

但本轮独立验收发现两个阻断项：

1. `project/project/info` 的根响应白名单仍允许 `Project.cost` 和 `Quote.cost`，设计师/PM 等无成本权限角色可通过项目详情响应获得成本字段。
2. App 没有消费服务端稳定顺序：`ProjectFactPhase` 重新排序节点，且排序键与 U1/U5 契约不同；API 归一化还对缺失的 `stage/current/version` 填入默认值，违反“同源、禁止前端重算/兜底”。

因此 U5-C **FAIL**，不得进入 U5-D。U5-A/U5-B1/U5-B2 仍按原状态保留为 `DEV_VERIFIED`，不因本轮被自动标记 ACCEPTED。

## 二、隔离快照

```text
后端：/tmp/u5c-backend.independent
App ：/tmp/u5c-app.independent
```

- U5-C scoped 文件：39 个（后端 35、App 4）。
- 清单 SHA-256：`82e212f330f4109df629f1293d8d4b18fb8051964367b05bbc2dc96b1b1cc7f9`。
- 与当前工作区 39 个文件逐一比较：0 mismatch。
- 后端基于 detached `HEAD`，仅覆盖 U5 契约、聚合、查询依赖与测试；App 仅覆盖项目模型/API、项目详情和 `ProjectFactPhase`。
- U5-C 没有 Web 切片；U5-D 未纳入，Web 不得作为本轮通过证据。

## 三、命令与结果

### 3.1 后端定向测试

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-22.jdk/Contents/Home \
mvn -o -q clean test \
  -Dtest=ProjectFactTraceContractTest,ProjectFactTraceContextTest,ProjectFactTracePropertiesTest,BusinessFactContractTest
```

结果：**18/18 PASS**，0 failures / 0 errors / 0 skipped。

- `ProjectFactTraceContractTest` 5/5
- `ProjectFactTraceContextTest` 7/7
- `ProjectFactTracePropertiesTest` 1/1
- `BusinessFactContractTest` 5/5

### 3.2 独立边界探针

`U5CProviderBoundaryProbeTest`：**2/2 PASS**

- `project/project/info` 在聚合事实前先调用 `requireReadableProject`；跨企业/非参与项目由既有 404 门禁阻断。
- 7 类角色只在 BOSS/店长/财务路径请求 `includeInternalCost=true`，设计师/PM/Marketer/TeamLeader 为 false。

`U5CFinalAcceptanceProbeTest`：**1/1 FAIL**

- 单独断言 `Project` 根字段不得包含 `cost/actualCost/grossProfit/grossMargin/costComplete`。
- 实际白名单命中 `cost`，探针失败；这是本轮 P1-01 的直接证据。

### 3.3 后端 package

```text
mvn -o -q -Dmaven.test.skip=true package
PASS：target/th4-decoboon-1.0.0.jar（约 7.6 MB）
```

### 3.4 App

```text
npm run type-check                    PASS
npm run build:mp-weixin               PASS
npm run check:subpackages             PASS
npm run check:navigation              PASS
```

构建结果：

```text
主包 1,009,988 B
最大分包 716,724 B
总包 3,826,369 B
主包 14 页 / 17 个分包 / 170 页
```

### 3.5 无关失败隔离

- `npm run check:ui`：FAIL，命中 `src/App.vue:62` 直接调用 `uni.showModal/system`，不是 U5-C 文件，按无关失败处理。
- `npm run check:owner-preview`：FAIL，命中既有 TeamLeader 缺少 `view:owner-preview` 的权限差异，不是 U5-C 文件，按无关失败处理。
- `check:document` 在独立 App worktree 因脚本依赖后端相对路径失败，未作为 U5-C 失败；U5-C 目标构建、分包和导航均已通过。

## 四、逐项判定

| 验收项 | 结论 | 证据 |
| --- | --- | --- |
| 固定 8 阶段顺序 | PASS | `ProjectFactPhase.vue` 固定 `Source → PreSale → Signing → Fulfillment → Change → Acceptance → Finance → AfterSales` |
| 当前阶段与异常阶段默认展开 | PASS | `isCurrent || hasAttention`；必需缺口和 `uncertainty/costComplete=false` 触发关注 |
| 同 nodeKey 历史折叠 | PASS | `groupNodes` 按 nodeKey 聚合，取 current 为主卡，历史卡内折叠 |
| 来源缺口展示 | PASS | `sourceGaps` 按阶段显示“必须补齐/可选 + reason” |
| 事实缺失不伪装空数组 | PASS | `!trace` 显示“项目事实暂不可确认”，不渲染为空列表 |
| 返回锚点 | PASS（静态） | `tab=facts`、`pendingFactAnchor`、`returnPath/returnAnchor`、`pageScrollTo` 已接线 |
| 项目详情与事实同源 | **FAIL（P1-02）** | App 重新排序节点并使用不同 tie-break；缺失 stage/current/version 被默认填充 |
| 成本角色边界 | **FAIL（P1-01）** | `Project.cost` / `Quote.cost` 仍在 `project/project/info` 根白名单 |
| 跨企业/非参与项目 | PASS（静态/单元） | Provider 先走 `requireReadableProject`；真实 HTTP 负向留 U6 |
| 无项目 | PASS（静态） | `projectId` 必填，访问上下文返回 404 |
| App type-check/build/分包/导航 | PASS | 见上 |
| U5-C 自有 DDL/迁移 | PASS | U5-C-owned 无 SQL/migration |
| U5-D 可进入 | **BLOCKED** | U5-C FAIL，未通过独立验收 |

## 五、阻断项

### P1-01 · 项目详情根响应泄露成本字段

证据：

- `src/main/java/com/th4/edge/decoboon/project/service/projects/info/support/Info.java:35`：Project 白名单包含 `cost`。
- 同文件 `:52`：Quote 白名单包含 `cost`。
- 独立 `U5CFinalAcceptanceProbeTest` 断言失败，实际命中 `cost`。

影响：设计师、PM、Marketer 等无成本权限角色仍可通过 `project/project/info` 获得成本字段；App 不展示不能替代服务端字段裁剪。

最小修复：

1. 从 `project/project/info` 的 Project/Quote 根白名单移除成本字段，或按角色使用安全投影。
2. 成本只允许通过已授权的 `Cost/ProfitSettlement` 事实节点或专门财务投影返回。
3. 增加 Provider 白名单回归，覆盖 BOSS/店长/财务与设计师/PM 的响应字段。

### P1-02 · App 重排事实节点并默认填充缺失契约字段

证据：

- `ProjectFactPhase.vue:86` 对服务端 nodes 再次 `sort(compareNodes)`。
- `ProjectFactPhase.vue:117-123` 使用 `businessTime || recordedAt → objectId`，缺少 `recordedAt` 和 `factType` tie-break，与 `BusinessFactContract.stableOrder()` 不一致。
- `src/domains/project/api.ts:84` 未知 stage 默认 `'source'`。
- 同文件 `:112-113` 缺失 current 默认 true、缺失 version 默认 1。

影响：服务端已冻结的稳定顺序可能被前端改变；缺失/未知字段会被伪装成来源阶段、当前事实或版本 1，违反 U5“同源、缺失暂不可确认、禁止前端兜底”。

最小修复：

1. App 按服务端 `trace.nodes` 原顺序分组，不再重新排序。
2. 若保留前端排序，必须完整复制 U1/U5 排序键：`businessTime asc nulls last → recordedAt asc nulls last → factType → objectId`。
3. 对缺失/未知 stage、current、version 直接显示“暂不可确认”或拒绝投影，不填默认业务事实。
4. 增加前端契约探针，验证输入顺序变化不影响页面、缺失字段不产生伪当前节点。

## 六、剩余风险

- 来源缺口目前只提供原因和“必须补齐/可选”提示，没有直接修复 CTA；修复动作应走来源对象或后续 U5-F/U6 导航。
- 返回锚点依赖页面实例保留展开阶段；若冷启动/重挂载，锚点节点可能因阶段折叠而不存在，需在 U6 真机验证。
- 真实 App 页面、真机、多机型、弱网、跨企业/门店/非参与项目 HTTP 负向仍未执行，留 U6。
- `GAP-053` 真实非空区域、`GAP-071` 成本三态口径仍开放。

## 七、门禁结论

```text
U1/U2/U3/U4：ACCEPTED
U5-A/U5-B1/U5-B2：DEV_VERIFIED / ACCEPTANCE_PENDING
U5-C：FAIL / ACCEPTANCE_REJECTED
U5-D~F：NOT_STARTED / BLOCKED
U6：NOT_STARTED
PI-20261022-010：DEV_IN_PROGRESS
GAP-065：OPEN
GAP-071：OPEN
```

不得进入 U5-D，不得把本次 App 构建通过写成 U5-C 验收通过。
