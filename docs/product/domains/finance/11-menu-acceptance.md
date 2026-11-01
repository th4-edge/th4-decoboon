# 财务角色现有 Web 菜单功能验收台账

- 日期：2026-10-29
- 状态：`PENDING_ACCEPTANCE`
- 基线：`USER-DEC-20261029-001`、`USER-DEC-20261029-007`
- 原则：升级、优化、完善，不新建；保留现有整体布局、菜单、路由、深链和 App Tab；本阶段不改后端业务逻辑。
- 菜单基线：`th4-decoboon-web/src/roles/workbenchConfig.ts` 的 Finance 配置，共 24 个菜单项。
- 验收单位：一个 Web 菜单 = 一个当前页面 + 一个角色任务 + 一条当前主行动 + 一组回读事实。
- 小程序对应台账：`12-app-finance-entry-acceptance.md`；两端分别验收，但必须核对同源事实、权限、状态和金额。

## 1. 验收结论口径

| 结论 | 含义 | 本阶段允许的处理 |
| --- | --- | --- |
| `PENDING` | 尚未按真实角色、真实数据验收 | 继续验收 |
| `PASS` | 当前页面能完成角色任务，业务和交互无需改动 | 保留 |
| `OPTIMIZE` | 业务成立，但信息层级、文案、交互或状态表达需原位优化 | 形成最小前端优化项 |
| `BUG` | 当前实现与已确认业务事实/契约不一致 | 登记缺陷，是否修复另行确认 |
| `BACKEND_GAP` | 需要改 Provider、状态机、权限、金额/成本/利润或后端逻辑 | 只登记，不在本阶段修改 |
| `BLOCKED` | 缺少真实数据、权限、账号、Provider 或业务裁决 | 记录阻塞和恢复路径 |
| `OUT_OF_SCOPE` | 不属于财务角色主责或不应在当前财务路径承载 | 记录归属，暂不处理 |

## 2. 单项验收记录要求

每个菜单必须记录：

1. 菜单名称、路由、当前页面文件；
2. 当前可见角色、门店范围、数据范围；
3. 页面要回答的角色问题；
4. 当前主要对象和主行动；
5. 使用的 Provider/事实源；
6. 正常、加载、空、失败、无权限、长内容、极端值；
7. App/Web 是否存在同一能力；
8. 当前页面是否与现有布局兼容；
9. 是否存在重复入口、同义对象或动作混层；
10. 最小优化建议、回滚点、是否需要后端另行处理；
11. 最终结论和证据。

## 3. 菜单验收清单

### 3.1 今日

| ID | 菜单 | 路由 | 当前页面 | 验收重点 | 状态 |
| --- | --- | --- | --- | --- | --- |
| FM-01 | 业务工作台 | `/finance/workbench` | `FinanceWorkbenchView.vue` | 财务角色首屏是否围绕现金、待收、待付、成本缺口、异常流水；主行动是否唯一；门店范围和数据截止时间；七态 | PENDING |

### 3.2 客户

| ID | 菜单 | 路由 | 当前页面 | 验收重点 | 状态 |
| --- | --- | --- | --- | --- | --- |
| FM-02 | 客户 | `/workbench/customers` | `StoreCustomerView.vue` | 财务是否复用客户主域；回款风险、应收/合同/项目摘要；DEC-080 权限；无复制页面 | BUG / OPTIMIZE |

### 3.3 资金收付

