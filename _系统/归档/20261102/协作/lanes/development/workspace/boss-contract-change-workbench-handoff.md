# 开发交接：企业负责人增减项工作台

- request_id：`REQ-20260922-BOSS-CONTRACT-CHANGE-WORKBENCH`
- lane：`development`
- 模式：`feature_iteration`
- 状态：`implementation_done_self_tested_pending_independent_acceptance`
- 产品设计：`../../前端代码/th4-decoboon-app/docs/product/2026-09-22-企业负责人增减项工作台产品设计.md`（v1.0 冻结）
- 目标角色：企业负责人（BOSS）；店长/团队负责人/财务按数据范围复用
- 事实来源：`SCN-004`、`DEC-001` 决策三、`MET-002`、`SOL-002`、`BR-002`、`BR-003`、`GAP-003`、`GAP-005`；`2026-09-21-合同增减项业务流程与交互设计.md` v1.2

## 1. 输入依据与范围

用户要求：构建企业负责人视角的增减项工作台。合同签订后施工过程中发生施工项/主材项增减，经设计师与客户协同确认形成清单，由财务/材料员下单，维护增减项清单，重算合同金额、形成合同变更单、新增收款计划、重算项目成本利润。

本轮不改业务状态机、不改金额/成本/利润口径、不新增 DDL：
- 沿用既有链路：发起（逐条/批量）→ 审批 → 客户确认 → 同事务更新合同当前额/项目合同额/应收/施工任务/材料计划/成本重算（`SCN-004`）。
- 新增企业负责人**只读经营聚合 + 复用既有审批/客户确认动作**，把在途变更、材料下单、合同金额与毛利投影放同一屏。
- 项目内现场工作台 `project/pages/change.vue` 保留，不破坏性替换。

## 2. 变更文件

### 后端（新增）

- `src/main/java/com/th4/edge/decoboon/statistic/model/BossContractChangeQuery.java`
- `src/main/java/com/th4/edge/decoboon/statistic/model/BossContractChangeReport.java`
- `src/main/java/com/th4/edge/decoboon/statistic/model/BossContractChangeGroup.java`
- `src/main/java/com/th4/edge/decoboon/statistic/model/BossContractChangeItem.java`
- `src/main/java/com/th4/edge/decoboon/statistic/model/BossContractChangePeriod.java`
- `src/main/java/com/th4/edge/decoboon/statistic/context/BossContractChangeContext.java`
- `src/main/java/com/th4/edge/decoboon/statistic/service/boss/contractchange/report/Report.java`
- `src/main/java/com/th4/edge/decoboon/statistic/service/boss/contractchange/report/support/Report.java`
  - 路由：`statistic/boss/contract-change/report`，继承 `Security`；`@Properties/@AllowProperties` 白名单；无成本权限角色由服务端置空 `costDelta/confirmedCostDelta/projectedGrossProfit/projectedGrossMargin/costComplete/costIncompleteCount`。

### 后端（改造）

- `src/main/java/com/th4/edge/decoboon/project/repository/ContractChangeItemRepository.java`：新增按 changeId 集合批量取明细。
- `src/main/java/com/th4/edge/decoboon/project/repository/MaterialPlanRepository.java`：新增按项目集合批量取材料计划，`@EntityGraph` 预取来源变更行。
- `src/main/java/com/th4/edge/decoboon/finance/repository/ReceivableRepository.java`：新增按项目集合批量取应收。
- `src/test/java/com/th4/edge/decoboon/security/ProviderSecurityAcceptanceTest.java`：路由基线 282→283（public 12 + protected 271）。该文件属 acceptance lane，**需 acceptance 复核**（本次为机械基线同步，precedent 见 `contract-change-p0-handoff.md`）。

### App（新增）

- `../../前端代码/th4-decoboon-app/docs/product/2026-09-22-企业负责人增减项工作台产品设计.md`
- `../../前端代码/th4-decoboon-app/src/subpackages/project/pages/change-workbench.vue`

### App（改造）

