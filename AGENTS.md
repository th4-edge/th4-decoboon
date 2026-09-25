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
10. `_系统/协作/工作约定.md`（省 Token 执行口径与近期改动简报，同一话题续做的唯一入口）

涉及用户偏好或长期背景时读 `_系统/身份/README.md`；涉及高复用经验或已知风险时读 `_系统/教训/README.md`；涉及后端实现时还必须读取根目录 `rules.md`。

## StarWork 文件边界

需求与规格 `docs/product/**`、`docs/plan/**`；实现 `src/main/**`；测试 `src/test/**`；验收 `docs/acceptance/**`；
营销素材/选题/脚本/设计/分析分别落 `docs/marketing/{material,topics,scripts,design,analytics}/**`；
项目状态写 `_系统/上下文/当前项目.md`，执行记录写 `_系统/任务/当前工作.md`；草稿与调研放本 lane `workspace/**`，成熟成果晋升到 `输出/确认成果/` 或对应业务目录。

## MultiAgent 协作规则

- 进入任务先确认当前 lane、绑定 session、职责与 `write_scope`；未绑定 lane 不得自行假定职责，只主动改本 lane 的 `write_scope`。
- 主流程 `product-planning -> development -> acceptance -> product-planning`；产品只出需求/规格，开发只出实现与自测，验收只出结论，互不代替。
- 项目内 5 个营销 Lane（material / topic-selection / content-writing / design / data-review）默认不由研发主流程派发；营销问题走
  `MKT_REPORTED -> 业务裁决 -> 研发修复 -> LEARNING_ACK -> VERIFIED -> MKT_UNLOCKED`，未收到 `MKT_UNLOCKED` 前双方不得对外使用待修复口径。
- 交接必须含：输入依据、changed_files、verification、risks、next_action、acceptance_needed；跨智能体修复另含 ISSUE-ID、业务 T12、重启/部署证据、learning_request、可用/禁用口径。
- 未确认接手只算「已交接」，不得表述为完成。任务结束更新本 lane worklog，跨 lane 可见成果登记 `shared.md`。

## 生产后端监控

- 生产巡检、错误归档、代码缺陷修复和受控发版统一使用 `.agents/skills/backend-prod-sentinel/SKILL.md`。
- 只读巡检命令：`bash scripts/prod-monitor.sh collect "30 minutes ago"`。
- 自动修复只允许处理高置信、单根因、可回滚且不涉及 DDL/迁移/密钥/权限放宽/资金金额语义的代码缺陷；其余事故只记录并人工处理。
- 任何生产发版必须使用 `scripts/deploy-prod.sh`，失败自动回滚到 `/opt/decoboon/releases/.previous`。

## 版本管理与发布分支（强制）

- 大功能从 `dev` 创建 `feature/<domain>-<slug>`，小修复或小变更从 `dev` 创建 `fix/<domain>-<slug>`；禁止在 `dev/test/prod/master` 上直接开发或直接推送。
- 正常发布链固定为 `feature|fix -> dev -> test -> prod`；生产事故才允许 `prod -> hotfix/* -> test 环境验证 -> prod -> test -> dev`。
- `test` 只接收发布经理合并的 `dev` 发布批次；`prod` 只接收已验收的 RC 或已验证热修。正常发布禁止逐条 cherry-pick、强推、重置或移动 tag。
- 每次生产发布必须从正式 tag 构建，记录 Release ID、commit、tag、制品 SHA-256、迁移、备份、回滚点和验证证据；发布记录使用 `docs/operations/releases/_发布记录模板.md`。
- 当前尚在迁移期：Web 必须先建立独立 Git 仓库，后端和 App 的混合未提交改动必须按需求/缺陷拆分后才能建立正式环境分支。
- 完整流程、分支保护、热修、回滚和多仓库顺序见 `docs/operations/版本管理与发布分支规范.md`。

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
涉及关联的测试必须显式 `open-in-view=false` 且不加 `@Transactional`。详见 `rules.md` §4.5 与错误集 `ECD-*`。

## 序列化控制

具体 Provider 用 `@Properties` 按 `page/list/info/save/update` 裁剪响应，通用 `{model}-{service}` 路由才用 `@ServiceProperties`（只管返回，不管权限）。
关联对象用 `@AllowProperties` 最小白名单：列表给摘要、详情给页面实际使用字段，避免 `Property -> Store -> Team -> Employee` 循环。
**对外可见投影必须显式组装（强制）**：读取投影不得直接返回实体，`status`、`currentStageName`、计数、金额汇总等派生字段必须按领域事实现场推导；
业主详情 `customer/project/info` 与 `customer/project/page`、`journey/info`、员工侧 `project/project/page|overview` 必须同源同口径，禁止用前端兜底掩盖后端未组装。
详见 `rules.md` §5、§5.6 与 `ECD-011`。

## 租户订阅约定

订阅主体 `Subscription`（主键 `subId`、状态 `SubStatus`）；剩余天数由 `serviceEndTime - now` 动态计算，禁止落库 `balanceDays` 或每日递减；
门店默认不体现（注册只建默认总店，多门店由 `Plan.maxStores` + `Company.setting.enableStore` 控制）；各版本均提供全部业务模块，差异只在门店数与席位数。详见 `rules.md` §11。

## 成本与利润口径（强制）

- 销售价与成本价分离：`CatalogItem.materialPrice/manualPrice` 为销售价、`costPrice` 为标准成本；成本价仅企业负责人、店长、财务可见。
- 项目成本统一由 `ProjectCostContext` 汇总：预计成本优先签约快照，实际成本优先采购到货/入库；任一应计成本缺失时 `costComplete=false`，
  利润与毛利率必须返回 `null`，前端显示「成本未完整」，**禁止以 0 成本计算或展示伪利润**。
- 实际材料成本只在到货/入库累计，领料与损耗只流转库存，退换货按退货类型冲减，禁止重复累计；历史价 `PartnerPrice` 不再作为标准成本必需来源。
- 详见 `rules.md` §10 与错误集 `ECD-002~004`。

## 品牌 Logo 统一（强制）

Logo 只能使用 `logo-export` 同一套 Mark（深色背景白色版、浅色背景彩色版）；App 只用 `JzBrandLogo`，Admin 只用 `BrandLogo`，禁止页面另存图片或复制路径。
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
