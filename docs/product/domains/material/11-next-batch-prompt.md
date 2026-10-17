# 材料域下一批提示词（B6.3 修复多供应商价格投影）

继续 material 域。先恢复外部状态，不要依赖聊天记忆：

1. 读取 `AGENTS.md`、`docs/operations/试运营单人轻量协作与发版规范.md`；
2. 读取 `.agents/skills/domain-delivery-orchestrator/SKILL.md`、`run-envelope.md`、`state.json`；
3. 读取 `00-charter.md`、`04-requirements.md`、`05-plan.md`、`06-implementation.md`、`08-acceptance.md`、`10-reconciliation.md`；
4. 读取 `evidence/B6/`、`evidence/B6.1/`、`evidence/B6.2/`；
5. 涉及材料价格/成本/结算时读取 `/Users/zhangwei/.agents/skills/zhuangdeqing-business/SKILL.md` 及 BR-044/BR-045/DEC-072/073/074。

当前预期：B6.2 独立复验为 149/150 FAIL；原 B6 六缺陷已关闭；新缺陷 B62-DEF-001 开放。以 `state.json` 为准。

## 本批只修一个缺陷

`B62-DEF-001`：同一作业单行拆给多个供应商后，采购明细分别为 Priced / Unpriced / Settled，但 `material/sheet/info|page` 的 sheet/item `priceState` 仍为 `Unpriced`，`purchasePrice/settlementPrice` 为空。

## 修复原则

- 只修作业单读模型的价格状态聚合，不改变采购、应付、结算金额公式；
- 状态应能表达：全有价、全无价、有价+无价混合、部分结算、全部结算；
- 从采购明细事实聚合，不在前端拼装、不修改历史金额事实；
- 如只是读模型投影，不新增表/DDL；若必须改库结构，停止写决策包；
- 不修改 B6/B6.1/B6.2 证据；修复证据落 `evidence/B6.3/`。

## 必测

- 多供应商同作业单行：Priced 3@100 + Unpriced 5；
- 全部到场后 Unpriced 5 以 120 结算；
- 作业单/行 `priceState` 分别正确显示 Mixed/Priced/Settled/Unpriced；
- 部分结算、撤销结算后状态回退正确；
- 采购/应付金额、权限、幂等、到场门禁不回归；
- 后端全量测试 + App/Web 构建 + 真实页面检查；
- 更新 implementation/UI review/reconciliation/state/history；
- 停在 `RECONCILED`，不得自批 `ACCEPTED`，B6.4 独立复验。

## 后续队列

- B6.4：独立复验 B62-DEF-001 + 回归；
- B7：材料业务助手上下文入口；
- B8：助手 UI 独立验收；
- B9：体验版/发布准备；正式版由用户完成。