- `../../前端代码/th4-decoboon-app/src/domains/workbench/model.ts`：新增 `BossContractChangeReport/Group/Item/Period/Query` 类型。
- `../../前端代码/th4-decoboon-app/src/domains/workbench/api.ts`：新增 `workbenchApi.bossContractChange` + 归一化。
- `../../前端代码/th4-decoboon-app/src/config/index.ts`：新增 `projectChangeWorkbench` 路由常量。
- `../../前端代码/th4-decoboon-app/src/pages.json`：注册 `pages/change-workbench`（分包 89 页）。
- `../../前端代码/th4-decoboon-app/src/pages/project/list.vue`：项目 Tab **顶部**新增「增减项经营工作台」入口，可见角色放宽为 boss/storeManager/teamLeader/finance。
- `../../前端代码/th4-decoboon-app/src/roles/catalog.ts`：工作台首页快捷入口新增「增减项」（boss/storeManager/teamLeader/finance）。
- `../../前端代码/th4-decoboon-app/src/subpackages/project/pages/contract.vue`：合同履约档案「变更（增减项）→ 增减项工作台」在经营角色下跳企业级工作台，其他角色保持项目内工作台。

### App 可发现性修正（2026-09-23）

首次交付后用户反馈「App 里看不到」。定位：原入口只对 `boss` 渲染且位于 4 张指标卡下方，且合同页「增减项工作台」仍跳旧的项目内页面。已修正为三处入口 + 角色放宽，并重新构建验证（type-check / build / subpackages 均 PASS）。若仍看不到，检查：账号角色是否为经营角色、微信开发者工具是否指向最新 `dist/dev/mp-weixin` 或 `dist/build/mp-weixin` 并重新编译、是否在看已上传的体验版/线上包。

## 3. 口径（不新增业务规则）

- 待审批 = `Pending`；待客户确认 = `Approved`；已确认 = `CustomerConfirmed`（`Rejected/Cancelled` 保留可查）。
- 当前合同额 = `ProjectContract.currentAmount`；在途 = Σ(Pending+Approved amount)；预计合同额 = 当前 + 在途（客户已确认金额已含在当前额，不重复累加）。
- 变更成本 = `Σ(ContractChange.costDelta)`；变更后预计成本 = `ProjectCostContext.estimatedCost + 在途及已确认变更成本`（与"全部在途确认后预计合同额"口径一致）。
- 变更后预计毛利 = 预计合同额 − 变更后预计成本；`ProjectCostContext.complete()==false` 或无报价基线时 `null`，App 显示「成本未完整」（`MET-002`/`GAP-003`）。
- 待下单主材：已确认变更中 `itemType=Material` 且材料计划为 `Planned/Requested/Approved` 或未生成计划；`Ordered/PartiallyArrived/Arrived` 记已下单/到场，`Closed` 不计。
- 变更款应收：项目应收中 `stageName=合同变更` 汇总应收/已收/未收/逾期；未生成时 `receivableGenerated=false`，App 显示「尚未生成应收计划」。
- 权限：Provider 在同一 Specification 内用 `ProjectAccessContext.visibleChild` 应用 Company/Store/Team/Self 数据范围；允许角色 Boss/StoreManager/TeamLeader/Finance，其余 403；成本仅 Boss/StoreManager/Finance 可见（现网 `CatalogCostContext` 口径）。
- 动作仍走既有 Provider：`project/contract-change/update`（仅 Boss/Finance 审批）、`customer-confirm`（Boss/PM/Designer）、项目内驳回/撤回沿用；工作台不绕过服务端权限。

## 4. 受影响对象 / Provider / 页面

| 类型 | 变更 |
| --- | --- |
| Provider（新增） | `statistic/boss/contract-change/report` |
| 上下文（新增） | `BossContractChangeContext` |
| 模型（新增） | `BossContractChangeQuery/Report/Group/Item/Period` |
| 仓储（改造） | 变更明细、材料计划、应收批量查询方法 |
| App 领域 | `workbench/model.ts`、`workbench/api.ts` |
| App 页面 | 新增 `change-workbench.vue`；项目 Tab 新增入口 |
| 不做 | 不改状态机/审批链；不新增 DDL；不把变更后预计毛利表述为最终财务利润；不替代 `change-detail`；不删除项目内 `change.vue` |

## 5. Verification

