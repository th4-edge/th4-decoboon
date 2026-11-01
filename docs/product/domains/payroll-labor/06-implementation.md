# 员工薪酬与薪资经营 · B1 实现记录

## 批次

- Domain：`payroll-labor`
- Batch：B1 财务薪酬方案维护闭环
- 阶段：`DEV_VERIFIED`
- 变更级别：L3（权限、金额、删除）
- DDL/迁移：无

## 已实现

### 后端

1. `PayrollAccessContext.resolveEmployee` 改为统一使用 `DataScope`：
   - 总部财务企业范围；
   - 门店财务只能解析本店员工；
   - 店长本店范围保持只读；
   - 跨店返回 404，无有效范围返回 403。
2. `payroll/employee/page` 接入 `StoreFilterContext`，门店财务的员工选择器只返回本店员工。
3. `CompensationScheme` 新增 `@Transient locked/lockedReason`，不新增数据库列。
4. `payroll/compensation/page`：
   - 支持 keyword/status/effectiveMonth/storeId；
   - 返回 scheduledDays、createdBy、createTime、updateTime、locked、lockedReason；
   - 使用 EntityGraph 预取员工、门店、团队和创建人。
5. `payroll/compensation/info`：
   - 按主键读取时统一走 `DataScopeSpecification`；
   - 按员工月份读取时使用已校正的员工数据范围；
   - 返回锁定状态和完整时间字段。
6. `payroll/compensation/save`：
   - 月固定工资必须大于 0；
   - 月应出勤必须为 1～31；
   - 校验有效期和同员工同生效日期唯一；
   - 新增默认 Active；
   - 写结构化审计。
7. `payroll/compensation/update`：
   - 统一走数据范围；
   - 员工不可更换；
   - 已形成考勤/提成/工资事实时锁定，返回 409；
   - 不开放任意状态切换；
   - 校验金额、应出勤、有效期和重复生效日期；
   - 写修改前后审计。
8. `payroll/compensation/delete`：
   - 统一走数据范围；
   - 事务内复核事实锁；
   - 删除前记录完整方案快照。
9. 新增 `PayrollCompensationContext`：
   - 批量构造锁定状态，固定 3 条聚合查询，禁止逐行查库；
   - 统一金额、应出勤、有效期和唯一版本校验。
10. 新增 `PayrollCompensationAuditContext`：结构化记录操作人、企业、门店、方案、动作和前后快照，不新增审计表。

### Web

1. 新增 `/finance/compensation-schemes` 薪酬方案专业列表页。
2. 支持员工关键词、状态、生效月份筛选、分页和门店维度。
3. 表格展示员工/岗位、门店/团队、固定工资、应出勤、有效期、状态、锁定、创建人和更新时间。
4. 新增/编辑/新增后续方案共用文档式抽屉。
5. 修复 Web 新增薪酬方案未提交 `scheduledDays` 的缺陷。
6. 支持终止和删除未形成事实的方案；锁定方案只显示新增后续方案。
7. 新增薪酬专属权限 `usePayrollAccess`：BOSS/财务可写，店长只读。
8. Finance 菜单和 StoreManager 只读菜单接入薪酬方案页。
9. 员工选择器支持权威 storeId 过滤。
10. 修复财务员工对象映射，岗位、门店、团队和角色中文正确展示。
11. 修复编辑抽屉在未锁定方案上错误显示“员工已锁定”的提示。
12. 缺 `scheduledDays` 改由业务校验返回 400，不再由框架 `validationNotNull` 拦截。
13. 新增抽屉在表单输入变化后清除旧客户端校验提示。
14. App 薪酬列表金额固定两位小数，`5432.12` 正确显示为 `¥5,432.12`。

### App

1. 薪酬方案模型与 API 补 locked、lockedReason、createdBy、updateTime。
2. 列表显示“已锁定”状态；锁定方案隐藏编辑/删除，提供“新增后续方案”。
3. 新增后续方案复用原编辑页，通过 `successorId` 查询参数带入员工、工资、应出勤和建议生效日期。
4. 删除前增加 locked 防护。
5. 复用同一 `payroll/compensation/*` Provider，不新增能力键。

