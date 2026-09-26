# Acceptance Worklog

## Current

`REQ-20260926-AI-USAGE-BRIEF-ACCEPTANCE` 联合独立验收完成。后端 `5252afa`、Web `68e6bff`；A0.1/A1 前序 7 项缺陷全部关闭，独立验收测试 9/9、全量 385/0/0/0，Provider 348/12/336，迁移 64 隔离 MySQL 双跑 PASS，Web 静态/Build 门禁 PASS。仅并发幂等回查、真实浏览器流程和生产库直连不可执行，最终状态 `CONDITIONAL`。

## Outputs

| title | path | audience | status |
| --- | --- | --- | --- |
| AI用量与经营参谋联合独立验收报告 | `docs/acceptance/AI用量与经营参谋联合独立验收报告.md` | development / product-planning / user | conditional |
| Provider 安全验收测试 | `src/test/java/com/th4/edge/decoboon/security/ProviderSecurityAcceptanceTest.java` | development / acceptance | 3 passed |
| 小程序企业负责人全链路闭环验收报告 | `docs/acceptance/小程序企业负责人全链路闭环验收报告.md` | development / product-planning / user | pass_with_defects |
| 小程序全链路回归规范（含根因反思） | `docs/acceptance/小程序全链路回归规范.md` | development / product-planning / acceptance | ready |
| 小程序全路由巡检脚本与结果 | `_系统/协作/lanes/acceptance/workspace/miniapp-boss-pilot/route-sweep.cjs`、`route-sweep-result.json` | development / acceptance | ready |
| 后端日志时间窗分析脚本 | `_系统/协作/lanes/acceptance/workspace/miniapp-boss-pilot/analyze-backend-log.sh` | development / acceptance | ready |
| 客户经营预约量房指标卡独立验收报告 | `docs/acceptance/客户经营预约量房指标卡独立验收报告.md` | development / product-planning / user | conditional |
| 预约量房工作台 2.0 P0 独立验收报告 | `docs/acceptance/预约量房工作台2.0独立验收报告.md` | development / product-planning / user | block |
| 预约量房工作台独立验收报告 | `docs/acceptance/预约量房工作台独立验收报告.md` | development / product-planning / user | block |
| 企业负责人首页与客户经营工作区独立验收报告（误派单，已作废） | `docs/acceptance/企业负责人首页与客户经营工作区独立验收报告.md` | development / product-planning / user | void / superseded |
| 阶段验收整改闭环独立验收报告（返工复验） | `docs/acceptance/阶段验收整改闭环独立验收报告.md` | development / product-planning / user | conditional |

## Notes

- 职责：负责测试用例设计、独立验证、缺陷结论和验收报告，不代替开发修改生产代码。
- `password-login`、`password-setup` 为公开路由；`password-change`、`logout` 继承 `Security`。
- Provider 安全测试使用 196 个显式路由的当前清单，不再依赖旧的 192/9 固定预期。

## Verification

- `mvn -o -q -Dtest=ProviderSecurityAcceptanceTest,SubscriptionAccessFilterAcceptanceTest,ProductionReleaseBaselineAcceptanceTest test`：PASS。
- `mvn -o -q test`：26 suites，79 tests，0 failures，0 errors，0 skipped。
- Provider 扫描：`total=196 protected=185 public=11 violations=0`。

## Defects

| id | severity | status | summary |
| --- | --- | --- | --- |
| `DEF-MP-BOSS-001` | High/P1 | OPEN | Boss 进入设计师列表调用店长专属 `statistic/store/report` 得 403，数据全为 0 |
| `DEF-MP-BOSS-002` | High/P1 | OPEN | `company/member/info` 对本人返回"不能操作自己的员工记录"，成员详情页空白 |
| `DEF-MP-BOSS-003` | Medium/P2 | OPEN | 已锁定方案的洽谈页直接 `design/talk/build`，无法查看既有洽谈记录 |
| `DEF-MP-BOSS-004` | Low/P3 | OPEN | 参数异常时页面 `onLoad` 未捕获 Promise 异常，出现白屏与未处理异常 |

## Next

