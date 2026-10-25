# 统一对象视图一期 U4 · 合同事实追踪独立验收

| 项 | 内容 |
| --- | --- |
| 问题 | `PI-20261022-010` 统一对象视图一期实施 |
| 验收切片 | `U4` 合同事实追踪（合同上游 + 下游摘要） |
| 日期 | 2026-10-23 |
| 角色 | 独立验收方（只读审查，不修改实现） |
| 前置 | U1/U2 独立验收 PASS；当前 issue/registry 显示 U3 `REMEDIATED_PENDING_REACCEPTANCE`，尚未 PASS |
| 结论 | **FAIL / U4 BLOCKED** |
| 问题状态 | `DEV_IN_PROGRESS`（不得 `CLOSED`） |
| U4 技术状态 | `DEV_VERIFIED`（仅 scoped 开发证据） |
| U5 门禁 | **BLOCKED** |

## 一、结论

U4 的核心事实链装配已通过 scoped 后端测试和 App/Web 构建，但本轮不能判 PASS：

1. 当前 issue/registry 明确记录 U3 尚未完成独立复验，U4 依赖 U3 PASS；前置条件不满足。
2. U4 下游 `Cost` 事实无条件携带金额并进入合同工作台响应；Web 设计师合同交接直接读取和渲染该事实，未按 BOSS/店长/财务裁剪成本。
3. U4 的承接来源摘要使用 `BusinessOrigin.getName()`（“接管基线”），未使用冻结标签“项目承接（接管基线）”。
4. 合同尚未生成项目时，上游节点的 `projectId` 为 `null`，未满足 U1 统一事实节点的项目绑定契约或明确豁免。

其中第 1 项是硬门禁，第 2 项是权限阻断；因此 U4 不能标记 `ACCEPTED`，U5 保持 BLOCKED。

## 二、隔离快照

```text
后端：/tmp/uov-u4-backend.BjevXx/repo
App ：/tmp/uov-u4-app.UJm5Dq/repo
Web ：/tmp/uov-u4-web.iz5i06/repo
```

后端快照从 `HEAD` 建立并复制 U1/U2/U3/U4 相关源码、pack/finance/material/supply 依赖闭包；App/Web 快照从各自 `HEAD` 建立并复制 U4 及 U3 当前消费依赖。未直接使用共享工作区 `target/` 或 `dist/`。

## 三、命令与结果

### 后端

```bash
mvn -o -q -Dtest=ContractFactTraceTest,QuoteViewContextTest,QuoteUnifiedViewPropertiesTest,CustomerJourneyChangeSectionTest,CustomerJourneyOwnerPreviewModeTest test
mvn -o -q -DskipTests compile
```

结果：

- 后端定向测试：**14/14 PASS**，0 failures / 0 errors / 0 skipped
- `ContractFactTraceTest`：2/2 PASS
- `mvn -o -q -DskipTests compile`：PASS
- U4-owned `git diff --check`：PASS
- U4-owned SQL/迁移扫描：无 SQL/迁移

### App

```text
npm run type-check                         PASS
npm run check:document                     PASS
npm run build:mp-weixin                    PASS
```

### Web

```text
npm run type-check                         PASS
npm run build                              PASS
```

构建中的 Sass deprecation warning 为既有告警，不是 U4 失败。

## 四、U4 已验证通过项

- `ContractFactTraceTest` 验证上游顺序为：
  `Customer → Property → Measure → Design → Quote → Contract`。
- 量房仅取 Completed，按 `completedAt desc, measureId desc`；区域数/实测数/待补数由 MeasureRegion 计算。
- 合同沿 `ProjectContract.quote`、`quote.design` 绑定设计/报价版本，未取最新报价或最新设计。
- 无设计时只增加“无设计方案来源”来源缺口，不新增来源字段或统计隔离。
- 下游事实包含项目、增减项、材料计划、验收、应收、成本摘要。
- App 合同页与 Web 合同交接均展示上游/下游事实卡；报价节点可跳统一报价详情。
- 未新增 U4-owned DDL、迁移、权限放宽、金额公式或状态机变更；U5 PHASE 未实现、未冒充。

## 五、阻断项

### P1-01 · U3 前置门禁未满足

当前 `PI-20261022-010` issue/registry 状态为：

```text
U3: REMEDIATED_PENDING_REACCEPTANCE
U4: BLOCKED
```

U3 仍存在金额缺失补 0/重算、分组缺失伪空和合同读取失败伪状态等开放项。U4 不得在前置 U3 重新独立验收 PASS 前启动或标记 ACCEPTED。

### P1-02 · 下游成本事实未按角色裁剪

- `ContractWorkbenchContext.facts` 对任何存在 Project 的合同无条件加入 `FactType.Cost`，并附带 `contract.getSignedCost()`。
- `Workbench` 的 `@Properties` 白名单包含 `BusinessFactNode.amount`，没有角色过滤。
- `DesignerContractHandoffView` 调用 `contractFactTrace` 并对 downstreamFacts 直接渲染 `fact.amount`。
- 这违反 DEC-086 “成本/成本完整性仅 BOSS、店长、财务可见；设计师/PM 不因统一视图获得成本权限”。

## 六、P2 / 风险

- 承接标签使用 `BusinessOrigin.getName()` 得到“接管基线”，未使用冻结文案“项目承接（接管基线）”；应增加承接场景断言。
- 合同尚未生成项目时，上游事实 `projectId` 可能为 `null`；需明确来源事实豁免并在节点校验/查询契约中表达，不能静默依赖前端忽略。
- `ContractFactTraceTest` 只覆盖有 Project 的 fixture；未覆盖无 Project、承接来源、成本角色隔离和事实链读取失败。
- 未执行真实登录、真实合同页面、跨企业/门店/非参与项目负向；真实 UI 和权限证据留 U6。
- `GAP-053` 真实非空区域数据仍未覆盖。

## 七、门禁结论

```text
U1：PASS
U2：PASS
U3：REMEDIATED_PENDING_REACCEPTANCE（当前 issue/registry）
U4：FAIL / BLOCKED
PI-20261022-010：DEV_IN_PROGRESS（不得 CLOSED）
U5/U6：BLOCKED
GAP-065：OPEN
```
