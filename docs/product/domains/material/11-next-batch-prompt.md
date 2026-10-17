# 材料域下一批提示词（B6.6 Web Mixed 状态聚焦独立复验）

继续 material 域。先恢复外部状态，不要依赖聊天记忆：

1. 读取 `AGENTS.md`、`docs/operations/试运营单人轻量协作与发版规范.md`；
2. 读取 `.agents/skills/domain-delivery-orchestrator/SKILL.md`、`run-envelope.md`、`state.json`；
3. 读取 `00-charter.md`、`05-plan.md`、`06-implementation.md`、`07-ui-review.md`、`08-acceptance.md`、`10-reconciliation.md`；
4. 读取 `evidence/B6.4/` 与 `evidence/B6.5/`；
5. 涉及金额/成本语义时读取 `/Users/zhangwei/.agents/skills/zhuangdeqing-business/SKILL.md` 与 BR-045/DEC-072/073。

当前预期：B6.4 发现 Web Mixed 状态缺失；B6.5 已修复并 RECONCILED，待 B6.6 独立复验。以 `state.json` 为准。

## 独立性要求

- 独立验收角色/任务，不得由 B6.5 实现者自批；
- 只做黑盒与真实浏览器验证，不改实现；
- 用户最终决定 `ACCEPTED`。

## 必验

1. 构造不同供应商拆单：Priced 3@100 + Unpriced 5，部分/全部到场后结算 5@120，再撤销；
2. Web `/finance/material-ordering` 1440×900 必须显示“部分已结算 · 成本待结算”，不得只显示“已到场”；
3. 验证 Unpriced、Priced、Settled 文案和状态 pill；不得由前端自行推导；
4. 回归 B62-DEF-001 后端价格投影、权限、幂等、到场门禁、暂估应付、50 黄金问题、旧路线、跨端；
5. App/Web 无横向溢出、Console Errors 0；
6. 证据落 `evidence/B6.6/`，更新 `08-acceptance.md`、`10-reconciliation.md`、`state.json/history`。

## 边界

- 不改业务规则/权限/金额公式；不做 DDL/迁移/生产/正式小程序发布；
- 不修改 B6–B6.5 证据；
- 通过后生成 B7 材料业务助手上下文入口提示词；失败则登记缺陷并停止。