1. 准备真实测试后端、员工账号和业务夹具。
2. 验证真实接口字段、分页、筛选、401/403 和退出跨页面行为。
3. 正式发布前替换生产占位域名并完成 HTTPS/备案/CORS 检查。
## 2026-09-16 小程序 Boss 全链路闭环验收

- 环境：本地 `th4-decoboon`（dev，8090）+ 本地 MySQL `decoboon` + 微信开发者工具 `th4-decoboon-app/dist/dev/mp-weixin`。
- 身份：BOSS 张伟 `18660219918`（合肥测试装饰工程有限公司）。
- 巡检：57/57 路由可达；未处理异常 1 次；3 个页面出现功能性失败。
- 闭环：小程序 UI 新建客户（`225745825844690944`）+ 记录跟进（`225745919914541056`）；其余节点沿用同一 Boss 会话完成量房、设计需求/方案/区域/分项、方案构建、报价（`225746268830302208`）、提交/接受、合同签约（`225746324421607424`）、合同生效 → 项目 `225746343807680512`（阶段 1、应收 3）自动生成，并在小程序项目详情页复核渲染正常。
- 后端业务校验（区域缺失、分项缺失、报价状态机、方案锁定）均按预期拒绝，判定为正确行为而非缺陷。

## Next

1. 将 `DEF-MP-BOSS-001/002/003/004` 移交 development lane 修复，acceptance 按同一四元矩阵复验。
2. 扩展巡检脚本支持 `--role`，覆盖店长/团队长/顾问/设计师/项目经理/履约/财务 7 类角色。
3. 补充业主端小程序闭环与真机（非开发者工具）回归。
4. 在新建项目 `225746343807680512` 上继续项目执行段（任务开工 → 日报 → 阶段验收 → 收款）写闭环。

## 2026-09-16 测试基线同步（历史断言清零）

- `ProviderSecurityAcceptanceTest`：路由总数断言 196 → **197**，受保护路由 185 → **186**（public 11 保持不变），与 `project/contract-change/batch-save` 等新增 Provider 对齐。
- `ProductionReleaseBaselineAcceptanceTest`：
  1. 原 `doesNotContain("127.0.0.1")` 与 `server.address: ${SERVER_ADDRESS:127.0.0.1}` 的回环绑定策略冲突 → 改为精确禁止外部依赖硬编码回环（`jdbc:mysql://127.0.0.1`、`127.0.0.1:3306`、`127.0.0.1:6379`、`localhost`、`mysqladmin`、`password: 123456`）。
  2. Runbook 断言由已失效的"尚未在真实生产等价库执行迁移和恢复演练"改为当前事实文本"备份恢复和回滚演练尚未完成"+"存量库差异迁移、`adopt` 和恢复演练尚未执行"。
- 结果：`mvn -o -q test` → **83 tests, 0 failures, 0 errors, 0 skipped（全绿）**。
- 说明：本次只改测试断言与规范文档，未修改生产代码，未发版。


## 2026-09-18 中小装企 P0 实机复验

- 请求：`REQ-20260918-SMB-P0-ACCEPT`。
- 账号：Boss、店长、团队负责人、业务员、设计师、PM、履约、财务及第二业务员共 9 个真实本地账号。
- 独立夹具：客户 `226701371305361408`，项目 `226702200000000003`；未修改既有赵云项目语义数据。
- 通过：A1–A3/A5、B1/B3/B5、C1/C2/C5、D2/D3/D-UI、E1–E5/E7/E8、F1/F3/F5、G2/G3/G5/G6、H1/H2、I3；F4 重复生效不新增项目，但重复 Signed 返回 400，记为 PARTIAL。
- 失败：
  - `DEF-SMB-P0-001` P0：Marketer 周业务完成已指派李设计的量房；DB `designer=李设计、executor=周业务、status=Completed`。
  - `DEF-SMB-P0-002/003` P1：Scheduled 量房显示“已量房”并提前获得完成评分，多房屋顶部标签污染当前房屋状态。
  - `DEF-SMB-P0-004` P1：并发首次开工一个请求返回 `undefinedErrorCode`，DB 已提交；日志显示 MySQL deadlock。
  - `DEF-SMB-P0-005` P1：未完成量房的 readiness 文案显示“开工前仍需完成：量房结果已完成”。
  - `DEF-SMB-P0-006/007/008` P2：合同 API 部分非 Boss 返回 404 而非 403；FinanceSummary 无 grossMargin 字段；重复 Signed 合同请求未返回同一事实。
