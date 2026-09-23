> 归档说明：本文描述的是旧 `decoboon.clue` 获客域方案。现行装得清 SaaS 方案已取消 `clue` 中间包和 `decoboon/` 路由前缀，本文仅保留作历史参考。

# 装得清（th4-decoboon）后端技术设计

> 以 JPA 为核心 · 智能体基于 AgentScope Harness · 对接 DeepSeek · 禁用 MyBatis / 禁用 Controller
> 项目根：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon`
> 版本：v1.0 · 2026-09-03

---

## 0. 设计目标与硬约束

| 维度 | 结论 |
| --- | --- |
| 数据访问 | 以 Spring Data JPA 为核心，**禁用 MyBatis**（框架内已有 `FrameworkMyBatisConfiguration`，本工程不启用） |
| 智能体 | 采用 **AgentScope Java 2.0** 的 `agentscope-harness`（`HarnessAgent`）作为智能体运行底座 |
| 大模型 | 对接 **DeepSeek**（2.0.1 起为一等供应商：`deepseek:<model>` + `DEEPSEEK_API_KEY`） |
| 多租户 | 每个装饰公司 = 一个 Env（复用框架 `EnvModel`/`EnvHolder`），业务员归属公司 |
| 包规范 | 延续 `com.th4.edge.decoboon.*`（获客域）分层：`configure/model/repository/provider/service/listener/event/context/agent` |
| 工程环境 | Spring Boot 3.4.0 / JDK 22 / MySQL / 依赖 `th4-admin:1.0.0` |

---

## 1. 总体架构

```
┌──────────────────────────────────────────────────────────────────────┐
└───────────────┬──────────────────────────────────────────────────────┘
                │ HTTP (JSON / Multipart)
┌───────────────▼──────────────────────────────────────────────────────┐
│  HTTP 层：th4-framework HttpProcessor（自动探测 Provider，无 Controller）  │
│  · 参数解码/编码 · JWT 安全 · 参数校验 · 分页/关键字/谓词自动装配            │
└───────────────┬──────────────────────────────────────────────────────┘
┌───────────────▼──────────────────────────────────────────────────────┐
│  应用层 Provider（业务用例入口，替代 Controller）                           │
│  clue.video / clue.asset / clue.lead / clue.company / clue.statistic    │
└───────┬───────────────────────────────┬──────────────────────────────┘
        │ 调用                       │ 调用
┌───────▼───────────┐      ┌──────────▼───────────────────────────────┐
│  领域层（JPA）       │      │  智能体层 AgentScope Harness + DeepSeek      │
│  Entity + Repository│◄────►│  HarnessAgent（workspace/memory/tool）      │
│  · 业务规则/事件/监听  │  Tool│  · 文案/话题钩子/线索分析/跟进提醒               │
└───────┬───────────┘      └──────────┬───────────────────────────────┘
        │ JPA                          │ 异步任务编排（@Async + 事件）
┌───────▼──────────────────────────────▼──────────────────────────────┐
│  数据层：MySQL（th4 JPA 自动建表 ddl-auto:update）+ Redis（缓存/会话/队列）   │
│  Agent 工作区：.agentscope/workspace（AGENTS.md / MEMORY.md / skills/）   │
└──────────────────────────────────────────────────────────────────────┘
```

**关键设计决策**

1. **无 Controller 的统一入口**：所有前端调用走 `Provider`，由框架 `HttpProcessor.detectServiceMapping()` 按 Bean 类型自动注册路由，参数自动从 HTTP 解码成 Request 对象。业务代码只关心 `Provider.call(Request)`。
2. **智能体不直接碰 HTTP**：`HarnessAgent` 通过 `@Tool` 注入的 Java 方法访问 JPA 仓储与业务服务，保证"模型→工具→数据库"链路完全受控、可审计。
3. **生成任务异步化**：AI 出片（文案→配音→字幕→合片→尾卡）是长耗时链路，采用"任务实体 + 状态机 + @Async 异步执行 + JPA 事件驱动下一步"。

---

## 2. 包结构设计（延续现有规范）

```text
com.th4.edge.decoboon
├── package-info.java
└── clue                                  # 获客域（主域，沿用现有命名）
    ├── company                           # 公司/门店/业务员（租户与账号）
    │   ├── configure                     # StoreType 等
    │   ├── model                         # Company / Store / Employee
    │   ├── repository                    # CompanyRepository / EmployeeRepository
    │   ├── provider                      # EmployeeLoginProvider / EmployeeInfoProvider
    │   └── listener                      # EmployeeEnvPrePersistListener
    ├── asset                             # 素材域（业务员上传的工地照片/视频）
    │   ├── configure                     # AssetType（已有）、AssetUsage
    │   ├── model                         # Asset
    │   ├── repository                    # AssetRepository
    │   ├── provider                      # AssetUploadProvider / AssetListProvider
    │   └── listener                      # AssetPostPersistListener
    ├── video                             # 视频域（AI 生成与发布）
    │   ├── configure                     # TemplateType（已有）、VideoState、VideoTaskState、PublishChannel
    │   ├── model                         # Template / Video / VideoCopy / VideoTask / VideoStat
    │   ├── repository                    # TemplateRepository / VideoRepository / VideoTaskRepository
    │   ├── provider                      # VideoGenerateProvider / VideoPublishProvider / TemplateListProvider
    │   ├── agent                         # VideoCreationAgent（HarnessAgent 封装）
    │   │   ├── configure                 # DecoboonAgentConfigure（DeepSeek 模型/工作区装配）
    │   │   ├── tool                      # TemplateTool / ScriptTool / LeadAnalysisTool / FollowRemindTool
    │   │   └── context                   # DecoboonAgentManager（会话/租户上下文）
    │   ├── service                       # VideoRenderService（FFmpeg 合片）、TtsService、SubtitleService
    │   ├── event                         # VideoTaskStateChangeEvent
    │   ├── listener                      # VideoTaskStateListener（驱动流水线下一步）
    │   └── context                       # VideoQueryService（看板聚合）
    ├── lead                              # 线索域（客户线索与跟进）
    │   ├── configure                     # LeadState、LeadSourceType
    │   ├── model                         # Lead / LeadFollow
    │   ├── repository                    # LeadRepository / LeadFollowRepository
    │   ├── provider                      # LeadRegisterProvider / LeadTransferProvider / LeadFollowProvider
    │   ├── listener                      # LeadPostPersistListener（微信提醒/超时未跟进提醒）
    │   └── context                       # LeadQueryService
    └── statistic                         # 看板统计域（老板 ROI 看板）
        ├── configure                     # StatisticType
        ├── model                         # MonthlyStatistic / StatisticLine
        ├── repository                    # StatisticRepository（聚合投影）
        └── provider                      # DashboardProvider / SalesmanRankProvider
```

> 与现有代码的衔接：`video/model/Video.java`、`video/model/Template.java`、`video/configure/TemplateType.java`、`asset/configure/AssetType.java` 原样保留，在此基础上补全字段与关系。

---

## 3. 领域模型设计（JPA 实体）

### 3.1 多租户与账号

公司（租户）模型采用框架 `EnvModel` 机制，业务员/数据行通过 Env 隔离。以 `Company` 作为 `EnvModel` 子类承载租户维度。

```java
// company/model/Company.java
@Note("装修公司")
@Description("平台租户，一个公司一套获客数据")
@Model
@Entity
@Table(name = "db_clue_company")
public class Company extends EnvModel {  // P0 决策：已改用 AbstractModel + 显式 companyId 列（见 11.1 落盘说明）

    @Note("公司索引")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long companyId;

    @Note("公司名称")
    @Column(nullable = false, length = 64)
    private String name;

    @Note("所属城市")
    @Column(length = 32)
    private String city;

    @Note("开通状态")
    @Column(nullable = false)
    private Boolean enabled = Boolean.TRUE;

    @Note("工具套餐（如 299 元/月）")
    @Column(length = 32)
    private String planCode;

    @Note("创建时间")
    @CreateTime
    @Column(nullable = false, updatable = false)
    private DateTime createTime;

    @Note("更新时间")
    @UpdateTime
    @Column(nullable = false)
    private DateTime updateTime;
}
```

业务员（登录账号）：

```java
// company/model/Employee.java
@Note("业务员")
@Model
@Entity
@Table(name = "db_clue_employee")
public class Employee {

    @Note("业务员索引")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long empId;

    @Note("所属公司")
    @Column(nullable = false)
    private Long companyId;

    @Note("姓名")
    @Column(nullable = false, length = 32)
    private String name;

    @Note("电话")
    @Column(length = 20)
    private String phone;

    @Note("微信")
    @Column(length = 64)
    private String wechat;

    @Note("在职状态")
    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    @Note("默认公司名展示（前端下拉默认项）")
    @Column(length = 64)
    private String displayCompanyName;
}
```

### 3.2 模板（5 类内容模板）

```java
// video/configure/TemplateType.java（补充完整）
@Note("模板类型")
public enum TemplateType implements Attribute<String> {
    SiteShoot("工地实拍", "适合发施工进度"),
    AvoidPit("避坑干货", "适合讲专业知识"),
    Price("价格透明", "适合报价类内容"),
    FinishedCase("完工案例", "适合晒成果"),
    CustomerProof("客户证言", "适合转介绍");

    private final String name;
    private final String desc;

    TemplateType(String name, String desc) { this.name = name; this.desc = desc; }

