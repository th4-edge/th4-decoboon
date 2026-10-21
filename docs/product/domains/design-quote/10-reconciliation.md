# 设计方案与设计报价审视 · 设计—蓝图—实现—页面—验收对账

状态：`UI_REVIEWED`

| REQ | 场景 | 业务规则 | 蓝图节点 | 字段/API | 页面 | 测试 | 验收 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-001 | 服务端统一价格 | DEC-078 | priceProjection | `calculatedAmount/currentQuote*` | 无 | DesignPriceContextTest | Web/App 同源列表与详情金额一致 | PASS |
| REQ-002 | App 设计工作台看价 | DEC-078 | priceProjection | `statistic/workbench/design/page` | App 设计工作台 | 原生页面、type-check、release build | `11.8万` 正常显示；无成本/毛利 | PASS |
| REQ-003 | Web 方案卡看价 | DEC-078 | priceProjection | `design/scheme/page` | `DesignSchemeRail` | Web build、真实渲染 | 1440/1280/1024 价格正常 | PASS |
| REQ-004 | Web 经营只读查看 | DEC-078/DEC-065/DEC-075 | uiBindings | `design/scheme/page|info` | Boss/店长/财务查看页 | Web build、三角色走查 | 价格/成本/分项/只读动作均符合 | PASS |
| REQ-005 | App 经营只读查看 | DEC-078 | uiBindings | `design/scheme/page|info` | App review/detail | 原生三角色页面、H5 复核、release build | BOSS/店长/财务列表/详情均通过 | PASS |
| REQ-006 | 成本角色裁剪 | BR-003/DEC-065 | cost-visible | `DesignCostContext` | 详情成本列 | 单测 + HTTP + 渲染 | 设计师 0 成本字段；三经营角色可见 | PASS |
| REQ-007 | 当前报价统一选择 | DEC-078/DEC-002 | current-quote | QuoteRepository + DesignPriceContext | 列表/详情 | DesignPriceContextTest | `Converted` 正式报价优先且版本稳定 | PASS |
| REQ-008 | 聚合性能 | BR-031 | priceProjection | 3 条批量聚合查询 | 列表 | 基线/50 方案各 8 次 | 5 方案 P95 54.2ms；50 方案 P95 72.1ms；查询结构固定 | PASS |
| REQ-009 | 执行/经营入口分离 | DEC-078 | uiBindings | `view:design-review` | App/Web 运营查看 | 路由、H5 直链、Web forbidden | 设计师直链回退；三经营角色可达 | PASS |

## 未映射

- 无。

## 已过期

- 无。

## 未决业务问题

- `GAP-061`：设计内容修订令牌与报价过期判断；本批不实现、不展示“报价已过期”。

## 本轮修复

- Web/App 详情补 `store` 白名单，修复“门店待确认”。
- App 列表/详情/工作台改用 `JzMoney`，修复 `¥¥82`。
- App 新增 `view:design-review`，修复设计师运营查看深链越权。

## 本批结论

- 已对齐：价格主口径、当前报价、只读运营查看、成本角色裁剪、无审批状态、执行/经营入口分离。
- 已通过：后端全量回归、Web 真实渲染、App 微信开发者工具原生页面、release 构建、跨店/跨企业 HTTP 负向、10 倍数据量性能。
- 未通过：独立验收签署。
- 下一批：保持 `UI_REVIEWED`，进入独立验收；采纳后推进 `ACCEPTED`，不得由开发侧自签。
