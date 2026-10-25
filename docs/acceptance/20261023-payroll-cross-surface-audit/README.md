# 2026-10-23 员工薪酬 Web/App 跨端一致性只读对账

## 目的与边界

核对员工薪酬域在 Web、App、后端 Provider 和业务规则之间是否形成同一能力契约。方式为当前工作区静态代码对账、生产只读数据核对和当前线上 Web 构建核对。未登录生产页面、未执行生产写操作、未修改业务代码、未做独立验收。

## 审计基线

| 端 | 分支/版本 | 提交或 release |
| --- | --- | --- |
| 后端 | `dev` | `7cf83fa` |
| Web | `dev` | `0cbdc06` |
| App | `dev` | `aa387a8` |
| 线上 Web | 生产 | `/opt/decoboon-web/releases/20261022-131900` |

三个工作区均有其它在途改动；本次只读取薪酬相关文件，不把工作区所有差异归入本问题。

## 生产只读数据

企业：麦鸿装饰 `com_id=1790066191585976744`。

- 当前有效薪酬方案 5 份，`scheduled_days` 均为 `27`；
- 2026-08 至 2026-10 已保存考勤的 `scheduled_days` 均为 `27`；
- 说明后端保存链路已按薪酬方案写入应出勤快照；
- 线上 Web 当前分块 `FinanceEmployeeSalariesView-C4oV3mks.js` 仍包含 `scheduledDays:"22"`，员工选择回调只更新姓名。

## 接口适配对账

- 后端薪酬相关 Provider：31 个；
- App `payrollApi` 使用：27 个；
- Web `financeApi` 使用：22 个。

App 已适配、Web 未适配：

```text
payroll/compensation/info
attendance/summary/info
attendance/summary/delete
payroll/sheet/info
payroll/sheet/delete
payroll/commission/release/page
```

Web 已适配、App 未适配：

```text
payroll/commission/report
```

两端均未适配：

```text
payroll/commission/adjustment/save
payroll/commission/info
payroll/store-payroll/info
```

接口数量不等同于用户功能缺陷数量；例如 App 的提成释放接口当前也没有页面消费。

## 主要差异

### Web 落后 App

1. 应出勤未从员工当月薪酬方案联动；
2. 不能查看并编辑已有考勤；
3. 不能删除未进工资单的考勤；
4. 缺外勤、年假、调休字段；
5. 不能删除工资单草稿；
6. 工资项构成展示不完整；
7. 确认前无二次确认，历史工资单跳考勤未携带正确月份；
8. 员工选择器固定最多读取 200 人后本地过滤。

### App 落后 Web

1. 薪酬方案缺员工、状态、生效月份筛选；
2. 缺提成汇总报告；
3. 我的工资条未读取详情接口。

### 两端共同缺口

1. 工资单复核 `Draft -> Pending`；
2. 工资单作废、退回重算；
3. 提成释放记录页面；
4. 人工提成负向调整页面；
5. 本店工资项详情；
6. 批量或部分员工发放 UI。

### 权限和后端一致性风险

1. Web 员工工资、提成和工作台错误复用通用财务权限；
2. App 提成页对店长仍显示“计提”；
3. 后端考勤新增/删除只允许 BOSS/Finance，与店长本店考勤规则不一致；
4. 店长员工发放权限在 `BR-038` 与 `DEC-065` 间仍未裁决；
5. `PayrollSettlementContext` 仍以提成未结清阻断工资确认，但 `EV-149` 记录已修复并生产发布；
6. `attendance/summary/info` 仅校验企业，未校验店长门店范围。

## 结论

当前问题不是单页面错误，而是薪酬能力契约缺少跨端对账门禁。先派发 H1 事故收口；B2 处理考勤完整对齐；B3A 处理工资单现有执行链；B3B 在店长确认/作废和员工发放边界裁决后启动。本审计不证明任何批次已修复或通过验收。


## 7. H1 执行补充（2026-10-23）

### 生产制品核对

- 当前后端 release：`/opt/decoboon/releases/20261022-131500`。
- 仅读提取 `th4-decoboon-1.0.0.jar` 中 `PayrollSettlementContext.class` 并执行 `javap`。
- 生产制品中的 `confirm` 只保留状态、考勤完整、工资项非空和实发非负校验，不存在 `commissionComplete` 阻断。
- 因此：**生产制品符合提成解耦口径，当前 `dev/prod` 源码发生了回归**。

### H1 本地验证

- 后端定向：`7/0/0/0`；
- 后端全量：`1072/0/0/0`；
- Web `type-check`、`build`、权限/门店范围/工作台静态门禁 PASS；
- 未执行真实 Web 登录态 UI、真实跨店 HTTP 和独立验收。


### H1 Java 22 隔离全量

- 共享工作区 JDK 22 全量：1072 tests，1 个非 H1 失败（并行费用专项 `ExpenseRepository` 的 `@EntityGraph` 属性过多）。
- 临时 detached worktree（HEAD `7cf83fa`）仅应用 H1 后端改动后：931/0/0/0，BUILD SUCCESS。
- 结论：H1 自身通过 Java 22 全量；共享工作区失败不得归入 H1，也不得越界修复。
