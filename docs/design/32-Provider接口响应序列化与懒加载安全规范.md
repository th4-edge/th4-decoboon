# Provider 接口响应序列化与懒加载安全规范

> 状态：生效（2026-10-31）
> 风险级别：L1（后端接口契约与安全基线）
> 关联变更：`CHG-20261031-040`
> 适用范围：th4-decoboon 所有返回前端、App、Admin、站点、第三方回调或内部 HTTP 客户端的 `@Service` Provider
> 上位规范：`AGENTS.md`、`rules.md` §4.5、§5、`docs/design/31-一后端多端同源架构规范.md`

## 1. 规范目标

本规范解决五类问题：

1. Provider 返回 JPA 实体时未声明 `@Properties`，框架默认把所有可达属性序列化；
2. 只给根实体声明白名单，未给关联对象声明白名单，关联对象仍然整棵输出；
3. 为规避懒加载而使用 `EAGER`、`@JsonIgnore`、双向关系注解或前端二次过滤；
4. 接口看似返回成功，实际 JSON 出现递归、超深嵌套、N+1、超时或 `LazyInitializationException`；
5. 实体新增字段后，接口契约在无人知晓的情况下自动扩大，造成敏感字段泄露和响应体膨胀。

一句话规则：

> **Provider 的响应字段必须默认拒绝、按服务显式允许；实体图中的每个运行时模型都要有白名单；注解负责序列化裁剪，查询负责关联加载，二者不能互相替代。**

## 2. 框架事实与问题根因

以下结论以当前 `th4-framework` 的 `Properties`、`AllowProperties`、`IgnoreProperties`、`ServiceProperties` 和 `PropertySerializeFilter` 实现为准。

### 2.1 没有白名单时，默认是全放行

- 具体 Provider 实现类未声明 `@Properties`，且没有其他匹配的属性规则时，框架对普通属性返回“允许序列化”。
- 因此“少写一个注解”不会得到保守响应，而会得到完整实体响应。
- `@JsonIgnore` 只改变某个实体的全局序列化行为，不能替代按 Provider、按场景的响应白名单。

### 2.2 白名单不自动级联到关联对象

- `@AllowProperties(model = Project.class, ...)` 只约束 `Project`。
- `Project.owner` 的运行时类型如果是 `Employee`，`Employee` 必须另有一条 `@AllowProperties(model = Employee.class, ...)`。
- `Project.property`、`Property.store`、`Store.team`、`Team.leader` 等每一层运行时类型都需要自己的规则。
- 某个关联类型没有匹配规则时，该关联仍会按其全部属性序列化。

### 2.3 规则按运行时类型匹配，不按声明父类或接口匹配

- 框架使用实际对象类查找规则；父类、接口或泛型声明不能自动覆盖子类。
- 多态模型和继承模型必须为实际运行时类型补充白名单。
- `@AllowProperties.model` 必须写实际序列化模型类。

### 2.4 `allow` 与 `ignore` 的选择优先级

同一模型同时配置 `allow` 和 `ignore` 时，`allow` 优先，容易造成“以为还排除了某个字段，实际字段已被 allow 放行”的误判。

- 新 Provider、修改中的 Provider 统一使用 `allow` 白名单。
- `ignore` 只作为确有兼容原因的例外，不得作为新接口默认方案。
- 同一模型不得同时依赖 `allow` 和 `ignore` 表达部分字段策略。
- 敏感字段、归属树、审计字段、长文本字段默认不进入 `allow`。

### 2.5 `@Properties` 约束字段输出，不负责预取关联

- 被白名单保留的关联仍需正确查询；`@Properties` 不会自动生成 `JOIN FETCH` 或 `@EntityGraph`。
- 生产 `open-in-view: true` 只是运行时兜底，不是代码可以依赖的许可。
- 白名单排除关联后，序列化器通常不会访问该关联；不得为了“补齐无关字段”主动触发额外查询。
- 白名单保留关联时，必须让该关联在 Provider 事务边界内可读，或在查询中显式预取。

### 2.6 注解必须落在具体 Provider 实现类

- 本项目约定 `@Properties` 标注在 `*.service.*.support.*` 的具体 Provider 实现类上。
- `@Properties` 不是通过 Provider 接口继承的；只标接口不能让具体实现自动获得响应裁剪。
- 同一个 Provider 不得同时声明 `@Properties` 和 `@ServiceProperties`。