    @Override public String getValue() { return toString(); }
    @Override public String getName() { return name; }
    public String getDesc() { return desc; }
}
```

```java
// video/model/Template.java（补全）
@Note("模板")
@Model
@Entity
@Table(name = "db_clue_template")
public class Template {

    @Note("模板索引")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tempId;

    @Note("模板类型")
    @Column(nullable = false, length = 32)
    private TemplateType type;

    @Note("模板名称")
    @Column(nullable = false, length = 64)
    private String name;

    @Note("话题钩子（候选文案，供业务员勾选）")
    @Column(length = 512)
    private String hooks;              // JSON 数组：["水电验收不注意这3点…","…"]

    @Note("文案提示词（喂给 Agent 的生成指令模板）")
    @Column(length = 2048)
    private String scriptPrompt;

    @Note("字幕样式")
    @Column(length = 128)
    private String subtitleStyle;      // 大字幕/黄高亮关键词等

    @Note("BGM 标识")
    @Column(length = 64)
    private String bgmCode;

    @Note("尾卡模板")
    @Column(length = 256)
    private String endCard;            // 公司Logo+电话

    @Note("上传图片数量限制")
    @Column(nullable = false)
    private Integer imageLimit = 5;

    @Note("启用")
    @Column(nullable = false)
    private Boolean enabled = Boolean.TRUE;

    @DataPreset({
        @DataItem({"SiteShoot","工地实拍","[\"水电验收不注意这3点…\"]","…","…","…","…","5"}),
        @DataItem({"AvoidPit","避坑干货","[…]","…","…","…","…","5"}),
        @DataItem({"Price","价格透明","[…]","…","…","…","…","5"}),
        @DataItem({"FinishedCase","完工案例","[…]","…","…","…","…","5"}),
        @DataItem({"CustomerProof","客户证言","[…]","…","…","…","…","5"})
    })
    private String presetPlaceholder;   // 仅示意：实际用框架 DataPreset 应用器初始化
}
```

> 模板属于**平台级共享数据 + 公司级可覆盖**：默认由 `@DataPreset` 预置 5 类模板；公司可复制并自定义。

### 3.3 素材（业务员上传的工地照片）

```java
// asset/model/Asset.java
@Note("素材")
@Model
@Entity
@Table(name = "db_clue_asset")
public class Asset {

    @Note("素材索引")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long assetId;

    @Note("所属业务员")
    @Column(nullable = false)
    private Long empId;

    @Note("素材类型")
    @Column(nullable = false, length = 16)
    private AssetType type;

    @Note("OSS 地址")
    @Column(nullable = false, length = 512)
    private String url;

    @Note("缩略图")
    @Column(length = 512)
    private String thumbUrl;

    @Note("大小")
    @Unit("MB")
    @Column
    private Long size;

    @Note("拍摄工地备注")
    @Column(length = 256)
    private String remark;             // 例如“水管走顶/强弱电交叉”

    @Note("创建时间")
    @CreateTime
    @Column(nullable = false, updatable = false)
    private DateTime createTime;
}
```

### 3.4 视频（AI 产物）与文案、统计

```java
// video/model/Video.java（补全）
@Note("短视频")
@Description("用于发布抖音引流的种草视频")
@Model
@Entity
@Table(name = "db_clue_video")
public class Video {

    @Note("视频索引")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long videoId;

    @Note("所属业务员")
    @Column(nullable = false)
    private Long empId;

    @Note("所属模板")
    @Column
    private Long tempId;

    @Note("封面图")
    private String cover;

    @Note("视频文件地址")
    @Column(length = 512)
    private String url;

    @Note("大小")
    @Unit("MB")
    @Column
    private Long size;

    @Note("时长")
    @Unit("秒")
    @Column
    private Long duration;

    @Note("标题")
    @Column(length = 128)
    private String title;

    @Note("状态")
    @Column(nullable = false, length = 16)
    private VideoState state;          // DRAFT/GENERATING/PUBLISHED

    @Note("发布渠道")
    @Column(length = 16)
    private PublishChannel channel;    // DOUYIN 等

    @Note("创建时间")
    @CreateTime
    @Column(nullable = false, updatable = false)
    private DateTime createTime;
}
```

```java
// video/model/VideoCopy.java —— 一条视频对应一段可复制发布的文案
@Note("视频文案")
@Model
@Entity
@Table(name = "db_clue_video_copy")
public class VideoCopy {

    @Note("文案索引")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long copyId;

    @Note("视频")
    @Column(nullable = false)
    private Long videoId;

    @Note("发布文案（业务员复制到抖音）")
    @Column(nullable = false, length = 2048)
    private String content;

    @Note("话题标签")
    @Column(length = 512)
    private String hashtags;           // #杭州装修 #水电改造…

    @Note("定位建议")
    @Column(length = 128)
    private String locationTip;
}
```

```java
// video/model/VideoStat.java —— 播放/评论/线索归因
@Note("视频数据")
@Model
@Entity
@Table(name = "db_clue_video_stat")
public class VideoStat {

    @Note("统计索引")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long statId;

    @Note("视频")
    @Column(nullable = false)
    private Long videoId;

    @Note("播放量")
    @Column(nullable = false)
    private Long viewCount = 0L;

    @Note("评论数")
    @Column(nullable = false)
    private Long commentCount = 0L;

    @Note("点赞数")
    @Column(nullable = false)
    private Long likeCount = 0L;

    @Note("由该视频带来的线索数")
    @Column(nullable = false)
    private Long leadCount = 0L;

    @Note("统计日期")
    @Column(nullable = false, length = 16)
    private String statDate;           // yyyy-MM-dd
}
```

### 3.5 生成任务（AI 流水线状态机）

```java
// video/configure/VideoTaskState.java
@Note("生成任务状态")
public enum VideoTaskState implements Attribute<String> {
    Created("已创建"),
    Writing("AI写文案"),
    Dubbing("AI配音"),
    Subtitling("生成字幕"),
    Rendering("合成视频"),
    EndCaring("生成尾卡"),
    Done("完成"),
    Failed("失败");

    private final String name;
    VideoTaskState(String name) { this.name = name; }
    @Override public String getValue() { return toString(); }
    @Override public String getName() { return name; }
}
```

```java
// video/model/VideoTask.java
@Note("视频生成任务")
@Model
@Entity
@Table(name = "db_clue_video_task")
public class VideoTask {

    @Note("任务索引")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long taskId;

    @Note("所属业务员")
    @Column(nullable = false)
    private Long empId;

    @Note("所属视频")
    @Column
    private Long videoId;

    @Note("模板")
    @Column(nullable = false)
    private Long tempId;

    @Note("选中的话题钩子")
    @Column(length = 512)
    private String hook;

    @Note("素材图片ID列表")
    @Column(length = 512)
    private String assetIds;           // JSON 数组

    @Note("当前阶段")
    @Column(nullable = false, length = 16)
    private VideoTaskState state = VideoTaskState.Created;

    @Note("阶段明细 JSON（文案/配音地址/字幕/合片地址）")
    @Column(length = 4096)
    private String detail;

    @Note("失败原因")
    @Column(length = 512)
    private String failReason;

    @Note("创建时间")
    @CreateTime
    @Column(nullable = false, updatable = false)
    private DateTime createTime;

    @Note("更新时间")
    @UpdateTime
    @Column(nullable = false)
    private DateTime updateTime;
}
```

### 3.6 线索（核心业务对象 + 状态机）

```java
// lead/configure/LeadState.java
@Note("线索状态")
public enum LeadState implements Attribute<String> {
    PendingContact("待联系"),
    PendingMeasure("待量房"),
    Measured("已量房"),
    Signed("已签约"),
    Abandoned("已流失");

    private final String name;
    LeadState(String name) { this.name = name; }
    @Override public String getValue() { return toString(); }
    @Override public String getName() { return name; }
}
```

```java
// lead/model/Lead.java
@Note("获客线索")
@Model
@Entity
@Table(name = "db_clue_lead")
public class Lead {

    @Note("线索索引")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long leadId;

    @Note("登记业务员")
    @Column(nullable = false)
    private Long empId;

    @Note("所属公司")
    @Column(nullable = false)
    private Long companyId;

    @Note("客户称呼")
    @Column(nullable = false, length = 32)
    private String customerName;

    @Note("联系电话/微信")
    @Column(nullable = false, length = 64)
    private String contact;

    @Note("需求描述")
    @Column(length = 512)
    private String requirement;

    @Note("来源视频")
    @Column
    private Long videoId;              // 线索自动关联来源视频（归因）

    @Note("来源渠道")
    @Column(length = 64)
    private String source;             // "抖音-水电避坑视频"

    @Note("当前状态")
    @Column(nullable = false, length = 16)
    private LeadState state = LeadState.PendingContact;

    @Note("最近跟进时间")
    @Column
    private DateTime lastFollowTime;

    @Note("预计产值")
    @Unit("元")
    @Column
    private Long estimatedValue;

    @Note("签约产值")
    @Unit("元")
    @Column
    private Long signedValue;

    @Note("创建时间")
    @CreateTime
    @Column(nullable = false, updatable = false)
    private DateTime createTime;
}
```

```java
// lead/model/LeadFollow.java —— 跟进记录
@Note("线索跟进记录")
@Model
@Entity
@Table(name = "db_clue_lead_follow")
public class LeadFollow {

    @Note("跟进索引")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long followId;

    @Note("线索")
    @Column(nullable = false)
    private Long leadId;

    @Note("跟进人")
    @Column(nullable = false)
    private Long empId;

    @Note("跟进内容")
    @Column(length = 512)
    private String content;

