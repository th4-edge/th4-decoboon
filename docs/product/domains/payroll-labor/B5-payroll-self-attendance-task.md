# PAY-SELF-ATTENDANCE · 员工本人月度考勤跨端查看

## 状态

- 批次：`PAY-SELF-ATTENDANCE`
- 变更级别：L3（薪酬数据权限、员工自助）
- 业务依据：`BR-038`、`DEC-017`
- 编码授权：用户 2026-10-30 指令“授权 下一步”
- DDL/迁移：无
- 当前状态：`DEV_VERIFIED / PENDING_REAL_UI_ACCEPTANCE`

## 目标

补齐全员本人月度考勤查看能力：员工在 App/Web 通过同一 Provider 读取本人考勤；服务端强制 Self 范围，不信任前端 `empId/storeId`；管理角色仍保留原有企业/门店范围。

## 垂直切片范围

### 后端

- `attendance/summary/page`：
  - BOSS、Finance、StoreManager 保持管理视角；
  - 其他内部员工始终只返回本人记录；
  - 增加 `selfOnly` 显式本人视角，供管理角色“我的考勤”使用；
  - `selfOnly=true` 或非管理角色时忽略客户端 `storeId/empId`，只按登录员工过滤。
- `attendance/summary/info`：
  - 管理角色继续按企业/门店范围读取；
  - 非管理员工通过统一 DataScope Specification 仅能读取本人记录，他人或跨企业返回“不存在或无权访问”。

### App

- 新增 `my-attendance` 本人月度考勤页；
- “我的工资”组新增“我的考勤”入口；
- 使用 `attendance/summary/page { selfOnly: true, month }`，不提交员工 ID 或门店 ID；
- 展示应出勤、正常出勤、事假、病假、旷工、外勤、年假、调休、状态和确认信息；
- 接口失败显示“暂不可确认”并提供重试。

### Web

- 新增 `/account/attendance` 本人月度考勤页；
- 账户菜单新增“我的考勤”；
- 使用同一 `attendance/summary/page { selfOnly: true, month }`；
- 支持月份切换、刷新、错误恢复和详情回读；
- 不提供编辑、确认、删除或他人数据入口。

## 权限与数据范围

| 角色 | 管理视角 | 本人视角 |
| --- | --- | --- |
| BOSS | 企业范围 | 仅本人 |
| 总部 Finance | 企业范围 | 仅本人 |
| 门店 Finance / StoreManager | 本店 | 仅本人 |
| Designer / Marketer / Pm 等员工 | 不允许 | 仅本人 |
| TeamLeader | 按现有应用访问门禁处理，不扩大权限 | 不扩大 |

服务端规则高于 `selfOnly`、`empId`、`storeId` 等客户端参数。任何员工访问他人考勤必须失败且不泄露记录是否存在。

## 验证

- 后端定向：员工本人 page/info 只返回本人；管理角色原范围不退化；管理角色 `selfOnly=true` 只返回本人；跨员工详情 404。
- Java 22 全量测试。
- App：payroll contract、type-check、client-copy/UI 门禁、`build:mp-weixin`。
- Web：type-check、production build、现有权限/门店/工作台门禁。
- 真实页面证据：待受控企业账号补录，不冒充独立验收。

## 不做

- 不开放员工编辑或确认本人考勤；
- 不开放员工查看提成；
- 不修改店长敏感字段边界；
- 不修改工资公式、工资单、应付、发放、作废；
- 不新增业务状态或能力语义；
- 不做 DDL、迁移、生产数据修复；
- 不正式发布小程序。
