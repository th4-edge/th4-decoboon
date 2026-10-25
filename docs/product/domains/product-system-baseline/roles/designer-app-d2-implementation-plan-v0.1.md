# DESIGNER-D2 · 设计师 App 信息架构与五阶段 Provider 对账计划 v0.1

> 日期：2026-10-24  
> 状态：`PLAN_READY / CODE_NOT_STARTED`  
> 问题档案：`docs/product/issue-governance/issues/PI-20261024-001-设计师App信息架构与五阶段队列同源.md`  
> 决策来源：`roles/designer-productization-v0.2.md` 的 `DPR-D01`～`DPR-D09`  
> 本批目标：先完成 App 导航壳层与 Provider 对账，不把 Web 三栏布局搬到 App。

## 1. 目标与非目标

目标：

```text
设计师 App 一级导航与三块职责一致：
工作台 / 客户 / 签单 / 项目 / 我的

五个业务阶段使用同一对象、同一 Provider、同一状态与下一步：
待我跟进 / 待我量房 / 待我设计 / 待我谈单 / 待我签约
```

非目标：

- 不新增 Designer 权限；
- 不改变金额、成本、利润、合同确认/生效主体；
- 不创建 App 专用业务 Provider；
- 不重做设计、报价、项目详情主链；
- 不把 Web 三栏工作台搬到 App；
- 不实现设计模板；
- 不实现主材采购下单；
- 不在本计划内完成合同只读收口 `D3`。

## 2. 当前快照

2026-10-24 00:55 工作区快照：

| 项 | 事实 |
| --- | --- |
| App Tab | 仍为 `工作台/设计/报价/项目/我的` |
| 客户入口 | 可从首页进入，但不是 Designer Tab |
| 量房候选页 | 已有 `designer-measure-workbench.vue`，尚未并入新 IA 验收 |
| 设计工作台 | 客户 + 房屋卡片，单列布局适合 App；接口仍返回数组 |
| 报价工作台 | 已接服务端分页；App 仍本地计算风险和动作 |
| 签约工作台 | 已接服务端分页；包含履约中/已完成；当前作为子页时 Tab 高亮错误 |
| 项目列表 | 服务端分页，但状态筛选只过滤当前页 |
| 合同页 | 项目内可进入，但仍存在“登记收款”等写动作 |

当前工作区有大量并行未提交改动，上述量房候选页只能作为“待复核输入”，不能作为已验证完成项。

## 3. 目标导航

### 3.1 一级 Tab

```text
工作台  /pages/index/index
客户    /pages/customer/list
签单    /pages/sign/index
项目    /pages/project/list
我的    /pages/mine/index
```

说明：

- `客户` 负责客户关系与跟进，不承载报价/合同历史；
- `签单` 是五阶段推进中心，不复制设计或报价编辑页面；
- `项目` 承接合同生效后的履约事实；
- 合同档案始终从项目详情进入；
- `我的` 承载业绩、只读资料库和设置。

### 3.2 签单中心

签单中心首屏只回答“当前五个阶段各有多少待办、哪里最紧急”：

```text
待我跟进
待我量房
待我设计
待我谈单
待我签约
```

每个阶段卡片包含：

- 队列数量；
- 高风险数量；
- 最近一个对象摘要；
- 唯一主入口；
- 接口失败时显示“暂不可确认”和重试。

点击后进入现有单列队列页，不复制页面内容。App 不使用 Web 三栏结构。

### 3.3 多入口上下文

`待我量房` 允许从“客户”和“签单”两个入口进入，但复用同一页面、同一 Provider、同一对象锚点。

进入时携带来源上下文：

```text
from=customer | sign
```

规则：

- 从客户进入：底部高亮“客户”，返回客户经营；
- 从签单进入：底部高亮“签单”，返回签单中心；
- 深链未携带来源：默认按签单上下文处理；
- 不允许出现两个同义量房页面。

## 4. 五阶段 Provider 对账矩阵

