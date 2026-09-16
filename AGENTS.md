# AGENTS.md — th4-decoboon 代码生成规范（AI 助手必读）

本工程是安徽昭青「装得清」SaaS 平台的后端（Java 22 + Spring Boot 3.4 + th4 框架）。
**所有新增/修改/重构代码必须严格遵循本规范**；与既有旧写法冲突时，以本规范为准并回改存量代码。


## StarWork 工作台入口（开始任务前先读）

1. `AGENTS.md`
2. `_系统/上下文/当前项目.md`
3. `_系统/任务/当前工作.md`
4. `.starwork/workspace.json`
5. `.starwork/skills.json`
6. `.starwork/rules/index.md`，再按索引读取相关扩展规则
7. `_系统/协作/agent-lanes.md`
8. `_系统/协作/lanes/{当前-lane}/worklog.md`
9. `_系统/协作/shared.md`

涉及用户偏好或长期背景时读 `_系统/身份/README.md`；涉及高复用经验或已知风险时读 `_系统/教训/README.md`；涉及后端实现时还必须读取根目录 `rules.md`。

## StarWork 文件边界

- 项目状态写入 `_系统/上下文/当前项目.md`，当前执行记录写入 `_系统/任务/当前工作.md`。
- 产品规划产物放 `docs/product/**`、`docs/plan/**`；开发代码放 `src/main/**`；测试代码放 `src/test/**`；验收报告放 `docs/acceptance/**`。
- 推广素材、选题、脚本、设计产物和数据分析分别放 `docs/marketing/material/**`、`docs/marketing/topics/**`、`docs/marketing/scripts/**`、`docs/marketing/design/**`、`docs/marketing/analytics/**`。
- Web 端工程位于 `../../前端代码/th4-decoboon-web/**`，固定使用 Vite + Vue 3 + TypeScript + Element Plus；Web 登录使用后端手机号 + BCrypt 密码 Provider，不保存或返回明文密码。
- Lane 的草稿、调研和中间产物放 `_系统/协作/lanes/<lane>/workspace/**`，不要把 lane workspace 当正式输出目录。
- 跨 lane 可见的正式输出登记到 `_系统/协作/shared.md`；成熟产物再晋升到项目正式事实源 `输出/确认成果/` 或对应业务目录。
- 每个 Agent 只主动修改自己 lane 的 `write_scope`；扩大范围前必须取得用户授权或登记共享请求。
- 项目 Skill 目录为 `.agents/skills/`；StarWork 机制状态只放 `.starwork/`。

## MultiAgent 协作规则

- 进入任务前先确认当前 lane、绑定 session、职责和 `write_scope`，未绑定 lane 前不得自行假定职责。
- 默认产品开发主流程为：`product-planning -> development -> acceptance -> product-planning`。
- 宣传推广营销支线当前全部暂停。`material`、`topic-selection`、`content-writing`、`design`、`data-review` 五个 Lane 已被用户禁用并释放绑定，不得派发新任务。
- 已禁用 Lane 的历史职责、worklog、脚本、设计草案、视觉产物和报告继续保留；只有用户明确要求恢复并重新绑定 session 后才能再次接收任务。
- 产品规划 lane 输出需求拆解、PRD/规格和可验收标准；开发 lane 输出实现、变更文件和验证证据；验收 lane 输出测试用例、缺陷结论和验收报告。
- 开发 lane 不得自行宣称验收通过；验收 lane 不得代替开发修改生产代码；产品规划 lane 不得绕过开发和验收直接修改生产实现。
- 里程碑交接必须包含：输入依据、changed_files / output_paths、verification、risks、next_action、acceptance_needed（按任务裁剪）。
- 接收方确认接手前，发送方只算“已交接”，不得把“已送达”表述为“任务已完成”。
- 每次任务结束更新所属 lane worklog；需要其他 lane 读取的成果登记 Shared Outputs。

## 生产后端监控

- 生产巡检、错误归档、代码缺陷修复和受控发版统一使用 `.agents/skills/backend-prod-sentinel/SKILL.md`。
- 只读巡检命令：`bash scripts/prod-monitor.sh collect "30 minutes ago"`。
- 自动修复只允许处理高置信、单根因、可回滚且不涉及 DDL/迁移/密钥/权限放宽/资金金额语义的代码缺陷；其余事故只记录并人工处理。
- 任何生产发版必须使用 `scripts/deploy-prod.sh`，失败自动回滚到 `/opt/decoboon/releases/.previous`。

## 五条铁律（th4 领域模型驱动设计）

