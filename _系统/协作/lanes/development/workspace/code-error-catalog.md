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

## ECD-005A 材料计划分页重复展开循环归属图（ECD-005 等价复发）

**级别**：P0/critical
**首次发生**：2026-09-18 供应商协作三角色实测
**现象**：企业负责人打开供应商协作后材料计划显示为 0 条，后端响应因 `Property -> Store/Team/Leader -> Store` 循环归属图异常膨胀。

### 根因

`project/material-plan/page` 的 `MaterialPlan` 白名单保留了 `property`，`Project` 白名单也保留了 `property`，重复触发 ECD-005 同类问题。

### 禁则

1. ❌ 禁止分页/列表为了“字段可能有用”返回完整 `Property/Store/Team/Employee` 归属树。
2. ❌ 禁止只在详情响应修白名单，列表链路必须同步审计。
3. ❌ 禁止以接口 `success=true` 作为通过依据，必须检查响应大小和前端解析。

### 必须写法

- 材料计划列表只返回 `Project` 摘要，不展开 `Property`。
- 报价项目只返回 `itemId/name/unit/brand/spec` 摘要。
- 回归：`DetailResponseWhitelistContractTest#materialPlanPageMustNotExposeRecursivePropertyGraph`。
- 实测修复后材料计划响应由异常膨胀降至约 650B，并在 BOSS、PM、材料员三角色正常加载。

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

## ECD-007 供应链按完整 ERP 建模导致中小装企主流程复杂

**级别**：P1/high
**首次发生**：2026-09-18 供应商协作设计复核
**现象**：材料计划、供应商、采购价、采购单、仓库、库存、批次、审批、应付、对账同时平铺，用户看不到“项目缺什么 → 找谁买 → 是否到货 → 月底付多少”的最短路径。

### 根因

- 设计从完整领域模型出发，而不是先从中小装企真实采购动作出发。
- 所有采购默认进入仓库和库存，供应商直送工地没有成为默认路径。
- 低频字段和高级能力没有分级展示。

### 禁则

1. ❌ 禁止未写真实业务场景就直接新增供应链实体、字段或页面。
2. ❌ 禁止要求中小装企为了采购先维护仓库、批次、税率和复杂审批。
3. ❌ 禁止把低频高级能力与高频主流程同时铺在首屏。
4. ❌ 禁止只按数据表结构组织页面，而不说明下一步交给谁。

### 必须写法

- 先写“询价、项目下单、到货、月结对账、退换货、定制/劳务”等真实场景。
- 默认采购交付方式为供应商直送工地，入仓库作为可选路径。
- P0 只暴露项目采购、供应商、采购价、到货、应付和月结；库存、批次和供应商门户按需展开。
- 每个页面必须给出当前业务节点和下一步动作。
- 回归验收必须按真实角色和真实动作跑通，不以字段齐全替代业务闭环。

---

## ECD-008 引导/进度状态硬编码且业务动作后不刷新

**级别**：P1/high
**首次发生**：2026-09-18 App 首次入驻引导
**现象**：BOSS 已维护成本价格和供应商品牌关系，返回首页后引导仍显示未完成。

### 根因

- 供应商步骤在生产组件中写死 `done: false`，完成状态与领域数据断开。
- 引导数据只在组件首次挂载时加载，Tab 页面从子页面返回时没有重新读取。
- 需求只定义了展示和状态来源，没有定义每步完成谓词、必做/可选分母和刷新触发条件。

### 为什么没拦住

- 类型检查、构建检查和页面截图只能证明页面存在，不能证明业务动作后状态收敛。
- 验收没有覆盖“动作成功 -> navigateBack/onShow -> 完成态刷新”的跨页面状态迁移。
- 静态占位值没有被开发自检门禁识别。

### 禁则

1. ❌ 禁止引导、进度、待办、资料完整度等业务状态使用硬编码 `done: false/true`、演示数据或仅前端本地布尔值。
2. ❌ 禁止只依赖组件首次挂载加载会随业务动作变化的数据；必须定义返回/重新显示时的刷新或缓存失效策略。
3. ❌ 禁止在未冻结“必做/可选、完成谓词、计数来源、异常态”前，把引导页面视为完成。
4. ❌ 禁止只用接口成功、页面可打开或 type-check 通过证明引导闭环完成。

