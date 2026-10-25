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

## 未映射

- 本批无 `UNMAPPED` 或 `STALE`；`EMG-V2-DEF-01`、`EMG-V2-DEF-02` 均已关闭。

## 已过期

- 本批无 `STALE`。

## 未决业务问题

- 本批无新增 P0；`GAP-016` 属后续 `EMG-V4`。

## 本批结论

- 已对齐：V1 Team 冻结与 V2 员工生命周期 18 个 P0 用例独立 PASS；App/Web/API/SQL 事实一致。
- 缺陷关闭：`EMG-V2-DEF-01`、`EMG-V2-DEF-02` 已独立复验关闭。
- 下一批：准备 `EMG-V3 组织影响与交接`；问题整体仍不得关闭。
