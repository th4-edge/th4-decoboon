# 员工薪酬与薪资经营 · 设计—蓝图—实现—页面—验收对账

状态：`PASS` / `PENDING` / `UNMAPPED` / `STALE` / `GAP` / `BLOCKED`

## B1 对账矩阵

| REQ | 业务规则/蓝图 | 后端/API | Web | App | 自动化测试 | UI/证据 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-B1-01 方案列表与筛选 | BR-038、DEC-065；enter/filter、UI-B1-01/02 | `payroll/compensation/page`；DataScope + EntityGraph + filters | `/finance/compensation-schemes` 列表、筛选、分页 | `compensation-list` 同 Provider | 全量 900/0/0；Web build/check:permissions/check:store-scope/check:workbenches PASS | 01–03 三档截图；筛选王宏文=1、无结果=0 | PASS |
| REQ-B1-02 新增方案 | BR-038、DEC-045；create、UI-B1-03 | `payroll/compensation/save`；`scheduledDays` 必填、金额/日期/唯一校验 | 文档式新增抽屉，字段完整 | `compensation-edit` 已有同能力 | 定向 11/0/0；全量 900/0/0 | 04 抽屉、05 校验截图；真实创建徐勇 `¥8,888/22天` | PASS |
| REQ-B1-03 编辑未锁方案 | BR-038；edit、UI-B1-03/04 | `payroll/compensation/info/update`；DataScope + 事实锁 + 字段校验 | 编辑抽屉；员工只读；锁定提示按服务端 `locked` 显示 | `compensation-edit` | 定向事实锁/审计测试 PASS；全量 PASS | 真实编辑徐勇 `¥8,888→¥9,999`；UI-B1-002 已修复 | PASS |
| REQ-B1-04 新增后续方案 | BR-038、DEC-045；lock/successor、UI-B1-04 | `save` 允许新增更晚版本；事实锁不回写旧方案 | Web 行操作“新增后续方案”，预填原值 | App `successorId` 路径同 Provider | 锁定状态 3 聚合测试 PASS；App type-check/contract/build PASS | 06 后续方案截图；真实锁定行入口可见 | PASS |
| REQ-B1-05 终止/删除 | BR-038；terminate-delete、UI-B1-05 | `update` 写 effectiveTo；`delete` 事务内事实锁 + 审计 | 终止 prompt、删除二次确认、锁定隐藏 | App locked 隐藏删除 | 删除锁/审计定向测试 PASS | 真实终止 `2026-10-21→2026-10-21`；真实删除后列表 6→5 | PASS |
| REQ-B1-06 数据范围与权限 | BR-038、DEC-065；CHK-B1-01/02/07 | `payroll/employee/page` 与 compensation 全部 Provider 同 DataScope | 财务可写、BOSS 全企业可写、店长本店只读 | App 能力键 `view/edit:payroll` | `PayrollCompensationScopeTest`：同店通过、跨店拒绝、总部跨店通过；全部 PASS | 08 店长只读、09 BOSS 可写；Finance 写入口 | PASS |
| REQ-B1-07 字段完整与跨端一致 | BR-038；CHK-B1-06/08 | page/info 返回 scheduledDays、createdBy、createTime、updateTime、locked | 财务员工映射修复岗位/门店/团队；角色中文化 | App 模型补 locked/lockedReason/createdBy/updatedAt | 白名单断言 PASS；Web/App type-check/build PASS | 列表最终截图显示中文岗位、门店、团队 | PASS |
| REQ-B1-08 输入校验 | BR-015、BR-038；CHK-B1-03 | salary > 0；scheduledDays 1～31；effectiveTo ≥ effectiveFrom | 前端即时错误并保留输入 | 原表单同步约束 | `PayrollCompensationContextTest` + Guard 测试 PASS | 05 客户端校验截图；服务端错误由 Provider 再次兜底 | PASS |
| REQ-B1-09 唯一版本 | BR-038；CHK-B1-04 | 同员工同 effectiveFrom 409；UI 与 DB 唯一约束共同兜底 | 错误保留输入并提示改日期/编辑原方案 | 同 Provider 错误提示 | 重复生效日期测试 PASS | 04/05 抽屉恢复路径；真实创建唯一日期成功 | PASS |
| REQ-B1-10 审计与恢复 | BR-038；RULE-B1-12、recover | 结构化 CREATE/UPDATE/DELETE 审计；createdBy/createTime/updateTime | 列表显示创建人与更新时间；失败保留输入 | 同 Provider | 审计快照测试 PASS；删除前快照记录 | 编辑后列表更新时间变化；真实 CRUD 成功 | PASS |
| REQ-B1-11 App 回归 | BR-038；UI-B1-06 | 共用 `payroll/compensation/*` | Web 同能力 | 列表锁定状态、新增/编辑/删除、后续方案 | `type-check`、`verify-payroll-closure-contract`、`build:mp-weixin` PASS | Web 与 App 字段/状态一致；App 真机与微信运行时独立验收待做 | PASS |