| ID | 菜单 | 路由 | 当前页面 | 验收重点 | 状态 |
| --- | --- | --- | --- | --- | --- |
| FM-03 | 待我收款 | `/finance/receivables` | `FinanceReceivablesView.vue` | 应收待收/部分/逾期；客户/项目上下文；收款核销；余额和回读 | PENDING |
| FM-04 | 收款记录 | `/finance/receipts` | `FinanceReceiptsView.vue` | 收款事实、来源应收、台账/凭证回读；退款/冲正入口边界 | PENDING |
| FM-05 | 待我付款 | `/finance/payables` | `FinancePayablesView.vue` | 应付待付/部分/逾期；来源追踪；付款核销；成本不等于付款 | PENDING |
| FM-06 | 费用与报销 | `/finance/expenses` | `FinanceExpensesView.vue` | 审批/退回/归属/证据/付款/凭证；财务与提交人动作边界 | PENDING |
| FM-07 | 付款记录 | `/finance/payments` | `FinancePaymentsView.vue` | 付款核销、收款方、工人补录、台账/凭证回读；重复付款 | PENDING |
| FM-08 | 工人工资 | `/finance/salaries` | `FinancePayablesView.vue` | 工人/班组工资应付与付款；与员工工资严格分离；项目成本口径 | PENDING |

### 3.4 往来与账本

| ID | 菜单 | 路由 | 当前页面 | 验收重点 | 状态 |
| --- | --- | --- | --- | --- | --- |
| FM-09 | 供应商往来 | `/finance/partners` | `FinancePartnersView.vue` | 供应商应付、付款、余额、到期；主数据来源；跨店范围 | PENDING |
| FM-10 | 资金台账 | `/finance/ledger` | `FinanceLedgerView.vue` | 资金流水、来源、账户、收支、期初；作废/更正权限和审计 | PENDING |
| FM-11 | 客户退款 | `/finance/refunds` | `FinanceRefundsView.vue` | 退款与冲正区分；原收款保留；回读应收/台账/提成 | PENDING |

### 3.5 凭证与归档

| ID | 菜单 | 路由 | 当前页面 | 验收重点 | 状态 |
| --- | --- | --- | --- | --- | --- |
| FM-12 | 收付凭证 | `/finance/vouchers` | `FinanceVouchersView.vue` | 来源款项→凭证；金额一致；打印/重打/归档；合并条件 | PENDING |
| FM-13 | 凭证模板 | `/finance/voucher-templates` | `FinanceVoucherTemplatesView.vue` | 平台/企业模板、版本、发布、打印边界；历史凭证不回溯 | PENDING |

### 3.6 报价与成本

| ID | 菜单 | 路由 | 当前页面 | 验收重点 | 状态 |
| --- | --- | --- | --- | --- | --- |
| FM-14 | 报价 | `/finance/quotes` | `FinanceQuotesView.vue` | 财务成本视角；报价状态和成本完整性；是否应回归报价主域 | PENDING |
| FM-15 | 报价项目库 | `/designer/catalog` | 报价项目库主域页面 | 财务维护标准成本、批量导入、停用/启用、未引用删除；成本字段裁剪 | PENDING |

### 3.7 薪酬

| ID | 菜单 | 路由 | 当前页面 | 验收重点 | 状态 |
| --- | --- | --- | --- | --- | --- |
| FM-16 | 薪酬工作台 | `/finance/payroll` | `FinancePayrollWorkbenchView.vue` | 当前周期状态、待考勤、待复核、待发、工资条；唯一主行动 | PENDING |
| FM-17 | 薪酬方案 | `/finance/compensation-schemes` | `FinanceCompensationSchemesView.vue` | 方案有效性、工资项来源、历史快照；财务权限边界 | PENDING |
| FM-18 | 员工工资 | `/finance/employee-salaries` | `FinanceEmployeeSalariesView.vue` | 工资单复核/确认/作废/退回/发放的职责分离；已发不改 | PENDING |
| FM-19 | 员工提成 | `/finance/employee-commissions` | `FinanceEmployeeCommissionsView.vue` | 计提、释放、发放、状态和项目成本；工资与提成分链 | PENDING |
| FM-20 | 提成负向调整 | `/finance/commission-adjustments` | `FinanceCommissionAdjustmentsView.vue` | 退款/离职/跨期调整；下期工资单抵扣；重复冲减 | PENDING |
| FM-21 | 本店薪酬 | `/finance/store-payroll` | `FinanceStorePayrollView.vue` | 店长本店只读薪酬构成；无确认/作废/发放入口 | PENDING |

### 3.8 项目核算

