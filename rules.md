# th4-decoboon 后端代码生成规范（rules.md）

> **权威性与适用范围**：本文件是 th4-decoboon 项目后端代码生成的**唯一权威规范**。所有新增、修改、重构的代码都必须严格遵循；与既有写法冲突时，以本规范为准，并逐步回改存量代码。
> 规范中的框架 API 均与 th4-framework 源码逐一核对（`com.th4.edge.framework.*`），不含臆测接口。
> 生成代码时若发现本规范未覆盖的场景，先遵循框架既有设计惯例，再补充回本规范。

---

## 0. 总纲：th4 领域模型驱动设计的五条铁律

th4 是一套区别于传统 MVC 的**领域模型驱动设计**架构，任何代码生成都必须先立住以下五条：

1. **服务（Provider）是 Controller 的唯一替代者**，其本质是"**为谁服务**"——入参 `request` 的类型就是**领域模型实体类型**（如 `Asset`、`Lead`），而不是 DTO 概念。**工程中不存在 DTO**。
2. **前端传入的字段**，除统计类服务外，一律通过 **Provider 类内声明成员变量 + `@AutoInject` 注入 + `@NotNull` 等校验注解**获取，而不是定义专门的请求对象。
3. **对外响应结构就是具体的实体对象类型**（`List<Asset>`、`Page<Asset>`、`Asset`），不做 VO/DTO 包装。
4. **实体采用对象关联建模**：如 `Asset` 里的关联业务员不是 `Long empId`，而是 `Employee emp` 对象字段，JPA 用 `@ManyToOne` 映射。
5. **实体序列化到前端要过滤属性**时，具体 Provider 用 **`@Properties`** 声明式控制；只有框架通用 `{model}-{service}` 路由没有具体 Provider 时，才用 **`@ServiceProperties`**。**禁止用 `@JsonIgnore` 硬编码**。

> 一句话记忆：**服务 = 为实体服务；入参 = 实体；字段 = AutoInject；响应 = 实体；关联 = ManyToOne；具体 Provider 序列化 = @Properties。**

### 0.1 实际业务场景先行（强制，2026-09-18 新增）

任何功能设计不得直接从数据表、字段清单或技术模型开始。必须先完成真实业务场景推演，再决定实体、字段和页面入口。

**每个功能至少写出 2 个场景，每个场景必须包含：**

1. 使用人和岗位。
2. 触发事件和时间点。
3. 当前线下做法。
4. 系统要完成的最短动作。
5. 下一步把任务交给谁。
6. 异常或例外情况如何处理。

**硬性要求：**

- 默认路径必须覆盖最高频场景，低频字段和高级能力按需展开。
- 禁止把大型企业 ERP 的完整模型强加给中小装企；仓库、批次、税务、供应商门户和复杂审批只能在场景明确命中时出现。
- 不能只按数据表结构生成页面，必须按“谁操作、下一步给谁”组织入口和状态。
- App 优先现场快捷操作，Web 优先批量维护、对账和经营分析，但同一业务事实只能有一个服务端口径。
- 场景未闭环时，不得直接生成生产代码；应先补 PRD/操作说明和验收场景。

### 0.2 权限先行：领域设计的第一约束（最高优先级）

任何业务实体、Provider、Repository、Criteria 查询或 Context 在生成前，必须先确定**企业归属、角色能力、数据范围、操作动作**四件事；权限校验属于服务端强逻辑，前端菜单或按钮隐藏不构成权限控制。

**硬性规则：**

1. 所有业务实体必须直接归属 `Company company`；禁止通过间接关联绕过企业归属，禁止信任前端传入的 `companyId`。
2. 所有需要登录的业务 Provider 必须继承 `Security`，当前员工必须通过 `@User` 注入。当前企业、角色、门店等上下文只能从登录员工加载，不能从前端参数获取。
3. 查询权限矩阵统一使用四级数据范围，并由 `EmployeeRole` 集中绑定，禁止在 Provider 中散落角色判断：
   - `BOSS`：`Company`，查询当前企业全量数据。
   - `门店店长`：`Store`，查询当前企业、当前门店数据。
   - `团队负责人`：`Team`，查询当前企业、当前团队数据。
   - 普通员工：`Self`，查询当前企业、本人负责数据。
4. `Company/Store/Team/Self` 只适用于有个人、团队或门店归属的业务实体，且只表示数据范围，不表示功能权限；BIM 工艺、材料、套餐模板等纯企业级主数据仍强制企业条件，但不得虚构负责人后错误套用 `Self`。
5. 分页、列表、计数等 Criteria 查询必须在服务端向 `criteriaQuery` 追加企业和数据范围条件；请求实体标注 `@DataPermission` 且 Provider 实现 `CriteriaQueryAware` 时，由 `DataScopeRequestFilter` 在框架构建 CriteriaQuery 后统一追加条件并认证当前员工。具体业务 Provider 仍必须继承 `Security`；不能只给请求对象 `setCompany(...)`，也不能依赖 th4 对象领域查询自动完成权限控制。
6. 框架通用 `service/info|update|delete` 由 `DataPermissionCrudAspect` 自动按当前数据范围加载实体；自定义详情、更新、删除 Provider 必须先通过 `DataScopeContext` 获取当前范围，再使用 `DataScopeSpecification` 与业务主键组合查询。两类实现都禁止先 `findById(id)` 再判断企业或负责人。
7. 保存时必须由服务端固定 `Company`：`BOSS` 可选择本企业负责人，非 `BOSS` 的负责人强制为当前员工；`store/team` 从服务端确认后的负责人归属复制；前端提交的 `companyId/empId/ownerId/storeId/teamId` 只能作为候选值。
8. 跨域读取其他聚合中的实体时也必须同时携带企业和数据范围条件，例如报价读取客户时使用 `DataScopeSpecification`，禁止直接 `findById(...)`。
9. 业务表必须建立与权限查询匹配的联合索引：企业范围 `(com_id, 业务主键)`、门店范围 `(com_id, store_id, 业务主键)`、团队范围 `(com_id, team_id, 业务主键)`、负责人范围 `(com_id, emp_id, 业务主键)`。`com_id` 必须始终是最左字段。
10. 数据归属是业务快照：客户转交、员工调店、员工调团队时必须明确并测试 `company/store/team/owner` 的更新规则，不能在查询期临时推导。
11. 每个有数据权限的领域必须补齐四角色、伪造参数、跨企业、详情越权、更新/删除越权测试；仅验证“接口可调用”不算完成。

**统一组件：**

- `com.th4.edge.tenant.user.access.context.DataScopeContext`：解析当前员工范围。
- `com.th4.edge.tenant.user.access.DataScopeCriteria`：生成 Criteria Predicate。
- `com.th4.edge.tenant.user.access.DataScopeSpecification`：生成详情、更新、删除和跨域查询条件。
- `com.th4.edge.tenant.user.access.filter.DataScopeRequestFilter`：自动处理符合条件的 page/list/count。
- `com.th4.edge.tenant.user.access.filter.AccessDataPermissionSaveRequestFilter`：在 `@ModelSaveInject` 之后覆盖框架 ModelSave 的企业、门店、团队和负责人归属。
- `com.th4.edge.tenant.user.access.filter.AccessDataPermissionUpdateRequestFilter`：在 `@ModelUpdateInject` 之后保护框架 ModelUpdate 的企业、门店、团队和负责人归属。
- `com.th4.edge.tenant.user.access.filter.DataPermissionCrudAspect`：自动处理框架通用 `service/save|update|delete|info`。
- `com.th4.edge.tenant.user.access.context.DataPermissionCrudContext`：服务端组装创建归属、合并安全更新字段、按范围加载详情与删除目标。
- `com.th4.edge.tenant.user.access.annotation.DataPermission`：声明实体权限字段。

**代码生成决策树（强制）：**

```text
业务实体是否存在个人/门店/团队归属？
├─ 否：纯企业级主数据
│   └─ 只强制 com_id；不要虚构 owner，也不要套用 Self
└─ 是：@DataPermission 业务实体
    ├─ 实体：company/store/team/owner + 四级联合索引
    ├─ 通用 {model}-{service}
    │   ├─ page/list/count -> DataScopeRequestFilter
    │   ├─ save -> AccessDataPermissionSaveRequestFilter + DataPermissionCrudAspect
    │   ├─ update -> AccessDataPermissionUpdateRequestFilter + DataPermissionCrudAspect
    │   └─ delete/info -> DataPermissionCrudAspect
    └─ 自定义 Provider
        ├─ page/list/count -> Security + CriteriaQueryAware
        ├─ info/update/delete -> DataScopeSpecification
        ├─ save -> 服务端组装 company/store/team/owner
        └─ 不允许 findById 后补权限判断
```

