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

## 发布后纠正（2026-10-24）

| 检查项 | 事实 | 状态 |
| --- | --- | --- |
| 后端代码与数据库依赖 | `e627bbb` 待谈单 SQL 引用 `cust_customer_loss` | 已补迁移 131 |
| 发布记录 | 原记录误写“无迁移、排除 124–131” | 已纠正 |
| 剩余迁移 | 124–130 未执行 | 明确排除 |

## DQ-X3：App 方案模版库浏览与实例化（UI_REVIEWED）

| 检查项 | 设计/业务规则 | 后端契约 | App 实现 | Web 实现 | 证据 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| 模板列表 | DEC-084/BR-040：浏览与筛选 | `design/template/page` | `templates.vue` 搜索/范围/状态筛选/分页 | 既有模板库 | App 真实页面、type-check/build；Provider shared | ALIGNED |
| 模板详情 | 区域、分项、公式；不含价格/成本 | `design/template/info` | `template-detail.vue` | 既有模板编辑器/详情 | 真实页面、无价格 API、白名单测试 | ALIGNED |
| 实例化 | 仅已发布版本；当前房屋；幂等快照 | `design/template/instantiate` | 详情页唯一主行动并回读方案 | 既有实例化 | 真实页面、DB 回读、继承测试 | ALIGNED |
| 真实渲染 | 七态与开发者工具证据 | - | PASS | 既有证据不替代本批 | `20261030-dq-x3-x5-app-template-acceptance` | PASS（物理真机未执行） |
| 完整双端 | 编辑/发布/停用/复制/区域分项维护 | 其余 `design/template/*` | 已实现 | 已实现 | Provider review `PARITY_REQUIRED=0` | ALIGNED |

- X3 初始只覆盖读取/实例化；X4/X5 完成后设计模板读写与版本 Provider 已全部共享。
- 对账结论：`UI_REVIEWED`；物理真机、独立验收仍待执行。


## DQ-X4：App 方案模版创建、区域/分项维护与发布（UI_REVIEWED）

| 检查项 | 设计/业务规则 | 后端契约 | App 实现 | Web 实现 | 证据 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| 模版创建 | Draft 草稿，归属范围受角色限制 | `design/template/save` | `template-edit.vue` 基础信息表单 | 既有创建抽屉 | App type-check/build；Provider shared | ALIGNED |
| 区域维护 | 草稿区域增删改；不承载房屋实测事实 | `design/template/room/save|delete` | 区域 sheet | 既有区域编辑 | App build；Provider shared | ALIGNED |
| 分项公式 | 引用报价项目库；公式/固定数量二选一 | `design/template/items/batchsave` | 目录选择 + 公式/数量配置 | 既有分项表 | App build；Provider shared | ALIGNED |
| 发布 | 至少一区域；公式/单位/引用校验 | `design/template/publish` | 唯一发布主行动 | 既有发布 | App build；Provider shared | ALIGNED |
| 复制/停用/版本 | App/Web 完整 | 对应 Provider | 已实现 | 已实现 | Provider review `PARITY_REQUIRED=0`；X5 真实页面 | ALIGNED |
| 真实渲染 | 七态、开发者工具证据 | - | PASS | 既有证据不替代本批 | `20261030-dq-x3-x5-app-template-acceptance` | PASS（物理真机未执行） |


## DQ-X5：App 已发布模版后续生命周期（UI_REVIEWED）

| 检查项 | 设计/业务规则 | 后端契约 | App 实现 | Web 实现 | 证据 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| 复制模板 | 复制为个人 Draft，源模板不变 | `design/template/copy` | 模板卡动作 | 既有复制 | App build；Provider shared | ALIGNED |
| 新建草稿版本 | 已发布版本不可变；草稿版本幂等 | `design/template/version/build` | 模板卡动作 | 既有版本入口 | App build；Provider shared | ALIGNED |
| 停用模板 | Disabled 停止新增实例化和编辑；历史报价不变 | `design/template/disable` | 二次确认动作 | 既有停用 | App build；Provider shared | ALIGNED |
| 真实渲染 | 动作面板与三条链路开发者工具证据 | - | PASS | 既有证据不替代本批 | `20261030-dq-x3-x5-app-template-acceptance` | PASS（物理真机未执行） |
| 房屋删除 | 独立高风险数据删除 | `property/property/delete` | 未实现 | 已实现 | GAP-WEB-025 OPEN | OUT_OF_SCOPE（不阻塞 DQ-X5） |

## DQ-X3 / DQ-X4 / DQ-X5 真实页面复核结论

- 微信开发者工具原生小程序：列表、详情、编辑、发布、实例化、复制、停用、新版本、错误态与权限态均 PASS。
- 修复 7 个真实页面/契约缺陷；最终列表/详情/方案页 console error=0。
- 证据：`th4-decoboon-app/docs/acceptance/runs/20261030-dq-x3-x5-app-template-acceptance/`。
- 临时数据清理完成，30 张相关表与备份基线计数一致，0 mismatch。
- 边界：物理真机未连接；`qwen2.5vl:7b` 只完成视觉机器预检，独立验收和最终视觉签字未执行。

