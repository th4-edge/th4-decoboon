# 材料域下一批提示词（B6.2 B6 缺陷修复独立复验）

继续 material 域。先恢复外部状态，不要依赖聊天记忆：

1. 读取 `AGENTS.md`、`docs/operations/试运营单人轻量协作与发版规范.md`；
2. 读取 `.agents/skills/domain-delivery-orchestrator/SKILL.md`、`run-envelope.md`、`state.json`；
3. 读取 `00-charter.md`、`04-requirements.md`、`05-plan.md`、`06-implementation.md`、`08-acceptance.md`、`10-reconciliation.md`；
4. 读取 `evidence/B6/` 与 `evidence/B6.1/`，以 B6 原始缺陷为复验基线；
5. 涉及权限、成本、应付、结算时读取 `/Users/zhangwei/.agents/skills/zhuangdeqing-business/SKILL.md` 及 BR-044/BR-045/DEC-072/073/074。

当前预期：B6 独立验收 FAIL，B6.1 已完成修复并 RECONCILED；B6.2 才能判断是否进入 ACCEPTED。以 `state.json` 为准。

## 独立性要求

- 必须使用独立验收角色/任务，不能由 B6.1 实现者自批；
- 只做黑盒验证和证据采集，不修改实现代码；
- 以 B6 的 6 个缺陷为主，复跑受影响矩阵并增加回归；
- 用户最终决定是否进入 `ACCEPTED`；无法启动独立角色时写决策包并 BLOCKED。

## 必验

1. Designer/PM 对 `material/sheet/info|page`、`project/purchase/page`、`material/settlement/page|info` 的权限负向；
2. 相同 `material/sheet/order` 重试只生成一份采购/采购明细/应付；相同幂等键不同 payload 必须冲突；
3. 未到场采购明细 preview 为空、confirm 被拒；到场后结算数量不得超过到场数量；
4. 暂估应付返回 `estimated=true`，App/Web 不显示 0 为真实金额；结算/撤销后标记与成本正确回退；
5. 复跑 B6 主链、跨企业/跨门店、旧路线、50 黄金问题、跨端、H5 UI；
6. 证据落 `evidence/B6.2/`，更新 `08-acceptance.md`、`10-reconciliation.md`、`state.json/history`。

## 边界

- 不改业务规则、权限语义、金额/成本公式；不做 DDL/迁移；不碰生产；不正式发布小程序；
- 不修改 B6/B6.1 证据；不把开发自测当独立验收；
- 通过后输出 B7（材料业务助手上下文入口）的实施提示词；失败则登记缺陷并停在修复门禁。