**客户域基准实现：**

- `Customer` 使用 `@DataPermission` 声明 `company/store/team/owner` 权限字段。
- `Customer` 表建立 `(com_id, cust_id)`、`(com_id, store_id, cust_id)`、`(com_id, team_id, cust_id)` 和 `(com_id, emp_id, cust_id)` 联合索引。
- `customer/customer/page`：由统一拦截器按 `Company/Store/Team/Self` 四档追加条件。
- `customer/customer/info`：使用同一 `DataScopeContext + DataScopeSpecification` 生成详情条件；无命中统一按“不存在或无权访问”处理。
- `customer/customer/save`：企业固定为当前员工所属企业；门店、团队、负责人必须从服务端校验后的负责人归属复制；非 `BOSS` 的负责人强制为当前员工。
- `customer/customer/update`：先用 `DataScopeSpecification` 加载可见客户；非 `BOSS` 不能转交，`BOSS` 转交时校验负责人属于当前企业并同步 `store/team`；`status` 不走通用编辑。

**平台运营管理端例外（强制）：**

- 仅 `com.th4.edge.platform` 平台域允许跨企业读取和人工订阅管控；普通业务域禁止复制该模式。
- 平台账号必须来自独立 `PlatformUser`，所属 `PlatformOrg` 和岗位状态有效，Provider 必须继承 `Security`。
- 跨企业范围校验必须集中在 `PlatformAccessContext`，禁止散落推断。
- 平台组织、部门、用户、审计表不得引用租户 `Company/Employee/Credentials`。
- 租户数据必须通过 `TenantDataAdapter` 转为平台标量快照，平台实体不得持有租户实体。
- 人工订阅调整必须使用悲观锁、服务端校验套餐/状态/有效期，并写入 `PlatformOperationLog`。
- 平台查询不得返回员工凭证、密码、密钥、连接串和客户隐私正文。
- 平台域只读接口不得替租户业务绕过数据权限；租户侧 `Company/Store/Team/Self` 规则保持不变。

---

## 1. 架构定位：与传统 MVC 的本质差异

| 维度 | 传统 MVC | th4 领域模型驱动 |
| --- | --- | --- |
| 入口 | Controller + @RequestMapping | **Provider**（框架自动注册路由，无 Controller） |
| 入参 | DTO / VO / Query 对象 | **领域模型实体**（`Asset`、`Lead`…） |
| 获取前端字段 | `@RequestBody DTO` + getter | **成员变量 + `@AutoInject` 注入 + 校验注解** |
| 响应 | VO / Result 包装 | **实体对象**（`Asset`、`Page<Asset>`…） |
| 数据访问 | Service → DAO | Repository（JPA）+ 框架 Criteria 过滤 |
| 实体关联 | 只存外键 ID | **对象关联 + `@ManyToOne`** |
| 序列化控制 | `@JsonIgnore` / 手写 VO | **具体 Provider 用 `@Properties`；通用 ModelService 用 `@ServiceProperties`** |
| 命名 | XxxController / XxxService / XxxDTO | **`service.{业务对象}.{能力}` + `support` 实现类** |

核心心智：**不再"为接口翻译数据"，而是"为领域实体提供服务"。**

---

## 2. 分层与包结构

### 2.1 顶层包结构

```text
com.th4.edge.decoboon
├── bim                                  # 工程工艺、材料主数据、套餐模板
├── customer                             # 客户
├── trade                                # 报价、合同等交易
├── project                              # 项目、工地、排期、施工、验收
├── material                             # 材料计划与供应链
├── finance                              # 应收、收款、支出、应付、结算
├── collab                               # 协同、消息、附件
├── statistic                            # 统计域（唯一允许自定义 Request 的域）
```

### 2.2 单个业务域的标准结构（以 asset 为例）

```text
asset
├── model/                 # 领域模型（实体），对象关联
│   ├── Asset.java
│   └── ...
├── configure/             # 枚举（实现 Enumerable<T>）
│   └── AssetType.java
├── repository/            # JPA 仓储（Spring Data 接口）
│   └── AssetRepository.java
├── service/               # ★ 领域服务层（Provider 的归属地，替代 controller）
│   └── assets/            # 业务对象包（一个业务对象一个包）
│       ├── page/          # 能力包：分页
│       │   ├── Page.java            # interface（声明 request/response 类型）
│       │   └── support/             # ★ 实现包
│       │       └── Page.java    # 实现类（命名一个单词，简单易懂）
│       ├── list/          # 能力包：列表
│       ├── info/          # 能力包：详情
│       ├── save/          # 能力包：保存/新增
│       ├── build/         # 能力包：构建（前端需要的初始对象）
│       ├── update/        # 能力包：更新
│       └── delete/        # 能力包：删除
├── context/               # 领域内横切/聚合服务（看板查询等）
└── listener/              # 实体生命周期监听（事件驱动）
```

> 能力包沿用框架 `service` 体系的固定能力名：`page / list / info / save / build / update / delete / count / enums / tree / report / unique`。

### 2.3 通用组件与业务 Provider 边界（components）

`com.th4.edge.components.{组件}` 是**通用技术框架分包**，用于封装与具体业务无关的能力，例如短信、对象存储、支付网关协议等。组件层和业务领域的边界必须固定：

| 层次 | 位置 | 允许内容 | 禁止内容 |
| --- | --- | --- | --- |
| 组件契约层 | `components.{组件}.service` | 能力接口、场景抽象、异常、值对象 | Provider、`@AutoInject`、前端路由 |
| 组件实现层 | `components.{组件}.sdk.{厂商}` | 厂商配置、签名、协议、实现类 | 业务落库、业务路由 |
| 业务编排层 | `{业务域}.service.{对象}.{能力}` | Provider、`@AutoInject`、业务事务、响应实体 | 厂商签名、AccessKey、底层协议 |
| 业务场景配置 | `{业务域}.configure` | 注册/登录等业务枚举 | 短信协议实现 |

规范细则：

1. `components` 中**禁止出现 `Provider`、`@AutoInject`、`@ServiceName` 和前端路由**。
2. `components` 只定义通用能力接口，具体能力实现放入 `components.{组件}.sdk.{厂商}.{能力}`。
3. 厂商无关的业务场景枚举放在业务域，例如 `UserVCodeScene`；组件只定义 `VCodeScene` 抽象接口。
4. 前端可调用的 Provider 必须位于业务领域，例如：
   - `tenant/user/service/user/sms/send`：用户域验证码发送入口；
   - `tenant/user/service/user/register`：注册入口，注入 `Validator` 校验验证码。
5. 业务 Provider 只依赖组件能力接口，不直接读取 AccessKey、不拼接厂商请求、不操作 Redis Key。
6. 组件不得反向依赖任何业务域。
7. Spring Data JPA 只管理关系型数据库实体；Redis 对象管理使用 Spring Data Redis Repository（`@RedisHash` + `CrudRepository` / `ReactiveCrudRepository`），不得混称。
8. Redis repository 无法表达的 CAS、原子校验、原子占位等操作，放入 Repository Custom Fragment；业务 Provider 仍只依赖组件接口。
9. 跨实例并发控制使用框架/组件提供的 Redisson 分布式锁；JVM 本地锁只能保护单进程内状态，不能替代分布式锁。
10. 短信认证产品为阿里云 DYPNS 时，验证码由阿里云生成和校验；业务 Provider 不生成验证码，也不直接调用 `SendSmsVerifyCode` / `CheckSmsVerifyCode`。

短信模块的目标分层示例：

```text
components/sms
├── service/vcode/sender/Sender.java                 # 通用发送契约
├── service/vcode/validator/Validator.java           # 通用校验契约
└── sdk/aliyun
    ├── sender/AliyunSmsSender.java                  # 阿里云发送实现
    └── validator/AliyunSmsValidator.java            # 阿里云通道校验实现

tenant/user
├── configure/UserVCodeScene.java                    # 注册/登录/找回密码场景
├── access                                           # 数据权限公共能力（注解/上下文/过滤器/CRUD 切面）
└── service/user
    ├── sms/send/support/Send.java                   # 前端发送 Provider
    └── register/support/Register.java               # 前端注册 Provider
```

