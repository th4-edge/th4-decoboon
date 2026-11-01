# DQ-X6 · 财务报价服务端汇总

> 日期：2026-10-31
> 状态：`DEV_VERIFIED`
> 关联变更：`CHG-20261031-043`
> 业务口径：`DEC-036`、`DEC-065`、`DEC-078`、`BR-001/002/003/028`

## 1. 本批目标

将 Web 财务报价页的报价总数、报价总额、成本未完整数和预计毛利从前端对设计师分组行 `reduce`，改为消费后端单行聚合接口 `quote/finance/summary`。

## 2. 不变项

- 报价金额、成本、预计毛利公式不变；
- 财务角色 Company/Store 数据范围不变；
- 成本未完整时不计算可靠利润的规则不变；
- 报价明细分页和逐份报价详情不改；
- 不新增业务对象、状态或数据库字段。

## 3. 实现

| 层 | 内容 |
| --- | --- |
| 后端模型 | `QuoteFinanceSummary` |
| 聚合上下文 | `QuoteFinanceAggregateContext.summary(scope,status)` 单行 Criteria 聚合 |
| Provider | `quote/finance/summary` |
| Web API | `quoteFinanceSummary()` |
| Web 页面 | `FinanceQuotesView.vue` KPI 直接消费 summary，删除三个 `reduce` |
| 互斥状态 | 与 `quote/finance/report` 共用数据范围和状态过滤 helper |

## 4. 对账

| 检查 | 预期 | 实际 | 结果 |
| --- | --- | --- | --- |
| 总部财务汇总 | count=3 / amount=350 / profit=70 / incomplete=1 | H2 测试一致 | PASS |
| 门店财务范围 | count=1 / amount=200 / profit=50 | H2 测试一致 | PASS |
| `Accepted` 过滤 | count=2 / amount=300 / profit=70 / incomplete=0 | H2 测试一致 | PASS |
| summary = Σ设计师分组 | count/amount/profit/incomplete 全部一致 | H2 测试一致 | PASS |
| summary count = page total | 同数据范围一致 | 本地财务数据 2=2 | PASS |
| 10× 数据量 | 30 份新增报价后 summary 仍单行 | count=33 / amount=380 / profit=70 / incomplete=1；分组仍 2 行 | PASS |
| 本地接口时延 | 12 次端到端采样 | P50 17.49ms / P95 22.57ms / max 23.10ms / 132B | PASS |
| 本地财务页面 | KPI 显示 2 份 / ¥3,522.44 / 0 份 / ¥895.94 | Web 真实页面一致 | PASS |
| 控制台/失败请求 | 0 error / 0 fail | 1440×900 | PASS |
| 响应式结构 | 1280/1024 无横向溢出 | DOM 几何检查 | PASS |

## 5. 证据

- `docs/acceptance/runs/20261031-dq-x6-quote-finance-summary/backend-test.log`
- `provider-response-audit.json`
- `reconciliation.json`
- `finance-quotes-1440.png`
- `finance-quotes-1280.png`
- `finance-quotes-1024.png`
- `web-finance-quotes.json`
- `web-build.log`

## 6. 边界

- 真实页面功能与数据已验证；截图已保存，当前主模型不支持图片输入，不声明视觉评审通过。
- 未执行独立验收、生产发布或小程序变更。
- 下一批优先级：Web 增减项/合同金额前端汇总、App 应收应付选择器合计。