- BLOCKED：真实业主授权会话（D4–D7）、第二企业收款夹具（E6 跨企业）、故障注入（C3/H3）、真实 0 成本（G4）。
- 报告：`docs/acceptance/中小装企P0实机复验报告.md`。
- 证据：`docs/acceptance/evidence/smb-p0/**`、`_系统/协作/lanes/acceptance/workspace/smb-p0/**`。
- 最终状态：`BLOCK`。development 修复 A4、I1/I2/I4、C4 和 readiness 文案后再复验。


## 2026-09-18 SMB P0 第二轮独立复验

- 请求：`REQ-20260918-SMB-P0-ACCEPT-2`。
- 指定返工结果：
  - A4 PASS：Marketer UI/API 返回 403，DB status/executor/latestMeasureAt/records 不变；Designer 完成任务。
  - I1/I2/I4 PASS：Scheduled 返回 `measure:scheduled`、评分 20；切换 101/303 按 activeProperty 重算。
  - C4 PASS：两个并发请求均 success，同一 Doing 事实，StageOpen=1。
  - B2 PASS：UI 显示“量房未完成”，后端 400，DB 无部分提交。
  - F/F4 PASS：非 Boss 保存/更新 403，缺失对象 404，重复 Signed 返回同一合同/项目。
  - G PASS：缺成本时 summary/row 为 null；补齐成本后 summary/row/settlement 毛利均 37.2。
  - 回归 A1–A3/A5、B1/B3/B5、C1/C2/C5、E1–E5/E7/E8、F1/F3/F5、G2/G3/G5/G6、H1/H2、I3 通过。
- 新发现：
  - `DEF-SMB-P0-009` P1：Designer 重复完成且未传 measureTime 时，历史 measureTime/latestMeasureAt 被覆盖为当前时间。
  - `DEF-SMB-P0-010` P2：量房响应 executor.empId=205，但 executor.role 返回 Marketer，会话和 DB 均为 Designer。
- 仍阻断：D4–D7 客户授权、E6 跨企业、C3/H3 故障注入、G4 真实 0 成本。
- 报告：`docs/acceptance/中小装企P0实机复验报告.md`。
- 证据：`docs/acceptance/evidence/smb-p0/recheck2/**`、`_系统/协作/lanes/acceptance/workspace/smb-p0/recheck-*.json`。
- 最终状态：`CONDITIONAL`。


## 2026-09-18 SMB P0 第三轮独立复验

- 请求：`REQ-20260918-SMB-P0-ACCEPT-3`。
- DEF-SMB-P0-009 CLOSED：Completed 量房顺序 3 次、并发 3 次，不带 measureTime；measureTime、latestMeasureAt、executor、designer、status、sitePhotos、structureImages、attachments、记录数全部不变。
- DEF-SMB-P0-010 CLOSED：Designer 响应 executor=`empId 205 / 李设计 / Designer`，仅返回 empId/name/role 白名单。
- A4 CLOSED：Marketer UI/API 重复 completion 仍先返回 403，未因幂等分支放行。
- 回归通过：I1/I2/I4、C4、B2、F/F4、G、E1–E5/E7/E8、H1/H2、I3。
- 外部阻断保留：D4–D7 客户授权、E6 跨企业收款、C3/H3 故障注入、G4 真实 0 成本。
- 报告：`docs/acceptance/中小装企P0实机复验报告.md`。
- 证据：`docs/acceptance/evidence/smb-p0/recheck3/**`、`_系统/协作/lanes/acceptance/workspace/smb-p0/recheck3-*.json`。
- 最终状态：`CONDITIONAL`（本地返工批次通过独立复验，外部场景未 PASS）。

## 2026-09-21 阶段验收整改闭环独立验收

