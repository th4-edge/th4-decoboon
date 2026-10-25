# Designer / 设计师 App/Web 全流程对齐报告 v0.1

> 日期：2026-10-23  
> 类型：只读静态对账；未修改 App/Web/后端业务代码、权限、状态机、DDL、迁移或生产数据。  
> 范围：客户 → 房屋 → 量房 → 设计方案 → 报价 → 合同 → 项目 → 项目协作/后续事实。  
> 基线口径：按用户已确认前提，App 关键业务操作是“已验证业务基线”；Web 应复用同一业务事实，只允许大屏交互、密度、批量和端侧能力增强。  
> 证据边界：本报告读取的是当前工作区，不是已发布候选。

## 0. 快照、结论与使用边界

### 0.1 代码快照（重要）

| 仓库 | HEAD | 工作区状态 | 风险 |
| --- | --- | --- | --- |
| `th4-decoboon` | `7cf83fa` | 510 个未提交/未跟踪变更 | 产品基线目录本身处于未跟踪状态，相关 Provider 也有大量修改 |
| `th4-decoboon-app` | `aa387a8` | 88 个未提交/未跟踪变更 | 设计、报价、合同、项目、权限页面均在工作树中被修改 |
| `th4-decoboon-web` | `0cbdc06` | 60 个未提交/未跟踪变更 | 设计师队列、合同交接、报价工作区、路由与工作台均有未提交修改 |

因此：

1. 本文的“当前实现”只代表 2026-10-23 工作树快照，不能写成已发布事实。
2. 本文不把 type-check/build、接口返回 200 或静态入口等同于客户可用。
3. 所有 App/Web 页面结论均为静态可达性、代码动作和 Provider 对账，没有重新执行真实页面、真机、跨租户或生产冒烟。
4. 已有验收文档可证明的部分单列为“已验收证据”；其余标记为“代码现状/待验证”。

### 0.2 结论概览

| 结论 | 判定 |
| --- | --- |
| 设计师岗位身份 | 固定 `Designer`；默认 `Self`，可因客户负责人/项目参与关系扩大可见范围 |
| 业务主责 | 量房、设计、报价、谈单、签约发起、增减项发起、选材、项目设计协作 |
| 明确不得拥有 | 合同最终确认签约/生效立项、财务收付、业主本人确认、企业治理 |
| App/Web 页面覆盖 | App 的设计、报价、合同、项目主链总体完整；Web 在客户 360、模板、表格报价方面更重 |
| 同源 Provider | 客户、房屋、量房、设计、报价详情、合同详情、项目、选材、变更主体大体复用同一 Provider |
| 仍存在的断点 | 工作台/队列 Provider 分裂；阶段验收权限冲突；合同深链丢失；设计模板 App 缺失；部分队列硬截断；概念文案未收口 |
| 已确认 P0 | 本次静态检查未确认新的 P0（跨租户、金额/成本错误或数据删除）；不等于已通过安全/权限验收 |

### 0.2.1 2026-10-23 用户补充裁决

| 事项 | 用户裁决 | 对报告的影响 |
| --- | --- | --- |
| 设计师阶段验收 | **允许设计师提交** | DAW-01 的规则方向关闭为“可提交”；App 只补 `act:acceptRequest`，不得授予 `act:acceptConfirm`；后端仍须按 `AcceptanceDone` 模式和项目参与关系校验 |
| Web 待我谈单/待我签约 | **工作队列与报价历史拆成两个页面/能力** | DAW-02/DAW-03 的目标结构冻结：待我谈单消费 `statistic/workbench/quote/page`，待我签约消费 `statistic/workbench/sign/page`，报价历史消费 `quote/designer/page` |
| App 设计模板 | **App 端先不做** | DAW-06 标记为 `DEFERRED_BY_USER`，不是已闭合，也不是已验收；Web 当前模板能力保留，App 不新增入口 |

### 0.3 最严重的 3 个跨端断点

1. **DAW-01 / P1：设计师阶段验收写能力在 App、Web、后端三处不一致**  
   App 设计师能力矩阵没有 `act:acceptRequest`，路由守卫也不可达；Web 与后端却允许项目参与设计师提交阶段验收。相同设计师会在 Web 能提交、App 不能提交。

2. **DAW-02 + DAW-03 / P1：Web「待我谈单」「待我签约」没有复用 App 的后台工作台 Provider，且风险天数口径漂移**  
   Web 使用通用报价分页后在浏览器筛选，最多 200 条；App 使用 `statistic/workbench/quote/page`、`statistic/workbench/sign/page`。Web 还以 `createdAt` 计算报价停留天数，把 Submitted 的 3 天阈值写成 7 天。

3. **DAW-06 / P1：设计模板方案库已裁决 App/Web 双端完整，但 App 仍无入口和消费链**  
   Web 已有模板列表、编辑、发布、停用、复制、实例化；App 无模板路由、页面、API 封装。这是已登记 `PI-20261022-004` / `DEC-084` 的明确能力缺口，不是交互差异。

---

## 1. 角色定义与边界

### 1.1 岗位与数据范围

| 项目 | 当前正式口径 | 证据 |
| --- | --- | --- |
| 角色键 | `Designer` | `01a-role-matrix-v0.2.md:54`; 后端 `EmployeeRole` |
| 中文名 | 设计师 | 同上 |
| 默认数据范围 | `Self` | `01a-role-matrix-v0.2.md:73` |
| 可扩展范围 | 客户负责人、指派设计师、项目 designer/manager/support/quote.createdBy 等明确参与关系 | `OBJ-001`; `ProjectAccessContext.visibleProject()` |
| 读取客户 | 本人负责或本人为负责设计师的客户 | `CustomerContext.findVisible()` |
| 读取报价 | 默认本人创建；合同确认相关角色可读本店/企业范围 | `quote/designer/page`; backend 设计/报价详情角色校验 |
| 读取项目 | 数据范围 + 项目参与关系；不得靠前端传参扩大 | `ProjectAccessContext.visibleProject()` |
| 成本/利润 | 不因设计师身份获得；后端按 BOSS/店长/财务裁剪 | `DesignCostContext`; `BR-052` |

### 1.2 可做 / 不可做

| 业务段 | 设计师可做 | 明确边界 |
| --- | --- | --- |
| 客户与房屋 | 查看本人客户；写跟进；加微信/到店；休眠/恢复；编辑基础资料；新增/编辑房屋 | 不能物理删除客户；不能改企业/门店/负责人范围；不能用第一条房屋兜底 |
| 量房 | 约定/调整量房；录入量房、原始区域、现场照片 | 量房必须绑定具体房屋；已完成量房冻结原始区域 |
| 设计 | 新建/复制/删除草稿方案；维护区域、施工/主材分项、图纸；生成谈单材料；App 可发起 AI 效果图 | 不能越权改成本；不能修改已锁定/已归档方案 |
| 报价 | 新建草稿、编辑明细、提交、登记客户接受、驳回/改版重报、PDF | 金额由服务端计算；不能直接确认合同生效或立项 |
| 合同 | 发起 `PendingSign`；提交企业确认；查看合同事实与 PDF | 不能确认 `Signed`；不能确认 `Effective`；不能生成项目 |
| 项目 | 查看参与项目；编排施工计划；项目内发起增减项；主材选材；查看现场、验收、整改 | 现场日报、节点留证、开工/完工、验收确认不默认属于设计师 |
| 增减项 | 发起变更单并对客户同频；查看审批/生效结果 | 审批由 BOSS/财务/本店店长；审批通过即生效，不建模业主确认 |
| 验收 | 当前存在冲突：后端/Web 允许项目参与设计师提交；App 能力键不允许 | 只能提交，不能代业主确认/驳回 |
| 财务 | 可看与合同履约相关的客户侧金额摘要 | 无收付、退款、记账、成本/利润写权限 |

### 1.3 交接边界

```text
业务员/店长/BOSS
→ 指派设计师或客户负责人
→ 设计师：量房、设计、报价、登记客户接受、发起合同
→ 店长/BOSS：复核最终签约价、确认签约、确认生效并生成唯一项目
→ 项目经理：现场执行、节点留证、日报、验收申请
→ 设计师：项目设计协作、增减项发起、选材、客户同频
→ 财务：下单/收付/结算/成本
→ 业主：报价接受按线下共识由设计师登记；阶段验收由业主本人确认
```

### 1.4 存量用户操作习惯（仅限有证据的候选）

以下不是生产行为统计，只是代码/冻结稿可证明的既有操作路径：

| 习惯候选 | 证据 |
| --- | --- |
| App 设计师固定 5 个 Tab：工作台、设计、报价、项目、我的 | `th4-decoboon-app/src/roles/catalog.ts:52` |
| App 首页常用动作顺序以“待我设计、待我报价、合同签约、增减项、客户诉求、我的客户、报价池、设计业绩”为主 | `roles/catalog.ts:112-126`，代码注释明确为高频动作排序 |
| App 将设计、报价、签约视为三个独立工作台，并依赖停留天数风险提示 | `BR-005`; `SCN-007`; `quote/pages/workbench.vue:73-76` |
| App 项目详情以“当前对象 + 阶段/任务/事实/现场/验收”连续工作面组织 | `subpackages/project/pages/detail.vue` |
| Web 已有冻结菜单结构：我的客户、待我跟进、待我量房、待我设计、待我谈单、待我签约、项目工地、主材选材、增减项、反馈、整改、业绩、报价项目库、方案模版、费用报销 | `th4-decoboon-web/src/roles/workbenchConfig.ts:153-193` |

因此，后续任何改名、合并页面或改动主行动顺序，都应视为可能影响存量习惯的产品变更，不能作为“纯前端优化”直接上线。

---

## 2. App 全功能页面清单

> 说明：`完整` 指当前代码存在页面、入口和至少一条成功/失败/空态路径；不代表真实客户验收。无权限页统一由路由守卫阻断，服务端仍需二次校验。  
> App 能力矩阵来自 `roles/permission.ts:148-160`，页面级守卫来自 `roles/guard.ts:13-120`。

### 2.1 导航与工作台

