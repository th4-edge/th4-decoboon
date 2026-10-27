# CHG-20261027-003 · 邀请业务线生产闭环修复

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261027-003 |
| 类型 | 线上缺陷 / 权限与组织主链修复 |
| 风险级别 | L3（员工身份、邀请与权限） |
| 状态 | 待发布 |
| 负责人 | Codex |
| 日期 | 2026-10-27 |
| 关联需求/事故/业务裁决 | `EMG-V2.2`、`DEC-081`、`DEC-082`、`DEC-085`、`INC-design-editable-projection-regression` |

## 2. 目标与范围

- 目标：修复邀请取消、邀请管理主体注入和待接受邀请重发链路，使被邀请员工能用正确手机号完成加入并获得正确工作台。
- 范围内：后端 `invite/invitation/page|cancel`、App 邀请管理与接受页面、成员/邀请运行态验证。
- 非范围：新增角色、解除 TeamLeader 冻结、开放跨租户、金额/成本/工资语义、DDL 或生产数据修改。
- 受影响端：后端、App；Web 复用同一 Provider 并做回归。

## 3. 业务与决策

- 业务事实来源：`DEC-081`、`DEC-082`、`DEC-085`；企业管理域冻结契约。
- 不变量：BOSS 全企业；店长本店且不能邀请店长/TeamLeader；邀请接受必须校验手机号；取消仅 BOSS/邀请发起店长；无员工凭证不能进入企业管理；过期/取消的邀请不能接受。
- 尚存 GAP：无。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | 新员工接受邀请 | Pending 邀请，微信凭证无员工 | 创建员工、绑定凭证、进入对应角色工作台 | 后端测试 + App 运行态 |
| 2 | BOSS 重发待接受邀请 | 同企业同手机号 Pending | 返回原邀请并可再次分享，不新建重复记录 | `InviteIdempotencyTest` + App |
| 3 | 店长/ BOSS 取消邀请 | Pending，符合条件的操作人 | 邀请变为 Cancelled，席位释放 | Provider 回归 + DB |
| 4 | 无员工凭证访问邀请管理 | Credentials 无 Employee | 返回业务 403，不出现类型转换或空公司异常 | `InvitationProviderPrincipalTest` |
| 5 | 过期邀请 | Pending 过期 | 自动 Expired，不进入待接受列表、不可取消 | `InvitePageExpiryTest` |
| 6 | 跨店邀请隔离 | 店长 A 邀请 B 店 | 403/404，不泄露对象 | 既有独立验收 + 回归 |
| 7 | TeamLeader 冻结 | 任意邀请入口 | 不可邀请、不可接受、不可进入 | `TeamLeaderOrganizationRulesTest` |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | `hotfix/design-editable-prod-20261027` | `本轮提交` | `invite/invitation/page|cancel`、发布契约 |
| th4-decoboon-app | `codex/invite-flow-release` | `006d22f` | 邀请管理、待接受列表、分享/取消、未归属身份页（1.0.72） |
| th4-decoboon-web | `hotfix/invitation-production-closure` | `d38e553` | Web 邀请创建、待接受列表、取消 |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| 定向后端测试 | `InvitationProviderPrincipalTest` 等 5 类 | PASS | Maven surefire |
| 全量后端测试 | `mvn -o -q test` | 245 套件 / 949 用例 / 0 失败 | Maven surefire |
| App 契约/类型/构建 | `check:invite-flow`、`type-check`、`build:mp-weixin:release`、`check:subpackages` | PASS | App 1.0.72 |
| Web 门禁/构建 | `type-check`、`check:permissions`、`check:workbenches`、`build` | PASS | Web `d38e553` |
| 真实页面运行态 | App 邀请页待接受列表/分享/取消确认 | PASS（1.0.72 发布记录） | `docs/71-微信小程序发布记录-2026-10-27-1.0.72.md` |
| 发布后生产冒烟 |  | 待发布后执行 |  |

- 未执行项及原因：实施中。
- 已知失败及是否与本次相关：生产已出现取消邀请 NPE 36 次、邀请页主体注入异常 12 次。

## 7. 发布

| 字段 | 内容 |
| --- | --- |
| Release ID | 待定 |
| 制品路径/SHA-256 | 待定 |
| 生产 release/版本 | 待定 |
| 上一版本/回滚点 | `/opt/decoboon/releases/20261027-103227` |
| 健康检查 | 待定 |
| 发布后观察 | 待定 |

- 数据库迁移：无
- 配置/密钥变化：无

## 8. 回滚

- 回滚条件：邀请接受/取消/重发任一主链失败，或出现越权。
- 回滚命令/步骤：切回上一后端 release；App 体验版回退到 1.0.67。
- 回滚验证：BOSS/店长邀请页可读，既有员工不受影响。
- 实际是否回滚：否

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集
- 人工耗时：进行中
- 一次通过：待定
- 返工原因：生产分支未包含已完成邀请取消修复
- 最终结论：处理中

## 10. 复盘与沉淀

- 是否出现重复问题：是（分支漂移导致已验收修复未上线）
- 是否需要新增门禁：是；发布契约增加邀请取消与邀请分页关键标记
- 候选 `EXP/ANT/PATH`：待收口
- 后续动作与负责人：完成后端全量测试、App 1.0.72 构建验证、生产发布和真实邀请复验
