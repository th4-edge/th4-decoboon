# 2026-11-04 考勤业务体系前后端与真实交互审计

## 1. 结论

当前考勤体系仍存在一类会产生真实数据污染的系统性问题：**切换月份或员工时，考勤录入字段没有完整重置**。该问题已在线上 Web 用财务真实操作路径复现，App 源码也走同一状态保留模式。

此外还确认了权限、状态机、确认留痕、天数校验、工资单重算和列表分页方面的风险。结论不是“单页文案问题”，而是考勤事实从录入、确认、锁定到工资单重算之间缺少一组统一约束。

本次只做只读生产核查、真实浏览器操作、本地自动化探针和源码审计；**未保存考勤、未修改生产数据、未执行生产回填、未做独立业务验收**。

## 2. 环境与基线

| 项 | 内容 |
| --- | --- |
| 生产地址 | `https://decoboon.joytsing.com` |
| 后端生产 release | `20261102-171937` |
| Web 生产 release | `20261102-171937` |
| 真实操作账号 | 吴濛濛 / Finance / 全公司 |
| 生产只读数据 | 安徽麦鸿装饰设计工程有限公司 |
| 当前后端 | `dev`，含 `CHG-20261104-001` 未发布改动 |
| Web/App | 当前工作区源码 + 线上 Web 实际构建 |
| 生产写操作 | 0 |

## 3. 真实交互复现

### WEB-01 切月保留考勤天数：FAIL

1. 打开“员工工资 → 月度考勤”；
2. 填写“正常出勤 5 天、事假 2 天”；
3. 工资月份从 `2026-11` 切换到 `2026-10`；
4. 切换后页面显示 `2026-10`，但正常出勤仍为 5、事假仍为 2。

证据：[web-month-switch-carries-days.png](evidence/web-month-switch-carries-days.png)

对应代码：

- `FinanceAttendanceDrawer.vue` 的 `onMonthChange()` 只清空 `editingId/scheduledDays/schemeError`，没有清空考勤天数与备注；
- `resetForm()` 只在抽屉打开/外部月份变化时执行，内部月份切换没有调用完整重置。

### WEB-02 切员工保留考勤天数：FAIL

1. 保持“正常出勤 5 天、事假 2 天”；
2. 从待选员工切换为“王宏文”；
3. 应出勤更新为 27 天，但正常出勤仍为 5、事假仍为 2。

证据：[web-employee-switch-carries-days.png](evidence/web-employee-switch-carries-days.png)

这说明：**这不是“9 月记录被接口错误查出”，而是上一员工/上一月输入值可以继续成为下一员工/下一月的待保存数据。**

### WEB-03 关闭后重开重置：PASS

关闭“月度考勤”抽屉后重新打开，月份回到默认 `2026-11`，考勤天数回到 0，员工为空。

因此当前只修了“重开时重置”，没有修完“抽屉内切月/切员工时重置”。

### App 同路径：静态 FAIL，未做真机验证

- `attendance-edit.vue` 的月份变化只触发 `loadEffectiveScheme()`，不清空七个考勤天数字段；
- 从员工选择页返回时 `onShow()` 只替换 `selectedEmployee`，不清空天数字段；
- 因此 App 同样存在切月、切员工后带值的问题。

未在微信开发者工具或真机执行，不得表述为 App 实机通过。

## 4. 已确认问题清单

