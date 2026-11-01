# AGENTS.md — th4-decoboon 代码生成规范（AI 助手必读）

本工程是安徽昭青「装得清」SaaS 平台的后端（Java 22 + Spring Boot 3.4 + th4 框架）。
**所有新增/修改/重构代码必须严格遵循本规范**；与既有旧写法冲突时，以本规范为准并回改存量代码。


## 试运营单人模式（当前最高优先级）

- 当前为试运营阶段，研发协作只有一人，以成本和响应速度优先。
- 小修复和普通需求默认直接使用 `dev`；不强制 lane、独立验收、RC tag 和全量测试。
- 默认验证按变更级别执行：定向测试 + 编译/类型检查/构建 + 必要时生产冒烟。
- 仅 DDL、迁移、权限、金额/成本/利润、支付、跨租户和数据删除保持全量测试、备份、回滚和用户确认。
- 本文件后续的 lane、严格发布和全量验收条款与本节冲突时，以本节和
  `docs/operations/试运营单人轻量协作与发版规范.md` 为准。

## 研发运营章程与统一变更记录（强制）

- 所有新增、修改、修复和发布，必须先按 `docs/operations/研发运营章程.md` 判定风险级别（L0–L3），并使用 `docs/operations/变更记录模板.md` 建立 `CHG-*` 记录。
- L0 文案/样式只验证并发布受影响端，禁止跑无关后端全量；L1+ 没有统一变更记录不得发布；L2/L3 涉及跨端或高风险时必须补齐对应证据和人工门禁。
- 构建必须来自已进入 `origin/test` 的提交；部署必须满足产物 `SOURCE_COMMIT=HEAD=origin/prod=origin/test=origin/dev`，并使用同一不可变制品。
- 热修必须回灌 `prod/test/dev`；未回灌、契约字段缺失或复用历史制品时，发布脚本必须失败。
- 禁止为单个事故无限追加检查。只有重复回归、跨端契约或不可逆风险才新增硬门禁，新增门禁必须包含负向验证。


## Web UI 智能体边界（2026-10-28）

- 以下旧智能体全部禁用，不得调用、恢复或重建：
  - `zhuangdeqing-web-ui-interaction`
  - `zhuangdeqing-product-web-development`
  - `zhuangdeqing-web-immersion-audit`
  - 前端仓库 `decoboon-web-frame`
- 唯一启用的 Web UI 智能体：`.agents/skills/zhuangdeqing-ui-craft/`。
- Web UI 设计、改造、视觉批判和交付门禁必须使用 `zhuangdeqing-ui-craft`，并以 `references/experience-charter.md`、`visual-system.md`、`workbench-patterns.md`、`zcool-research.md` 和 `review-rubric.md` 为执行依据。
- 旧技能和历史规范仅作人工参考，不得恢复，也不得绕开新智能体另建第二套 UI 规则。
- 恢复旧智能体必须经用户明确授权，并同步更新 `.starwork/skills.json`、本文件和 `.starwork/disabled-skills.json`。

## 推广前最低业务闭环门禁（2026-10-28，强制）

- 首发推广前必须以 `docs/product/01-推广前最低业务闭环与跨端质量门禁.md` 为统一关门标准。
- 只围绕一个入口场景收口：**签约后工地不乱**；变更、材料、业主、验收和钱是支撑场景，不再并列扩展功能。
- 任一核心能力必须在 App、Web、业主端和后端具备同 Provider、同权限、同状态、同金额/成本语义和同事实回读；只验证一端不得声称完成。
- 页面信息必须服务于判断、行动、交接、确认、留证或对账；仅有字段、数字、状态或入口而无法推动业务动作的，不算交付。
- Web 核心页面必须使用唯一 Token 真源并通过真实渲染视觉门禁；App 核心页面必须通过多机型、七态、弱网和真机证据门禁。
- 未完成范围冻结、真实项目闭环、独立验收和营销事实映射前，状态只能是“产品验证中”，不得宣称完整闭环、已验收或可规模化推广。

