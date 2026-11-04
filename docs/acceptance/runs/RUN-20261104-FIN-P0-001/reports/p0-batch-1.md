# RUN-20261104-FIN-P0-001 · P0 第一批执行摘要

## 环境

- 环境：`local-fixture`
- 企业：昭青科技 `199500000000000001`
- 后端：`http://127.0.0.1:8090`
- Web：`http://127.0.0.1:8080`
- 后端提交：`5df3d993`
- Web：`c6aec7c`
- App：`ff02451`
- 生产数据写入：0

## 执行结果

| 用例 | 结果 | 结论 |
| --- | --- | --- |
| TC-FIN-PAYROLL-ATTENDANCE-BLOCK-001 | BLOCKED | API/DB通过：未确认考勤阻断生成且无写入；页面证据未完成 |
| TC-FIN-PAYROLL-LOCK-ATOMIC-002 | BLOCKED | API/DB通过：Draft + 2条InPayroll + 2条工资项；页面证据未完成 |
| TC-FIN-PAYROLL-PENDING-NO-REBUILD-003 | BLOCKED | API/DB通过：财务重建Pending被拒，BOSS退回Draft；页面证据未完成 |
| TC-FIN-PAYROLL-DRAFT-RESTORE-004 | BLOCKED | API/DB通过：删除Draft后工资项清零、考勤恢复Confirmed；页面证据未完成 |
| TC-FIN-ATTENDANCE-STATE-BYPASS-005 | FAIL | 普通更新接口可直接写InPayroll |
| TC-FIN-ATTENDANCE-CONFIRMED-EDIT-006 | FAIL | 已确认考勤可修改且保留旧确认责任与时间 |
| TC-FIN-FINANCE-STORE-SCOPE-007 | FAIL | 滨湖店财务可修改总店考勤 |

汇总：`PASS=0 / FAIL=3 / BLOCKED=4 / NOT_RUN=7`。

## 已确认缺陷

### DEF-FIN-P0-001 · 状态机绕过

`attendance/summary/update` 在无工资项时可直接将考勤写成 `InPayroll`，且保留旧确认信息。实际结果：`success=true`，DB状态变为 `InPayroll`。

### DEF-FIN-P0-002 · 已确认考勤静默修改

修改 `normalDays=24→23` 返回成功，状态仍为 `Confirmed`，`confirmed_by` 与 `confirmed_at` 保留旧值。

### DEF-FIN-P0-003 · 门店财务跨店越权

滨湖店财务按总店 `summaryId` 修改考勤，实际 `success=true`，总店考勤 `normalDays 24→1`。

## 未完成

- 4 条主链页面证据未完成，不能标记 PASS。
- 7 条 P0 尚未执行。
- 本轮发现 3 个缺陷未修复、未复验。
- 独立验收未执行。
