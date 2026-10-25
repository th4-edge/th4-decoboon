# 统一对象视图一期 U4 · 最终独立复验

| 项 | 内容 |
| --- | --- |
| 问题 | `PI-20261022-010` 统一对象视图一期实施 |
| 验收切片 | `U4` 合同事实追踪（P2-01 修复后） |
| 日期 | 2026-10-23 |
| 角色 | 独立验收方（只读审查，不修改实现） |
| 前置 | U1/U2/U3 独立验收 PASS；U3 已 ACCEPTED |
| 业务事实 | `DEC-086`、`BR-052`、`T12-BIZ-20261022-007` |
| 最终结论 | **PASS / U4 ACCEPTED**；P0/P1/P2=`0/0/0` |
| U5 门禁 | **ALLOWED_PREPARE_AFTER_RECONCILIATION** |
| 历史报告 | 前两轮 U4 FAIL 报告保留，不覆盖 |

## 一、结论

U4 最后一项 P2 已关闭：

- `BusinessFactContract.validateNode` 已正式调用 `isPreProjectFact`。
- 无项目合同上游事实只有在带非空 uncertainty 时允许 `projectId=null`。
- 无 uncertainty 的无项目事实，以及下游事实 `projectId=null`，仍被拒绝。
- 成本角色矩阵、合同根响应成本字段、承接标签、上游顺序、量房排序、版本绑定、下游摘要和 App/Web 展示均通过复验。

因此 U4 独立验收 **PASS**，可标记 `ACCEPTED`。U5 仅允许重新对账和准备，不得启动、实施或宣称完成。

## 二、最新 scoped 三仓快照

```text
/tmp/u4-final.NiDrKN/backend
/tmp/u4-final.NiDrKN/app
/tmp/u4-final.NiDrKN/web
```

- 基线：U3 第二轮独立验收 PASS 的 scoped 快照 `/tmp/u3-reaccept.2VDfNg`。
- 增量：23 个 U4-owned 文件（后端 17、App 3、Web 3），包含两个摘要计数 Repository 依赖。
- U4-owned 清单 SHA-256 摘要：`d7ccde5fbe2afa61191b408d08d547f8cce99463fabeb339e486f319a8dd33d4`。
- 快照与当前工作区 23 个 U4-owned 文件逐一 SHA-256 比较：**0 mismatch**。
- 未引入并行财务、套餐、项目管理、迁移等无关改动。
- U3 ACCEPTED 报告：`docs/acceptance/runs/20261023-unified-object-view-u3-reacceptance/README.md`。

## 三、命令与结果

### 3.1 后端

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-22.jdk/Contents/Home \
mvn -o -q clean test \
  -Dtest=ContractFactTraceTest,ContractFactPropertiesTest,BusinessFactContractTest,QuoteViewContextTest,QuoteUnifiedViewPropertiesTest,CustomerJourneyChangeSectionTest,CustomerJourneyOwnerPreviewModeTest,DocumentArchiveContextTest,QuotePdfRendererTest

JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-22.jdk/Contents/Home \
mvn -o -q -Dmaven.test.skip=true package
```

结果：

- 9 个测试类：**26/26 PASS**，0 failures / 0 errors / 0 skipped。
- `ContractFactTraceTest` 3/3。
- `ContractFactPropertiesTest` 1/1。
- `BusinessFactContractTest` 5/5。
- U3 相关回归通过，未因 U4 修复回归。
- JDK 22 package：PASS，生成约 7.6 MB JAR。

### 3.2 独立探针

```text
U4FinalAcceptanceProbeTest：3/3 PASS
```

覆盖：

- `validateNode` 接受带明确 uncertainty 的无项目合同上游事实。
- 无 uncertainty、或下游事实 `projectId=null` 均被拒绝。
- 7 类员工角色的成本参数矩阵正确。
- 同一 `completedAt` 下，量房按 `measureId desc` 选中最新记录。

### 3.3 App

```text
npm run type-check          PASS
npm run check:document      PASS
npm run build:mp-weixin     PASS
```

### 3.4 Web

```text
npm run type-check          PASS
npm run check:client-copy   PASS
npm run check:actions       PASS
npm run build               PASS
```

### 3.5 静态审计

```text
U4-owned SQL/迁移文件：无
U4-owned @Lock/PESSIMISTIC/数据权限放宽：无
U4-owned 领域状态变更：无
U4-owned 金额/成本/利润公式变更：无
```

`setStatus` 命中均为 DTO/事实投影赋值或测试 fixture，不是领域状态写入。

## 四、逐项复验

| 验收项 | 结论 | 证据 |
| --- | --- | --- |
| U3 ACCEPTED 前置 | PASS | Registry U3 PASS；`u3-reacceptance/README.md` |
| pre-project 事实接受 | PASS | `validateNode` 调用 `isPreProjectFact`；探针 1/1 |
| 无 uncertainty 拒绝 | PASS | 独立探针断言 IllegalArgumentException |
| 下游空项目拒绝 | PASS | `FactType.Project + projectId=null` 被拒绝 |
| 生产校验调用 | PASS | `BusinessFactContract.java:74` |
| 成本角色矩阵 | PASS | BOSS/店长/财务 true；设计师/PM/营销/TeamLeader false |
| 设计师/PM 无 Cost 事实 | PASS | `ContractFactTraceTest` build(false) 断言不含 Cost |
| `workbench.cost=null` | PASS | `ContractWorkbenchContext` 非授权分支显式 setCost(null) |
| 合同根无 signedCost/costComplete | PASS | `ContractFactPropertiesTest` 覆盖 Workbench/Info/Page |
| 承接标签精确 | PASS | “项目承接（接管基线）” |
| 上游顺序 | PASS | `Customer → Property → Measure → Design → Quote → Contract` |
| 量房排序 | PASS | Completed 过滤；同时间 `measureId desc` 独立探针 PASS |
| 设计/报价版本绑定 | PASS | `contract.quote`、`quote.design`，未取最新 |
| 下游摘要 | PASS | 项目、增减项、材料、验收、应收、成本 |
| App/Web 展示 | PASS（静态/构建） | 上下游事实卡、报价跳统一详情；两端构建 PASS |
| DDL/权限/金额/状态变化 | PASS | 无 U4-owned DDL、无权限放宽、无公式/状态机变更 |

## 五、剩余风险

1. 真实 App/Web 合同页面、真机截图、跨企业/门店/非参与项目负向未执行，留 U6。
2. `completed_at` 数据库列可空；正常 Completed 保存路径会补齐，但历史异常数据的空值顺序仍应由 U6 数据/页面证据覆盖。
3. `validateNode` 是生产契约方法，但当前事实装配路径未对每个生成节点逐一调用它；接口级运行时校验仍应在 U6 响应验收中覆盖。
4. `GAP-053` 非空区域真实数据仍未覆盖。
5. U5 尚未对账和准备；不得因 U4 ACCEPTED 直接启动 U5。

## 六、门禁结论

```text
U1：PASS
U2：PASS
U3：ACCEPTED
U4：ACCEPTED
PI-20261022-010：DEV_IN_PROGRESS
U5：ALLOWED_PREPARE_AFTER_RECONCILIATION / 未启动
U6：未启动
GAP-065：OPEN
```

U4 历史 FAIL 报告保留为历史证据；本报告是 U4 最新独立验收事实源。
