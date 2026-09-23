# 客户经营聚合与负责人客户卡开发交接

## Input

- 用户确认：客户 Tab 保持“搜索、指标卡、阶段筛选、客户列表”的统一结构。
- BOSS 视角需要新增客户、有效跟进、签约客户、风险客户四项经营纵览。
- 客户卡片需要负责人、停留天数、风险等级、风险原因、下一步和业务金额投影。
- 设计文档：`docs/design/19-客户经营聚合与负责人客户卡设计.md`。

## Backend

### Provider

```text
customer/customer/overview
```

仅允许 BOSS。请求字段：

- `current`
- `size`
- `keyword`
- `stage`
- `sort`
- `storeId`

响应包含：

- 四张 `CustomerMetricCard`。
- 全部、待指派设计和八个客户阶段的 `CustomerStageCount`。
- 当前分页 `Customer` 列表。
- `current/size/total/hasMore`。

### 指标口径

- 新增客户：本月创建客户数；辅助今日、本周和上月同期对比。
- 有效跟进：本月有跟进、到店、量房或设计交接动作的去重客户数；辅助今日待跟进和逾期。
- 签约客户：本月产生签约时间的客户数和签约额；辅助当前报价中客户数。
- 风险客户：高风险客户数；辅助需关注、停滞和本月流失。

### 客户卡投影

在 `Customer` 上增加只读字段：

- `idleDays`
- `riskLevel`
- `riskReason`
- `nextAction`
- `quoteAmount`
- `signedAmount`
- `projectId`
- `projectName`

这些字段由服务端装配，不落库，不允许前端写入。

### Changed Files

- `src/main/java/com/th4/edge/decoboon/customer/configure/CustomerRiskLevel.java`
- `src/main/java/com/th4/edge/decoboon/customer/model/Customer.java`
- `src/main/java/com/th4/edge/decoboon/customer/model/CustomerMetricCard.java`
- `src/main/java/com/th4/edge/decoboon/customer/model/CustomerStageCount.java`
- `src/main/java/com/th4/edge/decoboon/customer/model/CustomerOverviewReport.java`
- `src/main/java/com/th4/edge/decoboon/customer/context/CustomerOverviewContext.java`
- `src/main/java/com/th4/edge/decoboon/customer/service/overview/**`
- `src/test/java/com/th4/edge/decoboon/customer/CustomerOverviewContextTest.java`

## App

- `src/domains/crm/model.ts` 增加指标卡、阶段计数和客户卡投影类型。
- `src/domains/crm/api.ts` 增加 `crmApi.overview`。
- `src/pages/customer/list.vue`：
  - 使用自定义导航并补齐 `JzPageHeader + JzTabBar`，与项目/财务 Tab 保持一致。
  - BOSS 加载服务端聚合。
  - 展示四张指标卡。
  - 增加风险、停留、金额、最新录入排序。
  - 客户阶段计数改读服务端。
  - BOSS 客户卡展示负责人、停留、风险和下一步。
  - 搜索采用服务端关键字查询。

## Verification

- 后端单元测试覆盖四张指标、阶段计数、风险排序、逾期动作和客户金额投影。
- 后端全量：`63 suites / 182 tests / 0 failures / 0 errors / 0 skipped`。
- Provider 安全：新路由未登录拒绝。
- App：TypeScript、微信小程序构建和分包检查通过；总包 1,666,294 B。
- 本地真实 BOSS 会话调用 `customer/customer/overview` 返回四张指标卡、完整阶段计数和分页结构。
- 空库场景返回四张零值指标卡、完整零计数阶段和空客户列表，不报错。

## Risks / Blockers

- 当前客户阶段没有历史状态流水，阶段计数是当前快照，不提供历史阶段趋势。
- 签约客户按本月合同签约时间和客户去重；客户成交率的正式口径仍以 MET-001/DEC-001 后续实现为准。
- 风险规则是首版经营参考规则，仍需真实企业样本验收后校准。
- 尚未在微信开发者工具/真机完成 BOSS 客户页视觉与交互 acceptance。

## Acceptance Needed

是。按 `docs/design/19-客户经营聚合与负责人客户卡设计.md` 第 8 节执行 CUST-OV-01 至 CUST-OV-12。

## Next Action

- acceptance lane 使用真实 BOSS、店长和业务员数据验证指标全量口径、分页和角色边界。
- 构造未分配负责人、逾期跟进、待量房未指派、报价、签约和流失数据验证卡片状态。
- 验证接口失败时不沿用旧指标，并保留重试恢复路径。