## 3. 强制响应分类

每个新增或修改的 Provider 在编码前必须先完成下列分类，分类结果写入 Provider 契约卡或变更记录。

| 类型 | 判定 | 必须动作 |
| --- | --- | --- |
| `ENTITY_GRAPH` | 返回 JPA 实体、实体集合、`Page<Entity>`，或包含实体引用的对象图 | 具体 Provider 类必须声明 `@Properties(allow = {...})`，并为所有实际输出的关联模型补白名单 |
| `READ_MODEL` | 返回由服务端显式组装的 `@Model` 读模型，不持有 JPA 实体引用 | 默认也应声明 `@Properties(allow = {...})`；如明确无需裁剪，必须证明模型中只有标量、枚举、时间和同类读模型，且无实体引用 |
| `SCALAR_BINARY` | 返回 `Boolean`、数字、字符串、枚举、`void`、文件流或明确的纯标量结果 | 不需要 `@Properties`，但不得在结果对象中夹带实体图；文件下载不得返回实体 |
| `FRAMEWORK_MODEL_ROUTE` | 无自定义 Provider，由框架通用 `{model}-{service}` 直接响应 | 在实体或通用服务声明处使用 `@ServiceProperties(uris = ..., properties = @Properties(allow = ...))` |

判定顺序：

1. 先看返回类型：纯标量、文件流或明确命令回执，归入 `SCALAR_BINARY`；否则继续；
2. 看路由是否由自定义 `@Service` Provider 实现：否，归入 `FRAMEWORK_MODEL_ROUTE`；是，继续；
3. 返回对象是否持有 JPA 实体引用、实体集合或可能递归的实体关系图：是，归入 `ENTITY_GRAPH`；否，继续；
4. 返回对象是否由服务端显式组装、只含标量和同类读模型：是，归入 `READ_MODEL`；
5. 无法明确回答以上任一问题时，Provider 契约未完成，不得编码或提交。

无法归入以上四类的 Provider 视为契约未完成，不得提交。

## 4. 按能力的字段策略

同一个业务对象在 `page/list/info/save/update/build/report` 中承担不同任务，不得复制同一套“大而全”字段表。

| 能力 | 必须返回 | 默认不返回 |
| --- | --- | --- |
| `page/list` | 列表展示、筛选、排序、跳转所需字段；主键、名称、状态、摘要金额/日期、最小责任人摘要 | 长文本、完整备注、完整归属树、敏感凭证、详情页专属字段 |
| `info` | 详情页实际展示和下一步动作所需业务字段；必要的摘要关联 | 未展示的关联、密码/令牌、内部审计树、无关成本字段 |
| `save/update` | 前端更新本地状态所需的最小结果：主键、被编辑字段、状态、最小关联摘要 | 完整聚合、全量快照、未参与本次编辑的关联树 |
| `build` | 新建表单默认值、枚举上下文、必要默认关系摘要 | 历史列表、完整实体图、内部规则集合 |
| `report/workbench` | 为页面判断显式组装的读模型字段、派生字段和 `nextAction` | 原始实体大对象、无关明细、可由前端重算的中间量 |
| `count/delete/action` | 计数、布尔结果、命令结果或明确的命令回执 | JPA 实体、完整更新后聚合 |

派生字段必须由后端按领域事实组装；不能因为不在白名单中就让前端猜测，也不能直接信任可能漂移的派生列。

## 5. 白名单写法规范

### 5.1 标准结构

```java
@Properties(allow = {
        @AllowProperties(model = Project.class, properties = {
                "projectId", "name", "status", "currentStageName", "owner"
        }),
        @AllowProperties(model = Employee.class, properties = {
                "empId", "name", "role"
        }),
        @AllowProperties(model = Property.class, properties = {
                "propertyId", "community", "building", "roomNo", "address", "area"
        })
})
@Service("project/project/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.project.service.project.page.Page {
    // ...
}
```

### 5.2 字段选择规则

