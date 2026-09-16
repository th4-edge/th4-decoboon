# 根因分析｜INC-20260916-a1233597b0（重新打开）

- 采集时间：2026-09-16 06:40:11 +08:00
- 采集窗口：近 12 小时
- 指纹：`LazyInitializationException: Could not initialize proxy - no session`
- 状态：`reopened`（已于 05:31 标记 fixed，但 05:40 起以**新的实体**再次出现）
- 次数：42 · 首次 05:14:57 · 最近 06:04:02
- 服务状态：`decoboon.service` active（PID 31996，05:32:22 启动），HTTPS `/api/auth/session/info` 返回 200

---

## 一、现象

生产日志中稳定出现（非偶发）：

```text
[2026-09-16 06:04:02.211] [ERROR] ... - project/project/page处理失败:Could not initialize proxy [com.th4.edge.tenant.billing.model.PlanVersion#1789503342321981197] - no session
[2026-09-16 06:04:02.139] [ERROR] ... - collab/notice/count处理失败:Could not initialize proxy [com.th4.edge.tenant.billing.model.PlanVersion#1789503342321981197] - no session
```

受影响 Provider（同一根因、不同入口）：

| Provider | 次数区间 | 说明 |
| --- | --- | --- |
| `project/project/page` | 05:40:19 - 06:04:02 | 项目列表，最主要 |
| `project/report/page` | 05:40:21 - 05:41:04 | 施工日报列表 |
| `collab/notice/count` | 05:40:19 - 06:04:02 | 未读通知计数 |

> 注意：05:14-05:15 的历史样本是 `Employee#...`，05:31 修复后该类不再出现；05:40 起全部是 `PlanVersion#1789503342321981197`。监控指纹只取异常类型，因此把两者合并为同一指纹并标记为 reopened。

---

## 二、根因

**生产关闭了 Open Session In View，而订阅校验链路在事务外访问了懒加载关联。**

调用链（请求进入任一受保护 Provider 时都会执行）：

1. `SubscriptionAccessRequestFilter.prepare()`（`RequestFilter`，priority=180，位于 Provider 事务之外）
   → `subscriptionContext.requireModule(current.getCompany(), moduleOf(route))`
2. `SubscriptionContext.requireModule()`（`@Component`，**无 `@Transactional`**）
   → `current(company)` → `subscriptionRepository.findByCompany_ComId(comId)`
   → Spring Data 默认只读事务，返回后事务即关闭，`Subscription` 变为 detached
3. `moduleConfig(subscription)` → `effectiveVersion(subscription)`
   → `subscription.getPlanVersion()` 返回**未初始化的 Hibernate 代理**
4. `version.getModules()` 触发代理初始化
   → 已无 Session → `LazyInitializationException ... no session`

### 环境差异（为什么线上报、本地不报）

| 配置 | 值 | 后果 |
| --- | --- | --- |
| `application-prod.yml` | `spring.jpa.open-in-view: false` | 无请求级 Session 兜底，事务外访问懒加载必然失败 |
| `application-dev.yml` / `application-test.yml` | 未设置（默认 `true`） | 请求期间 Session 保持打开，代理可被初始化，**掩盖缺陷** |

本地验证：`dev` 环境（OSIV=true）调用 `project/project/page`、`collab/notice/count` 均返回成功；线上（OSIV=false）同一调用 100% 失败。差异只来自 profile 配置，代码完全一致。

### 与 05:31 修复的关系

- 05:31 已修的是**同一类根因的另一处**：`CredentialsRepository` 增加
  `@EntityGraph(attributePaths = {"employee", "employee.store", "employee.team", "employee.company"})`，
  解决 `auth/session/info` 的 `Employee` 懒加载。
- 但 `SubscriptionRepository` 仍是**零 EntityGraph**：

```bash
grep -c EntityGraph src/main/java/com/th4/edge/tenant/billing/repository/SubscriptionRepository.java
# 0
```

- 结论：上一轮修复解决了"一个实体、一个入口"，没有把"事务外访问懒加载关联"当作**一类问题**做系统排查，因此同一根因在订阅校验链路上再次暴露。

---

## 三、影响