### 必须写法

- 每个步骤定义并冻结：`key / label / required / source_provider / completion_predicate / count / refresh_triggers / next_action`。
- 完成状态直接从当前企业真实领域对象派生；供应商品牌关系至少校验有效对象及关联关系。
- 页面重新显示时刷新可变状态；子页面写入后回首页能看到最新结果。
- 验收覆盖初始态、动作成功返回、动作失败、可选步骤完成、必做步骤未完成和重复进入。

### 检测与回归要求

- 扫描引导/进度组件中是否存在 `done: false`、静态 completion/status 字段。
- 回归必须真实执行：维护报价项/成本/供应商/客户 -> 返回首页 -> 进度状态更新。
- 可选步骤完成后分母不变；必做项未完成时下一步必须指向正确任务。

## ECD-009 长文本列长度与迁移基线漂移导致保存截断

**级别**：P1/high
**首次发生**：2026-09-21 凭证模板保存（`db_document_template.field_config`）
**现象**：App 横向模板配置页保存企业模板时报
`Data truncation: Data too long for column 'field_config' at row 1`。

### 触发链

1. 实体 `DocumentTemplate.fieldConfig` 早期为普通 `String`，Hibernate 在开发库把列建成了 `varchar(255)`。
2. 后续实体改为 `@Lob`（新库为 `longtext`），但 `spring.jpa.hibernate.ddl-auto: update` **不会修改既有列类型**。
3. 迁移 `41-单据模板与PDF归档.sql` 已声明 `longtext`，但该库由 ddl-auto 建表，从未执行该迁移。
4. 新增"常用凭证模板库"后字段 JSON 由约 160 字符涨到约 480 字符，超过 255 直接截断报错。

### 为什么没拦住

- H2 集成测试用 `ddl-auto: create-drop`，每次按实体最新定义建表，永远复现不出"存量列偏窄"。
- 既有校验只覆盖业务规则与白名单，没有覆盖持久化层长文本容量。
- 开发库列类型从未与迁移文件比对。

### 禁则

1. ❌ 禁止新增或扩展 JSON、快照、备注等长文本字段时只改实体不补迁移；必须同时提供可重复执行的 `alter table`。
2. ❌ 禁止依赖 `ddl-auto: update` 修正既有列长度——它只加列，不修改列类型。
3. ❌ 禁止在由 ddl-auto 建表的库上声称"迁移已覆盖"。

### 必须写法

- 长文本字段实体用 `@Lob`（新库 `longtext`），并为**既有库**提供 `alter table ... modify column ... longtext`。
- 判断依据以 `show create table` 实测列类型为准，不以实体注解为准。

### 检测方法

- 对含 `@Lob` 的实体，核对对应迁移与存量库列类型是否为 `longtext`。
- 出现同类截断报错时，先查列类型，再查业务长度。

## ECD-010 并发清理 target/classes 与多构造器导致应用启动失败

**级别**：P1/high  
**首次发生**：2026-09-22 本地启动 `com.th4.edge.Application`

### 现象

启动日志先出现：

```text
Failed to parse configuration class [com.th4.edge.Application]
Caused by: ClassNotFoundException: com.th4.edge.components.data.listener.SnowflakeEntityListener
```

类文件本身存在；干净重编译后又进一步暴露：

```text
Failed to instantiate [CustomerOverviewContext]: No default constructor found
```

### 根因

1. 工作区多 Lane 并发时，Maven `clean` 会删除整个 `target/`；IDE 若在清理过程中启动，classpath 可能只看到部分类，表现为随机 `ClassNotFoundException`。
2. `CustomerOverviewContext` 新增依赖后保留了旧、新两个构造器，未明确标注 `@Autowired`，Spring 在多个候选构造器中无法自动选择。

### 禁则

1. ❌ 禁止在 IDE 启动或 Maven 测试正在运行时由其他 Lane 执行 `mvn clean`。
2. ❌ 禁止在一个 Spring Bean 中保留多个构造器却不明确注入入口；优先只保留一个构造器。
3. ❌ 禁止仅凭 `target/classes` 中“看到某个 class”判断类路径完整；启动前必须干净编译并加载 Spring 上下文。

### 必须写法