> 一句话记忆：**components 提供"怎么发"，业务领域决定"什么时候发、发给谁、发完后做什么"。**

---

## 3. Provider 规范（核心）

### 3.1 服务 = "为谁服务"

`Provider` 是框架定义的服务契约，一切业务能力都收敛为它：

```java
public interface Provider<Request, Response> {
    Response call(Request request);
}
```

**第一个概念：为谁服务。** 声明 `Provider<Request, Response>` 时，`Request` 指向你服务的那个领域实体（如 `Asset`），`Response` 是返回给前端的实体形态。这一设计让"这个服务在服务谁"一眼可见，而不是看一个无意义的 DTO 名。

### 3.2 入参 request = 模型实体类型（禁止 DTO）

- **查询 / 列表 / 分页 / 详情 / 保存类服务**：`Request` 直接用实体类型，例如 `Provider<Asset, Asset>`、`Provider<Asset, Page<Asset>>`。
- **不要**再定义 `XxxRequest` / `XxxDTO` / `XxxVO` 内部类去搬运字段——那是 MVC 的旧习惯，th4 不存在。
- **唯一例外：统计域（statistic）**。统计需要聚合、多表、时间窗口等非实体语义的入参，允许定义专门的请求类型（如 `DashboardQuery`），但也**不允许命名为 DTO**，用语义化名称。

### 3.3 获取前端字段：成员变量 + @AutoInject（重点）

除统计服务外，**前端传入的字段不放进 request 对象，而是在 Provider 类里声明成员变量，用 `@AutoInject` 注入，并用校验注解约束**。

```java
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.Length;

@Service("asset/save")
public class Save implements Provider<Asset, Asset> {

    /** 前端传入字段：登记业务员 ID（由 @AutoInject 注入，默认取同名变量 empId） */
    @AutoInject
    @NotNull
    private Long empId;

    /** 前端传入字段：备注 */
    @AutoInject
    @Length(max = 256)
    private String remark;
    // ...
}
```

**`@AutoInject` 注入语义（与框架 `AutoInjectFieldInject` 源码一致）：**

| 规则 | 说明 |
| --- | --- |
| 变量来源 | 从 `RequestContext` 变量域取值（前端 JSON 请求体展开后的同名变量） |
| 默认变量名 | 与成员变量**同名**；`@AutoInject("alias")` 可指定别的变量名 |
| 简单类型 | `String / Number / Boolean / 基本类型`：变量存在时才注入 |
| 对象类型 | 变量存在时按类型转换；不存在时会尝试按字段名从变量域**自动封装对象**（支持嵌套 `@AutoInject`） |
| 集合 / Map | 自动装配 `List/Set/Map`，元素按泛型类型转换 |
| 枚举 / 日期 | 自动识别 `Enumerable` 枚举名、`Date/DateTime` 字符串 |

**使用约束：**
- 注入的成员变量**不要再用 getter/setter**，只在 `call()` 内直接读取。
- 必须给关键字段配校验注解（见 3.4），不要在 `call()` 里手写 `if (xxx == null) throw`。
- 与实体字段重名的上下文参数（如当前登录 `empId`、租户 `companyId`）优先用 `@AutoInject` 注入，避免与入参实体字段打架时，以"成员变量注入 + 实体入参"分工：**实体承载业务数据，成员变量承载上下文/额外条件**。

### 3.3.1 模型对象保存/修改注值：@ModelSaveInject / @ModelUpdateInject（重点）

两个注解来自 `com.th4.edge.framework.data.jpa.inject.annotation`，都可以标注 Provider 的 `call` 参数。它们负责把 `RequestContext` 中的变量装配到模型对象，**不负责持久化，也不负责数据权限**；Provider 仍必须调用 `repository.save(...)`/`entityManager.persist(...)`，自定义 Provider 仍必须自行完成 `@DataPermission` 范围控制。

| 注解 | 框架行为 | 典型用途 |
| --- | --- | --- |
| `@ModelSaveInject` | 按请求变量组装新模型；递归处理 `@ManyToOne`、集合、`@Embedded`、日期等；若模型实现 `Record`，同时注入审计主体 | 自定义 `save/build` Provider |
| `@ModelUpdateInject` | 要求入参非空；按模型 `@Id` 找到旧实体，再把请求变量中的非主键字段合并到旧实体 | 框架通用 `service/update`，或无额外字段保护的普通更新 |

**@ModelSaveInject 写法：**

```java
@Override
public Customer call(@ModelSaveInject Customer request) {
    Employee current = tenantContext.requireEmployee(employee);
    Employee owner = customerContext.resolveOwner(current, ownerId, current);
    customerContext.applyOwnership(request, owner);
    request.setStatus(CustStatus.New); // 服务端不可信字段强制覆盖
    return customerRepository.save(request);
}
```

- `@ModelSaveInject` 会自动把 `name/telNo/area/owner` 等请求变量组装到 `request`。
- 仍需保留关键字段的 `@AutoInject + @NotNull/@NotEmpty/@Length` 成员变量，框架校验过滤器只校验 Provider 对象；模型注值负责装配，校验字段负责准入。
- 企业、门店、团队、负责人、状态等安全字段必须在 `call()` 内由服务端重新覆盖，禁止直接使用前端模型值。

**@ModelUpdateInject 写法与边界：**

```java
// 框架通用 service/update 已使用 @ModelUpdateInject；只要实体标注 @DataPermission，
// 就同时受 DataPermissionCrudAspect 的范围加载和归属字段保护。
```

- 入参必须非空，并且请求中的主键名必须与实体 `@Id` 属性名一致；例如实体字段是 `custId`，请求不能只传 `customerId`。
- 它会加载旧实体并合并非主键字段，因此**会覆盖同名业务字段**；状态、余额、审批结果、冻结标志等不可由普通编辑修改的字段，不能直接暴露给该通用更新。
- `@ModelUpdateInject` 内部使用 `EntityManager.find` 加载模型，**不会自动应用 `@DataPermission`**。框架通用 `service/update` 由 `DataPermissionCrudAspect` 保护；自定义 `customer/customer/update`、`info/delete` 等带 DataScope 的 Provider 禁止先靠该注解加载，再补权限判断。
- 自定义更新需要严格字段白名单、状态保护或负责人转交规则时，仍使用 `DataScopeContext + DataScopeSpecification` 按主键与数据范围一次加载，再显式合并允许修改的字段。

### 3.4.1 登录保护：Security + @User（强制）

需要登录、必须携带 token 才能调用的 Provider，接口必须额外继承框架 `Security` 标记接口；当前登录员工必须通过 `@User` 注入。框架通过 `Security` 校验 token，通过 `@User` 解析并校验当前 `Employee` 的合法性，业务代码不得重复实现认证。

**写法一：Request 本身就是 Employee（参考 `user/info`）**

```java
// 接口
public interface Info extends Provider<Employee, Employee>, Security {
}

// 实现
import com.th4.edge.framework.provider.security.annotation.User;

@Service("user/info")
public class Info implements com.th4.edge.tenant.user.service.user.info.Info {

    @Override
    public Employee call(@User Employee employee) {
        return tenantContext.requireEmployee(employee);
    }
}
```

**写法二：Request 是其他业务实体**

此时 `@User` 必须声明为 Provider 字段，不能替换业务 Request：

```java
public interface Invite extends Provider<Invite, Invite>, Security {
}

@Service("user/invite")
public class Invite implements com.th4.edge.tenant.user.service.user.invite.Invite {

    @User
    private Employee employee;

    @Override
    public Invite call(Invite request) {
        Employee current = tenantContext.requireBoss(employee);
        // ...
    }
}
```

**硬性规则：**

