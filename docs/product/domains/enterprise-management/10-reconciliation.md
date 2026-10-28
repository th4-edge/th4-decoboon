# 企业管理 · 设计—蓝图—实现—页面—验收对账

状态：`ACCEPTED`（18/18 P0 PASS；2/2 follow-up defects CLOSED）

| REQ | 场景 | 业务规则 | 蓝图节点 | 字段/API | 页面 | 测试 | 验收 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-EMG-01 | 店长本店邀请 | 店长仅本店；不能邀请店长/TeamLeader | BP-EMG-INVITE-SCOPE / BP-EMG-TEAM-FROZEN | `invite/invitation/save`、`storeId` | App/Web 邀请页 | 独立 HTTP + Web/App 运行态 | `ACC-EMG-V2-02/03/04/18` | ACCEPTED |
| REQ-EMG-01 | BOSS 邀请财务 | BOSS 必须选择企业内门店 | BP-EMG-INVITE-SCOPE | `createInvite`、`storeId`、`StoreAttributionContext` | App/Web 邀请页 | 独立财务邀请探针 | `ACC-EMG-V2-01` | ACCEPTED |
| REQ-EMG-01 | 邀请取消 | BOSS/邀请发起店长可取消 Pending 邀请 | BP-EMG-INVITE-SCOPE | `invite/invitation/cancel` | App/Web 邀请管理 | 独立店长本人取消 + BOSS 代取消；席位/Pending 回落 | `EMG-V2-DEF-01` | CLOSED |
| REQ-EMG-02 | BOSS 跨店调岗调店 | BOSS 全企业；历史门店不回溯 | BP-EMG-MEMBER-ADJUST | `company/member/update` | App/Web 成员页 | 独立 HTTP + 成员日志 SQL | `ACC-EMG-V2-05` | ACCEPTED |
| REQ-EMG-02 | 店长本店调岗 | 店长本店、非 BOSS/非店长/非自己 | BP-EMG-MEMBER-ADJUST | `company/member/update` | App/Web 成员页 | 独立 HTTP + 页面无越权入口 | `ACC-EMG-V2-06/09` | ACCEPTED |
| REQ-EMG-02 | 店长跨店/提升店长 | 跨店 403；仅 BOSS 设置店长 | BP-EMG-MEMBER-ADJUST | 服务端范围与角色校验 | App/Web 隐藏无权入口 | 独立跨店/提升店长探针 | `ACC-EMG-V2-07/08` | ACCEPTED |
| REQ-EMG-03 | 停用/恢复 | 停用撤销访问；恢复校验席位/Active 门店 | BP-EMG-LIFECYCLE-ACTIONS | `company/member/suspend|resume` | App/Web 成员页 | 独立登录 403 + 停用门店恢复 400 | `ACC-EMG-V2-10/11/17` | ACCEPTED |
| REQ-EMG-03 | 离职/强制离职 | 离职终态、原因落库、未交接二次确认 | BP-EMG-LIFECYCLE-ACTIONS | `company/member/resign` | App/Web 成员页 | 在手客户 409 + 强制离职 + 历史归属 SQL | `ACC-EMG-V2-12/13/17` | ACCEPTED |
| REQ-EMG-05 | 管理员代办离职 | 操作人为实际管理员 | BP-EMG-AUDIT | `CompanyMemberLog.operator` | 成员审计事实 | 独立 HTTP + 审计 SQL | `ACC-EMG-V2-14` | ACCEPTED |
| REQ-EMG-06 | Team 冻结回归 | 无邀请/改岗选项；写参数拒绝；TeamLeader 403 | BP-EMG-TEAM-FROZEN | Team 参数拒绝、角色策略 | App/Web/后端 | 独立 Team 路由/邀请/登录探针 | `ACC-EMG-V2-16` | ACCEPTED |
| REQ-EMG-04 | 双端页面与异常 | 双端完整；无权限入口隐藏；错误明确 | BP-EMG-APP-SURFACE / BP-EMG-WEB-SURFACE | App/Web 页面与 API | App/Web 成员、详情、邀请 | 独立 App/Web 同源核对；店长组织页无 403 告警 | `ACC-EMG-V2-18` / `EMG-V2-DEF-02` | ACCEPTED |
| EMG-V2.1 | 重复邀请幂等 | 同企业同手机号未过期 Pending 返回原邀请 | BP-EMG-INVITE-SCOPE | `createInvite`、`reusedPending` | App/Web 邀请页 | `FinanceOrganizationRulesTest` | 待独立 HTTP / 运行态验收 | DEV_VERIFIED |
| EMG-V2.1 | Pending 邀请重发与取消 | 创建后可再次分享、取消后重建 | BP-EMG-INVITE-SCOPE | `invite/invitation/page|cancel` | App 邀请页 / Web 组织页 | App contract + Web type-check | 待独立运行态验收 | DEV_VERIFIED |
| EMG-V2.1 | 员工端无邀请编号概念 | 邀请页与未归属员工页不展示、不手输编号 | BP-EMG-APP-SURFACE | 微信分享卡片 + 手机号一致 | App 邀请页 / 身份选择页 | `check:invite-flow` | 待真机与独立验收 | DEV_VERIFIED |

