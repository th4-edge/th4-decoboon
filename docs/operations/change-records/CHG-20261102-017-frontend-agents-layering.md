# CHG-20261102-017 · App/Web AGENTS 分层拆分

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261102-017 |
| 类型 | 重构 |
| 风险级别 | L0 |
| 状态 | 已关闭 |
| 负责人 | Codex |
| 日期 | 2026-11-02 |
| 关联需求/事故/业务裁决 | 用户要求归档后拆分 App/Web Agent 上下文 |

## 2. 目标与范围

- 目标：把 App/Web 过长的根 `AGENTS.md` 拆分为根级边界、源码实现、文档规则和脚本规则。
- 范围内：Web `AGENTS.md`、`src/AGENTS.md`、`docs/AGENTS.md`；App `AGENTS.md`、`src/AGENTS.md`、`docs/AGENTS.md`、`scripts/AGENTS.md`。
- 非范围：运行时代码、接口、权限、金额、DDL、设计 Token 值和验收事实。
- 受影响端：App / Web 的 AI 协作上下文。

## 3. 决策与边界

- 根文件只保留跨任务硬边界和任务路由。
- `src/AGENTS.md` 只承载实现、交互、选择器、分包和验证规则。
- `docs/AGENTS.md` 只承载事实源、文档和验收证据规则。
- App `scripts/AGENTS.md` 承载真实小程序录屏、写操作授权和脚本质量。
- 历史完整 AGENTS 可从 Git 提交历史恢复，不在活跃上下文中重复。

## 4. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件 |
| --- | --- | --- | --- |
| th4-decoboon | codex/context-reset-20261102 | `3ffc6ced`、`19d97e52`、本次提交 | 归档、问题关闭、统一 CHG |
| th4-decoboon-web | codex/context-reset-20261102 | `ef7c619`、`6814e2e` | Web 归档和 AGENTS 拆分 |
| th4-decoboon-app | codex/context-reset-20261102 | `49d7159`、`95bd170` | App 归档和 AGENTS 拆分 |

## 5. 验收条件与结果

| 序号 | 场景 | 预期结果 | 结果 |
| --- | --- | --- | --- |
| 1 | Web 根任务 | 只读根级边界和路由 | PASS |
| 2 | Web 源码任务 | 叠加 `src/AGENTS.md` | PASS |
| 3 | Web 文档任务 | 叠加 `docs/AGENTS.md` | PASS |
| 4 | App 源码任务 | 叠加实现、视觉、选择器和分包规则 | PASS |
| 5 | App 文档/录屏任务 | 分别叠加 docs/scripts 规则 | PASS |
| 6 | 高风险规则 | 同源、编号、T06/T12、T15-T17、金额、权限、发版边界保留 | PASS |

## 6. 验证

- Web 活跃 AGENTS 合计：2,039 + 2,080 + 1,081 = 5,200 字符；原根文件 10,891 字符。
- App 活跃 AGENTS 合计：1,905 + 2,965 + 866 + 635 = 6,371 字符；原根文件 21,891 字符。
- 三个仓库 `git diff --check`：PASS。
- 引用路径和 npm scripts：PASS。
- 未运行页面构建和测试：本次仅修改 Markdown 规则，无运行时代码变化。

## 7. 发布

- Release ID：不适用。
- 数据库迁移：无。
- 生产版本：不适用；当前后端 tag 仍为 `v2026.11.02.2`。

## 8. 回滚

- 回滚条件：拆分后高风险规则丢失或目录规则无法被识别。
- 回滚步骤：恢复对应仓库上一 Git 提交中的单一 AGENTS 文件。
- 实际是否回滚：否。

## 9. 结论

- App 根级上下文由 21,891 字符降至 1,905 字符。
- Web 根级上下文由 10,891 字符降至 2,039 字符。
- 详细规则没有继续堆在根入口，改为按当前任务目录加载。