## 后续批次（未开始）

| REQ | 内容 | 状态 |
| --- | --- | --- |
| REQ-002 | 财务修正月度考勤 | PENDING |
| REQ-003 | 工资单复核、确认与发放 | PENDING |
| REQ-004 | 工资与提成解耦落地 | PENDING |

## 未映射

- 无。REQ-B1-01～11 均有业务规则、蓝图节点、Provider/API、页面、测试和 UI 证据。

## 已过期

- 无。

## 未决业务问题

- `GAP-060` 店长工资单确认/作废边界：B1 不涉及，非本批阻塞。
- 店长员工发放权限最终边界：B3 前关闭。
- 双门店真实 UI 负向：自动化已覆盖，独立验收阶段补真实双门店账号/对象。
- App 微信运行时/真机：独立验收阶段补。

## 本批结论

- 设计—蓝图—代码—Web—App—测试—UI：`PASS`
- 缺陷：UI-B1-001、UI-B1-002 均已修复并复验。
- 无 DDL/迁移；无未映射或过期项。
- 独立验收：`PASS`；ACC-B1-001/002/003 已关闭，ACC-B1-U01 已关闭。
- 当前状态：`ACCEPTED`
- 下一阶段：`RELEASED` 发布门禁；必须由用户确认 L3 生产发布，未确认前不得部署。


## 2026-10-23 跨端一致性与权限修复对账