- 需要登录的 Provider 接口必须继承 `Security`；登录、注册、短信发送、接受邀请等公开接口不得继承。
- Request 类型为 `Employee` 时，使用 `call(@User Employee employee)`。
- Request 类型为其他实体时，使用 `@User private Employee employee` 字段注入。
- `@User` 注解本身负责校验 `Employee` 是否为有效登录用户，业务 Provider 禁止手工调用 `SecurityManager.authenticate(...)`、`RequestContextHolder` 或自行解析 token。
- 只使用员工标识时读取 `employee.getEmpId()`；需要企业、门店、角色等完整实体时，以该 `empId` 查询，禁止信任前端传入的 `companyId/empId`。
- `TenantContext` 只承载租户、角色等业务校验，接收 `@User` 注入的 `Employee`，不得在 Context 内再次认证。
- `@User` 不是前端入参注入，不得与 `@AutoInject` 混用，也不得重复添加必填校验注解。
- `Security` 只解决“是谁”，数据权限必须继续校验“能访问哪个企业、哪些数据和执行什么动作”，禁止把登录成功等同于业务权限通过。
- 所有查询、保存、更新、删除都必须从登录员工重新加载当前租户和角色；前端提交的 `companyId`、`empId`、`ownerId` 默认视为不可信候选值。
- 数据范围必须通过 `DataScopeContext` 解析，通过 `DataScopeRequestFilter`、`DataPermissionCrudAspect` 或 `DataScopeSpecification` 应用；禁止在每个 Provider 内复制一套角色到数据范围的 `if/else` 逻辑。

### 3.4 校验注解（provider.validation 包）

校验注解直接标注在 `@AutoInject` 成员变量（或统计 Request 的字段）上，框架在调用前统一校验，**禁止在 `call()` 内手写空值判断**。

| 注解 | 用途 | 常用位置 |
| --- | --- | --- |
| `@NotNull` | 非空 | 必填字段 |
| `@NotEmpty` | 非空且非空字符串/集合 | 文案、ID 列表 |
| `@Length(max = N)` | 长度上限 | 字符串 |
| `@LengthEq(N)` | 定长 | 手机号等 |
| `@Gt(value)` | 数值大于 | 数量、金额 |
| `@Code` | 代码/枚举合法性 | 枚举类字段 |

### 3.5 响应 = 实体对象类型

`Response` 泛型直接写实体或实体的集合/分页：

| 场景 | Response 类型 |
| --- | --- |
| 列表 | `List<Asset>` |
| 分页 | `org.springframework.data.domain.Page<Asset>` |
| 详情 | `Asset` |
| 保存/更新/删除 | `Asset` |
| 统计 | 统计域内的语义化结果对象 |

> 实体对象会携带 `@ManyToOne` 关联对象一起序列化到前端。具体 Provider 需要裁剪哪些属性，直接在 Provider 上使用 `@Properties`（见第 5 章），不另做 VO。

### 3.6 命名与包结构：service → 业务对象 → 能力 → support

**这是 provider 命名的硬性规范**，所有 Provider 必须按此结构落位，禁止散落在旧的 `provider/` 包：

```text
{领域概念}.service.{业务对象}.{能力}           # 接口（interface），定义 request / response 类型
{领域概念}.service.{业务对象}.{能力}.support   # 实现类（命名尽可能一个单词，简单易懂）
```

分页示例（asset 素材域）：

```java
// ============ 接口：asset/service/assets/page/Page.java ============
package com.th4.edge.decoboon.asset.service.assets.page;

import com.th4.edge.decoboon.asset.model.Asset;

/**
 * 素材分页服务：入参 = 素材实体，响应 = 素材分页。
 * 继承框架 ModelService（其本身是 Provider<Model, Response>），即"为 Asset 服务"。
 */
public interface Page extends
        com.th4.edge.framework.service.ModelService<Asset, org.springframework.data.domain.Page<Asset>> {
}
```

```java
// ============ 实现类：asset/service/assets/page/support/Page.java ============
package com.th4.edge.decoboon.asset.service.assets.page.support;

import com.th4.edge.decoboon.asset.model.Asset;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.service.annotation.ServiceName;
import com.th4.edge.framework.service.page.context.AbstractPage;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * 素材分页实现：继承框架分页骨架（Criteria 通用查询），实现业务分页契约。
 */
@Note("素材分页")
@Description("按通用查询语法分页查询素材")
@Service("asset/page")          // 路由：前端 POST /asset/page
@ServiceName("page")
@Scope("prototype")
public class Page extends AbstractPage<Asset> implements Page {

    public Page(EntityManager entityManager) {
        super(entityManager);
    }
}
```

**命名规则：**
- 接口名 = **能力名**（一个单词）：`Page`、`List`、`Save`、`Info`、`Build`、`Update`、`Delete`、`Count`、`Enums`。
- `support` 实现类名 = **能力名**，简单易懂（如 `Page`、`Save`）。
- 路由 `@Component("{业务域}/{业务对象}/{能力}")`，与能力一一对应。

### 3.7 通用 CRUD：走 ModelService 体系

只要业务是标准 CRUD，**不要**手写 Provider，直接用框架通用服务（URL 约定 `POST /{modelName}-{service}`）：

| 能力 | 框架服务 | 说明 |
| --- | --- | --- |
| 构建 | `service/build` | 返回前端需要的新建初始对象 |
| 保存 | `service/save` | 新增 |
| 更新 | `service/update` | 按主键更新 |
| 删除 | `service/delete` | 删除 |
| 详情 | `service/info` | 单条 |
| 分页 | `service/page` | Criteria 分页 |
| 列表 | `service/list` | 列表 |
| 树 | `service/tree` | 树形 |
| 枚举 | `service/enums` | 枚举字典 |
| 计数 | `service/count` | count |
| 唯一校验 | `service/unique` | 唯一性 |
| 报表 | `service/report` | 报表聚合 |

只有通用 CRUD 覆盖不了的自定义能力，才在 `service.{业务对象}.{能力}` 下写自定义 Provider（如"线索登记并归因"、"素材上传并识别"）。

### 3.8 @ServiceName 使用边界

- `@ServiceName` **仅用于框架通用模型服务**（`ModelService` 体系下的 `page/list/info/save/update/delete/build/count` 等），用于声明当前 Provider 对应的通用模型能力名。
- 自定义业务 Provider（注册、登录、短信发送、邀请、接受邀请、支付回调等固定业务场景）**不得使用** `@ServiceName`。
- 判断标准：如果是为某个实体做通用 CRUD/分页/列表，可加；如果是具体业务动作，不加。

---

## 4. 实体规范（对象关联建模）

### 4.1 实体声明

```java
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.model.AbstractModel;

@Note("素材")                          // 类上必写 @Note
@Description("业务员上传的工地照片/视频素材")
@Model                                // 实体即 Spring 组件（prototype）
@Entity
@Table(name = "db_clue_asset")
public class Asset extends AbstractModel {
    // ...
}
```

### 4.2 对象关联：@ManyToOne（核心差异）

**关联字段必须用对象类型，不用裸外键 ID。** 例如"素材归属业务员"：

```java
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import com.th4.edge.decoboon.company.model.Employee;

@Note("归属业务员")
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "emp_id", nullable = false)
private Employee emp;
```

规则：
- 字段名用**对象语义名**（`emp`、`company`、`video`），不是 `empId`。
- 数据库列名由 `@JoinColumn(name = "...")` 显式指定（默认就是 `emp_id`），物理外键照常落库。
- 关联对象默认 `fetch = LAZY`，避免一次查询拖出整张关联表。
- 保存时从前端拿到 `empId`，在 Provider 里解析为 `Employee` 再 `setEmp(...)`（见 6.3 示例）。
- 需要"对象里冗余关联对象的属性以少查一次"时，用框架 `@Reference("emp.name")` 自动维护冗余字段，不要在实体里手写同步逻辑。

### 4.2.1 数据权限字段与索引（强制）

有个人、团队、门店归属的业务实体必须使用对象关联，并标注 `@DataPermission`。所有权限联合索引必须以企业字段 `com_id` 为最左前缀：

```java
@DataPermission
@Table(name = "cust_customer", indexes = {
        @Index(name = "idx_cust_customer_company", columnList = "com_id, cust_id"),
        @Index(name = "idx_cust_customer_company_store", columnList = "com_id, store_id, cust_id"),
        @Index(name = "idx_cust_customer_company_team", columnList = "com_id, team_id, cust_id"),
        @Index(name = "idx_cust_customer_company_owner", columnList = "com_id, emp_id, cust_id")
})
public class Customer extends AuditModel {
    private Company company;
    private Store store;
    private Team team;
    private Employee owner;
}
```

- 企业范围查询：`(com_id, 业务主键)`。
- 门店范围查询：`(com_id, store_id, 业务主键)`。
- 团队范围查询：`(com_id, team_id, 业务主键)`。
- 企业内再按负责人过滤：`(com_id, emp_id, 业务主键)`。
- 其他数据范围维度按 `(com_id, 范围字段, 业务主键)` 扩展，禁止只建裸主键或无关单列索引。
- 索引字段顺序服务实际 `WHERE` 条件；联合索引已经覆盖的左前缀不重复建单列索引。

