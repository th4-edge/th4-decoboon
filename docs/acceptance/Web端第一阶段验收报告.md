# 装得清 Web 第一阶段独立验收报告

> 验收日期：2026-09-15  
> 验收 lane：acceptance  
> 请求编号：`REQ-20260915-ACCEPT-WEB-PHASE1`、`REQ-20260915-ACCEPT-WEB-PHASE1-RECHECK`  
> 验收范围：Web 工程、User 密码登录、会话安全、角色菜单、核心页面/API 契约和后端回归  
> 最终结论：**PASS（自动化、构建和登录页真实浏览器渲染范围）**；已登录业务页的真实后端联调和桌面视觉冒烟为 **BLOCKED**
>
> 说明：本轮没有发现新的 P0/P1 功能缺陷。此前 `DEF-WEB-001` 至 `DEF-WEB-005` 均已关闭；`DEF-WEB-006` 仍是低优先级性能风险。由于当前环境没有可用的真实员工会话和业务后端，不能把已登录核心页面的真机视觉验证表述为通过。

## 0. 执行摘要

### 0.1 通过项

- 密码会话已拆分为四个显式 Provider：
  - `auth/session/password-login`：公开，允许匿名登录。
  - `auth/session/password-setup`：公开，允许未设密码账号首次设置。
  - `auth/session/password-change`：受保护，要求 token。
  - `auth/session/logout`：受保护，要求 token。
- `WebPasswordSessionAcceptanceTest` 共 7 项，覆盖首设、登录、错误计数/锁定、成功清零、停用账号、改密、登出 token 失效、旧密码失效、匿名改密/登出拒绝、弱密码拒绝和敏感密码字段排除。
- Provider 安全扫描结果：`total=196 protected=185 public=11 violations=0`。
- `ProviderSecurityAcceptanceTest` 3 项通过；196 个显式 Provider 路由均有 `Security` 或公开白名单，代表性受保护路由无 token 返回 401。
- Web 登录、驾驶舱、财务、报价、项目和任务的字段契约已对齐。
- `npm run type-check`、`npm run build` 均通过；构建 1723 个模块。
- `mvn -o -q test` 通过：26 个测试类、79 tests、0 failures、0 errors、0 skipped。
- 使用真实浏览器渲染登录页：
  - 1440×900：页面无横向溢出，登录表单可见。
  - 1280×800：页面无横向溢出，品牌区、登录卡和输入框均可见。
- Web 工程未发现业主端、微信扫码/OAuth 登录或虚假业务数据页面。

### 0.2 阻塞项

- 当前没有可登录的真实业务后端、测试账号和业务数据，无法在浏览器中完成驾驶舱、财务、报价和项目页面的真实接口渲染、交互及截图。该项标记为 `BLOCKED-ENV-WEB-001`，不得扩大解释为通过。
- `.env.production` 仍是 `https://api.invalid/` 安全占位地址；正式域名、证书、备案和后端发布状态属于外部发布条件，未纳入本轮“已可发布”结论。
- `DEF-WEB-006`：主 JS chunk 约 **1,096.13 kB**、gzip 约 **362.43 kB**，有 Vite 500 kB 体积警告，属于低优先级性能风险。

## 1. 验收输入与边界

### 1.1 输入

- `docs/plan/2026-09-15-Web端实施基线.md`
- `../../前端代码/th4-decoboon-web/**`
- `_系统/协作/lanes/development/worklog.md`
- `docs/design/migrations/21-用户密码登录.sql`
- 四个显式密码会话 Provider 和对应接口/实现
- 开发对 `REQ-20260915-WEB-FIX-1` 的修复交接

### 1.2 验收边界

- 本轮只读/验证生产代码，不修改 `src/main/**` 或 Web 业务页面。
- 验收测试写入 `src/test/**`。
- 验收报告和证据写入 `docs/acceptance/**`。
- 不使用开发 lane 自测作为独立验收证据。
- 未执行真实生产迁移、真实数据库写入或生产部署。

## 2. 执行命令与结果