## 业务信息项与闭环门禁（2026-10-28，强制）

- 业务信息架构事实源为装得清业务智能体 `BR-059`、`OBJ-010` 与 `00-治理/业务信息项注册表-v0.1.json`；研发执行标准见 `docs/product/02-业务信息架构与字段闭环标准.md`。
- 新增或修改实体字段、Provider 请求/响应、页面表单或派生指标前，必须先登记或引用唯一信息项 ID，明确主概念、主源、维护角色、应用场景、下游消费者和闭合业务动作。
- 一个信息项只能有一个主源；引用、快照、投影、派生和证据必须显式区分。主数据变化不得回写已冻结的报价、合同、任务、材料和成本快照。
- AI Prompt、Excel 导出、详情展示和搜索索引不等于业务使用；只有改变对象、状态、责任人、任务、确认、金额、成本或对账事实，才算形成业务闭环。
- 没有下游消费者、没有闭合动作、或无法处理缺失/失败/权限/历史快照的新增信息项，不得进入正式表单和研发。

## 业务对象选择器与选择结果展示（强制，2026-11-01）

- 跨端唯一执行规范：`docs/design/35-业务对象选择器与选择结果展示规范.md`；App、Web、Admin 新增或改版业务对象选择器、表单结果区和相关门禁必须遵循。
- 员工、项目、客户、房屋、报价、合同、工人、供应商、应收、应付、材料计划、模板、报价分项、账务分类等业务对象，禁止用原生 `<picker>`、`el-select`、可筛选下拉或 `JzSelectField` 承载选择；固定短枚举和有界子集合例外必须按规范判定并在 T15 声明。
- 专用选择器必须打开即展示业务数据，提供业务责任/状态/类别导航、服务端搜索分页、加载/空/失败/无权限/失效状态、选择/清除/取消、一次性回传和服务端复核；禁止固定 `size: 200`、`findAll` 或全量拉取后客户端过滤。
- 来源页面不得只显示名称或“已选择某某”；表单中的业务对象必须使用全宽已选业务卡，展示名称、状态、关键业务事实、更换和清除。列表筛选可使用紧凑摘要，但必须明确当前筛选。
- 新增或改版前必须走 `T15`；涉及表单补 `T16`；稳定单据补 `T17` 并取得 T06/T12 裁决。选择、清除、依赖失效和回显必须进入验收。
- Web 检查命令：`npm run check:forms`、`npm run check:concepts`；App 检查命令：`npm run check:selectors`、`npm run check:finance-pickers`。静态通过不等于全量合规，必须包含真实页面/真机证据和负向样本。
- 当前基线结论与存量问题见规范第 10 节；在 P1 问题闭环前，不得宣称 App/Web 业务对象选择器已全量统一。

## 业务界面内部编号隐藏（强制）

- 业务事实源：`DEC-095`、`BR-060`。
- 员工编号、企业编号、客户编号，以及系统生成的报价编号、合同编号、项目编号、方案编号、费用编号、凭证编号、邀请编号，默认不得出现在 App、Web、Admin 的页面、列表副标题、详情字段、表单提示和搜索提示中。
- 用户应通过名称、地址、角色、状态、金额、日期和责任人识别对象；界面不得要求用户记忆内部编号。
- 编号保留在数据库主键、Provider、权限校验、审计、日志、二维码、文件命名和要求凭证号的法定打印模板中，不得因隐藏展示而删除字段或改变接口契约。
- 统一社会信用代码、法定证照编号、发票法定号码等不属于系统内部编号，按对应业务场景和权限保留。
- Web 构建必须执行 `npm run check:identifier-exposure`，负向自测为 `npm run check:identifier-exposure:self-test`。App/Admin 后续涉及同类页面时必须使用同一业务规则并补对应门禁。