| 入口/路由 | 页面目标/关键字段 | 主要动作 | API / Provider | 状态与恢复 |
| --- | --- | --- | --- | --- |
| `/pages/index/index` 工作台 | 设计师今日概览、在途客户、合同/项目提醒、快捷入口 | 待我设计、待我报价、合同签约、增减项、诉求、客户、报价池、业绩、通知；内容任务入口 | `statistic/designer/report`; `project/project/page`; `site/report`; `collab/notice` | 首屏骨架；统计失败按区块提示；空数据有引导；下拉刷新 |
| `/subpackages/design/pages/workbench` 设计 Tab | 当前设计师待设计客户+房屋 | 按待设计/设计中/方案完成筛选；录诉求；去设计；查看方案；AI 生成 | `statistic/workbench/design/page` | 加载骨架、空态、错误 toast；停留 ≥7 天标红 |
| `/subpackages/quote/pages/workbench` 报价 Tab | 待跟进报价 | 全部/草稿/待确认/需调整；继续编/调整/催确认 | `statistic/workbench/quote/page` | 后端给 `stayDays`；Submitted≥3、Rejected≥1、Draft≥7 标红 |
| `/pages/project/list` 项目 Tab | 本人参与项目/项目经营概览 | 筛选、进入项目、发起合同、查看增减项经营（角色相关） | `project/project/page`; `project/project/overview` | 分页、下拉刷新、空态、错误重试；项目对象必须参与关系匹配 |
| `/pages/mine/index` 我的 | 个人资料、企业、薪酬、设置等 | 查看本人工资条、个人资料、退出等 | 身份/薪酬 Provider | 非本批重点；权限按能力键 |
| `/pages/customer/list` 我的客户 | 本人负责或指派给本人的客户 | 新建/编辑/跟进/筛选；查看阶段、设计师、下次跟进 | `customer/customer/overview`; `customer/customer/page` | 分页、空态、加载失败重试；不展示企业外客户 |

### 2.2 客户、房屋与量房

| 路由 | 页面目标/关键字段 | 动作 | Provider | 状态与恢复 |
| --- | --- | --- | --- | --- |
| `/subpackages/customer/pages/detail` | 客户档案、负责人/设计师、房屋多选、画像、跟进、阶段主行动 | 电话、复制号、写跟进、微信、到店、休眠/恢复、新增/编辑房屋、约定/录入量房、进入设计/报价/项目 | `customer/customer/info`; `customer/customer-follow/page`; `property/property/page`; `design/scheme/page`; `quote/designer/page`; `project/project/page` | 局部区块 `Promise.allSettled`；失败显示部分数据暂不可确认并可重试；客户/房屋必须匹配 |
| `/subpackages/customer/pages/edit` | 客户基础资料 | 新增/编辑客户；阶段不作为普通字段直改 | `customer/customer/save|update` | 服务端校验归属；错误 toast |
| `/subpackages/customer/pages/follow` | 跟进记录与下次跟进 | 保存跟进、图片证据 | `customer/customer-follow/save` | 保存后回读；失败保留输入 |
| `/subpackages/customer/pages/wechat-record` | 微信事实 | 记录已加微信/微信号 | `customer/customer/wechat` | 回读客户 |
| `/subpackages/customer/pages/portrait-edit` | 客户画像 | 保存决策角色、目的、预算/风格等 | `customer/customer-portrait/save` | 保存后刷新 |
| `/subpackages/customer/pages/tag-manage` | 标签字典与绑定 | 新建/改名/删除/绑定/解绑 | `customer/customer-tag/*` | 删除仅字典；绑定失败可重试 |
| `/subpackages/customer/pages/property-edit` | 房屋档案 | 新增/编辑房屋 | `property/property/save|update` | 地址重复、无权限由后端返回 |
| `/subpackages/customer/pages/measure` | 约定/调整量房 | 选择房屋、日期时间；校验晚于当前时间 | `property/measure/page`; `customer/customer/measure` | 无房屋时先建档；已量房阻断重复预约 |
| `/subpackages/customer/pages/measure-record` | 量房记录 | 日期时间、现场照片、原始区域面积/周长；保存后冻结 | `property/measure/save`; `property/measure/region/save|delete` | 已完成只读；区域必须 >0；三步保存后回读 |
| `/subpackages/customer/pages/flow` | 客户业务流转 | 查看房屋、设计、报价、谈单、合同、项目 | `customer/*`, `property/*`, `design/*`, `quote/designer/*`, `project/*` | 各段独立失败降级；设计师从客户详情/项目入口进入的可发现性较弱 |

### 2.3 设计

| 路由 | 页面目标/关键字段 | 动作 | Provider | 状态与恢复 |
| --- | --- | --- | --- | --- |
| `/subpackages/design/pages/list` | 客户 + 房屋下的设计 Brief、方案、报价关联 | 新建空白方案、复制、删除草稿、录诉求、生成谈单、生成报价、查看项目 | `design/scheme/page|save|delete`; `design/requirement/page`; `design/talk/build`; `quote/save`; `project/project/page` | 已接受报价/已立项锁定；部分加载失败可重试；空态引导新建 |
| `/subpackages/design/pages/scheme` | 方案资料、区域工程量、施工/主材分项、图纸 | 保存资料、上传 CAD、区域增删改、分项增删改、完成方案、生成报价、AI 效果图 | `design/scheme/*`, `design/space/*`, `design/item/*`, `design/generation/*` | 草稿可编辑；完成/锁定后只读；成本由后端裁剪 |
| `/subpackages/design/pages/requirements` | 客户诉求列表 | 查看/筛选诉求 | `design/requirement/page` | 失败重试；空态 |
| `/subpackages/design/pages/requirement` | 诉求详情/录入 | 新增/更新需求/排斥点、分类、优先级、空间、设计回应 | `design/requirement/save` | 保存后回读 |
| `/subpackages/design/pages/ai-generation` | AI 效果图任务、上下文、版本 | 发起生成、轮询、预览、版本对比、确认封面/区域图、错误码提示 | `design/generation/build|info|page|update` | 缺 CAD/户型图阻断；生成中轮询；失败给业务错误码；AI 概念图需人工确认 |
| `/subpackages/design/pages/talk` | 谈单 PPT 内容与版本 | 查看/生成/打开谈单材料 | `design/talk/page|build|info` | 无设计阻断；生成失败可重试 |
| `/subpackages/design/pages/design-picker` | 报价上下文选择方案 | 选择可报价方案 | `design/scheme/page` | 只显示可报价方案；空态提示去设计 |

### 2.4 报价与合同

| 路由 | 页面目标/关键字段 | 动作 | Provider | 状态与恢复 |
| --- | --- | --- | --- | --- |
| `/subpackages/quote/pages/list` 报价池 | 按客户聚合多房屋/多版本报价 | 新建、切换版本、查看谈单/客户 | `quote/designer/page`; `statistic/boss/quote/report`（BOSS） | 设计师只读本人创建；空态/错误重试 |
| `/subpackages/quote/pages/edit` | 套餐/自定义报价生成 | 选客户、房屋/方案、套餐、面积、折扣；生成草稿 | `quote/save`; 套餐/设计/房屋 Provider | 房屋已有确认报价或项目时锁定；缺地址阻断 |
| `/subpackages/quote/pages/detail` | 报价头、区域/类型视图、金额、成本裁剪、下游合同/变更 | 提交、驳回、登记接受、改版重报、PDF、进入设计/谈单、发起合同、变更审批（有权限时） | `quote/designer/info`; `quote/update`; `quote/line/*`; `document/quote/build`; `project/contract/*`; `project/contract-change/*` | 接口失败显示暂不可确认；报价版本冻结；金额服务端权威 |
| `/subpackages/quote/pages/adjust` | 套餐调整/替换/扣减 | 新增调整项、确认/忽略差异、删除 | `quote/line/save|delete` | 仅草稿可改；失败重新读取 |
| `/subpackages/quote/pages/quote-confirm` | 设计转报价确认 | 选套餐、确认估算金额、生成报价 | `quote/save` | 方案必须可报价；锁定后阻断 |
| `/subpackages/quote/pages/catalog/manage` | 报价项目库 | 筛选、查看详情/版本、维护（仅 BOSS/店长/财务）；设计师只读 | `catalog/*` | 设计师 `view:catalog`，无 `edit:catalog` |
| `/subpackages/quote/pages/package-manage` | 套餐模板 | 查看；BOSS/店长维护 | `pack/*` | 设计师只读 |
| `/subpackages/project/pages/sign-workbench` | 已接受报价的签约队列 | 去签约、查看合同；按待签约/履约/已完成筛选 | `statistic/workbench/sign/page` | 后端提供 `converted`、合同/项目状态、`stayDays`；停留风险阈值 3/7 天 |
| `/subpackages/project/pages/create` | 合同发起/签约/生效/立项表单 | 设计师发起；店长/BOSS 确认签约、生效；进入项目 | `project/contract/save|update`; 模板选择器 | 状态分步；设计师只能发起；重复操作幂等；错误状态阻断 |
| `/subpackages/project/pages/contract` | 合同履约档案 | 合同金额、状态、付款计划、变更、项目进度、选材、事实链、PDF | `project/contract/workbench`; `contract-line/page`; `document/contract/build` | 明确签约/生效分离；失败重试；合同深链可用 |

### 2.5 项目与后续事实

| 路由 | 页面目标/关键字段 | 动作 | Provider | 状态与恢复 |
| --- | --- | --- | --- | --- |
| `/subpackages/project/pages/detail` | 项目 Hero、阶段、任务、现场、验收、反馈、成员、事实 PHASE | 查看项目；设计师可发起增减项、进入选材、计划、合同、材料摘要；写日报/节点/验收受能力限制 | `project/project/info`; `project/stage/page`; `project/task/page`; `site/report/page`; `project/acceptance/page`; `project/contract/info`; `feedback/*` | 分区独立降级；事实节点可跳相应对象；不把无权限按钮伪装为可执行 |
| `/subpackages/project/pages/plan` | 施工阶段与任务计划 | 查看；设计师可进入计划编辑 | `project/stage/page`; `project/task/page` | `edit:plan` 控制；计划失败重试 |
| `/subpackages/project/pages/plan-edit` | 计划编排 | 设计师/PM/店长/BOSS 编排 | `project/task/save`; `project/stage/update` | 资格/状态服务端校验 |
| `/subpackages/project/pages/stage/detail` | 阶段详情 | 查看阶段/任务；开工、验收提交受能力限制 | `project/stage/info`, `project/task/page` | 无权限按钮不渲染；状态服务端权威 |
| `/subpackages/project/pages/task/detail` | 任务详情 | 设计师只读；节点留证属于 PM/BOSS/店长 | `project/task/info`, `project/task-node/save` | 能力键控制；失败回读 |
| `/subpackages/project/pages/album` | 项目相册：按日期/阶段归档现场汇报与验收照片 | 查看、分页加载、图片预览；不直接上传 | `customer/project/album/page` | 项目参与范围；空态/加载更多/错误重试 |
| `/subpackages/project/pages/members` | 项目成员 | 查看；更换成员仅 BOSS/本店店长 | `project/member/list`; `project/project/update` | 设计师只读 |
| `/subpackages/project/pages/selection-board` | 选材工作台 | 查看项目选材进度 | 选材 Board Provider | 设计师 `view:selection-board` |
| `/subpackages/project/pages/selection` | 项目主材选材 | 查看/维护选材状态与文本、交付财务 | `material-selection/*` | 设计师可编辑；进入采购后换材走增减项 |
| `/subpackages/project/pages/selection-item` | 单条选材 | 品牌/规格/颜色/照片等 | `material-selection-item/info|save` | 采购锁定后禁用直接换材 |
| `/subpackages/project/pages/change-project-picker` | 增减项项目选择 | 按设计师/状态筛选本人项目 | `project/project/page`; `identity/designer/list` | 设计师只能进入参与项目 |
| `/subpackages/project/pages/change` | 项目增减项列表 | 查看、发起、审批（有权限）、取消/撤回 | `project/contract-change/page|info|update|cancel` | DEC-068 状态显示为待审批/已生效/已驳回/已取消 |
| `/subpackages/project/pages/change-create` | 逐行增减项 | 选择原合同行/已生效变更目标，填写数量/金额/原因，预览后批量提交 | `project/contract-change/range|preview|batch-save` | 服务端核对可变更量和金额；失败阻断 |
| `/subpackages/project/pages/change-detail` | 变更详情 | 审批/驳回/撤回 | `contract-change/update|cancel` | 审批人角色由后端校验 |
| `/subpackages/project/pages/rectify-board` | 整改跟进 | 查看整改项 | `acceptance/rectify/page` | 项目参与范围 |
| `/subpackages/project/pages/feedback-list` | 客户意见投诉 | 设计师查看、回复、关闭（按反馈规则） | `customer/feedback/page|save|close` | 不能代业主确认；失败可重试 |
| `/subpackages/project/pages/owner-preview` | 业主视角预览 | 预览并返回员工工作面 | `ownerpreview/info` | 设计师 `view:owner-preview`；预览不等于业主确认 |
| `/subpackages/project/pages/journey` | 业主旅程 | 业主/预览的只读事实链 | `customer/project/journey/info` | App 业主基线；员工预览受控 |

