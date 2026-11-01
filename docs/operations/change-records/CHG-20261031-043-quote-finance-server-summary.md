# CHG-20261031-043 · 财务报价服务端汇总接口

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | `CHG-20261031-043` |
| 类型 | 缺陷修复 / 后端聚合 / 前端消费治理 |
| 风险级别 | L1（单 Provider + 单页面，只读金额汇总） |
| 状态 | `DEV_VERIFIED_PENDING_INDEPENDENT_ACCEPTANCE` |
| 负责人 | codex |
| 日期 | 2026-10-31 |
| 关联需求/事故/业务裁决 | `CHG-20261031-042`、`DEC-036`、`DEC-065`、`DEC-078`、`BR-001`、`BR-002`、`BR-003`、`BR-028` |

## 2. 目标与范围

- 目标：新增 `quote/finance/summary`，以单条聚合 SQL 返回财务报价总额、成本完整部分预计毛利、成本未完整数和报价数；Web 财务报价页改为消费服务端汇总，删除前端对设计师分组结果的二次 `reduce`。
- 范围内：报价汇总聚合上下文、Provider、响应模型、Web API、Web 财务报价页、单 Provider 对账测试和真实页面验证。
- 非范围：不改变报价金额、成本、毛利公式；不改变 DEC-065 财务数据范围；不处理报价明细分页和合同/项目利润口径。
- 受影响端：后端、Web。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：涉及既有报价金额、成本和预计毛利口径的读取，不改变公式。
- 业务事实来源与裁决编号：`DEC-036` 聚合 SQL 正模式、`DEC-065` 财务数据范围、`DEC-078` 运营只读查看、`BR-001/002/003` 报价与成本规则。
- 尚存 GAP 或假设：无；成本未完整报价的预计毛利按既有实现仅累计成本完整部分。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | 总部财务 | 3 份测试报价，2 份成本完整 | summary count=3，amount=350，profit=70，incomplete=1 | `FinanceQuoteAggregateTest` |
| 2 | 门店财务 | 仅可见本店 1 份报价 | summary count=1，amount=200，profit=50 | 同上 |
| 3 | 状态筛选 | `Accepted` | summary count=2，amount=300，profit=70，incomplete=0 | 同上 |
| 4 | 分组一致性 | 同筛选下设计师分组结果 | summary 各指标等于分组行合计 | 同上 |
| 5 | Web 真实页面 | 财务报价页 | KPI 消费 summary，不再前端 reduce；页面可加载/翻页 | 浏览器证据 |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | `dev` | 未提交工作区 | `QuoteFinanceAggregateContext`、`QuoteFinanceSummary`、`quote/finance/summary`、测试 |
| th4-decoboon-web | `dev` | 未提交工作区 | `FinanceQuotesView.vue`、`trade/api.ts` |
| th4-decoboon-app | `dev` | 无改动 | 不适用 |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| 定向测试 | `mvn -q -Dtest=FinanceQuoteAggregateTest test` | PASS；总部/门店/状态过滤/分组对账 | `backend-test.log` |
| Provider 响应门禁 | `python3 scripts/audit-provider-response-contract.py --check` + `--self-test` | PASS，543 Provider / 0 finding | `provider-response-audit.json` |
| 前端 Provider 契约 | `python3 scripts/audit-frontend-provider-contract.py --check` | PASS，`quote/finance/summary` 登记为 Web-only read | `frontend-provider-contract-check.txt` |
| Web 构建 | `npm run build` | PASS | `web-build.log` |
| API 对账 | summary vs report groups vs page total | PASS，2=2，金额/毛利/未完整数一致 | `reconciliation.json` |
| 真实页面 | `/finance/quotes` | PASS，KPI 与服务端一致，console/network error 0 | `web-finance-quotes.json`、三档截图 |
| 响应式结构 | 1440/1280/1024 | PASS，无横向溢出 | `viewports.json` |
| 10× 数据量 | H2 从 3 份增加到 33 份 | summary 仍单行，count/amount/profit/incomplete 正确 | `backend-test.log` |
| 接口 P95 | 12 次本地端到端采样 | P50 17.49ms / P95 22.57ms / max 23.10ms | `api-timings.txt` |

- 未执行项及原因：未独立验收、未发布；当前主模型不支持图片输入，不声明视觉评审通过。
- 已知失败及是否与本次相关：无。

## 7. 发布

- 不发布；仅本地验证。无 DDL、迁移、配置和密钥变化。

## 8. 回滚

- 回滚条件：报价汇总与分组结果不一致、成本未完整口径错误或页面无法加载。
- 回滚命令/步骤：回退 summary Provider 和 Web 页面改动。
- 回滚验证：重新执行定向测试、Web 构建和财务报价页。
- 实际是否回滚：否

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集
- 人工耗时：未采集
- 一次通过：是（首轮 type-check 修正前端新接口语法后通过）
- 返工原因：新 summary API 初次实现缺少本地 record 归一化并出现一次括号语法错误，已修正
- 最终结论：开发自测与真实财务页面验证通过，待独立验收

## 10. 复盘与沉淀

- 是否出现重复问题：是；前端二次汇总属于 PAG 规范存量问题。
- 是否需要新增门禁：后续补前端业务聚合门禁。
- 候选 `EXP/ANT/PATH`：`ANT-分组聚合再前端求和`、`PATH-汇总指标直接使用单行聚合 SQL`
- 后续动作与负责人：完成 DQ-X6 证据后继续下一批金额页面。
