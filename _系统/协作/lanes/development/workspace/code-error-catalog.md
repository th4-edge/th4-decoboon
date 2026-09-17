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

## ECD-002 成本缺失时仍计算并展示伪利润

**级别**：P0/critical
**首次发生**：2026-09-17 麦鸿全业务实测
**现象**：大量报价行没有成本时，利润直接按“合同额 - 已发生成本”计算，前端显示看似可靠的毛利，实际漏掉未采购、未维护采购价和未拆分应付的成本。

### 根因

- “已发生应付”被当成完整项目成本，未维护的报价行成本被隐式视为 0。
- 成本字段默认 `BigDecimal.ZERO`，接口没有独立的完整性状态，前端无法区分“真实 0 成本”和“成本未知”。
- 报价库标准成本、项目报价快照、采购实际成本、应付成本四套口径没有统一上下文。

### 禁则

1. ❌ 禁止在成本缺失时把缺项按 0 元计入利润。
2. ❌ 禁止用 `0` 表示“利润未知”；未知必须是 `null` + `costComplete=false`。
3. ❌ 禁止前端只根据金额判断，必须同时消费 `costComplete`、`costIncompleteCount`。
4. ❌ 禁止结算快照把不完整成本固化为可靠 `profitAmount/grossMargin`。

### 必须写法

- 成本汇总统一走 `ProjectCostContext`，返回估算成本、实际成本和缺失项数。
- `costComplete=false` 时，财务汇总、项目报告、结算、经营驾驶舱的利润和毛利率返回 `null`，前端显示“成本未完整”。
- 结算持久化利润字段允许为空，迁移见 `docs/design/migrations/28-成本完整性结算快照.sql`。
- 回归：`ProjectCostContextTest`、`FinanceProfitAccountingTest`。

---

## ECD-003 采购入库与领料重复累计实际材料成本

**级别**：P1/high
**首次发生**：2026-09-17 采购价回算改造
**现象**：到货入库已累计实际材料成本后，施工领料又按库存单位成本累计一次，项目实际成本和毛利失真。

### 根因

旧实现只在非采购入库动作上写回报价行成本；为了让采购实际成本入账，新增入库累计后，没有同步重审领料、损耗、退换货的动作语义。

### 禁则

1. ❌ 禁止在采购入库和领料两个环节重复累计同一批材料金额。
2. ❌ 禁止把库存数量流转等同于成本发生；库存移动与成本入账必须分开定义。
3. ❌ 禁止只改单一路径，必须覆盖入库、领料、损耗、退料、供应商退货。

### 必须写法

- 采购入库：按采购单价 × 本次到货数量累计实际材料成本。
- 领料、损耗：只流转库存，不改实际材料成本。
- 退料、供应商退货：按退货金额冲减，最小为 0。
- 回归：`InventoryContextTest`、`ProjectCostContextTest`。

---

## ECD-004 敏感成本裁剪使用错误员工上下文

**级别**：P1/high
**首次发生**：2026-09-17 采购价真实回归
**现象**：BOSS 查询报价项目时 `costPrice` 被错误裁剪，采购价虽然已同步到数据库和项目成本，但报价库页面看不到标准成本。

### 根因

Provider 已通过 `TenantContext` 校验并加载 `currentEmployee`，但响应裁剪仍使用未完成关联加载的 `@User` 员工对象，角色判断与业务上下文使用的不是同一实体。

### 禁则

1. ❌ 禁止在同一 Provider 中先用 `TenantContext` 加载 `currentEmployee` 校验，又用原始 `employee` 做权限或字段裁剪。
2. ❌ 禁止把字段可见性判断散落到各 Provider；必须复用统一成本上下文。
3. ❌ 禁止把敏感字段裁剪失败等同于普通前端展示问题，必须验证响应中字段根本不存在。

### 必须写法

- 所有 `CatalogCostContext.mask(...)` 必须传入 Provider 内已校验的 `currentEmployee`。
- 前端隐藏只作为体验；服务端裁剪是权限边界。
- 回归至少覆盖 BOSS 可见、材料员响应无 `costPrice`、材料员访问采购价接口返回 403。

---

## ECD-005 详情响应展开循环归属图导致小程序 JSON 解析失败

