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

## 发布后纠正（2026-10-24）

- 生产 commit `e627bbb` 的待谈单 SQL 引用 `cust_customer_loss`，原发布记录“无迁移、排除 124–131”不正确。
- 生产已补执行迁移 131，备份为 `/opt/backups/decoboon/migration-131-20261024-143402/decoboon-before-131.sql.gz`，SHA-256 `0ac0598f...26ed2b`。
- 迁移 124–130 未执行；发布记录已纠正。

## DQ-X3：App 方案模版库浏览与实例化（2026-10-30）

- Batch：`DQ-X3`
- 垂直切片：设计师在 App 从方案模版库选择已发布模板，查看区域/分项/公式并实例化到当前客户房屋。
- 不在本批：模板新建、编辑、发布、停用、复制、区域/分项维护和版本编辑。

### Provider 与客户端

- App 新增 `src/domains/designtemplate/model.ts`、`src/domains/designtemplate/api.ts`。
- App 复用唯一 Provider：
  - `design/template/page`
  - `design/template/info`
  - `design/template/instantiate`
- App 新增：
  - `subpackages/design/pages/templates.vue`
  - `subpackages/design/pages/template-detail.vue`
- App 设计转化页新增「从方案模版新建」入口，携带 `customerId/propertyId`。
- 权限新增 `view:design-template`、`edit:design-template`，仅 Boss/StoreManager/Designer 具备；服务端继续按企业/门店/个人范围裁剪。
- 模板页面不展示价格、成本、供应商历史价或毛利；实例化只提交 `templateId/propertyId/name`，成功后回读方案详情。
- 模板列表支持全部/已发布/草稿/已停用筛选；详情要求模板与当前版本均为 `Published` 才允许实例化；草稿/停用/缺房屋上下文显示阻断原因。

### 验证

- App `npm run type-check` PASS。
- App `check:subpackages`、`check:navigation`、`check:ui`、`check:client-copy`、`check:pagination` PASS。
- App `npm run build:mp-weixin:release` PASS；主包 1,075,678 B、最大分包 734,997 B、总包 4,186,107 B，178 页。
- 后端 `DesignTemplateWhitelistContractTest`、`DesignTemplateInstantiateInheritanceTest`、`DesignTemplateFormulaContextTest` PASS。
- Provider 巡检：`design/template/page|info|instantiate` 已归为 App/Web 共享；剩余模板编辑/发布等 10 项保持 `PARITY_REQUIRED`。
- 真实开发者工具页面已完成，证据见 `th4-decoboon-app/docs/acceptance/runs/20261030-dq-x3-x5-app-template-acceptance/`；仍未进行物理真机与独立验收。


## DQ-X4：App 方案模版创建、区域/分项维护与发布（2026-10-30）

- Batch：`DQ-X4`
- 垂直切片：设计师/Boss/店长在 App 新建模版草稿，维护区域、分项公式或固定数量，并发布个人/门店/企业模版。
- 不在本批：复制、停用、已发布模板新建版本和历史版本管理。

### 实现

- App 模板库增加状态筛选和“新建”入口。
- App 新增 `template-edit.vue`：
  - 创建模版基础信息；
  - 草稿区域增删改；
  - 报价项目库分项选择与公式/固定数量维护；
  - 发布前区域/分项门禁。
- App 领域 API 增加 `save/publish/saveRoom/deleteRoom/batchSaveItems`，全部复用既有 Web Provider。
- Web 清理未使用的 `design/template/item/save|delete` 单行包装；两端统一使用 `design/template/items/batchsave`。
- 前端不展示或提交模版价格、成本、供应商历史价或毛利。

### 验证

- App `type-check`、分包/导航/UI/文案/分页门禁 PASS。
- App release 构建 PASS：主包 1,077,007 B、最大分包 734,997 B、总包 4,220,290 B、180 页。
- Web `type-check` 与完整构建 PASS。
- Provider 巡检：模板 save/publish/room/save/room/delete/items/batchsave 为 shared；剩余模板写能力仅 copy、disable、version/build。
- 真实模板编辑器、区域/分项维护、发布确认与发布后只读已在微信开发者工具复核。


