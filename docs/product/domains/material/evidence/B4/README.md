# B4 只读材料业务助手证据

日期：2026-10-17

环境：本地 dev 后端 8090、MySQL 用户级 dev 快照、真实 Provider 数据。

入口：`material/assistant/ask`（HTTP 参数由 Provider `@AutoInject` 字段承载）。

角色：企业负责人（Boss，Company）与造数设计师（Designer，Self 范围）。

## 实现范围

- 意图识别：选材门禁、时间语义、作业单/采购单、作业单状态、价格成本、到场、增减项、结算、供应商、项目体检、权限、字段字典、越界拒答、未知问题。
- 知识来源：`blueprints/material-v1.json` 发布蓝图，不复制第二套业务事实。
- 实时工具：
  - `project/project/info`（项目可读范围）
  - `material/sheet/info`（作业单元数据范围）
  - `blueprint/check/report`（Boss/店长/财务项目体检）
  - `material/plan/workbench`（Designer/PM 等项目履约状态）
- 输出：回答正文、证据引用、只读工具调用、下一步、建议追问、审计状态。
- 边界：不生成/提交/审批/付款/删除/改权限，不把 `gap/stale/draft` 当正式结论。

## 50 个黄金问题

- `golden-questions.json`：50 个问题、预期意图、预期证据；
- `golden-question-results.json`：逐题真实 Provider 响应摘要；
- 结果：50/50 意图匹配，全部 `readOnly=true`，全部 `auditStatus=RECORDED`；
- 分布：EXPLANATION 47、PERMISSION_DENIED 2、REFUSAL 1。

## 关键响应样本

`api-samples.json` 包含：

- 价格/成本一般问题；
- 真实项目材料体检（利润 `null`）；
- 到场确认；
- 权限问题；
- 越界审批请求拒答；
- 无证据问题；
- Designer 本人项目；
- Designer 跨项目 404；
- Boss 跨企业项目 404。

其中 Designer 本人项目只返回履约状态，不读取成本；跨项目和跨企业均由既有数据范围返回 404。

## 审计

`audit-summary.json`：复用既有 `db_ai_usage_call`，记录能力、场景、业务对象、员工、状态和资源单位；**不保存问题正文或回答正文**。重复 `requestId + idempotencyKey` 返回同一审计记录。

## 验证

- `mvn -q -Dtest=MaterialAssistantIntentResolverTest,MaterialAssistantGoldenQuestionsTest,MaterialAssistantAuditContextTest,MaterialHealthContextTest,MaterialBlueprintRuntimeTest,MaterialSheetOrderFlowTest,MaterialSheetTemplateFieldConfigTest,MaterialSheetEnumsProviderTest,MaterialSheetTemplateWhitelistTest,MaterialSheetResponseProjectionContractTest test` → PASS；
- `mvn -q -DskipTests compile` → PASS；
- 50 个黄金问题真实 HTTP 扫描 → PASS；
- 无 DDL、无迁移、无生产发布；
- 本地测试账号密码哈希临时重置后已恢复。