## API 与响应契约

| Provider | 变化 |
| --- | --- |
| `payroll/employee/page` | storeId 经 StoreFilterContext 解析；门店财务只返回本店候选 |
| `payroll/compensation/page` | 新增 keyword/status/effectiveMonth/storeId；返回 scheduledDays、locked、createdBy、createTime、updateTime |
| `payroll/compensation/info` | 返回锁定原因和完整时间字段；主键路径应用 DataScope |
| `payroll/compensation/save` | `scheduledDays` 必填；金额 > 0；校验有效期和重复版本 |
| `payroll/compensation/update` | 事实锁、日期/金额/唯一校验；不再接受任意状态切换 |
| `payroll/compensation/delete` | 数据范围 + 事实锁 + 删除审计 |

## 验证证据

### 后端

```text
mvn -o -Dtest=PayrollCompensationContextTest,PayrollCompensationGuardTest,PayrollCompensationScopeTest,PayrollDeleteGuardTest test
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0

mvn -o test
Tests run: 900, Failures: 0, Errors: 0, Skipped: 0
```

覆盖：

- 门店财务同店员工成功、跨店拒绝；
- 总部财务跨店员工成功；
- 门店财务员工选择器只返回本店；
- 金额/应出勤/日期范围校验；
- 批量锁定状态；
- 审计快照；
- 重复生效日期；
- 删除事实锁；
- page/info 字段白名单。

### Web

```text
npm run type-check  PASS
npm run build       PASS
npm run check:permissions PASS
npm run check:store-scope PASS
npm run check:workbenches PASS
```

产物包含 `FinanceCompensationSchemesView-*.js`。

### App

```text
npm run type-check PASS
node scripts/verify-payroll-closure-contract.mjs PASS
npm run build:mp-weixin PASS
```

## 未完成/后续门禁

- 真实浏览器 UI 审查：Finance/BOSS/StoreManager 三种角色、1440/1280/1024 三档视口。
- 生产只读冒烟和正式发布未执行。
- 独立验收未执行。
- B2 月度考勤、B3 工资单确认/发放未开始。

## 回滚

- 无 DDL/迁移。
- 后端回滚到 `.previous`。
- Web 回滚到上一版静态包。
- 如后端回滚，Web 同批回滚。
- App 仅构建，未上传或正式发布。


## H1 员工薪酬 P0 事故收口（2026-10-23）

### 状态

- 批次：`H1`
- 当前阶段：`DEV_VERIFIED`
- 独立验收：`PENDING`
- DDL/迁移：无
- 生产发布：未执行

### Changed files

- `src/main/java/com/th4/edge/decoboon/payroll/context/PayrollSettlementContext.java`
- `src/main/java/com/th4/edge/decoboon/payroll/context/PayrollBuildContext.java`
- `src/main/java/com/th4/edge/decoboon/payroll/service/attendances/summary/info/support/Info.java`
- `src/test/java/com/th4/edge/decoboon/payroll/PayrollConfirmCommissionDecoupledTest.java`
- `src/test/java/com/th4/edge/decoboon/payroll/PayrollAttendanceInfoScopeTest.java`
- Web `src/domains/finance/payrollAccess.ts`
- Web `src/views/finance/FinanceEmployeeSalariesView.vue`
- Web `src/views/finance/FinanceEmployeeCommissionsView.vue`
- Web `src/views/finance/FinancePayrollWorkbenchView.vue`

### 实现结果

1. 删除工资单确认中的 `commissionComplete` 阻断；提成完整度只保留管理提示。
2. `attendance/summary/info` 改为 `findOne(byId + DataScope specification)`，不再按企业裸读。
3. Web 薪酬方案入口、工资单生成和提成计提改用 `edit:payroll`。
4. Web 月度考勤入口改用 `edit:attendance`。
5. Web 工资单最终确认仅 BOSS。
6. Web 薪酬工作台的只读提示改用薪酬能力，不再复用通用财务写权限。
7. 员工发放按钮保持原状，未在 H1 修改店长发放边界。

### 生产制品核对

