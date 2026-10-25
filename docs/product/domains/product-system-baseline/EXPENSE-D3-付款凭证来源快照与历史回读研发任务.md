# EXPENSE-D3 · 付款凭证来源快照与历史回读研发任务

| 项 | 内容 |
| --- | --- |
| 问题 | `PI-20261022-018` |
| 切片 | `EXPENSE-D3` |
| 当前门禁 | `DEV_VERIFIED_CANDIDATE / REACCEPTANCE_PENDING`（首轮 P1-003 已修复，迁移130隔离验证 PASS，待独立复验） |
| 业务依据 | `DEC-091` §8、`BR-057` §7、`T12-BIZ-20261022-012`、`DEC-020/BR-006/BR-018` |
| 依赖 | `EXPENSE-D2` 已独立验收 `PASS / ACCEPTED` |
| 结论 | 只从已付款费用生成凭证；来源链、模板版本和 PDF 快照不可回写历史；已授权实现 |

## 1. 两个真实场景

### 场景 A：项目现场垫付报销

| 项 | 内容 |
| --- | --- |
| 使用人 | 项目经理、店长、门店财务 |
| 触发事件 | 项目经理垫付 680 元购买项目保护材料，费用已审批并完成付款 |
| 当前动作 | 财务在费用详情点击“生成付款凭证”，选择企业已发布的报销付款模板 |
| 系统下一步 | 生成 `VoucherRecord`，来源链为 `VoucherRecord -> Payment -> Payable(Expense) -> Expense`；保存费用编号、分类、项目、提交人、收款员工、审批版本、付款金额/日期、模板版本、文件哈希；返回 PDF |
| 成功结果 | 费用详情、付款记录、凭证金额和来源一致；同一付款重打同号同文件 |
| 异常/恢复 | 未付款、付款已作废、费用不属于当前企业/门店、模板未发布时拒绝；模板升级后重打仍读取原版本 PDF |

### 场景 B：企业直付费用

| 项 | 内容 |
| --- | --- |
| 使用人 | BOSS、总部财务 |
| 触发事件 | 企业直接支付 333 元办公费用，已完成付款 |
| 当前动作 | 财务生成企业费用付款凭证，不关联项目 |
| 系统下一步 | 凭证来源快照记录企业归属、费用分类、提交人、收款方、审批版本和付款事实；PDF 不出现项目字段 |
| 成功结果 | 企业费用不进入项目成本；凭证金额只取付款金额；历史 PDF 可回读 |
| 异常/恢复 | 手工 `CompanyExpense` 不得伪造 Expense 来源；模板或 PDF 读取失败显示“暂不可确认”并保留重试入口 |

## 2. 当前实现与缺口

已有：

- `VoucherRecord` 已保存 `templateVersion`、`path`、`sha256`、文件大小、生成人和状态。
- `VoucherItem` 已保存 `Payment` 来源、金额、日期、账户、分类和备注。
- `Payment` 已关联 `Payable`；费用应付的 `Payable.sourceType=Expense/sourceId=expenseId`。
- `ExpenseRevision` 保存审批版本快照；费用审批后金额、分类、项目、收款人等事实稳定。
- `DocumentTemplateVersion` 保存不可变模板版本；`templateForIssue` 优先使用当前发布版本。
- `document/voucher/build` 对同一来源组合幂等返回原凭证，不重复生成文件。

缺口：

- 费用付款凭证目前只按通用 Payment 凭证生成，没有显式的费用来源快照。
- 费用详情尚未返回凭证状态、凭证号、模板版本和文件哈希。
- 缺少按费用查凭证的直接索引，只能通过 Payment 反查。
- 缺少费用模板预设的明确选择规则和历史 PDF 回读验收。

## 3. 拟定技术方案（待确认）

### 3.1 DDL 提案

在 `db_voucher_record` 增加 nullable 来源快照字段：

```sql
ALTER TABLE db_voucher_record
  ADD COLUMN source_type VARCHAR(32) NULL COMMENT '来源类型；费用凭证为 Expense',
  ADD COLUMN source_id BIGINT NULL COMMENT '来源业务主键；费用凭证为 expense_id',
  ADD COLUMN source_snapshot LONGTEXT NULL COMMENT '发行时费用来源快照JSON',
  ADD INDEX idx_voucher_company_source (com_id, source_type, source_id);
```