## 一后端多客户端同源（最高优先级）

- 装得清只有一个业务后端；App、Web、Admin 只是不同客户端，不得拥有独立后端、独立业务库或同义业务接口。
- 同一业务能力必须复用同一 Provider，并保持同数据、同请求/响应语义、同权限与数据范围、同状态机、同金额/成本/利润口径、同错误码和同恢复路径。
- 客户端只允许页面布局、交互密度和端侧能力差异；禁止在前端复制业务规则、金额公式、状态机或正式业务默认值。
- 正式业务字段缺失、接口失败或无业务记录时，必须显示“暂不可确认”并给恢复路径，禁止用 0、22、空数组或本地常量伪装事实。
- 新增/修改跨端能力前必须完成 App/Web Provider 对账矩阵；只验证一端不得声称跨端对齐。
- 线上问题必须先在受影响生产端闭环，未闭环前不得扩展相邻业务批次。
- 完整强制规范：`docs/design/31-一后端多端同源架构规范.md`。

## 复杂业务域交付协议（强制）

- 当用户要求“启动 / 继续 / 恢复 / 审查 / 对账某个业务域”时，必须先读取 `.agents/skills/domain-delivery-orchestrator/SKILL.md`，以及 `docs/product/domains/<domain>/state.json`、`00-charter.md`、`10-reconciliation.md`。
- 不得依赖聊天记忆推进复杂业务域；一次只做一个垂直切片批次，按 `INTAKE → ... → LEARNED` 门禁推进。
- 业务对象、状态、权限、金额、成本和对外口径必须先经 `zhuangdeqing-business` 裁决；没有 `BIZ_ALIGNED` 不得进入编码。
- 每批必须更新状态账本、对账矩阵和证据；没有真实页面/真机证据不得声称 UI 通过，开发自测不得冒充独立验收。
- 初始化领域包使用：`python3 .agents/skills/domain-delivery-orchestrator/scripts/domain_state.py init ...`；恢复时使用 `show`，门禁前使用 `validate`。

## StarWork 与轻量入口

默认只读：

1. `AGENTS.md`
2. `docs/operations/研发运营章程.md`
3. `docs/operations/变更记录模板.md`
4. `docs/operations/试运营单人轻量协作与发版规范.md`
5. `_系统/协作/工作约定.md` 的近期改动
6. 与任务直接相关的源码、迁移和最近发布记录

lane、完整 worklog、shared 和 rules 大段只在事故、大功能、权限/金额/迁移变更或用户要求时读取。

涉及后端实现时按需读取根目录 `rules.md` 对应章节，不全文预读。

## StarWork 文件边界

需求与规格 `docs/product/**`、`docs/plan/**`；实现 `src/main/**`；测试 `src/test/**`；验收 `docs/acceptance/**`；
营销素材/选题/脚本/设计/分析分别落 `docs/marketing/{material,topics,scripts,design,analytics}/**`；
项目状态写 `_系统/上下文/当前项目.md`，执行记录写 `_系统/任务/当前工作.md`；草稿与调研放本 lane `workspace/**`，成熟成果晋升到 `输出/确认成果/` 或对应业务目录。

## MultiAgent 协作规则（试运营模式简化）

- 试运营单人模式下，不强制 lane、跨 lane 交接和独立验收。
- 只有用户要求、大功能或高风险变更才启用多 Agent 协作。
- 多 Agent 之间以统一 `CHG-*` 记录交接，不以聊天记忆作为状态源；开发自测、独立测试和发布结论分别留证据。
- 业务对象、状态、权限、金额、成本、支付和对外口径仍必须先查业务事实源。
- 工作记录按 `工作约定.md` 一行追加，不为小任务写交接包。


## 装得清内容团队智能体（2026-10-29）

