# Product Planning Worklog

## Current

`REQ-20260915-WEB-PLAN` 已完成规划交付，状态为 `draft_for_review`。规划基于当前小程序角色/62 页路由/权限矩阵、后端 192 条 Provider、67 个实体、72 项后端测试和小程序构建结果；当前 `th4-decoboon-web` 仍为空目录，尚未开始 Web 实现。

## Outputs

| title | path | audience | status |
| --- | --- | --- | --- |
| 装得清上线前业务功能与开发范围规划 | `docs/plan/2026-09-15-上线前业务功能规划.md` | user / development / acceptance | draft_for_review |
| 装得清 Web 端建设规划 | `docs/plan/2026-09-15-装得清Web端建设规划.md` | user / development / acceptance | draft_for_review |

## Proposed Shared Outputs

以下为建议登记到 `_系统/协作/shared.md` 的内容；因 shared 文件不在本 lane 的主动 `write_scope`，本次未直接修改：

| from | title | path | audience | status | updated |
| --- | --- | --- | --- | --- | --- |
| product-planning | 装得清上线前业务功能与开发范围规划 | `docs/plan/2026-09-15-上线前业务功能规划.md` | user / development / acceptance | draft_for_review | 2026-09-15 |
| product-planning | 装得清 Web 端建设规划 | `docs/plan/2026-09-15-装得清Web端建设规划.md` | user / development / acceptance | draft_for_review | 2026-09-15 |

## Requests

| to | request | status | link |
| --- | --- | --- | --- |
| user | 确认首发模式（种子试用/收费）、首发渠道（仅小程序/含 PC）和业主模型 | waiting_user_decision | `docs/plan/2026-09-15-上线前业务功能规划.md` §7 |
| user | 确认 Web 是否仅服务员工、初始密码/短信验证码方案、各角色 Web 编辑边界 | waiting_user_decision | `docs/plan/2026-09-15-装得清Web端建设规划.md` §12.2 |

## Verification

- 读取 `AGENTS.md`、`.starwork/**`、当前项目/工作、agent lanes、shared、相关源码与设计文档。
- 当前后端统计：683 个主 Java 文件、192 条 Provider 路由、67 个 `@Entity`、25 个测试类。
- 当前后端测试：`mvn -o -q test` 为 72 tests、0 failures、0 errors、0 skipped。
- 当前小程序：62 个页面（主包 12、分包 50）；`npm run verify:mp-weixin` 通过；主包 378,198 B、最大分包 154,634 B、总包 1,103,656 B。
- 当前 Web：`th4-decoboon-web` 为空目录，文件数 0；本规划未创建 Web、后端或小程序代码。

## Key Findings

- 小程序当前有效角色是 8 个员工角色 + 业主；旧 `docs/04` 中的 supervisor/purchaser/warehouse 已不是当前源码角色。
- Web 应定位为员工桌面管理台，业主首版不进入 Web。
- 后端已有 192 条 Provider，绝大多数 Web 业务 API 可复用；采购、到场、供应链等移动端未完整接入的 Provider 可由 Web 新接入。
- 当前 `Credentials` 只有 `Wx`，没有密码字段、失败次数、锁定时间、首次设置或 Web 登录 Provider。
- 当前没有 `auth/session/logout`、密码修改/找回/重置 Provider。
- 当前没有 `@Role/@View`，Web 菜单无法由后端注册下发。
- 当前没有 `components/sms` / `Sender` / `Validator`，初始密码和重置密码链路缺少验证码基础设施。
- th4 框架可完成登录签名，但当前签名密钥、Token 过期和多实例注销策略不满足直接生产上线的安全要求。

## Next

1. 等待用户确认 Web 角色范围、初始密码/短信方案和首版模块边界。
2. 确认后先冻结 Web API 契约、`@Role/@View` 路由和密码数据迁移，再交接 development lane。
3. acceptance lane 独立设计登录锁定、菜单权限、路由守卫、401/403、四角色数据隔离和桌面端 E2E。
