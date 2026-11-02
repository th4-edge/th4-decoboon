# 财务经营与项目核算 · 设计—蓝图—实现—页面—验收对账

状态：`PENDING` / `GAP` / `BLOCKED` / `MAPPED`
当前阶段：`SCENARIOS_READY / BIZ_ALIGNMENT_REQUIRED`
说明：本表按能力级对账，不替代逐字段 Provider 矩阵、测试报告和独立验收。

## 1. 财务专业能力

| REQ | 场景 | 业务规则 | 蓝图节点 | 字段/API | 当前页面 | 测试 | 验收 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-FIN-001 | 财务经营台判断今日行动 | `DEC-065`、`MET-002/006` | F0-01/F0-02/PF-01 | `finance/summary/report` 等 | Web `finance/workbench`；App `pages/finance/index` | 汇总与列表口径需回归 | 未独立验收 | GAP |
| REQ-FIN-002 | 门店范围与下钻 | `DEC-065/081` | F0-03/PF-01~10 | 服务端 DataScope | Web/App 已有范围控件 | 已有部分范围测试 | 需跨端回归 | MAPPED / TEST_GAP |
| REQ-FIN-003 | 应收、收款与核销 | `BR-050/055` | F1-01~03/PF-02 | `finance/receivable|receipt` | Web 应收/收款；App 应收/收款 | 有部分实现证据 | 待独立验收 | MAPPED / ACCEPTANCE_PENDING |
| REQ-FIN-004 | 收款冲正、客户退款 | `BR-050/077` | F1-04/PF-02 | `finance/receipt/reverse`、`finance/refund/save` | App 有冲正页；Web 冲正缺失记录 | 部分存在 | 未完整验收 | GAP |
| REQ-FIN-005 | 来源应付 | `BR-055/DEC-089` | F2-01/PF-03 | `finance/payable/*` | Web/App 应付 | 部分测试 | 待独立验收 | MAPPED / TEST_GAP |
| REQ-FIN-006 | 付款核销与回读 | `BR-055` | F2-03/PF-03 | `finance/payment/*` | Web/App 付款 | 部分测试 | 欠款/跨店回归 | MAPPED / TEST_GAP |
| REQ-FIN-007 | 供应商往来 | `DEC-084/BR-045` | F2-04/PF-03 | `finance/partnerledger/*`、Partner | Web/App 供应商往来 | 部分 | 待独立验收 | MAPPED / ACCEPTANCE_PENDING |
| REQ-FIN-008 | 费用与报销 | `DEC-091/BR-057` | F3-01~05/PF-04 | `finance/expense/*` | Web/App 费用页 | D1-D3 已部分验收 | D4/付款/成本边界未收口 | GAP |
| REQ-FIN-009 | 薪酬复核/发放 | `DEC-093/BR-015` | F4-01~05/PF-05 | `payroll/*` | Web/App 薪酬页面 | B1 部分验收 | 独立验收未完成 | GAP |
| REQ-FIN-010 | 资金台账与期初 | 财务总纲、`BR-055` | F5-01/02/PF-06 | `finance/ledger/*`、opening-balance | Web/App 台账 | 部分 | 来源/权限回归 | MAPPED / TEST_GAP |
| REQ-FIN-011 | 高危流水动作 | `DEC-065/075` | F5-04/PF-06 | `finance/ledger/delete|correct` | Web 台账有删除/更正；App 有台账 | 部分 | 安全门/审计需独立验收 | GAP |
| REQ-FIN-012 | 项目成本完整性与毛利 | `DEC-092/BR-058`、`T12-BIZ-20261031-001` | F6-01~04/PF-07 | `ProjectCostContext`、`finance/project/report` | Web/App 项目账本 | 成本专项 6/6 + 全量 1169 PASS | 有效减项已修；真实人工成本未发生，最终利润仍空 | MAPPED / DEV_VERIFIED / ACCEPTANCE_PENDING |
| REQ-FIN-013 | 项目结算/质保 | 财务总纲、`MET-002` | F6-05/PF-07 | `finance/settlement/*` | Web/App 项目核算 | 部分 | 关闭/质保负向待验收 | MAPPED / TEST_GAP |
| REQ-FIN-014 | 业务凭证与重打 | `BR-006/018` | F7-01~04/PF-09 | `document/voucher/*` | Web/App 凭证 | 部分 | 模板/重打/权限待验收 | GAP |
| REQ-FIN-015 | 财务基础治理 | `DEC-079/084` | F8-01~05/PF-10 | ledger-category、fund account、Partner、CatalogItem | Web/App + 共享主数据 | 部分 | 删除/历史快照/审计待验收 | MAPPED / TEST_GAP |
| REQ-FIN-018 | 经营分析与风险处置 | `MET-002/006`、`GAP-070` | F9-05/PF-08 | 统计/风险 Provider | Web/App 经营分析 | 部分 | 利润未冻结不可展示 | BLOCKED |

