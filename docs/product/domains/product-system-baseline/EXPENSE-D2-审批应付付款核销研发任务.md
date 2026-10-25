# EXPENSE-D2 · 审批应付、付款核销与关闭研发任务

| 项 | 内容 |
| --- | --- |
| 关联问题 | `PI-20261022-018` |
| 业务裁决 | `DEC-091` / `BR-057` / `ANS-BIZ-20261022-012` |
| 研发交接 | `T12-BIZ-20261022-012` |
| 依赖 | `EXPENSE-D1` 已独立验收 PASS |
| 状态 | `ACCEPTED`（第三轮独立验收 PASS） |
| 变更级别 | L3：DDL、金额、支付、跨租户 |

## 1. 主闭环

```text
Approved Expense
-> 唯一 Payable(Expense)
-> finance/payment/save
-> PayableSettlement
-> Ledger(Expense)
-> 部分付款保持 Approved
-> 付清后 Expense Closed
```

## 2. 对象与约束

- `Expense.payable` 关联唯一 `Payable`，`payable_id` 有企业联合索引和外键。
- `Payable.sourceType = Expense`、`sourceId = expenseId`，企业+来源唯一。
- `Payable.type = Expense`，金额等于费用金额，门店/团队/项目/负责人/收款员工继承费用。
- `Payment`、`PayableSettlement`、`Ledger(Expense)` 复用现有财务主干。
- D2 不创建 `VoucherRecord`，凭证属于 D3。
- 费用应付只能单独付款，不支持多应付合并。

## 3. 权限

- 付款：BOSS 全企业、店长本店、总部财务全企业、门店财务本店。
- 财务/店长不能支付本人报销；BOSS 一人企业可按 `DEC-032` 例外。
- 付款方必须处于应付门店范围；跨店/跨企业一律拒绝。
- 收款姓名对报销费用由服务端强制使用费用收款员工姓名。

## 4. 金额与成本

```text
未付 = Payable.amount - Payable.paidAmount
部分付款：Expense.status = Approved，Payable.status = Partial
付清：Expense.status = Closed，Payable.status = Paid
```

- 审批创建应付时不得新增第二条成本。
- 项目报表只能计一次费用成本；不得同时按 Expense 和 Payable 双计。
- D2 不改公司/门店经营利润，D5 继续被 `GAP-070` 阻塞。

## 5. 迁移

- `129-企业费用D2应付关联.sql`
- `payable_id`、`idx_fin_expense_company_payable`、`fk_fin_expense_payable`
- 回滚：`rollback/129-企业费用D2应付关联-rollback.sql`

## 6. App/Web

- Expense 详情显示应付金额、已付、未付、付款状态、约定付款日。
- 满足 `allowedActions=pay` 时显示“安排付款”。
- Web 使用费用详情中的付款对话框调用 `finance/payment/save`；App 跳转现有财务付款入口并带 `payableId`。
- 付款后重新读取费用详情；失败显示“暂不可确认”。
- App/Web 不得新增第二套付款 Provider 或独立台账。

## 7. 验收

- 唯一应付、金额继承、来源唯一。
- 部分付款、全额付款、关闭、未付余额。
- 重复审批/重复付款/超付/未审批付款。
- 财务自付、店长自付、跨店、跨企业。
- 费用应付多付款拒绝。
- Payment/核销/Ledger 同事务，D2 无 Voucher。
- 项目费用不双计。
- 真实页面/真机/OSS 不可用则记录 BLOCKED。
