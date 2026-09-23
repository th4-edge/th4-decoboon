# 阶段验收整改闭环 · acceptance 回执

- request_id: `REQ-20260921-RECTIFICATION-CLOSURE`
- final_status: `BLOCK`
- report: `docs/acceptance/阶段验收整改闭环独立验收报告.md`
- worklog: `_系统/协作/lanes/acceptance/worklog.md`
- test: `src/test/java/com/th4/edge/decoboon/owner/OwnerClosureAcceptanceTest.java`

## 独立结果

- 后端全量：76 suites / 230 tests / 0 failures / 0 errors / 0 skipped（Java 22）。
- OwnerClosureAcceptanceTest：15 tests / 0 failures / 0 errors / 0 skipped。
- App：type-check、build:mp-weixin、check:subpackages PASS。
- 通过：多轮追加、历史轮次不覆盖、第 2 轮起整改证据必填、通过后禁止新轮次、业主重复确认幂等、员工代确认 403、直接越权读取 404、账务冻结。

## 待 development 修复

- `DEF-RECTIFY-001` P1：存量 Pending acceptance `227467828918222848` 无第 1 轮，迁移未 backfill。
- `DEF-RECTIFY-002` P1：顺序重复提交返回 400，未返回已有 Pending 轮次；并发首提交出现 `undefinedErrorCode`。
- `DEF-RECTIFY-003` P1：业主驳回 `issues=[]` 仍成功，整改责任人/截止日期未强制。
- `DEF-RECTIFY-004` P2：App 业主驳回没有问题清单输入，也没有整改责任人和截止日期交互。
- `BLOCKED-RECTIFY-MIGRATION-001`：迁移 47 未在目标环境执行及做存量审计/回填演练。

## 复验条件

1. 迁移 47 在目标等价库完成执行、重复执行、回填预演和回滚验证。
2. 重复/并发提交返回已有 Pending 轮次，无未定义错误码。
3. 业主驳回缺少问题清单、整改责任人或截止日期时返回 400。
4. App 真实表单可完成驳回和整改登记。
5. 真实微信业主授权会话可用后，执行完整 UI 闭环；在此之前不得声称生产可用。