| 需求 | 蓝图/规则 | Provider/API | Web | App | 测试/证据 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| H1-01 工资与提成解耦 | BR-039、DEC-047、EV-149、GAP-072 | `payroll/sheet/update` | 已按非阻断提示实现 | 已按非阻断提示实现 | 生产制品无阻断；源码已恢复；定向 1/1、H1 隔离 JDK22 全量 931/0/0/0、API 黑盒 PASS | MAPPED_PENDING_INDEPENDENT |
| H1-02 Web 薪酬能力键 | BR-038、DEC-046、DEC-065、GAP-073 | 薪酬 Provider 不变 | 已改用薪酬能力键；员工发放保持待裁决 | 能力键基本正确 | Web type-check/build/权限/门店/工作台门禁 PASS；三角色真实页面 PASS | MAPPED_PENDING_INDEPENDENT |
| H1-03 考勤详情门店范围 | BR-038、DEC-065、GAP-073 | `attendance/summary/info` | 未接详情 | 已接详情 | 已改为 DataScope Specification；单测 2/2；真实跨店/跨企业 HTTP PASS | MAPPED_PENDING_INDEPENDENT |
| B2-01 应出勤方案联动与月度调整 | BR-015、BR-038、DEC-045、USER-DEC-20261029-006、GAP-073 | `payroll/compensation/info`、`attendance/summary/save|update` | 方案值作为默认值，允许调整并保存考勤快照 | 已对齐可调整并提交快照 | API + Web PASS；App 契约/类型检查/小程序构建 PASS；1129 全量后端测试 PASS；真实页面待验收 | MAPPED_PENDING_UI_EVIDENCE |
| B2-02 考勤编辑/删除/字段 | BR-038、GAP-073 | `attendance/summary/info|delete|update` | 已支持编辑、删除和完整字段 | 已实现 | API + 真实 Web PASS；用户 2026-10-23 承接 | MAPPED_USER_ACCEPTED_NON_INDEPENDENT |
| B2-03 店长本店考勤权限 | BR-038、DEC-065 | `attendance/summary/save|delete|update` | 店长本店入口可用 | 同源 | 本店成功、跨店 404、已生成工资项 409；用户承接 | MAPPED_USER_ACCEPTED_NON_INDEPENDENT |
| B3A-01 工资单详情与金额构成 | BR-038、DEC-046 | `payroll/sheet/info`、`payroll/item/page` | 已接详情并展示全部金额构成 | 完整展示 | API 20/20；真实三角色 Web；JDK22 全量 932/0/0/0；用户 2026-10-23 承接 | MAPPED_USER_ACCEPTED_NON_INDEPENDENT |
| B3A-02 草稿删除与确认交互 | BR-038、DEC-046 | `payroll/sheet/delete|update` | BOSS 二次确认、草稿删除、确认后回读 | 已实现 | API 20/20；确认幂等 `5/5`；真实 Web PASS；用户承接 | MAPPED_USER_ACCEPTED_NON_INDEPENDENT |
| B3A-03 详情组织范围 | BR-038、DEC-065 | `payroll/sheet/info` | 详情与列表使用同一范围 | 同源 | StoreManager 他店 404；总部 Finance 企业范围成功；单测 2/2；用户承接 | MAPPED_USER_ACCEPTED_NON_INDEPENDENT |
| B3B1 可选复核与发放权限 | DEC-093、BR-038、T12-BIZ-20261023-002 | `payroll/sheet/update|delete`、`finance/payment/employee/*` | 复核/退回/原因回读/发放入口 | 复核/退回/发放入口 | 定向 9/9；API 14/14；JDK22 全量 938/0/0/0；Web/App H5 PASS | MAPPED_PENDING_USER |
| B3B2 作废与反向事实 | DEC-093、DEC-043 | `payroll/sheet/*`、应付/成本/工资条反向 | 未开始 | 未开始 | 需 DDL/版本设计 | BLOCKED |
| B4 批量/部分发放与剩余体验 | BR-038、DEC-017 | 待设计 | 未开始 | 未开始 | 批量/部分发放未裁决 | BLOCKED |
| PAY-EMP-PAY-01 员工发放上下文与幂等 | BR-015、DEC-093、REQ-BIZ-20261006-004 | `finance/payment/employee/page|save` | 按当前工资单和门店加载，不自动选中 | 日期、备注和幂等键对齐 | 定向 2/2；Java22 全量 1130/0/0/0；App/Web 构建 PASS | MAPPED_PENDING_UI_EVIDENCE |
| PAY-PERM-STORE-01 薪酬入口权限与门店范围 | BR-038、DEC-093、GAP-073 | `attendance/summary/page`、`payroll/employee/page` | 考勤和提成员工候选继承当前门店 | 提成计提入口按 `edit:payroll` 门控 | 范围定向 6/6；App/Web 构建 PASS；真实角色待验收 | MAPPED_PENDING_UI_EVIDENCE |

本表状态在批次完成并取得证据前全部保持 `UNMAPPED` 或 `BLOCKED`，不得宣称整域已对账通过。


## B3A 对账结论

- B3A-01～03 均有业务规则、Provider/API、Web、测试、真实 UI 与黑盒证据。
- 本批无 `UNMAPPED` 或 `STALE`。
- H1/B2/B3A 状态均为 `MAPPED_USER_ACCEPTED_NON_INDEPENDENT`；独立评审仍需另行执行。
- 业务裁决已完成：`DEC-093` / `T12-BIZ-20261023-002`。
- B3B1 已完成并黑盒 PASS，等待用户验收；B3B2 因 DDL/版本设计阻塞；B4 批量/部分发放仍待业务裁决。


