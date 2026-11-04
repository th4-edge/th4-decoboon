# RUN-20261104-FIN-P0-REMOTE-RETEST-001 · 远端正式加固复验

## 基线

- 后端：`52991afc1ba8840ed5feaca87660beeff01ed245`
- Web：`39a9bc2a0ffae34748b5e0f7d6e9aab9af5376ad`
- App：`1c7abeb58ceb57c9218050b401d1b03e283a374d`
- 环境：昭青科技本地 `local-fixture`
- 生产数据写入：0

## 结果

| 用例 | 结果 | 说明 |
| --- | --- | --- |
| TC-FIN-ATTENDANCE-STATE-BYPASS-005 | PASS | `InPayroll` 直写被拒绝，状态保持原值 |
| TC-FIN-ATTENDANCE-CONFIRMED-EDIT-006 | PASS | 修改后退回 Pending，确认人/时间清空 |
| TC-FIN-FINANCE-STORE-SCOPE-007 | PASS | 滨湖店财务跨店更新被拒绝，目标值不变 |
| TC-FIN-PAYROLL-ATTENDANCE-BLOCK-001 | BLOCKED | API/DB通过，页面证据待执行 |
| TC-FIN-PAYROLL-LOCK-ATOMIC-002 | BLOCKED | API/DB通过，页面证据待执行 |
| TC-FIN-PAYROLL-PENDING-NO-REBUILD-003 | BLOCKED | API/DB通过，页面证据待执行 |
| TC-FIN-PAYROLL-DRAFT-RESTORE-004 | BLOCKED | API/DB通过，页面证据待执行 |

汇总：`PASS=3 / FAIL=0 / BLOCKED=4 / NOT_RUN=0`。

## 结论

远端正式加固提交已经通过三个原 P0 缺陷的本地运行时复验；原缺陷标记为 `FIXED_PENDING_ACCEPTANCE`，不自行关闭。主链页面证据仍需补齐，财务最终门禁仍未到 `--final` 条件。
