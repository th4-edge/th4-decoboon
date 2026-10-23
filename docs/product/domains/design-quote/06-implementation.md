# 设计方案与设计报价审视 · 实现记录

## 本批范围

- Batch：B1
- 垂直切片：设计师与经营角色看到同一方案的当前价格，并完成企业运营视角的只读查看
- 不包含：审批、核价、退回、通过/拒绝流程；报价过期判断；合同/项目/成本核算改造

## 变更

- 后端：
  - `Design` 增加服务端价格只读投影：方案计算价、区域数、分项数、当前报价摘要；
  - `DesignPriceContext` 批量解析当前报价：`Converted > Accepted > 最新非 Superseded`；
  - `DesignItemRepository`、`SpaceRepository` 使用聚合查询，避免逐方案 N+1；
  - `design/scheme/page` 增加 `ownerId/storeId/keyword` 过滤与价格投影；
  - `design/scheme/info` 增加价格投影和当前报价；
  - `DesignCostContext` 对设计师、业务员、项目经理隐藏 `costPrice/cost`；
  - `statistic/workbench/design/page` 增加最近方案、价格来源、当前报价金额/状态。
- Web：
  - `DesignSchemeRail` 增加正式报价/方案计算价、报价状态；
  - 新增 `DesignReviewView` 与 `DesignReviewDetailView`；
  - Boss/StoreManager 增加 `/workbench/design-reviews`；
  - Finance 增加 `/finance/design-reviews`；
  - 页面只读，不新增任何写动作或状态。
- App：
  - 设计工作台卡片增加当前价格、报价编号和状态；
  - 方案列表无正式报价时回退显示方案计算价；
  - 新增 `design/review` 与 `design/review-detail` 只读页面；
  - Boss/店长/财务增加 `view:design-review` 只读能力，设计师直链由路由和页面双层拦截；
  - 金额展示统一使用 `JzMoney`，修复小于 1 万元时出现 `¥¥82` 的重复货币符号；
  - 移除小程序不兼容的 `> :nth-child(3)` WXSS 选择器，修复分包页面原生编译失败；
  - Boss/店长/财务增加设计方案运营查看入口。
- 迁移：无 DDL、无数据迁移。
- 配置：新增 App 页面路由与 Web 角色静态路由；无新 Provider 路由。

## 代码版本

| 工程 | dev 集成提交 | 隔离实施提交 |
| --- | --- | --- |
| th4-decoboon | `189843f` | `4ffb097` |
| th4-decoboon-web | `2351ce2` | `1e0c0a8` |
| th4-decoboon-app | `7697a8c` | `773833e` |

## 测试与证据

- 定向测试：`DesignPriceContextTest`、`DesignCostContextTest`、`DesignSchemeInfoFormulaTest`、`DesignerWorkbenchAggregationTest`，13/13 PASS。
- 后端全量：`mvn -o test`，901/901 PASS（本轮修复后复跑）。
- Web：`npm run build`、工作台可达、权限、表单门禁 PASS。
- App：`type-check`、UI、导航、设计分项、动作、分包门禁 PASS；`build:mp-weixin:release` PASS。
- App 构建体积：主包 937071 B，最大分包 664803 B，总包 3701188 B，17 分包/167 页。
- 真实页面：
  - Web BOSS/店长/财务列表与详情、设计师方案执行页均已真实渲染，1440/1280/1024 无横向溢出；
  - App H5 同源页面以 390×844 完成 BOSS/店长/财务/设计师复核；
  - 微信开发者工具原生页面完成 BOSS/店长/财务列表与详情、设计师工作台和直链拦截冒烟，截图见 `docs/acceptance/runs/20261021-design-quote-ui/`。
- 跨范围 HTTP 负向：临时二店店长/门店财务列表为 0、详情 404、跨店筛选 403；其他企业列表为 0、详情 404；本地临时数据已恢复。
- 性能采样：列表 P95 145.4ms、详情 P95 132.6ms（各 10 次）；5→50 方案 10 倍验证中，基线 P95 54.2ms、50 方案 P95 72.1ms，查询结构固定。
- 凭据恢复：BOSS/店长临时登录哈希已按备份恢复，其他企业凭据与临时二店已回滚。
- 部署/重启：未发布生产。
- 截图/日志：待 UI 审查批次补齐。

## 风险与未闭环

1. `GAP-061` 未关闭：方案变更后正式报价是否过期尚无稳定修订令牌口径；本批不展示“报价已过期”结论。
2. 独立验收尚未签署；开发侧证据不能替代 `ACCEPTED`。
3. 本批未发布 test/prod；App 正式版仍由用户本人提审发布。

## DQ-X1：待我谈单工作队列查询整改（2026-10-24）

- Batch：`DQ-X1`
- 垂直切片：待我谈单队列从全量报价加载、Java 过滤排序、内存切页，改为数据库 ID 分页和当前页实体批量预取。
- 范围：仅 `statistic/workbench/quote/page`、`quote/designer/page` 及其测试；不改变报价状态、金额、风险文案或前端交互。

### 实现