    @Note("跟进后状态")
    @Column(length = 16)
    private LeadState toState;

    @Note("创建时间")
    @CreateTime
    @Column(nullable = false, updatable = false)
    private DateTime createTime;
}
```

### 3.7 实体关系总览

```
Company (EnvModel) 1 ── n Store
       │1
       │
       n
   Employee 1 ── n Video  1 ── 1 VideoCopy
        │            │1        1 ── 1 VideoStat
        │            │
        │            └── 1 ── n Lead  (线索来源归因)
        │1                 │1
        │                  n
        └──────── LeadFollow
   Template 1 ── n Video / VideoTask
   Employee 1 ── n Asset（素材，供 VideoTask 引用）
```

---

## 4. 仓储设计（JPA Repository）

统一继承框架 `com.th4.edge.framework.data.jpa.repository.JpaRepository<T, ID>`（其本身继承 Spring Data JPA `JpaRepository`，`getEntityClass()` 供框架生成管理界面与查询）。**仓储层只声明接口，不写 MyBatis XML，不写 SQL 字符串**；复杂聚合用框架 `CriteriaQueryBuilder` / `Specification`。

```java
// lead/repository/LeadRepository.java
public interface LeadRepository extends JpaRepository<Lead, Long> {

    List<Lead> findByEmpIdAndState(Long empId, LeadState state);

    List<Lead> findByCompanyIdAndStateIn(Long companyId, Collection<LeadState> states);

    long countByCompanyIdAndState(Long companyId, LeadState state);

    long countByCompanyIdAndCreateTimeBetween(Long companyId, DateTime start, DateTime end);

    List<Lead> findByStateAndLastFollowTimeBefore(LeadState state, DateTime before);
}
```

```java
// video/repository/VideoTaskRepository.java
public interface VideoTaskRepository extends JpaRepository<VideoTask, Long> {

    List<VideoTask> findByStateOrderByCreateTimeAsc(VideoTaskState state);

    List<VideoTask> findByEmpIdAndState(Long empId, VideoTaskState state);
}
```

```java
// video/repository/VideoRepository.java
public interface VideoRepository extends JpaRepository<Video, Long> {

    List<Video> findByEmpIdOrderByCreateTimeDesc(Long empId);

    long countByEmpIdAndCreateTimeBetween(Long empId, DateTime start, DateTime end);

    long countByCompanyIdAndCreateTimeBetween(Long companyId, DateTime start, DateTime end);
}
```

**看板聚合**（按公司/业务员分组统计）：在 Provider 内用框架查询构建器按 `@Projection` + `@Group` 或 `CriteriaQueryBuilder` 聚合，避免手写 JPQL 字符串：

```java
// statistic/context/StatisticQueryService.java
public class StatisticQueryService {

    private final EntityManager em;

    // 月度看板：视频数、播放量、评论数、线索数、量房数、签约数、签约产值
    public MonthlyStatistic monthly(Long companyId, YearMonth month) {
        DateTime start = DateTime.of(month.atDay(1));
        DateTime end = DateTime.of(month.plusMonths(1).atDay(1));
        // 1) VideoStat 聚合（sum view/comment/like）
        // 2) Lead 按状态 count + sum signedValue
        // 3) Employee 活跃数
        // 采用 JPA Criteria 的 groupBy + 多路聚合（示例见 8.1）
        return builder.build(...);
    }
}
```

---

## 5. Provider API 设计（替代 Controller）

### 5.1 机制

- Provider 是 Spring Bean，实现 `com.th4.edge.framework.provider.Provider<Request, Response>`。
- 框架 `HttpProcessor` 在启动时通过 `detectServiceMapping()/match()` 按类型自动注册路由，前端以特定路径访问，**无需 Controller、无需手写 URL 映射**。
- 编程式调用用 `ProviderExecutor.execute(Class, Request)`。
- 请求参数校验用框架 `provider.validation` 注解（`@NotNull/@NotEmpty/@Length` 等），分页/关键字/谓词由 `provider.criteria` 过滤器自动装配。

### 5.2 主要 Provider 清单

| Provider | 说明 | 典型 Request/Response |
| --- | --- | --- |
| `TemplateListProvider` | 5 类模板 + 话题钩子列表（业务员选模板） | → `List<Template>` |
| `AssetUploadProvider` | 上传工地照片（多图，≤5 张） | Multipart → `Asset[]` |
| `VideoGenerateProvider` | 提交生成任务（模板+钩子+图片）→ 异步执行 | `VideoGenerateRequest` → `VideoTask` |
| `VideoTaskQueryProvider` | 轮询任务进度（30 秒出片） | `taskId` → 阶段/预览 |
| `VideoPublishProvider` | 下载 MP4 / 复制文案 | `videoId` → 下载/文案 |
| `LeadRegisterProvider` | 登记线索（自动关联来源视频） | `LeadRegisterRequest` → `Lead` |
| `LeadListProvider` | 我的线索 / 公司全部线索（分页+状态筛选） | 条件 → 分页 |
| `LeadFollowProvider` | 标记跟进（状态流转） | `LeadFollowRequest` → `Lead` |
| `LeadTransferProvider` | 离职一键交接（转给新业务员） | `transfer` → 批量 |
| `DashboardProvider` | 老板本月获客看板（ROI） | `companyId/月` → `DashboardVo` |
| `SalesmanRankProvider` | 业务员排行（发了几条/带几条线索/签约额） | `companyId/月` → 排行 |
| `AgentChatProvider` | 与获客助手对话（AgentScope Harness 入口） | `message/sessionId` → 流式 |

### 5.3 示例：VideoGenerateProvider

```java
// video/provider/VideoGenerateProvider.java
@Note("提交视频生成任务")
public class VideoGenerateProvider implements Provider<VideoGenerateRequest, VideoTask> {

    private final VideoTaskRepository taskRepository;
    private final TemplateRepository templateRepository;
    private final VideoTaskLauncher launcher;

    @Override
    public VideoTask call(VideoGenerateRequest req) {
        // 校验模板与素材归属
        Template template = templateRepository.findById(req.getTempId())
                .orElseThrow(() -> new DefinedException(SystemErrorCode.NOT_FOUND, "模板不存在"));
        if (CollectionUtils.isEmpty(req.getAssetIds()) || req.getAssetIds().size() > template.getImageLimit()) {
            throw new DefinedException(SystemErrorCode.PARAM_ERROR, "图片数量需在 1~" + template.getImageLimit() + " 之间");
        }
        // 创建任务（Created 态），异步启动 AI 流水线
        VideoTask task = new VideoTask();
        task.setEmpId(req.getEmpId());
        task.setTempId(req.getTempId());
        task.setHook(req.getHook());
        task.setAssetIds(JsonUtil.toJson(req.getAssetIds()));
        task.setState(VideoTaskState.Created);
        taskRepository.save(task);

        launcher.launch(task);      // @Async：串行驱动 文案→配音→字幕→合片→尾卡
        return task;
    }
}
```

> Request 校验示例（框架注解）：

```java
public class VideoGenerateRequest {
    @NotNull private Long empId;
    @NotNull private Long tempId;
    @NotEmpty @Length(max = 512) private String hook;
    @NotEmpty private List<Long> assetIds;
    // getters/setters
}
```

---

## 6. 智能体设计：AgentScope Harness + DeepSeek

### 6.1 依赖（pom.xml 追加）

```xml
<properties>
    <agentscope.version>2.0.1</agentscope.version>
</properties>

<dependencies>
    <!-- AgentScope Harness：生产级智能体运行底座（内含 ReActAgent、workspace、memory、compaction） -->
    <dependency>
        <groupId>io.agentscope</groupId>
        <artifactId>agentscope-harness</artifactId>
        <version>${agentscope.version}</version>
    </dependency>
    <!-- DeepSeek 无独立扩展坐标（Maven Central 404），实际实现在 OpenAI 兼容扩展包内：
         io.agentscope.extensions.model.openai.compat.deepseek.DeepSeekModelProvider，
         DeepSeek 开放平台 baseUrl=https://api.deepseek.com -->
    <dependency>
        <groupId>io.agentscope</groupId>
        <artifactId>agentscope-extensions-model-openai</artifactId>
        <version>${agentscope.version}</version>
    </dependency>
</dependencies>
```

> 注：已核实 `agentscope-extensions-model-deepseek` 坐标在 Maven Central 不存在（404），DeepSeek 实现在 `agentscope-extensions-model-openai:2.0.1` 的 `compat.deepseek` 包（`DeepSeekModelProvider`）。接入：`ModelRegistry.resolve("deepseek:<model>")` + `DEEPSEEK_API_KEY`，即 OpenAI 兼容通道：
> `OpenAIChatModel.builder().baseUrl("https://api.deepseek.com").modelName("deepseek-chat").apiKey(...)`（GitHub issue #884 已验证该接入方式）。

### 6.2 模型装配（DeepSeek）

```java
// video/agent/configure/DecoboonAgentConfigure.java
@Configuration
public class DecoboonAgentConfigure {

    @Value("${decoboon.agent.deepseek.api-key}")
    private String apiKey;

    @Value("${decoboon.agent.deepseek.model:deepseek-chat}")
    private String modelName;

    @Value("${decoboon.agent.workspace:./.agentscope/workspace}")
    private String workspacePath;