| 阶段 | canonical Provider | 当前请求/返回 | App 当前 | Web 当前 | D2 决策 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| 待我跟进 | `customer/my-customer/overview` | `current/size/keyword/stage/sort`；`CustomerOverviewReport` | 未使用该聚合，客户页搜索/筛选在当前页内完成 | 使用聚合但取前 100 条后本地分桶 | 必须增加服务端跟进分桶，或明确不使用“待我跟进”独立队列 | `BIZ_PENDING` |
| 待我量房 | `statistic/designer/measure/page` | `current/size/keyword/state`；`Page<DesignerMeasureTask>` | 已有候选页，消费该 Provider | 已消费同一 Provider | 复核候选实现后保留；双入口复用同一页面 | `CANDIDATE_VERIFY` |
| 待我设计 | `statistic/workbench/design/page` | 无查询参数；`List<DesignWorkbenchItem>` | 全量取数、客户端筛选 | 全量取数、客户端筛选 | 先复用现有对象；入站资格需 T05；分页升级另切片 | `BIZ_PENDING / SCALE_GAP` |
| 待我谈单 | `statistic/workbench/quote/page` | `current/size/keyword/filter/quoteId`；`Page<QuoteWorkbenchItem>` | 分页已接；无搜索；丢弃服务端风险和下一步 | 分页、搜索、风险和下一步已接 | App 补齐字段消费和搜索；不得本地重算 | `CODE_READY` |
| 待我签约 | `statistic/workbench/sign/page` | `current/size/keyword/filter/quoteId`；`Page<SignWorkbenchItem>` | 分页已接；无搜索；本地重算风险；包含履约中/已完成 | 分页、搜索、风险和下一步已接 | App 补齐字段消费和搜索；出站条件待 T05 | `BIZ_PENDING` |

### 4.1 统一消费字段

报价和签约 App 模型必须补齐：

```text
riskLevel
riskReason
nextActionKey
nextActionLabel
```

规则：

- 风险颜色、风险文案、下一步标签只消费服务端值；
- `stayDays` 仅用于事实展示，不在 App 重新换算风险；
- `nextActionKey` 只用于导航，不在前端定义新的状态机；
- 接口缺字段时显示“暂不可确认”，不得默认为正常。

### 4.2 查询与分页规则

- 搜索词、状态、阶段、页码和每页数量全部下推 Provider；
- 阶段计数必须来自服务端聚合，不能由当前页 `filter().length` 冒充全量；
- 筛选条件变化后必须重新请求，而不是只过滤当前数组；
- 接口失败显示错误态和重试，不返回空列表伪装成功；
- D2 不要求所有 Provider 立刻改为数据库级分页，但不得新增前端全量截断。

## 5. 实施切片

### DESIGNER-D2-1 · 导航壳层

范围：

- `src/roles/catalog.ts`
  - Designer Tab 改为 `home/customer/sign/project/mine`；
  - 新增 `TAB.sign`；
  - 旧“设计/报价”Tab 从一级导航移除，但页面保留。
- `src/config/index.ts`
  - 新增 `P.signCenter`；
  - 将签单中心加入 `TAB_PATHS`；
  - 保留旧 `designWorkbench`、`quoteWorkbench`、`signWorkbench` 深链。
- `src/pages.json`
  - 注册签单中心页面。
- 新建 `src/pages/sign/index.vue`
  - 只做五阶段入口、数量/风险摘要和失败恢复；
  - 不复制队列页面。
- `src/components/JzTabBar.vue`
  - 支持阶段页显式声明父 Tab；
  - 不再以找不到路径就默认高亮 `home`；
  - 多入口页面按 `from` 上下文高亮。
- `src/pages/index/index.vue`
  - 首页快捷动作改为五阶段 + 项目履约语义；
  - `待我报价` 收敛为 `待我谈单`；
  - `合同签约` 收敛为 `待我签约`。
- `src/roles/guard.ts`
  - 新签单中心复用现有能力键，不扩大权限。

完成门槛：

- Tab 结构、激活态、返回路径、旧深链重定向全部有测试；
- `check:navigation`、`check:subpackages`、`type-check`、`build:mp-weixin` PASS。

### DESIGNER-D2-2 · 客户与签单上下文

范围：

- 客户 Tab 暴露：我的客户、待我跟进、待我量房、客户诉求；
- 签单中心暴露五阶段；
- `待我量房` 双入口复用同一页面；
- 非来源深链默认签单上下文；
- 阶段页返回上一级，不出现循环跳转。

完成门槛：

- 双入口进入后 URL、对象、Provider 一致；
- 返回路径和 Tab 高亮符合来源；
- 不新增同义页面。

### DESIGNER-D2-3 · Provider 消费对齐

范围：

