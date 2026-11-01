# CHG-20261102-003 · 设计师报价接受留痕投影

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261102-003 |
| 类型 | 缺陷修复 |
| 风险级别 | L2 |
| 状态 | `COMMITTED / DEFECT RETEST PASS / APP BLOCKED` |
| 负责人 | codex |
| 日期 | 2026-11-02 |
| 关联 | `DEF-20261102-004`、`TC-DQ-PERMISSION-QUOTE-005`、`DEC-071` |

## 2. 修复

- 问题：`quote/designer/info` 未投影 `acceptedBy`；报价 Accepted 后设计师页面只能看到“已由设计师登记”，无法核对具体登记人和接受时间。
- 修复：设计师详情白名单加入 `acceptedBy`；Web Quote 模型/API 归一化接受登记字段；报价工作区展示登记人、接受时间和“员工登记客户接受，不代表客户系统自助确认”。

## 3. 验证

| 检查 | 结果 |
| --- | --- |
| Marketer置Accepted | 403，报价保持Submitted |
| Designer置Accepted | PASS，acceptedBy=审计设计，acceptedAt=2026-11-02 00:38:27 |
| 重复置Accepted | 400，首次登记人不被覆盖 |
| 金额/成本快照 | 15,103.75 / 10,109.00不变 |
| 设计师详情 acceptedBy/acceptedAt | PASS |
| Web页面接受留痕 | PASS |
| 后端定向测试 | PASS：干净提交 worktree `mvn -q -Dtest=FinanceSettlementPageCostProjectionTest,QuoteViewContractTest test` |
| Web type-check/build | PASS：干净提交 worktree `npm run build` |
| App端 | BLOCKED，无授权微信身份 |

复验：`RUN-20261102-QUOTE-TRACE-RETEST-001`，缺陷级 `VERIFIED`，整条跨端用例 `BLOCKED`。

## 4. 发布

已提交到本地 `dev`；未推送、未发布。主工作区仍有大量无关未提交变更，不能作为发布候选。