- 请求：`REQ-20260921-RECTIFICATION-CLOSURE`。
- 业务依据：`SCN-010`、`BR-008`、`DEC-007` 和用户裁决 `REQ-BIZ-20260921-002`。
- 自动化：Java 22 下 `mvn -o -q test` = 76 suites / 230 tests / 0 failures / 0 errors / 0 skipped；`OwnerClosureAcceptanceTest` = 15/0/0/0。
- PASS：多轮追加、第 4 轮通过、历史轮次不覆盖、复验整改说明与照片必填、通过后禁止新轮次、业主重复确认幂等、员工代确认 403、跨企业/非参与人直接读取 404、账务字段冻结。
- FAIL：
  - `DEF-RECTIFY-001` P1：存量 Pending acceptance `227467828918222848` 无第 1 轮，迁移未 backfill。
  - `DEF-RECTIFY-002` P1：顺序重复返回 400 而非已有 Pending 轮次；并发首提交出现 `undefinedErrorCode`。
  - `DEF-RECTIFY-003` P1：业主驳回 `issues=[]` 仍成功，责任人和截止日期未强制。
  - `DEF-RECTIFY-004` P2：App 业主驳回没有问题清单输入，也没有责任人和截止日期交互。
- BLOCKED：迁移 47 未在目标环境执行；真实微信授权会话、真机和视觉走查未完成；生产等价并发/故障注入未执行。
- 证据：`_系统/协作/lanes/acceptance/workspace/rectification-closure/**`、`src/test/java/com/th4/edge/decoboon/owner/OwnerClosureAcceptanceTest.java`。
- 报告：`docs/acceptance/阶段验收整改闭环独立验收报告.md`。
- 最终状态：`BLOCK`。

## 2026-09-22 阶段验收整改闭环返工复验

- 请求：`REQ-20260921-RECTIFICATION-CLOSURE`。
- 变更：`DEF-RECTIFY-001..004`；更新 `OwnerClosureAcceptanceTest` 旧契约 helper，并新增存量回填、顺序幂等、并发首提、驳回四要素校验用例。
- 自动化：Java 22 下 `OwnerClosureAcceptanceTest` = 19/0/0/0；全量 `mvn -o -q test` = 76 suites / 234 tests / 0 failures / 0 errors / 0 skipped。
- `DEF-RECTIFY-001` CLOSED（本地）：隔离 MySQL 8.4 执行原版迁移 47 三次；Pending/Rejected/Confirmed 各回填 `roundNo=1`，状态、问题、证据、责任人、截止日期完整，重复轮次 0。
- `DEF-RECTIFY-002` CLOSED（本地）：顺序重复返回同一 Acceptance，不新增轮次；H2 两线程并发首提均成功，同一 Acceptance、一条 `roundNo=1`，无 `undefinedErrorCode`。
- `DEF-RECTIFY-003` CLOSED（本地）：缺问题/责任人/截止日期分别 400；跨企业/非项目团队责任人 400；完整驳回后 Acceptance 与 Round 均落问题、责任人和截止日期。
- `DEF-RECTIFY-004` 代码/构建 PASS，真机 BLOCKED_EXTERNAL：App release build 通过；表单三项输入齐全，API 失败不清空已填内容；真实微信授权、真机选择日期/图片和提交未执行。
- 仍 BLOCKED：迁移 47 未在目标等价数据库执行；真实微信授权会话和真机路径未完成；长时无限循环压力测试未执行。
- 报告：`docs/acceptance/阶段验收整改闭环独立验收报告.md`。
- 证据：`_系统/协作/lanes/acceptance/workspace/rectification-closure/recheck2-results.json`。
- 最终状态：`CONDITIONAL`。

## 2026-09-22 企业负责人首页与客户经营工作区独立验收

- 请求：`REQ-20260922-BOSS-CUSTOMER-WORKSPACE`。
- 独立探针：负责人筛选/分页 PASS；搜索+门店+状态+负责人+排序组合 PASS。
- 失败：
  - `DEF-BOSS-CUSTOMER-001` P1：门店筛选后有效跟进和签约指标仍使用全企业跟进/合同，B 店事实泄漏到 A 店指标。
  - `DEF-BOSS-CUSTOMER-002` P2：`inRange` 使用闭区间结束点，今天 00:00 的记录同时计入今天与昨日。
