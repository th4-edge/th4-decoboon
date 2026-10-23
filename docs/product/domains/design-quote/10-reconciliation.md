# 设计方案与设计报价审视 · 设计—蓝图—实现—页面—验收对账

状态：`ACCEPTED`

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
- App 移除不兼容的 `> :nth-child(3)` WXSS 选择器，修复原生分包打开失败。

## DQ-X1：待我谈单查询整改（ACCEPTED）

| 检查项 | 业务/技术规则 | 实现 | 证据 | 状态 |
| --- | --- | --- | --- | --- |
| 数据库分页 | 不加载全量报价后内存切页 | `findWorkbenchQuoteIds` ID 分页，默认 20、上限 100 | `DesignerQuoteWorkbenchPaginationTest` | PASS |
| 服务端筛选排序 | 状态、关键词、Dormant、报价优先级均在 SQL 完成 | Native SQL 条件与 `ORDER BY` | `EXPLAIN ANALYZE` + Provider 测试 | PASS |
| N+1 控制 | 页面客户/房屋不得逐行查询 | 当前页 ID 批量预取 `customer/property`，固定 2 次仓储查询 | Mockito 批量查询断言 | PASS |
| 权限范围 | BOSS 企业、店长本店、设计师本人；其他角色拒绝 | `TenantContext.requireRole` + SQL 范围参数 | 4 个角色/门店测试 | PASS |
| 响应裁剪 | 只返回工作队列页面实际字段 | Provider `@Properties` 白名单 | `DesignerQuoteCostContractTest` | PASS |
| 深链 | `quoteId` 定位目标报价 | SQL 精确筛选目标 ID | Provider 测试 + 页面契约 | PASS |
| 真实 HTTP | 四角色、分页、筛选与性能 | dev API + MySQL 独立计数 | `docs/acceptance/runs/20261024-dq-x1/http-results.json` | PASS 17/17 |
| 真实页面 | 设计师/Boss/店长可见，Finance 403；Boss 可翻页 | Web 1440/1280 | `docs/acceptance/runs/20261024-dq-x1/screenshots/` | PASS |
| 独立验收 | 研发不得自批 | 18/18 PASS；固定 3 条 SQL；无 N+1；跨企业/门店与无令牌拒绝 | `docs/acceptance/runs/20261024-dq-x1-independent/` | PASS |

真实 UI 渲染、干净全量回归与独立验收均已完成，本批进入 `ACCEPTED`。

## 验收集成提交对账（2026-10-22）

| 端 | 最新 dev | 本轮内容 | 结论 |
| --- | --- | --- | --- |
| 后端 | `189843f` | 设计报价 B1 实现、UI 复核、对账与独立验收均已进入 dev | ALIGNED |
| Web | `2351ce2` | `889c3b4` 为祖先提交，路由、角色入口和查看页均在 dev | ALIGNED |
| App | `7697a8c` | 通过合并包含 `e2d4232`，运营查看、金额、权限和原生 WXSS 修复均在 dev | ALIGNED |

- 需求映射：`REQ-001`～`REQ-009`，需求 9 条、对账 9 条，无缺失或多余。
- 蓝图映射：`calculatedAmount`、`currentQuoteAmount`、`priceDisplayMode`、报价优先级、成本裁剪、App/Web/Backend UI 绑定均有实现和证据。
- 页面映射：App `review/review-detail/workbench`、Web `DesignReviewView/DetailView`、后端 3 个读接口均在最新 dev。
- 验收映射：跨店/跨企业 JSON、性能 CSV/JSON、Web/App 原生截图均在最新后端 dev。
- 无关变更：最新 dev 另含财务列表分页与汇总发布，不改变本域对象、权限或金额口径。

## 本批结论

- 已对齐：价格主口径、当前报价、只读运营查看、成本角色裁剪、无审批状态、执行/经营入口分离。
- 已通过：后端全量回归、Web 真实渲染、App 微信开发者工具原生页面、release 构建、跨店/跨企业 HTTP 负向、10 倍数据量性能。
- 独立验收：2026-10-22 完成，24/24 黑盒检查通过，见 `docs/acceptance/runs/20261022-design-quote-independent/`。
- 下一批：进入发布准备；生产发布需用户明确确认，小程序正式版仍由用户本人提审和发布。