- `QuoteRepository#findWorkbenchQuoteIds`：在 SQL 内完成企业/门店/本人范围、Draft/Submitted/Rejected、Dormant 排除、关键词、状态筛选、业务优先级和停留时间排序，返回稳定 ID 页。
- `QuoteRepository#findWorkbenchQuotes`：按当前页 ID 批量预取 Quote、Customer、Property，避免逐报价查询客户/房屋。
- `statistic/workbench/quote/page`：固定为 1 次 ID 分页查询 + 1 次页面实体批量查询；默认 20 条，服务端上限 100 条。
- 权限：仅 `Boss / StoreManager / Designer`；Boss 企业范围、店长必须归属本店、设计师仅本人创建报价；其他角色在查询前返回 403。
- `quote/designer/page`：同步改为 `Specification + Pageable` 数据库分页，支持 `customerId / propertyId / designId / status / keyword`，默认 20 条、上限 100 条。
- 迁移：无 DDL、无数据迁移。

### 验证

- `DesignerQuoteWorkbenchPaginationTest`、`DesignerWorkbenchAggregationTest`、`DesignerQuoteStoreManagerScopeTest` 定向测试 PASS。
- 后端全量：停止本地后端、移走旧 `target` 后执行干净 `mvn test`，**1098/1098 PASS，0 failures / 0 errors / 0 skipped**。
- 测试覆盖：默认范围参数、Boss 企业范围、店长本店范围、无门店店长空结果、非授权角色 403、页大小 500 被截为 100、ID 顺序恢复、单次批量预取。
- 本地 MySQL 8.0.24 `EXPLAIN ANALYZE`：20 条页查询命中 `quote` 企业索引并回表 `cust_customer/db_property` 主键；当前样本 23 条候选、20 条返回，实际约 2.6ms，查询结构固定。
- 真实 HTTP 验收：`docs/acceptance/runs/20261024-dq-x1/run-http-acceptance.mjs`，17/17 PASS；Boss/店长 22 条、设计师本人 14 条、Draft 13 条，10 次 P95 38.6ms。
- 真实页面：Web 1440/1280 完成设计师、Boss、店长、Finance 四角色回归；Boss 翻页后总数保持 22；Finance 直链进入 `/forbidden`；页面 console 0 error。截图见 `docs/acceptance/runs/20261024-dq-x1/screenshots/`。
- 验收中修复：设计师范围参数导致本人任务为 0；`CustomerHeroCard.vue` 未定义 `$danger-600`；`DesignerMeasureTasksView.vue` 房屋 ID 类型不匹配。
- Web 构建：`npm run build` PASS，包含分页门禁、type-check、客户端文案门禁和 Vite production build。
- 干净全量回归已通过；独立验收尚未执行，本批不标记 `ACCEPTED`。

## DQ-X2：设计师五阶段 App/Web 统一（2026-10-24）

- Batch：`DQ-X2`
- 垂直切片：统一设计师五阶段名称、顺序、筛选语义、关键词检索和 App 触底加载参数。
- 不包含：Provider、权限、状态机、金额、数据库和 Web 分页结构变更。

### 实现

- App 设计师入口与页面标题统一为 `待我跟进 → 待我量房 → 待我设计 → 待我谈单 → 待我签约`。
- App 待我量房筛选项 `全部` 统一为 `全部任务`。
- App 待我谈单筛选值统一为 Web 的 `all / draft / submitted / rejected`。
- App 待我签约筛选值统一为 Web 的 `all / toCreate / waitingConfirm / effective`。
- App 待我谈单和待我签约新增 320ms 防抖关键词搜索；首屏、刷新和触底加载携带相同筛选值与关键词。
- App 谈单、签约筛选条改为横向滚动，避免窄屏挤压。
- 五页继续使用下拉刷新第 1 页和 `onReachBottom` 追加下一页；不展示页码控件。

### 验证

- 微信开发者工具原生自动复核 24/24 PASS，0 error。
- Web 真实 Chromium 自动复核 20/20 PASS，Console error 0。
- 谈单搜索 `周先生` 返回空队列；签约搜索 `王亮` 仅返回王亮，证明检索由服务端同源执行。
- 谈单筛选中草稿返回 `Q1790383677036`，待客户确认/需调整为空；签约筛选中待发起合同/待企业确认/已生效分别返回周先生/空/王亮。
- App `type-check`、UI、文案、导航、分页门禁与 `build:mp-weixin:release` PASS。
- Web `build`、工作台、工作空间和权限门禁 PASS。
- Web 真实 Chromium 以同一设计师账号完成五页复核，侧栏顺序、H1、筛选文案和文档标题一致；截图见同目录验收记录。
- 证据：`docs/acceptance/runs/20261024-designer-five-stage-unified/README.md`。

### 验收结论

- 试运营单人模式只读验收 PASS；第三方独立验收未执行，不冒充独立验收。
- 本轮无业务写操作、无数据库变更。
- 小程序未上传、未提审、未发布；正式版仍由用户本人操作。

## 发布前复核（2026-10-24）

- 结论：`BLOCKED / DO_NOT_RELEASE`，证据见 `docs/acceptance/runs/20261024-dq-x1-x2-release-readiness/`。
- 阻断不在 DQ 功能验收，而在候选不可唯一识别：后端 271 M / 3 D / 308 ??；Web 71 M / 21 ??；App 84 M / 21 ??。
- Web 本地 `dev` 相对 `origin/dev` ahead 1 / behind 5。
- 当前工作区包含迁移 124–131 与对应 rollback；与 DQ-X1/DQ-X2 无直接关系，但发布包会带上。
- App 版本仍为 `1.0.66 / 166`，不能直接用于新体验版/正式版。
- 未执行提交、推送、test 同步、生产部署或小程序上传。