### 4.3 主键 / 时间 / 枚举

```java
import com.th4.edge.framework.data.date.model.DateTime;
import com.th4.edge.framework.data.model.prepare.annotation.CreateTime;
import com.th4.edge.framework.data.model.prepare.annotation.UpdateTime;
import com.th4.edge.framework.core.annotation.Unit;

@Note("素材索引")
@Id
@SnowflakeId
private Long assetId;

@Note("创建时间")
@CreateTime
@Embedded
@AttributeOverrides({ /* 时间各字段与列名映射，沿用现有 DateTime 惯例 */ })
private DateTime createTime;

@Note("更新时间")
@UpdateTime
@Embedded
@AttributeOverrides({ /* 同上 */ })
private DateTime updateTime;

@Note("素材类型")
@Column(nullable = false, length = 16)
private AssetType type;                 // 枚举，实现 Enumerable<T>
```

- 主键统一 `Long xxxId` + `@SnowflakeId`，禁止 `AUTO_INCREMENT`；实体通过 `SnowflakeEntityListener` 在持久化前生成主键。
- 时间字段按用途选择，禁止“一刀切”：
  - **统计/分析维度字段**：需要按年、月、季度、周、日聚合，或做时间区间筛选、趋势/同比环比分析时，使用框架 `DateTime` 值对象，并通过 `@Embedded + @AttributeOverrides` 展开年/月/日等维度列。
  - **纯日期时间标记字段**：仅用于标识“某时间点/某日期”，不参与多维统计时，使用 `java.time.LocalDateTime`；仅表示日期时使用 `java.time.LocalDate`。
  - **创建时间 / 更新时间**：默认继续使用框架 `@CreateTime / @UpdateTime + DateTime`（审计时间约定，同时天然支持按时间维度统计）；明确只做流水、不参与多维统计的表（如余额流水 `BalanceLedger`），`createTime` 可使用 `LocalDateTime`。
- 枚举实现框架 `Enumerable<T>`，字段用枚举类型。

### 4.3.1 全局搜索：@Keyword 全字段模糊（2026-09-04 新增）

前端全局搜索（`th4-decoboon-web` 场景插件化命令面板，Ctrl+K）依赖框架的 **keyword 查询机制**：
请求体带 `keyword` 字段时，框架对实体上标注 `@Keyword` 的 **String 字段做 OR 模糊匹配**（跨 `@ManyToOne` 关联对象递归），并与其它查询条件 AND。

- 可搜索的字符串字段（名称/电话/需求/标题/备注/正文等）一律加 `@Keyword`：

```java
import com.th4.edge.framework.data.jpa.keyword.annotation.Keyword;

@Note("客户称呼")
@Keyword
@Column(nullable = false, length = 32)
private String customerName;
```

- 已标注清单（2026-09-04）：`Lead(customerName,contact,requirement,source)`、`Company(name,city,planCode)`、`Store(name,address)`、`Employee(name,phone,wechat,displayCompanyName)`、`Video(title)`、`VideoScript(hook,content,tags)`、`Asset(remark,description)`。
- 注意：**没有 `@Keyword` 字段的模型，前端传 `keyword` 会退化为无谓词全量返回**（如 admin jar 内的 `user`，前端场景改用 `username[Like]` 单字段查询规避）。
- 新增模型如需被全局搜索命中，务必在可搜索字段补 `@Keyword`，并在前端 `src/views/search/scenarios/` 注册对应场景。

### 4.4 实体禁止事项

- ❌ 禁止在实体里写业务逻辑（只做数据承载）。
- ❌ 禁止用 `@JsonIgnore` 硬编码序列化控制（具体 Provider 用 `@Properties`，通用 ModelService 用 `@ServiceProperties`）。
- ❌ 禁止把关联字段退化成 `Long xxxId` 裸外键（必须对象关联）。
- ❌ 需要按年/月/季度等维度统计的业务时间字段，不得使用 `Date`/`LocalDateTime`/`LocalDate`，必须用 `DateTime`；纯标记型时间才允许使用 `LocalDateTime`/`LocalDate`。
- ✅ 每个字段用 `@Note` 标注中文标签；金额/大小等用 `@Unit`。

### 4.5 事务边界与懒加载禁则（强制，2026-09-16 新增）

生产 `application-prod.yml` **必须保持 `spring.jpa.open-in-view: true` 打开**（强制，2026-09-16 起）。生产为回环绑定部署，请求级 Session 承担关联加载兜底；一旦关闭，事务外访问 LAZY 关联的代码会立刻出现"本地与测试全绿、上线必崩"的 `LazyInitializationException ... no session`。2026-09-16 已因该模式发生两次生产事故（`Employee`、`PlanVersion`）。

OSIV 打开是**运行时兜底**，不是代码可以依赖的许可：下列禁则对代码始终有效，必须做到关闭 OSIV 也能正确运行。

- ❌ 禁止在 `RequestFilter`、`@Component` 上下文类（`XxxContext`）、工具类等**无事务边界**的代码中访问实体的 LAZY 关联字段。
- ❌ 禁止以 `dev` / `test` 环境通过作为通过依据（默认 OSIV=true 会掩盖缺陷）。
- ❌ 禁止新增 `@ManyToOne(fetch = FetchType.LAZY)` 关联后，不排查 Filter / Context 等事务外查询链路。
- ❌ 禁止用临时 `EAGER` 或 `@JsonIgnore` 绕开懒加载问题。
- ✅ 事务外需要读取的关联，必须在查询上使用 `@EntityGraph(attributePaths = {...})` 或 `JOIN FETCH` 预取。
- ✅ 聚合、校验类上下文方法若必须访问关联，应置于 `@Transactional(readOnly = true)` 内；带 `PESSIMISTIC_WRITE` 的查询不要与 `@EntityGraph` 随意组合。
- ✅ 涉及关联的回归测试必须显式设置 `spring.jpa.open-in-view=false`，且测试方法**不得**加 `@Transactional`，否则无法复现缺陷。
- ✅ 错误集与检测清单：`_系统/协作/lanes/development/workspace/code-error-catalog.md`（ECD-001）。

---

## 5. 序列化控制：@Properties 与 @ServiceProperties 使用边界

### 5.1 为什么需要它

实体用 `@ManyToOne` 关联后，返回前端时会**把关联对象的所有属性一起序列化**（如 `Employee` 的密码、手机号、内部字段都会漏出去）。序列化过滤必须声明式、可审计，替代 `@JsonIgnore` 的硬编码。

### 5.1.1 轻量化响应 JSON 设计原则（强制）

`@Properties` 的第一目标是满足当前前端页面的数据需求，第二目标才是兼容实体对象返回；禁止把完整领域聚合当作默认响应。

生成 Provider 前必须先确定响应场景：

| 场景 | 字段原则 | 典型内容 |
| --- | --- | --- |
| `page/list/count` | 只返回列表展示、筛选、排序和跳转所需字段 | 主键、名称、状态、摘要字段、最小负责人引用 |
| `info` | 返回当前详情页实际使用的业务字段 | 主键、完整业务字段、必要的编码/名称、最小关联对象 |
| `save/update` | 返回前端更新本地状态所需的最小实体，不返回完整聚合 | 主键、被编辑字段、状态、最小关联对象 |
| `build` | 只返回新建表单默认值和枚举上下文 | 表单字段、必要默认值 |

**硬性规则：**

- 关联对象只有在页面确实展示时才返回；不能因为实体声明了 `@ManyToOne` 就默认返回整棵对象树。
- 对关联对象使用 `@AllowProperties` 做白名单，只允许 `empId/name` 等页面字段；禁止把 `Company/Store/Team/Employee` 的管理字段整批透传。
- 懒加载关联是序列化性能风险：响应白名单未引用的关联不会被序列化，不应在响应过滤器或业务代码中为了“补全”无关字段触发额外查询。
- 列表响应与详情响应不得复用同一套“大而全”字段配置；列表排除详情专属长文本、备注、地址、审计字段和归属树。
- 企业、门店、团队、权限归属字段只在业务页面明确展示时返回；数据权限判断不依赖响应字段。
- 详情响应不得继续序列化会形成 `Property -> Store -> Team -> Employee -> Store` 等循环的完整归属图；只需项目摘要时只允许 `projectId/name` 等最小字段。新增或修改 `info` Provider 后必须检查实际 JSON 大小与可解析性，禁止出现数百 KB 或超深嵌套 JSON。
- 具体 Provider 继续使用 Provider 级 `@Properties`；只有无自定义 Provider 的通用路由才使用 `@ServiceProperties`。
- 禁止新增 DTO/VO 实现字段裁剪；裁剪必须通过 `@Properties/@AllowProperties` 声明。