- 字段来源于页面真实消费、业务判断、下一步动作和回读对账，不是来源于数据库表全部列。
- 关联对象只有页面明确展示或后续动作确实需要时才输出。
- 关联对象默认只给摘要，例如 `empId/name/role`、`storeId/name`、`teamId/name`。
- `company/store/team/owner` 等归属字段只在页面确实展示时输出；数据权限不能依赖响应字段。
- 密码哈希、令牌、盐值、密钥、权限集合、完整手机号等高敏字段，默认不得出现在任何业务响应；确有独立场景时必须单独设计 Provider 和权限。
- 成本、利润、毛利等受限金额字段只对授权角色输出；字段裁剪不能替代 `@DataPermission`。
- `@Lob`、长文本、快照 JSON 和完整附件数组不得默认进入列表；详情需要时必须单独评估体积。
- 不以 `null` 表示“隐藏字段”；隐藏字段应从 JSON 中完全缺失。

### 5.3 白名单完整性规则

- 根模型和每一个实际输出的嵌套模型都必须有白名单。
- 不允许只写根模型白名单后依赖“Jackson 会自己处理嵌套”。
- 不允许用点路径表达嵌套字段，例如 `customer.name`；必须为 `Customer` 单独声明 `@AllowProperties`。
- 不允许复制一个别的 Provider 的字段表来“先跑通”；每个能力的字段集合必须独立审查。
- 白名单字段拼写错误不会自动报错，可能表现为字段静默消失；必须通过真实 JSON 契约测试断言关键字段存在。
- 实体新增字段不会自动进入 allow 白名单，这是预期行为；若前端需要，必须显式修改 Provider 契约并补测试。

### 5.4 `ignore` 例外条件

只有以下情况可以使用 `ignore`，并在代码注释或变更记录中写明原因：

1. 框架通用路由只能匹配黑名单，无法改成 allow；
2. 兼容既有外部契约，需要保留大量字段，逐项 allow 风险更高；
3. 模型没有可递归关联，且排除项覆盖所有敏感和内部字段。

即便如此，也必须补充真实 JSON 测试，不得以“黑名单列全了”作为口头保证。

## 6. 懒加载、事务与查询边界

### 6.1 必须做到的运行时语义

- 业务代码即使关闭 OSIV 也必须正确；`open-in-view: true` 仅是生产兜底。
- `@Properties` 中保留的关联，必须在查询阶段显式预取或在 Provider 事务边界内完成访问。
- `@Properties` 中未保留的关联，不得在组装、日志、前端拼装或测试辅助代码中无意访问。
- 禁止用 `FetchType.EAGER`、`@JsonIgnore`、`@JsonBackReference`、`@JsonManagedReference`、`@JsonIdentityInfo` 或改变实体关系来掩盖 Provider 契约问题。
- 禁止在没有事务边界的 `RequestFilter`、`@Component`、`Context`、工具类中读取实体的 LAZY 关联。

### 6.2 推荐处理顺序

1. 先按页面字段确定响应模型和必须输出的关联；
2. 列表能使用标量读模型时，优先使用显式读模型，不返回重实体图；
3. 详情需要少量关联时，使用 `@EntityGraph` 或 `JOIN FETCH` 预取所需路径；
4. 聚合、统计和跨对象判断放在 `@Transactional(readOnly = true)` 的 Context 或查询中完成；
5. 序列化阶段只输出已经装配好的字段，不在序列化器中补查业务数据；
6. 用关闭 OSIV 的测试验证序列化不会触发 `LazyInitializationException`。

### 6.3 预取边界

- `@EntityGraph` 只预取页面实际输出的关联，不预取“以后可能用得上”的字段。
- 一个接口预取关联过多时，优先收敛响应字段或改为显式读模型，不以更多 JOIN 换字段完整。
- 只读 Provider 不得调用 `ForUpdate` 或 `PESSIMISTIC_*` 锁查询。
- 关联回归测试必须设置 `spring.jpa.open-in-view=false`，且测试方法不得加 `@Transactional`，否则会话不会真正关闭。
- 性能验收必须包含序列化后的响应体和端到端 P95，不能只看 SQL 条数。

## 7. 禁止模式

以下任一出现即阻断提交或发布：