1. **服务 = Provider（Controller 的唯一替代者）**，其本质是"为谁服务"——入参 `Request` 类型就是领域模型实体（如 `Asset`、`Lead`）。**工程中不存在 DTO**。
2. **前端传入的字段**，除统计类服务外，一律通过 **Provider 类内成员变量 + `@AutoInject` 注入 + `@NotNull/@NotEmpty/@Length` 校验**获取，不定义 Request 内部类。
3. **对外响应 = 具体实体对象类型**（`Asset`、`List<Asset>`、`Page<Asset>`），不做 VO/DTO 包装。
4. **实体采用对象关联建模，且所有业务实体必须归属企业**：如 `Asset` 关联业务员不是 `Long empId`，而是 `Employee emp` 字段 + `@ManyToOne(fetch=LAZY)` 映射；所有业务实体必须包含 `Company company`，**禁止裸外键 Long 字段**。
5. **实体序列化到前端要过滤属性**：具体 Provider 用 **`@Properties`**；只有无具体 Provider 的框架通用 `{model}-{service}` 路由才用 **`@ServiceProperties`**。**禁止 `@JsonIgnore`**。

## 权限先行（最高优先级）

任何业务模型的功能体系设计必须先定义**企业归属、角色能力、数据范围、操作动作**；查询、保存、更新、删除都必须有服务端强权限管控，前端隐藏菜单或按钮不构成权限。

- 登录 Provider 必须继承 `Security`，当前员工必须通过 `@User` 注入；当前企业、角色、门店只能从登录员工加载，禁止信任前端 `companyId/empId/ownerId`。
- 默认权限矩阵为四级：`BOSS=Company`、门店店长=`Store`、团队负责人=`Team`、普通员工=`Self`。角色与数据范围必须显式关联，禁止隐式放宽。
- th4 的对象领域查询只负责构建查询，不自动等于数据权限；有个人、门店或团队归属的实体必须用 `@DataPermission` 声明 `company/store/team/owner` 权限字段。
- `@ServiceProperties` 只控制通用 `{model}-{service}` 路由的响应字段过滤，禁止用它声明或替代查询权限；`@DataPermission` 管“查得到什么”，`@ServiceProperties` 管“返回什么”。
- 分页、列表、计数由 `DataScopeRequestFilter` 根据 `CriteriaQueryAware + @DataPermission` 在服务端统一追加条件，并由过滤器完成当前员工认证；具体业务 Provider 仍必须继承 `Security`，不能只依赖请求对象或前端查询参数。
- 框架通用 `service/info|update|delete` 由 `DataPermissionCrudAspect` 自动加载当前范围内实体并保护归属字段；自定义详情/更新/删除 Provider 必须使用 `DataScopeContext + DataScopeSpecification`，禁止 `findById(id)` 后判断。
- 保存时企业固定取登录员工所属企业；非 `BOSS` 的负责人强制为当前员工，`BOSS` 指定负责人时必须校验其属于当前企业，并由负责人归属复制 `store/team`。
- 业务表必须按实际权限维度建立联合索引：`(com_id, 业务主键)`、`(com_id, store_id, 业务主键)`、`(com_id, team_id, 业务主键)`、`(com_id, emp_id, 业务主键)`；`com_id` 始终是最左字段。
- 员工调店、调团队、业务转交时必须明确并测试归属快照规则；每个权限领域必须覆盖四角色、跨企业、伪造参数和越权详情/更新/删除测试。
- 客户域基准：客户包含 `company/store/team/owner` 对象归属，并建立 `(com_id, cust_id)`、`(com_id, store_id, cust_id)`、`(com_id, team_id, cust_id)`、`(com_id, emp_id, cust_id)` 联合索引。
- 完整的数据权限代码生成决策树、实体模板、通用 CRUD 模板、自定义 Provider 模板和测试模板见 `docs/design/07-数据权限设计规范.md`。

**平台运营例外：** 仅 `com.th4.edge.platform` 平台域允许跨企业只读和人工订阅管控。平台使用独立 `PlatformOrg/PlatformDepartment/PlatformUser/PlatformRole/PlatformPermission`，认证不得复用租户 `Company/Employee/Credentials`；权限集中在 `PlatformAccessContext` 校验。平台表与租户表禁止建立外键或实体关联，租户数据只能通过显式适配器以标量快照读取。所有人工写操作必须写 `PlatformOperationLog`。普通业务域禁止复制该模式。

## Provider 命名规范