客户域基准配置：

```text
page   -> custId/name/telNo/community/area/status/owner(empId,name)
info   -> page 字段 + addr/remark，仍不返回 company/store/team
save   -> custId/name/telNo/community/addr/area/status/owner(empId,name)/remark
update -> 与 save 相同
```

### 5.2 具体 Provider：直接使用 `@Properties`

只要当前响应由**明确的自定义 Provider**处理，就直接在 Provider 类上标注 `@Properties`。`@Properties` 不需要写 `uris`，其作用域就是当前 Provider 自身的响应。

```java
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.data.jackson.annotation.IgnoreProperties;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;

@Properties(
        // 对 Employee 模型，隐藏敏感字段
        ignore = @IgnoreProperties(model = Employee.class,
                                   properties = {"password", "phone", "salt"}),
        // 对 Asset 模型，只放行业务字段（可选）
        allow = @AllowProperties(model = Asset.class,
                                 properties = {"assetId", "emp", "type", "url", "size", "createTime"})
)
@Service("asset/page")
public class Page extends AbstractPage<Asset> implements Page { /* ... */ }
```

### 5.3 框架通用 ModelService：使用 `@ServiceProperties`

框架默认支持的 `{model}-save`、`{model}-update` 等通用路由没有对应的自定义 Provider，无法在 Provider 上声明响应过滤。此时才使用 `@ServiceProperties`，通过 `uris` 指定框架通用服务路由。

```java
@ServiceProperties(
        uris = {"user-save", "user-update"},
        properties = @Properties(
                ignore = @IgnoreProperties(model = User.class,
                                           properties = {"password", "salt"})
        )
)
@Model
@Entity
@Table(name = "db_user")
public class User extends AbstractModel { /* ... */ }
```

### 5.4 硬性规则

- `ignore`：忽略指定模型的指定属性（过滤敏感字段、内部字段）。
- `allow`：白名单模式，只输出指定属性（适合对前端暴露面做最小化）。
- **有具体 Provider：只用 `@Properties`，禁止再包一层 `@ServiceProperties`。**
- **无具体 Provider、由框架通用 `{model}-{service}` 响应：使用 `@ServiceProperties(uris=..., properties=@Properties(...))`。**
- `@ServiceProperties.uris` 必须与框架通用服务路由一致，不得用于自定义 Provider 路由。
- 同一个 Provider 不得同时使用 `@Properties` 与 `@ServiceProperties`。
- 关联对象 `Employee` 内部的嵌套关联也可继续用 `IgnoreProperties` 逐层裁剪。

### 5.5 与数据权限的边界（强制）

- `@ServiceProperties` **只负责响应序列化字段过滤**，不能被用作 Criteria 查询条件、数据范围或权限声明。
- `{model}-page|list|count` 的数据权限由实体上的 `@DataPermission`、`DataScopeContext` 和 `DataScopeRequestFilter` 负责；自定义详情、更新、删除、跨域查询使用 `DataScopeSpecification`。
- 禁止把 `company/store/team/owner` 写进 `@ServiceProperties` 试图实现权限过滤；它只能决定这些字段是否输出到前端，不能决定数据库查哪些数据。
- 有具体分页 Provider 时，响应裁剪使用 Provider 上的 `@Properties`；只有确实由框架通用 `{model}-page` 路由直接响应且没有具体 Provider 时，才使用 `@ServiceProperties(uris = "...-page", properties = @Properties(...))`。
- `@DataPermission` 管“查得到什么”，`@ServiceProperties/@Properties` 管“返回什么”，两者必须同时设计，不能互相替代。

---

## 6. 完整示例（以 asset 素材域落地）

### 6.1 实体：Employee 关联 Asset

```java
// company/model/Employee.java（片段）
@Note("员工")
@Model
@Entity
@Table(name = "db_clue_employee")
public class Employee extends AbstractModel {

    @Note("员工索引")
    @Id
    @SnowflakeId
    private Long empId;

    @Note("姓名")
    @Column(nullable = false, length = 32)
    private String name;

    @Note("手机号")
    @Column(length = 16)
    private String phone;

    @Note("登录密码")
    @Column(length = 64)
    private String password;            // 序列化时按服务边界用 @Properties/@ServiceProperties 隐藏
    // getters/setters
}
```

```java
// asset/model/Asset.java
@Note("素材")
@Description("业务员上传的工地照片/视频素材")
@Model
@Entity
@DataPermission(owner = "emp")
@Table(name = "db_clue_asset", indexes = {
        @Index(name = "idx_asset_company", columnList = "com_id, asset_id"),
        @Index(name = "idx_asset_store", columnList = "com_id, store_id, asset_id"),
        @Index(name = "idx_asset_team", columnList = "com_id, team_id, asset_id"),
        @Index(name = "idx_asset_owner", columnList = "com_id, emp_id, asset_id")
})
public class Asset extends AbstractModel {

    @Note("素材索引")
    @Id
    @SnowflakeId
    private Long assetId;

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("所属门店")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Note("所属团队")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Note("归属业务员")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", nullable = false)
    private Employee emp;               // ★ 对象关联，不再是 Long empId

    @Note("素材类型")
    @Column(nullable = false, length = 16)
    private AssetType type;

    @Note("OSS 地址")
    @Column(nullable = false, length = 512)
    private String url;

    @Note("大小")
    @Unit("MB")
    @Column
    private Long size;

    @Note("创建时间")
    @CreateTime
    @Embedded
    @AttributeOverrides({ /* 沿用现有 DateTime 列映射惯例 */ })
    private DateTime createTime;
    // getters/setters
}
```

### 6.2 分页服务

```java
// 接口：asset/service/assets/page/Page.java
public interface Page extends ModelService<Asset, org.springframework.data.domain.Page<Asset>>, Security {
}
```

```java
// 实现：asset/service/assets/page/support/Page.java
@Note("素材分页")
@Description("按通用查询语法分页查询素材")
@Properties(
        ignore = @IgnoreProperties(model = Employee.class,
                                   properties = {"password", "phone"})
)
@Service("asset/page")
@ServiceName("page")
@Scope("prototype")
public class Page extends AbstractPage<Asset> implements Page {

    public Page(EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    public org.springframework.data.domain.Page<Asset> call(Asset request) {
        return super.call(request);
    }
}
```

> 关键字、字段谓词、排序、page/size 由框架 Criteria 过滤器自动装配；因为 `Asset` 标注了 `@DataPermission(owner = "emp")`，`DataScopeRequestFilter` 会在 `call()` 前自动追加 `Company/Store/Team/Self` 条件。Provider 不应再复制角色判断或手写 `applyDataScope`。

### 6.3 保存服务（AutoInject + 对象组装）

```java
// 接口：asset/service/assets/save/Save.java
public interface Save extends Provider<Asset, Asset>, Security {
}
```

```java
// 实现：asset/service/assets/save/support/Save.java
import com.th4.edge.framework.data.jpa.inject.annotation.ModelSaveInject;

@Note("素材保存")
@Description("新增素材，自动关联归属业务员")
@Service("asset/save")
public class Save implements Save {

    /** 框架注入：当前登录员工。 */
    @User
    private Employee employee;

    private final AssetRepository assetRepository;
    private final EmployeeRepository employeeRepository;
    private final TenantContext tenantContext;

    public Save(AssetRepository assetRepository,
                EmployeeRepository employeeRepository,
                TenantContext tenantContext) {
        this.assetRepository = assetRepository;
        this.employeeRepository = employeeRepository;
        this.tenantContext = tenantContext;
    }

    /** 前端传入：归属业务员 ID */
    @AutoInject
    @NotNull
    private Long empId;

    /** 前端传入：素材类型 */
    @AutoInject
    @NotNull
    private AssetType type;

    /** 前端传入：OSS 地址 */
    @AutoInject
    @NotEmpty
    @Length(max = 512)
    private String url;

    @Override
    public Asset call(@ModelSaveInject Asset asset) {
        Employee current = tenantContext.requireEmployee(employee);
        Company company = current.getCompany();

        // 企业固定取登录员工；非 BOSS 不能把数据分配给其他员工。
        Employee emp = current;
        if (current.getRole() == EmployeeRole.Boss && empId != null) {
            emp = employeeRepository.findByEmpIdAndCompany_ComId(empId, company.getComId());
            if (emp == null) {
                throw new DefinedException(new DefaultErrorCode("400", "负责人不属于当前企业"));
            }
        }

        asset.setCompany(company);    // 组装企业对象关联
        asset.setStore(emp.getStore()); // 从服务端确认后的负责人复制门店快照
        asset.setTeam(emp.getTeam());   // 从服务端确认后的负责人复制团队快照
        asset.setEmp(emp);            // 组装 ManyToOne 对象关联
        // type/url 已由 @ModelSaveInject 组装；上面的 AutoInject 字段保留用于调用前校验。
        return assetRepository.save(asset);
    }
}
```