说明：

- 非费用凭证保持 `NULL`，不影响既有 Payment/Receipt 凭证。
- `templateVersion`、`sha256` 复用现有字段，不重复建模。
- 不新增外键，`source_type/source_id` 为受控多态来源；企业隔离仍由 `com_id` 强制。
- 迁移文件拟为 `130-企业费用D3凭证来源快照.sql`，必须附回滚。

### 3.2 来源快照

`source_snapshot` 固定包含：

- `expenseId`、`expenseNo`、`expenseStatus`
- `expenseCategory`、`paymentMode`
- `projectId/projectNo` 或企业归属摘要
- `requesterId/requesterName`
- `payeeEmployeeId/payeeEmployeeName`
- `approvalVersion`、`approvedById/approvedAt`
- `paymentId`、`paymentAmount`、`paidAt`
- `templateId`、`templateVersion`
- `fileSha256`

金额只取 `Payment.amount`，不重新计算费用金额或应付金额。

### 3.3 模板选择

- `Direct` → `P-EXPENSE`（费用报销付款单）
- `Reimbursement` → `E-REIMBURSEMENT`
- `Reimbursement + category.code=TRAVEL` → `E-TRAVEL`

调用方显式传入的 `presetCode` 只能选择当前企业已发布的 Payment 模板；费用凭证默认使用上述规则。该映射属于产品路由规则，不改业务金额或状态。

### 3.4 Provider 与页面

- `document/voucher/build`：识别费用付款，写来源快照，渲染费用 PDF。
- `document/voucher/report|page`：支持按 `expenseId` 过滤，返回 `sourceType/sourceId/voucherNo/templateVersion/sha256/url`。
- `expense/expenses/info`：返回费用关联凭证摘要；无凭证时返回 null，接口异常显示“暂不可确认”。
- App/Web 费用详情增加“生成/查看付款凭证”，不新增凭证权限。

## 4. 权限

- 生成/打印继续服从 `DEC-020`、`BR-006`、`PI-20261022-016`：BOSS/财务按企业范围，店长按本店范围；不新增店长凭证权。
- 费用详情凭证摘要继续使用费用数据范围；跨企业/跨店/跨项目返回 403/404。
- 财务/店长不得通过凭证入口绕过费用付款或审批规则。

## 5. 验收矩阵

| 编号 | 场景 | 预期 |
| --- | --- | --- |
| D3-01 | 未付款费用生成凭证 | 拒绝，无 Voucher 副作用 |
| D3-02 | 已付款费用生成凭证 | 凭证金额=付款金额，来源链可读至 Expense |
| D3-03 | 同一付款重打 | 同一 voucherNo/path/sha256，不覆盖历史 |
| D3-04 | 模板升级后重打 | 仍读取发行时模板版本和原 PDF |
| D3-05 | 项目费用 | PDF 显示项目摘要，来源快照含项目 |
| D3-06 | 企业费用 | PDF 不显示项目，企业归属正确 |
| D3-07 | 直接/报销/差旅模板路由 | 使用对应已发布 Payment 模板 |
| D3-08 | 跨企业/跨店/越权 | 403/404，无文件泄露 |
| D3-09 | 无模板/模板未发布 | 业务错误，不生成半成品 |
| D3-10 | 费用详情凭证摘要 | 与凭证列表、PDF 元数据同源 |
| D3-11 | App/Web 动作 | 均显示生成/查看状态，失败保留重试 |
| D3-12 | 全量回归 | Java 22 clean test + 定向 HTTP + PDF 哈希/回读 |

## 6. 回滚

- 代码回滚：关闭费用凭证入口，恢复通用 Payment 凭证路径；已生成凭证和 PDF 只读保留。
- 迁移回滚：删除来源快照字段和索引；不删除历史 PDF。
- 不重算金额、不删除 Payment/Payable/Expense/Ledger。

## 7. 停止门禁

当前只完成 D3 蓝图与计划。开始编码前必须由用户确认：

1. 是否批准 `130-企业费用D3凭证来源快照.sql` 的 DDL；
2. 是否允许进入 `EXPENSE-D3` 编码与定向测试；
3. 是否接受按 `Direct/E-REIMBURSEMENT/E-TRAVEL` 的默认模板路由。

未确认前不得创建迁移、修改实体、改动 Provider 或发布。