### 2.6 App 明确不可达或仅只读的设计师能力

| 页面 | 当前结论 | 证据 |
| --- | --- | --- |
| 量房预约经营工作台 | 设计师无 `view:measure-workbench`；不是设计师路由入口 | `permission.ts:148-160`; `guard.ts:75` |
| 阶段验收编辑页 `/subpackages/project/pages/accept/edit` | 需要 `act:acceptRequest` 或 `act:acceptConfirm`，设计师均无 | `guard.ts:66` |
| 现场日报编辑 | 设计师无 `act:report` | `permission.ts:148-160`; `guard.ts:64` |
| 任务编辑 | 设计师无 `edit:task` | 同上 |
| 合同确认/生效 | 设计师无 `act:contractConfirm`、`act:projectCreate` | 同上；`ProjectAccessContext.requireProjectCreator()` |
| 财务写操作 | 设计师无 `view:finance` / `edit:finance` | 同上 |
| 设计模板方案库 | App 无模板路由、页面、域名 API（已知缺口） | `config/index.ts:125-152`; `PI-20261022-004` |

---

## 3. Web 全功能页面清单

> Web 路由为按角色静态装配；设计角色集合见 `src/roles/routes.ts:197-202`，菜单见 `roles/workbenchConfig.ts:159-193`。

### 3.1 导航与工作台

| 路由 | 页面目标/关键字段 | 主要动作 | API / Provider | 状态与恢复 | 判定 |
| --- | --- | --- | --- | --- | --- |
| `/designer/workbench` | 设计师经营总览、主链、Today Queue | 看 KPI、漏斗、趋势、待办并进入工作台 | `statistic/workbench/designer/report`; `.../queue/list` | 骨架、报告失败、队列失败分别展示；空数据引导 | 交互增强；Provider 与 App 不同，见 DAW-04 |
| `/designer/my-customers` | 我的客户、阶段、风险、排序、分页 | 新增客户、进入客户 360 | `customer/my-customer/overview` | 分页/加载更多/空态/错误重试 | 对齐，Web 增强筛选 |
| `/designer/my-customers/:customerId` | 客户 360：客户、房屋、诉求、方案、报价、合同、项目、量房、付款 | 编辑客户/房屋、删除无事实房屋、进入各对象工作区 | `customer/my-customer/overview`（用于找客户）、`project/project/page`（200 条后前端过滤）等 | 分对象降级；付款无权限时显示合同条款 | 大屏增强；存在硬截断，见 DAW-09 |
| `/designer/followup` | 待我跟进队列 | 写跟进、画像、微信、到店、预约量房、休眠/恢复、编辑客户/房屋 | `customer/my-customer/overview`; `customer-follow/*`; `property/*`; `design/requirement/*` | 三栏、空态、失败重试；`intention` 预约量房先阻断并引导首联 | 对齐并增强；首 100 条截断风险 |
| `/designer/measure` | 待我量房任务 | 建房屋、预约/改约、登记量房与原始区域、新建方案、休眠 | `statistic/designer/measure/list`; `property/measure/*`; `design/scheme/save` | 服务端任务主行动；缺房屋可恢复建房屋；错误保留上下文 | App 无同页队列，但能力可达；Web 增强 |
| `/designer/customers` | 客户与设计推进工作区 | 维护房屋、方案、诉求 | `customer/customer/info`; `property/*`; `design/*` | 部分失败提示；多房屋显式选择 | 保留工作区，不在主菜单 |
| `/designer/design` | 待我设计 | 客户分组、房屋切换、新建模板/空白方案、录诉求、打开设计 | `statistic/workbench/design/page`; `design/*`; `design/template/*` | 空态/错误；方案锁定；模板失败降级 | 对应 App 设计工作台，Web 额外支持模板起稿 |
| `/designer/design/:designId` | 设计方案工作区 | 图纸/CAD、区域、分项、报价生成、谈单、保存 | `design/scheme/info|update`; `space/item/*`; `talk/*`; `quote/save` | 方案状态只读；服务端重算；错误可回读 | 对应 App，Web 大屏增强；App 独有 AI 效果图 |

### 3.2 报价、合同

| 路由 | 页面目标/关键字段 | 主要动作 | API / Provider | 状态与恢复 | 判定 |
| --- | --- | --- | --- | --- | --- |
| `/designer/quotes` | 待我谈单队列 | 筛选草稿/待确认/驳回/合同交接；进入报价工作区 | `quote/designer/page`（size 200 + 前端筛选） | 错误/空态/刷新 | 与 App 工作台 Provider 不一致，见 DAW-03 |
| `/designer/quotes/:quoteId` | Excel 式报价工作区 | 表格编辑、批量改价、版本轨、版本对比、Excel 导入导出、PDF、提交、登记客户接受 | `quote/designer/info`; `quote/lines/save`; `quote/update`; `quote/workbook/*`; `document/quote/build` | 服务端权威重算；版本切换拦截未保存；错误/导出/导入失败不静默跳过 | App 单行/移动交互 + Web 批量增强；允许的桌面增强 |
| `/designer/contract` | 待我签约 / 合同交接 | 发起合同、查看合同状态、签约资料/PDF/事实链 | `quote/designer/page` + `project/contract/info`; `project/contract/save` | 设计师发起；签约/生效仅 BOSS/店长；错误可重读 | 大屏增强；Provider、深链和停留口径存在断点 |
| `/designer/projects` | 项目工地 | 本人参与项目、阶段、任务、动态、验收；进入选材/变更/合同 | `project/project/page`; `project/project/info`; `project/stage/page`; `project/task/page`; `site/report/page`; `project/acceptance/page` | 分区错误提示；成员调整仅 BOSS/店长 | 对应 App 项目页；设计师只读现场执行 |
| `/workbench/project-execution` | 施工排期/PHASE 事实/现场留证 | 设计师可调整任务排期；节点留证和日报仅 PM/BOSS/店长；事实节点跳报价/设计/合同 | `project/project/info`; `project/task/*`; `site/report/*` | 事实节点角色裁剪；无权限不显示写按钮 | 对齐 App 计划/事实链；合同深链有 bug |
| `/designer/projects/:projectId/materials` | 主材选材工作区 | 逐项代录选材、照片、交付财务 | `material-selection/*` | 已采购锁定；发布/停止下单走规则 | 对应 App 选材 |
| `/designer/projects/:projectId/changes` | 增减项工作区 | 行级新增/减项/替换、试算、提交、查看审批 | `project/contract-change/range|preview|batch-save|page` | 服务端核对剩余量/占用；不建模业主确认 | 对应 App `change-create` |
| `/designer/projects/:projectId/acceptance` | 阶段验收工作区 | 选阶段、查看任务、提交首验/复验、证据、轮次 | `project/acceptance/page|save`; `round/page` | 任务未完/已有 Pending 阻断；业主确认不可代录 | Web/后端允许设计师，App 不可达，见 DAW-01 |
| `/workbench/contract-changes` | 增减项经营看板 | 审批/筛选/进入项目变更 | `contract-change/*` | 设计师可看，审批按钮按角色 | 角色相关 |
| `/workbench/material-selection` | 选材工作台 | 查看跨项目选材队列 | 选材 Board Provider | 空态/错误 | 对应 App |
| `/workbench/rectification` | 整改跟进 | 查看整改项 | `acceptance/rectify/page` | 空态/错误 | 对应 App |
| `/workbench/customer-feedback` | 客户意见投诉 | 查看/回复/关闭 | `customer/feedback/*` | GAP-WEB-015 仍待业务裁决 | 入口与首页卡片口径不一致 |

### 3.3 经营工具与共享页

| 路由 | 页面目标 | 动作 | Provider | 判定 |
| --- | --- | --- | --- | --- |
| `/designer/catalog` | 报价项目库 | 查询分类/品牌/系列/规格、看价格/工艺/验收/图片；维护仅 BOSS/店长/财务 | `catalog/*` | 对齐 App；Web 大屏表格详情增强 |
| `/designer/templates` | 我的方案库 | 新建、查看、编辑、发布、停用、复制、实例化 | `design/template/*` | Web 侧完整；App 缺失，见 DAW-06 |
| `/designer/templates/:templateId` | 模板编辑器 | 区域骨架、分项、公式、版本、发布 | `design/template/*` | Web 独有当前实现；需双端补齐 |
| `/designer/performance` | 我的业绩 | 本人经营/交付汇总、趋势、排行、待办 | `statistic/workbench/designer/report` | Web 增强；与 App `statistic/designer/report` 不同源 |
| `/designer/feedback` | 兼容重定向 | 转到统一反馈页 | 路由 redirect | 仅兼容 |
| `/account/payslips` | 我的工资条 | 本人查看已发放工资单 | 薪酬 Provider | 跨角色共享；非本批主链 |
| `/finance/expenses` | 费用与报销 | 本人费用申请/查看 | `expense/*` | 设计师自助；非主链 |

