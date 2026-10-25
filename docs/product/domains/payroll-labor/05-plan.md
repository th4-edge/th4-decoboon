# 员工薪酬与薪资经营 · 垂直切片计划

## 北向目标

财务按方案 A 完成员工薪酬日常操作，BOSS 保留最终确认与高风险动作，薪酬方案、考勤、提成、工资单、应付、发放和工资条形成可追溯闭环。

## B1 垂直切片

**财务在 Web 完成员工薪酬方案维护闭环，并修复其服务端门店数据范围和事实锁定。**

- 目标角色：Finance 主用；BOSS 全企业；StoreManager 本店只读。
- 涉及对象：CompensationScheme、Employee、AttendanceSummary、CommissionItem、PayrollItem、PayrollAccessContext。
- 单批边界：只做薪酬方案的列表、新增、编辑、锁定、新增后续方案、终止和删除；不触及考勤、工资单、提成、发放。
- 变更级别：L3（权限、金额、删除），必须全量后端测试、真实页面证据、回滚检查；无 DDL、无数据迁移。

## 批次

| 批次 | 垂直切片 | 涉及角色 | 涉及对象 | 后端 | App/Web | 业务校核 | 退出条件 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| B1 | 财务薪酬方案维护闭环 | Finance、BOSS、StoreManager | CompensationScheme、Employee、考勤/提成/工资事实锁 | 修正门店数据范围、完善字段投影、事实锁、输入校验、审计日志 | Web 新页 + App 锁定状态回归 | BR-038、DEC-045、DEC-065 | REQ-B1-01～11 全部实现并验证 | READY |
| B2 | 财务月度考勤修正闭环 | Finance、BOSS、StoreManager | AttendanceSummary | 后续冻结 | 后续冻结 | BR-038 | 另行蓝图与计划 | QUEUED |
| B3 | 工资单复核、确认与发放闭环 | Finance、BOSS | PayrollSheet、PayrollItem、Payable、Payment、Payslip | 后续冻结 | 后续冻结 | BR-038、BR-039、DEC-046/047 | 另行蓝图与计划 | QUEUED |

## B1 改动文件冻结

### 后端

| 文件/模块 | 计划改动 |
| --- | --- |
| `src/main/java/com/th4/edge/decoboon/payroll/context/PayrollAccessContext.java` | 统一 BOSS/财务/店长数据范围；门店财务解析员工、详情、更新、删除均限本店；保持写权限 BOSS/Finance，StoreManager 只读 |
| `src/main/java/com/th4/edge/decoboon/payroll/service/employees/page/support/Page.java` | 员工选择器按门店范围裁剪，接受权威 `storeId` 过滤，禁止门店财务看到全企业员工 |
| `src/main/java/com/th4/edge/decoboon/payroll/service/compensations/page/support/Page.java` | 增加 keyword/status/effectiveMonth/storeId 过滤；返回 `scheduledDays`、createdBy、createTime、updateTime、locked；禁止逐行查询 |
| `src/main/java/com/th4/edge/decoboon/payroll/service/compensations/info/support/Info.java` | 统一数据范围；返回 locked、createdBy、updateTime；跨店 404/403 |
| `src/main/java/com/th4/edge/decoboon/payroll/service/compensations/save/support/Save.java` | `baseSalary > 0`、日期区间、重复版本校验；审计日志；返回完整白名单字段 |
| `src/main/java/com/th4/edge/decoboon/payroll/service/compensations/update/support/Update.java` | 走数据范围；员工不可更换；事实形成后锁定关键字段；日期、重复和金额校验；不开放任意状态修改；审计日志 |
| `src/main/java/com/th4/edge/decoboon/payroll/service/compensations/delete/support/Delete.java` | 事务内复核事实锁；删除前写审计快照；保持物理删除仅限无事实方案 |
| `src/main/java/com/th4/edge/decoboon/payroll/model/CompensationScheme.java` | 增加 `@Transient` 派生字段 locked/lockedReason（不进迁移表），由 Provider 组装 |
| `src/main/java/com/th4/edge/decoboon/payroll/repository/CompensationSchemeRepository.java` | 增加必要的重复版本/事实存在查询或 EntityGraph，避免分页 N+1 |
| `src/main/java/com/th4/edge/decoboon/payroll/context/PayrollCompensationAuditContext.java` | 新增结构化审计组件：操作人、企业、门店、schemeId、动作、前后快照、结果；不新增数据库表 |
| `src/test/java/com/th4/edge/decoboon/payroll/PayrollCompensationGuardTest.java` | 扩展金额、日期、重复、事实锁和删除校验 |
| `src/test/java/com/th4/edge/decoboon/payroll/PayrollCompensationScopeTest.java` | 新增总部财务/门店财务/店长/跨店/跨企业权限矩阵测试 |
| `src/test/java/com/th4/edge/decoboon/payroll/PayrollCompensationProjectionTest.java` | 新增 page/info 字段白名单、locked 和字段完整性测试 |
| `src/test/java/com/th4/edge/decoboon/payroll/PayrollCompensationAuditTest.java` | 新增创建/更新/删除审计事件测试 |