- UI：静态发现 BOSS 客户内滚动区底部安全间距可能不足，TabBar/FAB 可能遮挡末项；开发者工具/真机未执行，BLOCKED。
- 自动化：`mvn -o -q -DforkCount=0 test` = 80 suites / 241 tests / 0 failures / 0 errors / 0 skipped。
- 报告：`docs/acceptance/企业负责人首页与客户经营工作区独立验收报告.md`。
- 证据：`_系统/协作/lanes/acceptance/workspace/boss-customer-workspace/**`。
- 最终状态：`BLOCK`。

### 2026-09-22 派单纠正（客户页任务作废）

- `REQ-20260922-BOSS-CUSTOMER-WORKSPACE` 因用户澄清而暂停并撤回。
- 日期维度和报价谈单式工作区应落在「预约量房工作台」，客户经营页改造另立后续讨论。
- 上一轮 `DEF-BOSS-CUSTOMER-001/002` 探针记录仅保留为误派单证据，不作为开发缺陷或验收结论。
- 当前状态：`VOID_SUPERSEDED_AWAITING_NEW_MEASURE_HANDOFF`。

## 2026-09-22 客户经营预约量房指标卡独立验收

- 请求：`REQ-20260922-CUSTOMER-MEASURE-METRIC`。
- 契约变更：BOSS「客户经营」第四张卡 `riskCustomer` → `measureCustomer`，标签「预约量房客户」。
- 口径：本月按 `Measure.scheduledAt`（缺失时兼容 `measureTime`）去重客户，排除 `Cancelled`；periods 为日增/昨日/近7日增/月增；副文案“本月完成 X 户 · 待派单 Y 户”。
- `CustomerOverviewContextTest` 已更新并通过：覆盖字段替换、历史兼容、取消排除、客户去重、四类窗口、本月完成、待派单，以及风险标签/负责人分组风险保留。
- 隔离回归：排除已开放的 measure 2.0 `BossMeasureWorkspace2AcceptanceTest` 和无关的 `ProviderSecurityAcceptanceTest` 路由基线漂移后，78 suites / 240 tests / 0 failures / 0 errors / 0 skipped。
- BLOCKED_EXTERNAL：真实账号/数据下的 App 指标卡标题、主值、副文案和窗口排版未实机验证。
- 报告：`docs/acceptance/客户经营预约量房指标卡独立验收报告.md`。
- 证据：`_系统/协作/lanes/acceptance/workspace/customer-measure-metric/**`。
- 最终状态：`CONDITIONAL`。

## 2026-09-22 预约量房工作台 2.0 P0 独立验收

- 请求：`REQ-20260922-BOSS-MEASURE-WORKSPACE-2`。
- 依据：产品稿 2.0、开发交接、`SCN-001`、`BR-001`；不重做 Measure 状态机，不新增接单/拒单/上门中。
- 独立自动化：`BossMeasureWorkspace2AcceptanceTest` = 4 tests / 2 pass / 2 fail；PASS 为七个派生状态和业务员本人数据范围；FAIL 为今日预约自然日口径、签约客户全企业事件保留。
- 全量：`mvn -o -q -DforkCount=0 test` = 80 suites / 247 tests / 2 failures / 0 errors / 0 skipped；2 项失败均为本条验收探针确认的业务缺陷。排除该探针后回归为 79 suites / 243 tests / 0 failures / 0 errors / 0 skipped。
- UI 返工静态复核 PASS：`exitWorkspaceFocus` + `suppressAutoFocus` 可恢复经营概览；日期维度和异常待办均为 2×2 grid；看板六项指标保留；客户卡片头像/身份、房屋卡、时间 chip、人员标签、下一步、右对齐胶囊操作、浅品牌/浅灰样式、细分割线、内缩渐变风险条和当前高亮均已接入。
- 缺陷：
  - `DEF-MEASURE-WS-001` P1：全企业 periods 仍先按 `ACTIVE_PRE_MEASURE` 截断客户，Signed 客户量房事件丢失。
  - `DEF-MEASURE-WS-003` P1：App 逾期筛选仍为 84h，后端与业务口径为 4h。
  - `DEF-MEASURE-WS-004` P2：绩效页月份/门店未传入 `bossMeasure()`，量房业绩与筛选范围不一致。
  - `DEF-MEASURE-WS-005` P1：今日预约漏计今天已完成和尚未到时的预约。
  - `DEF-MEASURE-WS-006` P1：业务员 action 声明 `view:measure-workbench`，但权限矩阵未授予，无法从首页进入“我的量房”。