- 结构：`{领域}.service.{业务对象}.{能力}` 声明 **interface**（定义 request/response 类型）+ `{能力}.support` 包写**实现类**（命名一个单词简单易懂，如 `Page`）。
- 能力名固定：`page / list / info / save / build / update / delete / count / enums / tree / report / unique`。
- 路由用 `@Service("{域}/{对象}/{能力}")`，**不再包含 `decoboon/` 或 `clue/` 前缀**（框架 `AbstractHttpProcessor` 仅按 `@Service` 扫描注册 Provider 路由，勿用 `@Component`）。
- `@ServiceName` 仅用于通用模型服务（`ModelService` 体系，如 `page/list/info/save/update/delete/build/count`）；注册、登录、短信发送、邀请等自定义业务 Provider 禁止使用。
- 需要登录、必须携带 token 的 Provider 接口必须继承 `Security`；当前登录员工通过 `@User` 注入，Request 为 `Employee` 时使用 `call(@User Employee employee)`，其他 Request 使用 `@User private Employee employee`。
- `@ModelSaveInject` 可在 `call` 入参加载并组装模型，`@ModelUpdateInject` 可按主键加载旧实体并合并字段；两者都不自动落库、不包含 `@DataPermission`。保存仍需 `save/persist`，带数据范围的自动更新优先走通用 `service/update`，自定义更新必须使用 `DataScopeSpecification`。
- 框架通用 ModelSave 实体的最终归属覆盖由 `AccessDataPermissionSaveRequestFilter` 在 `@ModelSaveInject` 之后执行；过滤器不可替代切面、保存和跨域查询权限。
- 框架通用 ModelUpdate 实体的数据范围加载和归属保护由 `DataPermissionCrudAspect + AccessDataPermissionUpdateRequestFilter` 共同完成，防止 `@ModelUpdateInject` 覆盖 `company/store/team/owner`。
- 统计域（`statistic`）是唯一允许语义化 Query 入参的地方，但禁止命名为 DTO。

## 通用组件与业务 Provider 边界

`com.th4.edge.components` 是通用技术框架分包，只承载与具体业务无关的短信、存储、支付协议等能力：

- **禁止**在 `components` 中放 `Provider`、`@AutoInject`、`@ServiceName`、前端路由或业务场景枚举。
- 通用能力接口放 `components.{组件}.service`，厂商实现放 `components.{组件}.sdk.{厂商}.{能力}`。
- 注册、登录、找回密码等业务场景枚举放在业务领域，如 `tenant/user/configure/UserVCodeScene`。
- 前端入口 Provider 必须放业务领域，如 `tenant/user/service/user/sms/send`、`tenant/user/service/user/register`。
- 业务 Provider 只注入通用能力接口（如 `Sender`、`Validator`），禁止直接处理 AccessKey、厂商签名、Redis Key 或 HTTP 协议。
- Spring Data JPA 只管理关系型数据库；Redis 对象使用 Spring Data Redis Repository（`@RedisHash` + `CrudRepository`），不可混称或用 JPA 管理 Redis。
- 跨实例互斥使用 Redisson 分布式锁；短信 DYPNS 的验证码生成、发送和校验由组件层完成，业务 Provider 只调用 `Sender/Validator`。

一句话边界：**components 决定“怎么发”，业务领域决定“何时发、发给谁、发完后做什么”。**

## 实体规范

- 继承 `AbstractModel`；类上 `@Model + @Note + @Entity + @Table`。
- 所有业务实体必须包含 `Company company` 企业归属，并使用 `@ManyToOne(fetch = FetchType.LAZY)` 映射。
- 关联字段：对象类型 + `@ManyToOne(fetch = FetchType.LAZY)` + `@JoinColumn(name = "...")`；字段语义名（`emp/company/video/template`）。
- 时间字段按用途选择：参与年/月/季度等多维统计的用框架 `DateTime`（`@Embedded` + `@AttributeOverrides`）；仅标记日期时间的用 `LocalDateTime` 或 `LocalDate`；审计时间默认用 `@CreateTime`/`@UpdateTime` + `DateTime`，但余额流水等纯流水时间可用 `LocalDateTime`。
- 枚举实现 `Enumerable<T>`；每个字段 `@Note`；金额/大小 `@Unit`。主键与枚举命名简洁，如 `subId`、`SubStatus`，不使用 `subscriptionId`、`SubscriptionStatus`。
- 所有 JPA 业务主键必须使用 `@Id + @SnowflakeId`，并声明 `@EntityListeners(SnowflakeEntityListener.class)`；数据库禁止 `AUTO_INCREMENT`，禁止使用可预测的顺序主键。
- `context` 包下的横切/领域上下文统一以 `Context` 结尾（如 `TenantContext`、`SubscriptionContext`），不命名为 `XxxService`。
- **可搜索字符串字段加 `@Keyword`**（全局搜索 keyword 全字段 OR 匹配，详见 rules.md §4.3.1）。
- 业务实体必须按权限查询建立 `(com_id, 业务主键)` 和 `(com_id, emp_id, 业务主键)` 联合索引；其他范围维度按 `(com_id, 范围字段, 业务主键)` 扩展。
- 禁止实体写业务逻辑；禁止 `@JsonIgnore`。

