# 企业负责人首页改造开发交接

## Input

- 用户确认按以下结论改造：四项经营指示灯、六个高频功能域、“我的”只负责治理与配置、首页项目只展示风险项目、最新动态拆分为经营动态与工地同频动态。
- 产品设计：`../../前端代码/th4-decoboon-app/docs/product/2026-09-20-企业负责人首页产品设计.md`。
- 本次没有新增表或迁移。

## Backend

### Provider

- 新增受保护路由：`statistic/boss/workbench`。
- 仅允许 BOSS 调用，企业范围从登录员工读取。
- 请求：`month`、`eventLimit`。
- 响应：`asOf`、`month`、`indicators`、`attentionProjects`、`businessEvents`。

### Changed Files

- `src/main/java/com/th4/edge/decoboon/statistic/service/boss/workbench/Workbench.java`
- `src/main/java/com/th4/edge/decoboon/statistic/service/boss/workbench/support/Workbench.java`
- `src/main/java/com/th4/edge/decoboon/statistic/context/BossWorkbenchContext.java`
- `src/main/java/com/th4/edge/decoboon/statistic/context/BusinessEventContext.java`
- `src/main/java/com/th4/edge/decoboon/statistic/model/BossWorkbench.java`
- `src/main/java/com/th4/edge/decoboon/statistic/model/BossWorkbenchQuery.java`
- `src/main/java/com/th4/edge/decoboon/statistic/model/WorkbenchIndicator.java`
- `src/main/java/com/th4/edge/decoboon/statistic/model/WorkbenchProject.java`
- `src/main/java/com/th4/edge/decoboon/statistic/model/BusinessEvent.java`
- `src/main/java/com/th4/edge/decoboon/statistic/configure/WorkbenchSignalStatus.java`
- `src/main/java/com/th4/edge/decoboon/statistic/configure/BusinessEventCategory.java`
- `src/main/java/com/th4/edge/decoboon/statistic/configure/BusinessEventType.java`
- `src/main/java/com/th4/edge/decoboon/statistic/configure/BusinessEventLevel.java`
- 客户、跟进、报价、合同、收款、付款、验收和合同变更仓储增加最近 200 条读取方法，避免首页扫描全部历史。

### Business Rules

- 四盏灯固定为签约额、回款额、项目毛利、在建项目。
- 毛利成本不完整时返回 `null` 和 `unknown`，前端显示“成本未完整”。
- 风险项目按经营预警的最高级别排序，最多返回 4 个，并给出原因、责任人和下一步。
- 经营动态来自真实领域事实，覆盖客户、报价、合同、变更、项目、验收、材料、收款和付款。
- 经营动态只扫描每类最近 200 条，并限制在最近 180 天。

## App

### Changed Files

- `src/pages/index/index.vue`
- `src/pages/mine/index.vue`
- `src/pages/dynamic/index.vue`
- `src/subpackages/statistic/pages/business-dynamics.vue`
- `src/domains/statistic/model.ts`
- `src/domains/statistic/api.ts`
- `src/config/index.ts`
- `src/pages.json`
- `src/roles/catalog.ts`
- `src/styles/home.scss`
- `scripts/verify-homepage-contract.mjs`
- `package.json`
- `docs/00-README.md`
- `docs/product/2026-09-20-企业负责人首页产品设计.md`

### UX Result

- BOSS 首页四项指标显示主数据、状态灯和健康原因。
- 首页功能域收敛为客户管理、设计调度、方案报价、合同签约、履约驾驶舱、财务总览。
- “我的”移除 BOSS 重复的经营驾驶舱、人员绩效、设计调度、履约驾驶舱和全公司项目入口。
- 首页项目区块改为“需要关注的项目”，无风险时不再复制项目 Tab。
- 首页新增“经营动态”，详情页支持客户、合同、项目、资金分类。
- 经营动态无真实事件时显示明确空态和四个起步动作；某个分类无事件时单独显示分类空态，不使用演示数据填充。
- 原“同频动态”改名为“工地同频动态”，与经营动态区分。

## Verification

- 后端：`mvn -o -q test`，61 suites / 177 tests / 0 failures / 0 errors / 0 skipped。
- 新增 `BossWorkbenchContextTest`、`BusinessEventContextTest`。
- `ProviderSecurityAcceptanceTest` 已同步路由总数 241（protected 229），并验证新路由未登录拒绝。
- App：`npm run verify:mp-weixin` PASS。
- 首页契约：`npm run check:home` PASS。
- 小程序分包：主包 532,672 B，最大分包 186,954 B，总包 1,613,650 B。

## Risks / Blockers

- 尚未使用真实 BOSS 账号和真实业务数据执行微信开发者工具/真机视觉与交互验收。
- 微信开发者工具会直接拒绝 `localhost`、`127.0.0.1` 的 request/download/upload/socket 请求；开发环境必须使用当前电脑局域网 IP 或 HTTPS 域名，并重新编译旧开发进程。
- 经营动态是当前领域事实聚合，不是不可变事件日志；同一业务对象的历史中间状态无法在未持久化事件的情况下完整还原。
- 首页聚合会读取多类最近 200 条记录，真实大数据量下需继续观察响应时间和 SQL 次数。
- 现有工作区含此前未提交改动，本批次未回滚或覆盖这些改动。

## Acceptance Needed

是。acceptance lane 应按产品文档第 10 节执行 HOME-01 至 HOME-10，并重点复验：

1. 四盏灯与经营驾驶舱、财务页、项目页数据一致。
2. 风险项目与经营预警一致且不重复。
3. 经营动态关键事件、金额、时间、负责人和跳转对象一致。
4. 接口异常时四盏灯和动态不会沿用旧值冒充最新事实。
5. “我的”调整后所有原功能仍可从唯一主入口到达。

## Next Action

- 在微信开发者工具中使用真实 BOSS 账号加载首页和经营动态页。
- 本地开发启动统一执行 `npm run dev:mp-weixin`；该命令会先校验 `.env` 的 `VITE_API_BASE`，禁止 localhost/127.0.0.1。
- 构造成本不完整、逾期应收、停工项目、收款和合同签约事件，执行 HOME-01 至 HOME-10。
- 若验收通过，再由 product-planning/acceptance 决定是否纳入下一发版窗口。