## 未映射

- 本批无 `UNMAPPED` 或 `STALE`；`EMG-V2-DEF-01`、`EMG-V2-DEF-02` 均已关闭。

## 已过期

- 本批无 `STALE`。

## 未决业务问题

- 本批无新增 P0；`GAP-016` 属后续 `EMG-V4`。

## 本批结论

- EMG-V2.1 邀请幂等、Pending 重发/取消、无邀请编号概念已完成开发验证，状态 `DEV_VERIFIED`，不得据此宣称独立验收通过。
- 已对齐：V1 Team 冻结与 V2 员工生命周期 18 个 P0 用例独立 PASS；App/Web/API/SQL 事实一致。
- 缺陷关闭：`EMG-V2-DEF-01`、`EMG-V2-DEF-02` 已独立复验关闭。
- 下一批：准备 `EMG-V3 组织影响与交接`；问题整体仍不得关闭。

## EMG-V2.2 邀请生产闭环补充（2026-10-27）

| REQ | 场景 | 业务规则 | 蓝图节点 | 字段/API | 页面 | 测试 | 验收 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| EMG-V2.2 | BOSS取消邀请 | 先加载完整员工，再校验企业/角色/发起人 | BP-EMG-INVITE-SCOPE | `invite/invitation/cancel` | App/Web 邀请管理 | `InvitationProviderPrincipalTest` | 开发定向 PASS，待生产冒烟 | DEV_VERIFIED |
| EMG-V2.2 | 无员工凭证访问管理接口 | 返回业务403，不允许类型转换/空公司异常 | BP-EMG-INVITE-SCOPE | `@User User<?>` | App/Web | 注入边界 + 业务错误测试 | 开发定向 PASS | DEV_VERIFIED |
| EMG-V2.2 | 待接受邀请重发/取消 | App可重发，Web可核对/取消，均复用同一Provider | BP-EMG-INVITE-SCOPE | `invite/invitation/page|save|cancel` | App 1.0.72 / Web 生产基线 | App contract/build、Web type/build | 待发布后真实页面复核 | DEV_VERIFIED |
| EMG-V2.2 | 邀请接受后岗位工作台 | 手机号一致后创建员工、绑定凭证、进入对应角色 | BP-EMG-EMPLOYEE-STATE | `invite/invitation/accept` | App 邀请接受页 | 既有18/18独立验收 + 生产待验证 | 待真实被邀请人复验 | PENDING_REAL_USER |

## EMG-V2.2 对账结论

- 本批没有新增角色、Team、跨租户或金额语义。
- App 邀请页和 Web 邀请管理均消费同一后端 Provider。
- 生产真实被邀请人完成接受前，不得将“邀请接受后正确工作台”写成生产 PASS。
- App 全量门禁存在与本批无关的历史首页契约失败；本批专项、类型检查、生产构建、分包检查均 PASS。

## EMG-V2.2 发布后状态（2026-10-27）

- 后端与 Web 已发布，生产健康与制品契约 PASS。
- App 1.0.72 已上传，但仍在用户正式提审/发布边界之外。
- `15905600230` 邀请仍为 Pending，凭证无员工；真实接受与正确工作台复验为当前唯一未闭环业务事实。
- 对账状态：技术实现 `RELEASED`；真实用户接受 `PENDING_REAL_USER`。

## EMG-V2.3 取消邀请频控恢复（2026-10-27）

- 业务口径：`Cancelled` 邀请保留审计，但不再占用 7 天或 180 天邀请次数；`Pending/Accepted/Expired/Rejected` 仍按既有规则统计。
- 场景 `15905600230`：当前 3 条邀请均为 `Cancelled`，修复后不再被 7 天上限阻塞，BOSS 可立即重新创建一条 `Pending` 邀请。
- 回归：`cancelledInvitesDoNotConsumeReinviteQuota` PASS；后端全量 245 套件 / 950 用例 / 0 失败。
- 状态：`DEV_VERIFIED_PENDING_RELEASE`。

### EMG-V2.3 发布结果

- 后端已发布 `/opt/decoboon/releases/20261027-172000`，源码提交 `293c39b`。
- 生产 JAR 已确认 `Cancelled` 被排除出 7 天/180 天频控。
- `15905600230` 当前 3 条 Cancelled，按新口径计数为 `0`，可立即重新创建邀请。
- 下一步：麦鸿创建新业务员邀请并分享；钱亮完成手机号授权接受。
