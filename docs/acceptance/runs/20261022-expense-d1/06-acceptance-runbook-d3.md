# EXPENSE-D3 独立验收运行手册

| 项 | 内容 |
| --- | --- |
| 问题 | `PI-20261022-018` |
| 切片 | `EXPENSE-D3` |
| 研发状态 | `PASS / ACCEPTED`（第二轮独立复验；首轮 FAIL 历史保留） |
| 验收范围 | 费用付款凭证来源快照、模板版本、PDF 哈希、幂等重打、费用详情凭证摘要、迁移 130 apply/rollback |
| 独立验收 | 第一轮 **FAIL**（11/10/1）；第二轮新快照 **PASS / ACCEPTED**（13/13，P1-003 CLOSED） |

## 必测

1. 未付款费用不得生成付款凭证，不得产生 Voucher/Payment 副作用。
2. 已付款费用生成凭证：`sourceType=Expense`、`sourceId=expenseId`、凭证金额=Payment.amount。
3. `source_snapshot` 包含费用编号、分类、项目/企业归属、提交人、收款员工、审批版本、付款金额/日期、模板版本、文件 SHA-256。
4. `Reimbursement + TRAVEL -> E-TRAVEL`、其他报销 `E-REIMBURSEMENT`、Direct `P-EXPENSE`。
5. 同一 paymentId 重打返回同一 voucherId/voucherNo/sha256/path，不覆盖历史文件。
6. `expense/expenses/info` 返回 payments/vouchers，voucher.paymentId 可回读。
7. `document/voucher/page|report` 支持 `expenseId` 过滤，不泄露跨企业/跨店凭证。
8. 迁移 130 在隔离 MySQL apply/rollback PASS；回滚保留 `db_voucher_record_source_rollback_130`。
9. 后端 Java 22 clean test；App/Web 构建；真实 Web/App/OSS 单独标记。

## 禁止

- 不得由研发自批 PASS。
- 不得在独立验收中修改产品代码、权限、金额语义或迁移。
- 不得将 H2/本地存储、Mock 渲染或研发截图冒充真实 UI/OSS。
- D3 未通过前不得进入 D4/D5；D5 继续受 `GAP-070` 阻塞。

## 证据入口

- `05-dev-evidence-d3.md`
- `evidence/d3/dev/`
- `evidence/d3/migration-130/`

## 执行结论

- 第一轮独立证据：`evidence/d3/independent-20261023/`，结论 **FAIL / P1-003 OPEN**，永久保留。
- 第一轮记录：`07-acceptance-record-d3.md` 第 1 节起。
- 第二轮独立复验：`07-acceptance-record-d3.md` “第二轮独立复验”起；证据 `evidence/d3/independent-reacceptance-20261023/`。
- 第二轮结论：**PASS / ACCEPTED**，13/13 HTTP，P0=0 / P1=0 / P2=0，P1-003 CLOSED。
- D3 可关闭；D4/D5 未启动；`GAP-070` 保持 OPEN；真实 Web/App/OSS BLOCKED；不发布。
