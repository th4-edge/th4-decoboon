# DQ-X9 · 业主与小程序合同行分页同源

> 日期：2026-10-31
> 状态：`DEV_VERIFIED`
> 关联变更：`CHG-20261031-046`
> 业务口径：`DEC-036`、`DEC-055`、`BR-003`

## 目标

补齐 DQ-X8 之后的跨端缺口：业主 `customer/contract-line/page` 不再全量返回，小程序合同履约页不再无界读取或用当前页金额冒充完整合同范围汇总。

## 接口契约

### `customer/contract-line/page`

| 参数/返回 | 规则 |
| --- | --- |
| `current` | 从 1 开始；缺省或小于 1 时为第 1 页 |
| `size` | 缺省 20；最大 50；小于 1 时使用 20 |
| 排序 | `sort ASC, lineId ASC` |
| 返回 | Spring Data `Page<ContractLine>`，业主白名单不含 cost |
| 访问 | 继续由 `CustomerAccessContext + CustomerContractContext` 校验项目授权 |

### `customer/contract/workbench`

返回 `lineCount / lineAmount`，供业主端合同范围摘要与分页组件使用；不返回成本对象。

## App 实现

- `tradeApi.contractLines(params, current, size)` 返回 `PageData<ContractLine>`，统一使用 `toPageData`。
- 合同履约页首屏请求第 1 页，滚动到底按 `lineHasMore` 加载下一页。
- 合同范围总项数和总金额只消费 `workbench.lineCount/lineAmount`。
- 未全部加载时不显示分组金额，避免当前已加载子集被误读为完整分组合计。
- 首屏失败显示可重试错误，不再 `.catch(() => [])` 静默伪装为空合同。
- 新增 `check:contract-line-pagination` 门禁，含缺失 current/size、当前页行数冒充总数、当前页金额求和等负向自测。

## 对账

| 检查 | 结果 |
| --- | --- |
| 业主 workbench | 6 项 / ¥977.80，cost 不出现 |
| 业主页大小 | 缺省 20；size=500 返回页大小 50 |
| 业主相邻页 | size=2 第 1/2 页 ID 无交集 |
| App 请求 | `{"contractId":"...","current":1,"size":20}` |
| 小程序原生 | 6 项 / ¥978 / 合同明细已全部加载；4/4 CHECK PASS |
| App 门禁 | 5/5 负向样本被拦截 |

## 边界

- 真实合同只有 6 行，原生页面未触发下一页加载；下一页逻辑由后端单测、真实业主 API 双页和 App 静态契约门禁验证。
- H5 开发运行器有 uni-h5 既有 `clientWidth` 空指针，不作为原生页面通过依据。
- 未执行独立验收和视觉签字。

## 证据

`docs/acceptance/runs/20261031-dq-x9-owner-contract-line-pagination/`