1. `@Service` 具体 Provider 返回实体图但没有 `@Properties`；
2. 只给根模型配置 allow，关联模型没有配置 allow；
3. 用 `@JsonIgnore`、`EAGER`、双向关系注解替代 Provider 白名单；
4. 在实体类上写“全局响应字段策略”，导致不同 Provider 共用一套过大字段表；
5. 自定义 Provider 使用 `@ServiceProperties`；
6. 通用 `{model}-{service}` 路由使用具体 Provider 的 `@Properties`，导致另一条路由没有过滤；
7. `page/list` 直接复用 `info` 的完整字段表；
8. `save/update` 返回完整实体聚合或全部关联树；
9. 通过前端 `pick`、`delete field`、局部对象赋值或展示层判断掩盖后端白名单缺失；
10. 使用空数组、`0`、`null`、默认实体或前端常量伪装接口失败或未加载；
11. 第三方回调、登录退出、支付、文件下载等非页面接口直接返回完整 JPA 实体；
12. 依赖 `open-in-view: true` 作为唯一懒加载方案。

## 8. 开发流程门禁

### 8.1 编码前

每个 Provider 必须填写以下响应契约卡：

```text
Provider 路由：
响应分类：ENTITY_GRAPH / READ_MODEL / SCALAR_BINARY / FRAMEWORK_MODEL_ROUTE
调用端：App / Web / Admin / 内部 / 第三方
页面用途：列表 / 详情 / 表单回显 / 动作回执 / 报表
返回类型：
根模型及白名单：
每个关联模型及白名单：
明确禁止字段：密码、令牌、归属树、长文本、敏感金额等
查询与预取：
异常与恢复路径：
真实 JSON 测试：
```

### 8.2 编码中

- 先写正确的查询和显式组装，再写最小白名单。
- 新增关联时同步检查所有调用方 Provider 的白名单。
- 修改实体字段时同步检查：是否应该进入任何 Provider 白名单、是否需要迁移、是否影响历史快照。
- Provider 返回类型变化时重新分类，不得沿用旧的 `@Properties` 不审查。

### 8.3 提交前

必须完成：

- [ ] 具体 Provider 实现类已声明 `@Properties(allow = ...)`，或属于已证明的 `SCALAR_BINARY`；
- [ ] 根模型及所有嵌套模型均有白名单；
- [ ] `page/list/info/save/update` 不共用大而全字段表；
- [ ] 无 `@JsonIgnore`、`EAGER`、双向关系注解等绕过方案；
- [ ] 关联已按白名单显式预取或在事务内读取；
- [ ] 真实 JSON 序列化测试通过；
- [ ] 敏感字段和归属树字段已做递归缺失断言；
- [ ] 关闭 OSIV 后请求仍成功；
- [ ] 响应字段与前端页面实际消费字段一致；
- [ ] 通用路由使用 `@ServiceProperties`，自定义 Provider 使用 `@Properties`。

### 8.4 验收证据

至少保留以下三类证据：

| 证据 | 要求 |
| --- | --- |
| 结构测试 | 断言 Provider 实现类存在 `@Properties`，并能找到根模型/关联模型的 `@AllowProperties` |
| 真实 JSON 测试 | 对实际 `call(...)` 返回值序列化，断言可解析、无异常、关键字段存在、敏感字段递归缺失 |
| 运行时冒烟 | App/Web 实际调用受影响 Provider，记录 HTTP 状态、响应体大小、耗时和关键字段 |

仅有“注解存在”的反射测试不充分；仅有“页面能打开”也不能证明完整实体图没有被序列化。

## 9. 推荐的正向与负向测试

### 9.1 反射契约测试

用于防止 Provider 漏标：

```java
Properties properties = Page.class.getAnnotation(Properties.class);
assertThat(properties).isNotNull();
assertThat(properties.allow()).anySatisfy(item -> {
    assertThat(item.model()).isEqualTo(Project.class);
    assertThat(Arrays.asList(item.properties()))
            .contains("projectId", "name", "status", "owner")
            .doesNotContain("company", "store", "team", "createTime");
});
```

### 9.2 真实 JSON 测试

测试必须使用实际 Provider 或实际 `ObjectMapper`：