- `zhuangdeqing-marketing-content`：负责将已核验装企场景转化为对外营销内容资产、价值主张、内容矩阵、短视频/图文简报和证据边界，向现有营销执行智能体移交。它不定义产品事实、不直接发布。
- `zhuangdeqing-usage-content`：负责入驻路径、角色教程、页面理解、App/Web 对比、FAQ、异常恢复和企业实际业务与产品模型的兼容台账。它不定义产品事实、不修改产品、不制作营销主张。
- 两个智能体均以 `zhuangdeqing-business` 为唯一业务事实源，并共同遵守 `docs/product/decisions/USER-DEC-20261029-001-线上产品渐进迭代与AppWeb定位.md` 与 `USER-DEC-20261029-002-装得清内容团队职责边界.md`。
- 内容团队协作入口：`docs/product/content-intelligence/00-装得清内容团队协作章程.md`。
- 知识库：`推广营销/装得清营销内容智能体/`、`推广营销/装得清使用内容智能体/`。

## 装得清独立验收执行智能体（2026-11-01，强制）

- 独立验收执行只使用 `.agents/skills/zhuangdeqing-acceptance-agent/`，不得再建第二套验收执行智能体。
- 执行前必须读取 `docs/testing/` 的测试计划、冻结用例、Oracle Lock、追溯矩阵和未映射台账；预期结果只来自业务事实源，不得从当前代码或页面行为反推。
- 页面按钮是业务动作第一入口；Provider、数据库、日志只用于核验和受控夹具，不能用 API 通过替代页面可用。
- 执行智能体不得修改业务规则、测试用例、Oracle、产品代码、权限、金额或数据终态；发现问题只登记 `DEF-*` 并交研发修复。
- 每次执行必须创建唯一 `RUN-*`，保留角色、租户、端面、页面路径、代码版本、数据前缀、截图/网络/接口/对账证据和缺陷关联。
- 无证据不得 `PASS`；开发自测、单测和代码变更不能替代独立复验；最终发布和高风险业务结论仍由用户授权主体决定。
- 标准命令：`python3 .agents/skills/zhuangdeqing-acceptance-agent/scripts/create_run.py ...`；关门前运行 `validate_run.py <run-dir> --final`。

## 生产后端监控

- 生产巡检、错误归档、代码缺陷修复和受控发版统一使用 `.agents/skills/backend-prod-sentinel/SKILL.md`。
- 只读巡检命令：`bash scripts/prod-monitor.sh collect "30 minutes ago"`。
- 自动修复只允许处理高置信、单根因、可回滚且不涉及 DDL/迁移/密钥/权限放宽/资金金额语义的代码缺陷；其余事故只记录并人工处理。
- 任何生产发版必须使用 `scripts/deploy-prod.sh`，失败自动回滚到 `/opt/decoboon/releases/.previous`。

## 小程序发版边界（强制）

- Codex 只负责小程序构建、开发版上传和体验版发布/验证；**正式版提审与发布由用户本人完成**。
- 严禁在微信公众平台点击“提交审核”“发布”等正式发版动作，也不得代填审核测试账号或提交审核表单。
- 上传成功后只报告版本号、上传信息路径、体验版状态和待用户完成的正式版步骤，然后停止。
- 用户明确要求“审核发布”时，仍按本条执行：完成体验版即止，正式版交由用户。

## 版本管理与发布（试运营轻量）

- 默认直接使用 `dev`；大功能可选 `feature/*`，生产事故用 `hotfix/*`。
- 小修复不强制全量测试、RC tag 和独立验收；按变更级别执行定向测试、构建和冒烟。
- 发布成功后把 `test`、`prod` 快进同步到 `dev` 当前提交；tag 按天、周或批次创建。
- 构建前运行 `scripts/verify-release-baseline.sh --build`；部署前运行 `--deploy` 和 `scripts/verify-release-contracts.py`，不得绕过硬门禁。
- DDL、迁移、权限、金额/成本、支付、跨租户和数据删除仍必须备份、全量验证和可回滚。
- 完整规则见 `docs/operations/试运营单人轻量协作与发版规范.md`。

