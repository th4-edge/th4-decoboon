# CHG-20261031-047 · 合同读模型成本聚合与全量门禁修复

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | `CHG-20261031-047` |
| 类型 | 重构 / 性能结构治理 / 缺陷修复 |
| 风险级别 | L2（合同成本读模型，保持口径不变） |
| 状态 | `DEV_VERIFIED_PENDING_INDEPENDENT_ACCEPTANCE` |
| 负责人 | codex |
| 日期 | 2026-10-31 |
| 关联需求/事故/业务裁决 | `CHG-20261031-042`、DQ-X10、`DEC-036`、`DEC-037`、`BR-003` |

## 2. 目标与范围

- 目标：`ProjectContractContext.enrich` 不再全量加载合同行计算 `signedCost/costComplete`，改为单条数据库聚合；同时修复 DQ-X6 新增 Provider 未纳入汇总/安全门禁的存量缺口。
- 范围内：合同成本聚合 SQL、`ProjectContractContext.enrich`、定向/L2 全量测试、`quote/finance/summary` 汇总包登记、Provider 安全面板从 542 更新为 543。
- 非范围：不改变成本、金额、利润、权限、状态或前端响应字段；不改造 `Range` 来源选择器分页；不处理文档生成全量合同行；不发布。
- 受影响端：后端。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：涉及合同成本读取实现，不改变业务公式。
- 业务事实来源与裁决编号：`BR-003` 成本边界、`DEC-036` 聚合 SQL、`DEC-037` 先测量后优化。
- 完整性口径保持：
  - `signedCost = SUM(COALESCE(cost,0))`
  - `Construction / Material / Custom / Soft / Appliance` 的 `cost <= 0` 视为缺失；
  - 合同无行时 `costComplete=false`；
  - 任意必备成本缺失时 `costComplete=false`。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | 完整成本 | 所有必备成本大于 0 | 返回成本合计，`costComplete=true` | `backend-test.log` |
| 2 | 成本缺失 | 至少一条必备成本缺失 | 返回成本合计，`costComplete=false` | `backend-test.log` |
| 3 | 空合同 | 合同行数为 0 | 成本 0，`costComplete=false` | `backend-test.log` |
| 4 | 禁止全量实体 | enrich 执行 | 不得调用 `findByContractOrderBySortAsc` | `backend-test.log` |
| 5 | 真实 API | Boss/Designer/StoreManager | 6 行/¥977.80、1 变更/¥-168.00 不变；Designer cost 不返回 | `api-probe.json` |
| 6 | 数据不变 | 只读验证前后 | `db_contract_line` checksum 和业务摘要一致 | `db-baseline.txt`、`db-after.txt` |
| 7 | 全量门禁 | DQ-X6 Provider | summary context 纳管，Provider 面板 543，未登录 401 | `backend-full-test.log` |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | `dev` | 未提交工作区 | `ContractLineRepository`、`ProjectContractContext`、`ProjectContractEnrichCostAggregateTest`、`ContractFactTraceTest`、`SummaryImplementationRulesTest`、`ProviderSecurityAcceptanceTest` |
| th4-decoboon-web | `dev` | 无改动 | 不适用 |
| th4-decoboon-app | `dev` | 无改动 | 不适用 |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| 定向测试 | 4 个合同相关测试类 | PASS，14/14 | `backend-test.log` |
| L2 全量测试 | `mvn test` | PASS，1184/1184 | `backend-full-test.log` |
| Provider 门禁 | `audit-provider-response-contract.py --check` + `--self-test` | PASS，543 Provider / 0 finding | `provider-response-audit.txt`、`provider-response-self-test.txt` |
| 前端 Provider 契约 | `audit-frontend-provider-contract.py --check` | PASS，unmatched=0 | `frontend-provider-contract-check.txt` |
| 真实 API | 三角色 contract info/workbench | PASS，业务摘要不变 | `api-probe.json` |
| 成本聚合 SQL | `EXPLAIN ANALYZE` | 合同索引 lookup + Aggregate，单行结果 | `explain-analyze.txt` |
| 数据只读 | `CHECKSUM TABLE` + 成本摘要 | 前后一致 | `db-baseline.txt`、`db-after.txt` |
| 接口采样 | Boss workbench 10 次 | P50 92.19ms / P95 99.84ms / 17,501B | `api-timings.json` |

- 未执行项及原因：未做真实页面回归，因为本次后端响应字段和页面契约未变化；独立验收、发布未执行。
- 已知失败及是否与本次相关：首次全量测试因 DQ-X6 的 summary 包登记和 Provider 数量清单未更新而失败；本批已修复并复跑通过。

## 7. 发布

- 不发布。无 DDL、迁移、配置、密钥或业务数据变化。

## 8. 回滚

- 回滚条件：成本合计或 `costComplete` 与逐行口径不一致、合同详情/workbench 异常。
- 回滚命令/步骤：回退 `summarizeCostByContract` 与 `ProjectContractContext.enrich` 聚合改动，恢复逐行计算；同步回退门禁登记不再需要，因为门禁修复独立成立。
- 回滚验证：重跑合同成本定向测试、L2 全量测试和三角色 API 对账。
- 实际是否回滚：否

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集
- 人工耗时：未采集
- 一次通过：否；全量测试发现两项 DQ-X6 门禁登记缺口，补齐后通过
- 返工原因：Provider 面板与 summary 包登记滞后于 DQ-X6 实现
- 最终结论：开发自测、L2 全量回归和真实 API 验证通过，待独立验收

## 10. 复盘与沉淀

- 是否出现重复问题：是，新增汇总 Provider 后未同步架构与安全清单
- 是否需要新增门禁：不需要新增；现有全量门禁已能发现，关键是实现同批更新登记
- 候选 `EXP/ANT/PATH`：`ANT-新增Provider不登记汇总安全清单`、`PATH-读模型聚合SQL+同批更新Provider面板`
- 后续动作与负责人：执行 DQ-X10 独立验收；下一优先切片为 `project/contract-change/range` 的 PICKER_PAGE 分页与批量快照改造
