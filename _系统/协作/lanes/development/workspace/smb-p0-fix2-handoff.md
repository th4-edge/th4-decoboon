# 交接：REQ-20260918-SMB-P0-FIX-2（验收 rework）

- request_id: `REQ-20260918-SMB-P0-FIX-2`
- lane: development
- 输入：`docs/acceptance/中小装企P0实机复验报告.md`、`_系统/协作/lanes/acceptance/workspace/smb-p0/*.json`、`docs/acceptance/evidence/smb-p0/**`
- 结论：8 项 defect 已修复并自测；不宣布验收通过，等待 acceptance 独立复验
- 探针证据：`_系统/协作/lanes/development/workspace/smb-p0-fix2-probe.txt`

## 1. changed_files

后端 `src/main/java/com/th4/edge/decoboon/`
- `property/service/measures/save/support/Save.java`（DEF-001 执行人权限 + 重复提交不新增量房记录）
- `property/repository/MeasureRepository.java`、`quote/repository/QuoteRepository.java`（按状态/企业的事实查询）
- `customer/context/CustomerTagContext.java`、`customer/service/cust/info/support/Info.java`、`customer/service/cust/page/support/Page.java`（DEF-002/003 完成态口径 + 预约标签 + 按房屋投影）
- `project/repository/ProjectStageRepository.java`、`project/service/stages/update/support/Update.java`（DEF-004 行级锁并发串行化）
- `project/service/contract/save/support/Save.java`、`project/service/contract/update/support/Update.java`、`project/context/ProjectAccessContext.java`（DEF-006/008 403 语义 + 重复签约幂等）
- `finance/model/FinanceSummary.java`、`finance/context/FinanceReportContext.java`、`finance/service/summary/report/support/Report.java`（DEF-007 汇总毛利率）

- `views/project/ProjectDetailView.vue`（DEF-005 readiness pending 文案）
- `domains/experience/customerMarketing.ts`、`views/customer/CustomerDetailView.vue`、`views/customer/CustomerListView.vue`、`domains/crm/api.ts`（DEF-002/003 预约态标签、按 activeProperty 重算、列表筛选）
- `domains/finance/model.ts`、`domains/finance/api.ts`、`views/finance/FinanceOverviewView.vue`（DEF-007 汇总毛利率展示）

文档：`_系统/协作/lanes/development/worklog.md`、本交接、`_系统/协作/lanes/development/workspace/smb-p0-fix2-probe.txt`

## 2. fix_results

| Defect | 修复 | 证据 |
| --- | --- | --- |
| DEF-SMB-P0-001 | 已指派设计师时量房完成仅允许该设计师；其他角色 403 且不落库。另：重复提交不再新增量房记录 | 业务员对 303(Scheduled)/202(Completed) 均 `403 该房屋已指定设计师，量房完成必须由指定设计师执行`；DB `designer/executor/status/latest_measure_at` 与记录行数前后完全一致；指定设计师本人完成 101 返回 success，行数仍 1 |
| DEF-SMB-P0-002/003 | `hasMeasure` 只认 Completed；新增"已预约量房"标签；客户 360 标签/分级按 `propertyId` 重算；列表改用后端投影 | 303（仅 Scheduled）tags=`[source:WalkIn, measure:scheduled]`、评分 20、互动理由"未量房"；202（Completed）tags=`[source:WalkIn, measured]`、评分 38、"已完成量房" |
| DEF-SMB-P0-004 | 阶段开工先 `PESSIMISTIC_WRITE` 锁定阶段，并发串行化后走幂等返回 | 两并发请求均 `success=true`、同一 stageId/Doing/actualStart；StageOpen=1；日志无 deadlock、无 undefinedErrorCode |
| DEF-SMB-P0-005 | readiness 增加 pending 文案，未完成项显示"量房未完成"等 | 构建产物 `ProjectDetailView-*.js` 含"量房未完成"；pending 列表与提示均用 pending 文案 |
| DEF-SMB-P0-006 | `contract/update` 非 Boss 一律 403；`contract/save` 同企业无能力 403，跨企业/不存在 404 | StoreManager/Marketer/Pm/Designer 对 update 与 save 均 403；`quoteId=999999999999` 仍 404 |
| DEF-SMB-P0-007 | `FinanceSummary.grossMargin`（成本不完整=null） | 汇总与项目行/项目报告/结算同口径：完整项目毛利率均 37.2；存在成本不完整项目时汇总 `costComplete=false`、利润与毛利率为 null |
| DEF-SMB-P0-008 | 重复 Signed 幂等返回同一合同事实 | 对已 Effective 合同重复 Signed：`success=true`、同一 contractId、status=Effective、同一 projectId，合同数不变 |

## 3. verification

- 后端：`mvn -o -q test` → **43 suites / 133 tests / 0 failures / 0 errors / 0 skipped**（与验收基线一致）。
- 运行期探针（真实 HTTP + MySQL 回查，探针夹具已还原）：见 `smb-p0-fix2-probe.txt`。
- 未执行：真实业主微信授权会话（D4–D7 BLOCKED_EXTERNAL）、第二企业可写收款夹具（E6）、网络/序列化故障注入（C3/H3）、真实 MySQL 8 迁移演练。

## 4. restart_evidence

- 后端：`./run-dev.sh`（loader `target/lib,target/classes`，profile dev）→ PID **35035**，`lsof -nP -iTCP:8090 -sTCP:LISTEN` 显示 `*:8090 (LISTEN)`；探针全部返回真实业务响应。
- 说明：MySQL 只在沙箱外可达，故重启与探针在沙箱外执行；探针后已还原 101/202/303 量房、赵云项目/阶段、应收等夹具状态。

## 5. remaining_defects / risks

- RISK-SMB-P0-001（历史脏数据）仍未清理：如 202 房屋量房 `designer=李设计、executor=周业务` 为历史误写结果；如需统一为"executor 与 designer 分离后的正确值"，需要产品确认后单独出数据修复迁移（本轮未改历史数据）。
- 收款幂等并发窗口仍由数据库唯一约束兜底（预检查 + 唯一键），未做并发压测。
- 汇总毛利率在公司存在任一成本不完整项目时为 null（业务口径：整体利润不可确认）；项目行各自按其 `costComplete` 展示。
- `rate(numerator, 0)` 仍返回 0（无合同额的空企业汇总毛利率为 0），与项目行/结算实现保持一致；如需表达"无数据"应作为产品口径单独裁决。

## 6. blocked

- 合同签约/生效最终角色矩阵、业主线下代录、人工材料计划补录：按 T06 裁决保持 BLOCKED，本轮未改。
- 真实业主微信授权会话缺失（D4–D7 / BLOCKED_EXTERNAL），不得由员工代确认替代。

## 7. acceptance_needed

- 复跑 A4（业务员对已指派房屋必须 403/400 且 DB 不变）、I1/I2/I4（Scheduled 显示"已预约量房"、无完成分值、多房屋按 activeProperty 重算）、C4（并发两种组合）、B2（readiness 文案）、F2/F4（合同 403 语义与重复 Signed）、G1/G2/G3/G5（三视图毛利率一致）。