- 本地启动前先执行 `mvn -o -q clean compile`，确认 `target/classes` 是完整产物。
- Spring 组件优先使用单一构造器依赖注入；确需多个构造器时明确 `@Autowired` 主构造器。
- 启动类路径问题至少用一条 `@SpringBootTest` 上下文测试验证，不能只看单元测试通过。

### 检测方法

- `test -f target/classes/com/th4/edge/components/data/listener/SnowflakeEntityListener.class`。
- `mvn -o -q -Dtest=BossMeasureReportContextTest test` 能加载完整 Spring 上下文。
- 搜索 `Context/ `Service` 类中构造器数量，多个构造器必须能解释为什么需要并存。

## 错误集维护规则

1. 每个生产 `code_bug` 事故必须登记为 `ECD-NNN`，包含：现象、触发条件、事故链、为什么没拦住、禁则、必做写法、检测方法。
2. 禁则必须同步写入 `rules.md`（代码生成事实源）与 `AGENTS.md`（AI 助手必读），否则视为未完成。
3. 代码评审与 AI 代码生成都需逐条核对本文件；命中任一条禁则即打回重写。
4. 已修复的错误集不得删除，只能标记状态；用于防止同类问题再次出现。

## ECD-011 对外可见投影直出实体，派生字段未按领域事实推导

**级别**：P1/high
**首次发生**：2026-09-24 业主端沉浸式体验审计（一次抓到 3 处同源）
**现象**：同一业主在「我的家」首页看到项目"待验收"、在项目旅程看到"施工中"；项目详情"当前： · 阶段 0/8"当前阶段为空，而其他 8 个页面显示"开工交底"。

### 根因

- `customer/project/info`（业主详情）**直接把实体返回**（`return accessContext.requireProject(...)`），完全没有组装投影；
- `customer/project/page`（业主项目列表）同样直出，未推导 `currentStageName` 与业主可见状态；
- `customer/project/journey/info` 修过一次（FIX-015）但只改了"当前阶段"，未改"项目状态"；
- `Project.status` / `Project.currentStageName` 是**可回填的派生列**，只有 `project/project/page`、`project/project/overview` 会临时赋值 → **4 条业主可见路径出现 3 种口径**；
- 前端用 `projectStageText()` 兜底后，页面从"空白"变成"显示项目状态"，**掩盖了后端未组装**，使缺陷更难发现。

### 为什么没拦住

- 类型检查、构建、单页截图都只能证明"这一页有内容"，证明不了**跨页面口径一致**；
- 没有"同一字段在多个投影间必须相等"的断言测试；
- 前端兜底让缺失看起来像"降级展示"，验收容易放过。

### 禁则

1. ❌ 禁止对外可见投影直接 `return entity;`（含 `return accessContext.requireProject(...)`）。
2. ❌ 禁止直接返回 `status` / `currentStageName` 等**派生列**而不按领域事实校验/推导。
3. ❌ 禁止同一派生字段在多个投影各写一套推导（发现第二处必须收敛为共享实现）。
4. ❌ 禁止用前端兜底替代后端组装（兜底只能用于字段确实缺失时的降级）。
5. ❌ 禁止以 OSIV/懒加载兜底关联读取。

### 必须写法

- 业主端四条投影（`customer/project/page`、`customer/project/info`、`customer/project/journey/info`、员工侧 `project/project/page|overview`）**共用同一套推导实现**；
- 冻结口径：当前阶段 = `Doing/Accepting` 优先否则第一个阶段；业主可见状态 = 存在 `Pending` 验收单 ⇔ `Accepting`，否则已开工 ⇒ `Ongoing`，全 `Done` ⇒ `Finished`，无开工 ⇒ `Preparing`，`Suspended/Finished/Settled` 保留；
- 详见 `rules.md` §5.6。

### 检测与回归要求

- 自检：`grep -rn "return accessContext.requireProject\|^\s*return project;" src/main/java/com/th4/edge/decoboon/customer` 应无命中；
- 断言测试：同一项目在**我的家首页 / 项目详情 / 项目旅程**三处 `status` 与 `currentStageName` 相等；
- 迁移回归：验收**驳回**后三处同时由"待验收"变"施工中"；重新提交验收后同时回到"待验收"；
- 小程序内人工复验：三页同时截图，比对同一字段。