| ID | 等级 | 问题 | 证据 | 状态 |
| --- | --- | --- | --- | --- |
| ATT-01 | P0 | 门店财务可修改同企业其他门店的考勤 | `Update` 仅对 `StoreManager` 手工校验门店，对 `Finance` 未使用 DataScope；本地探针已复现 | CONFIRMED |
| ATT-02 | P1 | Web/App 切月或切员工不清空考勤天数 | 线上 Web 两场景复现，App 静态同路径 | CONFIRMED |
| ATT-03 | P1 | 状态机可绕过：更新接口可直接写 `InPayroll`、`Confirmed`、`Rejected` 等任意合法枚举 | `Update.parseStatus()` 直接 `valueOf`；本地探针将 Pending 直接写成 InPayroll | CONFIRMED |
| ATT-04 | P1 | 已确认考勤可修改数据但不重新确认，确认人和确认时间保持旧值 | `Update` 不校验当前状态；本地探针修改 Confirmed 数据后旧确认痕迹不变 | CONFIRMED |
| ATT-05 | P1 | 考勤新增、修改、删除没有业务审计留痕 | 没有 attendance audit context；仅建表时间和确认人字段 | CONFIRMED |
| ATT-06 | P1 | 后端校验七个考勤天数字段，也不校验各天数字段与应出勤的关系 | `Save/Update` 只校验 scheduledDays；负数探针保存成功；生产 8 月已有 5 条 27 正常 + 15 事假 > 27 应出勤 | CONFIRMED |
| ATT-07 | P1 | 工资单草稿重算不清理已退出方案范围的旧工资项 | `PayrollBuildContext` 只 upsert 当前来源，不删除旧项；本地探针确认旧项仍计入合计 | CONFIRMED |
| ATT-08 | P1 | 当前生产仍存在工资单已生成但考勤未确认/缺失的状态不一致 | 2026-09 有 5 个工资项但吴濛濛无 9 月考勤；2026-10 有 5 个工资项但 5 人考勤均为 Pending | PRODUCTION_CONFIRMED |
| ATT-09 | P2 | 非法月份冒泡为系统异常，不是可读业务错误；并发重复保存也缺少唯一冲突转换 | `YearMonth.parse(month)` 无捕获；探针传入 `2026-13` 得到 `DateTimeParseException` | CONFIRMED |
| ATT-10 | P2 | 考勤分页在内存中完成：先全量加载可见记录，再过滤月份并分页，接口允许 `size=200` | `attendance/summary/page` 使用 `findAll(...).stream()`，违反服务端分页和 50 行硬上限 | CONFIRMED |
| ATT-11 | P2 | 考勤确认无二次确认，确认后只提示成功，不提供下一步状态/回读说明 | Web/App 的 `confirm()` 直接调用更新接口 | CONFIRMED |
| ATT-12 | P2 | 快速切月/刷新存在响应乱序覆盖风险 | Web `load()` 无请求版本号；App `usePagedList.refresh()` 同样无请求版本号 | RISK_CONFIRMED_BY_CODE |

## 5. 生产数据发现

截至本次只读核查，麦鸿共有 14 条考勤记录、5 名员工，月份为 `2026-08` 至 `2026-10`。

### 5.1 8 月已形成异常工资事实

5 名员工均为：

```text
应出勤 27 天
正常出勤 27 天
事假 15 天
记录天数合计 42 天 > 应出勤 27 天
```

对应 8 月工资单状态为 `Paid`，考勤扣款按 15 天事假计算：

| 员工 | 基本工资 | 考勤扣款 | 实发 |
| --- | ---: | ---: | ---: |
| 吴濛濛 | 5000 | 2777.78 | 2222.22 |
| 王宏文 | 5000 | 2777.78 | 2222.22 |
| 董夏鹏 | 3000 | 1666.67 | 1333.33 |
| 谢玲玲 | 3000 | 1666.67 | 1333.33 |
| 黄贤杰 | 5000 | 2777.78 | 2222.22 |

这是当前最需要用户确认的历史数据风险：如果 8 月并不是全员 15 天事假，则该月已发放工资的计算依据错误；如果 15 天事假真实，则“正常出勤 27 天”字段同样错误。两种情况下都不能把当前记录视为一致事实。

### 5.2 9/10 月工资单与考勤不一致

| 月份 | 工资单 | 工资项数 | 考勤问题 |
| --- | --- | ---: | --- |
| 2026-09 | Draft | 5 | 吴濛濛无 9 月考勤；谢玲玲、黄贤杰仍为 Pending |
| 2026-10 | Draft | 5 | 5 人考勤均为 Pending |

当前生产制品尚未包含 `CHG-20261104-001` 的“生成前考勤完整 + 生成后 InPayroll + 删除草稿恢复 Confirmed”修复，因此线上仍可复现旧状态链。

### 5.3 董夏鹏 9/10 月同值

- 2026-09：正常 22、事假 1，InPayroll；
- 2026-10：正常 22、事假 1，Pending；
- 两条创建时间相差约 45 秒，binlog 显示为两次独立 INSERT。

因此不是查询串月，而是录入值被复制到了另一个月份；当前 Web 仍具备再次制造该结果的交互条件。

## 6. 状态机与权限审计

### 状态机目标

业务规则为：

```text
Draft → Pending → Confirmed → InPayroll
```

### 当前实现

- `Attendance/summary/update` 把编辑和确认合并为一个入口；
- 只要传入合法枚举，就能从任意状态跳到任意状态；
- 没有检查 `Pending → Confirmed`、`Confirmed → InPayroll` 等前置路径；
- 没有在已确认数据变更时重新盖确认章或清空旧确认信息；
- 没有要求 `InPayroll` 只能由工资单生成动作设置。

### 权限