- 命中范围：所有经过 `SubscriptionAccessRequestFilter` 且 `moduleOf(route)` 非空的受保护接口，在访问订阅时若触发 `effectiveVersion` 的业务分支。
- 用户可见：项目列表、日报列表、未读通知计数直接失败（HTTP 200 + `success:false`，前端表现为数据加载失败/为空）。
- 影响等级：critical（高频核心页面）；但**不影响数据正确性与安全边界**，是可用性缺陷。

---

## 四、修复建议（未自动执行，待确认）

1. 首选（与 05:31 修复保持一致的最小改动）：
   在 `SubscriptionRepository.findByCompany_ComId` 上增加
   `@EntityGraph(attributePaths = {"planVersion", "plan", "company"})`；
   同步评估 `findByCompany_ComIdForUpdate` 是否也需要。
2. 备选：让 `SubscriptionContext.requireModule / requireWritable / requireSeatCapacity` 在只读事务内完成校验，避免在事务外触碰代理。
3. 系统性排查：所有在 `RequestFilter` / `Context`（无事务）中读取实体的链路，逐一确认关联字段是 EAGER、EntityGraph 或处于事务内。本次已发现两处，不能再按单点处理。
4. 回归测试要求：测试必须在 `spring.jpa.open-in-view=false` 下运行，否则仍然会被 OSIV 掩盖。
5. 验证方式：修复后调用 `project/project/page`、`project/report/page`、`collab/notice/count`，确认不再出现该异常指纹。

> 自动修复门禁评估：单根因、改动文件预计 ≤3、不涉及 DDL/迁移/密钥/资金/权限放宽，满足 conditions；但需用户确认后再进入修复与发版流程。

---

## 五、其余日志归类（非代码缺陷）

| 指纹 | 次数 | 归类 | 依据 |
| --- | --- | --- | --- |
| `访问受限，缺少授权标识` | 24 | expected | 匿名/健康检查请求，`auth/session/info` 无 token → 401 语义 |
| `手机号或密码错误` | 1 | expected | 登录失败噪音 |
| `首次登录请授权手机号` | 1 | expected | 首次微信登录正常分支 |
| `当前用户已有在职企业` | 1 | expected | 重复注册拦截 |
| `微信支付回调签名缺失/原始报文不存在` | 各 1 | expected | 空回调探测 |
| `找不到对应的处理服务:.env` | 1 | expected | 外部扫描器探测 |
| `找不到对应的处理服务:config` | 2 | unknown（倾向扫描器噪音） | 06:33:46 出现 `/config` 探测，无业务调用方；建议观测是否重复 |

---

## 六、待办

1. 用户确认后修复 `SubscriptionRepository` 并补充 OSIV=false 回归测试。
2. 修复发版后重新采集 `10 minutes ago`，确认该指纹不再增长。
3. 更新 `INC-20260916-a1233597b0` 事故状态与开发 worklog。

---

## 七、后续处置（2026-09-16 06:58 更新）

按项目负责人决策，生产 `application-prod.yml` 的 `spring.jpa.open-in-view` 已调整为 **true**，
release `/opt/decoboon/releases/20260916-065845` 已发布，进程 06:59:33 启动，HTTPS 健康检查 200，
发版后 10 分钟窗口无 `LazyInitializationException` 新增。

**保留的改动**：`SubscriptionRepository.findByCompany_ComId` 的 `@EntityGraph` 不回滚。
理由：预取能减少额外 SQL（N+1），并让代码在"无 OSIV 兜底"时依然正确，避免将来再次关闭 OSIV 时复发。

**需要知晓的取舍（OSIV=true 的代价）**：

1. 每个请求会在整个响应周期内占有一个数据库连接；当前 HikariCP `maximum-pool-size` 默认 10，
   高并发/慢接口场景下连接池耗尽的风险高于 OSIV=false。建议纳入生产压测与连接池监控。
2. OSIV 会把"事务外访问懒加载"从报错改为静默多查一次，**掩盖**此类设计问题；
   因此代码规范仍然禁止依赖 OSIV 兜底（见 `rules.md` §4.5、`AGENTS.md`）。
3. 懒加载回归测试 `SubscriptionLazyLoadingRegressionTest` 继续显式 `open-in-view=false` 运行，
   保证代码质量不因生产配置放宽而退化。
