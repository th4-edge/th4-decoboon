# DQ-X8 · 合同基线行数据库分页

> 日期：2026-10-31
> 状态：`DEV_VERIFIED`
> 关联变更：`CHG-20261031-045`
> 业务口径：`DEC-036`、`DEC-055`、`BR-003`

## 目标

合同档案不再通过 `size:500` 拉取合同行后在前端截取前 30 条；合同基线行改为数据库真实分页，合同变更来源改为只消费服务端 range 投影。

## 接口契约

`project/contract-line/page`

| 参数 | 规则 |
| --- | --- |
| `projectId` / `contractId` | 二选一，沿用项目/合同数据范围校验 |
| `current` | 从 1 开始；缺省或小于 1 时为第 1 页 |
| `size` | 缺省 20；最大 50；小于 1 时使用 20 |
| 排序 | `sort ASC, lineId ASC` |
| 返回 | Spring Data `Page<ContractLine>`，按 Provider `@Properties` 白名单裁剪 |

Web 适配层显式返回 `ContractLinePage { list, current, size, total }`，因为现有 TH4 Page JSON 不返回请求页码；页码由本次请求参数保留，总数读取 `totalElements`。

## 实现

- `ContractLineRepository` 增加 Pageable 派生查询；
- Provider 使用 `PageRequest` 在数据库切页，禁止全量查询后 `subList`；
- 无成本权限角色在返回前将当前页 `cost` 置空；
- 合同档案页使用 `lineCount/lineAmount` 作为头部汇总，表格只显示当前页；
- 合同变更页删除合同行全量加载和 range 失败后的前端拼接降级，只使用 `project/contract-change/range`；
- 移除 Web 固定 `size:500` 基线项。

## 对账

| 检查 | 结果 |
| --- | --- |
| 默认页 | 0/20，稳定排序 |
| 大页请求 | size=500 被钳制为 50 |
| 真实相邻页 | size=2 的第 1/2 页 ID 无交集 |
| 合同档案 | 请求 `current:1,size:20`；6 项 / ¥977.80 |
| Boss / StoreManager | 成本字段按角色返回 |
| Designer | cost 字段 0 个 |
| 合同变更来源 | 7 个 range 来源；页面无 `contract-line/page` 请求 |
| Web | 1440/1280/1024 无横向溢出，console 0 error |
| 接口时延 | P50 30.16ms，最大 34.67ms |

## 边界

- 真实合同只有 6 行，页面翻页按钮未在真实数据下出现；翻页窗口由 Provider 单测和 API `size=2` 两页请求验证。
- 未执行独立验收和视觉签字。
- 其他仍加载全量合同行的上下文不在本批范围，应作为后续独立切片分类处理。

## 证据

`docs/acceptance/runs/20261031-dq-x8-contract-line-pagination/`