### 3.4 Web 明确缺失/未接入的设计师能力

| 能力 | 当前事实 | 证据 |
| --- | --- | --- |
| AI 效果图生成/确认 | Web 路由、菜单、设计工作区均无 `design/generation/*` 消费；App 有完整页面 | 对照 `routes.ts:65-88`、`DesignerDesignWorkspaceView.vue` 与 App `guard.ts:27`、`ai-generation.vue` |
| App 设计模板 | App 无模板路由/页面/API；Web 有 | 见 DAW-06 |
| 设计师移动端现场采集式交互 | Web 可使用上传，等价于文件上传；无强制要求复刻相机交互 | 属允许端侧差异 |

---

## 4. 逐业务对象对比

| 对象 | 身份/版本 | 状态与下一步 | 金额/成本可见性 | App 事实源 | Web 事实源 | 是否同源/是否自拼装 |
| --- | --- | --- | --- | --- | --- | --- |
| 客户 Customer | `custId`；无版本；负责人/设计师对象关联 | `New/Following/PendingMeasure/Quoted/Signed/Serving/Finished/Lost/Dormant` | 无成本；预算等客户侧金额 | `customer/customer/info|page|overview` | 客户 360 使用 `my-customer/overview` 找客户；跟进队列也使用 overview | 后端同源，但 Web 360/队列存在硬截断和前端查找 |
| 房屋 Property | `propertyId/propertyNo`；客户 1:N | `PendingMeasure→Measuring→Measured→Designing→Quoted→Contracted→Serving→Completed` | 无成本；报价/合同继承房屋归属 | `property/property/*` | 同 Provider | 同源 |
| 量房 Measure | `measureId`；每次量房一条事实；原始区域 `measureRegionId` | `Scheduled/Completed/Cancelled`；完成后原始区域冻结 | 无金额 | App 写入时间、现场照片、原始区域；只显示部分后端字段 | Web 补写原始结构、尺寸、客户诉求、设计师结论、结构图、附件 | 同 Provider，但 App 字段面 partial |
| 设计 Design | `designId + version`；按客户累计，房屋由 `property` 绑定 | `Draft/Ready/Presented/Selected/Archived`；Ready 可报价，Selected 后进入合同交接 | 设计师可见销售金额；成本由 `DesignCostContext` 裁剪 | `design/scheme/*`; App 独有 AI generation | `design/scheme/*`; Web 独有模板实例化 | 核心同源；模板与 AI 能力不对称 |
| 报价 Quote | `quoteId + version`；一条记录一版；合同绑定明确版本 | `Draft→Submitted→Accepted/Rejected`；Rejected 后新版本；Accepted 后发合同 | 销售价可见；成本仅 BOSS/店长/财务；设计师无成本 | 详情/编辑同 Provider；工作台使用 `statistic/workbench/quote/page` | 详情/工作区同 Provider；队列使用 `quote/designer/page` 后前端筛选 | 详情同源；队列、风险与停留天数不同源 |
| 合同 ProjectContract | `contractId`；当前无独立版本字段，按“合同基线 + 当前金额 + 变更历史”表达 | `Draft/PendingSign/Signed/Effective/...`；生效后才创建唯一项目 | 设计师可见合同金额/应收摘要；不得见成本利润 | `project/contract/*`; App 通过 sign workbench 进入 | `project/contract/*`; Web 通过通用报价列表 + 拼装合同队列进入 | 读写合同同源；队列与深链不同源 |
| 项目 Project | `projectId/projectNo`；唯一来源为生效合同 | `Preparing/Ongoing/Suspended/Accepting/Finished/Settled` | 合同额可见；成本/利润不属设计师 | `project/project/*`; 详情页 PHASE 事实 | `project/project/*`; `ProjectExecutionView` PHASE 事实 | 核心同源；页面拆分不同；合同事实深链有断点 |
| 合同变更 ContractChange | `changeId/changeNo`；来源合同行或已生效变更 | `Pending→CustomerConfirmed` 对外显示待审批→已生效；`Rejected/Cancelled` | 项目设计师可见销售差额；成本/利润裁剪 | 新路径 `range/preview/batch-save`；旧报价详情仍走 `contract-change/save` | `range/preview/batch-save` | 主路径同源；旧入口语义漂移 |
| 主材选材 MaterialSelection | `selectionId`；项目/合同行/房屋关联 | 未选→已选→交付财务→已采购等；采购后换材走增减项 | 设计师可见销售/选材信息；不可见采购价/成本/利润 | `material-selection/*` | 同 Provider | 同源 |
| 阶段验收 Acceptance | `acceptanceId`；同阶段一张主验收单 + `AcceptanceRound` | Pending→Confirmed/Rejected；整改/复验追加轮次 | 无金额、成本、应收影响 | App 只读/不代业主确认；设计师提交入口不可达 | Web 设计师可提交，业主确认不可代录 | Provider 同源，角色门禁不一致 |
| 反馈 CustomerFeedback | `feedbackId` | submitted/processing/resolved/escalated/closed | 无金额 | `customer/feedback/*` | 同 Provider | 同源；入口/交互仍待裁决 |
| 报价 PDF/合同 PDF | 绑定对象 ID、版本、分组 | 生成归档，不覆盖旧归档 | 客户版仅销售字段 | `document/quote/build`; `document/contract/build` | 同 Provider | 同源 |

### 4.1 已签约项目的四类事实核查

| 事实 | App | Web | 结论 |
| --- | --- | --- | --- |
| 报价 | 项目详情「事实」节点可进入绑定报价；合同页可打开上游报价；客户业务流也可查 | 项目事实节点可跳报价工作区；客户 360 有报价 Tab | 两端均有，但入口和信息密度不同 |
| 设计方案 | 项目事实节点进入设计评论/设计页；报价详情可打开设计；客户业务流可查 | 项目事实节点跳设计工作区；客户 360 有方案 Tab | 两端均有 |
| 合同 | 项目详情加载 `project/contract/info`，合同页接受 `projectId/contractId` | 项目事实节点生成 `projectId/contractId`，但合同交接页只读取 `quoteId`，不按项目/合同定位 | **Web 深链断点，DAW-05** |
| 项目事实 | App 项目详情有 `factTrace` 和阶段/任务/现场/验收上下文 | Web 项目工地页本身未加载 factTrace；需进入 `workbench/project-execution` | 能力存在但被拆页；不是完整缺失，但可发现性和上下文连续性弱 |

---

## 5. 功能能力矩阵

> `完整`：静态上存在入口、读取、写动作、成功回读和失败/空态；`部分`：有入口但字段/动作/回读不完整；`缺失`：无入口或能力键阻断；`增强`：同业务能力下的桌面交互增强。

| 能力 | App | Web | 差异判定 |
| --- | --- | --- | --- |
| 查看客户/房屋 | 完整 | 完整（客户 360 更强） | 对齐/增强 |
| 新增/编辑客户 | 完整 | 完整 | 对齐 |
| 写跟进、微信、到店、休眠/恢复 | 完整 | 完整 | 对齐 |
| 客户转交负责人/设计师 | 设计师无 `act:assignDesigner`，不可 | Web 设计师也无维护入口 | 对齐 |
| 删除客户 | 无 | 无 | 对齐；仅 BOSS |
| 新增/编辑房屋 | 完整 | 完整 | 对齐 |
| 删除无事实房屋 | App 可（后端校验） | Web 客户 360 可（后端校验） | 对齐 |
| 约定/调整量房 | 完整 | 完整 | 对齐 |
| 录入量房与原始区域 | 部分：时间/照片/区域 | 完整：另有原始结构/尺寸/诉求/结论/结构图/附件 | Web 事实面扩展，待确认是否补 App |
| 查看量房 | 完整 | 完整 | 对齐 |
| 新建/复制/删除草稿方案 | 完整 | 完整 | 对齐 |
| 编辑区域/分项/图纸 | 完整 | 完整 | 对齐 |
| 模板列表/编辑/发布/停用/复制/实例化 | 缺失 | 完整 | **App 缺失，DAW-06** |
| AI 效果图生成/确认 | 完整 | 缺失 | **Web 缺失，DAW-07** |
| 谈单 PPT 生成/查看 | 完整（设计列表/甲方页/报价详情） | 完整（报价/合同交接） | 对齐，入口不同 |
| 创建套餐/自定义报价 | 完整 | 完整 | 对齐 |
| 报价逐行编辑/数量公式 | 完整 | 完整 | 对齐 |
| 报价批量改价/版本对比/Excel 导入导出 | 缺失/弱 | 完整 | **允许的桌面增强**，不要求 App 复制全部交互 |
| PDF 报价/合同 | 完整 | 完整 | 对齐 |
| 提交报价/驳回/接受登记 | 完整 | 完整 | 对齐；接受登记主体按 DEC-071 |
| 合同发起 | 完整 | 完整 | 对齐 |
| 合同确认签约 | 无（后端拒绝） | 无（前端按角色隐藏，后端拒绝） | 对齐 |
| 合同确认生效/立项 | 无（后端拒绝） | 无（后端拒绝） | 对齐 |
| 合同事实链查看 | 完整（合同页/项目详情） | 完整（合同页），但项目→合同深链有 bug | **DAW-05** |
| 查看项目/阶段/任务/现场/验收 | 完整 | 完整 | 对齐 |
| 编辑施工计划 | 完整 | 完整 | 对齐 |
| 节点留证/日报 | 缺失（能力键不允许） | 缺失（角色按钮不渲染） | 对齐；业务规则为 PM/BOSS/店长 |
| 提交阶段验收 | 缺失/不可达 | 完整（设计师可提交） | **DAW-01 冲突** |
| 业主确认/驳回验收 | 无（仅业主） | 无（仅业主） | 对齐 |
| 发起增减项 | 完整（新路径） | 完整（行级） | 对齐；App 另保留旧入口 |
| 审批增减项 | 无（后端拒绝） | 无（后端拒绝） | 对齐 |
| 主材选材查看/代录 | 完整 | 完整 | 对齐 |
| 采购下单/结算/成本 | 无（设计师） | 无（设计师） | 对齐 |
| 查看客户反馈/回复 | 完整 | 完整 | 对齐；入口规则待裁 |
| 查看业绩 | 完整（旧 Provider） | 完整（新 Provider） | **Provider 需收口，DAW-04** |
| 费用与报销自助 | 完整 | 完整 | 对齐（非本主链） |

---

## 6. 概念与状态矩阵

### 6.1 核心状态对照

