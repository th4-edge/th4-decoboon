# EXPENSE-D1 研发证据

| 项 | 内容 |
| --- | --- |
| 问题 | `PI-20261022-018` |
| 业务裁决 | `DEC-091` / `BR-057` / `ANS-BIZ-20261022-012` |
| 研发交接 | `T12-BIZ-20261022-012` |
| 日期 | 2026-10-23 |
| 状态 | `D1_ACCEPTED_BACKEND_UI_BLOCKED` |
| 结论 | 开发与定向验证通过；D1 后端 HTTP + 隔离 MySQL 已独立验收 PASS；真实 Web/App/OSS 未覆盖 |

## 1. 实现范围

- 后端：费用分类、费用/报销、证据附件、提交版本与详情版本列表、提交/撤回/退回/重提/审批/拒绝、数据范围、防自审、只读动作投影、附件授权。
- App：费用列表、新建/编辑草稿、附件上传、详情与提交版本、审批动作、退回重提、异常恢复。
- Web：费用列表、详情抽屉（含提交版本）、表单抽屉、上传、审批动作、退回重提、异常恢复。
- 未实现：应付、付款、流水、凭证、利润和付款/凭证页面。

## 2. 后端 changed files

新增：

- `src/main/java/com/th4/edge/decoboon/finance/configure/ExpenseStatus.java`
- `src/main/java/com/th4/edge/decoboon/finance/configure/ExpensePaymentMode.java`
- `src/main/java/com/th4/edge/decoboon/finance/configure/ExpenseDocumentType.java`
- `src/main/java/com/th4/edge/decoboon/finance/model/Expense.java`
- `src/main/java/com/th4/edge/decoboon/finance/model/ExpenseCategory.java`
- `src/main/java/com/th4/edge/decoboon/finance/model/ExpenseAttachment.java`
- `src/main/java/com/th4/edge/decoboon/finance/model/ExpenseRevision.java`
- `src/main/java/com/th4/edge/decoboon/finance/repository/ExpenseRepository.java`
- `src/main/java/com/th4/edge/decoboon/finance/repository/ExpenseCategoryRepository.java`
- `src/main/java/com/th4/edge/decoboon/finance/repository/ExpenseAttachmentRepository.java`
- `src/main/java/com/th4/edge/decoboon/finance/repository/ExpenseRevisionRepository.java`
- `src/main/java/com/th4/edge/decoboon/finance/context/ExpenseAccessContext.java`
- `src/main/java/com/th4/edge/decoboon/finance/context/ExpenseCategoryContext.java`
- `src/main/java/com/th4/edge/decoboon/finance/context/ExpenseLifecycleContext.java`
- `src/main/java/com/th4/edge/decoboon/finance/context/ExpenseProjectionContext.java`
- `src/main/java/com/th4/edge/decoboon/finance/service/expenses/**`
- `src/main/java/com/th4/edge/decoboon/finance/service/expensecategories/**`
- `src/test/java/com/th4/edge/decoboon/finance/ExpenseLifecycleTest.java`

修改：

- `src/main/java/com/th4/edge/decoboon/common/file/configure/FileScope.java`
- `src/main/java/com/th4/edge/decoboon/common/file/context/FileAuthorizationContext.java`
- `docs/design/migrations/README.md`
- `scripts/build-release.sh`
- `MIGRATION_END` 上调到 `126`

迁移：

- `docs/design/migrations/126-企业费用与员工报销D1.sql`
- `docs/design/migrations/rollback/126-企业费用与员工报销D1-rollback.sql`

## 3. App changed files

新增：

- `src/domains/expense/model.ts`
- `src/domains/expense/api.ts`
- `src/domains/expense/meta.ts`
- `src/subpackages/finance/pages/expenses.vue`
- `src/subpackages/finance/pages/expense-edit.vue`
- `src/subpackages/finance/pages/expense-detail.vue`
- `src/subpackages/finance/styles/expense.scss`

修改：