后端约束：

- 继续使用 `Provider + @Properties + @User`；不新增 Controller、DTO、VO、Request/Response 包装。
- `payroll/compensation/page|info|save|update|delete` 路由保持不变。
- `PayrollAccessContext` 是唯一数据范围入口，不允许各 Provider 再复制角色判断。
- 无 DDL、无迁移、无生产数据修复。

### Web

| 文件/模块 | 计划改动 |
| --- | --- |
| `src/views/finance/FinanceCompensationSchemesView.vue` | 新增专业列表页：筛选、分页、行操作、锁定提示、新增后续方案 |
| `src/components/finance/FinanceCompensationDrawer.vue` | 改为新增/编辑共用；补 `scheduledDays`；编辑时员工只读；侧栏提示工资/考勤影响 |
| `src/components/picker/EmployeePicker.vue` | 支持权威 `storeId` 过滤；门店财务仅展示本店候选 |
| `src/domains/finance/model.ts` | 补 `scheduledDays`、createdBy、createTime、updateTime、locked、lockedReason |
| `src/domains/finance/api.ts` | 规范化新字段；compensation page 支持 filters/storeId；增加 delete；employeeOptions 支持 storeId |
| `src/domains/finance/payrollAccess.ts` | 新增薪酬专属权限判定：写=BOSS/Finance，读=BOSS/Finance/StoreManager，禁止复用含 StoreManager 的通用 `canWrite` |
| `src/views/finance/FinanceEmployeeSalariesView.vue` | “薪酬方案”入口改为跳转新页，移除旧的仅新增抽屉入口 |
| `src/roles/routes.ts` | 新增 `/finance/compensation-schemes` 静态路由 |
| `src/roles/workbenchConfig.ts` | Finance 薪酬菜单新增「薪酬方案」 |
| `src/roles/workbenchHome.ts` | 如现有财务首屏需要入口，补最小卡片；不新增未注册菜单 key |

Web 交互冻结：

- 列表列：员工、岗位、门店/团队、月固定工资、月应出勤、生效期、状态、锁定、创建人、更新时间、操作。
- 未锁定行：编辑、终止、删除。
- 已锁定行：只显示「新增后续方案」和查看详情。
- 新增后续方案预填员工、工资、应出勤和默认后续生效日，但必须由财务确认新生效日期。
- 删除使用危险确认，明确“仅无考勤/提成/工资事实可删除”。
- 所有失败保留抽屉输入；409/403/404 展示服务端原因并提供刷新。

### App

| 文件/模块 | 计划改动 |
| --- | --- |
| `src/domains/payroll/model.ts` | 补 locked、lockedReason、createdBy/updateTime（如当前模型缺） |
| `src/domains/payroll/api.ts` | 规范化 locked、时间字段；继续复用同一 Provider |
| `src/subpackages/payroll/pages/compensation-list.vue` | 显示锁定状态；锁定时隐藏编辑/删除，提供新增后续方案 |
| `src/subpackages/payroll/pages/compensation-edit.vue` | 保持新增/编辑；后端锁定 409 时显示“新增后续方案”修复指引 |

App 不新增页面、不新增能力键，只做 B1 同能力回归和锁定状态补齐。

## 依赖与顺序

1. 先完成后端数据范围、字段投影、输入校验和事实锁，并跑定向测试。
2. 后端稳定后接 Web API/模型和新页面。
3. 再做 App 锁定状态回归，确保同一 Provider、同一错误文案。
4. 最后运行全量后端测试、Web/App 构建和真实浏览器走查。
5. 部署顺序：后端 → Web → App；后端必须先于 Web，否则新页字段和权限修复不可用。

## 测试矩阵

### 后端

- `mvn -o -Dtest=PayrollCompensationGuardTest,PayrollCompensationScopeTest,PayrollCompensationProjectionTest,PayrollCompensationAuditTest test`
- `mvn -o test`
- 正向：财务创建、编辑、终止、删除、新增后续方案。
- 反向：店长写 403；门店财务跨店选人/详情/更新/删除 404/403；重复生效日期 409；事实锁 409；金额/日期/出勤非法 400。
- 字段：page/info 返回 scheduledDays、locked、createdBy、createTime、updateTime。
- 审计：创建/更新/删除均记录操作人、范围、动作和前后快照。

### Web

