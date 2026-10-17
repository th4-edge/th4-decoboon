# 材料域下一批提示词（B6.1 修复独立验收缺陷）

继续 material 域。先恢复外部状态，不要依赖聊天记忆：

1. 读取 `AGENTS.md`、`docs/operations/试运营单人轻量协作与发版规范.md`；
2. 读取 `.agents/skills/domain-delivery-orchestrator/SKILL.md`、`run-envelope.md`；
3. 读取 `state.json`、`00-charter.md`、`04-requirements.md`、`05-plan.md`、`06-implementation.md`、`08-acceptance.md`、`10-reconciliation.md`；
4. 读取 `evidence/B6/README.md`、`defects.json`、`blackbox-results.json`、`summary.json`、`api-samples.json`；
5. 涉及材料权限、金额/成本口径时，读取 `/Users/zhangwei/.agents/skills/zhuangdeqing-business/SKILL.md` 及其规范事实源。

当前预期：B6 独立验收结论为 FAIL；B5 实现不可视为已验收。以 `state.json` 为准。

## B6.1 修复范围

只修复 B6 独立证据已确认的 6 个缺陷，不扩业务规则、不做 DDL：

- `B6-DEF-001`：Designer/PM 对 `material/sheet/info|page` 的成本字段可见性；
- `B6-DEF-002`：Designer/PM 对 `project/purchase/page` 的采购金额、供应商敏感信息可见性；
- `B6-DEF-003`：Designer/PM 对 `material/settlement/page|info` 的角色门禁；
- `B6-DEF-004`：`material/sheet/order` 重复请求的业务幂等键，禁止重复采购/应付；
- `B6-DEF-005`：`material/settlement/preview|confirm` 必须要求 `receivedQuantity>0`；
- `B6-DEF-006`：无价采购的暂估应付读模型不得显示 0，必须可识别暂估/待定价。

## 边界

- 先核对 BR-044/BR-045、DEC-072/073/074 与既有权限规范，修复只能恢复到已裁决口径；
- 不改状态机、不改金额公式、不改角色、不做 DDL/迁移、不碰生产、不正式发布小程序；
- 不修改 B6 证据文件；新修复证据落 `evidence/B6.1/`；
- Designer/PM 的履约需求继续可读，但成本、采购金额、结算金额、供应商敏感字段必须按角色最小可见；
- 幂等实现优先复用现有框架能力或业务唯一键，不引入前端可信 ID 作为权限依据。

## 验证与退出

- 定向测试覆盖 6 个缺陷的正向/反向/重复请求/跨角色；
- 金额/成本/权限变更执行全量后端测试；App/Web 相关构建与真实页面走查；
- 补充 B6.1 请求/响应、DB 前后差异、测试结果和页面证据；
- 更新 `06-implementation.md`、`07-ui-review.md`、`10-reconciliation.md`、`state.json/history`；
- 停在 `RECONCILED`，不得自批 `ACCEPTED`；B6.2 由独立验收角色复跑受影响矩阵与回归。

## 后续队列

- B6.2：独立复验 B6.1；
- B7：材料业务助手上下文入口（App/Web 统一单轮「问材料」抽屉，复用 `material/assistant/ask`，不做多轮记忆）；
- B8：助手 UI 聚焦独立验收；
- B9：体验版/发布准备；正式小程序提审和发布永远由用户完成。