| 对象 | 后端枚举/名称 | App | Web | 判定 |
| --- | --- | --- | --- | --- |
| 客户 | `New` 新客户 | 待首联/新客户 | 待首联 | 语义近似，需统一文案 |
| 客户 | `Following` 跟进中 | 跟进中 | 跟进中 | 一致 |
| 客户 | `PendingMeasure` 待量房 | 待量房 | 待量房 | 一致 |
| 客户 | `Quoted` 已报价 | 方案报价 | 方案报价 | 一致但不同于后端名称 |
| 客户 | `Dormant` 休眠客户 | 已休眠 | 已休眠 | 一致 |
| 房屋 | `PendingMeasure` | 待量房 | 待量房 | 一致 |
| 房屋 | `Measured` | 已量房 | 已量房 | 一致 |
| 量房 | `Scheduled/Completed/Cancelled` | 已预约/已完成/已取消 | 已预约/已完成/已取消 | 一致 |
| 设计 | `Draft` 草稿 | 设计中 | 设计中 | 两端一致，但与后端枚举名称冲突 |
| 设计 | `Ready` 待谈单 | 待谈单 | 待谈单 | 一致 |
| 设计 | `Presented` 已讲解 | 已讲解 | 已讲解 | 一致 |
| 设计 | `Selected` 客户选中 | 已选中 | 已选中/已选定 | 近义不同名 |
| 报价 | `Draft` 草稿 | 草稿 | 草稿 | 一致 |
| 报价 | `Submitted` 已提交 | **待客户确认** | 报价页通常“待客户确认”，列表/客户360有“**待客户决策**” | 同义不同名 |
| 报价 | `Accepted` 客户接受 | 客户已接受 | 客户已接受 | 一致 |
| 报价 | `Rejected` 客户拒绝 | 已驳回 | 已驳回/客户驳回 | 近义不同名 |
| 报价 | `Converted` 已转项目 | 已转项目 | 已转项目 | 一致 |
| 合同 | `PendingSign` 待签约 | 待签约 | 待签约 | 一致 |
| 合同 | `Signed` 已签约 | 已签约 | 已签约（未生效） | Web 语义更明确 |
| 合同 | `Effective` 已生效 | 已生效 | 已生效 | 一致 |
| 合同 | `Void` | 前端映射“已作废” | 部分模型/页面包含 `void` | 后端 `ContractStatus` 当前无 `Void`，仅前端预留，需清理 |
| 项目 | `Preparing` | 待开工 | 待开工 | 一致 |
| 项目 | `Ongoing` | 施工中 | 施工中 | 一致 |
| 变更 | `Pending` | 待审批 | 待审批 | 一致 |
| 变更 | `Approved`（历史） | 待审批 | 待审批 | 一致；不得再产生新历史态 |
| 变更 | `CustomerConfirmed` 字面 | 已生效 | 已生效 | 一致；内部键名不代表系统内业主确认 |
| 验收 | `Pending/Confirmed/Rejected` | 待业主确认/业主已确认/业主驳回 | 待业主确认/业主已确认/业主驳回 | 一致 |

### 6.2 同义不同名/同名不同义

| 概念 | App 名称 | Web 名称 | 处理建议 |
| --- | --- | --- | --- |
| 报价待客户决策 | 待我报价 / 待客户确认 / 报价池 | 待我谈单 / 待客户决策 / 报价工作台 | 需冻结一个用户语言；至少状态文案统一 |
| 合同交接 | 合同签约 / 合同履约档案 | 待我签约 / 合同交接 | “交接”是设计动作，“签约”是合同阶段，不应混用 |
| 设计要求 | 客户诉求 / 设计 Briefs | 客户诉求 / 设计要求 | 去掉英文 Briefs 或定义为内部辅助 |
| 设计分项 | 设计分项 / 报价分项 | 设计项 / 分项 | 以对象卡为唯一名称 |
| 量房完成 | 已完成 / 已量房 | 已完成 / 已量房 / 待交设计 | 明确状态与下一步是两个字段 |
| 房源 | 房屋 / 装修房屋 | 房屋 / 房源（个别筛选/代码） | 正式 UI 统一“房屋” |
| 项目事实 | 项目事实 PHASE | PHASE 事实 / 施工执行 | 同源视图可保留“事实/执行”两种视图名，但不得改变事实集合 |
| 合同当前金额 | 当前合同额 | 当前合同金额 | 统一“当前合同金额” |

### 6.3 下一步语义

| 阶段 | 正确下一步 | 常见错位 |
| --- | --- | --- |
| 量房完成 | 进入设计/建方案 | 被写成“直接报价”或“已签约” |
| 设计 Ready | 谈单、生成报价 | Web 部分文案写“客户选定” |
| 报价 Accepted | 设计师发起/交接合同 | Web 合同页仍可能默认选中别的报价 |
| 合同 PendingSign | 企业负责人/店长确认签约 | 设计师页面不得提供“确认签约”可执行按钮 |
| 合同 Signed | 企业负责人/店长确认生效并立项 | 不得把 Signed 当成“项目已建立” |
| 合同 Effective | 进入项目、计划、选材、变更 | 不得在合同未生效时创建项目 |
| 变更 CustomerConfirmed | 查看合同/任务/材料/应收联动 | 不得把它解释成系统内“业主已确认” |

---

## 7. 跨端差异问题清单

> **GAP 标记**：以下 `DAW-xx` 均为本次只读对账产生的报告级 `GAP-DAW-xx`。本会话未修改 `docs/product/issue-governance/registry.json`，因此它们尚未成为正式问题注册项；进入研发前仍需由产品治理流程确认编号、负责人和关闭条件。

### DAW-01 · P1 · 设计师阶段验收能力冲突

- **用户场景**：设计师完成本项目的设计/施工配合，阶段任务全部完成后，在 Web 提交阶段验收并上传证据；同一个人在 App 看不到可执行入口。
- **App 事实**：
  - 设计师能力矩阵没有 `act:acceptRequest`、`act:acceptConfirm`：`th4-decoboon-app/src/roles/permission.ts:148-160`。
  - `/subpackages/project/pages/accept/edit` 需要上述能力之一：`roles/guard.ts:66`。
  - 项目详情“提交验收/查看验收单”由 `act:startStage` 控制：`subpackages/project/pages/detail.vue:229,599`。
- **Web 事实**：
  - Designer 静态路由包含 `/designer/projects/:projectId/acceptance`：`th4-decoboon-web/src/roles/routes.ts:197-202`。
  - `DesignerAcceptanceView` 的主体条件只判断阶段/任务/验收状态，没有设计师能力栏：`src/views/designer/DesignerAcceptanceView.vue:57-81,166-176`。
- **后端事实**：`project/acceptance/save` 明确允许 `Designer`：`project/service/acceptances/save/support/Save.java:96-104`。
- **业务口径**：`BR-008` 写“项目经理或具备阶段验收执行能力的项目成员”；角色矩阵 A11 中 Designer 为“协”，未冻结“可提交”动作。
- **用户裁决（2026-10-23）**：设计师**允许提交**阶段验收。
- **业务影响**：设计师在 Web 能推进，在 App 被认为无权推进；同一角色、同一订单、同一阶段产生不同操作结果。若培训仍以 App 为准，Web 会形成员工无法理解的第二套权限。
- **处理方向**：App 补 `act:acceptRequest`、页面入口和动作回归；绝不授予 `act:acceptConfirm`。后端仍须先校验 `stage.acceptanceMode=AcceptanceDone`、项目参与关系、任务完成、证据和幂等；Web 不应仅凭 session role 自行放行。
- **复验条件**：Designer、Pm、Boss、StoreManager 四角色在同一阶段分别验证：可见性、按钮、服务端 200/403、Pm 提交、Designer 提交、业主确认不可代录、重复提交幂等。

### DAW-02 · P1 · Web 待签约队列未复用 App 的签约工作台 Provider

- **用户场景**：设计师有很多已接受报价，需要按同一队列契约看到待发起、待企业确认、待生效、已立项和停留风险。
- **App 事实**：`workbenchApi.sign()` 调用 `statistic/workbench/sign/page`：`th4-decoboon-app/src/domains/workbench/api.ts:637-640`。
- **Web 事实**：`DesignerContractHandoffView.load()` 调用 `quoteDesignerPage({size:200})`，仅保留 `accepted/converted`，再对每条报价调用 `project/contract/info`：`th4-decoboon-web/src/views/designer/DesignerContractHandoffView.vue:505-520`。
- **后端事实**：
  - App Provider 在服务端排除 Dormant 客户，按角色范围查询，并一次组装合同、项目、金额、停留天数：`statistic/service/workbench/sign/page/support/Page.java:76-168`。
  - 通用 `quote/designer/page` 只按 Designer 创建人过滤，没有 Dormant/签约队列语义：`quote/service/quotes/designerpage/support/Page.java:79-118`。
- **业务影响**：
  - 200 条以外任务可能消失；Dormant 客户仍可能在 Web 显示。
  - 报价到合同的停留时间和排序与 App 不一致。
  - 200 次合同详情请求形成 N+1，首屏变慢后更容易误判为空。
- **建议方向**：Web 改用同一 `statistic/workbench/sign/page`，页面仅转换布局；不得在前端重新定义队列。
- **用户裁决（2026-10-23）**：工作队列与报价历史拆成两个页面/能力；“待我签约”使用 `statistic/workbench/sign/page`。
- **复验条件**：同一设计师准备 ≥201 条已接受报价、含 Dormant、含 PendingSign/Signed/Effective，App/Web 队列总数、对象集合、顺序、下一步和金额完全一致。

### DAW-03 · P1 · Web 待谈单风险天数与 App/BR-005 不一致

- **用户场景**：设计师按“待我谈单”的风险颜色决定今天先催哪一单。
- **App 事实**：
  - 使用 `statistic/workbench/quote/page`：`th4-decoboon-app/src/domains/workbench/api.ts:631-635`。
  - Submitted≥3、Rejected≥1、Draft≥7 标红：`subpackages/quote/pages/workbench.vue:73-76`。
  - 后端以 Submitted 的 `submittedAt`、Rejected 的 `rejectedAt` 作为 stage start：`statistic/service/workbench/quote/page/support/Page.java:93-123`。
- **Web 事实**：
  - 使用 `quote/designer/page({size:200})` + 前端过滤：`DesignerQuoteListView.vue:365-380`。
  - 所有状态都用 `quote.createdAt` 计算停留：`DesignerQuoteListView.vue:167-198`。
  - Submitted 只有 ≥7 天才标“等待较久”，与 App/BR-005 的 3 天不一致：`DesignerQuoteListView.vue:197`。