| 命令 | 结果 | 说明 |
| --- | --- | --- |
| `npm run type-check` | PASS | `vue-tsc --noEmit` 无错误 |
| `npm run build` | PASS | 1723 modules；产物生成成功；保留 chunk-size 警告 |
| `mvn -o -q -Dtest=WebPasswordSessionAcceptanceTest test` | PASS | 7 tests，0 failures，0 errors |
| `mvn -o -q -Dtest=ProviderSecurityAcceptanceTest,SubscriptionAccessFilterAcceptanceTest,ProductionReleaseBaselineAcceptanceTest test` | PASS | Provider 安全、订阅过滤、发布基线定向验证通过 |
| `mvn -o -q test` | PASS | 26 suites，79 tests，0 failures，0 errors，0 skipped |
| `python3 _系统/协作/lanes/development/workspace/provider-security-scan.py .` | PASS | `total=196 protected=185 public=11 violations=0` |
| Vite preview + 真实浏览器 1440×900 | PASS（登录页） | 无横向溢出，表单可见；截图见证据 |
| Vite preview + 真实浏览器 1280×800 | PASS（登录页） | 无横向溢出，关键区域可见；截图见证据 |
| 已登录业务页浏览器冒烟 | BLOCKED | 缺少真实后端、测试账号和业务数据 |

Surefire 汇总可从以下报告复核：

- `target/surefire-reports/TEST-com.th4.edge.tenant.user.WebPasswordSessionAcceptanceTest.xml`
- `target/surefire-reports/TEST-com.th4.edge.decoboon.security.ProviderSecurityAcceptanceTest.xml`
- `target/surefire-reports/TEST-com.th4.edge.decoboon.owner.OwnerClosureAcceptanceTest.xml`

## 3. User 密码登录与会话安全

### 3.1 Provider 路由和认证边界

| 路由 | 接口认证 | 预期 | 结果 |
| --- | --- | --- | --- |
| `auth/session/password-setup` | 公开 | 未设密码账号可携带 bootstrap token 首次设置 | PASS |
| `auth/session/password-login` | 公开 | 手机号 + 密码可匿名登录 | PASS |
| `auth/session/password-change` | `Security` | 必须携带 token，校验旧密码 | PASS |
| `auth/session/logout` | `Security` | 必须携带 token，登出后 token 失效 | PASS |

验收测试使用真实 Spring 上下文、MockMvc 和 H2 测试数据库，不依赖开发自测结论。

### 3.2 自动化用例

| 用例 | 覆盖 | 结果 |
| --- | --- | --- |
| `passwordSetupAndLoginMustWorkWithoutAnExistingToken` | 首次 setup、BCrypt、哈希非明文、登录、敏感字段排除 | PASS |
| `passwordLoginMustLockAfterRepeatedFailures` | 连续错误计数、锁定截止时间、锁定后正确密码仍拒绝 | PASS |
| `correctPasswordMustClearFailureCount` | 正确密码登录后失败次数清零 | PASS |
| `disabledEmployeeMustNotLogin` | 停用/禁用员工拒绝登录 | PASS |
| `changeAndLogoutMustRequireTokenAndOldPassword` | 改密、旧密码失效、新密码可登录、logout、token 失效 | PASS |
| `changeAndLogoutMustRejectAnonymousRequests` | 匿名改密/登出返回 `tokenIsUndefined` | PASS |
| `weakPasswordMustBeRejected` | 不符合策略的弱密码返回 `PASSWORD_POLICY` | PASS |

补充检查：

- BCrypt 哈希以 `$2` 开头，明文密码不落库、不出响应。
- 响应不包含 `passwordHash`、`password_hash`、`failedAttempts`、`lockedUntil`、`passwordUpdatedAt` 或测试明文。
- `password-change` 和 `logout` 的匿名调用不会进入业务成功路径。

## 4. Provider 安全与公开白名单

Provider 扫描确认：

```text
total=196 protected=185 public=11 violations=0
```

公开白名单为：

- `auth/session/login`
- `auth/session/password-login`
- `auth/session/password-setup`
- `auth/session/register`
- `identity/customer-invite/accept`
- `identity/customer-invite/info`
- `invite/invitation/accept`
- `invite/invitation/info`
- `invite/invitation/reject`
- `order/callback`
- `file/private/download`

代表性受保护路由的匿名访问均被拒绝：

- `project/project/page`
- `project/task/update`
- `identity/customer-invite/save`
- `file/private/grant`
- `finance/receivable/page`

