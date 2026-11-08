# CHG-20261108-003 · 工资确认门禁与付款月份展示热修

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | `CHG-20261108-003` |
| 类型 | 热修 / 薪酬状态机与跨端展示 |
| 风险级别 | L3 |
| 状态 | 开发自测完成，待提交发布 |
| 负责人 | Codex |
| 日期 | 2026-11-08 |
| 关联问题 | `CHG-20261108-002`：麦鸿装饰 2026-10 工资单在 `Draft` 被负责人直接确认 |
| 业务裁决 | `ANS-BIZ-20261108-001`、`T12-BIZ-20261108-001`、`EV-434` |
| 关联规则 | `BR-015`、`BR-038`、`DEC-017`、`DEC-097`、`DEC-098` |

## 2. 目标与范围

- 目标：
  - 后端只允许 `Pending → Confirmed`，禁止负责人直接确认 `Draft`；
  - 提交复核和确认时实时校验对应员工考勤已进入工资单且数据完整；
  - Web/App 仅在 `Pending` 展示确认入口；
  - 付款记录显式展示工资月份，避免 8 月、9 月工资付款被误认为重复。
- 非范围：
  - 不修改工资、考勤、提成、应付、成本和发放公式；
  - 不新增接口、DDL 或数据库迁移；
  - 不自动回改已确认或已发放历史；
  - 不处理员工付款并发锁的独立风险。
- 受影响端：后端、Web、App。

## 3. 业务口径

- `Draft`：财务/制单权限账号编辑和提交复核。
- `Pending`：企业负责人或本店店长可确认。
- `Confirmed`：形成应付和工资条待发事实。
- 确认前置：工资项非空、实发非负、当前工资项的每名员工都有同月 `InPayroll` 考勤且天数字段完整。
- 校验失败不得生成应付、工资条，不得修改工资单、工资项或考勤状态。
- 付款记录必须显式展示工资归属月份；名称中的月份不能替代结构化字段。

## 4. 实现

### 后端

- `PayrollSettlementContext`：
  - `confirm` 改为仅允许 `Pending`，`Confirmed` 保持幂等；
  - `submitReview` 与 `confirm` 共用实时考勤校验；
  - 实时加载同月考勤，逐工资项核对 `InPayroll` 和天数完整性。
- `finance/payment/page`：
  - `Payable` 响应白名单增加 `period`；
  - 新增响应投影契约测试。

### Web

- 员工工资详情：确认按钮仅对 `Pending` 展示；草稿增加“财务提交复核后才能确认”提示。
- 付款记录：新增“归属月份”列。

### App

- 工资单详情：确认按钮仅对 `Pending` 展示；草稿增加相同提示。
- 付款记录：工资付款显式展示“工资月份”，提成付款显式展示“提成月份”。

## 5. 验证

| 检查 | 命令/页面 | 结果 |
| --- | --- | --- |
| 后端定向测试 | `PayrollSheetReviewFlowTest,PayrollConfirmRoleTest,PayrollConfirmCommissionDecoupledTest,FinancePaymentPageProjectionContractTest` | PASS：7 + 1 + 1 + 1 个用例，0 失败 |
| 后端全量测试 | `mvn -q test` | PASS：318 个报告 / 1259 个用例，0 失败、0 错误、0 跳过 |
| Provider 响应契约 | `audit-provider-response-contract.py --check/--self-test` | PASS：providers=550 properties=544 findings=0 |
| Web 类型检查/构建 | `npm run type-check && npm run build` | PASS |
| App 类型检查/构建 | `npm run type-check && npm run check:ui && npm run build:mp-weixin && npm run check:subpackages` | PASS |
| 真实页面 | 财务 Draft、负责人 Draft/Pending、付款记录月份 | 待发布后执行 |

## 6. 风险与回滚

- 风险：历史 `Draft + 工资项` 如果仍存在，负责人将无法直接确认，必须由财务重新提交 `Pending`；这是本次收紧的预期行为。
- 回滚：回退后端/Web/App 代码，不涉及数据回滚。
- 数据回滚：本次热修不执行 DML；`CHG-20261108-002` 的数据回退记录随本分支保留。

## 7. 发布

- 生产后端：`scripts/build-release.sh` + `scripts/deploy-prod.sh`。
- Web：`scripts/deploy-web.sh`。
- App：只构建、上传开发版；正式提审与发布由用户本人完成。
- 发布记录：`docs/operations/releases/2026-11-08-payroll-confirm-gate-hotfix.md`。

## 8. 结论

- 当前状态：开发自测完成，待提交与热修发布。
- 开发自测不等于独立验收；真实页面和生产冒烟待发布后补。