- **业务影响**：同一报价在两端风险不同，Web 可能漏掉已经停留 3 天的待确认报价，违背设计师工作台核心目的。
- **建议方向**：改用服务端 QuoteWorkbench Provider 的 `stayDays/submittedAt/lastFeedbackAt`；阈值只保留一处（服务端风险字段或共享配置），禁止 Web 重新算。
- **用户裁决（2026-10-23）**：待我谈单与报价历史拆分；“待我谈单”只消费 `statistic/workbench/quote/page`，`quote/designer/page` 仅作报价历史。
- **复验条件**：固定 Draft/Submitted/Rejected 各 6/7/8 个样本，App/Web 的 stayDays、风险颜色、排序和“下一步”一致。

### DAW-04 · P1 · 设计师工作台/业绩指标使用两套聚合 Provider

- **用户场景**：设计师打开 App 工作台和 Web 业务工作台/我的业绩，核对本月签约、报价、在途客户和待办。
- **App 事实**：App 首页/业绩调用 `statistic/designer/report`：`th4-decoboon-app/src/domains/statistic/api.ts:628-644`；首页设计角色分支在 `pages/index/index.vue:459-467`。
- **Web 事实**：业务工作台调用 `statistic/workbench/designer/report` 与 `.../queue/list`：`th4-decoboon-web/src/domains/workbench/api.ts:16-23`；业绩页复用同一新 Provider：`useDesignerPerformance.ts:95`。
- **后端事实**：两者是不同 Provider，分别由 `statistic/service/designer/report/support/Report.java` 和 `statistic/service/workbench/designer/report/support/Report.java` 实现，指标组装路径不同。
- **业务影响**：同月、同员工、同数据范围内，KPI、漏斗、排行、在途客户和待办数量可能出现两个版本；且没有字段级一致性断言。
- **建议方向**：明确唯一 canonical Provider。若 `statistic/workbench/designer/report` 是新标准，App 工作台/业绩必须切换；若不是，Web 必须回退到 App Provider。两套 Provider 在过渡期至少建立字段级对比测试。
- **复验条件**：同员工同日数据下，两端本月报价金额、签约金额、在途客户、待客户确认数、合同交接数、项目数、趋势点和排行榜逐项相同。

### DAW-05 · P1 · Web 从项目事实进入合同会丢失 projectId/contractId

- **用户场景**：设计师在 Web 项目 PHASE 事实中点击“合同”，希望打开这个项目绑定合同；实际合同交接页默认展示队列第一项或空。
- **Web 事实**：
  - `ProjectExecutionView.factRouteTarget()` 对 `Contract` 生成 `designer-contract` 路由，并带 `projectId/contractId`：`src/views/workbench/ProjectExecutionView.vue:193-194`。
  - `DesignerContractHandoffView.load()` 只读取 `route.query.quoteId`，不读取 projectId/contractId：`src/views/designer/DesignerContractHandoffView.vue:521-527`。
  - `syncQuery()`虽然会写 contractId，但 load 不恢复该锚点：同文件 `427-446`。
- **App 事实**：App `openFactNode('Contract')` 使用 `projectId + contractId` 打开合同页；App `contract.vue` 同时读取两参数：`th4-decoboon-app/src/subpackages/project/pages/detail.vue:258-292`; `contract.vue:68-83`。
- **业务影响**：设计师从项目事实核对合同时会看错合同，或被迫重新在队列中查找客户；破坏“对象 → 事实 → 精确跳转”的统一对象视图。
- **建议方向**：合同交接页恢复 projectId/contractId 锚点，优先加载指定合同；若队列无对应任务，展示“合同存在但当前账号不可见”的明确状态，不得默认第一项。
- **复验条件**：从 App/Web 项目事实各打开同一合同，合同号、报价版本、金额、状态、所属项目一致；无 quoteId 时必须按 contractId 命中。

### DAW-06 · P1 · 设计模板方案库已裁决双端完整，App 仍缺失

- **用户场景**：设计师在 Web 新建个人模板、维护区域和公式并发布；回到 App 现场需要查看、复制或实例化同一模板。
- **业务事实**：`BR-040` 明确 App/Web 双端必须提供等价查看、编辑、发布、停用、复制和实例化闭环；`DEC-084` 已覆盖旧 Web-only 口径。
- **当前缺口**：`PI-20261022-004` 已记录“Web 有完整管理；App 当前无入口”，并列出 `EOP-D10a/EOP-D10b` 待实现（`docs/product/issue-governance/issues/PI-20261022-004-企业主数据经营标准与设计模版边界.md:63-65,103-105`）。
- **App 事实**：`src/config/index.ts:125-152` 无模板路由；`roles/guard.ts:22-29` 无模板守卫；源码无 `designtemplate` 领域。
- **Web 事实**：`routes.ts:86-87` 含模板库/编辑器；`workbenchConfig.ts:187-190` 含“方案模版”；`domains/designtemplate/api.ts` 覆盖 page/info/save/publish/copy/disable/version/instantiate。
- **业务影响**：设计师无法在 App 完成已冻结的标准化能力；这直接违反用户 2026-10-22“App/Web 双端完整”裁决。
- **用户裁决（2026-10-23）**：**App 端先不做**；DAW-06 标记为 `DEFERRED_BY_USER`，Web-only 现状仅作为暂缓范围，不得宣称为双端完成。
- **后续方向**：暂不新增 App 模板路由、页面或 API 封装；恢复时按既有 EOP-D10a/D10b 切片补齐，不重新开新接口。
- **复验条件**：个人级模板新建、编辑、发布、停用、复制、实例化在 App/Web 均有入口；企业级/门店级维护对 Designer 均为 403；同一房屋+版本重复实例化幂等；历史报价不因模板修改变化。

### DAW-07 · P1 · AI 效果图能力 App 有、Web 无

- **用户场景**：设计师在为方案选效果图时，App 可发起 AI 生成、预览多版本并确认封面/区域图；Web 只能维护方案，无法继续。
- **App 事实**：`config/index.ts:130` 有 `designGeneration`；`roles/guard.ts:27` 允许 `edit:design`；`ai-generation.vue` 调用 `design/generation/build|info|page|update`，包含生成失败、轮询、版本对比和确认。
- **Web 事实**：`routes.ts:65-88` 与 `workbenchConfig.ts:159-193` 无 generation 路由/菜单；`DesignerDesignWorkspaceView.vue` 只消费 design/scheme、space/item、talk、quote，没有 generation API。
- **业务影响**：Web 设计师无法在桌面完成 App 已有的 AI 效果图闭环；若客户在 Web 做大屏谈单，仍要回 App 补图。
- **分类**：若 AI 效果图确认属于正式设计事实，则必须是双端完整；若仅作为 P1 辅助能力，至少必须明确 Web 只读/不可用并给出“回 App 处理”的恢复路径，不能无提示消失。
- **复验条件**：同一方案在 App/Web 生成的任务 ID、版本、选中封面/区域图、错误码和确认结果一致；Web 缺失时显示明确回退路径而非空白。

### DAW-08 · P1 · 量房事实在 App 仅实现部分字段

- **用户场景**：设计师在量房现场既要存原始结构、尺寸、客户诉求、设计师结论和结构图/附件，也要存原始区域面积/周长。
- **App 事实**：`measure-record.vue` 的保存 payload 只有 `propertyId/measureTime/completedAt/sitePhotos`，并在有区域时保存 `measureRegion`：`th4-decoboon-app/src/subpackages/customer/pages/measure-record.vue:159-200`。
- **Web 事实**：`DesignerMeasureTasksView.saveMeasure()` 与 `DesignerCustomersView.saveMeasureResult()` 写入 `originalStructure/dimensions/customerRequirements/designerConclusion/sitePhotos/structureImages/attachments`：`th4-decoboon-web/src/views/designer/DesignerMeasureTasksView.vue:588-635`; `DesignerCustomersView.vue:854-869`。
- **后端事实**：`property/measure/save` 接收并保存这些字段：`property/service/measures/save/support/Save.java:46-57,100-130`。
- **业务影响**：Web 产生的量房事实在 App 不可编辑/不可补录，App 产生的量房事实在 Web 显示“待补充”；同一 Measure 两端字段完整度不同。是否是“允许的 Web 增强”尚未见业务裁决。
- **建议方向**：先由业务确认量房四类字段是否为统一业务事实；若为统一事实，App 必须补齐同等写/读，若只作为 Web 记录增强，需明确不可用边界和只读展示。
- **复验条件**：同一量房记录在 App/Web 都能看到相同字段；App 写、Web 读、Web 写、App 读双向一致；量房完成后区域冻结规则一致。

### DAW-09 · P1 · Web 客户 360 与部分队列用固定条数后前端查找

- **用户场景**：客户数量超过 100、项目超过 200 后，设计师从“我的客户”进入某个客户 360，页面可能显示“不在可见范围”；项目事实缺项。
- **Web 事实**：
  - `DesignerCustomer360View.loadCustomer()` 拉 `myCustomerOverview({size:100})` 后在前端 `find`：`src/views/designer/DesignerCustomer360View.vue:205-216`。
  - `loadStructure()` 拉 `projectApi.listProjects({size:200})` 后按 `customerId` 前端过滤：同文件 `220-232`。
  - `DesignerFollowUpTasksView.load()` 同样只取 `myCustomerOverview({size:100})`：`src/views/designer/DesignerFollowUpTasksView.vue:343-357`。
- **App 事实**：客户详情直接调用 `customer/customer/info`，项目按 `customerId` 调 `project/project/page`：`th4-decoboon-app/src/subpackages/customer/pages/detail.vue:320-340`。
- **业务影响**：同一个可见客户在列表中存在，进入 360 却提示无权限；已签约项目在该客户 360 中消失。属于“前端分页无法证明全量”的确定性完成错误。
- **建议方向**：客户 360 直接调用客户详情 Provider；项目按客户/房屋服务端过滤，或使用明确分页；“未搜索到”必须和“无权限/数据未加载完整”分开。
- **复验条件**：准备 101 位客户、201 个项目，选择最后一位客户与最后一个项目，App/Web 均能命中；断网/超时显示“暂不可确认”而非“无数据”。

### DAW-10 · P1 · 增减项发起存在行级与非行级两套语义

- **用户场景**：设计师在报价详情和历史项目页面都能“发起变更单”，但一个入口只让填金额/原因，另一个入口要求选择合同行、目标、数量和单价。
- **App 事实**：
  - 报价详情走旧 `tradeApi.createContractChange(contractId, amount, reason)`：`th4-decoboon-app/src/subpackages/quote/pages/detail.vue:336-365`。
  - 该请求映射 `project/contract-change/save`：`src/domains/trade/api.ts:329-345`。
  - App 新路径 `change-create.vue` 则使用 `range/preview/batch-save`：`subpackages/project/pages/change-create.vue:180-200,434-500`。
