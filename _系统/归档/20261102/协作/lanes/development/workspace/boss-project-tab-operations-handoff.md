# 开发交接：企业负责人「项目」Tab 运营视角改造

- 批次 ID：`REQ-BIZ-20260921-PROJECT-TAB-OPS`
- 模式：`feature_iteration`
- 日期：2026-09-21
- 目标角色：企业负责人（BOSS）；其他角色保持原施工列表
- 事实来源：`AGENTS.md`、`rules.md`、`DEC-001`、`BR-002/003/004`、`MET-002`、`GAP-003`、`GAP-005`；现有 `Project / ProjectContract / Receivable / ProjectCostContext / BusinessAlertContext`
- 产品设计：`../../前端代码/th4-decoboon-app/docs/product/2026-09-21-企业负责人项目Tab运营视角产品设计.md`（v1.1，4 项用户裁决见 §15）
- 需求登记：`BR-APP-035`（门店维度全局铺开）/ `BR-APP-036`（项目 Tab 指标与毛利口径）/ `APP-DATA-004`

## 1. 输入依据

- 用户要求：企业负责人「项目」Tab 应按企业运营视角展示，先做产品 UI 设计梳理，再做功能代码研发，交互水平对齐既有「报价谈单」页面。
- 现状：`pages/project/list.vue` 只有施工进度视角（封面、名称、社区/面积、设计/经理、进度、当前阶段、合同额），负责人看不到盘子、钱、风险和利润。
- 收敛参照：`pages/customer/list.vue`（企业负责人客户经营 Tab）的「服务端聚合指标卡 + 状态分段 + 经营卡片」结构。

## 2. 业务问题与结论

负责人打开项目 Tab 必须能回答五件事：盘子（在建规模）、钱（应收/已收/逾期）、交付（阶段/进度/工期）、风险（等级/原因/下一步）、利润（成本完整性 + 动态毛利）。

R1 因果：项目 Tab 缺少经营结论层，导致负责人要在客户、合同、项目、财务四个页面之间手工拼装事实。
R2 条件：金额与风险必须复用既有来源（合同当前金额、应收计划、`ProjectCostContext`、`BusinessAlertContext`），不新增第二套口径；成本不完整时毛利必须为空。
R3 体验：BOSS 分支走「指标带 → 经营排序 → 状态分段 → 经营卡片」，其他角色走原渲染分支，不泄漏经营字段。

## 3. 受影响对象 / Provider / 页面

| 类型 | 变更 |
| --- | --- |
| Provider（新增） | `project/project/overview`（继承 `Security`，企业负责人经营聚合） |
| 上下文（新增） | `project/context/ProjectOverviewContext.java` |
| 模型（新增） | `ProjectOverviewReport`（含 `contractCounts`）、`ProjectMetricCard`（含 `trendRate`）、`ProjectStatusCount` |
| 枚举（新增） | `project/configure/ProjectRiskLevel`、`ProjectRiskType`、`ProjectContractStage` |
| 实体投影 | `project/model/Project.java` 新增 11 个 `@Transient` 只读投影字段 |
| 枚举复用 | `statistic/configure/BusinessAlertType#nextAction()`（首页风险项目与项目台账共用同一处置建议文案） |
| App 领域 | `domains/project/model.ts`、`api.ts`、`meta.ts` |
| App 页面 | `pages/project/list.vue`（新增 BOSS 运营分支） |
| 安全清单 | `src/test/java/com/th4/edge/decoboon/security/ProviderSecurityAcceptanceTest.java` |
| 单测（新增） | `src/test/java/com/th4/edge/decoboon/project/ProjectOverviewContextTest.java` |

## 4. 口径（不新增业务规则）

