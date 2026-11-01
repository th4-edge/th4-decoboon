# PAY-SELF-COMMISSION · 员工本人提成跨端查看

## 状态

- 批次：`PAY-SELF-COMMISSION`
- 变更级别：L3（薪酬数据权限、员工自助）
- 业务依据：`BR-038`、`DEC-017`
- 编码授权：用户 2026-10-30 指令“下一步”
- DDL/迁移：无
- 当前状态：`ACCEPTED / USER_ACCEPTED_NON_INDEPENDENT / PENDING_RELEASE`

## 目标

补齐员工本人查看本人提成的 App/Web 同源只读能力。两端复用 `payroll/commission/page|info`；服务端强制 Self 范围，不信任客户端 `empId/storeId`。

## 范围

### 后端

- `payroll/commission/page` 增加 `selfOnly` 本人视角；
- 非管理员工始终只返回本人提成；
- 管理角色传 `selfOnly=true` 时忽略门店参数并只返回登录员工本人；
- 管理角色原企业/门店范围保持不变；
- `payroll/commission/info` 使用统一 DataScope Specification，普通员工只能读取本人提成。

### App

- 新增 `my-commission` 只读页面；
- “我的工资”组新增“我的提成”入口，使用已有 `view:commission` 能力键；
- 展示项目、签约额、手工比例、提成金额、已计提、已释放、已发放、状态、备注和时间；
- 不提供计提、释放、调整、发放或他人入口。

### Web

- 新增 `/account/commissions`；
- 账户菜单新增“我的提成”；
- 使用同一 Provider 和 `selfOnly=true`；
- 支持分页、详情回读、错误重试和空态；
- 不提供管理动作。

## 权限与不变量

| 角色 | 管理视角 | 本人视角 |
| --- | --- | --- |
| BOSS / 总部 Finance | 企业范围 | 仅本人 |
| 门店 Finance / StoreManager | 本店 | 仅本人 |
| Designer / Marketer / Pm 等员工 | 不允许 | 仅本人 |
| TeamLeader | 保持现有应用访问门禁 | 不扩大 |

- 正式金额只读服务端字段，前端不计算、不改写提成公式。
- 普通员工读取他人提成必须返回“不存在或无权访问”。
- 不改变提成状态机、释放公式、发放流程和管理端动作。

## 验证

- 后端定向：本人 page/info、管理角色 selfOnly、管理范围回归、他人详情拒绝。
- Java 22 全量测试。
- App 薪酬契约、type-check、UI/文案/导航、小程序构建与分包检查。
- Web type-check、生产构建、权限/门店/工作台门禁。
- 真实页面证据待受控企业账号补录，不冒充独立验收。

## 不做

- 不开放员工计提、释放、调整或发放；
- 不修改提成公式、状态机、应付、工资单或工资条；
- 不处理店长敏感字段边界；
- 不新增业务字段或 DDL；
- 不正式发布小程序。