新增密码登录和首次设置路由已加入公开契约验证；旧动态路由固定预期的 `192/9` 不再作为验收依据，当前测试以 Provider 实际注册清单和显式白名单为准。

## 5. Web 页面字段契约与 API 路由

### 5.1 修复后的契约

| 页面/模块 | 已对齐字段或行为 | 结果 |
| --- | --- | --- |
| 驾驶舱 | `signedAmount`、`receivedAmount`、`grossProfit`、`activeProjectCount` 的 `DashboardMetric` 对象 | PASS |
| 财务总览 | `receivableAmount`、`receivedAmount`、`payableAmount`、`paidAmount`、`outstandingAmount`、`overdueAmount`、`unpaidAmount`、`cashBalance` | PASS |
| 报价列表/详情 | `title`、`amount` | PASS |
| 项目列表 | `Preparing`、`Ongoing` 状态值和 `status` 筛选 | PASS |
| 项目任务/施工计划 | `owner.name`、`plannedEnd` | PASS |
| 财务流水 | 按应收、应付、收款、付款、台账分别使用对应 ID、金额和日期字段 | PASS |
| 项目/报价筛选 | Provider 支持 `status`；项目/报价支持 `keyword` | PASS |

上述结论基于前端类型、页面模板、API 包装和后端 Provider 的交叉静态核验。没有真实后端返回数据，因此不把静态字段对齐等同于真实接口联调通过。

### 5.2 API 路由存在性

原验收的 18 个 Web API 路由均能在后端找到对应显式 `@Service` Provider，包括：

```text
statistic/boss/dashboard
customer/customer/page
customer/customer/info
project/project/page
project/project/info
project/stage/page
project/task/page
quote/page
quote/info
finance/summary/report
finance/receivable/page
finance/payable/page
finance/ledger/page
finance/receipt/page
finance/payment/page
design/requirement/page
collab/notice/page
company/member/page
```

结论：路由存在性 PASS。

## 6. 路由、菜单和 401/403

### 6.1 角色菜单矩阵（静态）

| 角色 | 可访问能力摘要 |
| --- | --- |
| BOSS | 全部模块 |
| 门店店长 | 除订阅外的经营模块 |
| 团队负责人 | 驾驶舱、客户、项目、供应链、统计、消息、个人设置 |
| 家装顾问 | 客户、消息、个人设置 |
| 设计师 | 客户、设计、报价、消息、个人设置 |
| 项目经理 | 项目、供应链、消息、个人设置 |
| 履约专员 | 项目、供应链、消息、个人设置 |
| 财务 | 报价、财务、统计、消息、个人设置 |

- 受保护路由均配置 `permissions` 元数据，前端守卫按角色能力判断。
- 未发现业主端或微信扫码身份入口。
- 前端菜单和按钮隐藏不构成服务端权限；后端仍由 `Security`、`@DataPermission` 和 `DataScope` 作为权限事实源。

### 6.2 401/403

- 401 相关错误会清理本地 token/profile 并跳转登录页。
- 403 仅提示权限不足，不清除有效会话。
- 密码 logout 验收确认登出后原 token 无法继续访问 `auth/session/info`。
- 运行时代码路径存在，但真实浏览器的 401/403 跨页面行为仍属于已登录业务页 BLOCKED 范围。

## 7. 桌面浏览器冒烟

使用构建产物和 Vite preview，在真实浏览器中执行：

### 7.1 1440×900

- `window.innerWidth=1440`、`window.innerHeight=900`。
- 根页面宽度与视口一致，`body.scrollWidth=1440`，无横向溢出。
- `/` 重定向到 `/login?redirect=/dashboard`。
- 登录标题、手机号输入框、密码输入框和“进入工作台”操作可见。
- 证据：`docs/acceptance/evidence/web-phase1-login-1440x900.png`

### 7.2 1280×800

- `window.innerWidth=1280`、`window.innerHeight=800`。
- `body.scrollWidth=1280`，无横向溢出。
- 品牌区宽 736px、登录面板宽 544px；登录卡约 365px，手机号和密码输入框可见。
- 证据：`docs/acceptance/evidence/web-phase1-login-1280x800.png`

### 7.3 阻塞说明

没有真实员工账号、业务后端和数据库会话，浏览器无法进入驾驶舱、财务、报价和项目页面。因此：