- 在建：`Preparing / Ongoing / Suspended / Accepting`；`Finished / Settled` 不计入在建；「已完工」分段同时覆盖 `Finished` 与 `Settled`。
- 合同当前金额：`ProjectContract.currentAmount`，为 0 时回退 `baselineAmount`，无合同时保留 `Project.contractAmount`。
- 应收 / 已收：按项目对既有 `Receivable.amount / receivedAmount` 求和；逾期口径与 `BusinessAlertContext` 一致（非已收清/已关闭，且状态为逾期或到期日早于观察日）。
- 回款率：已收 / 应收 × 100；未生成应收计划时显示「尚未生成应收计划」，不使用伪 0。
- 风险：完全复用 `BusinessAlertContext`（逾期应收 / 停工与工期逾期 / 材料超领 / 变更率异常），按项目去重，`High → 高风险`、`Medium/Low → 需关注`。
- 毛利：`ProjectCostContext.summarize(project)`；`complete()==false` 时 `grossProfit=null`，App 显示「成本未完整」，禁止 0 成本伪利润（`GAP-003`）。
- 权限：Provider 使用 `TenantContext.requireBoss`；项目集合按登录员工数据范围过滤；App 仅 BOSS 调用该接口。

### 4.1 用户裁决落地（2026-09-21）

| 裁决 | 落地 |
| --- | --- |
| 第四张指标卡用「本月新签项目」 | 指标卡固定为 在建项目 / 在建合同额 / 回款率 / **本月新签项目**（`newSignedProject`，含上月对比、本月签约额与环比；上月为 0 且本月有值时环比为空，不制造无穷增长）。风险不再单独占指标卡，改为项目卡风险条 + 「风险优先」默认排序 |
| 门店筛选条本轮不做、先记录需求、后期全部增加门店概念 | 登记 `BR-APP-035` / `APP-DATA-004`；本轮 `storeId` 仍只作预留入参，页面不铺筛选条 |
| 毛利行按实际成本口径展示 | `Project.actualCost` 投影（`ProjectCostContext.actualCost()`）；卡片显示 实际成本 · 毛利 · 毛利率；成本不完整时只显示「成本未完整」 |
| 滚动时中间项目卡高亮 | 补「滚动居中卡片高亮」：视口居中附近项目卡获得品牌色边框 + 品牌色阴影；锚点绑定当前查询维度实际渲染的列表（`displayedProjects`），维度切换 / 分段筛选 / 排序 / 下拉刷新 / 分页追加统一通过 `watch + nextTick + createSelectorQuery` 重测，高亮不会停留在已消失的卡片上；风险态卡片居中时品牌色优先，风险信息由风险条承载 |
| 增加「合同履约中 / 已完成」分段 | 新增合同维度分段，采用「按项目状态 / 按合同履约」维度切换（复用报价详情同款交互）；`ProjectContractStage`：`pending 待签约` / `active 履约中` / `completed 已完成`；报告新增 `contractCounts` |

## 5. changed_files / output_paths

后端：
- `src/main/java/com/th4/edge/decoboon/project/service/overview/Overview.java`
- `src/main/java/com/th4/edge/decoboon/project/service/overview/support/Overview.java`
- `src/main/java/com/th4/edge/decoboon/project/context/ProjectOverviewContext.java`
- `src/main/java/com/th4/edge/decoboon/project/model/ProjectOverviewReport.java`
- `src/main/java/com/th4/edge/decoboon/project/model/ProjectMetricCard.java`
- `src/main/java/com/th4/edge/decoboon/project/model/ProjectStatusCount.java`
- `src/main/java/com/th4/edge/decoboon/project/configure/ProjectRiskLevel.java`
- `src/main/java/com/th4/edge/decoboon/project/configure/ProjectRiskType.java`
- `src/main/java/com/th4/edge/decoboon/project/configure/ProjectContractStage.java`
- `src/main/java/com/th4/edge/decoboon/project/model/Project.java`（只读投影字段）
- `src/main/java/com/th4/edge/decoboon/statistic/configure/BusinessAlertType.java`（`nextAction()`）
- `src/main/java/com/th4/edge/decoboon/statistic/context/BossWorkbenchContext.java`（改为委托 `nextAction()`，行为不变）