- **Web 事实**：`DesignerContractChangesView` 使用 `contractChangeRange`、`previewContractChange`、`batchSaveContractChange`：`src/views/designer/DesignerContractChangesView.vue:562,592,805`。
- **后端事实**：旧 `project/contract-change/save` 仍生成单条 `ContractChangeItem`，可无 `sourceLine`：`project/service/contract-change/save/support/Save.java:142-169`；新批量路径才核对当前有效剩余和待审批占用。
- **业务影响**：同一变更可以从“无来源行”的旧表单进入，绕过新规则要求的逐行来源、可变更量和金额核对；审计/下游材料/任务重算语义可能不完整。
- **建议方向**：将报价详情“发起变更单”统一跳转到 `change-create`；旧 `contract-change/save` 若仅为兼容历史，应标记 legacy、限制入口，不能继续作为设计师新路径。
- **复验条件**：从报价详情、项目详情、Web 变更工作区发起同一变更，得到同一来源行、同一差额、同一预览金额、同一审批/生效结果；无来源行请求被 400/403 拒绝。

### DAW-11 · P2 · 概念与状态文案未统一

- **用户场景**：设计师接受同一培训、做同一判断，但在 App/Web 看到“待客户确认/待客户决策”“报价池/报价工作台”“方案完成/待谈单”等不同语言。
- **证据**：
  - App `Quote.STATUS_META.Submitted = 待客户确认`：`subpackages/quote/pages/detail.vue:49`。
  - Web 报价列表筛选为 `待客户决策`：`DesignerQuoteListView.vue:32`；客户 360 也是 `待客户决策`：`DesignerCustomer360View.vue:41`。
  - 后端 `QuoteStatus.Submitted.getName() = 已提交`：`quote/configure/QuoteStatus.java`。
  - Web `CONTRACT_STATUS_LABELS` 缺 `void`，App 映射含 `void`；后端 `ContractStatus` 无 `Void`。
  - 设计后端 `Draft("草稿")`，App/Web 都显示“设计中”。
- **业务影响**：新员工需要两套词典；报表筛选和口头沟通容易混淆；状态名一旦被产品文档引用会形成多版本口径。
- **建议方向**：建立设计师专用概念表，区分“内部枚举名、员工 UI 名、业主 UI 名”；同义不同名只允许有明确 `conceptKey`。
- **复验条件**：扫描 App/Web 状态映射，所有同一 `conceptKey` 的 label 一致或登记为端侧差异；`void` 等不存在枚举不得留在正式映射。

### DAW-12 · P2 · Web 客户意见投诉的菜单入口与首页卡片口径不一致

- **用户场景**：设计师在左侧菜单能看到“客户意见投诉”，但在首页工作台卡片中该入口显示“待确认”且不可点击。
- **证据**：
  - 菜单 `workbenchConfig.ts:184` 固定加入 `/workbench/customer-feedback`。
  - 首页 `workbenchHome.ts:893-905` 对该能力使用 `decisionStatus: 'pending_business_review'`，并将 `to` 设为 `null`，注释说裁决前不给入口。
- **业务影响**：同一角色、同一页面，导航与首页卡片冲突；用户会以为菜单是有效入口或首页是坏链接。
- **建议方向**：统一到同一能力裁决结果：若 GAP-WEB-015 未裁决，菜单也应明确“待确认”或仅只读；若页面已可用，首页卡片恢复同一路由。
- **复验条件**：Designer 登录后，菜单、首页卡片、直接 URL、服务端权限四处一致；未裁决状态不伪装为完整能力。

---

## 8. Web 扩展边界

### 8.1 允许的纯大屏交互增强

| 扩展 | 依据 | 边界 |
| --- | --- | --- |
| 三栏客户/量房/报价/合同工作面 | `REQ-BIZ-20261002-001` 已确认设计师最后一公里 Web 可更重 | 只改变布局、密度和并行浏览；事实/动作/状态必须同源 |
| 报价表格、批量改价、批量删除、Excel 导入/导出 | `ANS-BIZ-20261002-002` 明确 App 单行兼容、Web 可做结构化工作簿 | 正式金额、公式、成本、版本仍由服务端权威重算；不得暴露成本 |
| 版本轨、版本对比、快捷键 | 报价工作空间裁决 | 版本语义必须与 App/合同绑定版本一致 |
| 客户 360 对房屋/方案/报价/合同/项目/量房/付款的统一查看 | 统一对象视图标准 | 宿主页只显示摘要和跳转；不得复制第二套明细 |
| 模板编辑器、桌面端密度和批量操作 | `DEC-084` 双端完整、交互可不同 | App 仍必须有等价动作，只是移动交互可简化 |
| PDF/Excel 文件选择、宽屏打印、键盘操作 | 端侧交互差异 | 归档/版本/字段白名单必须后端同源 |

### 8.2 必须与 App 完全同源的语义

1. 客户、房屋、量房、设计、报价、合同、项目、选材、变更、验收的对象 ID 和归属关系。
2. 状态机、可执行动作、动作前置条件、下一步责任人、阻断原因。
3. 报价/合同金额、成本完整性、利润 `null` 语义、PDF 字段白名单。
4. 需求/诉求/画像是“客户事实”还是“内部判断”，以及谁能修改。
5. Provider 错误码、403/404 语义和恢复路径。
6. 写操作成功后的服务端事实回读。

### 8.3 可能破坏存量用户习惯、需用户确认

1. 将 App 的“待我报价/报价池”改成 Web 的“待我谈单/报价工作台”。
2. 把 App 设计/报价/签约三个独立工作台合并为 Web 的大工作区（可合并入口，不能合并状态语义）。
3. 给 Web 新增 App 未有的写动作（当前最典型是设计师提交阶段验收），即使后端已允许，也必须先确认 App 基线是否升级。
4. 把 App 的量房/设计/报价/合同/项目页面统一重定向到 Web 式对象页。
5. 引入跨端不同的“当前版本/主体”默认选择，尤其是报价和合同。

---

## 9. 待用户确认的问题

### Q1. 设计师阶段验收的提交边界

- 已确认：设计师允许提交阶段验收。
- 实施时必须同时确认：仅授予 `act:acceptRequest`，不授予 `act:acceptConfirm`；后端按 `AcceptanceDone` 和项目参与关系校验。
- 仍需冻结：设计师是否可提交所有复验轮次，还是仅首验；本报告建议首验和复验都走同一提交动作，由后端按同一验收单追加轮次。

### Q2. 设计模板 App 暂缓范围

- 已确认：App 端先不做。
- 当前状态：Web 模板能力保留；App 模板能力仍缺失，标记 `DEFERRED_BY_USER`，不能用于“App/Web 已对齐”的验收结论。
- 恢复时需要重新确认该暂缓是否覆盖 `EOP-D10a`（查看/实例化）和 `EOP-D10b`（编辑/发布/停用/复制）。

### Q3. 设计师工作台和我的业绩的 canonical Provider 是哪一个？

- App：`statistic/designer/report`。
- Web：`statistic/workbench/designer/report` + `queue/list`。
- 需要确认哪一个作为统一口径；如果 Web 新 Provider 是替代标准，App 必须切换并补对账；如果 App 是基线，Web 必须回退。

### Q4. “待我报价/待我谈单”“合同签约/待我签约/合同交接”的正式用户语言

- 当前两端都有代码证据，但不是同一套词。
- 需要确认采用哪组主术语，并明确哪些是状态、哪些是页面名、哪些是动作名。

### Q5. Web 量房扩展字段是否要回补 App

- `originalStructure/dimensions/customerRequirements/designerConclusion/structureImages/attachments` 是否为统一量房事实。
- 若是，App 必须补齐；若否，Web 需标注为 Web-only 记录增强并明确 App 只读展示边界。

### Q6. 客户 360 与项目事实的默认入口

- App 更偏“客户详情 + 项目详情事实”；
- Web 更偏“客户 360 + 项目 PHASE + 独立合同工作台”。
- 需要确认是否保留两条入口，以及从项目事实点击合同时是否必须保持 `projectId + contractId` 深链（本报告认为必须）。

---

## 10. 验证状态、证据与下一步

### 10.1 当前可确认

- 设计师职责与边界、合同发起/确认分离、报价接受登记、变更审批生效、阶段验收业主确认等业务口径有正式卡或用户决策。
- 客户、房屋、量房、方案、报价详情、合同详情、项目、选材、变更、验收的主体 Provider 大多存在，且 App/Web 多数复用同一路径。
- Web 报价工作区已经改为消费服务端 `regionGroups/typeGroups`，没有前端合并金额；这是正确方向。
- 合同 `Signed` 与 `Effective` 分离、项目只能由生效合同生成，后端和两端页面基本一致。

### 10.2 不能确认

- 任一未提交 Web/App 页面是否真实可登录、可操作、可回读。
- 生产数据下 100/200 条边界之后的客户/项目/报价/合同队列是否正确。
- Designer 的验收写能力最终业务口径。
- 设计模板 App 闭环是否已在并行工作区实现（当前未见源码，且 `PI-20261022-004` 仍记录缺口）。
- 所有报告项的真实跨企业、跨门店、无参与关系、断网/OSS 回源和 PDF 归档行为。

### 10.3 建议的下一批只读复验顺序

1. 先关闭 DAW-01 的规则问题；未裁决前不修改 App/Web 权限。
2. 对 DAW-02/03/04 做 Provider 契约对账：固定样本、固定金额、固定状态，比较 App/Web 返回值。
3. 修复 DAW-05 的深链并验证从项目事实进入合同。
4. 把 DAW-06 纳入既有 `PI-20261022-004` 的 `EOP-D10a/D10b`，不要另起重复需求。
5. 对 DAW-09 做大数据量分页/搜索验收。
6. 对 DAW-10 做单一变更入口验收，避免继续保留旧金额-only 路径。
7. 最后再做角色×动作×状态×金额×数据的矩阵验收；真实页面证据不能由编译/构建替代。

### 10.4 本报告实际读取的证据入口

后端与领域：

- `AGENTS.md`、`rules.md` §0.1/§0.2/§0.6/§4/§5/§5.6/§13
- `.agents/skills/domain-delivery-orchestrator/SKILL.md`、`references/protocol.md`
- `docs/operations/试运营单人轻量协作与发版规范.md`
- `/Users/zhangwei/.agents/skills/zhuangdeqing-business/SKILL.md`
- 业务事实源：`OBJ-001`、`OBJ-005`、`OBJ-007`、`BR-001`、`BR-002`、`BR-005`、`BR-008`、`BR-040`、`BR-052`、`BR-054`、`SCN-001`、`SCN-002`、`SCN-003`、`SCN-007`、`DEC-040`、`DEC-041`、`DEC-052`、`DEC-053`、`DEC-062`、`DEC-069`、`DEC-071`、`DEC-078`、`DEC-084`、`DEC-086`、`DEC-087`、`DEC-088`
- `01a-role-matrix-v0.2.md`、`02d-biz-operations-a-customer-measure-v0.1.md`、`02e-biz-operations-a-design-quote-contract-v0.1.md`、`02f-biz-operations-b-project-delivery-v0.1.md`
- `03-quote-unified-view-concept-v0.1.md`、`04-unified-business-object-view-standard-v0.1.md`、`05-project-fact-trace-design-v0.1.md`、`06-contract-fact-trace-design-v0.1.md`、`10-reconciliation.md`、`12-uov-v5d-web-project-phase-plan-v0.1.md`
- 当前后端 Provider/Context/枚举：`CustomerContext`、`ProjectAccessContext`、`DesignContext`、`DesignCostContext`、`ProjectFactTraceContext`、`ContractWorkbenchContext`、`QuoteStatus`、`DesignStatus`、`ContractStatus`、`ProjectStatus`、`MeasureStatus`、`statistic/workbench/{design,quote,sign,designer}/*`