- release：`/opt/decoboon/releases/20261022-131500`
- `javap` 结果显示生产 `PayrollSettlementContext.confirm` 不存在提成完整性阻断。
- 结论：生产制品符合 BR-039/DEC-047；当前 `dev/prod` 源码为回归状态，H1 已恢复源码一致。

### 测试证据

```text
mvn -o -Dtest=PayrollConfirmCommissionDecoupledTest,PayrollAttendanceInfoScopeTest,PayrollAttendanceScheduledDaysTest,PayrollCompensationScopeTest test
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

mvn -o test
Tests run: 1072, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

npm run type-check
PASS

npm run build
PASS

npm run check:permissions
npm run check:store-scope
npm run check:workbenches
PASS
```

### 未覆盖/待验收

- 真实登录态角色矩阵页面证据；
- 真实跨店 `attendance/summary/info` HTTP 404/403；
- 独立验收；
- 生产发布与回滚演练。


### JDK 22 隔离全量复核

当前共享工作区包含并行费用专项改动，JDK 22 全量出现 1 个非 H1 失败：

```text
SummaryImplementationRulesTest.entityGraphMustStayNarrow
finance/repository/ExpenseRepository.java @EntityGraph 8 个属性
```

为隔离 H1，使用临时 detached worktree（HEAD `7cf83fa`）仅应用 H1 后端改动后重跑：

```text
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-22.jdk/Contents/Home
mvn -o test
Tests run: 931, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

结论：H1 后端在 Java 22 干净基线下通过全量；共享工作区的唯一失败来自并行费用专项。


## B2 财务月度考勤跨端对齐（2026-10-23）

### 状态

- 当前阶段：`ACCEPTED`（用户验收，非独立评审）
- 黑盒验证：`PASS`
- 用户验收：`PASS`（2026-10-23，用户指令“下一步”）
- DDL/迁移：无

### Changed files

- `src/main/java/com/th4/edge/decoboon/payroll/service/attendances/summary/save/support/Save.java`
- `src/main/java/com/th4/edge/decoboon/payroll/service/attendances/summary/delete/support/Delete.java`
- `src/main/java/com/th4/edge/decoboon/payroll/service/attendances/summary/update/support/Update.java`
- 对应考勤权限/守卫测试
- Web `src/components/finance/FinanceAttendanceDrawer.vue`
- Web `src/components/picker/EmployeePicker.vue`
- Web `src/domains/finance/api.ts`
- Web `src/domains/finance/model.ts`
- Web `src/views/finance/FinanceEmployeeSalariesView.vue`

### 实现结果

1. Web 选择员工和工资月份后调用 `payroll/compensation/info`，将应出勤作为默认值加载；允许在 1～31 天内调整，保存为考勤快照；无方案不回退 22。
2. 支持已有考勤编辑、删除、确认和完整异常字段。
3. 历史工资单“去确认考勤”携带工资单月份。
4. 店长可本店新增/编辑/删除考勤；跨店新增、更新、删除返回 404。
5. 已生成工资项的考勤更新返回 409；删除同样阻断。
6. 已进入工资单的考勤前端不可编辑/删除。

### 2026-10-30 App 对齐补充

- App 月度考勤新增/编辑页与 Web 使用同一规则：方案值作为默认值，允许财务在 1～31 天内调整。
- App 保存和更新均显式提交 `scheduledDays`，由 `attendance/summary/save|update` 写入考勤快照。
- App 与 Web 均不得在无生效薪酬方案时伪造考勤，不得使用 22 作为缺失方案兜底。

### 验证证据

```text
mvn -o -Dtest=PayrollDeleteGuardTest,PayrollAttendanceScheduledDaysTest,PayrollAttendanceInfoScopeTest test
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

