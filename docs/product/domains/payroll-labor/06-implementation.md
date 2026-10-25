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

1. Web 选择员工和工资月份后调用 `payroll/compensation/info`，只读显示应出勤；无方案不回退 22。
2. 支持已有考勤编辑、删除、确认和完整异常字段。
3. 历史工资单“去确认考勤”携带工资单月份。
4. 店长可本店新增/编辑/删除考勤；跨店新增、更新、删除返回 404。
5. 已生成工资项的考勤更新返回 409；删除同样阻断。
6. 已进入工资单的考勤前端不可编辑/删除。

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
