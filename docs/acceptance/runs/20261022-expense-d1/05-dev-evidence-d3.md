# EXPENSE-D3 研发证据

| 项 | 内容 |
| --- | --- |
| 问题 | `PI-20261022-018` |
| 切片 | `EXPENSE-D3`：付款凭证、来源快照、模板版本和历史 PDF 回读 |
| 业务裁决 | `DEC-091` §8、`BR-057` §7、`T12-BIZ-20261022-012` |
| 日期 | 2026-10-23 |
| 状态 | `DEV_VERIFIED_CANDIDATE / REACCEPTANCE_PENDING` |
| 结论 | 首轮独立验收 P1-003 已修复并完成开发侧复验；等待独立复验，不进入 D4/D5 |

## 实现

- 费用付款凭证通过 `Payment` 生成，来源链为 `VoucherRecord -> Payment -> Payable(Expense) -> Expense`。
- `VoucherRecord` 新增 `sourceType/sourceId/sourceSnapshot`，来源快照包含费用编号、分类、项目/企业归属、提交人、收款员工、审批版本、付款金额/日期、模板版本和文件哈希。
- 采用 `REIMBURSEMENT + TRAVEL -> E-TRAVEL`、其他报销 `E-REIMBURSEMENT`、企业直付 `P-EXPENSE` 的默认模板路由；允许调用方选择已发布 Payment 模板。
- 同一付款重复生成返回原 `voucherId/voucherNo/sha256/path`，不重写历史 PDF。
- `expense/expenses/info` 返回付款记录和已发行凭证摘要；`document/voucher/page|report` 支持按 `expenseId` 过滤。
- App/Web 费用详情增加付款记录、凭证状态、生成/查看付款凭证入口。
- 未修改金额、成本口径、审批状态机或权限范围。

## 迁移

- `130-企业费用D3凭证来源快照.sql`
- `rollback/130-企业费用D3凭证来源快照-rollback.sql`
- `MIGRATION_END=130`
- 隔离 MySQL 8.4 应用/回滚 PASS；回滚保留 `db_voucher_record_source_rollback_130` 快照备份表。

## 验证结果

| 命令/范围 | 结果 |
| --- | --- |
| `ExpenseVoucherLifecycleTest` | PASS，2/2（含非法 Expense 来源拒绝） |
| `SummaryImplementationRulesTest` | PASS，7/7（EntityGraph ≤4） |
| 隔离 D2 回归 + D3 HTTP | PASS，18/18，0 failure/error/skip |
| Java 22 主源码 clean compile | PASS，2087 文件 |
| Java 22 完整仓库 clean test | PASS，264 suites / 1074 tests / 0 failure/error/skip |
| App type-check / build:mp-weixin | PASS |
| Web 隔离 type-check / production build | PASS（仅临时兼容无关 FinanceAttendanceDrawer 模板类型/文案问题） |

## HTTP 证据

- `VoucherRecord.sourceType=Expense`，`sourceId=expenseId`。
- 差旅报销 `templatePresetCode=E-TRAVEL`，`templateVersion=2`，`fileSha256` 为 64 位 SHA-256。
- 同一付款重打 `voucherId`、`voucherNo`、`sha256` 不变。
- `expense/expenses/info` 返回 1 条 Payment 和 1 条 Voucher，Voucher `paymentId` 可回读。
- `document/voucher/page?expenseId=...` 仅返回该费用凭证。
- 跨店支付凭证生成返回 403/404，无 `hibernateErrorCode`。

证据目录：`evidence/d3/dev/`、`evidence/d3/migration-130/`。

## P1-003 修复（2026-10-23）

- 根因：`VoucherContext.expenseForPayment` 对声明 `sourceType=Expense` 但无法解析的费用来源返回 null，调用方随后继续走普通付款模板，生成“工资发放凭证”。
- 修复：Payable 声明 Expense 来源时，必须解析到当前企业 Expense；`sourceId` 为空、跨企业/不可见、Expense 与 Payable 不一致、费用未 Approved/Closed 时，在模板选择、PDF 和存储副作用前返回业务 404/400。
- 回归：`ExpenseVoucherLifecycleTest` 2/2 PASS；D2 回归 + D3 HTTP 18/18 PASS；Java 22 主源码编译 PASS；完整仓库 264 suites / 1074 tests / 0/0/0 PASS。
- 证据：`evidence/d3/dev/java22-compile-after-p1-003.log`、`full-repository-clean-test-after-p1-003.log`、`d2-regression-and-d3-http-final2.log`。

## 未覆盖

- D3 独立复验。
- 真实 Web/App 浏览器、真机/微信开发者工具和真实 OSS PDF 下载/回读。
- dev/test/prod 数据库迁移和发布。
- `EXPENSE-D4`、`EXPENSE-D5` 未启动；`GAP-070` 仍 OPEN。

独立验收通过前不得标记 D3 PASS 或进入 D4/D5。