隔离 JDK 22 全量：931 tests / 0 failures / 0 errors / 0 skipped
Web type-check/build + 权限/门店范围/工作台/文案门禁 PASS
API 黑盒：10 PASS / 0 FAIL
```

### 未覆盖

- 用户验收；
- 独立评审；
- 生产发布与冒烟。


## 2026-10-30 员工发放上下文与幂等对齐

- `finance/payment/employee/page` 新增可选 `sheetId` 过滤，工资单详情发放只返回该工资单 `PayrollItem` 产生的应付。
- Web 发放抽屉接收 `storeId` 和 `sheetId`，不再自动选中第一条应付；工资单详情仅在已确认或部分发放状态显示发放动作。
- App 员工发放补齐发放日期和备注；失败重试复用同一 `paymentIntentId`，应付、账户、日期或备注变化后重新生成提交意图。
- 验证：后端定向 2/2；Java 22 全量 1130/0/0/0；App type-check、契约、小程序构建 PASS；Web type-check、生产构建及门禁 PASS。
- 未覆盖：真实 Finance/BOSS 账号完成 Web/App 发放及回读。


## 2026-10-30 薪酬入口权限与门店范围对齐

- App 员工提成列表的“计提”入口改为 `edit:payroll` 门控；店长保留只读列表但不能看到不可执行动作。
- Web 月度考勤抽屉接收 `storeId`，分页请求和员工选择器均继承当前门店。
- Web 提成计提抽屉接收 `storeId`，员工选择器继承当前门店。
- 验证：后端范围定向 6/6；App 契约/type-check/文案/UI/小程序构建 PASS；Web type-check/生产构建 PASS。
- 未覆盖：真实店长/财务账号的门店页面交互和独立验收。


## B3A 工资单现有执行链 Web/App 对齐（2026-10-23）

### 状态

- 当前阶段：`ACCEPTED`（用户验收，非独立评审）
- 黑盒验证：`PASS`
- 用户验收：`PASS`（2026-10-23，用户指令“下一步”）
- DDL/迁移：无
- 生产发布：未执行

### Changed files

- `src/main/java/com/th4/edge/decoboon/payroll/context/PayrollAccessContext.java`
- `src/main/java/com/th4/edge/decoboon/payroll/service/sheets/info/support/Info.java`
- `src/test/java/com/th4/edge/decoboon/payroll/PayrollSheetInfoScopeTest.java`
- Web `src/domains/finance/api.ts`
- Web `src/domains/finance/model.ts`
- Web `src/views/finance/FinanceEmployeeSalariesView.vue`

### 实现结果

1. Web 选择工资单时同时调用 `payroll/sheet/info` 和 `payroll/item/page`，详情标题与摘要不再只依赖列表快照。
2. 工资项展示基本工资、提成、奖金、补贴、考勤扣款、社保、个税、其他扣款、应发、扣款合计、实发、状态、归属和备注。
3. 确认前增加二次确认，明确生成员工应付、计入项目/企业成本、工资条发放后才可见。
4. 确认成功后重新读取工资单详情和工资项，状态、确认人、确认时间和工资项状态同步更新。
5. BOSS 可删除 Draft 工资单并二次确认；已确认、已发放或已形成应付的草稿删除由后端 409 兜底。
6. 最终确认按钮继续只对 BOSS 展示；Finance/StoreManager 不展示确认与删除。
7. `payroll/sheet/info` 从企业裸读改为与列表一致的组织范围 Specification：店长按本店工资单头或本店工资项可见，总部财务保持企业范围。
8. 工资单详情读取使用只读事务，确认前后均回读服务端事实。

### 测试证据

```text
mvn -o -Dtest=PayrollSheetInfoScopeTest,PayrollSheetDeleteGuardTest,PayrollConfirmCommissionDecoupledTest test
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

隔离 JDK22 全量：
Tests run: 932, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