## 2026-10-30 员工本人月度考勤对账

| 需求 | 蓝图/规则 | Provider/API | Web | App | 测试/证据 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| PAY-SELF-ATT-01 员工本人月度考勤只读 | BR-038、DEC-017、REQ-PAY-SELF-ATT-01；`B5-payroll-self-attendance-blueprint.json` | `attendance/summary/page|info`；`selfOnly` 固定本人，服务端强制 Self | `/account/attendance`，账户菜单入口；月份切换、详情回读、错误恢复 | `my-attendance`，个人中心入口；月份切换、列表/详情、错误恢复 | 后端定向 8/8；Java22 全量 1136/0/0/0；App/Web 构建与门禁 PASS；真实页面待补 | MAPPED_PENDING_UI_EVIDENCE |

- 本批无 `UNMAPPED`/`STALE` 行；因缺少真实员工/管理角色跨端回读，不进入 `ACCEPTED`。
- 前序 `PAY-ATTENDANCE-APP-ALIGN`、`PAY-EMPLOYEE-PAYMENT-ALIGN`、`PAY-PERMISSION-STORE-SCOPE` 仍为 `PENDING_REAL_UI_ACCEPTANCE`，不因本批开发完成而自动关闭。


## 2026-10-30 员工本人提成对账

| 需求 | 蓝图/规则 | Provider/API | Web | App | 测试/证据 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| PAY-SELF-COMMISSION-01 员工本人提成只读 | BR-015、BR-038、DEC-016、DEC-017；`B6-payroll-self-commission-blueprint.json` | `payroll/commission/page|info`；`selfOnly` 固定本人，服务端强制 Self | `/account/commissions`，账户菜单入口；分页、详情回读、错误恢复 | `my-commission`，个人中心入口；详情、错误恢复 | 后端定向 8/8；Java22 全量 1144/0/0/0；真实微信小程序 8/8；真实 Web/API PASS；1440/1280/1024 无横向溢出；用户 2026-10-30 确认 | MAPPED_USER_ACCEPTED_NON_INDEPENDENT |

- 本批无 `UNMAPPED`/`STALE` 行；真实员工/管理角色跨端回读已 PASS，用户已确认，记 `MAPPED_USER_ACCEPTED_NON_INDEPENDENT`，不代表独立验收或生产发布。
- 员工本人工资条、考勤和提成现均有开发验证，但前序真实页面验收仍未完成，不能宣称薪酬域完整闭环。


## 2026-10-30 B7 店长敏感字段业务门禁

| 需求 | 业务规则/缺口 | Provider/API 事实 | App/Web 事实 | 当前状态 | 下一步 |
| --- | --- | --- | --- | --- | --- |
| REQ-PAY-MGR-SENS-01 店长工资构成敏感字段边界 | `DEC-096`、`ANS-BIZ-20261030-001`、`B7-manager-sensitive-fields-blueprint.json`、`GAP-074` 已关闭 | `store-payroll/info` 与 `payroll/item/page` 返回全部现有详情字段；`store-payroll/page` 保持摘要字段 | 两端详情展示社保/个税等字段 | 定向 7/7 PASS；生产代码无需变更；真实店长 UI 待验收 | `MAPPED_PENDING_UI_EVIDENCE` |


## 2026-10-30 店长本店薪酬动作对账

| 需求 | 业务规则 | Provider/API | Web | App | 测试/证据 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| REQ-PAY-MGR-ACT-01 店长本店确认/退回/删除/发放 | DEC-097、ANS-BIZ-20261030-002；`B8-manager-actions-blueprint.json` | `payroll/sheet/update|delete`、`finance/payment/employee/page|save` | `payrollAccess` 店长确认/退回/删除/发放入口已接线 | `confirm:payroll/pay:payroll` 与 Draft 删除能力已接线 | 定向 16/16；App/Web 构建门禁 PASS；真实店长页面待补 | MAPPED_PENDING_UI_EVIDENCE |


