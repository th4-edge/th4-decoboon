# 统一对象视图一期 U4 · 修复后第二轮独立验收

| 项 | 内容 |
| --- | --- |
| 问题 | `PI-20261022-010` 统一对象视图一期实施 |
| 验收切片 | `U4` 合同事实追踪（修复后复验） |
| 日期 | 2026-10-23 |
| 角色 | 独立验收方（只读审查，不修改实现） |
| 业务事实 | `DEC-086`、`BR-052`、`T12-BIZ-20261022-007` |
| 前置 | U1/U2 独立验收 PASS；U3 必须有独立验收 PASS 报告 |
| 最终结论 | **FAIL / U4 BLOCKED**；P0=0、P1=1、P2=1 |
| 问题状态 | `DEV_IN_PROGRESS` |
| U5 门禁 | **BLOCKED** |

## 一、结论

本轮确认首轮 U4 的成本角色、承接标签和实际无项目展示已完成修复，但 U4 仍不能标记 `ACCEPTED`：

1. **P1-01 · U3 前置事实不一致。** 用户任务说明称 U3 独立报告已为 PASS，但当前落盘文件 `docs/acceptance/runs/20261023-unified-object-view-u3-independent/README.md` 仍明确写着 `最终结论：FAIL（仅 U3）`；`u3-remediation/README.md` 也仍为 `DEV_VERIFIED / REMEDIATED_PENDING_REACCEPTANCE`，没有独立复验 PASS 记录。复验开始时 registry 同时存在 `developmentTask.u3AcceptanceStatus=PASS` 与 `u3Acceptance.status=FAIL`、原 findings 仍 OPEN 的矛盾状态；本轮已按落盘报告校正为 U3 修复待独立复验。因此 U3 前置门禁仍未满足。
2. **P2-01 · 无项目豁免未接入节点校验契约。** `BusinessFactContract.isPreProjectFact` 能识别带说明的无项目上游事实，但 `validateNode` 仍无条件要求 `projectId != null`，且生产代码没有调用该豁免。实际响应不伪造对象，但统一契约仍会拒绝合法的无项目事实。

成本事实、承接标签和实际无项目响应均通过本轮复验，不再作为阻断项。

## 二、隔离快照

```text
后端：/tmp/uov-u4-backend.BjevXx/repo
App ：/tmp/uov-u4-app.UJm5Dq/repo
Web ：/tmp/uov-u4-web.iz5i06/repo
```

后端快照在首轮隔离基线中同步当前 U4 修复文件；App/Web 的 U4 文件与当前工作树逐文件 SHA-256 一致。构建产物在隔离快照内生成，未使用共享工作区 `target/` 或 `dist/` 作为通过证据。

## 三、命令与结果

### 3.1 后端

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-22.jdk/Contents/Home \
mvn -o -q clean test \
  -Dtest=ContractFactTraceTest,BusinessFactContractTest,QuoteViewContextTest,QuoteUnifiedViewPropertiesTest,CustomerJourneyChangeSectionTest,CustomerJourneyOwnerPreviewModeTest test

JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-22.jdk/Contents/Home \
mvn -o -q -DskipTests compile
```

结果：

- 后端相关回归：**20/20 PASS**，0 failures / 0 errors / 0 skipped。
- `ContractFactTraceTest` 3/3。
- `BusinessFactContractTest` 5/5。
- `QuoteViewContextTest` 7/7。
- 其余关联回归 5/5。
- compile：PASS。
- 开发证据声称 19/19；本轮使用额外契约类得到 20/20，未发现失败。

### 3.2 独立探针

隔离探针 2/2 通过，分别确认：

- 7 类员工角色仅 BOSS、店长、财务向 `ContractWorkbenchContext.build` 传入 `includeInternalCost=true`；设计师、PM、营销、TeamLeader 均为 false。
- `isPreProjectFact` 对无项目事实返回 true，但同一节点调用 `validateNode` 仍因 `projectId` 为空抛错，证明豁免尚未接入统一校验。

### 3.3 App

```text
npm run type-check          PASS
npm run check:document      PASS
npm run build:mp-weixin     PASS
```

### 3.4 Web

```text
npm run type-check          PASS
npm run check:client-copy   PASS（由 build 执行）
npm run check:actions       PASS
npm run build               PASS
```

### 3.5 静态审计

```text
U4 相关 git diff --check：PASS
U4-owned DDL/迁移：无
U4-owned 权限放宽：无
U4-owned 金额/成本/利润公式变化：无
U4-owned 状态机变化：无
```

工作区存在并行迁移 `127-付款施工计划模板不可变版本.sql`，不属于 U4 变更。

## 四、U4 复验项

| 验收项 | 结论 | 证据 |
| --- | --- | --- |
| U3 独立验收 PASS 前置 | **FAIL（P1-01）** | U3 报告仍 FAIL；registry U3 口径冲突；仅有修复开发证据 |
| 上游事实固定顺序 | PASS | `Customer → Property → Measure → Design → Quote → Contract` |
| 最近已完成量房与排序 | PASS | 过滤 Completed；非空 `completedAt desc, measureId desc`；区域/实测/待补计数 |
| 绑定设计/报价版本 | PASS | 沿 `contract.quote`、`quote.design` 读取，未取最新 |
| Cost 仅授权角色生成 | PASS | 7 角色探针；设计师/PM 无 Cost 节点且 `workbench.cost=null` |
| 合同响应成本字段白名单 | PASS | 当前 `ProjectContract` 白名单不含 `signedCost/costComplete` |
| Web 设计师合同页成本展示 | PASS（静态） | 后端不返回 Cost 节点；页面未发现成本专用展示 |
| 承接标签精确 | PASS | 固定文案“项目承接（接管基线）”，并有承接场景断言 |
| 无项目事实不伪造对象 | PASS（行为） | 实际上游节点带 `projectId=null` 与“项目尚未建立，事实按合同绑定关系展示”；缺量房/设计只进 sourceGaps |
| 无项目豁免统一契约 | **FAIL（P2-01）** | `isPreProjectFact=true` 时 `validateNode` 仍拒绝 `projectId=null` |
| 下游摘要 | PASS（scoped） | 项目、增减项、材料、验收、应收、成本摘要均接入 |
| App/Web 展示 | PASS（构建/静态） | App 合同页与 Web 设计师合同交接展示事实卡，报价可跳统一视图 |
| 真实页面、真机、跨租户负向 | 未覆盖 | 留 U6；不得由本轮静态/构建证据冒充 |

### 4.1 量房边界风险

`completed_at` 数据库列允许为空。当前比较器先 `nullsLast` 再整体 `reversed`，非空值排序正确，但空值会变为最前；正常 Completed 保存路径会补 `completedAt`，因此本轮不单列为阻断项，但应在后续契约测试中覆盖空 `completedAt`。

## 五、首轮问题闭环

| 首轮问题 | 本轮判定 | 说明 |
| --- | --- | --- |
| P1-02 成本事实未按角色裁剪 | **CLOSED** | 角色矩阵、`workbench.cost=null`、无 Cost 节点和合同成本白名单均通过 |
| P2-01 承接标签错误 | **CLOSED** | 精确输出“项目承接（接管基线）” |
| P2-02 无项目 `projectId` 边界 | **PARTIAL** | 实际节点行为修复；统一 `validateNode` 未接受豁免 |
| P1-01 U3 前置 | **OPEN** | U3 仍无独立 PASS 证据；复验开始时 registry 的 PASS/FAIL 冲突已按落盘报告校正 |

## 六、门禁结论

```text
U1：PASS
U2：PASS
U3：FAIL / REMEDIATED_PENDING_REACCEPTANCE（以已落盘报告为准）
U4：FAIL / BLOCKED
PI-20261022-010：DEV_IN_PROGRESS
U5：BLOCKED（仅允许 U4 ACCEPTED 后重新对账并准备）
U6：未启动
GAP-065：OPEN
```

下一步必须先完成 U3 独立复验并落盘 PASS 报告、修正 registry 的 U3 单一事实口径；同时把无项目豁免接入统一节点校验。之后才能重新独立验收 U4。本轮不进入 U5/U6。
