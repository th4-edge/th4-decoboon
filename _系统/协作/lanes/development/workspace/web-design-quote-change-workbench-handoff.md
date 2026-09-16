# Web 设计、报价、增减项高频录入闭环开发交接

## 输入依据

- 用户要求：Web 是设计师主要操作渠道，需支持设计、报价、增减项，并充分考虑 Excel 迁移用户的连续录入效率。
- 现有事实源：Web v2 全业务体系、报价快照规则、合同变更状态机和 development lane 写权限。
- 设计约束：服务端继续负责企业归属、数据范围、价格和金额计算；前端不传递可信 `companyId/empId/ownerId`。

## 本次输出

### 1. 设计方案 Excel 式工作台

路径：`/design/schemes/:id/workbench`

- 设计方案列表点击名称或双击行可直接进入。
- 支持从 Excel 粘贴多行：推荐 `区域、项目名称/编码、数量、备注`。
- 支持新增、复制、删除行、区域快捷新增、Enter 连续跳格、Ctrl/⌘+S 保存。
- 项目名称/编码与企业报价项目库匹配，价格由项目库自动带出，不允许前端手工改价。
- 保存后重新读取服务端汇算金额；可直接设置折扣并一键生成报价快照。
- 支持导出当前方案分项 CSV，便于继续在 Excel 中处理。

### 2. 报价操作闭环

路径：`/trade/quotes/:id`

- 草稿报价可回到关联设计方案工作台调整。
- 支持导出报价分项 CSV。
- 报价金额仍为服务端快照；调整后通过方案重新生成报价版本，符合服务端算价和状态机约束。

### 3. 增减项批量工作台

路径：`/projects/:id/contract-changes/new`

- 项目详情新增“合同增减项”页签和“发起增减项”入口。
- 支持增项、减项、替换、数量变更、价格变更。
- 支持从 Excel 粘贴多行：推荐 `项目名称、规格、金额、成本、说明`。
- 增项自动按正数、减项自动按负数入账；页面实时显示净变更、成本变化和变更后预估合同额。
- 多行一次提交为一张合同变更单，后端在同一事务内保存变更单和全部明细。
- 项目详情可展开查看明细，企业负责人/财务可审批通过或驳回。
- 合同变更列表页保留单行变更入口，作为非批量场景的兜底。

## 后端接口

- 新增 `project/contract-change/batch-save`。
- Provider 继承 `Security`，使用 `@User` 注入当前员工。
- 通过可见合同和项目访问上下文校验数据范围，并复用 `requirePlanEditor` 做操作权限校验。
- 使用 `List<ContractChangeItem>` 实体作为明细入参，不新增 DTO/VO。
- 服务端重新计算汇总金额和成本，固定企业、门店、团队、负责人归属。
- 返回具体 `ContractChange` 实体，并通过 Provider 级 `@Properties` 裁剪响应。

## 变更文件

### 后端

- `src/main/java/com/th4/edge/decoboon/project/service/contractchange/batchsave/BatchSave.java`
- `src/main/java/com/th4/edge/decoboon/project/service/contractchange/batchsave/support/BatchSave.java`
- `_系统/协作/lanes/development/workspace/provider-security-scan.csv`
- `_系统/协作/lanes/development/workspace/provider-security-scan.md`

### Web

- `../../前端代码/th4-decoboon-web/src/composables/spreadsheet.ts`
- `../../前端代码/th4-decoboon-web/src/domains/design/model.ts`
- `../../前端代码/th4-decoboon-web/src/domains/design/api.ts`
- `../../前端代码/th4-decoboon-web/src/domains/trade/api.ts`
- `../../前端代码/th4-decoboon-web/src/resources/types.ts`
- `../../前端代码/th4-decoboon-web/src/resources/index.ts`
- `../../前端代码/th4-decoboon-web/src/router/index.ts`
- `../../前端代码/th4-decoboon-web/src/views/design/DesignSchemeWorkbenchView.vue`
- `../../前端代码/th4-decoboon-web/src/views/project/ContractChangeCreateView.vue`
- `../../前端代码/th4-decoboon-web/src/views/project/ProjectDetailView.vue`
- `../../前端代码/th4-decoboon-web/src/views/trade/QuoteDetailView.vue`
- `../../前端代码/th4-decoboon-web/src/views/resources/ResourceListView.vue`