Web type-check/build + 权限/门店范围/工作台/客户端文案门禁 PASS
API 黑盒：20 PASS / 0 FAIL
```

### 未覆盖

- 用户验收；
- 独立评审；
- 生产发布与冒烟；
- 复核、作废/退回和员工发放权限，继续保持 B3B BLOCKED。


## B3B1 薪酬复核与员工发放权限闭环（2026-10-23）

### 状态

- 当前阶段：`RECONCILED`
- 黑盒验证：`PASS`
- 用户验收：`PENDING`
- DDL/迁移：无
- 生产发布：未执行

### Changed files

- `src/main/java/com/th4/edge/decoboon/payroll/context/PayrollSettlementContext.java`
- `src/main/java/com/th4/edge/decoboon/payroll/context/PayrollSettlementAuditContext.java`
- `src/main/java/com/th4/edge/decoboon/payroll/context/EmployeePaymentContext.java`
- `src/main/java/com/th4/edge/decoboon/payroll/service/sheets/update/support/Update.java`
- `src/main/java/com/th4/edge/decoboon/payroll/service/sheets/info/support/Info.java`
- `src/main/java/com/th4/edge/decoboon/finance/service/payments/employee/page/support/Page.java`
- `src/test/java/com/th4/edge/decoboon/payroll/PayrollSheetReviewFlowTest.java`
- `src/test/java/com/th4/edge/decoboon/payroll/PayrollEmployeePaymentAccessTest.java`
- Web `src/domains/finance/api.ts`
- Web `src/domains/finance/model.ts`
- Web `src/domains/finance/payrollAccess.ts`
- Web `src/views/finance/FinanceEmployeeSalariesView.vue`
- Web `src/views/finance/FinanceEmployeeCommissionsView.vue`
- App `src/domains/payroll/api.ts`
- App `src/domains/payroll/model.ts`
- App `src/subpackages/payroll/pages/sheet-detail.vue`

### 实现结果

1. BOSS/Finance 可将 Draft 工资单提交为 Pending；提交前执行考勤、工资项和实发非负校验。
2. BOSS 可将 Pending 退回 Draft，原因必填并写入最新退回原因；完整状态迁移写入结构化审计日志。
3. BOSS 可直接确认 Draft 或 Pending；FINANCE/店长确认继续 403。
4. 店长员工工资/提成发放 page/save 均改为薪酬角色校验，店长 403；Web/App 发放入口均不显示。
5. Web/App 均支持复核提交、Pending 展示、BOSS 退回和退回原因展示。
6. BOSS 确认和草稿删除回归通过；重复确认保持应付/工资条 `5/5`。
7. 本批无 DDL，不包含作废、反向应付/成本、版本模型或批量/部分发放。

### 验证证据

```text
B3B1 定向测试：9/0/0/0
隔离 JDK22 全量：938/0/0/0
API 黑盒：14 PASS / 0 FAIL
Web 三角色：PASS，console error 0
App H5 三角色：PASS，console error 0
Web build / 权限 / 门店范围 / 工作台 / 文案门禁 PASS
App type-check / 薪酬闭环契约 / mp-weixin build PASS
```

证据目录：`docs/acceptance/runs/20261023-payroll-b3b1/`。

## PAY-SELF-ATTENDANCE 员工本人月度考勤（2026-10-30）

### 状态

- 当前阶段：`DEV_VERIFIED`
- 真实页面：`PENDING_REAL_UI_ACCEPTANCE`
- DDL/迁移：无
- 生产发布：未执行
- 变更记录：`CHG-20261030-005`

### Changed files

- 后端：`PayrollAccessContext.java`、`attendance/summary/page/support/Page.java`、`attendance/summary/info/support/Info.java`
- 后端测试：`PayrollAttendanceAccessContextTest.java`、`PayrollAttendanceSelfViewTest.java`、`PayrollAttendanceInfoScopeTest.java`
- App：`domains/payroll/api.ts`、`my-attendance.vue`、`pages/mine/index.vue`、`roles/permission.ts`、`roles/guard.ts`、`config/index.ts`、`pages.json`
- App 门禁：`scripts/verify-payroll-closure-contract.mjs`
- Web：`domains/finance/api.ts`、`domains/finance/meta.ts`、`MyAttendanceView.vue`、`roles/routes.ts`、`layouts/RoleAreaShell.vue`、`components/finance/FinanceAttendanceDrawer.vue`

### 实现结果

1. `PayrollAccessContext` 新增考勤查看主体解析：BOSS/Finance/StoreManager 保留管理视角，其他员工必须解析为 Self 范围。
2. `attendance/summary/page` 新增 `selfOnly` 本人视角；非管理员工无论是否传参数都固定本人，管理角色传 `selfOnly=true` 时也只返回登录人，均不信任客户端 `empId/storeId`。
3. `attendance/summary/info` 同步支持本人 Detail 读取；非管理员工仍通过统一 DataScope Specification，读取他人记录返回“考勤不存在或无权访问”。
4. App 新增“我的考勤”页面与“我的工资”入口，支持月份切换、列表/详情、错误重试和空态；能力键新增 `view:ownAttendance` 并纳入页面守卫。
5. Web 新增 `/account/attendance` 与账户菜单“我的考勤”，支持月份切换、详情回读、错误恢复；复用同一 Provider，不新增 Web 专用后端。
6. App/Web 本人视角均不提交 `empId` 或可信门店归属；Web 使用服务端分页契约，未通过固定大页绕过门禁。

### 验证证据

```text
后端定向：8/0/0/0
Java22 全量：1136/0/0/0
App payroll contract/type-check/client-copy/UI/navigation/mp-weixin/subpackages：PASS
Web type-check/production build/permissions/store-scope/workbenches：PASS
```

证据目录：`docs/acceptance/runs/20261030-payroll-self-attendance/`。

### 未覆盖

- 受控员工与管理角色的 App/Web 真实页面回读；
- 独立验收；
- 生产发布与冒烟。

## PAY-SELF-COMMISSION 员工本人提成（2026-10-30）

### 状态

- 当前阶段：`DEV_VERIFIED`
- 真实页面：`PENDING_REAL_UI_ACCEPTANCE`
- DDL/迁移：无
- 生产发布：未执行
- 变更记录：`CHG-20261030-006`

### Changed files

- 后端：`PayrollAccessContext.java`、`payroll/commission/page/support/Page.java`、`payroll/commission/info/support/Info.java`
- 后端测试：`PayrollCommissionAccessContextTest.java`、`PayrollCommissionSelfViewTest.java`、`PayrollCommissionInfoScopeTest.java`
- App：`domains/payroll/api.ts`、`my-commission.vue`、`pages/mine/index.vue`、`roles/guard.ts`、`config/index.ts`、`pages.json`
- App 门禁：`scripts/verify-payroll-closure-contract.mjs`
- Web：`domains/finance/api.ts`、`domains/finance/meta.ts`、`MyCommissionsView.vue`、`roles/routes.ts`、`layouts/RoleAreaShell.vue`、`FinanceEmployeeCommissionsView.vue`

### 实现结果

1. `PayrollAccessContext` 新增提成查看主体解析：管理角色保留企业/门店视角，其他员工必须解析为 Self。
2. `payroll/commission/page` 新增 `selfOnly` 本人视角；非管理员工始终按登录员工过滤，管理角色本人视角也只返回本人。
3. `payroll/commission/info` 从企业裸读改为统一 DataScope Specification；普通员工读取他人提成返回“不存在或无权访问”，同时修复管理详情未统一使用范围的问题。
4. App 新增“我的提成”页面和“我的工资”入口，复用已有 `view:commission` 能力键；只读展示项目、比例、提成金额、计提/释放/发放进度和状态。
5. Web 新增 `/account/commissions` 与账户菜单“我的提成”，使用同一 Provider、服务端分页和详情回读。
6. App/Web 本人视角均不提交 `empId` 或可信门店归属；不提供计提、释放、调整或发放动作。

### 验证证据

```text
后端定向：8/0/0/0
Java22 全量：1144/0/0/0
App payroll contract/type-check/UI/navigation/mp-weixin/subpackages：PASS
App check:client-copy：被并行改动 template-edit.vue:461 阻断（非本批）
Web type-check/production build/permissions/store-scope/workbenches：PASS
```

证据目录：`docs/acceptance/runs/20261030-payroll-self-commission/`。

### 未覆盖

- 受控员工与管理角色的 App/Web 真实页面回读；
- 独立验收；
- 生产发布与冒烟。

### PAY-SELF-COMMISSION 真实页面验收（2026-10-30）

- 环境：本地 dev 后端 `8090`、Web `8080`、微信开发者工具 `dist/dev/mp-weixin` + automator `9420`。
- 测试身份：普通员工 Designer、对照员工 Marketer、Finance 管理角色；测试数据仅存本地验证库，验收后清理。
- 微信小程序：本人列表/详情、个人中心入口、他人详情拒绝、Finance 本人视角、Finance 管理范围共 8/8 PASS。
- Web：员工账户菜单进入本人提成、本人列表/详情、Finance 本人视角、Finance 管理页均 PASS；1440/1280/1024 无横向溢出；无 Runtime exception/console error。
- 金额一致：员工两端均为提成金额 `¥2,500`、已释放 `¥1,500`、已发放 `¥500`、状态待发放；Finance 本人两端均为 `¥600`；管理端两端均为 3 条。
- 越权：普通员工读取他人提成返回 404“提成不存在或无权访问”；伪造 `storeId` 和 `selfOnly=false` 仍只返回本人。
- 账本状态：`USER_ACCEPTED_NON_INDEPENDENT / PENDING_RELEASE`；独立验收未执行。

### 用户验收回写

- 2026-10-30 用户明确回复“确认”，本批记为 `USER_ACCEPTED_NON_INDEPENDENT`。
- 仍不宣称独立验收；生产发布未执行。

## PAY-MANAGER-SENSITIVE-FIELDS 店长本店薪酬字段（2026-10-30）

### 状态

- 业务裁决：`DEC-096` / `ANS-BIZ-20261030-001` / `EV-428`；`GAP-074` 已关闭。
- 当前阶段：`DEV_VERIFIED / PENDING_REAL_STORE_MANAGER_UI`
- 生产代码变更：无；现有 Provider 已符合裁决。
- DDL/迁移：无。

### 实现结果

1. 店长在本店范围内可读取薪酬详情全部现有字段，包括 `socialInsuranceAmount`、`taxAmount`。
2. 跨店、跨企业仍由 DataScope/StoreFilterContext 拒绝。
3. B7 当时按 `DEC-093` 保持动作权限不变；该动作限制随后已由 `DEC-097` 在本店范围放开，B8A 另行实现。
4. 本批不新增银行卡、身份证、税率、应纳税所得额或专项扣除字段。
5. `PayrollCompensationGuardTest` 新增店长详情和 `payroll/item/page` 字段白名单断言，固定本裁决。

### 验证

```text
定向：PayrollCompensationGuardTest / PayrollEmployeePaymentAccessTest / PayrollAttendanceInfoScopeTest
7/7 PASS
```

全量测试本轮受共享工作区 `target` 并发清理影响，出现 2 个非薪酬类生成类/配置错误；此前同分支 Java 22 全量为 1144/0/0/0。B7 字段页面验收待补；动作权限已转入 B8A。

## PAY-MANAGER-ACTIONS-B8A 店长本店薪酬动作（2026-10-30）

### 状态

- 业务裁决：`DEC-097` / `ANS-BIZ-20261030-002` / `EV-429`
- 当前阶段：`DEV_VERIFIED / PENDING_REAL_STORE_MANAGER_UI`
- DDL/迁移：无
- 作废：B8B 仍 `BLOCKED`，不得只改状态。

### 实现结果

1. `PayrollAccessContext` 拆分动作角色：确认/退回/删除允许 BOSS+StoreManager；发放允许 BOSS+Finance+StoreManager；制单/复核仍 BOSS+Finance。
2. `PayrollSettlementContext` 确认和退回使用新动作角色；确认/退回前强制店长工资单全部工资项属于本店。
3. `payroll/sheet/delete` 使用 `visibleSheets(current)` 和本店工资项检查，店长不能再按企业 ID 裸删除。
4. `finance/payment/employee/page|save` 改用 `requirePaymentActor`；店长只读取/发放本店应付，资金台账、应付核销、工资条和幂等逻辑不变。
5. App 店长获得 `confirm:payroll`、`pay:payroll`，Draft 删除改为能力键门控；Web `payrollAccess` 同步允许店长确认、退回、删除和发放。
6. StoreManager 仍不获得薪酬方案、考勤、提成计提、工资单生成、最终确认以外的新建类动作；B8B 作废未实现。

### 验证

```text
后端定向：16/0/0/0
App type-check / payroll contract / client-copy / UI / navigation / mp-weixin / subpackages：PASS
Web type-check / production build / permissions / store-scope / workbenches：PASS
```

证据：`docs/acceptance/runs/20261030-payroll-manager-actions/`。


## 2026-10-31 PAY-OPS-01 薪酬工作台聚合 SQL 化

### 状态

- 当前阶段：`DEV_VERIFIED`
- 真实页面：`PENDING_REAL_UI_ACCEPTANCE`
- 独立验收：`PENDING`
- DDL/迁移：无
- 生产发布：未执行
- 变更记录：`CHG-20261031-051`

### Changed files

- `src/main/java/com/th4/edge/decoboon/payroll/context/PayrollWorkbenchContext.java`
- `src/main/java/com/th4/edge/decoboon/payroll/context/PayrollWorkbenchAggregateContext.java`
- `src/test/java/com/th4/edge/decoboon/payroll/PayrollWorkbenchContextTest.java`
- `src/test/java/com/th4/edge/decoboon/arch/SummaryImplementationRulesTest.java`
- `src/test/java/com/th4/edge/decoboon/finance/FinanceStoreScopeHttpAcceptanceTest.java`

### 实现结果

1. `PayrollWorkbenchContext` 不再注入 5 个实体 Repository，不再 `findAll` 后 Java `filter/count/reduce`；只负责权限、门店、期间解析和聚合结果组装。
2. 新增 `PayrollWorkbenchAggregateContext`，用 Criteria 聚合/投影 SQL 返回 6 组有界结果；每组最多 1 行。
3. 工资单单店可见性用子查询下推，避免“先查本店工资项 ID，再拼接主查询”的额外查询。
4. 考勤保持 `Pending/Disputed`；员工提成待付跨期间；工资取目标期间。
5. 员工工资/提成未付金额逐条 `max(应付-已付,0)`；已释放未付提成逐条 `max(已释放-已付,0)`。
6. 最近工资单使用 `period DESC, sheetId DESC LIMIT 1`。
7. 响应字段和 `@Properties` 白名单未改变。

### 验证证据

```text
改造前：15 SQL / 22 entity loads / 493B
改造后：14 SQL / 2 entity loads / 493B
10×：14 SQL / 2 entity loads
定向：33/0/0/0
全量：1188/0/0/0
Provider 审计：543 providers / 0 findings
```

证据目录：`docs/acceptance/runs/20261031-pay-ops-01/`。

### 未覆盖

- 生产候选环境复测；
- 独立视觉与业务验收；
- 生产发布与回滚演练。


## 2026-10-31 PAY-OPS-01 Web/App 回读与本地 MySQL 性能

- Web 真实页面 `/finance/payroll?period=2026-10` 与服务端一致：¥1,600.00、考勤 2、工资单草稿 1；
- 浏览器 API 20 次：P50 41.9ms、P95 66.1ms、max 71.2ms、HTTP 200、491B、console error 0；
- 1440/1280/1024 三档无横向溢出；
- 微信开发者工具原生页面 5/5 PASS，显示同一金额和计数；
- 视觉审查：`VISUAL_REVIEW_UNAVAILABLE`（当前模型无图片输入）；
- 两个隔离周期合成数据均已清理，目标表计数为 0。


## 2026-10-31 PAY-OPS-01 10× MySQL 并发压测

- 数据：8 员工/96 工资项 → 80 员工/960 工资项、960 考勤、960 工资条、960 提成、1,040 应付；
- 三次独立运行中位值：
  - 10× 顺序：P50 13.54ms、P95 21.87ms、P99 22.43ms、max 22.43ms；
  - 10× 20 并发 ×200 请求：P50 19.25ms、P95 28.19ms、P99 29.75ms、max 31.78ms；
  - 错误率 0，吞吐 974.26 RPS；
  - MySQL `Questions` 单请求增量：基线 22、10× 22；
  - 响应体约 503B。
- 预算：核心读取 API P95≤500ms、P99≤1000ms，PASS。
- 压测后隔离 ID 段清理为 0。
- 限制：本机 MySQL 与真实 Provider HTTP，不等同生产机器规格、网络和生产并发；发布前需在生产候选环境复测。
