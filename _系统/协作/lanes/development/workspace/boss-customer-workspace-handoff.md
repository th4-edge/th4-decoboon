# 企业负责人首页与客户经营工作区 开发交接

- 日期: 2026-09-22
- lane: development
- 状态: `implementation_retained_pending_product_discussion`（用户已澄清本轮目标其实是预约量房工作台；客户页改造暂不进入验收，后续单独讨论）
- 用户要求: 首页「量房预约」补图标并改名「预约量房」；客户经营增加日增/昨日/近7日增/月增；客户筛选、负责人分组和客户列表同屏，参考报价谈单工作区。

## 变更范围

### 1. 首页入口

- `roles/catalog.ts`：BOSS 首页快捷入口 `量房预约` 改为 `预约量房`。
- `JzIcon.vue`：补充 `ruler` 图标定义，解决入口无图标问题。

### 2. 客户经营指标日期窗口

`customer/customer/overview` 的指标卡增加 `periods` 数组；定义均为既有事实的日期窗口切片，不新增业务 KPI：

| key | 文案 | 口径 |
| --- | --- | --- |
| `today` | 日增 | 今天 00:00 至当前时刻 |
| `yesterday` | 昨日 | 昨天 00:00 至今天 00:00 |
| `last7Days` | 近7日增 | 含今天在内的滚动 7 个自然日 |
| `month` | 月增 | 当前自然月 1 日至当前时刻 |

当前应用于：

- 新增客户：按客户录入时间；
- 有效跟进：跟进记录、到访、量房、交设计等既有活动时间形成去重客户数；
- 签约客户：按合同 `signedAt` 形成去重客户数。

风险客户暂不生成日期窗口：现有模型没有“风险首次形成时间”，不能用当前风险回填历史，避免制造假趋势。

### 3. 客户经营同屏工作区

BOSS 客户页改为：

```text
搜索 / 门店筛选
-> 4 张经营指标卡
-> 左侧客户负责人分组（全部 / 各负责人 / 未分配）
-> 右侧条件（状态、排序） + 客户列表滚动区
```

- 分组按 `Customer.owner` 形成，不把 designer 混为负责人。
- 服务端新增 `ownerId` 筛选：空=全部，`0`=未分配负责人，其余=员工 empId。
- `ownerGroups` 返回负责人、门店、客户数、风险数、待量房数、签约客户数、报价额和签约额。
- 右侧列表继续服务端分页；分组切换、状态筛选、排序、搜索、门店变化都会重新请求，不依赖前端一次性拉全量。
- 滚动客户区时当前卡片使用品牌色边框和阴影高亮；滚动后自动进入客户工作区，支持返回经营概览。

## changed_files / output_paths

后端：

- `src/main/java/com/th4/edge/decoboon/customer/model/CustomerMetricPeriod.java`（新增）
- `src/main/java/com/th4/edge/decoboon/customer/model/CustomerOwnerGroup.java`（新增）
- `src/main/java/com/th4/edge/decoboon/customer/model/CustomerMetricCard.java`
- `src/main/java/com/th4/edge/decoboon/customer/model/CustomerOverviewReport.java`
- `src/main/java/com/th4/edge/decoboon/customer/context/CustomerOverviewContext.java`
- `src/main/java/com/th4/edge/decoboon/customer/service/overview/support/Overview.java`

App：

- `src/components/JzIcon.vue`
- `src/roles/catalog.ts`
- `src/domains/crm/model.ts`
- `src/domains/crm/api.ts`
- `src/pages/customer/list.vue`

## verification

- 后端 `compile` PASS。
- `CustomerOverviewContextTest` PASS。
- `ProviderSecurityAcceptanceTest` PASS，未新增 Provider 路由。
- 后端回归排除当前验收 lane 正在升级的 `OwnerClosureAcceptanceTest`：77 suites / 217 tests / 0 failures / 0 errors / 0 skipped。
- App `type-check` PASS。
- App `build:mp-weixin` PASS。
- App `check:subpackages` PASS；主包 616502 B，最大分包 302211 B，总包 2048887 B，14 分包 / 87 页。

## risks / blockers

1. 指标日期窗口已经冻结为直接日期切片，但尚未由 acceptance 用真实跨日数据核验边界（月初、今天无数据、昨天无数据、近7日跨月）。
2. 负责人分组只按客户负责人，不含设计师维度；如后续需求要按设计师或团队分组，需另立维度，不能复用本字段偷换语义。
3. 风险客户没有历史风险时间，当前不生成日增/昨日/近7日/月增。
4. App 真机上的左右分栏宽度、长负责人名称、客户列表内滚动和底部 Tab/悬浮登记按钮遮挡仍需独立验收。

## acceptance_needed

- BOSS 首页显示「预约量房」且有正确尺子图标。
- 新增客户 / 有效跟进 / 签约客户四组日期窗口与真实领域时间一致。
- 风险客户卡不出现伪造日期趋势。
- 全部、负责人、未分配三个分组切换后，服务端分页和总数正确。
- 搜索、门店、状态、排序、分组叠加后结果一致。
- 长列表内滚动高亮、放大客户区、返回经营概览可用。
- 其他角色客户页保持原列表体验，不受 BOSS 工作区影响。