## 实际业务场景推演（强制）

- 任何新功能在定义实体、字段、页面和权限前，先写清至少 2 个真实中小装企场景，含「使用人、触发事件、当前动作、系统下一步、异常处理」，没有闭环不得进入编码。
- 默认路径只服务最高频场景；低频/高风险字段按需展开，禁止把大型 ERP 的完整模型变成中小装企必填项。
- 详细口径见 `rules.md` §0.1。

## 可完成状态投影（强制）

首次入驻、实施清单、待办、进度、审批、质检清单等必须：编码前冻结 `key/label/required/完成条件/异常态`；
「完成」只能由当前租户真实领域对象推导（禁止 `done:true`、演示数据、前端本地布尔）；分母区分必做与可选；
新增/编辑/删除/状态变化后必须重新读取；接口异常表达为「暂不可确认」并保留恢复路径，禁止用空数组伪装完成。
详细门槛见 `rules.md` §13 与 `ECD-008`。

## 五条铁律（th4 领域模型驱动设计）

1. **服务 = Provider**（唯一取代 Controller），入参 `Request` 即领域模型实体，工程中不存在 DTO。
2. 前端字段一律用 **Provider 类内成员变量 + `@AutoInject` + 校验注解**获取，不定义 Request 内部类（统计域例外）。
3. 对外响应 = **具体实体对象类型**（`Asset`/`List<Asset>`/`Page<Asset>`），不做 VO/DTO 包装。
4. 实体用**对象关联建模**，`Employee emp` 而非 `Long empId`；所有业务实体必须含 `Company company`。
5. 实体序列化到前端必须按服务用 `@Properties`（通用路由用 `@ServiceProperties`）过滤，**禁止 `@JsonIgnore`**。

逐条示例与自检清单见 `rules.md` §0、§3、§4、§5、§8。

## 权限先行（最高优先级）

登录 Provider 必须继承 `Security`，当前员工用 `@User` 注入；企业/角色/门店只能从登录员工加载，禁止信任前端 `companyId/empId/ownerId`。
默认矩阵 `BOSS=Company / 店长=Store / 团队负责人=Team / 普通员工=Self`；员工角色固定 7 类（Boss、StoreManager、Designer、Marketer、Pm、Finance、TeamLeader），禁止新增重复角色。
`@DataPermission` 管查得到什么，`@ServiceProperties` 管返回什么，不可互替；分页/列表/计数由 `DataScopeRequestFilter` 统一追加范围；
自定义详情/更新/删除必须用 `DataScopeContext + DataScopeSpecification`（禁止 `findById` 后自行判断），保存时企业取登录员工所属企业、非 BOSS 负责人强制为本人。
业务表按权限维度建联合索引，`com_id` 始终最左：`(com_id, 业务主键)`、`(com_id, store_id/team_id/emp_id, 业务主键)`。
平台运营例外：仅 `com.th4.edge.platform` 允许跨企业只读与人工订阅管控，禁止与租户表建立外键或实体关联。
决策树与模板见 `rules.md` §0.2、`docs/design/07-数据权限设计规范.md`。

## Provider 命名规范

`{领域}.service.{对象}.{能力}` 声明 interface、实现放 `{能力}.support`；能力名固定 `page/list/info/save/build/update/delete/count/enums/tree/report/unique`。
路由 `@Service("{域}/{对象}/{能力}")`，不带 `decoboon/`、`clue/` 前缀，不可用 `@Component` 代替；`@ServiceName` 仅限通用 ModelService。
需登录接口继承 `Security`：Request 为 `Employee` 用 `call(@User Employee employee)`，否则 `@User private Employee employee`。
`@ModelSaveInject/@ModelUpdateInject` 只负责加载组装（不落库、不含数据权限），自定义更新仍走 `DataScopeSpecification`。详见 `rules.md` §3。

