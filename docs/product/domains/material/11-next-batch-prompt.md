# 材料域下一批提示词（B6.5 修复 Web Mixed 状态展示）

继续 material 域。先恢复外部状态，不要依赖聊天记忆：

1. 读取 `AGENTS.md`、`docs/operations/试运营单人轻量协作与发版规范.md`；
2. 读取 `.agents/skills/domain-delivery-orchestrator/SKILL.md`、`run-envelope.md`、`state.json`；
3. 读取 `00-charter.md`、`04-requirements.md`、`05-plan.md`、`06-implementation.md`、`07-ui-review.md`、`08-acceptance.md`、`10-reconciliation.md`；
4. 读取 `evidence/B6.4/` 与 `evidence/B6.3/`；
5. 涉及价格/成本语义时读取 `/Users/zhangwei/.agents/skills/zhuangdeqing-business/SKILL.md` 与 BR-045/DEC-072/073。

当前预期：B6.4 独立复验 139/140；B62-DEF-001 后端已关闭；B64-UI-WEB-SHEET-01 开放。以 `state.json` 为准。

## 本批只修 Web UI 展示

Web `/finance/material-ordering` 的作业单行只展示履约状态，Mixed 作业单没有“混合/部分已结算/成本待结算”提示，尽管 `sheet.priceState=Mixed` 且 App H5 正确显示。

## 要求

- 以 `sheet.priceState` / `item.priceState` 为唯一数据源，不在前端推导价格/成本；
- 对 Unpriced、Mixed、Settled、Priced 显示明确文案和视觉标记；
- 保留已到场、部分到场、已下单等履约状态；
- 覆盖空态、全价、无价、Mixed、全部结算、权限受限、加载/失败/重试；
- 不改后端语义、金额公式、权限或数据库；不新增 DDL；
- Web 真实 1440×900 截图和类型检查/构建；
- 更新 UI review、implementation、reconciliation、state/history；修复证据落 `evidence/B6.5/`；
- 停在 RECONCILED，不得自批 ACCEPTED；B6.6 独立复验。

## 后续

- B6.6：Web Mixed 状态聚焦独立复验 + 回归；
- B7：材料业务助手上下文入口；
- B8：助手 UI 独立验收；
- B9：体验版/发布准备。
