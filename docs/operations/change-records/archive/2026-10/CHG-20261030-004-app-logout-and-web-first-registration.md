# CHG-20261030-004 · App 退出登录同源与 Web-first 备案

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261030-004 |
| 类型 | 跨端契约治理 / 会话生命周期 |
| 风险级别 | L2 |
| 状态 | 开发自测通过，待用户验收 |
| 负责人 | codex |
| 日期 | 2026-10-30 |
| 关联裁决/缺口 | `CHG-20261030-003`、`GAP-WEB-024`、`GAP-WEB-025`、`GAP-WEB-026`、`SCN-016`、`CAP-004` |

## 2. 目标与范围

- 目标：完成两个小闭环：
  1. App 主动退出登录复用唯一 `auth/session/logout` Provider；
  2. Web 设计分项排序和负责人风险处置完成 Web-first 备案，房屋删除保持显式阻塞。
- 范围内：App 会话与 HTTP 授权失败解耦；身份 API 退出调用；Web GAP 台账和 Web 优先能力清单；Provider 评审状态。
- 非范围：改变后端 logout 实现；改变风险状态机或权限；实现房屋删除软删/归档；设计模板 App 双端补齐。
- 受影响端：App；Web 规范文档；后端 Provider 评审基线。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：涉及会话生命周期和风险处置能力备案；不修改风险状态机、权限或金额。
- 业务依据：
  - `SCN-016`、`CAP-004`：负责人风险队列沉浸式处置；
  - `GAP-WEB-024`：设计分项排序仅改变展示顺序；
  - `GAP-WEB-025`：房屋删除当前物理删除，删除/归档语义未确认，保持 OPEN；
  - `GAP-WEB-026`：风险写能力 Web-first 备案关闭；
  - `WEB-建设核心思路 A2`：Web 优先能力四条件。
- 尚存 GAP：
  - `GAP-WEB-025`：房屋删除物理删除/软删归档语义待用户确认；
  - 设计模板 App 双端能力仍待补齐。

## 4. 验收条件

| 序号 | 场景 | 预期结果 | 证据 |
| --- | --- | --- | --- |
| 1 | App 主动退出 | 本地会话立即清空，并使用退出前 token 调用 `auth/session/logout` | App 代码检索 / 类型检查 |
| 2 | App 远程退出失败 | 本地仍退出，不把用户锁在旧会话 | 代码路径与构建 |
| 3 | HTTP 401 | 通过注入的 `setUnauthorizedHandler(clearSession)` 清会话，不反向依赖 Session 领域 | App 代码检索 |
| 4 | Web 分项排序 | `design/items/reorder` 标为已批准 Web-first，仅改变展示顺序 | `GAP-WEB-024` |
| 5 | 风险处置 | 5 个风险写动作标为已批准 Web-first，依据 SCN-016/CAP-004 | `GAP-WEB-026` |
| 6 | 房屋删除 | 不误标为已批准；保持 `WEB_FIRST_PENDING_REGISTRATION` | `GAP-WEB-025 OPEN` |
| 7 | 契约巡检 | 0 未匹配路由；23 项评审覆盖；11 项已关闭、12 项 open debt；`ROUTE_MISMATCH=0` | `--check` |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | `dev` | 未提交 | Provider 基线、评审清单、说明、CHG |
| th4-decoboon-app | `dev` | 未提交 | `src/core/http.ts`、`src/domains/identity/api.ts`、`src/domains/identity/session.ts`、`src/App.vue` 及退出调用页 |
| th4-decoboon-web | `dev` | 未提交 | `docs/standards/GAP台账.md`、`docs/standards/WEB-建设核心思路.md` |

## 6. 验证

| 检查 | 命令/页面 | 结果 |
| --- | --- | --- |
| App 类型检查 | `npm run type-check` | PASS |
| App 发布构建 | `npm run build:mp-weixin:release` | PASS；主包 1,072,949 B、最大分包 734,997 B、总包 4,158,376 B |
| App 定向门禁 | `check:design-items`、`check:ui`、`check:navigation`、`check:pagination`、`check:client-copy` | PASS |
| Web 规范检查 | `npm run check:actions`、`check:permissions`、`check:states`、`check:workbenches`、`type-check` | PASS |
| Provider 巡检 | `python3 scripts/audit-frontend-provider-contract.py --self-test`、`--check` | PASS；23 项评审覆盖 |
| 严格模式 | `python3 scripts/audit-frontend-provider-contract.py --check --strict-web-only` | 按预期返回 1，剩余 12 项 open debt |

- 未执行项及原因：没有真实设备/生产后端会话完成主动退出写入冒烟；需用户本地真实账号验证。
- 已知失败及是否与本次相关：房屋删除仍被 GAP-WEB-025 阻塞，不是本次回归失败。

## 7. 发布

| 字段 | 内容 |
| --- | --- |
| Release ID | 不适用 |
| 制品路径/SHA-256 | 不适用 |
| 生产 release/版本 | 未发布 |
| 上一版本/回滚点 | 当前工作区 |
| 健康检查 | 不适用 |
| 发布后观察 | 不适用 |

- 数据库迁移：无
- 配置/密钥变化：无

## 8. 回滚

- 回滚条件：App 主动退出现在无法清理本地会话或退出请求造成登录态异常；Web 规范备案被错误解释为风险/删除权限扩大。
- 回滚步骤：恢复 App 身份 API/Session/HTTP/页面；恢复 Web GAP 与 Web 优先能力清单；恢复 Provider 评审清单。
- 回滚验证：App 类型检查/构建通过；401 仍能清会话并回登录页。

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集
- 人工耗时：未采集
- 一次通过：否；首次拆解 HTTP/Session 依赖后需重新验证循环依赖和类型检查。
- 返工原因：HTTP 层原直接依赖 Session，改为注入 Unauthorized handler 以允许 Session 使用身份 API。
- 最终结论：App logout 已同源；风险处置与分项排序 Web-first 已备案；房屋删除因物理/软删语义未定保持阻塞。

## 10. 复盘与沉淀

- 是否出现重复问题：退出登录只清本地、Web-only 能力未备案，属于跨端治理常见遗漏。
- 是否需要新增门禁：已有 Provider 巡检；后续将 `--strict-web-only` 接入 release preflight，但当前 12 项 open debt 尚未归零。
- 候选 `EXP/ANT/PATH`：
  - `PATH`：先拆 HTTP 与 Session 依赖，再让 Session 复用身份 API；
  - `ANT`：物理删除未确认却先登记为 Web-first；
  - `EXP`：Web-first 备案必须同时写能力边界、App 兼容方式和后端唯一 Provider。
- 后续动作与负责人：用户确认房屋删除软删/归档语义；验收 App 真实退出登录；负责人：用户 + codex。