## 通用组件与业务 Provider 边界

`components` 只放与业务无关的技术能力（短信、存储、支付协议），禁止出现 Provider、`@AutoInject`、`@ServiceName` 或业务枚举；
业务场景枚举与前端入口 Provider 放业务领域，业务只注入通用能力接口。详见 `rules.md` §2.3。

## 实体规范

继承 `AbstractModel`，类上 `@Model + @Note + @Entity + @Table`；主键 `@Id + @SnowflakeId` 并声明 `@EntityListeners(SnowflakeEntityListener.class)`，禁止自增与顺序主键。
关联用对象类型 + `@ManyToOne(fetch = LAZY)` + `@JoinColumn`，业务实体必须含 `Company company`，禁止裸外键 `Long`；时间字段按用途选 `DateTime`/`LocalDateTime`，枚举实现 `Enumerable<T>`，可搜索字段加 `@Keyword`，实体不写业务逻辑、禁止 `@JsonIgnore`。
长文本字段必须与迁移一致（`ddl-auto: update` 不改既有列类型，需补 `alter table ... longtext`，见 `ECD-009`）。
字段、索引与示例见 `rules.md` §4。

## 事务边界与懒加载禁则（强制）

生产 `application-prod.yml` 必须保持 `spring.jpa.open-in-view: true`（回环部署兜底）；但业务代码必须做到「关闭 OSIV 也正确」：
禁止在无事务边界的 `RequestFilter/@Component`/工具类中访问 LAZY 关联；事务外必须读取的关联用 `@EntityGraph` 或 `JOIN FETCH` 预取，
或把聚合逻辑放进 `@Transactional(readOnly = true)`；禁止用临时 `EAGER`、`@JsonIgnore` 或「dev 通过」替代回归。
**只读事务禁锁**：`@Transactional(readOnly = true)` 及其**间接调用链**禁止调用带 `@Lock(PESSIMISTIC_*)` 的仓储方法（MySQL 会报 1792）；凡仓储方法名含 `ForUpdate` 或以 `findLockedBy` 开头，其调用方必须处于**写事务**（见 `ECD-012`）。

涉及关联的测试必须显式 `open-in-view=false` 且不加 `@Transactional`。详见 `rules.md` §4.5 与错误集 `ECD-*`。

**API 高效 SQL 与并发锁（强制）**：th4-framework 通用 API 低效时允许直接使用更高效 SQL（须保留租户/数据权限、参数绑定与金额口径）；业务并发禁止数据库锁（`@Lock(PESSIMISTIC_*)`、`FOR UPDATE`、表锁/间隙锁），统一使用 `RedissonLock` 分布式锁；详见 `rules.md` §0.3 与 `DEC-029`/`BR-027`。

**统计汇总实现逻辑（强制，2026-10-07 新增）**：统计/汇总/看板/报表/排行类接口的返回结果是数字，必须用**聚合/投影 SQL** 直接算出（标量返回 O(1) 行、列表 `ORDER BY ... LIMIT N` 且字段投影）；**禁止**「全量实体 + Java 循环求和」「循环体内查库」「全量后取前 N」「用重 JOIN 换 SQL 条数」「用方法内部读数充当接口验收数」「逾期/未收判定放在 Java 循环」；业务公式类允许内存计算但循环体内零查询，金额口径不得变。验收：端到端 SQL ≤30 条、并发 P95 ≤500ms、10× 数据量下 SQL 条数不增长，未达标即阻断。详见 `rules.md` §0.4 与 `DEC-036`/`BR-031`/`GAP-027`。