## DQ-X5：App 已发布模版后续生命周期（2026-10-30）

- Batch：`DQ-X5`
- 垂直切片：模板卡“更多”动作面板支持复制为个人草稿、停用已发布模板、为已发布模板新建草稿版本。
- 不在本批：物理真机与独立验收，以及独立于设计模板的房屋删除语义。

### 实现

- 模板库卡新增“更多”动作，按角色/归属展示可用动作。
- 复制：`design/template/copy`，进入新个人草稿。
- 新版本：`design/template/version/build`，已有草稿时由服务端幂等复用。
- 停用：二次确认后调用 `design/template/disable`，历史报价不变。
- 领域 API 新增 copy/disable/buildVersion；App/Web 设计模板读写与版本 Provider 已全部共享。

### 验证

- App `type-check`、模板契约、导航、UI、文案、分页门禁 PASS。
- App release 构建 PASS：主包 1,077,228 B、最大分包 734,997 B、总包 4,222,981 B、180 页。
- Provider 巡检：设计模板 Provider 共享完成，剩余 Web-only 写能力仅 12 项、open review 1 项（房屋删除）。
- 真实动作面板、复制、停用、新版本链路已在微信开发者工具复核。

## DQ-X3 / DQ-X4 / DQ-X5 · 真实页面复核修复（2026-10-30）

- 真实页面发现并修复：
  - 模板编辑页补挂 `JzConfirmDialog`，恢复发布/删除确认反馈；
  - 详情页区域类型由内部枚举映射为中文业务文案；
  - 新版本和复制完整复制 `referenceArea/referencePerimeter`；
  - 模板编辑/详情状态标签改用当前版本状态；
  - 模板分页“全部状态”不再默认排除 `Disabled`；
  - 模板相关 Provider 的 `CatalogItem` 白名单移除销售价格字段；
  - 详情页英文 eyebrow 改为“方案模版”。
- 后端定向测试：`DesignTemplateWhitelistContractTest`、`DesignTemplateVersionCopyTest`、`DesignTemplateInstantiateInheritanceTest`、`DesignTemplateFormulaContextTest` PASS。
- App：`type-check`、模板/导航/UI/文案/分包门禁与 `build:mp-weixin` PASS；主包 1,077,077 B、最大分包 734,997 B、总包 4,223,451 B、180 页。
- 真实页面最终列表/详情/方案页 console error=0。
- 清理：临时员工、客户/房屋、2 个模板及 4 个版本、1 个实例化方案、测试报价项目已删除；30 张相关表与备份基线计数一致，0 mismatch。
- 边界：物理真机未连接；视觉只有 `qwen2.5vl:7b` 机器预检，最终视觉签字和独立验收仍待执行。

## DQ-X6 财务报价服务端汇总（2026-10-31）

- 新增 `QuoteFinanceSummary`、`QuoteFinanceAggregateContext.summary()` 和 `quote/finance/summary`。
- 汇总与设计师分组共用 `DataScopeSpecification.visible + status` 谓词；单条 Criteria 聚合返回一行。
- Web 财务报价页改为用 summary 驱动报价总数、报价总额、成本未完整数和预计毛利，删除三个前端 `reduce`。
- 定向测试、Provider 白名单审计、Web 构建通过；真实财务页面和 2/2 对账通过。
- 证据：`docs/acceptance/runs/20261031-dq-x6-quote-finance-summary/`。

## DQ-X7 项目合同档案服务端汇总（2026-10-31）

- `ContractWorkbench` 增加 `lineCount/lineAmount`，由 `ContractLineRepository.summarizeByContract` 单条聚合 SQL 返回。
- Web 项目合同档案消费 `project/contract/workbench`，删除合同行金额和变更金额两个前端 `reduce`。
- 合同变更摘要统一使用服务端 `change.confirmedAmount`。
- 修复施工计划 `acceptanceMode` 枚举对象转中文。
- 证据：`docs/acceptance/runs/20261031-dq-x7-contract-archive-summary/`。