- `src/domains/file/api.ts`
- `src/config/index.ts`
- `src/pages.json`
- `src/pages/finance/index.vue`
- `src/pages/mine/index.vue`
- `src/roles/permission.ts`
- `src/roles/guard.ts`

## 4. Web changed files

新增：

- `src/domains/expense/model.ts`
- `src/domains/expense/api.ts`
- `src/domains/expense/meta.ts`
- `src/components/expense/ExpenseFormDrawer.vue`
- `src/components/expense/ExpenseDetailDrawer.vue`
- `src/views/finance/FinanceExpensesView.vue`

修改：

- `src/roles/routes.ts`
- `src/roles/workbenchConfig.ts`
- `src/roles/workbenchHome.ts`
- `src/layouts/RoleAreaShell.vue`
- `src/components/shell/NavIcon.vue`
- `src/domains/file/model.ts`
- `docs/standards/business-state-machines.json`
- `docs/standards/business-action-policies.json`
- `docs/standards/business-permission-policies.json`
- `docs/standards/business-page-tasks.json`
- `docs/standards/business-acceptance-scenarios.json`

## 5. 验证结果

| 层 | 命令 | 结果 |
| --- | --- | --- |
| 后端 | `mvn -Dtest=ExpenseLifecycleTest,MigrationNumberingTest test`（隔离构建目录） | PASS：费用 5 项、迁移 1 项，0 failure |
| App | `npm run type-check` | PASS |
| App | `npm run build:mp-weixin` | PASS |
| App | `npm run check:navigation` | PASS |
| App | `npm run check:client-copy` | BLOCKED：并发改动 `src/subpackages/quote/pages/detail.vue:609` 命中“服务端统一视图”研发型文案；非本次费用文件 |
| Web | `npm run type-check` | PASS |
| Web | `npm run build` | PASS |
| Web | `npm run check:actions` | PASS |
| Web | `npm run check:states` | PASS |
| Web | `npm run check:permissions` | PASS |
| Web | `npm run check:page-system` | PASS |

后端测试覆盖：

- 草稿创建与幂等键；
- 提交版本 v1；
- BOSS/店长审批；
- 店长自批本人报销拒绝；
- BOSS 一人企业自审留痕；
- 财务退回补充、财务审批拒绝、财务自退拒绝；
- 缺证据草稿可保存、提交被拒；
- 员工 Self、跨企业 403/404；
- 项目费用归属与 D1 不创建应付；
- 详情返回提交版本列表，版本快照可回读；
- 状态投影 `allowedActions/nextAction`；
- 迁移编号唯一性。

Web Mock 渲染证据（非真实后端、非独立验收）：

- `th4-decoboon-web/output/playwright/expense-d1/list-1440.png`
- `th4-decoboon-web/output/playwright/expense-d1/list-1280.png`
- `th4-decoboon-web/output/playwright/expense-d1/list-1024.png`
- `th4-decoboon-web/output/playwright/expense-d1/form-1024.png`
- `th4-decoboon-web/output/playwright/expense-d1/detail-1024.png`

## 6. 未执行或未通过

- 未执行迁移到任何真实 dev/test/prod 数据库；仅完成 SQL 与实体/H2 结构验证。
- 未连接真实后端完成 App/Web 全链路写入验收。
- 未做 App 真机/微信开发者工具真实角色走查。
- Web Mock 渲染不能替代真实接口、真实权限和独立 UI 验收。
- App 全量 `verify:mp-weixin` 仍受工作区既有 `check:owner-preview` / `check:ui` 问题阻断；最终 `check:client-copy` 另被并发报价页 `detail.vue:609` 阻断。本次费用文件的类型检查、小程序构建和导航门禁通过。
- 未提交 Git、未发布、未上传体验版。

## 7. 独立验收入口

- `docs/acceptance/runs/20261022-expense-d1/README.md`
- 研发不得自批；独立验收通过前不得把 `PI-20261022-018` 标记 CLOSED。