```java
String json = objectMapper.writeValueAsString(response);
JsonNode root = objectMapper.readTree(json);

assertThat(root.has("projectId")).isTrue();
assertThat(root.has("owner")).isTrue();
assertThat(root.get("owner").has("passwordHash")).isFalse();
assertThat(root.has("company")).isFalse();
assertThat(root.has("store")).isFalse();
assertThat(root.has("team")).isFalse();
```

关联测试配置示例：

```java
@SpringBootTest(properties = "spring.jpa.open-in-view=false")
// 不要给测试方法加 @Transactional，否则无法验证真实会话关闭行为
class ProviderJsonContractTest { /* ... */ }
```

### 9.3 负向用例

每个涉及实体图的 Provider 至少覆盖：

1. 未配置根模型白名单 → 契约测试失败；
2. 根模型有白名单但关联模型无白名单 → 测试发现敏感字段或递归风险；
3. 关联被白名单保留但查询未预取 → 关闭 OSIV 后触发 `LazyInitializationException`；
4. 列表错误复用详情白名单 → 体积/字段断言失败；
5. 更新响应返回完整聚合 → 字段白名单断言失败；
6. 新增敏感实体字段 → 若未显式加入 allow，响应中必须继续缺失。

## 10. 存量缺口巡检方法

2026-10-31 对工作区的文件级扫描结果：

| 检查 | 结果 |
| --- | ---: |
| `@Service` Provider 实现文件 | 542 |
| 声明 `@Properties` 的实现文件 | 537 |
| 声明非空 `allow` 的实现文件 | 535 |
| 完全未声明且为标量/文件的实现文件 | 5 |
| 仅声明空 `@Properties` 的标量实现文件 | 2 |
| 递归关联白名单缺口 | 0 |

2026-10-31 首轮扫描发现 21 个 Provider 完全未声明 `@Properties`。经逐项分类，16 个实体/读模型 Provider 已补齐白名单，5 个布尔、文件或纯标量 Provider 按第 3 节豁免。所有已声明 Provider 的递归关联路径已收敛为 0 缺口；后续新增缺口由 `--check` 阻断。

机器门禁：

```bash
python3 scripts/audit-provider-response-contract.py --check
python3 scripts/audit-provider-response-contract.py --self-test
```

门禁同时检查：

- Provider 是否声明具体实现类级 `@Properties`；
- 返回实体、读模型或普通响应对象时是否存在 `allow` 规则；
- `allow` 保留的关联字段是否为其运行时模型声明了后续白名单；
- 标量、文件流等无实体图响应是否被误判。

`--check` 已纳入 `scripts/build-release.sh`。新增 Provider、修改返回类型、修改实体关联或修改 `@Properties` 时，必须在提交前运行；发布构建存在任一 finding 即失败。

巡检不是发布前一次性工作：新增 Provider、修改返回类型、修改实体关联、修改 `@Properties` 白名单时都必须重新执行。

## 11. 阻断与例外

### 11.1 直接阻断

- 新 Provider 返回实体图但没有 `@Properties`；
- 响应可能导致无限递归、超深嵌套、敏感字段泄露或运行时懒加载异常；
- 自定义 Provider 误用 `@ServiceProperties`；
- 用前端过滤、`@JsonIgnore`、`EAGER` 替代后端白名单；
- 测试仅在默认 OSIV 打开时通过。

### 11.2 允许例外

只有同时满足以下条件才允许例外：

1. 返回类型可证明为纯标量、文件流或不含实体引用的显式读模型；
2. 不涉及敏感字段、归属树、金额/成本/利润、权限或支付；
3. 在 Provider 契约卡和变更记录中写明分类、理由和回归测试；
4. 返回类型、字段或关联发生变化时自动重新进入实体图审查。

没有分类证据的“先不写 `@Properties`”不算例外。

## 12. 最终验收口径

一个 Provider 只有同时满足以下条件，才能标记“接口字段契约完成”：

- 有明确响应分类；
- 有按能力设计的最小字段白名单；
- 所有实际输出的关联模型有各自白名单；
- 序列化结果无递归、无敏感字段、无无关归属树、无超预期体积；
- 关闭 OSIV 后仍能完成真实请求；
- App/Web/Admin 消费的字段与 Provider 实际返回一致；
- 变更记录、测试证据和真实接口冒烟可追溯。

“能返回 JSON”“页面没报错”“本地 OSIV 打开能跑”都不构成完成标准。