App：

- `AGENTS.md`
- `src/pages.json`、`src/config/index.ts`、`src/roles/catalog.ts`、`src/roles/permission.ts`、`src/roles/guard.ts`
- `src/pages/{index,customer,project,mine}/*`
- `src/subpackages/customer/pages/*`、`src/subpackages/design/pages/*`、`src/subpackages/quote/pages/*`、`src/subpackages/project/pages/*`
- `src/domains/{crm,property,design,trade,project,acceptance,site,workbench,statistic}/*`

Web：

- `AGENTS.md`
- `src/core/router/index.ts`、`src/roles/routes.ts`、`src/roles/permission.ts`、`src/roles/workbenchConfig.ts`、`src/roles/workbenchHome.ts`、`src/layouts/DesignerLayout.vue`
- `src/views/designer/*`、`src/views/workbench/ProjectExecutionView.vue`
- `src/domains/{crm,property,design,trade,contract,project,acceptance,designtemplate,workbench,document,finance}/*`

核对原则：

- 业务规则优先于代码现状；
- 代码现状只作为实现证据；
- 页面入口只作为可达性证据；
- 验收记录若不包含真实页面、真机、权限负向和金额回读，不升级为客户可用；
- 未提交工作树中的改动不与已验收基线混写。

### 10.5 后续执行范围锁定（2026-10-23）

状态：`PLAN_LOCKED / CODE_NOT_STARTED`

`DAW-FIX-01` 的实施级计划见：`roles/daw-fix-01-designer-acceptance-plan-v0.1.md`。

唯一目标：

```text
让 Designer 在 App/Web 具备一致的阶段验收提交能力；
让 Web 的待我谈单、报价历史、待我签约各自消费正确且同源的能力；
不在本批扩展设计模板 App 能力。
```

本批允许进入方案设计的范围：

| 切片 | 范围 | 明确产物 |
| --- | --- | --- |
| `DAW-FIX-01` | Designer 阶段验收提交 | App 能力键和入口对齐、后端 `AcceptanceDone`/项目参与/任务/证据/幂等门禁、App/Web 同口径回归 |
| `DAW-FIX-02` | Web 报价与签约工作队列 | “待我谈单”消费 `statistic/workbench/quote/page`；“报价历史”消费 `quote/designer/page`；“待我签约”消费 `statistic/workbench/sign/page`；取消前端风险重算和 200 条硬截断 |

本批明确排除：

1. App 设计模板列表、编辑、发布、复制、实例化；状态保持 `DEFERRED_BY_USER`。
2. 其他角色（Boss/店长/PM/业务员/财务/业主）的角色能力改造。
3. 报价、合同、成本、利润、支付或数据删除的公式和语义变更。
4. DDL、迁移、生产数据修复。
5. 合同事实深链、量房字段补全、AI 效果图 Web 化等相邻问题；保留为后续专项，不借本批顺手扩展。
6. 页面视觉重构；本批只处理与上述两项同源语义直接相关的页面状态、入口和错误恢复。

不可破坏的约束：

- 只允许 Designer 获得 `act:acceptRequest`，不得获得 `act:acceptConfirm`。
- 阶段验收最终仍由业主本人确认/驳回；任何员工不得代录。
- 待我谈单和报价历史不是同一能力；报价历史不得出现在待我谈单工作队列的分母中。
- 待我签约必须使用签约工作台 Provider，不得以通用报价列表加前端筛选替代。
- 风险等级、停留天数、下一步和阻断原因由服务端权威给出，前端只展示。
- 不新增同义后端接口；不复制 Web 与 App 各一套业务规则。

本批完成的最低证据：

- Designer/Pm/Boss/StoreManager/业主五类主体的正反向权限矩阵。
- `None / ConstructionDone / AcceptanceDone` 三种阶段验收模式。
- Draft/Submitted/Rejected/Accepted/Converted 与 Dormant 的队列集合对账。
- 201 条以上数据的截断测试。
- App/Web 同一对象的状态、金额、风险、下一步一致。
- 真实页面/真机、跨企业/跨门店、接口失败恢复和写入后回读。
- 权限相关变更按高风险门禁执行备份、回滚和完整回归。

### 10.6 DAW-FIX-02 真实页面复核（2026-10-23）

状态：`UI_REVIEWED / USER_WEB_ACCEPTANCE_PENDING`

已完成：

- Web「待我谈单」改为独立工作队列页 `/designer/quotes`，消费 `statistic/workbench/quote/page`；
- Web 新增独立「报价历史」页 `/designer/quote-history`，消费 `quote/designer/page` 的服务端分页；
- Web「待我签约」改为消费 `statistic/workbench/sign/page`，不再使用通用报价列表并逐条查询合同详情；
- 签约队列选中一项后仅按需读取该合同详情，不做 200 条合同 N+1；
- 后端 `QuoteWorkbenchItem`、`SignWorkbenchItem` 增加服务端 `riskLevel/riskReason/nextActionKey/nextActionLabel`；
- 后端报价工作队列增加 StoreManager 本店范围；
- 新增 `DesignerWorkbenchRisk`，统一报价与签约停留风险、下一步和风险文案；
- 真实页面复核发现并修复 `DAW-FIX-02-UI-01`：Effective 无项目时，Provider 原返回「查看合同」，页面却自行推导「合同已生效待项目建立」。修复后 Provider 返回 `waitProjectCreated` /「合同已生效待项目建立」，Designer 只读主行动改为消费服务端 `nextActionLabel`。

真实页面证据：

- Designer 待我谈单：Draft/Submitted 混杂队列、17/4 天风险、筛选、搜索、选中联动、报价工作区跳转均 PASS；
- 报价历史：请求 `{current:1,size:20}`，23 条返回 20+3 两页；第 2 页、`status=Draft` 筛选和搜索均回源服务端；
- 待我签约：`createContract / waitEnterpriseSign / waitEnterpriseEffective / waitProjectCreated / viewProject` 状态链路已覆盖；选中后只读取当前合同详情；
- StoreManager：本店队列 2 条、历史 8 条、签约 6 条；跨店临时报价和 Accepted 报价均未返回；`/designer/quote-history` 可达；
- Designer 队列、历史、签约在 1024 下无横向溢出；1440/1024 截图与 console 0 error 已归档。

验证：

- `mvn -Dtest=DesignerWorkbenchRiskTest test`：2/2 PASS；
- Web `npm run type-check`：PASS；
- Web `npm run build`：PASS；
- Web `check:actions`、`check:permissions`、`check:workbenches`、`check:workspaces`：PASS；
- App `type-check`、`build:mp-weixin`、`check:action`、`check:contract-sign`、`check:navigation`：沿用既有 PASS。

证据入口：

- `docs/acceptance/runs/20261023-designer-web-ui-review/README.md`
- `docs/acceptance/runs/20261023-designer-web-ui-review/ui-checks.json`
- `output/playwright/20261023-designer-web-acceptance/`

仍待验证或未纳入本批：

- 用户本人 Web 验收；研发侧页面复核不能升级为 `ACCEPTED`；
- 独立验收；
- 合同事实 `projectId/contractId` 深链仍是后置问题；
- 设计模板 App 仍为 `DEFERRED_BY_USER`；
- DAW-FIX-01 的 `None / ConstructionDone / 空 acceptanceMode` 全模式完成路径仍未闭合。

### 10.7 DAW-FIX-02 200+ 分页与 Dormant 排除（2026-10-23）

状态：`UI_REVIEWED / USER_WEB_ACCEPTANCE_PENDING`

本轮已关闭：

- `statistic/workbench/quote/page` 从数组改为服务端 `Page<QuoteWorkbenchItem>`；
- `statistic/workbench/sign/page` 从数组改为服务端 `Page<SignWorkbenchItem>`；
- 两个 Provider 接受 `current / size / keyword / filter`，签约队列支持 `quoteId` 自动定位所在页；
- App 报价/签约工作台与 Web「待我谈单」「待我签约」同步消费服务端分页；
- Dormant 客户在队列组装阶段排除，且排除发生在分页之前，不进入总数、筛选结果和页面 DOM。

真实 Web 证据：

- 待我谈单：214 条、11 页；第 2 页正常；`filter=draft` 返回 75 条且全部为草稿；
- 待我签约：214 条、11 页；`filter=toCreate` 返回 200 条、10 页；
- 搜索 `TMP-DORMANT-QUEUE` / `TMP-DORMANT-SIGN`：接口总数 0，页面不显示；
- 1440 与 1024 均无横向溢出，console 0 error。

验证：

- `DesignerWorkbenchAggregationTest + DesignerWorkbenchPaginationTest + DesignerWorkbenchRiskTest`：11/11 PASS；
- Web type-check/build/actions/permissions/workbenches/workspaces：PASS；
- App type-check/build:mp-weixin/action/contract-sign/navigation：PASS。

证据：

- `docs/acceptance/runs/20261023-designer-queue-pagination/README.md`
- `docs/acceptance/runs/20261023-designer-queue-pagination/ui-checks.json`
- `output/playwright/20261023-designer-web-acceptance/designer-quotes-pagination-214-*.png`
- `output/playwright/20261023-designer-web-acceptance/designer-contract-pagination-*.png`

边界：当前 Provider 在完整候选集完成 Dormant 过滤、风险排序后切片分页，解决了 200 条漏单和客户端截断；若后续队列量级显著增长，再单独将筛选、排序和分页下推 SQL，不在本轮扩大范围。

---

## 11. 本报告结论一句话

**App 已形成设计师从量房到项目协作的主链基线；用户已确认设计师可提交阶段验收，Web 已把待我谈单、报价历史、待我签约拆成独立能力，并完成 200+ 分页与 Dormant 排除的真实页面复核。当前仍需用户本人 Web 验收、独立验收和阶段验收全模式门禁；App 设计模板已由用户明确暂缓，不能写成双端完成。**
