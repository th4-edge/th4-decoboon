# 材料域下一批提示词（B5）

继续 material 域。先恢复外部状态，不要依赖聊天记忆：

1. 读取 `AGENTS.md`、`docs/operations/试运营单人轻量协作与发版规范.md`；
2. 读取 `.agents/skills/domain-delivery-orchestrator/SKILL.md`、`run-envelope.md`；
3. 读取 `state.json`、`00-charter.md`、`10-reconciliation.md`、`03-blueprint.json`、`04-requirements.md`、`05-plan.md`；
4. 读取 `06-implementation.md`、`07-ui-review.md`、`evidence/B2/`、`evidence/B3/`、`evidence/B4/`；
5. 涉及材料对象、权限、金额/成本口径时，读取 `/Users/zhangwei/.agents/skills/zhuangdeqing-business/SKILL.md` 及其规范事实源。

当前预期：B2、B3、B4 已完成；B5 尚未开始。以 `state.json` 为准。

## B5 独立性要求

B5 是**独立验收批次**，不得由 B1–B4 实现者自批：

- 使用独立验收角色/任务或用户指定的验收方；
- 只做黑盒验证和证据采集，不修改实现代码；
- 发现问题只登记缺陷和证据，不自行宣布通过；
- 用户最终决定是否进入 `ACCEPTED`。

如果当前环境无法启动独立验收角色，停止 B5 并写决策包，不伪造独立验收。

## 验收范围

- Web：`/business-blueprints`、`/finance/projects/:id` 材料体检；
- App：项目详情材料体检、作业单详情时间/取消上下文、建单来源说明、帮助主题；
- Provider：`material/assistant/ask` 50 个黄金问题、权限负向、成本边界、审计；
- 对账：设计 ↔ 蓝图 ↔ 需求 ↔ 代码/API ↔ 页面 ↔ 测试 ↔ 验收。

## 必测用例

- 正向：选材、下单、无价、部分到场、结算、成本完整性、增减项三分支、供应商/经销商边界；
- 反向：未选材、超量、缺测量、已下单取消、跨企业、跨项目、越权、无证据拒答、自动审批/付款/删除/改权限拒答；
- 幂等：重复提交/下单/结算、重复请求审计；
- 边界：成本未完整时利润 null、无价长期悬挂、部分到场尾量、空项目/无材料计划；
- 跨端：App/Web 对同一项目和作业单的阶段、计数、下一步和权限一致。

## 证据

- 新证据落 `docs/product/domains/material/evidence/B5/`；
- 包含黑盒请求/响应、截图、租户/角色、失败恢复、缺陷台账；
- 更新 `08-acceptance.md`、`10-reconciliation.md`、`state.json/history`；
- 无 `UNMAPPED`/`STALE`；
- 不发布正式小程序；体验版边界仍按 run-envelope。

## 返回包

- 批次结果与证据路径；
- 正向/反向/越权/幂等/边界/跨端逐项结果；
- 未闭环项、缺陷与风险；
- 是否需要用户决策；
- 是否满足 `ACCEPTED` 的独立证据条件；
- 下一批 B6 发布准备提示词（仅当 B5 通过）。