## Verification

- Web：`npm run type-check` 通过。
- Web：`npm run verify:system` 通过，31 个资源页、40 个导航入口、8 类角色校验通过。
- Web：`npm run build` 通过。
- 后端：`mvn -q -DskipTests compile` 通过。
- 后端：完整回归通过（排除仅需更新路由数量常量的 `ProviderSecurityAcceptanceTest`）。
- 浏览器：已使用 `18660219918` 登录 `http://127.0.0.1`，完成目录维护、设计 Excel 粘贴、生成报价、提交/接受报价、签约、合同生效立项、增减项录入/审批/客户确认、项目完工验收及财务页面验证。
- 测试证据：`data/design/maihong-web-closed-loop-result.json`。
- 后端全量测试：79 tests / 1 failure。失败项为 `ProviderSecurityAcceptanceTest` 仍断言总路由数 196，实际为 198；其中包含既有 `file/private/upload` 和本次新增 `project/contract-change/batch-save`。开发侧扫描清单已更新为 `total=198 protected=187 public=11 violations=0`。

## Risks

- 已完成 Boss 账号真实浏览器联调；其余 7 类角色的数据权限和关键动作仍需 acceptance 复验。
- 设计方案分项保存仍逐行调用现有 `design/item/save`；批量场景中若中途失败，已成功的行会保留，未成功的行会显示错误，不会静默丢失。
- 已确认 Excel 多行粘贴、保存和生成报价在 Boss Web 会话中可用。
- 增减项批量接口当前以显式金额、成本变化为主，未在 UI 中选择原合同行；替换、数量和价格变更已经可以录入金额影响，但原合同行级追溯仍可后续增强。
- Web 主入口 Element Plus chunk 约 916 KB 的既有性能风险未在本任务处理。

## 本轮浏览器测试发现并修复

- 客户确认增减项响应约 14 MB：补齐 `ContractChangeItem/ContractLine/ProjectContract/Project/Property/Employee` 白名单后约 1.2 KB。
- 合同变更履约新增施工阶段、任务、材料计划时缺少 `property`，补齐房屋归属。
- 锁开关关闭时 `RedissonLock` 仍强制注入 `RedissonClient`，改为按锁开关条件注册。
- 报价状态推进后覆盖轻量响应导致详情字段消失，改为推进后重新读取完整报价。
- 设计方案列表金额字段不存在而显示 ¥0，改为真实方案面积；项目生效后工作台显示锁定提示。
- 设计项目库只加载前 100 条，改为分页加载全部目录。
- 通用资源编辑表单将空字段错误填入 `row.id`，并修复目录更新缺少 `itemId`。
- 财务“万”单位、百分比重复放大、账户枚举、应收/应付状态中文化已修复。

## Next Action

- acceptance lane 将 `ProviderSecurityAcceptanceTest` 的期望路由数更新为 198 / 187 / 11，并新增批量变更接口的权限和事务测试。
- 使用其余 Designer、StoreManager、TeamLeader、Marketer、Pm、ProjectSupport、Finance 账号验证数据范围、菜单和关键动作。
- 产品侧确认增减项是否必须强制选择原合同行；若必须，再设计合同行选择器和来源行快照规则。

## Acceptance Needed

- 设计：Excel 粘贴匹配、连续录入、区域归属、金额自动带出、保存后服务端汇算。
- 报价：草稿报价回改设计、重新生成版本、CSV 导出字段完整。
- 增减项：多行一次提交、正负金额、企业/项目数据范围、跨企业伪造、非编辑角色越权、审批状态机。
