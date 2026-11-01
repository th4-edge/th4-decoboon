# DQ-X7 · 项目合同档案服务端汇总

> 日期：2026-10-31
> 状态：`DEV_VERIFIED`
> 关联变更：`CHG-20261031-044`
> 业务口径：`DEC-036`、`DEC-055`、`BR-003`、`BR-041`

## 目标

Web 项目合同档案的合同范围合计和合同变更金额改为消费 `project/contract/workbench` 服务端摘要，删除两个前端 `reduce`。

## 实现

- `ContractWorkbench` 新增 `lineCount / lineAmount`；
- `ContractLineRepository` 增加单条聚合 SQL；
- `ContractWorkbenchContext` 在构建档案时写入合同行摘要；
- Web 使用 `contractWorkbench(projectId)` 获取合同、行摘要和变更摘要；
- 合同变更金额使用 `change.confirmedAmount`，仅累计 CustomerConfirmed；
- 修复施工计划 `acceptanceMode` 枚举对象显示 `[object Object]`。

## 对账

| 检查 | 结果 |
| --- | --- |
| 合同行数 | workbench 6 = 列表 6 |
| 合同行金额 | workbench ¥977.80 = 列表合计 ¥977.80 |
| 变更数量 | workbench 1 = 列表 1 |
| 已生效变更金额 | workbench ¥-168.00 = 列表 CustomerConfirmed 合计 |
| 合同当前额 | ¥858.69，保持业务快照值 |
| Designer | 汇总可见，cost=null |
| StoreManager | 汇总可见，成本按角色返回 |
| Web 页面 | 6 项 / ¥977.80；1 项 / ¥-168.00；无 `[object Object]` |
| Console/network | 0 error / 0 fail |
| 接口 P95 | 88.33ms，17.5KB |

## 证据

`docs/acceptance/runs/20261031-dq-x7-contract-archive-summary/`
