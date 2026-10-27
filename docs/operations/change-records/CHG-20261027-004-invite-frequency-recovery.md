# CHG-20261027-004 · 邀请频控恢复口径修复

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261027-004 |
| 类型 | 线上缺陷 / 业务规则恢复 |
| 风险级别 | L3（邀请与员工身份准入） |
| 状态 | 待发布 |
| 负责人 | Codex |
| 日期 | 2026-10-27 |
| 关联需求/事故/业务裁决 | `EMG-V2.2`；用户确认修复取消后无法重新邀请问题 |

## 2. 目标与范围

- 目标：`Cancelled` 邀请保留审计，但不再占用 7 天和 180 天邀请次数，管理员取消错误邀请后可立即重新邀请。
- 范围内：`MembershipContext.validateInviteFrequency`、邀请幂等/频控回归。
- 非范围：取消物理删除、放开 Pending 复用、改变邀请有效期、解除 Rejected 7 天限制、跨店或角色权限变化。
- 受影响端：后端；App/Web 使用同一 Provider。

## 3. 业务与决策

- 业务事实来源：企业管理冻结契约；Cancelled/Expired 为终态并保留审计。
- 本次口径：取消是管理纠错动作，不应消耗实际邀请频控额度；`Pending / Accepted / Expired / Rejected` 仍计入既有窗口。
- 尚存 GAP：无。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | 同手机号已有 3 条 Cancelled | BOSS 重新邀请 | 允许创建新 Pending 邀请 | 新增单测 |
| 2 | 同手机号达到 Pending/其他有效状态上限 | BOSS 重新邀请 | 仍按原规则拒绝 | 既有幂等/频控测试 |
| 3 | 取消后审计 | 管理员取消 | 记录仍为 Cancelled，不物理删除 | 既有独立验收 |
| 4 | 生产当前数据 | 15905600230 有 3 条 Cancelled | 发布后 BOSS 可立即重新创建 | 发布后业务复核 |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | `hotfix/design-editable-prod-20261027` | 本轮提交 | `MembershipContext`、`InviteIdempotencyTest` |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| 定向测试 | `InviteIdempotencyTest` 等 | PASS | Maven surefire |
| 全量后端测试 | `mvn -o -q test` | 245 套件 / 950 用例 / 0 失败 | Maven surefire |
| 标准构建 | `build-release.sh` | 待执行发布构建 |  |
| 生产重新邀请 | Web/App BOSS | 待执行 |  |

## 7. 发布

| 字段 | 内容 |
| --- | --- |
| Release ID | 待定 |
| 制品路径/SHA-256 | 待定 |
| 生产 release/版本 | 待定 |
| 上一版本/回滚点 | `/opt/decoboon/releases/20261027-161846` |
| 健康检查 | 待定 |
| 发布后观察 | 待定 |

## 8. 回滚

- 回滚条件：重新邀请出现越权、重复接受或频控绕过异常。
- 回滚命令：`bash scripts/rollback-prod.sh /opt/decoboon/releases/20261027-161846`
- 实际是否回滚：否

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集
- 人工耗时：进行中
- 一次通过：待定
- 最终结论：待发布