| ID | 菜单 | 路由 | 当前页面 | 验收重点 | 状态 |
| --- | --- | --- | --- | --- | --- |
| FM-22 | 项目账本 | `/finance/projects` | `FinanceProjectsView.vue` | 项目收入、三态成本、成本完整性、毛利/结算摘要；成本不完整空值 | PENDING |
| FM-23 | 设计方案与报价 | `/finance/design-reviews` | `DesignReviewView.vue` | 财务查看设计方案/报价成本；是否应回归设计/报价主域 | PENDING |

### 3.9 帮助

| ID | 菜单 | 路由 | 当前页面 | 验收重点 | 状态 |
| --- | --- | --- | --- | --- | --- |
| FM-24 | 材料业务说明 | `/business-blueprints` | 业务说明页面 | 是否帮助财务理解材料/应付/成本；是否存在过时口径；是否保留 | PENDING |

## 3.10 首轮已验收记录

- `FM-02 客户`：结论 `BUG / OPTIMIZE`。财务当前使用 `StoreCustomerView` 表格，设计师使用 `DesignerCustomersView` 三栏工作面；“查看方案”跳转 `/designer/customers` 后真实落入 `/forbidden`。详见 `12-FM-02-customer-acceptance.md`；Web/App 页面总清单见 `13-customer-page-inventory-web-app-v0.1.md`；客户业务能力清单见 `14-customer-page-business-capability-list-v0.1.md`。

## 4. 角色验收矩阵

每个菜单至少覆盖当前适用的角色；不得用管理员或开发者身份代替真实角色验收。

| 角色 | 验收范围 | 禁止扩大 |
| --- | --- | --- |
| 总部财务 | Company；全部门店+单店 | 不获得财务高危动作、薪酬最终确认/作废/退回 |
| 门店财务 | 本店 | 不看跨店合计、其他门店名称和金额 |
| BOSS | 按现有权限复核财务超集能力 | 不冒充业主/客户；不跳过状态机 |
| 店长 | 本店普通记账、部分高危动作、本店薪酬只读 | 不跨店；不确认/作废/发放员工工资 |
| 普通员工 | 仅费用/本人工资条等既有能力 | 不访问财务读写或成本/利润 |

## 5. 页面级验收最低证据

| 维度 | 证据 |
| --- | --- |
| 菜单与路由 | 当前菜单配置、角色可见性、深链可用性 |
| 真实数据 | 当前测试企业/真实角色/现有 Provider 返回 |
| 业务任务 | 进入→判断→操作→反馈→回读→交接 |
| 七态 | 正常、加载、空、失败、无权限、长内容、极端值 |
| 金额/成本 | 同对象同口径；未知值不为 0；利润空值正确 |
| 权限 | 本店/全企业/跨店/跨租户负向 |
| 交互 | 主行动唯一；高危动作安全门；筛选/分页/返回保持 |
| 跨端 | App/Web 同 Provider、同状态、同金额、同错误、同回读 |
| UI | 1440/1280/1024 当前真实截图；DOM 字号、热区、溢出、焦点 |
| 视觉 | 视觉桥预检 + 人工/更强视觉模型复核；否则标记不可用 |

## 6. 本轮绝对不做

- 不改后端 Java 业务逻辑；
- 不新增/修改 Provider、实体、字段、状态机、DDL、权限、金额/成本/利润公式；
- 不重做财务导航；
- 不删除或重命名现有菜单；
- 不一次性改完所有页面；
- 不把跨域对象复制成新的财务页面；
- 不以界面换皮代替功能验收。

## 7. 完成定义

本台账只有在以下条件满足时才可标记完成：

- 24 个菜单全部有结论；
- 每个 `OPTIMIZE` 项有最小改动说明和回滚方式；
- 每个 `BUG/BACKEND_GAP` 项有独立问题记录；
- 所有菜单有角色范围、主行动和回读证据；
- 无权限、跨店、失败、空态和成本不完整至少各覆盖一次；
- 当前布局、路由和深链兼容性通过；
- 未经用户确认，不将验收发现直接扩展为后端改造。