- 后端：`mvn -q -DskipTests compile` **PASS**（多次）。
- 后端 Spring 上下文 + 安全清单：`mvn test -Dtest=ProviderSecurityAcceptanceTest` **PASS（3 tests / 0 failures / 0 errors，BUILD SUCCESS）**。该用例加载完整 `Application` 上下文，验证新 Provider 注册、路由基线 283（public 12 + protected 271）与新增仓储派生查询可用。
- 环境说明：执行中一度受并发会话清理 `target/classes` 影响，连续出现 `SnowflakeIdGenerator` / `WorkerPrivacyContext` / `PlatformDepartment` 随机缺类（`ECD-010` 同源），并在并发构建结束后重跑通过；非代码缺陷。
- App：`npm run type-check` **PASS**。
- App：`npm run build:mp-weixin` **PASS**（`Build complete`）。
- App：`npm run check:subpackages` **PASS**（主包 625071 B / 最大分包 364864 B / 总包 2139477 B；主包 13 页，分包 14 个 / 89 页）。

## 6. Risks

1. **后端业务用例仍需 acceptance 补**：`ProviderSecurityAcceptanceTest` 已验证上下文与路由，但数据范围、成本裁剪、状态过滤、材料下单口径、利润完整性、审批/客户确认联动尚无独立用例；需在无并发构建时执行定向/全量测试。
2. **变更后动态毛利仍是读时投影**：`ProjectCostContext` 尚未把已确认变更成本纳入统一聚合（`GAP-5`），本工作台用「`estimatedCost + 已确认变更成本`」在只读层补足；P1 应下沉到 `ProjectCostContext`，避免各页面重复口径。
3. **履约结果聚合未做**：变更单详情的任务/材料/应收联动结果仍是 `GAP-3`，工作台只汇总材料计划状态与变更款应收，不展开每张变更的派生对象清单。
4. **变更成本完整性无独立标记**：`ContractChange.costDelta` 为非空列，工作台以 `ProjectCostContext.complete()` 作为项目级完整性门槛；若出现「主材成本未录但记为 0」的脏数据，需 acceptance 用真实夹具验证是否会被误判为完整。
5. **并发基线风险**：开发期间检测到 `EmployeeRole` 被另一会话移除 `ProjectSupport`、`CatalogCostContext` 同步调整；本工作台已按当前枚举适配（允许角色 Boss/StoreManager/TeamLeader/Finance）。合并前需再确认路由基线与角色枚举无二次改写。
6. **项目 Tab 入口仅 BOSS 显示**：店长/财务可用 Provider，但 App 暂无各自入口；P1 按门店/财务角色补入口与门店筛选（`BR-APP-035`）。

## 7. Next Action

1. acceptance 在无并发构建时补跑定向/全量测试，并覆盖数据范围、成本裁剪与状态迁移用例。
2. acceptance 设计独立用例：数据范围、成本裁剪、状态过滤、材料下单口径、利润完整性、审批/客户确认联动。
3. P1：把已确认变更成本纳入 `ProjectCostContext`（`GAP-5`）并补变更单履约结果聚合（`GAP-3`）。
4. P1：店长/财务入口与门店筛选。

## 8. Acceptance Needed

- 正向：BOSS 打开工作台，4 指标/日期窗口/项目分组/变更单与真实变更单一致；Pending 可审批；Approved 可登记客户确认；确认后合同当前额、变更款应收、材料计划、成本投影同事务更新；材料 `Ordered` 前计待下单、后移出；成本完整时预计毛利 = 合同投影 −（预计成本+已确认变更成本）。
- 反向：非经营角色 403；TeamLeader/设计师/业务员响应无成本字段；跨企业/跨门店/跨团队不可见；变更后合同额 <0 被拒；重复客户确认不重复生成应收/任务/材料；已确认不可编辑/撤回；Rejected/Cancelled 仍可查。
- 异常：成本不完整、无合同、无应收计划分别显示「成本未完整」「无生效合同」「尚未生成应收计划」；接口失败显示「增减项状态暂不可确认」+ 重试，不用 0 或旧值。
- 状态投影：初次进入、审批成功、客户确认成功、接口异常、可选完成、必做未完成、撤回/回退、重复进入八种情况均有明确状态与恢复路径。