## 2. 跨域财务投影

| REQ | 场景 | 业务规则 | 目标节点 | 现有实现 | 财务投影 | 验收 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-FIN-016A | 客户经营与回款风险 | `DEC-080` | SH-01 / PF-02 | Web `/workbench/customers`；App 客户 | 回款风险、应收摘要、合同/项目摘要 | 同对象、同权限、无复制页 | MAPPED / UI_GAP |
| REQ-FIN-016B | 报价项目库标准成本 | `DEC-079` | SH-02 / PF-10 | Web 设计师目录；App 报价项目库 | 标准成本、批量导入、停用/启用、删除限制 | 财务可维护，其他角色裁剪 | MAPPED / ACCEPTANCE_PENDING |
| REQ-FIN-016C | 报价与合同付款计划 | `DEC-086`、`BR-052` | SH-03 | Web `/finance/quotes`、`/finance/design-reviews`；App 报价 | 成本、付款计划、变更影响、应收基线 | 回到报价/合同主域 | GAP |
| REQ-FIN-017A | 项目核算对象 | `DEC-092` | SH-04 / PF-07 | Web `/finance/projects/:id`；App 项目详情 | 成本三态、收付款、结算、风险 | 回到项目主域嵌入财务面板 | GAP |
| REQ-FIN-017B | 材料下单与结算 | `DEC-073/074` | SH-05 | Web `/finance/material-ordering`、`/finance/material-settlement`；App 材料结算 | 供应商、价格、应付、实际成本 | 回到材料主域嵌入财务动作 | GAP |
| REQ-FIN-009A | 薪酬对象与财务发放 | `DEC-093` | SH-06 / PF-05 | Web `/finance/employee-salaries` 等；App 薪酬 | 工资构成、复核、发放、工资条 | 明确薪酬主域并复用对象 | GAP |

## 3. App/Web 同源对账

| 能力 | Web | App | 同 Provider | 同状态/金额/权限 | 当前结论 |
| --- | --- | --- | --- | --- | --- |
| 财务工作台 | `/finance/workbench` | `pages/finance/index` | 部分 | 汇总字段需对账 | GAP |
| 应收/收款 | `/finance/receivables`、`/finance/receipts` | `finance/receivables`、`receipts` | 预期同源 | 需回读矩阵 | TEST_GAP |
| 应付/付款 | `/finance/payables`、`/finance/payments` | `finance/payables`、`payments` | 预期同源 | 需回读矩阵 | TEST_GAP |
| 退款/冲正 | Web 冲正入口缺失 | App `receipt-reversal` | 需对账 | 能力不一致 | GAP |
| 费用报销 | `/finance/expenses` | `finance/expenses` 等 | 部分 | 需证据/附件/审批对账 | GAP |
| 资金台账 | `/finance/ledger` | `finance/ledger` | 预期同源 | 需高危动作对账 | TEST_GAP |
| 项目核算 | `/finance/projects/:id` | 项目详情/财务项目页 | 部分 | 三态成本/利润空值需核对 | GAP |
| 薪酬 | 多个 `/finance/*` 页 | 多个薪酬页 | 部分 | 角色动作/工资条可见性需对账 | GAP |
| 凭证与模板 | `/finance/vouchers`、`/finance/voucher-templates` | `finance/vouchers`、`voucher-templates` | 预期同源 | 打印/重打/模板版本需对账 | TEST_GAP |

