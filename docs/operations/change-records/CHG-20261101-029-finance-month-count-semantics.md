# CHG-20261101-029 · 财务本月资金笔数口径修正

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | `CHG-20261101-029` |
| 类型 | 财务指标缺陷修复 |
| 风险级别 | L2 |
| 状态 | DEV_VERIFIED_PENDING_USER |
| 负责人 | Codex |
| 日期 | 2026-11-01 |
| 关联需求/业务裁决 | 用户反馈小程序财务“本月资金”显示收 20 笔、付 46 笔，但本月回款为 0；指标字典 `monthReceiptAmount` / `monthPaymentAmount` |

## 2. 根因与范围

- 根因：`FinanceSummary.receiptCount/paymentCount` 是累计有效记录数，被前端错误放在“本月资金”标题下展示；本月金额本身已正确按日期过滤。
- 本次新增独立字段 `monthReceiptCount`、`monthPaymentCount`，只统计本月有效收款/付款。
- 保留累计 `receiptCount/paymentCount`，供累计口径页面和既有 Provider 使用。
- 不改变收款、付款、台账金额，不改变退款/冲正、门店范围或权限规则。

## 3. 数据对账（麦鸿快照，2026-11-01）

| 指标 | 数据 |
| --- | --- |
| 累计有效收款 | 20 笔，¥192,980.00 |
| 累计付款 | 46 笔，¥59,445.44 |
| 本月收款 | 0 笔，¥0.00 |
| 本月付款 | 1 笔，¥77.00 |
| 本月资金收入 | ¥0.00 |
| 本月资金支出 | ¥77.00 |
| 本月现金流 | -¥77.00 |

## 4. 实现

- 后端 `FinanceSummary`、`FinanceReportContext`、`finance/summary/report` 白名单增加本月笔数字段。
- App/Web 财务汇总模型解析新字段。
- App 页面显示“本月收 0 笔 · 本月付 1 笔”。
- Web 页面显示“本月收款 0 笔 · 本月付款 1 笔”。

## 5. 验证

| 检查 | 命令/页面 | 结果 |
| --- | --- | --- |
| 后端定向测试 | `mvn -Dtest=FinanceClosedLoopTest test` | PASS |
| Provider 契约 | `audit-provider-response-contract.py --check/--self-test` | PASS：547 providers，0 findings |
| App 类型检查/构建 | `npm run type-check`、`build:mp-weixin -- --mode development` | PASS |
| App 真实页面 | `/pages/finance/index` | PASS：本月收 0 笔、本月付 1 笔；本月回款 ¥0、支出 ¥77 |
| Web 类型检查/构建 | `npm run type-check`、`npm run build` | PASS |
| Web 真实页面 | `/finance/workbench` | PASS：本月收款 0 笔、本月付款 1 笔 |

## 6. 回滚

- 回滚条件：累计收款/付款列表统计被误改，或本月笔数与本月金额日期过滤不一致。
- 回滚步骤：恢复本记录涉及的 FinanceSummary、FinanceReportContext、Provider 白名单和 App/Web 展示字段。
- 实际是否回滚：否。

## 7. 结论

- 最终结论：开发自测通过，待用户确认；不改变金额事实，只修正展示口径。
