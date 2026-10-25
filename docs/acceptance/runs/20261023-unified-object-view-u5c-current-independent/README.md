# 统一对象视图一期 U5-C · 当前工作区只读独立复核

| 项 | 内容 |
| --- | --- |
| 问题 | `PI-20261022-010` 统一对象视图一期实施 |
| 验收切片 | `U5-C` App 项目详情 PHASE 视图 |
| 日期 | 2026-10-23 |
| 角色 | 独立只读复核（不修改产品或验收快照实现） |
| 前置 | U5-C 已有第二轮独立验收 PASS；U5-D 后续已独立 ACCEPTED |
| 最终结论 | **PASS / U5-C ACCEPTED**；P0/P1/P2=`0/0/0` |
| U5-E 门禁 | 未实现；仅 `RECONCILIATION_AND_PREPARATION_ONLY` |
| 发布 | 未发布 |

## 一、结论

当前 U5-C 产品代码与既有独立验收快照一致，既有 `P1-01`、`P1-02` 修复仍成立，本轮重新执行的 22 项后端定向门禁、App 构建/分包/导航和 9 项同源 UI 探针全部 PASS。

因此当前 U5-C 可以维持：

```text
PASS / U5-C ACCEPTED
```

首轮 FAIL 和修复后第二轮 PASS 报告均保留，不覆盖历史。

## 二、只读复核范围

- 现有独立快照：
  - 后端：`/tmp/u5c-reaccept-backend`
  - App：`/tmp/u5c-reaccept-app`
- 当前产品源码与快照中可枚举的 27 个 U5-C changed product files 逐一比较：**0 mismatch**。
- 当前 27 文件清单 SHA-256：`32d8cc24b5640d2073552ff10da8125d69d30edd713e91969134e68b850a5225`。
- 两个 `U5C*ProbeTest` 仅存在于独立验收快照，不是产品源码；未混入产品 `src/test`。
- U5-C-owned SQL/migration：无。
- 固定快照 HEAD：
  - 后端 `7cf83fa2b526156640ea8c227eb348ab81ca27d1`
  - App `aa387a86c198e413e4a340b7c887ba961e0169f0`

## 三、重新执行结果

### 3.1 后端

```text
ProjectFactTraceContractTest          5/5 PASS
ProjectFactTraceContextTest           7/7 PASS
ProjectFactTracePropertiesTest        2/2 PASS
BusinessFactContractTest              5/5 PASS
U5CFinalAcceptanceProbeTest           1/1 PASS
U5CProviderBoundaryProbeTest          2/2 PASS
----------------------------------------------------------------
合计                                  22/22 PASS
failures=0 errors=0 skipped=0
mvn exit=0
```

关键闭环：

- `project/project/info` 的 Project/Quote 根白名单不含 `cost`。
- 7 角色成本参数矩阵由独立探针锁定。
- App 消费服务端稳定顺序，不重新排序。
- 缺失/未知 stage、current、version 等契约字段不再默认填充。

### 3.2 App

```text
npm run type-check             PASS
npm run build:mp-weixin        PASS
npm run check:subpackages      PASS
npm run check:navigation       PASS
node u5c-independent-probe.mjs PASS 9/9
```

构建体积：

```text
主包 1,010,856 B
最大分包 716,177 B
总包 3,826,690 B
主包 14 页 / 17 个分包 / 170 页
```

同源探针：

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

## 四、逐项判定

| 验收项 | 结论 | 证据 |
| --- | --- | --- |
| 固定 8 阶段顺序 | PASS | UI 探针 |
| 当前/异常阶段默认展开 | PASS | UI 探针与组件静态核对 |
| 同 nodeKey 历史折叠 | PASS | UI 探针 |
| 来源缺口/无 trace 失败态 | PASS | UI 探针 |
| 返回锚点、来源路由 | PASS（静态） | UI 探针 |
| 服务端稳定顺序直连 | PASS | UI 探针 + API 契约 |
| 禁止前端默认兜底 | PASS | UI 探针 + API 契约 |
| Project/Quote 根成本字段裁剪 | PASS | 后端独立探针 |
| 7 角色成本参数矩阵 | PASS | Provider 边界探针 |
| App type-check/build/分包/导航 | PASS | 命令结果 |
| U5-C-owned DDL/金额/状态变化 | 无 | 文件范围审计 |