**级别**：P0/critical
**首次发生**：2026-09-17 工地详情阶段/任务详情
**现象**：点击“施工进度 → 拆除”显示“阶段加载失败”，点击任务详情显示“加载失败”。接口本身返回 `success=true`，但阶段详情响应达到 240KB+ 且嵌套数百层，小程序无法稳定解析。

### 根因

- `project/stage/info` 的项目摘要白名单误包含 `property`。
- `Property` 继续带出 `store/team/leader/store` 等循环归属对象，序列化深度不断增长。
- 前端把阶段详情、任务、日报、验收串在同一个失败链路中，辅助接口异常也会把整页置为失败。

### 禁则

1. ❌ 禁止详情响应为了“字段可能有用”而透出完整关联树，尤其是 `Property/Store/Team/Employee` 循环链。
2. ❌ 禁止只靠接口 `success=true` 判断正常，必须检查 JSON 大小、嵌套深度和小程序解析结果。
3. ❌ 禁止让非关键辅助数据失败直接覆盖主详情页错误态。

### 必须写法

- 阶段详情只需要项目摘要时，`Project` 白名单只保留 `projectId/no/name/siteAddr`，不得包含 `property`。
- 任务详情同样不得带出 `Property` 完整归属图。
- 日报、任务列表、验收单等辅助数据使用独立 `catch`，主详情已加载时必须继续展示。
- 回归：`ProjectDetailSerializationContractTest`；真实麦鸿响应由 240KB+ 降至 462B，任务详情 1.4KB。
- 2026-09-17 扩展修复：人员绩效 Employee->store/team、报价详情 createdBy/Property 员工关联、验收详情 Project.property 均补齐最小白名单；新增 `PerformanceReportSerializationContractTest`、`DetailResponseWhitelistContractTest`。报价详情由 286KB 不可解析降至 41KB。

---

## ECD-006 报价库成本概念被替换但未保留可见与维护入口

**级别**：P1/high
**首次发生**：2026-09-18 供应商采购价改造后的报价项目库
**现象**：企业负责人进入报价项目库后看不到标准采购成本，也找不到维护供应商采购价的入口，表现为“成本功能消失了”。

### 根因

- 旧版本允许在报价项目库直接输入 `costPrice`。
- 供应商采购价上线后，按新规范删除该输入是正确的，但实现只删除了旧区块，没有补上“标准成本只读展示”和“从目录项进入供应商采购价维护”的替代路径。
- 后端 `costPrice` 仍由 `PartnerPrice` 自动汇总，接口也有字段；缺陷发生在前端功能链路断接，不是成本模型丢失。

### 禁则

1. ❌ 禁止在切换成本来源时只删除旧入口，不提供新来源的可见路径和维护路径。
2. ❌ 禁止把“后端字段仍存在”等同于“前端业务功能完整”。
3. ❌ 禁止让 BOSS、店长、财务只能靠记忆进入二级菜单维护报价项目成本。
4. ❌ 禁止在报价项目库重新开放前端直接覆盖 `CatalogItem.costPrice`。

### 必须写法

- 报价项目库列表和编辑页为 BOSS、店长、财务展示只读“标准采购成本”；无权限角色响应中不得出现该字段。
- 目录项必须提供“维护采购价”入口，并携带 `itemId/itemType` 跳转到供应商采购价页面，自动预选报价项目。
- 报价项目保存/更新仍只维护销售价，标准成本继续由有效 `PartnerPrice.purchasePrice + laborPrice` 自动汇总。
- 回归至少覆盖：BOSS 可见成本与维护入口、采购价页面预选正确项目、材料员不出现成本字段和入口。

---

## 错误集维护规则

1. 每个生产 `code_bug` 事故必须登记为 `ECD-NNN`，包含：现象、触发条件、事故链、为什么没拦住、禁则、必做写法、检测方法。
2. 禁则必须同步写入 `rules.md`（代码生成事实源）与 `AGENTS.md`（AI 助手必读），否则视为未完成。
3. 代码评审与 AI 代码生成都需逐条核对本文件；命中任一条禁则即打回重写。
4. 已修复的错误集不得删除，只能标记状态；用于防止同类问题再次出现。
