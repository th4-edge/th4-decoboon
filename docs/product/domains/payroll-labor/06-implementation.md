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