- BLOCKED：真实微信开发者工具/真机点击、长名称、实际横向溢出和底部遮挡未执行。
- 排除项：准时率、派单时长、接单率、改期原因不在本轮，不因缺事实判缺陷。
- 报告：`docs/acceptance/预约量房工作台2.0独立验收报告.md`。
- 证据：`_系统/协作/lanes/acceptance/workspace/boss-measure-workspace/workspace2-{acceptance.log,results.json,static-audit.json,static-audit.mjs,app-build.log}`；`src/test/java/com/th4/edge/decoboon/statistic/BossMeasureWorkspace2AcceptanceTest.java`。
- 最终状态：`BLOCK`。

## 2026-09-22 预约量房工作台独立验收（结构收敛版）

- 请求：`REQ-20260922-BOSS-MEASURE-WORKSPACE`。
- 通过：首页/页面标题「预约量房」、`ruler` 图标；预约量房页仅按业务员分组；来源/执行设计师分组已移除；业务员组内仅保留客户数/待预约/已预约/已完成/风险；组内 KPI 与同一 `currentGroup.items` 一致；近 7 日跨月边界 `[start,end)` 探针通过；App release build PASS。
- 失败：
  - `DEF-MEASURE-WS-001` P1：全企业 periods 先截断为 `ACTIVE_PRE_MEASURE` 客户池，客户变为 Signed 后当天预约/完成事件从日期增量消失。独立探针：预期 today scheduled=1/completed=1，实际 0/0。
  - `DEF-MEASURE-WS-003` P1：App「逾期」筛选仍使用 84 小时；后端与业务口径为 4 小时，4–84 小时风险会漏算。
  - `DEF-MEASURE-WS-004` P2：人员绩效考核页切换月份/门店时，`statisticApi.performance` 收到筛选，但 `workbenchApi.bossMeasure()` 未收到月份/门店，业务员量房业绩与页面筛选不一致。
- 已移除：`DEF-MEASURE-WS-002` 原“执行设计师分组缺少未指派设计”不再适用，最新结构已删除设计师生分组。
- BLOCKED：真机左右分栏、长名称、小屏、内滚动高亮、放大模式、Tab/FAB 遮挡。
- 自动化：`BossMeasureReportContextTest` 定向复跑 PASS；后端最近稳定全量 `mvn -o -q -DforkCount=0 test` = 79 suites / 240 tests / 0 failures / 0 errors / 0 skipped；本结构独立探针 3 项 = 2 PASS / 1 FAIL。并行 lane 清理共享 `target` 曾导致编译噪声，不据此判业务缺陷。
- App release build：PASS；主包 616698 B，最大分包 302211 B，总包 2056741 B，14 分包 / 87 页。
- 报告：`docs/acceptance/预约量房工作台独立验收报告.md`。
- 证据：`_系统/协作/lanes/acceptance/workspace/boss-measure-workspace/latest-probe-summary.json`、`latest-probe.log`。
- 最终状态：`BLOCK`；误派单的客户经营页报告保持 `VOID / SUPERSEDED`，不计入本轮结论。

## 2026-09-26 AI用量与经营参谋联合独立验收