## 4. 页面框架对账

| 目标页面 | 当前对应 | 主域 | 结论 | 状态 |
| --- | --- | --- | --- | --- |
| PF-01 财务经营台 | `FinanceWorkbenchView.vue` | 财务/经营分析 | 保留页面，重构首屏结论与行动 | MAPPED / REPLACE_IN_PLACE |
| PF-02 应收与回款 | 应收、收款、退款页 | 财务 | 合并为任务工作面 + 对象抽屉 | MAPPED / CONSOLIDATE |
| PF-03 应付与付款 | 应付、付款、供应商往来页 | 财务 | 合并为任务工作面 + 往来账本 | MAPPED / CONSOLIDATE |
| PF-04 费用与报销 | `FinanceExpensesView.vue` | 财务/费用 | 保留，补审批、证据、归属、付款回读 | MAPPED / REFINE |
| PF-05 薪酬发放 | 薪酬工作台/员工工资/提成页 | 薪酬 | 统一为薪酬对象工作面，财务嵌入发放 | GAP / NEEDS_BUSINESS_DECISION |
| PF-06 资金与对账 | `FinanceLedgerView.vue` | 财务 | 保留，升级为对账台和安全门 | MAPPED / REFINE |
| PF-07 项目核算 | `FinanceProjectView.vue` | 项目+财务 | 迁入共享项目对象，财务面板嵌入 | GAP / REHOME |
| PF-08 经营分析 | Web/App 经营分析页 | 经营分析 | 复用同源指标，不新增财务专属报告 | MAPPED / ALIGN |
| PF-09 单据与档案 | 凭证/模板页 | 财务+文档 | 保留并统一档案页 | MAPPED / REFINE |
| PF-10 财务设置 | 分类/账户/模板/主数据分散页 | 财务治理 | 设置中心 + 跳转主域 | GAP / CONSOLIDATE |

## 5. 未映射

以下内容尚未形成稳定业务/页面映射：

1. `FIN-Q01` 财务在客户、报价、合同、项目、材料、薪酬和企业组织中的完整职责边界；
2. `FIN-Q03` 跨域页面复用的最终导航与路由归属；
3. `FIN-Q05` 公司/门店经营利润在页面中的展示方式；
4. `FIN-Q06` 发票字段/状态是否形成独立跟踪；
5. `FIN-Q07` 收款冲正、手工流水作废/更正、退款、结算关闭、质保释放的统一安全门归属；
6. `FIN-Q08` 薪酬对象工作面、财务发放页和 BOSS 最终确认的页面分离；
7. 财务汇总与列表页的待付金额差异根因；
8. App/Web 现有财务页面逐字段 Provider 对账矩阵；
9. `FinanceWorkbenchView` 的最终结论排序和唯一主行动；
10. 财务页面母型共享组件的实际代码映射与迁移顺序。

## 6. 已过期

- 历史审计“财务菜单 14 项”已过期；当前 Finance 配置注册 24 个菜单项。以当前代码和 B2 真实页面为准，旧数字不用于冻结范围。
- 历史财务页面的截图与视觉评分仅作基线证据，不代表当前最新 UI；后续 B2 必须重新渲染。
- `INFO-FINANCE-004` 的发票信息项可能落后于 `DEC-016/091` 的 L3 边界，标记为潜在过期/冲突，待 T06 处理。
- 既有 `docs/design/08-财务域业务闭环设计.md` 中部分角色权限描述已被 `DEC-065/075/079/080/081/084/093` 修订，以新决策为准。

## 7. 未决业务问题