## DQ-X8 合同基线行数据库分页（2026-10-31）

- `project/contract-line/page` 改为 `Pageable` 数据库查询，默认 20 条、服务端上限 50 条，排序固定为 `sort ASC, lineId ASC`。
- 合同档案 Web 适配层新增 `ContractLinePage { list, current, size, total }`，页面只渲染当前页，头部仍使用 workbench 的服务端汇总。
- 合同变更页删除 `listContractLines` 全量加载及 range 失败后的前端拼接，来源只消费 `project/contract-change/range`。
- 新增 `ContractLinePaginationProviderTest` 覆盖默认值、上限、相邻窗口、稳定排序和无成本角色。
- 真实合同 6 行/¥977.80；Boss/Designer/StoreManager API 通过，Designer cost 不返回；三档页面无溢出、console error 0。
- 证据：`docs/acceptance/runs/20261031-dq-x8-contract-line-pagination/`。

## DQ-X9 业主与小程序合同行分页同源（2026-10-31）

- `customer/contract-line/page` 改为 Pageable 数据库查询，默认 20、上限 50、稳定排序 `sort,lineId`。
- `customer/contract/workbench` 白名单补充 `lineCount/lineAmount`，业主合同范围摘要不再由当前页行计算。
- App `contractLines` 改为显式 `current/size` 的 `PageData<ContractLine>`，合同履约页按页加载，失败可重试。
- 未加载全部合同行时隐藏分组合计，只显示 workbench 全量摘要；全部加载后再显示分组合计。
- 新增 App 合同行分页契约门禁及 5 个负向自测。
- 真实业主 API 6 项/¥977.80、相邻页无交集、成本不返回；微信开发者工具原生合同页 4/4 PASS。
- 证据：`docs/acceptance/runs/20261031-dq-x9-owner-contract-line-pagination/`。

## DQ-X10 合同读模型成本聚合（2026-10-31）

- `ProjectContractContext.enrich` 不再全量加载合同行，改用 `summarizeCostByContract` 单条聚合返回行数、成本合计和缺失行数。
- 成本完整性口径保持：必备成本类型 `cost <= 0` 视为缺失，空合同仍为不完整。
- 合同详情、合同分页与 workbench 的响应字段和业务金额不变；三角色真实 API 对账通过。
- 全量测试暴露并修复 DQ-X6 门禁登记缺口：`QuoteFinanceAggregateContext` 纳入汇总治理，Provider 面板更新为 543。
- L2 全量测试 1184/1184 PASS；合同行表前后 checksum 与摘要一致。
- 分类完成：`Range` 为下一优先 PICKER_PAGE + N+1 切片，文档生成和写链路材料化单独处理。
- 证据：`docs/acceptance/runs/20261031-dq-x10-contract-enrich-cost-aggregate/`。

## DQ-X11 增减项来源选择器分页与批量快照（2026-10-31）

- `project/contract-change/range` 改为 PICKER_PAGE：current、size、keyword、itemType、spaceName、group；返回 total/hasMore/facets。
- 原合同行通过 `Specification + count + findAll(Pageable)` 数据库分页；已生效变更目标来自一次合同变更明细批量加载结果。
- `ContractChangeRangeContext` 新增批量快照重载，Range 每请求只加载一次变更明细，消除逐来源重复查询。
- Web 来源选择器增加服务端翻页、防抖搜索、类型/区域筛选和 source cache；App 增加服务端筛选与滚动加载。
- Web/App 新增合同来源分页门禁，各 4/4 负向样本被拦截。
- 真实 API、Web 与微信开发者工具原生页面通过；L2 全量 1186/1186。
- 证据：`docs/acceptance/runs/20261031-dq-x11-contract-change-range-picker/`。