- `npm run type-check`
- `npm run build`
- `npm run check:permissions`
- `npm run check:store-scope`
- `npm run check:workbenches`
- 真实浏览器：Finance 在 1440×900、1280×800、1024×768 完成列表→新增→编辑→新增后续方案→删除；检查无横向溢出、键盘操作、空态/错误态和锁定状态。
- 店长登录检查只读；BOSS 检查企业范围可写；门店财务检查本店范围。

### App

- `npm run type-check`
- `node scripts/verify-payroll-closure-contract.mjs`
- `npm run build:mp-weixin`
- 回归薪酬方案列表、新增、编辑、删除和锁定提示；不进入正式版提审。

## 验收证据

- 后端测试报告和失败/通过摘要。
- Web/App 构建日志摘要。
- 真实浏览器截图与 DOM/控制台零错误证据。
- 正向与反向角色矩阵证据。
- 工资日期匹配和锁定版本回归数据。
- 对账矩阵中 REQ-B1-01～11 全部变为 `PASS`，无 `UNMAPPED/STALE`。

## 回滚

- 本批无 DDL、无迁移、无批量数据修复。
- 后端部署失败：使用 `scripts/deploy-prod.sh` 自动回滚到 `.previous`。
- Web 回滚：回到上一版静态资源；若后端已回滚，Web 必须同批回滚，避免新页依赖旧接口。
- App 未进入正式版；开发版/体验版可回退上一构建，不执行正式发布。
- 薪酬方案数据在部署回滚时无需数据库回滚；但生产写操作冒烟应使用受控测试企业，禁止修改真实员工薪酬做验证。

## 风险与止损

- 最高风险：门店财务数据范围；任何跨店读写通过即阻断发布。
- 金额风险：0/负工资、缺应出勤、日期倒置；任一落库即阻断。
- 历史风险：事实锁定失效会污染工资历史；任一锁定绕过即阻断。
- UI 风险：前端仍使用通用财务 `canWrite`，导致店长看到写按钮；权限门禁不一致即阻断。
- 同一测试失败最多修复 2 轮；连续 2 次无进展则停止并报告，不扩大批次。

## 门禁

- 本批结束停在：`DEV_VERIFIED`，随后进入真实 Web UI 审查。
- 进入编码前必须：用户确认 B1 实施范围，且 `state.json` 切到 `DEV_IN_PROGRESS`。
- 进入 `RECONCILED` 前必须：REQ-B1-01～11 均有代码、测试、页面和验收证据。
- 进入生产发布前必须：L3 用户确认、全量测试通过、备份/回滚点确认，且不使用真实员工薪酬做写冒烟。

## 下一批

- B1 完成后由监督流程生成 Web UI 审查提示词；UI 通过后再进入独立验收。
- B2 月度考勤修正批次不得在 B1 未闭环时并行修改。


## 2026-10-23 跨端一致性与权限修复队列

来源：`PI-20261023-001`、`REQ-BIZ-20261023-001`、`ANS-BIZ-20261023-001`、`T12-BIZ-20261023-001`。

| 批次 | 垂直切片 | 前置 | 状态 | 任务书 |
| --- | --- | --- | --- | --- |
| H1 | 提成解耦回归、Web 薪酬按钮权限、考勤详情门店范围 | 用户确认 | BLACK_BOX_PASS / USER_ACCEPTED_NON_INDEPENDENT | `H1-payroll-p0-hotfix-task.md` |
| B2 | Web 月度考勤完整对齐 App；店长考勤后端权限 | H1 | USER_ACCEPTED_NON_INDEPENDENT | `B2-payroll-attendance-parity-task.md` |
| B3A | 工资单详情、草稿删除、确认弹窗、历史月份上下文 | H1、B2 | USER_ACCEPTED_NON_INDEPENDENT | `B3A-payroll-sheet-parity-task.md` |
| B3B1 | 财务可选复核、Pending 退回、店长发放权限闭环 | DEC-093 / T12-BIZ-20261023-002 | RECONCILED / PENDING_USER | `B3B1-payroll-review-payment-permission-task.md` |
| B3B2 | BOSS 作废、反向应付/成本、工资条作废、版本与幂等 | DEC-093 / DEC-043 | BLOCKED_DDL_AND_VERSION_DESIGN | `B3B2-payroll-void-reversal-task.md` |
| B4 | 提成释放、人工调整、批量/部分发放、两端剩余体验 | B3A + 批量/部分发放裁决 | BACKLOG_BLOCKED | 待建 |

门禁：

- H1/B2/B3A 均含权限或金额语义，属于 L3；
- B3B1 已完成黑盒验证，等待用户验收；B3B2 需 DDL 设计确认；B4 批量/部分发放仍待裁决；
- 每批完成后必须更新 `state.json`、`06-implementation.md` 和 `10-reconciliation.md`；
- 独立验收前不得标记 CLOSED。