## 事务边界与懒加载禁则（强制）

生产 `application-prod.yml` **必须保持 `spring.jpa.open-in-view: true` 打开**（项目强制要求，2026-09-16 起生效）：生产为回环绑定部署，请求级 Session 用于兜底关联加载，关闭后会导致"本地全绿、上线必崩"的 `LazyInitializationException`（本工程已发生两次：`Employee`、`PlanVersion`）。

⚠️ OSIV 打开只是**运行时兜底**，不改变代码要求：业务代码仍不得依赖它，必须做到"关闭 OSIV 也正确"。

- 禁止在 `RequestFilter`、`@Component` 上下文类、工具类等**无事务边界**的代码中访问实体的 LAZY 关联字段。
- 事务外必须读取的关联，查询上必须 `@EntityGraph(attributePaths = {...})` 或 `JOIN FETCH` 预取；聚合/校验上下文方法应置于 `@Transactional(readOnly = true)` 内。
- 禁止以 `dev`/`test` 通过作为依据；涉及关联的回归测试必须显式 `spring.jpa.open-in-view=false`，且测试方法不得加 `@Transactional`。
- 禁止用临时 `EAGER` 或 `@JsonIgnore` 绕开；必须排查同类链路，而不是只修一处。
- 错误集与检测清单见 `_系统/协作/lanes/development/workspace/code-error-catalog.md`；完整规范见 `rules.md` §4.5。

## 序列化控制

具体 Provider 直接在类上使用 `@Properties(ignore = @IgnoreProperties(model = X.class, properties = {...}))` 过滤当前服务响应。只有框架默认支持的 `{model}-{service}` 路由没有对应 Provider 时，才在模型或统一配置上使用 `@ServiceProperties(uris = "...", properties = @Properties(...))` 按 URI 配置。

- 响应字段必须按 `page/list/info/save/update` 场景分别设计，优先满足当前前端实际使用的数据，不返回完整领域聚合。
- 关联对象使用 `@AllowProperties` 最小白名单；列表只返回摘要和最小负责人引用，详情才返回详情页字段，保存/更新不返回无关归属树。
- 需要重视懒加载序列化带来的额外 SQL；响应白名单不得引用页面不需要的关联。`@Properties/@ServiceProperties` 只控制输出，不替代数据权限。

## 租户订阅约定

- 订阅主体为 `Subscription`，主键 `subId`，状态枚举 `SubStatus`。
- 剩余可用天数不落库：由 `serviceEndTime - 当前时间` 动态计算，禁止保存 `balanceDays`，禁止每日递减订阅天数。
- 门店默认不体现：注册仅创建默认总店；多门店是套餐/权益能力，由 `Plan.maxStores` 控制，`Company.setting.enableStore` 控制前端是否展示。
- 免费版、基础版、专业版和旗舰版均提供全部业务模块；版本差异只允许体现在门店数和席位数，禁止以供应链、财务、经营分析等模块功能作为版本门槛。

## 成本与利润口径（强制）

- 销售价与成本价分离维护：报价库表单只维护 `materialPrice/manualPrice` 等销售价；标准成本由有效 `PartnerPrice` 的采购价与人工/班组价形成，业务代码禁止接受前端直接覆盖 `CatalogItem.costPrice`。
- 采购价按企业、供应商、报价项目和有效期管理；BOSS、店长、财务可维护，材料员等角色不得读取采购成本字段。
- 项目成本统一由 `ProjectCostContext` 汇总：`estimatedCost` 使用报价成本快照，缺少快照时按当前标准采购成本回算；`actualCost` 优先采购入库等实际成本，缺少实际值时按当前采购价回算，采购价变化必须反映到毛利。
- 任一应计成本缺失时，`costComplete=false`，利润和毛利率必须返回 `null`，结算持久化字段允许为空；前端统一显示“成本未完整”，禁止以 0 成本计算或展示伪利润。
- 实际材料成本只在采购入库时累计；领料、损耗只流转库存，退换货按退货类型冲减，禁止重复累计采购金额。
- 成本错误集见 `_系统/协作/lanes/development/workspace/code-error-catalog.md` 的 ECD-002～ECD-004。

## 完整规范

全部细节（含逐段代码示例、命名速查表、代码生成自检清单）见工程根目录 **`rules.md`**，两者冲突时以 `rules.md` 为准并同步修订本文件。