    @Bean
    public Model decoboonChatModel() {
        // 一等 DeepSeek 供应商
        return DeepSeekChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)     // deepseek-chat / deepseek-reasoner
                .stream(true)
                .build();
        // 备选（OpenAI 兼容通道）：
        // return OpenAIChatModel.builder()
        //         .baseUrl("https://api.deepseek.com")
        //         .modelName("deepseek-chat")
        //         .apiKey(apiKey)
        //         .stream(true)
        //         .build();
    }
}
```

### 6.3 HarnessAgent 装配（Spring 单例 + 多租户会话）

```java
// video/agent/context/DecoboonAgentManager.java
@Component
public class DecoboonAgentManager {

    private final HarnessAgent agent;
    private final AgentToolkit toolkit;   // 业务 Tool（见 6.4）

    public DecoboonAgentManager(Model model, AgentToolkit toolkit, DecoboonWorkspaceInitializer wsInit) {
        Path workspace = wsInit.init();    // 首启生成 AGENTS.md，之后复用
        this.toolkit = toolkit;
        this.agent = HarnessAgent.builder()
                .name("decoboon-get-customer-agent")
                .sysPrompt("你是装得清的装修获客助手……")
                .model(model)
                .workspace(workspace)
                .compaction(CompactionConfig.builder()
                        .triggerMessages(30)
                        .keepMessages(10)
                        .flushBeforeCompact(true)   // 压缩前把关键事实沉淀到每日记忆
                        .build())
                .tools(toolkit.allTools())          // 业务工具注册
                // 生产可加：.distributedBackend(MysqlDistributedBackend.builder()...build())
                .build();
    }

    /** 对话入口：sessionId 持久会话，userId=公司Id 做租户隔离 */
    public Mono<Msg> chat(String userId, String sessionId, String text) {
        RuntimeContext ctx = RuntimeContext.builder()
                .sessionId(sessionId)
                .userId(userId)              // 天然多租户文件/记忆命名空间
                .build();
        Msg msg = Msg.builder().role(MsgRole.USER).textContent(text).build();
        return agent.call(msg, ctx);
    }
}
```

**工作区结构**（`AGENTS.md` 承载智能体人设与知识库）：

```text
.agentscope/workspace/
├── AGENTS.md          # 人设：你是装得清获客助手，遵循"模板要土、直接报价留电话"
├── MEMORY.md          # 长期记忆（后台合并去重）
├── memory/YYYY-MM-DD.md  # 每日事实日志
├── KNOWLEDGE.md       # 装修避坑知识/话术库（RAG 注入）
└── skills/decoboon-script/SKILL.md   # 文案技能（提示词模板）
```

### 6.4 业务 Tool（Agent 与 JPA 的桥）

Agent 通过 `@Tool` 方法访问仓储与业务服务，模型自主决策调用时机（ReAct 循环）：

```java
// video/agent/tool/ScriptTool.java
@Component
public class ScriptTool {

    private final TemplateRepository templateRepository;

    @Tool("根据模板与素材备注生成抖音口播文案+发布文案+话题标签")
    public String generateScript(
            @ToolParam("模板ID") Long tempId,
            @ToolParam("素材备注(JSON)") String assetRemarks,
            @ToolParam("选中的话题钩子") String hook) {
        Template t = templateRepository.findById(tempId).orElse(null);
        // 拼接行业知识 + 钩子 + 图片要素，返回结构化文案（JSON：voice/overlay/hashtags）
        return "...";
    }
}
```

```java
// lead/agent/tool/LeadAnalysisTool.java
@Component
public class LeadAnalysisTool {

    private final LeadRepository leadRepository;

    @Tool("分析某业务员本月线索结构与跟进情况，给出跟进建议")
    public String analyze(String empId, String month) {
        // 读取 JPA 聚合结果，输出人话结论，供老板/业务员查看
        return "...";
    }
}
```

> 工具清单：`TemplateTool`（选模板/钩子）、`ScriptTool`（文案生成）、`LeadAnalysisTool`（线索归因/跟进建议）、`FollowRemindTool`（生成提醒文案）、`StatisticTool`（看板解读）。

### 6.5 异步生成流水线（Agent 与确定性编排结合）

AI 出片要"快、稳、可重试"，不把整条流水线交给模型自由发挥，采用**确定性状态机编排 + Agent 只产出高价值内容**：

```
VideoTask(Created)
   │  VideoTaskLauncher.launch()  @Async
   ▼
Writing ──▶ Agent.generateScript()  → 口播文案/字幕文本/发布文案/hashtags
   ▼
Dubbing ──▶ TtsService（调用 TTS 供应商）→ 配音 mp3
   ▼
Subtitling ──▶ SubtitleService（按时间轴生成字幕轨，黄色高亮关键词）
   ▼
Rendering ──▶ VideoRenderService（FFmpeg：图片→视频 + 配音 + 字幕 + BGM）
   ▼
EndCaring ──▶ 叠加公司 Logo + 电话尾卡
   ▼
Done ──▶ Video 落库 + VideoCopy 落库 + 触发线索登记提示
```

```java
// video/listener/VideoTaskStateListener.java —— 每完成一阶段，JPA 事件驱动下一步
@Component
public class VideoTaskStateListener {

    @EventListener
    @Async
    public void onStateChange(VideoTaskStateChangeEvent evt) {
        VideoTask task = evt.getTask();
        switch (task.getState()) {
            case Created  -> launcher.writeScript(task);   // 调用 Agent
            case Writing  -> launcher.dub(task);
            case Dubbing  -> launcher.subtitle(task);
            case Subtitling -> launcher.render(task);      // FFmpeg
            case Rendering  -> launcher.endCard(task);
            case EndCaring  -> launcher.finish(task);      // 生成 Video+Copy
            default -> { /* Failed: 记录 failReason + 告警 */ }
        }
    }
}
```

> 30 秒出片目标：文案（DeepSeek ~3-5s）+ TTS + FFmpeg 合片是主要耗时，全链路异步 + 前端轮询 `VideoTaskQueryProvider`。

---

## 7. 线索工作流与提醒

```
待联系 ──▶ 待量房 ──▶ 已量房 ──▶ 已签约
   │          │         │
   └──────────┴─────────┴──▶ 已流失
```

- **登记即归因**：`LeadRegisterProvider` 保存时自动带 `videoId`（来自"来源视频"），实现"每条视频带来多少线索"。
- **跟进提醒**：`LeadPostPersistListener` 在保存后发送微信/站内提醒；定时任务（`@Scheduled`）扫描 `state=待联系 && lastFollowTime < 3天前`，提醒老板（对应场景"小张有 3 条线索登记了但没跟进"）。
- **离职交接**：`LeadTransferProvider` 批量把 `empId` 改为新业务员，客户资产不丢失。

---

## 8. 看板统计（ROI 一目了然）

### 8.1 老板本月看板指标

| 指标 | 计算 |
| --- | --- |
| 视频发布总数 | `Video.countByCompanyIdAndCreateTimeBetween` |
| 总播放量/评论数 | `VideoStat` sum（按月归因） |
| 登记线索数 | `Lead.countByCompanyIdAndCreateTimeBetween` |
| 已量房/已签约 | `Lead.countByCompanyIdAndState` |
| 签约产值 | `Lead` sum(signedValue) where state=Signed |
| 工具成本 | 公司 `planCode` 对应月费（如 299） |
| **单客获客成本** | 工具成本 ÷ 签约数（对比土巴兔 3000 元） |

```java
// statistic/provider/DashboardProvider.java
public class DashboardProvider implements Provider<DashboardRequest, DashboardVo> {

    @Override
    public DashboardVo call(DashboardRequest req) {
        YearMonth m = req.getMonth();
        // 用 EntityManager Criteria API 一次聚合视频/线索/产值，避免多次查库
        CriteriaQueryBuilder qb = new CriteriaQueryBuilder(em, List.of());
        // ... group by 公司/业务员，投影 sum/count ...
        return DashboardVo.builder()
                .videoCount(...).viewCount(...).leadCount(...)
                .measuredCount(...).signedCount(...).signedValue(...)
                .toolCost(company.getPlanCode()).unitCost(...)
                .build();
    }
}
```

### 8.2 业务员排行（激励与提醒）

按 `empId` 分组：发视频数、线索数、量房数、签约数、签约产值，排序输出——支撑"小李 12 条/8 线索，小王 15 条/2 签约应给提成，小张 3 条/3 天未跟进需谈话"。

---

## 9. 配置汇总

### 9.1 pom.xml 关键追加

```xml
<dependency>io.agentscope:agentscope-harness:2.0.1</dependency>
<dependency>io.agentscope:agentscope-extensions-model-openai:2.0.1</dependency><!-- DeepSeek 实现在 openai 扩展的 compat.deepseek 包 -->
<dependency>org.springframework.boot:spring-boot-starter-data-jpa</dependency>
<!-- 复用 th4-admin 传递依赖；不引入 mybatis-spring-boot-starter -->
```

### 9.2 application-dev.yml 追加

```yaml
spring:
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: update            # JPA 自动建表，实体即 DDL
    database: mysql

decoboon:
  agent:
    deepseek:
      api-key: ${DEEPSEEK_API_KEY:sk-xxx}
      model: deepseek-chat        # 或 deepseek-reasoner
    workspace: ./data/agentscope/workspace
    tts:
      provider: volcengine        # 配音供应商（预留可切换）
    render:
      ffmpeg-bin: /usr/local/bin/ffmpeg
      bgm-dir: ./data/bgm
