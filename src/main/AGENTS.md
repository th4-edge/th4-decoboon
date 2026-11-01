# src/main — th4 后端实现规范

> 根目录 `AGENTS.md` 同时生效。本文件只约束 `src/main/**`；完整示例和边界以 `rules.md` 对应章节为准。

## 1. 架构与 Provider

- 服务 = Provider，不新增 Controller；入参 `Request` 就是领域模型实体，不定义 DTO。
- 前端字段使用 Provider 成员变量 + `@AutoInject` + 校验注解；统计域的自定义 Request 是唯一例外，按 `rules.md` §6.4。
- 对外响应使用具体实体、列表或 `Page<Entity>`；禁止 VO/DTO 包装。
- Provider 包结构固定：`{领域}.service.{对象}.{能力}` 声明接口，`{能力}.support` 放实现；能力名使用 `page/list/info/save/build/update/delete/count/enums/tree/report/unique`。
- 路由使用 `@Service("{域}/{对象}/{能力}")`，不带 `decoboon/`、`clue/` 前缀；`@ServiceName` 仅限通用 ModelService。
- `components` 只放通用技术能力，不放业务 Provider、业务枚举、`@AutoInject` 或 `@ServiceName`。

## 2. 权限先行

- 需要登录的 Provider 继承 `Security`：Request 为 `Employee` 时用 `call(@User Employee employee)`，否则用 `@User private Employee employee`。
- 企业、角色、门店、团队和数据范围只能从登录员工加载，禁止信任前端 `companyId/empId/ownerId`。
- 默认权限矩阵：BOSS=Company，店长=Store，团队负责人=Team，普通员工=Self；角色固定为 Boss、StoreManager、Designer、Marketer、Pm、Finance、TeamLeader。
- `@DataPermission` 控制查得到什么，具体 Provider `@Properties` 控制返回什么，二者不可互替。
- 分页、列表、计数统一由 `DataScopeRequestFilter` 追加范围；自定义详情/更新/删除必须使用 `DataScopeContext + DataScopeSpecification`，禁止 `findById` 后自行判断。
- 保存时企业取登录员工所属企业；非 BOSS 负责人强制为本人。业务表权限索引以 `com_id` 最左。

## 3. 实体与关联

- 实体继承 `AbstractModel`，类上使用 `@Model + @Note + @Entity + @Table`；主键使用 `@Id + @SnowflakeId` 和 `SnowflakeEntityListener`。
- 关联使用对象类型 + `@ManyToOne(fetch = LAZY)` + `@JoinColumn`，禁止裸外键 `Long`；业务实体必须包含 `Company company`。
- 时间字段按用途选择 `DateTime`/`LocalDateTime`；枚举实现 `Enumerable<T>`；可搜索字段加 `@Keyword`。
- 实体不写业务逻辑，禁止 `@JsonIgnore`。长文本字段必须与迁移一致；`ddl-auto: update` 不改既有列类型。

## 4. 事务、懒加载与锁

- 生产保留 `spring.jpa.open-in-view: true` 作为兜底，但业务代码必须关闭 OSIV 仍正确。
- 禁止在无事务边界的 `RequestFilter`、`@Component`、工具类中访问 LAZY 关联；需要时使用 `@EntityGraph`、`JOIN FETCH` 或 `@Transactional(readOnly = true)` 聚合。
- 禁止用 `EAGER`、`@JsonIgnore` 或“dev 通过”绕过懒加载问题。
- 只读事务及其间接调用链禁止悲观锁；仓储方法名含 `ForUpdate` 或以 `findLockedBy` 开头时，调用方必须处于写事务。
- 业务并发统一使用 `RedissonLock`；禁止数据库行锁、`FOR UPDATE`、表锁/间隙锁作为业务并发方案。`rules.md` §0.3 的 SQL 效率例外优先。

## 5. 查询、分页与聚合

