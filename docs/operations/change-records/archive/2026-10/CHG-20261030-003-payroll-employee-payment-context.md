# CHG-20261030-003 · 员工发放跨端上下文、日期与幂等对齐

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261030-003 |
| 类型 | 需求 / 缺陷 |
| 风险级别 | L3 |
| 状态 | 待验证 |
| 负责人 | Codex |
| 日期 | 2026-10-30 |
| 关联需求/事故/业务裁决 | `BR-015`、`DEC-093`、`REQ-BIZ-20261006-004`、`CHG-20261030-002` |

## 2. 目标与范围

- 目标：员工工资/提成发放与具体工资单、当前门店、发放日期和提交意图保持一致；失败重试不得因重新生成幂等键而重复或丢失恢复能力。
- 范围内：`finance/payment/employee/page` 的可选 `sheetId` 过滤、Web 员工发放抽屉上下文、App 员工发放日期/备注/幂等键、对应测试和验收证据。
- 非范围：工资公式、应付生成规则、员工发放角色矩阵、作废/退回、批量/部分发放、DDL 和迁移。
- 受影响端：后端、App、Web。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：是，涉及员工应付选择、付款日期和资金流水。
- 业务事实来源与裁决编号：`BR-015`、`DEC-093`；幂等沿用 `REQ-BIZ-20261006-004`，跨端原则沿用 `CHG-20261030-002`。
- 尚存 GAP 或假设置：无；批量/部分发放仍属于 B4，不在本批。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | Web 财务从已确认工资单发放 | 工资单存在多处员工应付 | 发放抽屉只显示该工资单来源的应付，不自动选中其他员工 | `PayrollEmployeePaymentPageScopeTest`、Web build |
| 2 | Web 财务在门店维度发放 | 当前选择单店 | 发放应付列表按当前门店查询，服务端仍做权限兜底 | Web build、后端权限回归 |
| 3 | App 财务发放 | 选择待发应付 | 可填写发放日期和备注，提交后回读变更 | App type-check、契约、小程序构建 |
| 4 | 弱网失败重试 | 原请求可能已成功 | 同一提交意图复用幂等键；修改日期/账户/备注后才生成新意图 | App 契约与幂等契约测试 |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | `dev` | 待提交 | 员工发放分页 `sheetId` 过滤、分页范围测试、领域文档 |
| th4-decoboon-app | `dev` | 待提交 | `subpackages/payroll/pages/payment-create.vue`、薪酬闭环契约脚本 |
| th4-decoboon-web | `dev` | 待提交 | `FinanceEmployeePaymentDrawer.vue`、`FinanceEmployeeSalariesView.vue`、`FinanceEmployeeCommissionsView.vue`、`domains/finance/api.ts` |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| 定向后端测试 | `mvn -q -Dtest=PayrollEmployeePaymentPageScopeTest,PayrollEmployeePaymentAccessTest test` | PASS，2/2 | `docs/acceptance/runs/20261030-payroll-employee-payment-align/backend-verification.txt` |
| Java 22 全量后端回归 | JDK 22 `mvn -q test` | PASS，1130 tests / 0 failures / 0 errors / 0 skipped | `docs/acceptance/runs/20261030-payroll-employee-payment-align/backend-verification.txt`；完整日志 `/tmp/chg-20261030-003-jdk22-full.log` |
| App 契约 | `node scripts/verify-payroll-closure-contract.mjs` | PASS | `docs/acceptance/runs/20261030-payroll-employee-payment-align/frontend-verification.txt` |
| App 类型检查/构建 | `npm run type-check`、`npm run build:mp-weixin` | PASS | `docs/acceptance/runs/20261030-payroll-employee-payment-align/frontend-verification.txt` |
| Web 类型检查/构建 | `npm run type-check`、`npm run build` | PASS；分页/文案/编号/品牌/壳层门禁 PASS | `docs/acceptance/runs/20261030-payroll-employee-payment-align/frontend-verification.txt` |
| 真实页面/接口冒烟 | Web 工资单详情发放、App 员工发放 | 待执行 | 验收记录 |

- 未执行项及原因：未使用真实财务账号发放，避免污染生产资金数据。
- 已知失败及是否与本次相关：无。

## 7. 发布

| 字段 | 内容 |
| --- | --- |
| Release ID | 未发布 |
| 制品路径/SHA-256 | 未生成 |
| 生产 release/版本 | 未发布 |
| 上一版本/回滚点 | 沿用 `CHG-20261030-002` 对应回滚点 |
| 健康检查 | 待发布 |
| 发布后观察 | 待发布 |

- 数据库迁移：无
- 配置/密钥变化：无

## 8. 回滚

- 回滚条件：发放抽屉显示跨工资单应付、门店范围错误、App 发放日期/幂等异常。
- 回滚命令/步骤：回退本 CHG 的后端分页过滤和 App/Web 发放文件；保留已有应付、付款和台账事实，不做数据回滚。
- 回滚验证：原发放接口仍可按原权限和幂等规则工作。
- 实际是否回滚：否

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集
- 人工耗时：未采集
- 一次通过：待验证
- 返工原因：待验证
- 最终结论：待真实发放页面验证后关闭

## 10. 复盘与沉淀

- 是否出现重复问题：是，发放入口曾只有类型过滤，没有工资单来源上下文。
- 是否需要新增门禁：是，后端工资单来源过滤测试 + App 发放字段/幂等契约。
- 候选 `EXP/ANT/PATH`：类型过滤不能替代对象上下文；发放必须先确定来源应付。
- 后续动作与负责人：真实发放验收后再处理批量/部分发放（B4）。