- App `workbench/model.ts`、`workbench/api.ts` 接收 service 的 risk/nextAction；
- 报价、签约页面移除本地风险计算；
- App 增加关键词输入并下推 `keyword`；
- 客户/项目筛选参数下推 Provider；
- 待我跟进、待我设计的开放问题取得 T05/T12 后单独处理。

完成门槛：

- 同账号同对象 App/Web/后端字段一致；
- 20 条以上分页、跨页搜索结果正确；
- 服务端风险与 App 颜色/文案一致；
- 不修改 Provider 语义时只做消费端对齐。

### DESIGNER-D2-4 · 页面与跨端验收

- App：375×667、390×844；
- Web：1440×900、1024×768；
- 五阶段各准备至少一条真实数据；
- 对照同一 Designer、同一企业、同一对象；
- 检查成功、空态、接口失败、无权限、深链恢复；
- 检查菜单 URL 不携带 `storeId`。

## 6. 文件边界

预计涉及：

```text
th4-decoboon-app/src/roles/catalog.ts
th4-decoboon-app/src/roles/guard.ts
th4-decoboon-app/src/config/index.ts
th4-decoboon-app/src/pages.json
th4-decoboon-app/src/components/JzTabBar.vue
th4-decoboon-app/src/pages/index/index.vue
th4-decoboon-app/src/pages/sign/index.vue
th4-decoboon-app/src/pages/customer/list.vue
th4-decoboon-app/src/pages/project/list.vue
th4-decoboon-app/src/domains/workbench/model.ts
th4-decoboon-app/src/domains/workbench/api.ts
th4-decoboon-app/src/subpackages/design/pages/workbench.vue
th4-decoboon-app/src/subpackages/quote/pages/workbench.vue
th4-decoboon-app/src/subpackages/project/pages/sign-workbench.vue
th4-decoboon-app/src/subpackages/customer/pages/designer-measure-workbench.vue
```

D3 才允许处理：

```text
th4-decoboon-app/src/subpackages/project/pages/contract.vue
```

## 7. 验收矩阵

| 用例 | 预期 |
| --- | --- |
| Designer 登录 | 五个 Tab 为工作台/客户/签单/项目/我的 |
| 点击客户 | 客户页可访问，底部客户激活 |
| 点击签单 | 签单中心可访问，底部签单激活 |
| 从客户进入量房 | 同一量房 Provider，客户激活 |
| 从签单进入量房 | 同一量房 Provider，签单激活 |
| 进入设计/谈单/签约阶段页 | 底部签单激活，返回签单中心 |
| 旧 `/subpackages/design/pages/workbench` 深链 | 可访问且签单激活，或安全重定向 |
| 搜索第 21 条以后客户/项目 | 服务端返回正确结果 |
| 报价/签约风险 | App 与 Web 显示同一等级、原因和下一步 |
| 接口 500 | 显示“暂不可确认”+重试，不显示空队列 |
| 用户无能力键子页 | 明确不可访问，不泄露数据 |
| 菜单和阶段跳转 | URL 不携带 `storeId` |

## 8. 回滚

- 回退 Tab 映射到旧的 `design/quote` 结构；
- 保留新签单中心文件和深链别名，不删除业务页面；
- 回退 App 归一化字段时保留服务端响应兼容；
- 不修改数据、不删除审计、不做迁移；
- 旧深链在回滚后继续可用。

## 9. 开放决策

| ID | 问题 | 阻塞范围 |
| --- | --- | --- |
| `RQ-D2-01` | 待我设计是否必须在已完成量房后入站 | D2-3 设计阶段资格 |
| `RQ-D2-02` | 合同生效并立项后是否立即离开待我签约 | D2-3 签约出站 |
| `RQ-D2-03` | 待我跟进四类分桶是否正式下推服务端 | D2-3 跟进查询 |
| `RQ-D2-04` | 量房双入口是否只以来源上下文区分 | D2-2 激活态与返回 |

## 10. 完成定义

- [ ] 五 Tab 导航和激活态真实可用；
- [ ] 签单中心不复制业务页面；
- [ ] 五阶段 Provider 对账矩阵无未知字段；
- [ ] 报价/签约风险和下一步只消费服务端；
- [ ] 搜索、筛选、分页、计数不落在当前页；
- [ ] 旧深链兼容；
- [ ] App/Web 同账号真实页面证据；
- [ ] 构建、类型、路由、分包门禁 PASS；
- [ ] 独立验收 PASS 后才可宣称 D2 完成。

当前只完成计划，未修改业务代码。