**分页与聚合接口（强制，2026-10-31 新增）**：所有读取接口先分类为 `PAGE_LIST / PICKER_PAGE / BOUNDED_LIST / REPORT_SUMMARY`。增长型列表必须数据库分页，默认 `size=20`、普通列表和选择器硬上限 `50`；禁止固定 `size=200/500`、`findAll` 后 `subList` 分页、`PageRequest.of(0,500)` 再 Java 过滤。金额、计数、比例、排行、趋势必须调用独立 `report/summary/overview` Provider，前端不得对业务列表做 `reduce/filter().length/groupBy` 生成正式指标。同筛选下必须验证列表 `totalElements == report.count`，并完成 10× 数据量、响应行数和 P95 验证。详见 `rules.md` §0.4.5 与 `docs/design/33-后端分页与聚合接口强制规范.md`。

## 后端接口查询与业务编排（强制，2026-10-31 新增）

- 一个 Provider 只服务一个真实用户目标。编码前必须写清使用人、触发时点、最短动作、下游消费者、数据增长、异常路径和验收；禁止按表或页面字段直接堆“万能接口”。
- 每个接口先分类为 `READ_DETAIL / PAGE_LIST / PICKER_PAGE / BOUNDED_LIST / REPORT_SUMMARY / EXPORT_JOB / WRITE_ACTION / BATCH_ACTION`，并声明返回规模、查询链、最大行数和 SQL 是否随数据量增长。
- 禁止 N+1：循环体、`forEach`、`stream.map/flatMap` 内不得调用 Repository 或触发 LAZY 查询；当前页关联必须批量 `IN` 或投影装配。
- 禁止全量 JPA 实体加载后 Java `filter/sum/count/groupBy/reduce` 生成业务指标；金额、计数、比例、排行、趋势、逾期/未收必须聚合/投影 SQL 下推。
- 详情只返回根对象和有限摘要；增长型历史必须独立分页。批量写入必须一次批量加载/校验后批量写入，禁止循环查库。
- 查询接口不得有业务副作用；写接口不得顺带重算整页工作台。业务并发用 Redisson，只读事务禁止悲观锁。
- 新增/修改接口必须保留场景卡、查询链、权限负向、同筛选对账、10× 数据量和 P95/响应体证据。完整规范见 `docs/design/34-后端接口查询与业务编排强制规范.md`，关联 `rules.md` §0.7、31–33 号设计规范与 `CHG-20261031-049`。

## 序列化控制

具体 Provider 用 `@Properties(allow=...)` 按 `page/list/info/save/update` 裁剪响应，通用 `{model}-{service}` 路由才用 `@ServiceProperties`（只管返回，不管权限）。
每个新增或修改的 Provider 必须先分类为 `ENTITY_GRAPH / READ_MODEL / SCALAR_BINARY / FRAMEWORK_MODEL_ROUTE`；实体图必须在具体 `support` 实现类上声明 `@Properties(allow=...)`，根模型和所有实际输出的关联模型都要各自白名单。
框架没有 Provider 级白名单时默认全放行，且白名单不会自动级联；`@Properties` 只负责输出裁剪，不负责关联预取。禁止用 `@JsonIgnore`、`EAGER`、双向关系注解或前端字段过滤绕过。
关联对象用 `@AllowProperties` 最小白名单：列表给摘要、详情给页面实际使用字段，避免 `Property -> Store -> Team -> Employee` 循环。
**对外可见投影必须显式组装（强制）**：读取投影不得直接返回实体，`status`、`currentStageName`、计数、金额汇总等派生字段必须按领域事实现场推导；
业主详情 `customer/project/info` 与 `customer/project/page`、`journey/info`、员工侧 `project/project/page|overview` 必须同源同口径，禁止用前端兜底掩盖后端未组装。
涉及实体图的 Provider 必须有真实 JSON 契约测试，并在 `spring.jpa.open-in-view=false` 且测试方法不加 `@Transactional` 的条件下通过。
后端 Provider 响应改动必须运行 `python3 scripts/audit-provider-response-contract.py --check`，发布构建已强制执行；门禁规则自测使用 `--self-test`。
详见 `rules.md` §5、§5.6、§8、`docs/design/32-Provider接口响应序列化与懒加载安全规范.md` 与 `ECD-011`。