- 编码前把接口分类为 `READ_DETAIL / PAGE_LIST / PICKER_PAGE / BOUNDED_LIST / REPORT_SUMMARY / EXPORT_JOB / WRITE_ACTION / BATCH_ACTION`，声明最大行数、查询链和 SQL 是否随数据量增长。
- 一个 Provider 只服务一个真实用户目标；禁止按表或页面字段堆“万能接口”。
- 增长型列表必须数据库分页，默认 `size=20`，普通列表和选择器硬上限 `50`；禁止固定 `size=200/500`、`findAll` 后内存分页或客户端全量过滤。
- 禁止 N+1：循环、`forEach`、`stream.map/flatMap` 内不得访问 Repository 或触发 LAZY 查询；当前页关联用批量 `IN` 或投影装配。
- 金额、计数、比例、排行、趋势、逾期/未收必须用聚合/投影 SQL 下推；禁止全量实体后在 Java 中 `filter/sum/count/groupBy/reduce` 生成正式指标。
- 正式指标必须由独立 `report/summary/overview` Provider 返回；前端不得对业务列表做 `reduce/filter().length/groupBy` 生成金额、计数、比例、排行或趋势。
- 同筛选下校验列表 `totalElements == report.count`；统计接口验收要求端到端 SQL ≤30 条、并发 P95 ≤500ms，10× 数据量下 SQL 条数不增长。
- 查询接口不得有业务副作用；写接口不得顺带重算整页工作台。性能优化必须先做端到端采样，只优化实测占比最大的层；响应体大小是一等公民，SQL 条数只作辅助指标，遵守 `rules.md` §0.5 和 `DEC-037`。

## 6. 序列化与响应契约

- 具体 Provider 使用 `@Properties(allow=...)` 按 `page/list/info/save/update` 裁剪；通用 `{model}-{service}` 路由才使用 `@ServiceProperties`。
- 每个 Provider 先分类为 `ENTITY_GRAPH / READ_MODEL / SCALAR_BINARY / FRAMEWORK_MODEL_ROUTE`；实体图必须在具体 support 类声明白名单，根模型和实际输出的关联模型分别配置 `@AllowProperties`。
- 白名单不会自动级联，`@Properties` 只控制输出，不负责预取。禁止用 `EAGER`、`@JsonIgnore`、双向关系或前端字段过滤绕过。
- 派生字段必须显式组装；业主项目、详情、旅程与员工侧项目读取面必须同源同口径。
- 实体图 Provider 必须有真实 JSON 契约测试，并在 `open-in-view=false`、测试方法不加 `@Transactional` 的条件下通过。
- 修改 Provider 响应后运行 `python3 scripts/audit-provider-response-contract.py --check`；规则自测运行 `--self-test`。

## 7. 金额、成本与订阅

- 销售价与成本价分离；成本价仅 BOSS、店长、财务可见。
- 项目成本统一经 `ProjectCostContext` 汇总：预计成本优先签约快照，实际成本优先采购到货/入库；任一应计成本缺失时 `costComplete=false`，利润和毛利率返回 `null`，前端显示“成本未完整”，禁止按 0 成本计算。
- 实际材料成本只在到货/入库累计；领料与损耗只流转库存，退换货按类型冲减，禁止重复累计；`PartnerPrice` 不再作为标准成本必需来源。
- 订阅剩余天数由 `serviceEndTime - now` 动态计算，禁止落库 `balanceDays` 或每日递减；默认总店不展示，多门店由 `Plan.maxStores` 和 `Company.setting.enableStore` 控制；各版本业务模块相同，差异只在门店数和席位数。

## 8. 编码前与交付检查

涉及业务对象、状态、角色权限、数据范围、金额/成本/利润、客户可见性时，先使用 `zhuangdeqing-business` 并确认规则来源、历史快照、幂等和权限负向场景。

新增、修改、重构代码必须严格遵循本规范；既有旧写法冲突时，以新规范为准并回改存量代码。

交付前至少检查：

- 场景卡、Provider 分类、权限正向/负向、失败恢复和历史快照边界；
- 实体/关联、白名单、懒加载、事务和锁边界；
- 定向测试与编译；相关接口补真实 JSON、分页/聚合、10× 和 P95 证据；
- 变更记录、受影响端和未执行项。开发自测不得表述为独立验收。

## 9. 详细规则索引

- 五条铁律：`rules.md` §0
- 权限与数据范围：§0.2、§3.4.1、§4.2.1
- 查询、分页、聚合、性能：§0.4、§0.5、§0.7、`docs/design/33`、`34`
- Provider：§3
- 实体与懒加载：§4、§4.5
- 序列化：§5、`docs/design/32`
- 成本与订阅：§10、§11
- 状态投影：§13
- 发布：§14
