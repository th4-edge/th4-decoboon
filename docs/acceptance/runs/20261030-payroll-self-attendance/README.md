# 2026-10-30 员工本人月度考勤跨端开发验证

## 结论

- Domain：`payroll-labor`
- Batch：`PAY-SELF-ATTENDANCE`
- Phase：`DEV_VERIFIED`
- 状态：`PENDING_REAL_UI_ACCEPTANCE`
- 未发布、未独立验收

## Provider 对账

| 能力 | Provider | App | Web | 服务端权限 |
| --- | --- | --- | --- | --- |
| 本人月度考勤列表 | `attendance/summary/page` | `myAttendancePage({ month, selfOnly:true })` | `myEmployeeAttendances({ month, current, size })`，内部固定 `selfOnly:true` | 非管理员工始终 Self；管理角色 `selfOnly=true` 时也只返回登录人 |
| 本人月度考勤详情 | `attendance/summary/info` | 列表字段完整；不另建同义 Provider | `myEmployeeAttendanceInfo(summaryId)` | 非管理员工通过 DataScope Self 读取；他人记录返回不存在或无权访问 |
| 管理端月度考勤 | 同一 Provider | 原 `attendancePage` 保留 | 原 `employeeAttendances` 保留 | BOSS/Finance/StoreManager 原企业/门店范围不退化 |

## 完整闭环范围

本批只补“员工本人查看本人月度考勤”的读侧闭环，不开放本人写考勤、不提成、不发工资、不触碰店长敏感字段。

## 自动化证据

- 后端定向：8/8 PASS。
- Java 22 全量：1136 tests / 0 failures / 0 errors / 0 skipped。
- App：薪酬契约、type-check、文案/UI/导航门禁、小程序构建、分包检查全部 PASS。
- Web：生产构建及其门禁、权限、门店范围、工作台检查全部 PASS。
- 初始 Web 构建被“固定大页 200”门禁拦截，已改为 `current/size` 分页契约，未修改基线放行。

## 待补真实证据

1. 普通员工在 App“我的工资 > 我的考勤”选择月份，只看本人记录。
2. 同一员工在 Web 账户菜单“我的考勤”查看同一月份，字段/状态/确认信息一致。
3. 普通员工无法通过已知他人记录打开详情。
4. 管理角色打开本人视角仍只显示本人。
5. 管理角色原考勤管理视图仍按企业/门店范围显示。
