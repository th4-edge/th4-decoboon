# 材料域下一批提示词（B6.4 B62-DEF-001 聚焦独立复验）

继续 material 域。先恢复外部状态，不要依赖聊天记忆：

1. 读取 `AGENTS.md`、`docs/operations/试运营单人轻量协作与发版规范.md`；
2. 读取 `.agents/skills/domain-delivery-orchestrator/SKILL.md`、`run-envelope.md`、`state.json`；
3. 读取 `00-charter.md`、`04-requirements.md`、`05-plan.md`、`06-implementation.md`、`08-acceptance.md`、`10-reconciliation.md`；
4. 读取 `evidence/B6/`、`evidence/B6.1/`、`evidence/B6.2/`、`evidence/B6.3/`；
5. 涉及成本/结算口径时读取 `/Users/zhangwei/.agents/skills/zhuangdeqing-business/SKILL.md` 与 BR-044/BR-045/DEC-072/073/074。

当前预期：B6.2 独立复验 149/150，B62-DEF-001 开放；B6.3 已修复并 RECONCILED。以 `state.json` 为准。

## 独立性要求

- 独立验收角色/任务，不得由 B6.3 实现者自批；
- 只做黑盒验证与证据采集，不修改实现代码；
- 用户最终决定 `ACCEPTED`。

## 复验重点

1. 同一作业单行拆单：Priced 3@100 + Unpriced 5；
2. 全部/部分到场后，未定价部分结算/撤销；
3. `material/sheet/info|page` 的 sheet/item `priceState`：
   - 全有价 Priced；
   - 全无价 Unpriced；
   - Priced+Unpriced 或 Priced+Settled Mixed；
   - 全部 Settled（同价可回填 settlementPrice）；
4. 采购/应付金额、权限、幂等、到场门禁不得回归；
5. 复跑 B6 主链、50 黄金问题、跨企业、旧路线、App/Web UI；
6. 证据落 `evidence/B6.4/`，更新 `08-acceptance.md`、`10-reconciliation.md`、`state.json/history`。

## 边界

- 不改业务规则、权限、金额/成本语义；不做 DDL/迁移；不碰生产；不正式发布小程序；
- 不修改 B6/B6.1/B6.2/B6.3 证据；
- 通过后生成 B7 材料业务助手上下文入口提示词；失败则登记新缺陷并停在修复门禁。