- 登录页桌面渲染：PASS。
- 已登录业务页桌面渲染、交互和真实接口展示：BLOCKED。
- 未使用伪造数据冒充真实业务验收到结论。

## 8. 缺陷状态

| ID | 等级 | 状态 | 说明 |
| --- | --- | --- | --- |
| `DEF-WEB-001` | Blocker/P0 | CLOSED | 已拆分为公开的 `password-login`/`password-setup` 和受保护的 `password-change`/`logout` |
| `DEF-WEB-002` | High/P1 | CLOSED | 驾驶舱指标对象字段已对齐 |
| `DEF-WEB-003` | High/P1 | CLOSED | 财务总览字段已对齐 |
| `DEF-WEB-004` | Medium/P2 | CLOSED | 报价金额/标题、任务负责人/计划结束时间已对齐 |
| `DEF-WEB-005` | Medium/P2 | CLOSED | 项目状态枚举、状态筛选和关键字参数已修复 |
| `DEF-WEB-006` | Low/P3 | OPEN | 主 JS chunk 约 1.1 MB，构建有体积警告 |

环境阻塞项：

| ID | 类型 | 状态 | 说明 |
| --- | --- | --- | --- |
| `BLOCKED-ENV-WEB-001` | 环境/联调 | BLOCKED | 缺少真实业务后端、测试账号和业务数据，无法执行已登录页面的真实浏览器验收 |

## 9. 覆盖矩阵

| 验收领域 | 自动化/静态证据 | 浏览器证据 | 结论 |
| --- | --- | --- | --- |
| 公开登录/首设 | `WebPasswordSessionAcceptanceTest` 2 个相关用例、Provider 安全测试 | 登录页可见 | PASS |
| 失败锁定/停用账号 | 密码会话测试 | 无真实后端 | PASS（后端） |
| 改密/登出/token 失效 | 密码会话测试 | 无真实后端 | PASS（后端） |
| Provider 认证边界 | Provider 扫描 + 3 项安全测试 | 登录页可访问 | PASS |
| 角色菜单/路由 | 源码静态矩阵 | 无真实员工会话 | PASS（静态） |
| 页面字段契约 | 类型/模板/Provider 交叉核验 | 无真实接口数据 | PASS（静态） |
| 401/403 | 源码检查 + token 失效测试 | 无真实业务会话 | PASS（代码/后端） |
| 1440×900 登录页 | 浏览器尺寸和溢出检查 | 截图 | PASS |
| 1280×800 登录页 | 浏览器尺寸和溢出检查 | 截图 | PASS |
| 已登录业务页视觉 | 未执行 | 未执行 | BLOCKED |
| 后端全量回归 | Surefire 79/79 | 不适用 | PASS |

## 10. 最终判定

| 范围 | 结果 |
| --- | --- |
| Web 类型检查与构建 | PASS |
| 首设、登录、改密、登出 | PASS |
| BCrypt、失败锁定、停用账号、敏感字段排除 | PASS |
| Provider 安全边界和公开白名单 | PASS |
| 路由/菜单静态权限矩阵 | PASS |
| Web 页面字段和 API 路由契约 | PASS（静态） |
| 401/403 会话处理 | PASS（代码/后端） |
| 后端全量回归 | PASS（79 tests，0 failures，0 errors） |
| 1440×900 / 1280×800 登录页真实浏览器渲染 | PASS |
| 已登录业务页真实浏览器冒烟 | BLOCKED |

**final_status：`PASS`（自动化、构建、Provider 安全和登录页渲染范围）；`BLOCKED`（真实已登录业务页视觉/联调范围）。整体不能据此宣称已完成生产环境端到端发布验收。**

## 11. 下一阶段动作

1. 由 product-planning/运维提供真实测试后端、有效员工账号和覆盖驾驶舱、财务、报价、项目的业务夹具。
2. 在 1440×900 和 1280×800 浏览器中完成已登录页面真实渲染、字段展示、分页、筛选、401/403 和退出流程冒烟。
3. 正式发布前替换 `https://api.invalid/`，验证 HTTPS 域名、证书、备案、后端 CORS 和网络策略。
4. 根据发布优先级决定是否优化 `DEF-WEB-006` 的主包体积和首屏加载。
5. 真实环境验证完成前，保留 `BLOCKED-ENV-WEB-001`，不得将本轮 PASS 扩大为生产可发布结论。
