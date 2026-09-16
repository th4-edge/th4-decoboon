# 装得清运营管理端开发交接

## 输入依据

- 用户要求：构建 `th4-decoboon-admin`，支持全平台企业查询、人工订阅续费/套餐/席位/门店管控、营销推广运营支持、系统负载与异常监控。
- 工程规范：`AGENTS.md`、`rules.md`、`docs/product/00-装得清产品与定价白皮书.md`。
- 现有模型：`Company`、`Employee`、`Subscription`、`Plan`、`PlanVersion`、`Order`、`Credentials`。

## Output Paths

### 前端

- `../../前端代码/th4-decoboon-admin/`
- 页面：运营总览、客户企业、订阅与续费、增长运营、套餐策略、系统运维、操作审计。
- 生产构建：`../../前端代码/th4-decoboon-admin/dist/`。

### 后端

- `src/main/java/com/th4/edge/platform/context/PlatformAdminContext.java`
- `src/main/java/com/th4/edge/platform/context/SystemMonitorContext.java`
- `src/main/java/com/th4/edge/platform/model/**`
- `src/main/java/com/th4/edge/platform/repository/PlatformOperationLogRepository.java`
- `src/main/java/com/th4/edge/platform/service/**`

### 数据库和文档

- `docs/design/migrations/24-运营管理端基础.sql`
- `docs/design/migrations/01-空库基线.sql`
- `docs/product/装得清运营管理端业务闭环设计.md`
- `docs/plan/2026-09-16-运营管理端实施基线.md`
- `docs/operations/装得清运营管理端运行手册.md`
- `docs/acceptance/装得清运营管理端验收清单.md`

## Changed Capabilities

- 平台权限：`Company.platform=true` + `EmployeeRole.Boss` 才允许访问 `platform/*`。
- 全企业查询：`platform/tenant/page`、`platform/tenant/info`。
- 运营总览：企业、订阅、回款、风险、漏斗、套餐分布。
- 人工订阅调整：续费、套餐、席位、门店、状态。
- 审计日志：操作人、目标企业、动作、前后值、原因、时间。
- 系统监控：OSHI、JVM、磁盘、MySQL、Redis、应用进程、运行时长。
- 前端只连接真实 `th4-decoboon` Provider，无 mock、演示账号或断网回退。

## Verification

- `th4-decoboon-admin/npm run verify:real-api`：PASS。
- `th4-decoboon-admin/npm run type-check`：PASS。
- `th4-decoboon-admin/npm run build`：PASS。
- 浏览器：登录后 7 个路由全部渲染，无 error/warning console log；订阅调整对话框和真实 Provider 提交链路已验证。
- `th4-decoboon/mvn -o -q clean test`：29 suites / 84 tests / 0 failures / 0 errors。
- `PlatformAdminAcceptanceTest`：PASS。
- `ProviderSecurityAcceptanceTest`：PASS，路由总数 205（public 11 + protected 194）。

## Risks

- 生产 `ddl-auto=validate`，上线前必须先执行迁移 `24-运营管理端基础.sql` 并将平台运营企业 `platform` 设置为 true。
- 当前管理端是独立静态工程，不依赖 th4-admin 菜单注册表。
- 系统状态是即时采样，不替代 Prometheus/云监控历史数据。
- 当前版本平台账号为单一 Boss 超级管理员；多岗位组织设计见 `docs/product/装得清运营管理端用户组织与权限设计.md`。
- 首次初始化通过默认关闭的 `PlatformAdminBootstrap` 完成，必须显式提供环境变量。
- 管理端人工履约不生成支付订单，线下回款仍需财务登记。
- 尚未在生产环境创建平台运营账号或执行真实浏览器验收。

## Next Action

- acceptance 独立验证平台账号隔离、跨企业查询字段、订阅调整并发和审计一致性。
- 运维在隔离/生产环境执行迁移、建平台运营账号、Nginx 托管和真实 HTTPS 冒烟。
- 后续按 `docs/acceptance/装得清运营管理端验收清单.md` 完成生产演练。

## Acceptance Needed

- 平台运营 Boss 正常访问。
- 普通企业 Boss 与匿名请求均被拒绝。
- 续费、套餐、席位、门店和状态操作正确落库并产生审计。
- 系统指标和依赖健康状态可展示。