## DQ-X6：财务报价服务端汇总（DEV_VERIFIED）

| 检查项 | 设计/业务规则 | 后端契约 | Web 实现 | App 实现 | 证据 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| 汇总来源 | DEC-036：聚合 SQL，不前端求和 | `quote/finance/summary` 单行 Criteria 聚合 | KPI 消费 summary | 不涉及 | H2 测试、API 对账 | ALIGNED |
| 金额口径 | 报价总额、成本完整部分预计毛利、成本未完整数 | 与 `quote/finance/report` 同谓词和公式 | 删除 3 个 `reduce` | 不涉及 | 分组汇总 = summary | ALIGNED |
| 数据范围 | DEC-065 财务与门店范围 | 复用 `DataScopeSpecification.visible` | 财务页面真实数据 | 不涉及 | 总部/门店 H2 用例 | ALIGNED |
| 状态过滤 | 与财务报价列表一致 | 复用 `status` | 页面当前默认全部 | 不涉及 | Accepted 定向用例 | ALIGNED |
| 分页一致性 | 列表 total 与汇总 count 可对账 | summary 不接收分页参数 | 表格分页使用 page total，KPI 使用 summary | 不涉及 | 本地 2=2 | ALIGNED |
| 页面功能 | 不改变布局，只替换数据源 | - | 1440/1280/1024、console 0 | - | `20261031-dq-x6-quote-finance-summary` | PASS（视觉未签） |

DQ-X6 无 `UNMAPPED`/`STALE`。独立验收未执行。

## DQ-X7：项目合同档案服务端汇总（DEV_VERIFIED）

| 检查项 | 设计/业务规则 | 后端契约 | Web 实现 | App 实现 | 证据 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| 合同行摘要 | DEC-036：聚合 SQL | `ContractWorkbench.lineCount/lineAmount` | 合同范围消费 workbench | 不涉及 | 6=6、¥977.80=¥977.80 | ALIGNED |
| 变更摘要 | BR-041/DEC-055 | `change.totalCount/confirmedAmount` | 合同变更消费 workbench | 不涉及 | 1=1、¥-168.00=¥-168.00 | ALIGNED |
| 角色范围 | 合同项目可见性 | `ProjectAccessContext` | Boss/Designer/StoreManager | 不涉及 | 三角色 API 对账 | ALIGNED |
| 成本边界 | BR-003 | workbench 按角色裁剪 | 页面沿用服务端字段 | 不涉及 | Designer cost=null | ALIGNED |
| 页面功能 | 只读合同档案 | - | 1440/1280/1024 | - | `20261031-dq-x7-contract-archive-summary` | PASS（视觉未签） |

DQ-X7 无 `UNMAPPED`/`STALE`。合同行详情分页仍作为独立后续切片。

## DQ-X8：合同基线行数据库分页（DEV_VERIFIED）

| 检查项 | 设计/业务规则 | 后端契约 | Web 实现 | App 实现 | 证据 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| 真实分页 | DEC-036：数据库切页 | `Pageable`，默认 20、上限 50 | 请求 current/size，消费 Page total | 不涉及 | 单测 + size=2 双页 | ALIGNED |
| 稳定顺序 | 同一合同排序确定 | `sort ASC, lineId ASC` | 当前页不重排 | 不涉及 | 单测 + API IDs | ALIGNED |
| 汇总口径 | 行数为全量事实，不按当前页求 | workbench `lineCount/lineAmount` | 头部消费 workbench | 不涉及 | 6 / ¥977.80 | ALIGNED |
| 成本边界 | BR-003 | 当前页按角色裁剪 | 页面不展示成本 | 不涉及 | Designer cost 字段 0 | ALIGNED |
| 变更来源 | DEC-055 当前有效剩余 | `contract-change/range` | 删除合同行回退 | 不涉及 | 7 来源；无 line/page | ALIGNED |
| 页面功能 | 只读合同档案 + 增减项来源 | - | 1440/1280/1024、console 0 | - | `20261031-dq-x8-contract-line-pagination` | PASS（视觉未签） |

DQ-X8 无 `UNMAPPED`/`STALE`。真实数据不足 20 行，页面翻页交互未在真实数据下触发；独立验收未执行。

## DQ-X9：业主与小程序合同行分页同源（DEV_VERIFIED）