## 2026-10-31 PAY-OPS-01 薪酬工作台聚合对账

| 需求 | 业务规则/蓝图 | Provider/API | App/Web | 测试/证据 | 状态 |
| --- | --- | --- | --- | --- | --- |
| REQ-PAY-OPS-01-01 工作台状态与最近工资单 | `ANS-BIZ-20261031-001`、`B9-payroll-workbench-aggregate-blueprint.json` | `payroll/workbench/report` 返回相同字段；6 组有界查询 | Web/App 真实渲染 PASS；视觉质量待独立评审 | 定向/全量 PASS；旧新对账 PASS | MAPPED_RENDER_PASS_VISUAL_REVIEW_UNAVAILABLE |
| REQ-PAY-OPS-01-02 待付工资与提成 | `BR-015`、`ANS-BIZ-20261031-001` | 工资取目标期间、提成跨期间；逐条负余额钳制 | 金额展示口径不变 | `pendingEmployeePayableAmount` 60.00→80.00；提成负余额不冲减 | MAPPED |
| REQ-PAY-OPS-01-03 查询结构有界 | `BR-025`、`BR-031`、`DEC-036/037` | 6 组聚合/投影，无全量实体 | 响应体 493B 不变 | 22→2 entity loads；10× SQL/实体加载不增长 | MAPPED |
| REQ-PAY-OPS-01-04 错误与权限 | `BR-025`、数据范围规则 | 门店解析在聚合前；本店成功、跨店 403；错误不伪 0 | Web/App 正常态 PASS；异常态视觉待独立评审 | 定向/全量 PASS | MAPPED_RENDER_PASS_VISUAL_REVIEW_UNAVAILABLE |

- 本批无 `UNMAPPED`/`STALE` 行；
- 无 DDL、无生产发布；
- Web/App 真实渲染与本地 MySQL 10× 并发压测已 PASS，但图片视觉评审、生产候选环境复测和独立业务验收未完成，不进入 `ACCEPTED`。


## 2026-10-31 PAY-OPS-01 Web/App 回读补充

| 对账项 | Web | App | API/DB | 状态 |
| --- | --- | --- | --- | --- |
| 待发员工薪酬 | ¥1,600.00 | ¥1,600 | 1,600.00 | PASS |
| 待确认考勤 | 2 | 2 | 2 | PASS |
| 工资单草稿 | 1 | 1 | 1 | PASS |
| 门店范围 | BOSS 全部门店 | BOSS 全部门店 | 公司范围 | PASS |
| Console/DOM 错误 | 0 | 页面 5/5 PASS | HTTP 200 / 491B | PASS |
| 横向溢出 | 1440/1280/1024 无 | 原生页面已渲染 | N/A | PASS |
| 视觉质量 | 截图已留存 | 截图已留存 | N/A | VISUAL_REVIEW_UNAVAILABLE |
| 测试数据清理 | — | — | 0 残留 | PASS |

本补充不替代独立验收，也不把真实渲染功能 PASS 表述为视觉验收 PASS。


## 2026-10-31 PAY-OPS-01 10× 性能补充

| 指标 | 基线 | 10× | 门禁 | 状态 |
| --- | ---: | ---: | --- | --- |
| 员工 | 8 | 80 | 数据量10× | PASS |
| 工资项/工资条/提成 | 96 | 960 | 查询结构不增长 | PASS |
| 应付 | 104 | 1,040 | 查询结构不增长 | PASS |
| MySQL Questions/请求 | 22 | 22 | 不随明细增长 | PASS |
| 顺序 P95/P99 | — | 21.87/22.43ms | ≤500/1000ms | PASS |
| 20并发 P95/P99 | — | 28.19/29.75ms | ≤500/1000ms | PASS |
| 错误率 | — | 0 | 0 | PASS |
| 响应体 | — | 503B | 页面字段白名单 | PASS |
| 测试数据清理 | — | 0 残留 | 无污染 | PASS |

本结果为本机 MySQL 生产等价数据与并发压测，不替代生产候选环境复测和独立验收。
