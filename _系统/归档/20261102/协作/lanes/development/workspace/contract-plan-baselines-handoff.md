# 合同付款计划与施工计划基线开发交接

- request_id：`REQ-20260920-DEV-CONTRACT-PLAN-BASELINES`
- lane：`development`
- 状态：`implementation_done_self_tested_pending_independent_acceptance`
- 依据：`BR-APP-008～012`、`APP-BASE-001/002`、历史导入到 PDF 归档路线 P0-B/P0-C
- 边界：本轮只实现计划模板基线、合同级快照、阶段/应收生成和签约页面选择；不执行生产迁移，不建设企业自定义计划模板编辑器。

## 1. 基线数据

平台基线文件：

- `src/main/resources/baselines/payment-plan-baselines.json`
- `src/main/resources/baselines/construction-plan-baselines.json`

付款计划基线：

1. `STANDARD_30_40_30`：签约 30% / 开工 40% / 竣工 30%。
2. `PROGRESS_4_STAGE`：签约 30% / 水电验收 25% / 泥木验收 25% / 竣工 20%。
3. `FIRST_FINAL_50_50`：签约 50% / 竣工 50%。

施工计划基线：

1. `RESIDENTIAL_STANDARD_8`：开工交底、拆除改造、水电施工、泥瓦防水、木作吊顶、油漆墙面、安装收尾、竣工保洁验收，标准 120 天。
2. `RESIDENTIAL_SIMPLE_5`：开工交底、水电、泥木、油漆安装、竣工验收，标准 90 天。

## 2. 合同快照

- `ContractPlanContext.select` 校验模板编码并按合同开工/完工日期形成阶段快照。
- 阶段计划天数按模板默认工期比例分配，最后一个阶段严格收敛到合同计划完工日期。
- 付款节点比例合计必须为 100%；最后一笔自动承接尾差，保证节点金额合计等于最终签约价。
- 签约时按最终签约价重算付款节点；生效后不能再更换模板。

## 3. 生效联动

- `ProjectContext` 优先按合同施工计划快照生成 `ProjectStage`，不再直接按报价分类临时拼装阶段。
- 报价施工行按类别关键词映射到对应阶段；主材行继续生成材料计划。
- 付款节点按合同快照生成 `Receivable`，写入 `stageCode`、`ProjectStage` 和触发类型。
- 首期款到账不参与合同生效和首个阶段开工门禁。
- 没有快照的历史合同保留旧式 30/40/30 和相关阶段生成回退逻辑。

## 4. Provider 与页面

- 新增受保护路由 `plan/template/page`，返回平台付款和施工计划基线目录。
- `project/contract/save`、`project/contract/update` 新增 `paymentPlanCode`、`constructionPlanCode` 入参。
- App 签约页：选择付款计划、施工计划，显示节点和阶段预览。

## 5. 主要变更文件

后端：

- `src/main/java/com/th4/edge/decoboon/plan/**`
- `src/main/java/com/th4/edge/decoboon/project/context/ProjectContractContext.java`
- `src/main/java/com/th4/edge/decoboon/project/context/ProjectContext.java`
- `src/main/resources/baselines/**`
- `src/test/java/com/th4/edge/decoboon/plan/**`
- `docs/design/migrations/42-付款计划与施工计划快照.sql`

App：

- `src/domains/plan/**`
- `src/domains/trade/model.ts`
- `src/domains/trade/api.ts`
- `src/subpackages/project/pages/create.vue`
- `scripts/verify-plan-contract.mjs`


- `src/domains/plan/**`
- `src/domains/trade/model.ts`
- `src/domains/trade/api.ts`
- `src/views/trade/QuoteDetailView.vue`

## 6. 验证证据

- 后端 `mvn -o -q test`：59 suites / 175 tests / 0 failures / 0 errors。
- App `npm run verify:mp-weixin`：契约检查、类型检查、小程序构建和分包检查通过；总包 1,591,852 B。
- 测试覆盖：3 套付款基线、2 套施工基线、合同快照、最终签约价重算、8 阶段生成、阶段编码、应收触发类型和阶段关联。

## 7. 风险与待验收

- 迁移 42 尚未在目标 MySQL/生产库执行。
- 企业自定义计划模板编辑、启停、版本升级和企业覆盖还未实现。
- 阶段和任务映射当前使用类别关键词启发式；PM 可在后续计划编辑能力中调整。
- 需要在隔离库和真实 App 中验证历史合同回退、金额尾差、计划重选、生效幂等和跨企业访问。

## 8. 建议验收步骤

1. 执行迁移 42，创建企业、客户、房屋、报价和待签合同。
2. 打开 App 签约页，确认可看到 3 套付款计划和 2 套施工计划。
3. 选择 `PROGRESS_4_STAGE + RESIDENTIAL_STANDARD_8`，以非整百金额签约，核对每笔节点合计等于最终签约价。
4. 合同生效后核对 8 个阶段编码和计划日期，确认阶段最后一天等于合同计划完工日期。
5. 核对三笔应收均已绑定阶段编码；首期款未到账仍可生效和开工。
6. 修改平台基线后确认历史合同快照不再变化。