## 五、越权编码审计

### 5.1 是否发生越权编码

**是，发生过临时越权编码。** 在收到停止指令前，已按当前线程“授权，下一步”开始 U5-E 后端实现。收到停止指令后，U5-E 编码已立即停止，所有临时 U5-E 代码与准备文档均已回退；没有 U5-E 代码进入本次 U5-C 验收证据。

### 5.2 已回退的 U5-E 临时代码修改

| 文件 | 临时 diff 摘要 | 当前状态 |
| --- | --- | --- |
| `src/main/java/com/th4/edge/decoboon/project/model/ProjectFactTraceContract.java` | 曾新增 `OWNER_FACT_TYPES`、`ownerProjection`、`ownerNode`、`ownerGap` | **已回退，无 U5-E 标识** |
| `src/main/java/com/th4/edge/decoboon/customer/model/CustomerJourney.java` | 曾新增 `ProjectFactTrace factTrace` 字段及 getter/setter | **已回退** |
| `src/main/java/com/th4/edge/decoboon/customer/context/CustomerJourneyContext.java` | 曾注入 `ProjectFactTraceContext`，构造后设置 owner projection | **已回退** |
| `src/main/java/com/th4/edge/decoboon/customer/service/project/journey/info/support/Info.java` | 曾放行 `factTrace`、节点和 sourceGap 属性 | **已回退** |
| `src/main/java/com/th4/edge/decoboon/project/service/ownerpreview/info/support/Info.java` | 同上 | **已回退** |
| `src/test/java/com/th4/edge/decoboon/project/ProjectFactTraceContractTest.java` | 曾新增 ownerProjection 过滤/脱敏测试与 gap helper | **已回退** |
| `src/test/java/com/th4/edge/decoboon/project/ProjectFactTracePropertiesTest.java` | 曾新增 Owner journey/preview 属性白名单测试 | **已回退** |
| `src/test/java/com/th4/edge/decoboon/customer/CustomerJourneyOwnerPreviewModeTest.java` | 曾新增 ProjectFactTraceContext 构造参数和 factTrace parity 断言 | **已回退** |
| `src/test/java/com/th4/edge/decoboon/customer/CustomerJourneyChangeSectionTest.java` | 曾新增 ProjectFactTraceContext 构造参数 | **已回退** |
| `src/test/java/com/th4/edge/decoboon/project/OwnerPreviewInfoAcceptanceTest.java` | 曾新增 factTrace parity 断言 | **已回退** |
| `src/test/java/com/th4/edge/decoboon/customer/CustomerJourneyTestSupport.java` | 曾新增 U5-E mock helper | **文件已删除** |

- 未发生 App 代码修改。
- 曾创建的 U5-E 准备单、registry/state/reconciliation/issue/worklog 变更均已回退。
- `ProjectFactTraceContract.java` 等文件当前仍显示为 pre-existing worktree diff，但内容中不存在 `ownerProjection`、`factTrace`、`OWNER_FACT_TYPES` 等本轮 U5-E 标识。
- 本次 U5-C 命令均在冻结快照执行，U5-E 临时代码没有参与任何 U5-C 证据。

## 六、剩余风险

1. 真实微信开发者工具/真机、多机型、弱网和跨企业/门店/非参与项目 HTTP 负向仍留 U6。
2. 冷启动返回锚点在阶段折叠后是否稳定仍需真机验证。
3. `GAP-053` 非空区域数据、`GAP-071` 成本三态口径保持 OPEN。
4. U5-D 已另案 ACCEPTED；U5-E 未实现，不得因本报告进入 U5-E。

## 七、门禁

```text
U1/U2/U3/U4：ACCEPTED
U5-A/U5-B1/U5-B2：DEV_VERIFIED
U5-C：PASS / ACCEPTED
U5-D：ACCEPTED（另案证据）
U5-E：RECONCILIATION_AND_PREPARATION_ONLY
U5-F：NOT_STARTED
U6：NOT_STARTED
PI-20261022-010：DEV_IN_PROGRESS
GAP-065：OPEN
GAP-071：OPEN
```