> 保存注解只负责组装模型；Provider 必须继续执行服务端归属覆盖、状态保护和 `save/persist`。普通自定义更新不要用 `@ModelUpdateInject` 绕过 `@DataPermission`；自动权限更新优先走框架通用 `service/update`，严格字段更新使用 `DataScopeSpecification`。

### 6.4 统计服务（唯一允许自定义 Request 的域）

```java
// statistic/service/dashboard/Dashboard.java（接口）
public interface Dashboard extends Provider<DashboardQuery, DashboardResult> {
}
```

```java
// 统计入参允许语义化请求类型，但绝不命名 DTO
public class DashboardQuery {
    @AutoInject @NotNull private Long companyId;
    @AutoInject private Integer month;      // 统计需要，允许走成员变量注入
    // ...
}
```

---

## 7. 命名速查表

| 元素 | 规范 |
| --- | --- |
| 服务接口 | `{能力名}`（一个单词：`Page/List/Save/Info/Build/Update/Delete/Count/Enums`） |
| 服务实现 | `{能力名}`（放在 `support` 包） |
| 服务包 | `{领域}.service.{业务对象}.{能力}` + `.support` |
| 路由 | `@Service("{业务域}/{业务对象}/{能力}")`（框架按 `@Service` 扫描注册 Provider 路由） |
| 实体 | `{业务对象单数}`（`Asset`），继承 `AbstractModel`，`@Model + @Note` |
| 仓储 | `{业务对象}Repository`，接口继承 Spring Data |
| 枚举 | 实现 `Enumerable<T>`，放 `configure` 包；命名简洁，如 `SubStatus`，不使用 `SubscriptionStatus` |
| 主键字段 | 简洁缩写，如 `subId/comId/storeId/empId/planId/orderId/ledgerId`，不使用 `subscriptionId` |
| 关联字段 | 对象语义名（`emp/company/video`）+ `@ManyToOne` |
| 请求类型 | 实体类型；统计域用语义化 `XxxQuery`（禁止 DTO） |
| 登录保护 | 需要 token 的 Provider 接口继承 `Security`；公开能力不继承 |
| 当前登录员工 | Request 为 Employee：`call(@User Employee employee)`；其他 Request：`@User private Employee employee` |
| 序列化 | 具体 Provider：`@Properties(ignore/allow)`；框架通用 `{model}-{service}`：`@ServiceProperties(uris, properties=@Properties(...))` |
| context 类 | `context` 包下的横切/领域上下文统一以 `Context` 结尾（如 `TenantContext`、`SubscriptionContext`），不命名为 `XxxService` |

---

## 8. 代码生成自检清单（交付前逐项核对）

- [ ] 是否有 `Controller` / `@RequestMapping`？→ 必须改为 Provider，删除。
- [ ] 是否定义了 `XxxDTO / XxxVO / XxxRequest` 内部类？→ 除统计域外，一律删除，入参用实体。
- [ ] 前端传入字段是否用 `@AutoInject` 成员变量注入？是否遗漏 `@NotNull/@NotEmpty/@Length` 校验？
- [ ] 需要登录的 Provider 接口是否继承 `Security`？公开接口是否错误继承？
- [ ] 当前登录员工是否通过 `@User` 注入（Request=Employee 用参数，其他 Request 用字段）？是否误用了 `SecurityManager.authenticate(...)` 或手工解析 token？
- [ ] 是否先定义了企业、角色和数据范围权限矩阵？是否把登录成功误当成业务权限通过？
- [ ] 查询、保存、更新、删除是否都从 `@User` 重新确定当前企业和角色？是否仍信任前端 `companyId/empId/ownerId`？
- [ ] 实体是否包含 `company/store/team/owner` 归属并标注 `@DataPermission`？字段名和 `@DataPermission` 是否一致？
- [ ] 分页/列表/计数是否通过 `CriteriaQueryAware + @DataPermission` 进入统一拦截器，具体业务 Provider 是否仍继承 `Security`？是否只做 `request.setCompany(...)` 而留下越权入口？
- [ ] 框架通用 `service/info|update|delete` 是否穿透 `DataPermissionCrudAspect`？自定义 Provider 是否使用 `DataScopeSpecification`，而不是 `findById(id)` 后判断？
- [ ] 跨域读取业务实体时是否同时携带企业和数据范围条件？
- [ ] 保存时 `Company` 是否固定取当前登录企业；非 BOSS 的负责人是否强制为本人；BOSS 指定负责人时是否校验其属于当前企业，并复制正确 `store/team`？
- [ ] 是否在 `call()` 里手写 `if (xxx == null) throw`？→ 改用校验注解。
- [ ] 响应类型是否直接是实体 / `List<实体>` / `Page<实体>`？
- [ ] 实体关联字段是否对象关联（`Employee emp` + `@ManyToOne`），而不是裸 `Long empId`？
- [ ] 业务表是否按实际权限维度建立 `(com_id, 业务主键)`、`(com_id, store_id, 业务主键)`、`(com_id, team_id, 业务主键)`、`(com_id, emp_id, 业务主键)` 索引？
- [ ] 是否完成四角色、跨企业、伪造参数、详情越权、更新/删除越权测试？
- [ ] 是否明确员工调店、调团队、业务转交时的数据归属快照规则？
- [ ] 实体序列化是否用了 `@JsonIgnore`？→ 必须改为：具体 Provider 用 `@Properties`，框架通用 ModelService 用 `@ServiceProperties`。
- [ ] 具体 Provider 是否误用了 `@ServiceProperties`？→ 必须改为直接使用 `@Properties`。
- [ ] `page/list/info/save/update` 是否按前端场景分别设计字段白名单，而不是复用完整实体图谱？
- [ ] 关联对象是否只返回页面实际使用的最小字段？是否避免序列化 Company/Store/Team 等完整归属树？
- [ ] 是否存在未使用的懒加载关联或大文本字段被默认序列化，导致额外 SQL 或响应膨胀？
- [ ] Provider 是否放在 `service.{业务对象}.{能力}` 结构下（接口 + `support` 实现）？
- [ ] 是否写了 `@Note/@Description`（类）与字段 `@Note`？时间字段是否按用途选择：统计维度用 `DateTime`，纯日期时间标记用 `LocalDateTime`/`LocalDate`，审计时间用 `@CreateTime`/`@UpdateTime + DateTime`？
- [ ] 路由 `@Service` value 是否符合 `{域}/{对象}/{能力}` 约定（框架 `AbstractHttpProcessor` 按 `@Service` 扫描注册路由，勿用 `@Component`）？
- [ ] 是否避免了 DTO、裸 SQL、空 catch、实体业务逻辑？
- [ ] 本次新增/修改的查询是否可能被 Filter / Context（事务外）调用？若是，LAZY 关联是否已 `@EntityGraph` / `JOIN FETCH` 预取？
- [ ] 涉及实体关联的回归测试是否显式 `spring.jpa.open-in-view=false` 且未加 `@Transactional`，并确认修复前会失败？
- [ ] 是否排查了同一懒加载根因的其他调用点，而不是只修一处（见 ECD-001）？

---

## 9. 前端视图注册规范（@Role / @View）

前端菜单与路由由后端声明式注册（th4-admin `SystemViewManager` 启动时扫描落库，登录后按角色下发）。

### 9.1 角色注册

