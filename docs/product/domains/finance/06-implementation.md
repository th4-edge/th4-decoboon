# 财务经营与项目核算 · 实现记录

## 本批范围

- Batch：`B6`
- 垂直切片：有效 Remove/Quantity 成本完整性分母 + 成本专项测试
- 不包含：Replace 目标需求完整模型、真实成本补录、结算确认/关闭、发布

## 变更

- 后端：`ContractChangeRangeContext` 提供当前有效剩余纯计算；`ProjectCostContext.materialFacts` 按有效需求判断；`ContractChangeItemRepository` fetch join sourceLine。
- App：无改动；只读回读同源项目财务。
- Web：无改动；项目核算只读回读新成本缺失数。
- 迁移：无。
- 配置：无。

## 测试与证据

- 定向测试：成本专项 6/6 PASS；FinanceClosedLoopTest + 成本上下文测试 PASS。
- 后端全量：`mvn -q test`，1169 tests / 0 failures / 0 errors / 0 skipped PASS。
- 真实页面：Finance Web 项目核算成本缺失 2→1；利润为空；结算仍 Draft。
- 账务不变量：`coreBalanced=true`。
- 证据：`S70-S72`、`CHG-20261031-039`、`T12-BIZ-20261031-001`。

## 风险与未闭环

- 真实人工成本未发生，最终利润仍不可用；独立成本验收未执行。
- Replace 必须同时纳入目标需求后才能关闭来源，后续单批处理。