## 租户订阅约定

订阅主体 `Subscription`（主键 `subId`、状态 `SubStatus`）；剩余天数由 `serviceEndTime - now` 动态计算，禁止落库 `balanceDays` 或每日递减；
门店默认不体现（注册只建默认总店，多门店由 `Plan.maxStores` + `Company.setting.enableStore` 控制）；各版本均提供全部业务模块，差异只在门店数与席位数。详见 `rules.md` §11。

## 性能问题定位（强制）

性能问题必须先测量、再优化，只优化实测占比最大的那一段（`DEC-037` / `BR-032`，细则见 `rules.md §0.5`）。

- **指标分层**：L1 用户感知（首屏 / P95）> L2 传输（响应体大小 / TTFB）> L3 服务端（序列化 / GC / 业务计算）> L4 存储（SQL 条数 / 慢查询）。
- **SQL 条数、循环查询数量是防劣化辅助指标，不得作为优化方向**；L4 指标改善而 L1 指标未改善即判**劣化**，必须回滚。
- 定位第一步必须是**端到端采样**（`curl -w` 的 TTFB / size / total，≥10 次），禁止先改代码后验证。
- **响应体大小为一等公民**：列表/驾驶舱接口必须最小字段白名单 + 条数上限 + 体积评估；嵌套关联只给摘要，禁止复用主实体全量白名单。
- **止损**：同一指标连续 2 次优化无改善 → 停止该类优化并重新测量；「内部指标已干净但延迟仍高」必须立即转层。

## 成本与利润口径（强制）

- 销售价与成本价分离：`CatalogItem.materialPrice/manualPrice` 为销售价、`costPrice` 为标准成本；成本价仅企业负责人、店长、财务可见。
- 项目成本统一由 `ProjectCostContext` 汇总：预计成本优先签约快照，实际成本优先采购到货/入库；任一应计成本缺失时 `costComplete=false`，
  利润与毛利率必须返回 `null`，前端显示「成本未完整」，**禁止以 0 成本计算或展示伪利润**。
- 实际材料成本只在到货/入库累计，领料与损耗只流转库存，退换货按退货类型冲减，禁止重复累计；历史价 `PartnerPrice` 不再作为标准成本必需来源。
- 详见 `rules.md` §10 与错误集 `ECD-002~004`。

## 品牌 Logo 统一（强制）

Logo 只能使用 `logo-export` 同一套 Mark（深色背景白色版、浅色背景彩色版）；App 只用 `JzBrandLogo`，Admin 只用 `BrandLogo`，Site 只用 `brand-logo`，禁止页面另存图片或复制路径。
资源路径、校验值与验收命令见 `rules.md` §12。

## 完整规范

全部细节（含逐段代码示例、命名速查表、代码生成自检清单）见工程根目录 **`rules.md`**，两者冲突时以 `rules.md` 为准并同步修订本文件。

## 装得清业务智能体咨询门禁（2026-09-18）

涉及业务对象、状态机、角色权限、数据范围、金额、成本、利润、指标、客户可见性、装企场景匹配或对外宣传口径时，必须先调用全局技能 `/Users/zhangwei/.agents/skills/zhuangdeqing-business/SKILL.md`（全局技能 `zhuangdeqing-business`） 并读取规范事实源。

- 需求定义前：确认场景、角色、对象、前置条件、状态变化、例外和验收标准。
- 编码实现前：确认规则来源、权限范围、金额/成本口径、历史数据和幂等要求。
- 验收前：按业务条件矩阵覆盖正向、反向、越权、重复和边界用例。
- 对外表达前：确认知识状态为 `verified`，并核验证据、授权和不可承诺边界。
- 发现口径冲突时建立 `GAP-*`，不得自行选择更省事的版本。
