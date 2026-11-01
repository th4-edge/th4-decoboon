# DQ-X11 · 增减项来源选择器分页与批量快照

> 日期：2026-10-31
> 状态：`ACCEPTED`
> 关联变更：`CHG-20261031-048`
> 业务口径：`DEC-036`、`DEC-037`、`DEC-055`、`BR-041`

## 目标

将 `project/contract-change/range` 从“无界返回全部来源 + 每个来源重复加载全部变更明细”改为 PICKER_PAGE：
- 原合同行数据库 count/page；
- 服务端关键词、类型、区域、来源分组筛选；
- 有限分组 facets；
- 变更明细只加载一次，批量计算当前有效剩余/占用/可变更量；
- Web 与小程序按页消费。

## 接口契约

`project/contract-change/range`

| 参数 | 规则 |
| --- | --- |
| current | 从 1 开始，默认 1 |
| size | 默认 20，最大 50 |
| keyword | 名称/品牌/规格/区域/变更单号 |
| itemType | ItemType 枚举名 |
| spaceName | 区域；`__none__` 表示综合项目 |
| group | `contract` / `effective`；空为全部 |
| 返回 | `sources` 当前页 + `current/size/total/hasMore/contractTotal/effectiveTotal/itemTypes/spaces` |
| selected | 指定 sourceLineId/sourceChangeItemId 时独立回带，不受当前页筛选影响 |

## 实现

- `ContractChangeRangeContext` 新增批量快照重载，Range 只调用一次合同变更明细仓储。
- 原合同行用 `Specification + count + findAll(Pageable)` 数据库分页。
- 已生效变更目标来自同一次批量加载结果，并在内存完成筛选和切页。
- facets 通过合同行 group by 聚合 + 已生效目标单批投影生成，避免前端从当前页伪造全局计数。
- Web 增加服务端翻页、关键词防抖、类型/区域筛选、来源缓存和分页器。
- App 增加服务端筛选、滚动加载、facets 和服务端总数消费。
- Web/App 新增合同来源分页门禁，各含 4 个负向样本。

## 对账

| 检查 | 结果 |
| --- | --- |
| 默认 | 7 = 6 原合同 + 1 已生效目标 |
| size=2 | 页 1/2/4 返回 2/2/1，ID 无重复 |
| size=500 | 服务端截为 50 |
| effective 分组 | 1 条 |
| Material | 3 条（2+1） |
| 关键词全屋保洁 | 1 条 |
| 非法筛选 | 业务 400 |
| Web | 主材筛选 3 项，请求体含 current/size/itemType，console 0 error |
| 小程序原生 | 初始 7 项、主材 3 项，2/2 PASS |
| 全量测试 | 1186/1186 PASS |
| 接口 | P50 39.98ms / P95 43.49ms / 3,038B |

## 边界

- 当前合同只有 7 个来源，真实页面未触发翻页器；分页窗口由 API size=2 验证。
- 已生效变更目标仍基于一次合同变更明细批量加载后切页；若历史规模增长，继续做聚合 SQL 分页。
- 独立验收初验发现连续翻页 P1，修复后复验 55/55 PASS；P1 已关闭。视觉签字仍未执行。

## 证据

- 开发证据：`docs/acceptance/runs/20261031-dq-x11-contract-change-range-picker/`
- 独立验收：`docs/acceptance/runs/20261031-dq-x8-x11-independent-acceptance/`

