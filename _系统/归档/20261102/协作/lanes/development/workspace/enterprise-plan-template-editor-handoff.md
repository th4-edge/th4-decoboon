# 企业自定义付款与施工计划模板编辑器开发交接

## Input

- 用户决策：本轮建设企业自定义计划模板编辑器，后端与 App 前端均支持。
- 现有边界：平台基线只读；合同签约复制为合同快照；合同生效后不能换模板。
- 设计文档：`docs/design/18-企业自定义付款与施工计划模板设计.md`。

## Backend

### Data Model

- `db_company_payment_plan`
- `db_company_payment_node`
- `db_company_construction_plan`
- `db_company_construction_stage`

实体均归属企业，包含编码、版本、状态、默认标记、来源基线、发布时间和审计字段。

### Provider Routes

```text
plan/payment-template/page
plan/payment-template/save
plan/payment-template/update
plan/payment-template/publish
plan/payment-template/disable
plan/payment-template/default

plan/construction-template/page
plan/construction-template/save
plan/construction-template/update
plan/construction-template/publish
plan/construction-template/disable
plan/construction-template/default
```

`plan/template/page` 继续作为签约可用目录，返回“企业已启用模板 + 平台基线”，企业默认模板优先。

### Rules

- 写操作仅允许 BOSS。
- 企业归属从登录员工覆盖，客户端不能指定企业。
- 付款比例合计必须 100%。
- 阶段触发必须指定施工阶段编码。
- 施工阶段编码不能重复，工期必须为 1–365 天。
- 默认模板每个企业、每种类型最多一个。
- 停用默认模板时自动清除默认标记。
- 编辑已启用模板时版本递增；历史合同继续使用原快照。

### Changed Files

- `src/main/java/com/th4/edge/decoboon/plan/configure/PlanTemplateStatus.java`
- `src/main/java/com/th4/edge/decoboon/plan/configure/PlanTemplateSource.java`
- `src/main/java/com/th4/edge/decoboon/plan/model/CompanyPaymentPlan.java`
- `src/main/java/com/th4/edge/decoboon/plan/model/CompanyPaymentNode.java`
- `src/main/java/com/th4/edge/decoboon/plan/model/CompanyConstructionPlan.java`
- `src/main/java/com/th4/edge/decoboon/plan/model/CompanyConstructionStage.java`
- `src/main/java/com/th4/edge/decoboon/plan/repository/Company*Repository.java`
- `src/main/java/com/th4/edge/decoboon/plan/context/CompanyPlanTemplateContext.java`
- `src/main/java/com/th4/edge/decoboon/plan/context/ContractPlanContext.java`
- `src/main/java/com/th4/edge/decoboon/plan/service/{paymenttemplate,constructiontemplate}/**`
- `src/main/java/com/th4/edge/decoboon/plan/service/template/page/support/Page.java`
- `docs/design/migrations/43-企业自定义付款与施工计划模板.sql`
- `docs/design/18-企业自定义付款与施工计划模板设计.md`

## App

### Entry

「我的 -> 经营工具」新增：

- 付款计划模板。
- 施工计划模板。

### Pages

- `src/subpackages/plan/pages/payment-list.vue`
- `src/subpackages/plan/pages/payment-edit.vue`
- `src/subpackages/plan/pages/construction-list.vue`
- `src/subpackages/plan/pages/construction-edit.vue`

列表支持复制平台基线、新建、编辑、启用、停用和设为默认。编辑器支持付款节点比例、触发条件和施工阶段工期配置。

### Changed Files

- `src/domains/plan/model.ts`
- `src/domains/plan/api.ts`
- `src/config/index.ts`
- `src/pages.json`
- `src/pages/mine/index.vue`
- `src/subpackages/plan/**`
- `scripts/verify-plan-contract.mjs`

## Verification

- 后端：`62 suites / 181 tests / 0 failures / 0 errors / 0 skipped`。
- Provider 安全：总路由 253，public 12，protected 241；企业模板全部未登录拒绝。
- App：`npm run verify:mp-weixin` PASS。
- App 分包：13 个分包 / 73 页；主包 535,309 B，最大分包 186,954 B，总包 1,656,289 B。
- 本地开发库已执行迁移 43。
- 真实接口闭环已验证：
  1. 复制 `PROGRESS_4_STAGE` 为企业付款模板。
  2. 启用并设为默认。
  3. 复制 `RESIDENTIAL_SIMPLE_5` 为企业施工模板。
  4. 启用并设为默认。
  5. `plan/template/page` 返回企业默认模板优先。
- 验证产生的模板数据和临时测试密码已清理。

## Risks / Blockers

- 尚未在微信开发者工具/真机完成页面级视觉与交互 acceptance。
- 当前模板采用“企业当前版本 + 合同不可变快照”模型，不保留企业模板自身的历史版本回滚；历史合同仍安全。
- 迁移 43 仅在本地开发库执行，目标测试/生产库尚未执行。

## Acceptance Needed

是。按 `docs/design/18-企业自定义付款与施工计划模板设计.md` 第 10 节执行 PLAN-TPL-01 至 PLAN-TPL-12。

## Next Action

- acceptance lane 在隔离环境执行迁移 43。
- 使用真实 BOSS 账号执行复制、编辑、启用、停用、默认和合同签约快照验收。
- 验证非 BOSS 写接口拒绝、跨企业越权拒绝和历史合同快照隔离。