App：
- `src/domains/project/model.ts`、`src/domains/project/api.ts`、`src/domains/project/meta.ts`（`CONTRACT_STAGE_META`）
- 需求记录：`../../前端代码/th4-decoboon-app/docs/plan/APP业务闭环改造待研发功能记录表-20260920.md`（v2.1）
- `src/pages/project/list.vue`

测试与文档：
- `src/test/java/com/th4/edge/decoboon/project/ProjectOverviewContextTest.java`
- `src/test/java/com/th4/edge/decoboon/security/ProviderSecurityAcceptanceTest.java`
- `../../前端代码/th4-decoboon-app/docs/product/2026-09-21-企业负责人项目Tab运营视角产品设计.md`

## 6. verification

- 后端 `mvn -o test -Dtest='ProjectOverviewContextTest,ProviderSecurityAcceptanceTest,BossWorkbenchContextTest,DetailResponseWhitelistContractTest'`：4 套 12 个用例全绿（`ProjectOverviewContextTest` 4/4：四张指标卡与 `newSignedProject`、双维度分段、实际成本毛利、排序与分页）；`ProviderSecurityAcceptanceTest` 3/3 PASS（路由清单含 `project/project/overview`，总数 273 = public 12 + protected 261）。
- 后端 `mvn -o -q -DskipTests compile` PASS。
- App `npm run verify:mp-weixin` PASS（`type-check`、`build:mp-weixin`、`check:subpackages`）；构建体积：主包 594,603 B，最大分包 286,590 B，总包 1,995,982 B。
- 静态设计预览：`/Users/zhangwei/.codex/visualizations/2026/09/20/01a0c053-559e-7283-8add-3250f918e0bb/project-tab-full.png`（375×2600 高保真排版稿，供 UI 评审，不是运行时截图）。

## 7. risks

- 本轮 BOSS 分页每页最多 100 条，成本汇总按当前分页命中项目计算；企业项目规模显著增大后需要改为分类批量汇总。
- 风险等级聚合沿用 `BusinessAlertContext` 的每类 50 条上限，极端数据量下风险条数可能低于真实值。
- 门店筛选按 `BR-APP-035` 全局铺开后再统一实现；当前 `storeId` 只作预留入参，且按客户门店口径过滤（与 `project/project/page` 一致）。
- 「已完成」合同分段合并了 已完工/已结算/已关闭/已终止 四种已结束履约的合同状态；「待签约」覆盖无合同或草稿/待签约合同的项目，避免分段计数与「全部」不闭合。
- 本轮未做真实 BOSS 会话下的微信开发者工具回归；指标与首页风险项目、财务页的口径一致性待独立验收。
- 仓库内存在并行开发会话（主材选材 / 合同履约档案），本轮验证期间出现过 `target/` 被并发 `mvn` 清理导致的 `NoClassDefFound` 假失败；已通过重新编译确认本模块测试通过，全量套件需在没有并发构建时复跑。

## 8. next_action

1. 独立 acceptance：真实 BOSS 账号验证 PROJ-01 ~ PROJ-13（含跨企业、伪造 storeId、接口失败、成本不完整、分页）。
2. 与首页风险项目、财务项目页做同口径回归，确认指标卡与首页指示灯一致。
3. 微信开发者工具多机型检查指标带换行、叠字与长文案（风险原因 / 下一步）折行。
4. 无（设计文档 §15 的 4 项裁决已于 2026-09-21 闭环；门店概念按 `BR-APP-035` 另行排期）。

## 9. acceptance_needed

- 角色：BOSS、店长、项目经理、设计师、业务员、客户。
- 数据状态：正常项目、停工项目、逾期应收项目、材料超领项目、变更率异常项目、成本不完整项目、无项目企业。
- 失败路径：概览接口失败、分页失败、非 BOSS 调用、跨企业/跨门店访问。