| 编号 | 问题 | 阻塞 | 下一动作 |
| --- | --- | --- | --- |
| FIN-Q01 | 财务完整职责边界 | 页面归属、导航、能力动作 | T06 回答 REQ-BIZ-20261029-002 |
| FIN-Q03 | 跨域页面复用 | 路由、页面、组件迁移 | T06/用户确认 |
| FIN-Q04 | 首屏判断优先级 | B3 财务经营台 | 业务/用户评审 |
| FIN-Q05 | 公司/门店经营利润 | 经营分析、驾驶舱金额 | `GAP-070` 关闭 |
| FIN-Q06 | 发票字段边界 | 应收/费用字段 | T06/业务裁决 |
| FIN-Q07 | 高危动作页面归属 | 安全门和对象动作 | 业务/产品/UI 联合冻结 |
| FIN-Q08 | 财务与薪酬职责分离 | 薪酬页面归属和按钮 | T06/用户确认 |
| GAP-071 | 项目成本三态与实际毛利 | 项目核算/利润 | 按既有研发推进 |
| PI-016 | 财务写路径/门店高危动作 | 写权限、门店范围 | 按既有问题台账 |

## 8. 本批结论

- 已对齐：财务边界、角色范围、普通写权限、高危动作、来源、成本完整性、费用报销、薪酬职责、材料下单/结算。
- 待对齐：财务完整职责、导航、跨域页面复用、经营利润、发票、薪酬页面分离。
- 下一批：完成 T06/T12 业务答复；之后进入 B2 真实 UI 基线，不直接写代码。

## 9. 2026-10-30 跨角色 Web/App 混合审计

- 审计包：`docs/acceptance/runs/20261030-finance-cross-role-cross-surface-audit/`
- 环境：本地隔离库 `decoboon-test`，`ZDQ造数M`，Boss/Finance/StoreManager，Web + 微信开发者工具
- 形态：只读；未执行新增、收款、付款、冲正、退款、审批、删除或发布

| 对账项 | 页面 | API/DB | 状态 |
| --- | --- | --- | --- |
| 应收主金额 | Web 正确；App 顶部为 0 | DB 73 笔/¥135,672.23 | `FAIL_CROSS_SURFACE` |
| 收款汇总 | Web ¥13,477.14/18；App 为 0 | DB 一致 | `FAIL_CROSS_SURFACE` |
| 应付汇总 | Web/App 一致 | DB 一致 | `PASS` |
| 付款汇总 | Web ¥9,500/19；App 为 0 | DB 一致 | `FAIL_CROSS_SURFACE` |
| 台账展示 | Web/App 一致 | 页面/DB 一致，但来源不守恒 | `FAIL_DATA_INTEGRITY` |
| 店长范围 | 本店数据；跨店被拒绝 | 显式跨店 API 403 | `PASS_WITH_NAV_GAP` |
| 手工应收/应付类型 | 暴露全部类型 | `BR-055` 仅允许受限类型，Save 未校验 | `FAIL_BUSINESS_RULE` |
| 项目/费用/凭证 | 旧 schema 加载失败并出现伪 0 | `undefinedErrorCode` | `BLOCKED_ENVIRONMENT + FAIL_ERROR_STATE` |

阻断 ID：`DEF-FIN-001~003`。修复前不得宣称财务闭环、账务准确性或跨端一致性通过。

## 10. 2026-10-30 全新本地装企业务驱动审计

- 审计包：`docs/acceptance/runs/20261030-fresh-enterprise-business-audit/`
- 数据：本地 `decoboon`，昭青科技 `199500000000000001`，2 门店、7 类角色、8 员工、1 条核心业务链
- 端面：Web 8080、微信开发者工具 `dist/dev/mp-weixin`
- 结论：核心业务/财务读取链与 Boss/Finance/StoreManager 双端页面为 `PASS_READ_ONLY`；整体业务审计 `PARTIAL`

