# DQ-X10 · 合同读模型成本聚合与合同行读取分类

> 日期：2026-10-31
> 状态：`DEV_VERIFIED`
> 关联变更：`CHG-20261031-047`
> 业务口径：`DEC-036`、`DEC-037`、`BR-003`

## 目标

消除合同详情、合同分页和合同 workbench 共同调用的 `ProjectContractContext.enrich` 全量合同行读取，改为单条成本聚合，同时保持成本完整性口径不变。

## 剩余合同行全量读取分类

| 位置 | 类型 | 判断 | 后续 |
| --- | --- | --- | --- |
| `ProjectContractContext.enrich` | 接口读模型 | 被合同详情/分页/workbench 调用，原实现全量加载合同行后 Java 求和 | 本批改为单条聚合 SQL |
| `DocumentArchiveContext.renderContract` | 文档生成 | 合同 PDF 必须输出完整合同范围，不属于 JSON 列表接口；需另行评估超大文档生成上限 | 保留，后续独立治理 |
| `ProjectContext.createProject` | 写链路材料化 | 合同行用于生成任务/材料和签约成本，不是接口响应；当前有一次重复查询 | 后续独立优化 |
| `ProjectContractContext.snapshotSignedCost` | 写链路快照 | 签约时需逐行补齐并冻结成本，不能仅用聚合替代 | 保留 |
| `contract-change/range` | PICKER_PAGE | 返回全部可选来源，且每个来源重复查询全部变更明细，存在 N+1 | 下一优先切片 |

## 实现

- `ContractLineRepository.summarizeCostByContract` 用一条 native aggregate 返回行数、成本合计和必备成本缺失数。
- `ProjectContractContext.enrich` 用聚合结果设置 `signedCost/costComplete`，不再加载合同行实体。
- 聚合条件保持原口径：Construction/Material/Custom/Soft/Appliance 的成本小于等于 0 视为缺失；无合同行时不完整。
- 补齐 DQ-X6 `QuoteFinanceAggregateContext` 汇总门禁登记，Provider 安全面板更新为 543。

## 对账

| 检查 | 结果 |
| --- | --- |
| SQL 成本摘要 | 6 行 / ¥866.50 / 缺失 0 |
| 真实 API | 6 行 / ¥977.80；1 变更 / ¥-168.00 |
| 合同当前额 | ¥858.69 |
| Designer | workbench cost 对象不返回 |
| Boss / StoreManager | 成本对象按角色返回 |
| 全量测试 | 1184/1184 PASS |
| 合同行表 | 前后 checksum `2999847383`，业务摘要一致 |
| 接口采样 | P50 92.19ms / P95 99.84ms / 17,501B |

本次没有取得端到端延迟改善，因此不宣称性能优化收益；收益是去除随合同行数增长的全量实体加载和 Java 汇总，响应契约与业务口径保持不变。

## 边界

- 未做真实页面回归，因为响应字段与前端契约未变化。
- `Range` 的无界来源和逐来源查询仍在，作为下一优先切片。
- 未执行独立验收和发布。

## 证据

`docs/acceptance/runs/20261031-dq-x10-contract-enrich-cost-aggregate/`
