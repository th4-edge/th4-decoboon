# EMG-V1 · Team 冻结收口研发任务

> 文件名保留 `EMG-V1-Team生命周期研发任务.md` 仅为任务链路稳定；正文已按用户 2026-10-22“Team 冻结”裁决覆盖。

| 项 | 内容 |
| --- | --- |
| 研发任务 | `DEV-PI-20261022-005 / EMG-V1` |
| 关联问题 | `PI-20261022-005` |
| 业务裁决 | `DEC-085` / `EV-353` / `T12-BIZ-20261022-006` |
| 当前状态 | `ACCEPTED` |
| 变更级别 | L2（入口、权限与写入校验；无 DDL） |
| 影响端 | 后端、App、Web |

## 1. 冻结口径

- Team 与 TeamLeader 一并冻结；
- Team 只保留历史表、字段和快照；
- 不新建/修改/归档 Team；
- 新邀请和新员工不分配 Team；
- 员工岗位/门店调整不写 Team；
- App/Web 移除 Team 管理、筛选和经营入口；
- 历史 Team 字段可按最小白名单只读解释事实；
- 不删除、不清空、不迁移、不猜测回填历史 Team。

## 2. 后端范围

### 2.1 邀请

- `invite/invitation/save` 保持参数兼容，但 `teamId/teamName` 非空时必须拒绝。
- `MembershipContext.createInvite` 不再解析或创建 Team，新邀请 `team=null`。
- 接受邀请不产生新的 Team 归属；历史邀请 Team 字段仅保留历史。
- 邀请分页/详情可保留历史 Team 只读字段，但不得用于筛选或授权。

### 2.2 员工更新

- `company/member/update` 收到 `teamId` 时拒绝。
- 调岗、调店、停用、恢复、离职、交接均不写 Team。
- `company/member/page|info` 可继续返回历史 Team 摘要；不得开放 Team 写入口。

### 2.3 团队成员/经营接口

- 不新增 `user/team/save|update|status`。
- `user/team/list` 如保留，仅作为历史只读兼容，不得被 App/Web 新入口调用。
- `statistic/team-leader/report` 不再作为任何角色页面数据源；TeamLeader 继续不可登录。
- 角色门禁中移除 TeamLeader 的有效分支或保持 403；不得恢复 Team 数据范围。

### 2.4 权限与审计

- 店长不再获得 `view:team` 等 Team 入口能力。
- Team 写入拒绝必须返回明确 400/403。
- 不需要新增 Team 审计动作；员工组织变更继续写成员日志。
- 不新增 DDL、迁移或数据清理脚本。

## 3. App 范围

| 文件 | 变更 |
| --- | --- |
| `src/roles/catalog.ts` | 移除店长“门店团队”快捷入口 |
| `src/roles/permission.ts` | 移除店长的 `view:team` 能力；TeamLeader 保持空能力 |
| `src/subpackages/company/pages/invite.vue` | 移除 Team 加载、选择、`teamId` 提交 |
| `src/domains/identity/api.ts` | 创建邀请保留兼容参数，但页面不再传 Team |
| `src/composables/teamContext.ts` | 不再允许 TeamLeader 进入团队上下文 |
| 团队成员/工作台页面 | 保留历史代码不作为入口；不得被角色能力、路由或深链接放开 |

历史员工详情、邀请详情和账户信息中的 Team 字段可以只读显示；新增/编辑表单不得出现 Team。

## 4. Web 范围

| 文件 | 变更 |
| --- | --- |
| `src/views/workbench/BossOrganizationView.vue` | 邀请角色移除 TeamLeader；不再提供 Team 维护 |
| `src/roles/routes.ts` | Boss 路由移除 `team-overview`、`team-customers`；TeamLeader 仍仅共享路由 |
| `src/roles/workbenchConfig.ts` | 移除“团队经营概览”菜单 |
| `src/roles/workbenchHome.ts` | 移除 Boss 团队经营入口场景 |
| `src/components/picker/EmployeePicker.vue` | 移除 Team 筛选控件；历史 Team 可只读展示 |
| `src/domains/team/*` | 不再被任何路由引用；可保留类型兼容或后续清理 |

## 5. 历史兼容

- 历史 Team/TeamLeader 关系不删除；
- 历史员工、客户、项目、任务、通知和报表中的 Team 字段可只读展示；
- 历史 Team 不再出现在新建、编辑、筛选、经营入口或权限判断中；
- 旧路由/深链接必须由角色路由或服务端拒绝，不能通过前端缓存进入；
- 生产 TeamLeader/Team 当前均为 0，但不得删除任何历史数据。

## 6. 测试

### 后端

- 邀请携带 `teamId/teamName` → 400；
- 成员更新携带 `teamId` → 400；
- 新邀请不产生 Team；
- 接受邀请不产生新 Team 归属；
- TeamLeader 邀请/改岗/登录/授权 → 403；
- 店长不再具有 Team 入口能力，跨店请求仍 403/404；
- 历史 Team 字段仍可按白名单读取；
- 不产生 Team 写审计或 DDL 变更。

### App/Web

- `type-check`、目标构建；
- 店长快捷入口无“门店团队”；
- 邀请页无 Team 选择、请求无 `teamId`；
- Web 无 TeamLeader 邀请选项；
- Web Boss 无团队概览/团队客户菜单、路由和首页入口；
- 历史 Team 只读字段不报错；
- 旧深链接无法进入 Team 管理/经营页面。

### 回归

- 员工邀请、岗位调整、财务调店；
- 店长本店权限与跨店拒绝；
- TeamLeader 冻结；
- 登录/会话/订阅过滤器；
- App/Web 构建与真实页面走查。

## 7. 回滚

- 回退 App/Web 入口清理和后端 Team 写入拒绝；
- 不恢复 TeamLeader；
- 不恢复 Team 管理入口；
- 不改历史 Team 数据；
- 回滚后仍需保持 TeamLeader 不可登录、不可邀请、不可授权。

## 8. 独立验收最低用例

- `ACC-EMG-01`：Team 入口不存在且服务端写入拒绝，历史 Team 只读；
- `ACC-EMG-02`：TeamLeader 历史兼容与拒绝路径；
- `ACC-EMG-09`：跨企业/跨门店/伪造 teamId 拒绝；
- `ACC-EMG-12`：App/Web 无 Team 入口且历史事实一致。