- 请求：`REQ-20260926-AI-USAGE-BRIEF-ACCEPTANCE`。
- 后端基线：`5252afa`；Web 基线：`68e6bff`。
- A0.1 PASS：企业总量=门店分摊+企业公共；业务门店优先；企业公共不归默认总店；失败/取消不计有效点；顺序同幂等键只一条。
- A1 后端 PASS：前序 4 项缺陷全部关闭；`AiUsageBriefJointAcceptanceTest` 9/9；稳定幂等键、部分数据源降级、财务缺失成本状态、行动去重和对象级 sources 均通过。
- Web A1 PASS（代码/构建）：移除挂载自动生成；补预计消耗说明；来源抽屉展示 Provider/对象/版本/更新时间。
- Provider Security PASS：348 routes / 12 public / 336 protected。
- 后端全量：115 suites / 385 tests / 0 failures / 0 errors / 0 skipped。
- 迁移 64：SHA-256 `c99dba4b19632b756eaa3d7e81267ed0626d204fc5678761565eaa7f2c267b75`；隔离 MySQL 8.0.24 连续执行两次 PASS；生产账本为证据文档登记 SUCCESS，沙箱不可直连复核。
- BLOCKED：真实 BOSS 会话的浏览器加载/筛选翻页；同幂等键并发冲突回查；生产库直连；门店级简报 finance/alert 口径。
- 报告：`docs/acceptance/AI用量与经营参谋联合独立验收报告.md`。
- 证据：`_系统/协作/lanes/acceptance/workspace/ai-usage-and-brief/**`。
- 最终状态：`CONDITIONAL`。

## 2026-09-24 麦鸿设计 · 业主端沉浸式体验审计（微信开发者工具）

- 运行号：`MH-OWNER-AUDIT-20260924`；产出目录 `docs/acceptance/20260924-麦鸿设计-业主端沉浸式体验审计/`（12 份成果 + `raw/` 证据）。
- 环境：本地 dev 后端（03:01 重新打包，含当日全部改动）+ 本地 MySQL/Redis + **微信开发者工具**（AppID `wxe5cc5fec9e50a9a9`，`dist/dev/mp-weixin`，`miniprogram-automator` 经 `cli auto` 连接）。
- 关键突破（修正旧结论）：**开发者工具的 `wx.login` code 可真实换取 openid**（已用 `jscode2session` 验证），业主会话可在本地建立；该 openid 已有 `Credentials` 且已绑定业主「赵霞 / 麦鸿演示项目 41」，**未新增任何业务事实**。
- 覆盖：业主端 12 个页面全部打开并采集（`element.text()` 聚合自定义组件）+ 截图 18 张；业主视角 12 个场景按装修经历逐步走查。
- 结论：业主端可登录/可浏览/信息基本完整（PASS）；**发现 P1×1、P2×3、P3×1**：
  - `XA-20260924-001`（P1）**业主驳回验收后项目状态仍为"待验收"**，与 DB 事实（验收 Rejected / 阶段 Doing / 项目 Accepting / progress 0）矛盾，且「验收整改」显示待确认 0 → 违反 DEC-009 决策二；
  - `XA-20260924-002`（P2）项目旅程"量房"摘要直出 ISO `2026-09-10T05:03:07`；
  - `XA-20260924-003`（P2）项目详情"当前： · 阶段 0/8"当前阶段为空（其他 8 页正常）；
  - `XA-20260924-004`（P2）「待我处理 2」不可展开、与各模块待确认数对不上；
  - `XA-20260924-005`（P3）演示数据测试串 `111`/`1111` 出现在业主端。
- 方法学教训（已写入 08 号文档）：首轮用 `page.$$('text')` 采集文本**不跨自定义组件**，导致"报价金额空白""验收无明细"两项**误报**；改用容器 `element.text()` 复核后撤回。后续小程序自动化取文本必须用容器 text。
- 未覆盖：真机微信授权/相机/相册（外部设备）、跨租户负向（本库仅 1 租户 1 项目）、弱网、支付回调。
- 价值判断：业主端已能"看得见/算得清/留得下证据/提得出意见"；主要风险是**状态可信度**（透明一旦自相矛盾即反噬信任），其次业主只能被动确认、无发起通道。
- 下一步：P1 状态机由 development 按 XA-001 修复后回归；P2 三项合并为一批；整改责任人与复验入口、业主发起通道进入产品规划。