- 读取：`info/page` 使用 DataScope，现有测试覆盖本人和门店读取；
- 新增：`Save.resolveEmployee()` 正确限制门店财务到本店；
- 删除：`Delete` 使用 DataScope；
- **修改：`Update` 是例外**。它只对 StoreManager 手工比较 `current.store` 与考勤 `store`，没有对 Finance 解析 DataScope，故门店财务可按 `summaryId` 跨店更新。

## 7. 自动化与门禁结果

| 检查 | 结果 |
| --- | --- |
| 后端薪酬考勤定向测试 | 8 个测试类、26 个用例、0 失败 |
| 缺陷现状探针 | 6/6 成功复现当前缺陷行为 |
| Web `npm run type-check` | PASS |
| App `npm run type-check` | PASS |
| Web `npm run check:permissions` | PASS |
| App `npm run check:finance-ia` | PASS |
| App `npm run check:finance-pickers` | PASS |
| 生产真实浏览器交互 | 3 个场景：2 FAIL、1 PASS |
| 微信开发者工具 / 真机 | 未执行 |
| 生产写操作 / 数据修复 | 未执行 |

现有门禁能证明“代码能编译、角色入口存在”，但不能证明“切月/切员工不串值、状态不会被跳过、已确认事实不会被静默改写”。

## 8. 建议修复顺序

### 第一批：阻断数据继续污染

1. Web/App 在切换月份或员工时统一调用完整表单重置；
2. 后端 `Update` 改为 DataScope + 状态迁移校验；
3. 禁止业务更新接口直接写 `InPayroll`；确认动作只允许合法前置状态；
4. 对七个天数字段做 0–31 校验，并明确跨字段合计校验口径；
5. 修改/删除考勤写业务审计记录。

### 第二批：修复工资单一致性

1. 重算草稿时清理不再属于当月方案范围的旧工资项，并同步恢复对应考勤状态；
2. 发布并验证 `CHG-20261104-001`；
3. 核对 2026-08 已发放工资的 15 天事假是否真实；
4. 处理 9 月缺失考勤、10 月未确认考勤和现有 Draft 的状态冲突。

### 第三批：交互与规模

1. 确认动作增加二次确认、状态回读和恢复路径；
2. 服务端分页下推，取消全量加载和 200 上限；
3. 快速切月/刷新增加请求版本或取消机制；
4. 补 App 真机场景和跨端同源证据。

## 9. 证据与限制

- 原始交互结果：[raw/web-observations.json](raw/web-observations.json)
- 后端探针结果：[raw/runtime-probes.json](raw/runtime-probes.json)
- 生产只读汇总：[raw/production-summary.json](raw/production-summary.json)
- 修复验证汇总：[raw/fix-verification.json](raw/fix-verification.json)
- 生产发布只读冒烟：[raw/release-smoke.json](raw/release-smoke.json)
- 截图：[evidence/web-month-switch-carries-days.png](evidence/web-month-switch-carries-days.png)、[evidence/web-employee-switch-carries-days.png](evidence/web-employee-switch-carries-days.png)

## 10. 修复执行状态（2026-11-04）

修复变更：`CHG-20261104-003`。

已完成：

- Web/App 切月、切员工清空考勤输入；
- 后端 Update 使用 DataScope；
- 状态迁移和确认完整性校验；
- 已确认数据修改退回 Pending 并清空旧确认章；
- 考勤新增、修改、确认、删除、锁定和恢复写结构化审计日志；
- 重算清理失效工资项并恢复考勤；
- 考勤分页下推数据库；
- 确认动作增加二次确认；
- 后端全量 315 个测试类、1239 个用例通过；
- Web 生产构建、App 类型检查与微信小程序构建通过。

已完成发布与只读冒烟：

- 后端 `20261104-123233`、Web `20261104-123300` 已发布；
- App `1.0.86 / 186` 开发版已上传；
- 真实财务账号验证切月/切员工归零、确认弹窗取消无写入、分页上限 50、非法月份 400；
- 生产数据计数保持 14/3/15，Pending 考勤 7 条不变。

仍待完成：

- 微信开发者工具/真机复测；
- 门店财务跨店真实负向验证；
- 2026-08 已发放异常数据确认与历史数据修复；
- 独立验收、业务验收和客户签字。

限制：

- 未在微信开发者工具/真机执行 App；
- 未用门店财务账号执行真实跨店写操作；跨店问题由本地定向探针和修复后代码约束确认；
- 未执行生产写入和历史数据回填；
- 本报告不等同于独立验收、业务验收或客户签字。