| 对账项 | 页面 | API/DB | 状态 |
| --- | --- | --- | --- |
| 应收/已收/待收 | Web/App 均为 ¥3,495.75 / ¥748.73 / ¥2,747.02 | DB 一致 | PASS |
| 应付/已付/待付 | Web/App 均为 ¥2,000 / ¥500 / ¥1,500 | DB 一致 | PASS |
| 台账 | Web/App 4 条，净现金流 -¥851.27 | DB 一致 | PASS |
| 项目成本 | 成本未完整，利润与毛利率为空 | `costComplete=false`、预计 ¥1,760、实际 ¥800 | PASS |
| 店长范围 | 本店客户 1、待开工项目 1 | 跨店 envelope 403 | PASS |
| Provider 嵌套关联 | 财务/店长页面真实回读 | 修复前截断，修复后合法 JSON | FIXED_PENDING_ACCEPTANCE |
| 高危动作 | 店长台账可见更正/删除 | FIN-Q07 未冻结 | BLOCKED_BUSINESS_DECISION |
| 材料/验收 | 仅材料计划与施工初始事实 | 未形成采购/到场/验收闭环 | PARTIAL |

关联变异：`CHG-20261030-035`、`CHG-20261030-036`、`CHG-20261030-037`；问题：`PI-20261030-002`、`PI-20261030-003`、`PI-20261022-016`。

## 11. 2026-10-31 B6 成本专项：有效 Remove/Quantity 完整性分母

- 业务裁决：`T12-BIZ-20261031-001`；变更：`CHG-20261031-039`；问题：`PI-20261030-004`。
- `ProjectCostContext` 对原合同材料行按 `ContractChangeRangeContext.effectiveRemainingQuantityForCost` 计算当前有效需求。
- 已生效 Remove 为 0 时不产生材料需求缺失；Quantity 减少只按减后剩余判断；Pending 不提前扣减。
- 已到场后 Remove 保留采购/到场事实并要求退货或调整闭环；Replace 不在本批单独关闭来源，避免目标需求未计。
- `findEffectiveByProjectIds` 增加 sourceLine fetch join，避免变更行逐条懒加载。

| 对账项 | 修复前 | 修复后 | 状态 |
| --- | ---: | ---: | --- |
| 当前项目成本缺失项 | 2 | 1（真实人工成本） | PASS |
| 当前合同额 | ¥858.69 | ¥858.69 | PASS |
| 已收/待收 | ¥858.69 / ¥0 | ¥858.69 / ¥0 | PASS |
| 应付/已付 | ¥88 / ¥88 | ¥88 / ¥88 | PASS |
| 实际成本/预计成本 | ¥88 / ¥746.50 | ¥88 / ¥746.50 | PASS |
| 项目利润/结算利润 | NULL | NULL | PASS（未伪算） |
| 结算状态 | Draft | Draft | PASS（门禁保持） |
| `coreBalanced` | true | true | PASS |

证据：`docs/acceptance/runs/20261030-true-page-level-closed-loop/raw/api/S70-cost-effective-reduction-fixed.json`、`S71/S72`、`raw/sql/accounting-invariants-cost-effective-reduction.json`。

结论：`DEV_VERIFIED / PENDING_INDEPENDENT_ACCEPTANCE`。真实人工成本与业主验收仍未完成，项目结算不得确认或关闭。

## 12. 2026-11-02 财务角色高危动作数据范围对账

- 业务裁决：`DEC-099`；变更：`CHG-20261102-023`。
- 现行权限：BOSS/总部财务全企业，门店财务/店长本店；其他角色拒绝。
- 后端统一入口：`FinanceAccessContext.requireHighRiskEditor`，范围只认登录员工解析出的 `DataScope`。
- 覆盖资产：手工流水作废、手工流水更正/冲正重记、收款冲正。
- 自动化：后端财务专项 43 个用例通过；Web 完整构建通过；App `verify:mp-weixin`、类型检查和小程序构建通过。
- 未执行：生产发布、生产账号真实写入、独立验收。
