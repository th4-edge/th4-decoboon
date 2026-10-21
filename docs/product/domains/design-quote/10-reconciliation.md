# 设计方案与设计报价审视 · 设计—蓝图—实现—页面—验收对账

状态：`DEV_VERIFIED`

| REQ | 场景 | 业务规则 | 蓝图节点 | 字段/API | 页面 | 测试 | 验收 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-001 | 服务端统一价格 | DEC-078 | priceProjection | `calculatedAmount/currentQuote*` | 无 | DesignPriceContextTest | 待独立验收 | DEV_VERIFIED |
| REQ-002 | App 设计工作台看价 | DEC-078 | priceProjection | `statistic/workbench/design/page` | App 设计工作台 | type-check/UI/构建 | 待真实账号 | DEV_VERIFIED |
| REQ-003 | Web 方案卡看价 | DEC-078 | priceProjection | `design/scheme/page` | `DesignSchemeRail` | Web build/门禁 | 待渲染 | DEV_VERIFIED |
| REQ-004 | Web 经营只读查看 | DEC-078/DEC-065/DEC-075 | uiBindings | `design/scheme/page|info` | Boss/店长/财务查看页 | Web build/可达性 | 待三角色走查 | DEV_VERIFIED |
| REQ-005 | App 经营只读查看 | DEC-078 | uiBindings | `design/scheme/page|info` | App review/detail | type-check/导航/构建 | 待真机 | DEV_VERIFIED |
| REQ-006 | 成本角色裁剪 | BR-003/DEC-065 | cost-visible | `DesignCostContext` | 详情成本列 | DesignCostContextTest | 待 HTTP/渲染 | DEV_VERIFIED |
| REQ-007 | 当前报价统一选择 | DEC-078/DEC-002 | current-quote | QuoteRepository + DesignPriceContext | 列表/详情 | DesignPriceContextTest | 待真实多版本数据 | DEV_VERIFIED |
| REQ-008 | 聚合性能 | BR-031 | priceProjection | 3 条批量聚合查询 | 列表 | 代码审查/全量测试 | 待10倍数据压测 | PENDING_PERF |

## 未映射

- 无。

## 已过期

- 无。

## 未决业务问题

- `GAP-061`：设计内容修订令牌与报价过期判断；本批不实现、不展示“报价已过期”。

## 本批结论

- 已对齐：价格主口径、当前报价、只读运营查看、成本角色裁剪、无审批状态。
- 待对齐：真实 UI 渲染、跨角色 HTTP/真机验收、性能测量。
- 下一批：进入 `UI_REVIEWED` 前完成真实渲染和缺陷修正；报价过期规则待 `GAP-061` 裁决。