| 检查项 | 设计/业务规则 | 后端契约 | App 实现 | Web 实现 | 证据 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| 业主分页 | DEC-036：数据库切页 | 默认 20、上限 50、稳定排序 | 显式 current/size，滚动加载下一页 | 不涉及 | 单测 + 业主 API 双页 | ALIGNED |
| 全量摘要 | 分页行不等于合同全量事实 | workbench `lineCount/lineAmount` | 头部消费 workbench | 不涉及 | 6 项 / ¥977.80 | ALIGNED |
| 成本边界 | BR-003 | 业主合同行/workbench 不返回 cost | 页面无成本字段 | 不涉及 | 业主 API cost=0 | ALIGNED |
| 失败恢复 | 不用空数组伪装 | Provider 正常返回 | 首屏错误可重试 | 不涉及 | 源码 + 门禁 | ALIGNED |
| 分页回归门禁 | 跨端契约重复发生 | - | 5/5 负向样本被拦截 | - | `app-contract-line-gate-self-test.log` | ALIGNED |
| 原生页面 | 合同范围真实回读 | - | 微信开发者工具 4/4 PASS | - | `app-contract-native.png` | PASS（视觉未签） |

DQ-X9 无 `UNMAPPED`/`STALE`。真实合同不足 20 行，原生页面下一页交互未触发；独立验收未执行。

## DQ-X10：合同读模型成本聚合（DEV_VERIFIED）

| 检查项 | 设计/业务规则 | 后端契约 | Web/App | 证据 | 状态 |
| --- | --- | --- | --- | --- | --- |
| 成本合计 | BR-003：必备成本缺失即不完整 | 单条 aggregate 返回 count/sum/incomplete | 响应字段不变 | SQL + 单测 | ALIGNED |
| 读模型 | DEC-036：禁止全量实体循环求和 | enrich 不调用全量合同行查询 | 不涉及 | 3 条定向用例 | ALIGNED |
| 空合同 | 不得伪装成本完整 | count=0 时 false | 不涉及 | 定向用例 | ALIGNED |
| 业务响应 | 合同金额和汇总不变 | 6 行/¥977.80、1 变更/¥-168.00 | 不涉及 | 三角色 API | ALIGNED |
| 角色边界 | Designer 不返回成本对象 | 保持既有白名单 | 不涉及 | API | ALIGNED |
| 全量门禁 | DQ-X6 新 Provider 必须纳管 | summary context 已登记，面板 543 | 不涉及 | 1184/1184 | ALIGNED |
| 数据安全 | 只读改造不得写合同行 | checksum 前后一致 | 不涉及 | `db-baseline/after.txt` | ALIGNED |

DQ-X10 无 `UNMAPPED`/`STALE`。未取得端到端延迟改善，不声明性能收益；独立验收未执行。

## DQ-X11：增减项来源选择器分页与批量快照（ACCEPTED）

| 检查项 | 设计/业务规则 | 后端契约 | Web/App | 证据 | 状态 |
| --- | --- | --- | --- | --- | --- |
| Picker 分页 | DEC-036：服务端 current/size | 默认 20、上限 50、total/hasMore | Web 分页器；App 滚动加载 | API + 单测 | ALIGNED |
| 服务端筛选 | 关键词/类型/区域/分组 | keyword/itemType/spaceName/group | 两端发送筛选参数 | 真实页面 | ALIGNED |
| 批量快照 | 来源剩余/占用/可变更口径不变 | 变更明细每请求只加载一次 | 展示服务端结果 | 单测 + API | ALIGNED |
| Facets | 不用当前页伪造全局类型/区域计数 | group by + 单批投影 | 两端消费服务端 facets | API + 页面 | ALIGNED |
| 错误边界 | 非法筛选不泄露数据 | 业务 400 | 页面可重试 | API | ALIGNED |
| 跨端门禁 | 防止退回全量来源 | - | Web/App 各 4/4 负向 | 门禁日志 | ALIGNED |
| 原生页面 | 真实来源选择链 | - | 2/2 PASS | 小程序截图/JSON | PASS（视觉未签） |

DQ-X11 无 `UNMAPPED`/`STALE`。独立验收初验发现连续翻页 P1，修复后复验 55/55 PASS，P1 已关闭。已生效变更目标仍基于一次批量加载结果切页；视觉签字和业主直接黑盒未执行。

## UI-QF-01：设计队列同族降噪（DEV_VERIFIED）

| 检查项 | 设计/业务规则 | Web 实现 | Provider/状态 | 证据 | 状态 |
| --- | --- | --- | --- | --- | --- |
| 待我跟进 | 保留客户、跟进时机、风险、下一步、写跟进 | 删除页头/引导重复眉题和完成说明 | 未修改 | 精确 DOM + 1170/1684/2116px 三档 | ALIGNED |
| 待我量房 | 保留房屋、风险、主行动、真实阻断 | 删除重复眉题、下一步二次展示和泛化完成提示 | 未修改 | eyebrow=0、inlineNext=0、三档无溢出 | ALIGNED |
| 待我签约 | 保留报价、合同状态、主行动、下一责任人 | 删除重复眉题、完成说明和重复刷新 | 未修改 | eyebrow=0、nextOwner=true、三档无溢出 | ALIGNED |
| 动作完整性 | 三页既有合法动作不得缺失 | 写跟进、量房主/次动作、签约主行动及辅助入口保留 | 未修改 | DOM/页面证据 | ALIGNED |

UI-QF-01 无 `UNMAPPED`/`STALE`。未独立验收；机器视觉误报不作为通过或失败依据。