- 一个角色一个 `@Role`（`@Role` 不可重复标注，多角色分 class），放在 `configure` 包。
- `idKey` = 角色索引 = 登录账号 = 前端 `role.home`（`/marketer`、`/boss`）；`user = true` 启动时自动创建该账号用户（初始密码 = idKey）。
- `title` = 浏览器标题（品牌文案，如「装得清 · 装修获客」）；`component` = 布局壳 `views/admin/Home.vue`。

```java
@Role(idKey = "marketer", name = "业务员", title = "装得清 · 装修获客",
      component = "views/admin/Home.vue", user = true)
@View(name = "素材管理", parent = "获客管理@Promotion", path = "asset/asset",
      icon = "Picture", priority = 1,
      route = @Route(component = "views/marketer/asset/asset/manage/AssetManageView.vue"),
      roles = {@Role(idKey = "marketer")})
@Component
public class MarketerViewConfigure { }
```

### 9.2 @View 硬性规则

| 规则 | 说明 |
| --- | --- |
| `name` 全局唯一 | 路由表按 name 去重；**同一功能多角色必须用差异化菜单名**（如「我的线索」/「线索管理」） |
| `path` 相对 role.home | 不写角色前缀（URL = `/marketer` + `/asset/asset`）；同一 path 可被不同角色各自注册 |
| `parent` 菜单分组 | 支持 `父/子` 多级（按 `/` 拆分）；`名称@图标` 写法给分组菜单加图标；同组菜单跨角色复用 |
| `route.component` | 与前端文件路径逐字符一致，以 `views/` 开头 |
| `roles` | 必填，声明可见角色（角色须已由 `@Role` 注册） |
| `icon` / `priority` | Element Plus 图标名；同组内按 priority 升序排列 |

### 9.3 前端页面目录 ↔ 后端注册对照

```text
后端 @View(path="asset/asset")
  → 前端访问 URL /marketer/asset/asset
  → 前端组件 views/marketer/asset/asset/manage/AssetManageView.vue
```

前端页面生成规范详见前端工程 `th4-decoboon-web/rules.md`（页面目录、BasicManageView、自定义页、接口约定）。


---

## 10. 成本、利润与套餐边界（强制）

- **销售价与成本价分离**：`CatalogItem.materialPrice/manualPrice` 表达销售价，`CatalogItem.costPrice` 直接表达企业标准成本价。成本价由 `Boss`、`StoreManager`、`ProjectSupport`、`Finance` 维护；设计师、业务员、项目经理默认不可见。
- **供应商后置**：报价项目可以先建立并维护成本，不要求先创建供应商或逐项采购价。供应商在采购前维护，并通过品牌关系匹配材料，例如“马可波罗”品牌关联多个供应商。
- **采购匹配**：材料采购按 `CatalogItem.brand` 查询供应商品牌关系，采购单保存本次供应商和采购执行价。`PartnerPrice` 只保留历史价格、比价和参考，不再作为标准成本必需来源。
- **签约成本快照**：报价阶段成本仅作参考；合同签约时按当时 `CatalogItem.costPrice` 和报价行数量计算，并冻结到合同行。签约后修改报价项目成本不得改变已签约成本。
- **项目成本**：`ProjectCostContext` 是项目成本与完整性唯一口径。预计成本优先使用签约合同行成本；实际成本优先采购到货、入库等实际发生值，缺失实际值时按签约预计成本回算。
- **利润可靠性**：所有应计成本项都有成本时才允许计算利润。缺失时返回 `costComplete=false`、利润/毛利率为 `null`，前端显示“成本未完整”。
- **实际成本累计**：采购到货累计实际材料成本；领料、损耗不得再次累计；退换货按方向冲减。
- **套餐功能边界**：免费版、基础版、专业版、旗舰版均提供完整业务模块；版本差异只允许是门店数和席位数。

## 11. 租户订阅与流水特殊约定

- 订阅主体命名为 `Subscription`，主键为 `subId`，状态枚举为 `SubStatus`。
- **剩余可用天数不落库**：由 `serviceEndTime - 当前时间` 动态计算，禁止在实体中保存 `balanceDays`，也禁止每日定时递减订阅天数。
- 续费/试用只维护 `serviceEndTime`、`plan`、`status`、`trialActivated` 等字段；余额流水 `BalanceLedger` 保留变动天数与变动后快照用于审计。
- `BalanceLedger.createTime` 属于纯流水记录时间，使用 `java.time.LocalDateTime`，不参与年/月/季度多维统计，不使用 `DateTime`。
- **门店默认不体现**：每家企业注册时只创建一家默认总店（`Store.defaultStore=true`），前端默认不展示门店维度。
- **多门店是套餐/权益能力**：`Plan.maxStores` 控制最大门店数；`maxStores=1` 表示仅默认总店，`maxStores>1` 才允许启用多门店；`Company.setting.enableStore` 控制前端是否展示门店维度。

---

## 12. 装得清品牌 Logo 统一规范（强制）

### 12.1 唯一品牌母版

装得清只允许使用 `th4-decoboon-app/logo-export/` 导出的同一套 Logo，不允许手写文字 Logo、不能用“清/装”单字方块、不能用通用 building/briefcase 图标冒充品牌 Logo。

- 深色或品牌色背景：使用 `logo-mark-white@2x.png` 的白色版。
- 浅色或白色背景：使用 `logo-mark-color@2x.png` 的彩色版。
- 单色印刷/特殊限制场景：才允许 `logo-mark-black@2x.png`，界面代码默认不使用。

### 12.2 各端统一资源

| 端 | 统一组件 | 运行时资源 |
| --- | --- | --- |
| App | `src/components/JzBrandLogo.vue` | `src/static/brand/logo-mark-white.png`、`src/static/brand/logo-mark-color.png` |
| Web | `src/components/BrandLogo.vue` | `public/brand/logo-mark-white.png`、`public/brand/logo-mark-color.png` |
| Admin | `src/components/BrandLogo.vue` | `public/brand/logo-mark-white.png`、`public/brand/logo-mark-color.png` |

> 页面不得直接新增 Logo 文件或写死图片路径；必须使用上述组件，并通过 `variant="white|color"` 选择背景适配版本。

### 12.3 资源校验

- `logo-mark-white.png` SHA-256：`12ade1524dfe4cd4b11d06593beff4ead19179a0288cb5f8765f08c3d2ffc997`
- `logo-mark-color.png` SHA-256：`1115517a138f1998c54cc71619b74bb7aa84af1193b0eb791c08d5db6bc75577`

更新 Logo 时必须在三端同时替换、更新组件资源和本节校验值，并执行 `type-check`、Web/Admin 构建、App `verify:mp-weixin`。

---

## 13. 可完成状态投影规范（强制）

首次入驻、实施清单、待办、资料完整度、节点进度、审批进度和质量检查清单，统一遵守本节。

### 13.1 冻结字段

编码前必须冻结：

```text
key / label / required / source_provider / completion_predicate / count / refresh_triggers / next_action / 异常态
```

没有完成谓词、必做/可选口径、数据源和刷新触发的页面，不得进入编码或验收。

### 13.2 完成事实

- 完成状态只能来自当前租户的真实领域对象和明确谓词。
- 禁止 `done: true/false`、演示数据、接口 `success=true` 或仅前端本地布尔值。
- 接口异常、数据缺失和分页无法证明全量时，状态为未知/失败，不得折叠为确定完成或确定未完成。
- 可选步骤完成后，不得改变必做分母、必做完成数或必做下一步。

### 13.3 刷新与幂等

- 可变进度必须在页面重新显示、下拉刷新或跨页写入后重新读取，或使用明确的失效机制收敛。
- 新增、编辑、删除、停用和状态回退都必须反映到下一次投影。
- 重复进入、重复刷新和重复事件不得重复计数或错误推进。

### 13.4 验收最低矩阵

必须覆盖初始未完成、动作成功返回、接口异常、可选完成、必做未完成、删除/停用回退和重复进入。截图、类型检查、构建或接口 200 不能替代“业务动作 -> 返回页面 -> 状态收敛”的真实证据。

### 13.5 首次入驻冻结口径

- 必做固定为 3 项：有效报价项目、至少一项正数标准成本、至少一个客户并关联房屋。
- 供应商品牌关系为可选步骤；完成谓词为 `status=active && type=supplier && brands.length > 0`。
- 第一份报价、签约和项目立项属于业务里程碑，不纳入首次入驻完成分母。
- 权威状态表与验收矩阵见 `输出/草稿/装得清首次入驻三件套-20260918/07-系统内引导与验收清单.md`。
