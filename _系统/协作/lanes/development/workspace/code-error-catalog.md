# 代码错误集与禁则（Error Catalog & Deny-list）

> 目的：把生产事故收敛为**可检索的错误模式**，并转成"后续代码生成不允许再出现"的硬性禁则。
> 规则源：`rules.md` §4.5、§8 与 `AGENTS.md`「事务边界与懒加载禁则」。
> 维护：development lane。每新增一个生产 code_bug 指纹，必须登记到本文件并同步 `rules.md`。

---

## ECD-001 事务外访问懒加载关联（LazyInitializationException）

**级别**：P0/critical
**首次发生**：2026-09-16（`Employee`，已修复）
**复发**：2026-09-16（`PlanVersion`，已修复并标记 `INC-20260916-a1233597b0`）
**生产影响**：`project/project/page`、`project/report/page`、`collab/notice/count` 直接失败，42 次

### 现象

```text
ERROR ... ApiHttpProcessor : project/project/page处理失败:
Could not initialize proxy [com.th4.edge.tenant.billing.model.PlanVersion#...] - no session
```

HTTP 返回 `200 + success:false`，前端表现为"数据加载失败/列表为空"，不报 500。

### 触发三要素（缺一不可）

1. **历史触发条件（生产关闭 OSIV）**：当时 `application-prod.yml` 为 `spring.jpa.open-in-view: false`（`dev`/`test` 默认 `true`）。现已强制生产保持 `open-in-view: true` 作为兜底，但**代码仍必须满足下列禁则**，做到关闭 OSIV 也正确。
2. **查询发生在事务之外**：`RequestFilter`（如 `SubscriptionAccessRequestFilter.prepare()`，priority 在执行链最前）或 `@Component` 上下文类（如 `SubscriptionContext`）直接调用 Repository；Spring Data 方法自带的只读事务返回后立即关闭，实体变 detached。
3. **访问 LAZY 关联的字段**：`@ManyToOne(fetch = LAZY)` 返回的是未初始化代理，一读取代理属性就要求 Session。

### 事故链（PlanVersion 案例）

```
SubscriptionAccessRequestFilter.prepare()            ← 在 Provider 事务之外
  → SubscriptionContext.requireModule(...)           ← @Component，无 @Transactional
    → subscriptionRepository.findByCompany_ComId()   ← 事务返回即关闭，实体 detached
      → effectiveVersion(subscription)
        → subscription.getPlanVersion()              ← 未初始化代理
          → version.getModules()                     ← 触发初始化 → no session 💥
```

### 为什么测试没有拦住

- `dev`/`test` 默认 `open-in-view=true`，Session 覆盖整个请求，代理能被初始化，**缺陷被掩盖**。
- 历史成功修复只处理了"一个实体、一个入口"（`CredentialsRepository` 加 `@EntityGraph` 解决 `Employee`），没有把"事务外访问懒加载"当成**一类问题**做全量排查，所以同类根因在 `SubscriptionRepository` 上再次爆发。

### 禁则（后续代码生成一律不允许再出现）

1. ❌ 禁止在 `RequestFilter`、`@Component` 上下文类、工具类等**无事务边界**的代码里访问实体的 LAZY 关联字段。
2. ❌ 禁止用"dev/test 跑通了"作为通过依据——这两个 profile 默认 OSIV=true，会掩盖该缺陷。
3. ❌ 禁止在新增 `@ManyToOne(fetch = LAZY)` 关联后，不检查所有查询路径（尤其是 Filter/Context 链路）。
4. ❌ 禁止靠"给某一个 Repository 加注解"就宣布问题解决；必须排查同类链路。
5. ❌ 禁止用 `@JsonIgnore` 或临时 `EAGER` 全局放大来绕开问题（违反 `rules.md` §4.4、§5）。

### 必须写法（二选一）

- **A. 查询期预取（首选，改动最小）**：
  ```java
  @EntityGraph(attributePaths = {"planVersion", "plan", "company"})
  Subscription findByCompany_ComId(Long comId);
  ```
  与既有修复一致（`CredentialsRepository` 使用 `@EntityGraph({"employee","employee.store","employee.team","employee.company"})`）。
- **B. 明确事务边界**：让校验/聚合逻辑在 `@Transactional(readOnly = true)` 内完成，避免在事务外触碰代理。
  ⚠️ 带 `PESSIMISTIC_WRITE` 的方法不要与 `@EntityGraph` 随意组合，锁与 join 的语义需单独评估。

### 检测与回归要求

1. **测试必须显式关闭 OSIV**：
   ```java
   @SpringBootTest(properties = { ..., "spring.jpa.open-in-view=false" })
   ```
   且测试方法**不能加** `@Transactional`，否则 Session 覆盖整个方法，无法复现。
2. 现成范例：`src/test/java/com/th4/edge/tenant/billing/SubscriptionLazyLoadingRegressionTest.java`
   - 修复前：3 失败（`LazyInitializationException`）
   - 修复后：3 通过
3. 上线前扫描：对所有 `filter/**`、`context/**` 中注入的 Repository 调用点，逐一确认关联字段已预取或在事务内。

### 交付前自检（本错误集专属）

- [ ] 本次新增/修改的查询，是否可能被 Filter / Context（事务外）调用？
- [ ] 若可能，返回实体的 LAZY 关联是否已 `@EntityGraph` / `JOIN FETCH` 预取？
- [ ] 是否补了 `open-in-view=false` 的回归测试，并确认修复前会失败？
- [ ] 是否排查了同一根因的其他调用点，而不是只修一处？

---

## 配置现状说明（2026-09-16）

生产 `application-prod.yml` **必须保持 `open-in-view: true`**（项目强制要求，release `/opt/decoboon/releases/20260916-065845` 起生效）。

⚠️ **禁则不因该配置而放宽**：
- OSIV=true 只是"兜底"，会让事务外懒加载从报错变成静默多查一次，掩盖设计问题；
- 一旦将来重新关闭 OSIV（或接入多数据源、异步、批量任务），同一代码会立即崩溃；
- 懒加载回归测试继续以 `open-in-view=false` 运行，代码必须做到"无 OSIV 兜底也正确"。

---

## 错误集维护规则

1. 每个生产 `code_bug` 事故必须登记为 `ECD-NNN`，包含：现象、触发条件、事故链、为什么没拦住、禁则、必做写法、检测方法。
2. 禁则必须同步写入 `rules.md`（代码生成事实源）与 `AGENTS.md`（AI 助手必读），否则视为未完成。
3. 代码评审与 AI 代码生成都需逐条核对本文件；命中任一条禁则即打回重写。
4. 已修复的错误集不得删除，只能标记状态；用于防止同类问题再次出现。
