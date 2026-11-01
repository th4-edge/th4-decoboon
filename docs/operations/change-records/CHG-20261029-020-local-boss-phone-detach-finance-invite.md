# CHG-20261029-020 · 本地 Boss 手机号迁移与 186 财务邀请测试

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | `CHG-20261029-020` |
| 类型 | 本地测试数据 / 权限身份 |
| 风险级别 | L3 |
| 状态 | `ACCEPTED_DEV_VERIFIED` |
| 负责人 | codex |
| 日期 | 2026-10-29 |
| 关联需求/事故/业务裁决 | 用户要求保留 Boss 用户并用 18660219918 测试财务受邀注册 |

## 2. 目标与范围

- 目标：释放真实微信身份 `18660219918`，保留原 Boss 账号并迁到本地测试手机号，随后邀请 `18660219918` 为 Finance。
- 范围内：本地 `decoboon` 库的员工手机号、唯一微信凭证、加入限制手机号；通过既有 Web Provider 创建财务邀请。
- 非范围：生产库、业务代码、权限规则、正式发布。
- 受影响端：本地后端、App/微信开发者工具、Web。

## 3. 业务与决策

- 同一手机号不能在当前企业拥有两个有效员工，原 Boss 必须先迁到独立测试手机号。
- 原 Boss 凭证改为 Password 类型，手机号和 idKey 均改为 `19900009918`，从而释放真实微信 openId/unionId。
- 原 Boss 保留 password hash，可继续通过 Web 使用新手机号和原密码登录。
- Finance 邀请只通过现有 `invite/invitation/save` Provider 创建，不手工伪造“已接受”状态。
- 测试手机号：`19900009918`。

## 4. 验收条件

| 序号 | 场景 | 预期结果 | 结果 |
| --- | --- | --- | --- |
| 1 | 改库后 Boss | 员工手机号和凭证手机号为 `19900009918`，角色仍 Boss Active | PASS |
| 2 | 原微信身份 | `18660219918` 和原真实 openId 不再绑定旧 Boss | PASS |
| 3 | Boss 登录 | `19900009918` + 原密码可登录 Web Boss | PASS |
| 4 | Finance 邀请 | `18660219918` 存在 Pending Finance 邀请，归属总店 | PASS |
| 5 | Web 邀请列表 | 成员与组织页显示待接受 Finance 邀请 | PASS |
| 6 | 微信测试 | 微信开发者工具用 186 授权后可接受邀请并生成 Finance 员工 | PASS，19:15:27 已生成 Finance 员工 |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | `dev` | 未提交 | 本地数据库测试数据，无代码提交 |
| th4-decoboon-web | `dev` | 未提交 | `src/domains/identity/api.ts` 邀请枚举机器值修复，见 CHG-021 |

## 6. 验证

| 检查 | 结果 |
| --- | --- |
| 相关表备份 | PASS，`~/.decoboon-local/20261029-finance-invite-186/before.sql` |
| Boss 密码登录 | PASS，`19900009918` / 原密码 |
| 微信 openId 释放 | PASS，旧 phone/openId 凭证数为 0 |
| Finance 邀请创建 | PASS，inviteId `1793271807543993360` |
| 公开邀请详情 | PASS，Finance / 总店 / Pending |
| Web 待接受列表 | PASS |
| 微信接受邀请 | PENDING_USER_ACTION |

- 未执行项及原因：微信手机号授权必须由用户在微信开发者工具交互完成。
- 已知失败及是否与本次相关：无

## 7. 发布

- 不发布；仅本地测试库。

## 8. 回滚

- 备份：`~/.decoboon-local/20261029-finance-invite-186/before.sql`（权限 600）。
- 未接受邀请快速回滚：`~/.decoboon-local/20261029-finance-invite-186/rollback-before-accept.sql`。
- 已接受邀请：停止写入，使用完整备份恢复员工、凭证、邀请、加入限制、成员日志和安全日志。
- 实际是否回滚：否

## 9. 成本与结论

- 最终结论：186 已完成受邀注册并成为 Finance。随后发现 App 本地会话未升级，已由 CHG-20261029-022 修复会话恢复。

## 10. 复盘与沉淀

- 该方案可在只有一个真实手机号时，把 Boss Web 登录与 186 微信测试身份分离。
- 该方案只适合本地测试；生产环境禁止直接改凭证 openId/phone。