## DQ-X2：设计师五阶段 App/Web 统一（ACCEPTED）

| 检查项 | 设计/业务规则 | 后端契约 | App 实现 | Web 实现 | 证据 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| 阶段名称与顺序 | 统一 `待我跟进 → 待我量房 → 待我设计 → 待我谈单 → 待我签约` | 五个既有 Provider 不变 | 首页入口与五页标题统一 | 菜单与页面标题统一 | App 24/24 审计；Web 1440×900 五页复核 | ALIGNED |
| 跟进语义 | 全部待办/已逾期/今日/3天内/未安排 | `customer/my-customer/overview` | `keyword + followFilter`，下拉+触底 | 同 Provider、分页组件 | App 审计、Web 构建 | ALIGNED |
| 量房语义 | 全部任务/逾期/今天/已预约/待预约/待交设计 | `statistic/designer/measure/page` | `keyword + state`，下拉+触底 | 同 Provider、分页组件 | App 审计、Web 构建 | ALIGNED |
| 设计语义 | 全部/待设计/设计中/方案完成 | `statistic/workbench/design/page` | `keyword + filter`，下拉+触底 | 同 Provider、分页组件 | App 审计、Web 构建 | ALIGNED |
| 谈单语义 | 全部任务/草稿/待客户确认/需调整 | `statistic/workbench/quote/page` | `all/draft/submitted/rejected + keyword`，下拉+触底 | 同值筛选 + 搜索 + 分页 | App 搜索/筛选实测、Web 构建 | ALIGNED |
| 签约语义 | 全部/待发起合同/待企业确认/已生效 | `statistic/workbench/sign/page` | `all/toCreate/waitingConfirm/effective + keyword`，下拉+触底 | 同值筛选 + 搜索 + 分页 | App 搜索/筛选实测、Web 构建 | ALIGNED |
| 无旧分页控件 | App 仅下拉刷新和触底加载 | `Page.current/size` | 无上一页/下一页/页码/总页数 | Web 保留分页组件 | App 24/24 审计 | ALIGNED |
| 验收 | 试运营单人模式只读黑盒 | - | App 24/24 | Web 20/20 | `acceptance-summary.json` | PASS |
| 独立验收 hat | 研发侧之外只读复跑 | 五个既有 Provider | App 24/24；API 103/103 | Web 6/6；构建/权限门禁 PASS | `20261024-designer-five-stage-unified-independent/` | PASS |

本批进入 `ACCEPTED`。App/Web 均完成真实页面复核并命中同一批业务样本；独立验收 hat 已完成只读复跑，无 P0/P1。第三方独立验收未执行，不冒充第三方验收。

## 发布前复核（BLOCKED）

| 检查项 | 期望 | 实际 | 状态 |
| --- | --- | --- | --- |
| 唯一发布提交 | 三仓工作树干净，目标提交明确 | 后端 582 项、Web 92 项、App 105 项未提交 | BLOCKED |
| 分支同步 | test/prod 可从已验收提交快进 | Web `dev` ahead 1 / behind 5；test/prod 落后 | BLOCKED |
| 变更范围 | 仅 DQ-X1/DQ-X2 | 当前树混入迁移 124–131 及大量其他业务改动 | BLOCKED |
| App 版本 | 新构建使用新版本号 | 仍为 `1.0.66 / 166` | BLOCKED |
| test 冒烟 | 独立 test 环境执行 | 无独立远端 test 部署入口 | BLOCKED |
| 生产授权 | 用户明确授权 | 尚未授权 | BLOCKED |

- 证据：`docs/acceptance/runs/20261024-dq-x1-x2-release-readiness/readiness.json`。
- 处理：先做发布范围决策与干净提交拆分；不得直接执行 `deploy-prod.sh` 或小程序正式发布。