```

---

## 10. 数据库表清单（JPA 自动建表）

| 表 | 说明 | 关键字段 |
| --- | --- | --- |
| `db_clue_company` | 公司（租户 Env） | companyId, name, city, planCode |
| `db_clue_store` | 门店 | storeId, companyId, name |
| `db_clue_employee` | 业务员 | empId, companyId, name, phone, wechat |
| `db_clue_template` | 内容模板（5 类） | tempId, type, name, hooks, scriptPrompt |
| `db_clue_asset` | 上传素材 | assetId, empId, type, url, remark |
| `db_clue_video` | AI 生成视频 | videoId, empId, tempId, url, title, state |
| `db_clue_video_copy` | 发布文案 | copyId, videoId, content, hashtags |
| `db_clue_video_stat` | 视频数据 | statId, videoId, viewCount, leadCount |
| `db_clue_video_task` | 生成任务状态机 | taskId, videoId, state, detail, failReason |
| `db_clue_lead` | 线索 | leadId, empId, companyId, videoId, state, signedValue |
| `db_clue_lead_follow` | 跟进记录 | followId, leadId, empId, toState |

---

## 11. 实施路线（建议顺序）

| 阶段 | 内容 | 验收 |
| --- | --- | --- |
| P0 数据底座 | Company/Employee/Template/Asset/Video/Lead 实体 + Repository，`@DataPreset` 预置 5 模板 | 启动自动建表，管理端可维护 |
| P1 视频生成 | VideoTask 状态机 + `VideoGenerateProvider` + Agent 文案 Tool | 30 秒异步出片（先 Mock TTS/FFmpeg） |
| P2 线索闭环 | 线索登记/列表/跟进/交接 Provider + 提醒定时任务 | 登记自动关联来源视频 |
| P3 看板 | Dashboard/Rank Provider + 月度聚合 | ROI 一眼可见 |
| P4 Agent 增强 | 对话式获客助手（`AgentChatProvider`）+ 长期记忆 + 流式 | DeepSeek 对话可用、租户隔离 |

### 11.1 P0 落盘说明（2026-09-03 已执行并验证）

- **落盘范围**：`src/main/java/com/th4/edge/*` 已生成 6 枚举 + 11 实体 + 11 仓储 + 5 Provider（TemplateList / LeadRegister / LeadList / Dashboard / SalesmanRank）。
- **主键策略**：设计稿原用 `@SnowflakeId`，实测框架处理器仅对 String 生成且默认未启用 → **全部改为 `@GeneratedValue(strategy = GenerationType.IDENTITY)`**（MySQL 自增，且被 `@DataPreset` 解析器自动跳过）。
- **DateTime 映射**：框架 `DateTime` 为 `@Embeddable`，同一实体多时间字段（create/update/follow）必须 `@Embedded + @AttributeOverrides` 逐子字段指定独立列名，否则报 `Column 'date' is duplicated`。
- **多租户 P0 决策**：Company 采用 `AbstractModel + 显式 companyId 列`（Employee/Lead/Store 均带 companyId 外键列，查询按 companyId 过滤），暂不启用 `EnvModel` 租户解析；P3 看板可平滑升级。
- **验证结果**：`mvn compile` 通过；启动 `Started Application in 5.4s`，Hibernate `ddl-auto:update` 自动创建 11 张 `db_clue_*` 表（InnoDB）；`@DataPreset` 预置 5 类模板入库（tempId 1-5，enabled=1）；`POST /api/video/template-list` 返回 200 + 5 模板 JSON。

### 11.2 P1 视频生成流水线落地说明（2026-09-03 已执行并验证）

- **新增代码（`video/` 下）**：
  - `ai/AiScriptWriter`：AgentScope `HarnessAgent` + DeepSeek 文案生成器。`ModelRegistry.resolve("deepseek:deepseek-chat", ModelCreationContext(apiKey,baseUrl))` 装配模型；`Toolkit.registerTool(SaveVideoScriptTool)` 挂工具；`agent.call(Msg).block(90s)` 一次对话产出文案。Key 取 `th4.edge.decoboon.ai.deepseek.api-key` → 环境变量 `DEEPSEEK_API_KEY` → 都缺时降级本地 Mock，保证流水线不阻断。返回 `record ScriptResult(content,hashtags,locationTip)`。
  - `ai/SaveVideoScriptTool`：`@Tool(name="save_video_script")`，Agent 生成后把文案落盘 `data/agent-drafts/task-{taskId}.md`（Mock 路径同样经工具落盘，保持产物一致）。
  - `pipeline/VideoPipelineService`：异步状态机 `Created→Writing(DeepSeek文案)→Dubbing(系统TTS)→Subtitling(SRT)→Rendering(FFmpeg)→EndCaring(尾卡)→Done`；每阶段产物写入 `VideoTask.detail`(JSON)；任一步异常 → `Failed` + `retryCount++`；`@Scheduled(fixedDelay=30s)` 扫描 `Failed(retryCount<3)` 从 `detail.lastState` 断点续跑。
  - `provider/VideoGenerateProvider`：`@Service("video/generate")` 生成入口，无 Controller。创建 Video(Draft) + VideoTask(Created) 后 `@Async` 触发流水线。
  - `media/*`（P1.5 真实媒体层，2026-09-03 已接入；TTS 已升级阿里云，见下）：
    - `TtsService`（接口 `generate(text, taskId)` → `TtsResult(audioPath, durationSec)`）+ `RoutingTtsService`（@Primary 路由，按 `video.tts.provider` 选主实现，阿里云失败自动降级系统 TTS，流水线不阻断）+ `MacSystemTts`（macOS `say -v Tingting` → aiff → ffmpeg 转 mp3，免凭证兜底）。
    - `AliyunNlsTts`（阿里云智能语音交互语音合成，2026-09-03 接入）：**纯 JDK 实现，零额外 SDK 依赖**。鉴权：appkey（管控台项目）+ token（阿里云 POP `CreateToken` 用 AccessKeyId+Secret 换得，HMAC-SHA1 签名规范逐字节对齐官方文档，token 缓存至过期前 10 分钟）；合成：POST `nls-gateway-{region}.aliyuncs.com/stream/v1/tts`，JSON body（appkey/token/text/format=wav/sample_rate/voice），返回 audio/wav 二进制；文本 >250 字自动按句分块逐条合成，用 ffmpeg concat demuxer 拼接后转 mp3(44.1k)。配置：`th4.edge.decoboon.video.tts.provider=aliyun` + `aliyun.appkey/access-key-id/access-key-secret/token/voice/sample-rate/region`。**踩坑**：token 服务域名是 `nls-meta.{region}.aliyuncs.com`（点分隔），误写连字符 `nls-meta-{region}` 会 DNS 解析失败；网关域名是 `nls-gateway-{region}.aliyuncs.com`（连字符）。**已实测（任务10，2026-09-03）**：CreateToken 换 token 成功（有效期 24h）→ aixia 音色真实合成 40.29s → 全链路 Done，final.mp4 43.6s。
    - `SubtitleGenerator`：把文案按句切条、按约 3 秒/条合并、按配音时长等比分配时间轴，生成标准 SRT；提供 `parseSrt` 回读 cue 列表（毫秒时间窗）供硬字幕复用。
    - `FfmpegRenderService`：**真实 FFmpeg 合片**。renderMain：素材图 `scale+crop` 到 720x1280 → 按配音时长均分每图时长 → concat 轮播 → `-shortest` 对齐配音 → mux `mov_text` 中文字幕轨；**burnSubtitles（硬字幕烧录）**：本机 ffmpeg 无 libass（无 drawtext/subtitles filter），改用 `SubtitleBannerGenerator` 生成透明白字黑边字幕条 PNG，overlay 按 `enable='between(t,start,end)'` 时间窗叠加进画面像素——任何播放器/平台直接可见，且对每个字幕条输入用 `-itsoffset/-t/-framerate` 限时生成帧，50s 成片烧录仅约 1.3s；appendEndCard：尾卡 3s 片段（带静音音轨）`concat filter` 重编码拼接，`-map 0:s? -c:s mov_text` 保留字幕轨。
    - `EndCardGenerator`：Java AWT 直接绘制尾卡 PNG（深色底 + 公司名 + 电话 + slogan），无需 drawtext。
    - `AssetImageResolver`：按 assetIds 解析素材图片（Asset 表 url 为本地文件时直用）；素材缺失时生成占位工地图，保证不阻断。**2026-09-03 改进**：assetIds 可能为 `"1,2,3"` 或 JSON 数组 `"[1, 2, 3]"`，原按逗号切分会被方括号干扰丢素材，改为正则 `\\d+` 提取所有 ID，5 张全解析实测通过；同日将用户 12 张工地照片复制至 `data/media/assets/site-01..12.jpg` 并入库（emp=1, type=Image, url=本地绝对路径）。
- **请求参数读取（框架实测结论）**：`ApiHttpProcessor` 调用 `provider.call(null)`，但 `ProviderAspect` 会先跑 `RequestFilter` 链——其中 `ProviderFieldInjectRequestFilter` 会按 `call` 方法首参类型**实例化一个空 Request** 传入；JSON 请求体由 `ApplicationJsonHttpServletRequestDecoder` 展开进 `RequestContext.variables`。因此自定义 Provider 取参应「合并读取」：`request` 字段为空时再 `RequestContextHolder.getRequestContext().lookupVariable(key)` 兜底。
- **依赖坐标**：`io.agentscope:agentscope-harness:2.0.1` + `io.agentscope:agentscope-extensions-model-openai:2.0.1`（DeepSeek 实现在 openai 扩展的 `compat.deepseek` 包，`agentscope-extensions-model-deepseek` 坐标不存在）。
- **验证结果（端到端实测，已配 DeepSeek Key）**：`mvn compile` 通过；任务5 全链路：DeepSeek 在线文案（干净正文，无模型前缀）→ TTS 49.5s → 35 条 SRT → FFmpeg 主片 → 尾卡 → `final.mp4`（720x1280 h264+aac+**mov_text 字幕轨**，52.4s），DB `VideoTask.state=Done`、`Video.url/duration/state=Published`、`VideoCopy` 落文案，Agent 草稿 `data/agent-drafts/task-5.md` 落盘。任务3 曾验证失败重试：尾卡 concat 首版缺音轨 → Failed+retryCount 递增，修复后全通。文案产物路径写入 `task.detail`：script/audio/subtitle/render/endcard/final。

---

## L1 电影感剪辑（运镜 + 卡点 + 音效，2026-09-03 落地）

**目标**：把"静态图轮播"升级为有动态感/节奏的成片，弥合与人工短视频的差距（动态感、剪辑节奏、声音层次）。

**实现位置**：`FfmpegRenderService.renderMain` 按 `video.l1.enabled` 分流 → `renderMainCinematic`，流水线调用点（`VideoPipelineService.rendering`）零改动。产物仍走 `render_v.mp4 → render_a.m4a → render.mp4 → render_burn.mp4 → final.mp4` 链路。

**四项能力**：

1. **zoompan 运镜（Ken Burns）**：`buildVideoOnly` 每张图 `scale=1080:1920:force_original_aspect_ratio=increase,crop=1080:1920` → 偶数图推近 `z='min(zoom+0.0006,1.15)'`、奇数图拉远 `z='if(eq(on,1),1.15,max(zoom-0.0006,1.0))'`，`d=round(段秒×25)` 帧、`s=720x1280:fps=25`；每段 `settb=AVTB,setpts=PTS-STARTPTS` 归一化后 concat。`ken-burns=false` 时退化为原静态裁切。
2. **fade 转场**：每段 `fade=t=in:st=0:d={fade}` + `fade=t=out:st={段末-fade}:d={fade}`，段间视觉有呼吸感。
3. **BGM 卡点切镜**：新增 `BeatDetector`（ffmpeg 解码 16k 单声道 PCM → 40ms 窗短时能量包络 → 局部极大值 onset 检测，无第三方音频库）。`snappedDurations` 先把"配音时长均分"的段边界吸附到最近拍子（容差 0.7s），再做单调性/最小时长钳制（0.8s/段），实现"切镜踩在拍子上"。
4. **混音与音效**：`buildMixedAudio` = 人声 + BGM（`sidechaincompress` 以人声为侧链自动避让 ducking）+ 开场钩子音效（880Hz 衰减"叮"，`hook-delay-ms=350`）+ 每个切点 tick（1800Hz 短促点击），逐级 `amix=inputs=N:duration=first:normalize=0` 保持响度不归一。BGM 无配置时自动生成 100BPM 演示配乐（底鼓+弱拍嚓+低音铺底，`data/media/bgm/demo-tension.m4a`），用户可把 `video.l1.bgm.path` 指向自己的 mp3/m4a。

**配置（`th4.edge.decoboon.video.l1.*`）**：`enabled/ken-burns/fade/bgm.{enabled,path,volume}/sfx.{enabled,hook-delay-ms,hook-volume,tick-volume}`。

**踩坑**：`[vo][bgd][f0]amix=inputs=2` 输入标签 3 个但 inputs=2 → 报 `More input link labels...`（退出码 234）；amix 的 inputs 必须等于实际输入标签数（有 BGM 时首级为 3，后续恒为 2），已修复。

**端到端实测（任务14，2026-09-03）**：5 张真实水电施工照片 + zhixiaobai 配音 41.09s → BeatDetector 检出 24 拍 → L1 电影感主片（BGM=on 卡点=on 运镜=on）→ 14 条硬字幕烧录 → 尾卡 → `final.mp4`（720x1280 h264+aac 44.4s，4.2MB）。验证：`ffprobe` 三流正常；`freezedetect` 全片无 freeze（运镜/转场持续运动）；抽帧确认硬字幕逐条变化、尾卡正常。

---

## L2 素材增强（竖版画布 + 调色 + 锐化，2026-09-03 落地）

**目标**：让进入合片的素材图更"精"——解决混合比例素材在竖版视频里被裁边、小图放大发糊、观感不统一三个问题。

**实现**：新增 `AssetEnhancerService`，在 `VideoPipelineService.rendering` 的 `resolveImages` 之后、进渲染前对每张图增强，产物写 `data/media/task-{taskId}/enhanced/`（幂等，同名跳过）。流水线只加一行调用，`video.l2.enabled=false` 即回退原图。

**三项能力**：

1. **竖版画布统一（1080x1920）**：用 `ImageIO` 探测原图比例——接近 9:16 走 `scale+cover crop` 裁满；其他比例（3:4 等）走**模糊背景补边**：背景 = 原图铺满画布 + `gblur=sigma=25` + 压暗，前景 = 原图完整缩放居中 overlay，主体不丢内容、两侧模糊延伸，与主流剪辑 App 的竖版化同思路。
2. **工地实拍风格调色**：`eq=contrast=1.10:saturation=1.18:brightness=0.02` + 暖色 `colorbalance=rs=0.02:bs=-0.02` + 轻暗角 `vignette=PI/6`，统一观感、贴近口播短视频的明快真实感。
3. **锐化增强**：`unsharp=5:5:0.40` 轻锐，弥补 810x1440 等小图放大到 1080 的发糊。

**超分扩展点**：`video.l2.super-resolution.provider=none`（确定性增强即可交付）；`provider=realesrgan` 且配置 `binary`（如 `/opt/homebrew/bin/realesrgan-ncnn-vulkan`）时先外部 CLI 2x 超分再竖版化，失败自动降级直接增强。本机当前无该 CLI，属可选升级项。

**配置（`th4.edge.decoboon.video.l2.*`）**：`enabled/canvas-width/canvas-height/contrast/saturation/brightness/warm/sharpen/vignette/super-resolution.{provider,binary,scale}`。

**端到端实测（任务15，2026-09-03）**：5 张混合比例素材（9:16 的 site-02/04 + 3:4 的 site-09/10/12）→ 5 张 1080x1920 增强图（3:4 全部模糊背景补边，主体完整）→ L1 电影感渲染 → 14 条硬字幕 → 尾卡 → `final.mp4`（720x1280 h264+aac 44.8s，5.4MB）。验证：`ffprobe` 三流正常；`freezedetect` 无 freeze（L1 运镜未受影响）；抽帧确认 3:4 素材主体完整居中 + 调色增锐进画面 + 硬字幕逐条变化。

---

## L2.5 素材-文案语义对齐（A1 视觉识别 + B1 文案对齐 + C1 一致性校验，2026-09-03 落地）

**根因**：原 `AiScriptWriter.buildUserPrompt` 只把 `assetIds`（数字 ID）丢给 DeepSeek，模型看不到画面，文案与素材内容必然脱节（观感"内容不符合"）。

**方案**（对应技术路线 A→B→C）：

1. **A1 素材语义化（新增 `DashScopeVisionService`）**：调阿里云百炼通义千问 VL（qwen-vl-plus，OpenAI 兼容接口）识别素材图，输出"画面内容 + 可讲卖点"。纯 JDK HTTP + Jackson，图片 base64 data URL 直传（无需 OSS）。配置 `ai.vision.api-key/model/base-url`，Key 缺失降级文件名/备注。
2. **A2 描述入库（新增 `AssetTaggerService` + `Asset.description` 字段）**：识别结果写回 `db_clue_asset.description`（ddl-auto 自动加列），幂等复用（description 非空即跳过，避免重复烧额度）；视觉失败降级 remark/文件名，出片不阻断。
3. **B1 文案对齐（改造 `AiScriptWriter`）**：`writeScript` 先 `describeAssets` 取素材描述清单（按播放顺序），注入 `buildUserPrompt`，并要求"每句话必须对应某张素材画面、严禁编造画面里没有的东西"。
4. **C1 一致性校验（内置轻量关键词覆盖）**：预置装修词库（水电/线管/强弱电/防水/贴砖/毛坯/飘窗/封口/水泥…），从素材描述抽关键词，校验文案覆盖数 ≥ max(2, min(3,素材数))；不过则携带素材关键词重写一次。仅当存在真实视觉描述（含"可讲卖点"）时强制，避免降级描述误触发。

**端到端实测（任务16，2026-09-03）**：5 张素材全部 qwen-vl 识别成功（单张 95~117 字，耗时约 1.3s/张）→ 描述入库 → DeepSeek 文案 166 字，C1 命中 4/15 关键词通过 → zhixiaobai 配音 35.36s → 12 条硬字幕 → L1 电影感 → 尾卡 → `final.mp4`（720x1280 38.6s）。**实证**：文案"一看线管齐不齐↔asset2 线管铺设整齐；二看绿胶带封口↔asset9/10 管道口绿胶带封住；三看水管接头水泥封堵↔asset12 水泥封堵"，抽帧确认字幕与画面一一对应（t=9 绿套封管+「二看管子封口」、t=16 绿盖管道+「下水准出幺蛾子」）。

---

## L2.6 分镜标题·花字（屏幕大字与画面对应，2026-09-03 落地）

**背景**：上一轮"文案对得上画面"解决底部口播字幕，但抖音信息流习惯"大字标题速读"——用户提出两个方向（L3 AI 视频融合动效 / 分镜标题花字），本轮优先落地**花字**（零依赖、可立即验证，直接延伸"屏幕大字与画面对应"）。

**方案**：
1. **A1 扩展（改 `DashScopeVisionService`）**：qwen-vl 识别 prompt 追加"花字：XXX"（≤8 字短促有力分镜标题关键词，如"线管走墙角 / 封口要严实 / 接头要实"），随描述一次返回，不增加调用次数。
2. **A2 解析入库（改 `AssetTaggerService`）**：`AssetDesc` 增加 `tagline` 字段；`parseTagline()` 从描述中解析"花字：xxx"行（去行尾标点、去中文引号、超 8 字截断）。复用/新识别两分支都填 tagline，幂等不变。
3. **B1 传递（改 `AiScriptWriter`）**：`ScriptResult` 增加 `taglines: List<String>`，从素材描述清单按播放顺序收集；DeepSeek 与 Mock 分支都携带，写入 `task.detail.taglines`。
4. **渲染（新增 `TaglineBannerGenerator` + 改 `FfmpegRenderService`）**：为每张素材生成 720x220 大号花字条（PingFang SC Bold 92px 白字 + 8 向黑色粗描边 + 半透明深色圆角底块，保证任何背景可读）；复用 burnSubtitles 同款 overlay 时间窗机制（`-loop 1 -framerate 25 -itsoffset/-t` 限时输入 + `overlay=0:{y}:enable='between(t,s,e)'`）叠加到画面**中上部 y=140**（区别于底部字幕 y=1120），时间窗 = `snappedDurations` 每段素材时长。产物 `render_tag.mp4` → mux 音轨+字幕 → 后续硬字幕/尾卡链路不变。配置 `video.tagline.enabled/y`。

**踩坑**：`render_v.mp4` 是纯视频（无音轨），buildTaglines 用 `-map 0:a` 报"Stream map matches no streams"（退出码 234）→ 去掉音轨映射（音轨由 render_a.m4a 在 mux 阶段提供）。

**端到端实测（任务19，2026-09-03）**：5 素材重识别（含花字行）→ 花字 5 张（线管走墙角/封口要严实/接头要实…）→ DeepSeek 文案 → zhixiaobai 56.2s → L1 电影感（花字=on(5)）→ 19 条硬字幕 → 尾卡 → `final.mp4`（720x1280 59.5s）。**实证**：t=3 帧花字"线管规整防水到位"↔线管+防水画面、t=12 帧花字"封口要严实"↔绿胶带封口管道，花字（上部）+口播字幕（底部）双图层并存、各司其职。任务17 曾遇 overlay 失败（`-map 0:a` 无音轨）→ 修复后任务 18/19 全链路 Done。

---

## L3 AI 视频融合动效（通义万相图生视频，2026-09-03 落地）

**目标**：把"静态照片 + zoompan 运镜"升级为"AI 生成的真实动态画面"——每张素材图用通义万相图生视频（wan2.6-i2v-flash）生成一段缓慢推近的动态短片，让素材"活"起来，缩小与人工拍摄视频的观感差距。

**方案**：
1. **新增 `DashScopeI2VService`（纯 JDK）**：图片经 ffmpeg 缩至 720 宽 → base64 data URL 直传（wan2.6 检查器不接受 file://，已验证）→ `POST /api/v1/services/aigc/video-generation/video-synthesis`（`X-DashScope-Async: enable`）异步提交 → `GET /api/v1/tasks/{id}` 每 5s 轮询至 SUCCEEDED（单段最长 7 分钟）→ 下载 `video_url` 落 `task-{id}/i2v/seg-{idx}.mp4`。产物幂等（已存在即复用，支持断点续跑）。Key 缺失自动降级回 L1 运镜。
2. **渲染改造（改 `FfmpegRenderService`）**：`renderMain` 新增 L3 分支（`l3.enabled && i2v.isEnabled()`）→ `renderMainAi`：逐素材 I2V → 每段重编码 720x1280/25fps/h264 无音轨、时长对齐 `snappedDurations`（不足慢放 `setpts`、超则 `-t` 截断）→ concat demuxer 无缝拼接 → 替代 `render_v.mp4`，后续花字/音轨/字幕/尾卡链路完全复用。motion prompt 复用素材语义描述（`buildMotionPrompt`，截去"可讲卖点"）。
3. **配置 `video.l3.*`**：enabled / api-key（留空回退 `ai.vision.api-key`）/ model=wan2.6-i2v-flash / resolution=720P（wan2.6 仅 720P/1080P，480P 报错）/ duration=5 / poll-interval-ms / poll-timeout-ms。

**踩坑**：① `file://` 图经 wan2.6 检查报 `No connection adapters` → 改 base64 data URL；② `wan2.1-i2v-turbo`/`wan2.1-i2v-plus` 报 `Model not exist`（该 Key 不可用）→ 实测 `wan2.6-i2v-flash` 可用；③ `resolution=480P` 报 `not supported, available ['720P','1080P']` → 改 720P；④ `l3.api-key` 为空串时 `@Value` 不触发默认回退 → 构造器运行时 `hasText(l3ApiKey)?l3ApiKey:visionApiKey`。

**端到端实测（任务21，2026-09-03）**：2 素材（asset2 线管 + asset9 绿胶带封口）→ 2 段 I2V 各约 70s 生成（seg-0 8.1MB / seg-1 10.7MB，720x1280 30fps 5s 竖版）→ 拼接 + 花字(2) + 卡点 BGM + 17 条硬字幕 + 尾卡 → `final.mp4`（51.9s）。**实证**：连续帧像素差异确认 AI 动态（非静态运镜）；t=30 帧白管绿胶带封口画面 ↔ 花字「封口要严实」↔ 字幕「白管顶上都拿绿胶带封严」一一对应。**成本**：wan2.6-i2v-flash 约 0.1 元/秒，2 段 5s ≈ 1 元/条；5 素材全量约 2.5 元/条。**已知观感点**：素材少 + 配音长时单段需慢放 2~5 倍，运动偏"缓"；可调 duration=10 或增加素材数改善（成本同步上升）。

### L3.1 文案泄漏清洗（DeepSeek 重写指令残留防护，2026-09-03 补）

**问题**：C1 一致性校验触发"重写"分支时，DeepSeek 偶发把**重写提示词本身**写进正文（如"现在素材补全为5个了，按5个画面写出新一版…"、"按一套新的叙事结构…写第五版"、"已给出一版…切入的版本，句式与措辞全新重写"），污染成片开头 3~10 秒字幕/配音。

**根因**：提示词泄漏的**措辞无限多变**，逐条枚举正则治标不治本。

**方案（改 `AiScriptWriter.cleanContent`）**：双层防线——① `LEAK_LITERALS` 字面删除已知整段残片；② **META_CLAUSE 元叙述检测**：循环剥离正文开头"写作元叙述分句"（含 版本/切入/角度/句式/措辞/重写/覆盖/画面/素材补全/写出/上一版/补全/纯正文/结尾引导/严禁/只输出/围绕/提到或对应/第X版 等写作行为词的分句，最多 5 句）。判据：真实口播正文是"直接面向观众说话"，几乎不以写作元词开头；模型泄漏的重写指令则高频命中。单测 5 CASE：三种不同措辞泄漏全部剥离干净、正常正文（"水电做完别急着让师傅收工…"等）零误删。

**端到端实测（任务25，2026-09-03）**：5 素材 I2V 段复用（0 额外成本）→ DeepSeek 文案 207 字（一致性命中 5/15，本次未触发重写）→ L3 主片 44.8s → `final.mp4`（48.0s）。字幕泄漏检测 0，文案"这水电活到底值不值那个工钱，验收光看干得漂不漂亮没用，得抠细节…"逐句对应画面；t=16 帧竖管绿盖水泥封堵 ↔ 花字「封口要严实」↔ 字幕「再来就蹲到地上看水管」实证一致。

### L3.2 三优化落地（2026-09-03）：duration=10 / 并行 I2V / 素材 AI 筛选

**① 慢放观感（`video.l3.duration=10` + `motion` 档位）**：段长 5s→10s（wan2.6 支持 5/10，成本 2x）。此前 5s 段 + 40s 配音需慢放 2~5 倍观感偏"缓"；10s 段 + 匹配字数文案（主片≈5×10s）基本 1:1 不慢放。`video.l3.motion` 支持 subtle/normal/active 三档运镜 prompt（`DashScopeI2VService.buildMotionPrompt` 按档位 + 素材要点生成）。配套 `AiScriptWriter.buildUserPrompt` **动态字数约束**：`maxWords=(素材数×duration-8)×4.5`，从源头杜绝文案过长导致的强制慢放。

**② 并行 I2V（`FfmpegRenderService.buildAiVideo`）**：固定线程池（`I2V_PARALLEL=3`）并行提交全段，完成一段即补提下一段（滑动窗口）。实测 5 段 10s 全生成 **7.3 分钟**（vs 串行约 19 分钟，**提速 ~2.6x**）；I2V 段产物幂等（`seg-{i}.mp4` 存在即复用），断点续跑/复用历史段 0 成本。

**③ 素材 AI 筛选（`AssetPickerService`）**：DeepSeek 一次性审阅全部素材描述，按"出片价值"挑 top N 并排序（`video.l3.pick.enabled/top-n/min`），子集驱动文案/花字/渲染（`ScriptResult.pickedAssetIds` → detail → rendering 用子集图片），控 I2V 成本。保底：素材过少/LLM 失败/挑太少 → 全量回退，绝不阻断。实测 5 张图被全量保留并重排优先级 `[9,4,2,12,10]`。

**④ 修复：detail 断点续跑类型**：`pickedAssetIds` 经 JSON 落库读回变 `List<Integer>`，续跑 cast `List<Long>` 抛 ClassCastException → 新增 `toLongList` 兼容转换。

**⑤ 提示词泄漏清洗升级（引号检测）**：任务26 又现变体"用'验收顺序'当抓手，从进门到蹲地排出'三步检查'"——核心信号是**开头分句用中文引号引用写作策略**（真实口播不会）。`META_CLAUSE` 词表扩为 版本/切入/角度/句式/措辞/重写/覆盖/画面/素材补全/写出/上一版/补全/纯正文/结尾引导/严禁/只输出/围绕/提到或对应/第X版/抓手/叙事/引入/不重/前面几版/验收顺序/三步检查 + **成对引号分句**。单测 5 CASE（含引号变体）全过、正常正文零误删。任务28/29 字幕泄漏检测 0。

**⑥ 已知运维事项（重要）**：阿里云百炼账户在 task-26（5 段 10s ≈ 5 元）后**欠费**（HTTP 400 `Arrearage`），新 I2V 提交被拒；**复用已有 I2V 段仍可纯离线出片**（task-27~29 全部复用 task-26 段，0 成本）。恢复新段生成需在百炼控制台充值。

**端到端实测（任务29，2026-09-03 终版）**：5 素材 I2V 段复用 → Picker 5→5 重排 → 文案 257 字（一致性 4/15 通过，无泄漏）→ L3 主片 53.2s（≈5×10s 不慢放）→ final.mp4（56.4s，19.2MB）。t=14 帧红线管工地 ↔ 花字「封口要严实」↔ 字幕「那道绿防水刷满刷透」对应；字幕泄漏检测 0。

### L3.3 按图缓存：API 调用结果沉淀复用（2026-09-03）

**动机**：业务场景同一素材图会被多次使用（多任务/多模板/多文案），视觉识别（qwen-vl）与图生视频（I2V）都是**按调用计费**的高成本 API。目标是**同图结果只付一次费，全项目复用**。

**① 视觉识别缓存（已具备，核实）**：`AssetTaggerService.describeAssets` 幂等——识别结果写 `Asset.description`（含"可讲卖点/花字"），非空即复用，不重复调 qwen-vl；tagline 从缓存描述解析。未配 Key/识别失败降级文件名备注，不阻断。

**② I2V 按图缓存（新增，`DashScopeI2VService`）**：三级复用链——任务内段 `task-{id}/i2v/seg-{i}.mp4`（断点续跑）→ **按图缓存** `data/media/i2v-cache/{图片MD5}/{promptMD5}.mp4` → 调 API（生成后**先写缓存**再复制任务目录）。缓存 key = 增强后图片文件内容 MD5 + motion prompt MD5：同图同 prompt 跨任务自动命中，零重复烧钱；不同运动档位/画面（prompt 或图片变化）自动换 key 不误用。

**③ 存量迁移（预热）**：脚本按与 Java 一致的 prompt 复算逻辑，把 task-26 已生成的 5 段 10s I2V 迁入缓存（`i2v-cache/{imgMd5}/{promptMd5}.mp4`），实现"已有资产即刻复用"。

**端到端实测（任务30/31，2026-09-03）**：触发新任务**不预复制任何段** → 5 段全部日志「按图缓存命中」（字节数与源段一致，**0 次 I2V API 调用**）→ 纯离线出片 Done。**账户欠费下仍可出片**（复用历史段），新段生成需充值后首跑自动入缓存。任务31 成片 76.2s：字幕泄漏检测 0，t=12 帧绿盖白管 ↔ 花字「线管规整，防水到位」↔ 字幕「蹲下来，那根绿盖顶的白管」对应。

**④ 配套加固（同轮）**：**超长压缩重写**——`writeScript` 中若文案超 `(素材数×duration-8)×4.5×1.3` 字（模型偶发不守字数、超长致 I2V 段慢放失真），带压缩指令重写一次（任务31 实证 423→339 字）；`buildUserPrompt` 同步强约束"从第一字即正文 + 禁写作元词"。

---

## P2a 话术库：场景命中直出 + 差异化轮换 + 自动沉淀（2026-09-04）

**定位**：从"链路"走向"产品"的第一步。把"每次从零生成"升级为"先复用、再生成、生成即沉淀"，同时解决**费钱**与**同质化**两个产品痛点。

**数据底座**：`VideoScript`（`db_clue_video_script`，Entity + Repository + Provider 全量）——字段：scene 场景枚举 / style 风格档 / hook 钩子 / content 正文 / tags 覆盖素材关键词 / **score 效果分**（学习回灌用）/ **useCount 已直出次数**（轮换退休用）。预置 5 条"土味口播"语料（水电验收 4 条不同叙事结构 + 通用 1 条）：对比式/故事式/清单喊话式/悬念式。

**命中链路**（`AiScriptWriter.writeScript`，走 LLM 之前）：
1. **场景推断** `inferScene`：素材描述 + 钩子关键词词典 → WaterElectric/Tiling/Waterproof/Cabinet/General
2. **查库**：`findBySceneAndEnabledTrueOrderByScoreDescUseCountAsc`（效果分高、用得少优先）
3. **三重过滤**后才直出：`useCount < 3`（防一个味）+ 字数 ≤ I2V 容量（防慢放）+ `contentAligns`（话术 tags 命中素材画面，防错配）→ **0 LLM 费出稿**，`useCount++` 实现天然轮换
4. 未命中 → 走 LLM；**生成成功自动入库**（`saveToScriptLib`：场景归类 + 素材关键词 tags 落库），库随使用增长

**端到端实测（2026-09-04）**：任务32/33 并发命中话术#1、任务34 命中话术#2——**全部 0 LLM 调用**，I2V 全走按图缓存，纯离线出片。任务34 成片 40s：字幕泄漏检测 0，t=14 帧红白管道 ↔ 花字「封口要严实」↔ 字幕「返工花了小两万。」（故事式）对应；`useCount` 落库正确。

**已知限制与下一步**：① 并发同秒可能命中同一话术（useCount 读取竞争，真实业务低概率，后续可加"近期已用"短期缓存去重）；② score 回灌待 P2d 效果回流落地（当前默认 0，靠 useCount 轮换保证差异化）；③ 话术直出跳过了 LLM 一致性校验，靠 `contentAligns` 保底对齐。

---

## P2b 风格画像 + 叙事结构池：按业务员画像生成（2026-09-04）

**定位**：把个性化从"话术轮换"推进到"按人生成"。每个业务员一套画像（风格档 + 结构偏好 + 常做场景），生成时注入——同素材不同业务员，出的是不一样味道的视频。

**数据底座**：
- `StyleProfile`（`db_clue_style_profile`，Entity + Repository，预置 emp=1 土味/Suspense）：empId / style 风格档（土味/专业/唠嗑/悬念/权威）/ structurePref 首选结构 / sceneFocus 常做场景
- `ScriptStructure`（枚举结构池 6 套）：悬念式/对比式/清单式/故事式/痛点式/权威式，各带骨架描述 + 开头钩子套路

**生成链路（`AiScriptWriter`）**：
1. **加载画像**：`findByEmpIdAndEnabledTrue` → 无画像时随机抽结构（保同素材差异化）
2. **话术命中按画像风格过滤**：`styleMatches`——土味画像命中土味话术直出（0 费）；专业画像对土味话术全跳过 → 走 LLM
3. **prompt 注入画像**：`buildUserPrompt` 注入"口播风格（按业务员画像）+ 叙事结构骨架 + 开头钩子套路"
4. **LLM 生成按画像风格入库**：`saveToScriptLib` 用画像 style 落库 → 该风格话术随使用沉淀

**端到端实测（2026-09-04，同素材对比）**：
| 任务 | 画像 | 链路 | 结果 |
|---|---|---|---|
| 35 | emp=1 土味/Suspense | 命中话术#3（清单式土味）直出 | **0 LLM 费**，Done |
| 36 | emp=2 专业/Authority | 土味话术全跳过 → LLM 权威式生成 | 359→303 字入库（style=专业），Done |

字幕对比：任务35 土味「师傅收工别急着走人，这三处低头看仔细了」vs 任务36 专业「干了十多年水电监理，合格工地你盯准这三处就够」——**同一批素材，两种完全不同的味道**。任务36 权威式文案已入库（id=8/9，style=专业），下次 emp=2 出水电视频可直接免 LLM 复用。t=16 帧画面（黄漆墙+绿防水+白管）↔ 花字「封口要严实」↔ 字幕「二处，蹲下摸那根白管」对应。


---

## 附：设计要点自检

- [x] 数据访问全部 JPA（Entity + Repository），无 MyBatis、无 XML Mapper
- [x] 无 Controller，入口统一走 `Provider`，由框架 `HttpProcessor` 自动路由
- [x] 智能体基于 `agentscope-harness` 的 `HarnessAgent`（workspace/记忆/压缩/会话持久）
- [x] 对接 DeepSeek（一等供应商；OpenAI 兼容通道兜底）
- [x] 延续 `com.th4.edge.decoboon.*` 包规范与 `@Model/@Note` 注解体系；主键采用 `@GeneratedValue(IDENTITY)`（框架 Snowflake 生成器仅支持 String 且默认未启用，Long 主键禁用）
- [x] 多租户（公司=Env）与会话隔离（RuntimeContext.userId）
- [x] 生成任务异步 + 状态机 + JPA 事件驱动，可断点续跑/重试
