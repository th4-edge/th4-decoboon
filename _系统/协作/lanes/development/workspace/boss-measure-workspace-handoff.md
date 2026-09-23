# 企业负责人预约量房工作台 开发交接

- 日期: 2026-09-22
- lane: development
- 状态: `implementation_done_self_tested_pending_acceptance`
- 用户口径纠正: 日期维度和报价谈单式工作区应落在「预约量房工作台」，不是客户经营页面；客户页改造保留但暂停讨论。
- P0 产品稿: `../../前端代码/th4-decoboon-app/docs/product/2026-09-22-企业负责人预约量房工作台2.0产品设计.md`

## 变更范围

### 1. 首页与页面命名

- BOSS 首页快捷入口 `量房预约` 改为 `预约量房`。
- `JzIcon` 新增 `ruler` 尺子图标。
- 预约量房页面标题改为「预约量房工作台」；业务员视角为「我的预约量房」。

### 2. 预约量房经营日期维度

`statistic/boss/measure/report` 新增 `periods`，每个窗口同时返回预约量房数和完成量房数：

| key | 文案 | 口径 |
| --- | --- | --- |
| `today` | 日增 | 今天 00:00 至当前时刻 |
| `yesterday` | 昨日 | 昨天 00:00 至今天 00:00 |
| `last7Days` | 近7日增 | 含今天在内的滚动 7 个自然日 |
| `month` | 月增 | 当前自然月 1 日至当前时刻 |

- 预约量房：按房屋级 `Measure.scheduledAt`；
- 完成量房：按 `Measure.completedAt`；
- 不与当前 `scheduledCount/completedCount` 混用：Hero 仍是当前在办池，日期窗口是事件增量；
- 不改预约率、完成率、4h 逾期和 48h 待交设计的当前快照口径。

### 3. 预约量房客户工作区

页面从“横向业务员 Tab + 长列表”改为报价谈单式工作区：

```text
全企业统计总览（固定）
-> 侧边业务员分组
-> 右侧状态条件
-> 右侧客户/房屋卡片内滚动
-> 当前卡片品牌色高亮
-> 放大客户区 / 返回经营概览
```

- 预约量房页只保留业务员分组，不再把客户来源、执行设计师同时铺在作业页；
- 条件仍为全部 / 未预约 / 已预约 / 逾期 / 已完成 / 待交设计；
- 全企业统计固定在页面顶部，包含当前预约、完成、预约率、完成率、风险以及四个日期增量；
- 返回经营概览时强制恢复顶部统计区并抑制滚动监听误判；日期窗口使用 2×2 宫格，不产生横向滚动；
- 客户卡片优化：头像+客户/状态、独立房屋信息、预约时间 chip、责任人标签、下一步提示区和主次操作区，降低小屏信息拥挤。
- 客户卡片操作与风险色再优化：操作区改为右对齐轻量胶囊，主操作浅品牌底、次操作浅灰底；风险左边框改为内缩渐变细条，避免硬红边切割卡片。
- 首页增加经营看板和异常待办：今日预约、待派单、近7日预约、已完成、完成率、交设计率；待派单、预约逾期、待交设计、未预约可点击过滤列表；
- 右侧客户工作区只保留当前业务员的最少作业指标：客户数、待预约、已预约、已完成、风险；
- 业务员的日增/昨日/近7日/月增、预约率、完成率、逾期率和待指派进入「人员绩效考核」页的“业务员量房业绩”区块；
- 列表主按钮按派生状态变化：约定量房、立即派单、改约、查看预约、查看量房记录；次按钮统一为联系客户；
- 立即派单复用客户详情 `designer=change`，不新增派单主对象、不新增接单状态；
- 卡片保留客户、来源、业务员、房屋、预约/完成时间、设计责任人、执行人、照片数量、下一步和操作按钮；
- 不新增路由、不改量房状态机、不改现场执行权限。

## changed_files / output_paths

后端：

- `src/main/java/com/th4/edge/decoboon/statistic/model/BossMeasurePeriod.java`（新增）
- `src/main/java/com/th4/edge/decoboon/statistic/model/BossMeasureReport.java`
- `src/main/java/com/th4/edge/decoboon/statistic/service/boss/measure/report/support/Report.java`

App：

- `src/components/JzIcon.vue`
- `src/roles/catalog.ts`
- `src/domains/workbench/model.ts`
- `src/domains/workbench/api.ts`
- `src/subpackages/customer/pages/measure-workbench.vue`

## verification

- 后端 `compile` PASS。
- `BossMeasureReportContextTest` PASS。
- 后端回归排除 acceptance lane 管理的 `OwnerClosureAcceptanceTest`：79 suites / 222 tests / 0 failures / 0 errors / 0 skipped。
- App `type-check` PASS。
- App `build:mp-weixin` PASS。
- App `check:subpackages` PASS；主包 616698 B，最大分包 302211 B，总包 2068482 B，14 分包 / 87 页。

## risks / blockers

1. 日期增量由当前量房工作台事实集计算；跨日、月初、近7日跨月需要 acceptance 用固定时间夹具独立验证。
2. 当前完成量房事件使用 `Measure.completedAt`，历史 `measureTime` 兼容回填由迁移 48 负责；目标等价库仍需验证迁移 48。
3. 分组切换目前是客户端基于报告已有分组事实切片；报告仍是一次返回全量数据，客户量极大时需要另立服务端分页/聚合方案。
4. 真机上的左右分栏宽度、长业务员/来源名称、内滚动高亮、放大器模式需要独立视觉验收。
5. 准时率、接单率、派单时长、改期/取消原因仍未纳入 P0：现有事实不足，需后续单独裁决并补字段/事件。

## acceptance_needed

- 日增 / 昨日 / 近7日增 / 月增的预约和完成计数分别与 `Measure.scheduledAt/completedAt` 一致。
- 月初、今天无数据、昨天无数据、近7日跨月边界稳定。
- 预约量房页只显示业务员分组，不再出现来源/设计师分组。
- 全企业统计与全量客户事实一致；切换业务员后，客户数/待预约/已预约/已完成/风险与列表事实一致。
- 人员绩效页的业务员量房业绩、四日期窗口和列表事实一致。
- 六类条件在全部分组和指定分组下均可正确过滤。
- 当前卡片高亮、放大客户区和返回经营概览在长列表中可用。
- 其他角色仍只看到自己权限范围内的量房事实。
- 账务、合同、成本、应收应付、结算和利润不变。
