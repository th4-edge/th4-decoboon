# App 签约后业务流转交接

- request_id: REQ-20260918-APP-FLOW-001
- status: implemented / pending_independent_acceptance

## input_basis
- 用户指令：App 客户约定量房后“查看业务流转”应直接跳转签约后流转页，可查看设计、报价、谈单 PPT、合同、工地与计划；业务员只读。
- 后端 `MarketerReport` 已新增周期指标、量房率、跟进数与企业/门店排名（本批不修改）。

## changed_files
- App:
  - `src/config/index.ts`（新增 `customerFlow` 路由）
  - `src/pages.json`（注册 `subpackages/customer/pages/flow`）
  - `src/subpackages/customer/pages/flow.vue`（新增）
  - `src/subpackages/customer/pages/detail.vue`（`viewFlow` 改为跳转）

## implementation
- App `viewFlow` 不再 toast，直接 `uni.navigateTo` 到签约后业务流转页，携带 `customerId + propertyId`。
- 业务流转页按 `activePropertyId` 过滤报价、合同、项目与施工计划，规避多房屋串线；无 activeProperty 时不展示具体房屋数据。
- 排名口径沿用后端：客户录入量 → 量房率 → 跟进数。

## verification
- App: `npm run type-check` PASS；`npm run build:mp-weixin` PASS；`git diff --check` PASS。

## risks
- 业务流转页依赖 design/trade/project provider 返回的 property/quote/project 关联字段；关联缺失时该条目不展示，不伪造。
- App 未在真机/微信开发者工具做交互验证，仅完成类型检查与构建。

## blocked
- 无。

## next_action
- 由验收 lane 按业务条件矩阵覆盖：多房屋串线、量房后跳转、排名口径、趋势为空的情况。

## acceptance_needed
- App 签约后业务流转跳转与只读可见性验收。
