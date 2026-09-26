# Shared Agent Context

## Shared Outputs

| development | AI 经营参谋 P0 基线版：`ai/brief/report` 汇总经营驾驶舱、财务与预警，返回业务/交付/资金事实和行动建议；规则生成器可替换大模型，生成调用归企业公共影子用量；Web 经营总览新增简报卡和来源抽屉 | 后端 `ai/brief/**`、`ProviderSecurityAcceptanceTest`；Web `OverviewView.vue`、`api/{console,types}.ts`；交接 `_系统/协作/lanes/development/workspace/ai-boss-brief-handoff.md` | product-planning,acceptance,user | implementation_and_self_test_done_pending_joint_test_and_acceptance | 2026-09-26 |
| development | AI 调用与企业门店影子用量底账：新增 `ai/usage/report`、`ai/usage/page`，企业/业务门店/执行门店/企业公共归属、幂等、影子点数和只读聚合；迁移 63，P0 不扣真实资源包 | 后端 `ai/**`、`ProviderSecurityAcceptanceTest`、迁移 63、`scripts/build-release.sh`；交接 `_系统/协作/lanes/development/workspace/ai-usage-shadow-metering-handoff.md` | product-planning,acceptance,user | implementation_and_self_test_done_pending_acceptance_and_target_migration | 2026-09-26 |
| development | 接管后剩余施工计划 P0：阶段增改排序删除、进度基线保护、计划确认/调整留痕、App 四步引导、业务承接返回与卡片优化；微信开发者工具真实 BOSS 已验证新增、确认和锁定调整，进度保持 40% | 后端 `carryover/**`、`Project/ProjectStage`、`ProjectContext`、迁移 62；App `subpackages/carryover/pages/{index,edit,plan}.vue`、`subpackages/project/pages/plan.vue`、`domains/carryover/**` | product-planning,acceptance,user | implementation_and_self_test_done_pending_acceptance_and_target_migration | 2026-09-26 |
| development | v20260924-rc1 正式发布：后端 `/opt/decoboon/releases/20260924-044032` 已上线，迁移 48–59 全部 SUCCESS，首次缺迁移自动回滚后补齐成功；小程序 1.0.0 已上传，待微信后台提交审核；整改复验/业主发起暂缓 | `docs/operations/releases/2026-09-24-v20260924-rc1.md`、`docs/acceptance/20260924-业务App发版准备与遗留清单.md` | product-planning,acceptance,user | backend_released_miniapp_uploaded_pending_wechat_review | 2026-09-24 |
| development | 预约量房 2.0 P1 修复与发版候选基线：修正今日预约自然日、保留签约客户历史量房事实、绩效月份/门店筛选、4h 逾期和业务员量房入口；发布包迁移 24–59，迁移 56 幂等 | 后端 `statistic/service/boss/measure/report/support/Report.java`、`BossMeasureQuery.java`、`ProviderSecurityAcceptanceTest.java`、`scripts/build-release.sh`、迁移 56；App `measure-workbench.vue`、`assessment.vue`、`permission.ts` | product-planning,acceptance,user | release_candidate_ready_pending_business_ack | 2026-09-24 |
| development | 业主详情纳入「对外可见投影必须显式组装」规范：业主 `project/page`、`project/info`、`journey/info` 与员工侧同源投影统一冻结口径，派生状态/当前阶段按事实现场推导，禁止实体直出与前端兜底掩盖 | `rules.md` §5.6、`AGENTS.md`、`_系统/协作/lanes/development/workspace/code-error-catalog.md` ECD-011；App `docs/fixes/business-rules.md` §14.5、`docs/fixes/log.md` FIX-020 | product-planning,acceptance,user | norm_synced_pending_cross_projection_regression | 2026-09-24 |
| development | 业主端事实页展示重构：量房原样照片（带时间）、设计 CAD 图纸+区域尺寸、报价按区域清单各自独立成页（8 个 owner-* 页），客户旅程降级为全貌总览；后端 `CustomerJourney.Item` 新增 media/regions/priceGroups 并放开业主 Design/Measure 文件授权 | 后端 `customer/model/CustomerJourney.java`、`customer/context/CustomerJourneyContext.java`、`common/file/context/FileAuthorizationContext.java`、`customer/service/project/journey/info/support/Info.java`；App `components/owner/**`、`subpackages/project/pages/owner-*.vue`、`domains/journey/**`；测试 `CustomerJourneyEnrichmentTest` | product-planning,acceptance,user | implementation_and_self_test_done_pending_device_and_acceptance | 2026-09-24 |
| development | 业主端设计方案状态随商务主链闭环：新增 DesignLifecycleContext，客户接受报价/合同签约生效/项目立项统一把 Ready/Presented 方案推进为 Selected（单向、不回退、幂等）；历史数据迁移 58 回填。修复用户反馈「已报价、已施工仍显示待谈单」 | 后端 `design/context/DesignLifecycleContext.java`、`quote/context/QuoteContext.java`、`project/context/ProjectContractContext.java`、`project/context/ProjectContext.java`；测试 `DesignLifecycleClosureTest`、`DesignLifecycleContextTest`；迁移 `docs/design/migrations/58-设计方案状态闭环回填.sql` | product-planning,acceptance,user | implementation_and_self_test_done_pending_independent_acceptance | 2026-09-23 |
| development | 业主端同频监管闭环 1.0：项目旅程聚合量房/设计/报价/合同/增减项/材料/施工/验收/款项/意见投诉；新增业主意见投诉对象、员工回复处理、App 项目旅程与事项页面；员工项目/阶段可见客户整改并要求阶段显示整改中 | 后端 `customer/context/CustomerJourneyContext.java`、`customer/service/project/journey/**`、`customer/service/feedback/**`、`project/service/feedback/**`、`CustomerFeedback`；App `domains/{journey,feedback}/**`、`subpackages/project/pages/{journey,feedback-list,feedback-edit}.vue`；迁移 `docs/design/migrations/57-业主意见投诉.sql` | product-planning,acceptance,user | implementation_and_self_test_done_pending_independent_acceptance | 2026-09-23 |
| development | 业主单项目资料首页 2.0：移除业主经营指标卡，改为单项目概览和八个资料入口；项目旅程展示量房照片、设计区域/分项、报价逐项清单、选材、增减项、施工、验收和付款节点 | App `pages/index/index.vue`、`styles/home.scss`、`subpackages/project/pages/journey.vue`；后端 `CustomerJourneyContext`、`FileAuthorizationContext` | acceptance,product-planning,user | implementation_and_self_test_done_pending_acceptance | 2026-09-23 |
| development | 业主端同频监管业务闭环审计：确认整改意见业务端不可见、阶段“进行中”语义失真、意见投诉与客户感知缺失；输出项目/阶段可见性修复、统一意见反馈状态、服务感知和评价体系建议 | `_系统/协作/lanes/development/workspace/owner-supervision-closure-20260923/OWNER-SUPERVISION-CLOSURE-DRAFT.md`；`docs/product/consultations/REQ-BIZ-20260923-005-业主端同频监管与意见投诉闭环.md` | product-planning,acceptance,user | analysis_done_pending_business_decision | 2026-09-23 |
| development | 业主验收整改图片上传授权修复：业主仅在 Acceptance 范围且有 Active 项目授权时可上传，其他范围仍拒绝；整改图片可授权预览并随轮次留痕 | 后端 `common/file/context/FileStorageContext.java`、`FileAuthorizationContext.java`；App `domains/acceptance/api.ts`、`accept/edit.vue`；测试 `OwnerFileExposureAcceptanceTest`、`OwnerClosureAcceptanceTest` | acceptance,product-planning,user | implementation_and_self_test_done_pending_acceptance | 2026-09-23 |
| development | 业主验收轮次时间线身份域拆分：新增 `customer/acceptance/round/page`，按 CustomerAccess 校验验收单项目授权；App 业主端轮次请求不再走员工 Provider | 后端 `customer/service/acceptance/round/page/**`；App `domains/acceptance/api.ts`；测试 `OwnerClosureAcceptanceTest`、`ProviderSecurityAcceptanceTest` | acceptance,product-planning,user | implementation_and_self_test_done_pending_acceptance | 2026-09-23 |
| development | 整改类型/多图/服务端截止日期：施工整改 3 天、设计整改暂定 5 天，业主与业务均不可设置截止日期；支持最多 9 张问题图片；PM 整改看板显示类型与图片数 | 后端 `AcceptanceRectificationType`、`Acceptance/AcceptanceRound`、`ProjectContext`、customer/project acceptance Providers；App `domains/acceptance/**`、`accept/edit.vue`、`rectify-board.vue`；迁移 `docs/design/migrations/56-验收整改类型与问题图片.sql` | acceptance,product-planning,user | implementation_and_self_test_done_pending_acceptance | 2026-09-23 |
| development | 业主整改责任人默认项目经理：业主提交整改要求无需选择责任人，服务端固定取项目 `manager`，未指定项目经理时阻断；同步登记 BR-008/SCN-010 口径更新咨询 | 后端 `customer/service/acceptance/update/support/Update.java`；App `domains/acceptance/{api,model}.ts`、`subpackages/project/pages/accept/edit.vue`；咨询单 `docs/product/consultations/REQ-BIZ-20260923-003-业主整改责任人默认项目经理.md` | acceptance,product-planning,user | implementation_and_self_test_done_pending_acceptance | 2026-09-23 |
| development | 业主合同履约档案身份域拆分：新增 `customer/contract/workbench` 与 `customer/contract-line/page`，按 CustomerAccess 做项目/合同授权，响应排除成本利润与内部经营字段；App 按 customer 角色分流并隐藏员工动作 | 后端 `customer/context/CustomerContractContext.java`、`customer/service/contract/workbench/**`、`customer/service/contractlines/page/**`；App `domains/trade/api.ts`、`subpackages/project/pages/contract.vue`；测试 `OwnerClosureAcceptanceTest`、`ProviderSecurityAcceptanceTest`；全量 92 suites / 301 tests PASS | acceptance,product-planning,user | implementation_and_self_test_done_pending_independent_acceptance | 2026-09-23 |
| development | 企业负责人增减项工作台：全企业只读聚合在途审批/待客户确认/材料下单/变更款应收/合同金额与预计毛利投影，复用既有审批与客户确认动作；不改状态机与金额口径 | 后端 `statistic/model/BossContractChange*.java`、`statistic/context/BossContractChangeContext.java`、`statistic/service/boss/contractchange/report/**`、变更明细/材料计划/应收仓储批量查询；App `docs/product/2026-09-22-企业负责人增减项工作台产品设计.md`、`subpackages/project/pages/change-workbench.vue`、`domains/workbench/{model,api}.ts`、`pages/project/list.vue` | acceptance,product-planning,user | implementation_done_self_tested_pending_acceptance | 2026-09-22 |
| development | 企业负责人客户经营第四张指标卡由风险客户改为预约量房客户（含日增/昨日/近7日增/月增） | `src/main/java/com/th4/edge/decoboon/customer/context/CustomerOverviewContext.java`、`property/repository/MeasureRepository.java`；App `pages/customer/list.vue` 复用指标卡渲染 | acceptance,product-planning,user | implementation_done_self_tested_pending_acceptance | 2026-09-22 |
| development | 客户经营日期窗口与负责人分组工作区（误派单范围内的开发保留，用户澄清后暂停验收，后续单独讨论客户页） | `_系统/协作/lanes/development/workspace/boss-customer-workspace-handoff.md`；后端 `customer/model/CustomerMetricPeriod.java`、`CustomerOwnerGroup.java`、`CustomerOverviewContext.java`；App `pages/customer/list.vue` | product-planning,user | implementation_retained_pending_product_discussion | 2026-09-22 |
| development | 预约量房 2.0 P0：全企业看板+异常待办+业务员作业列表；业务员量房日期维度与转化指标迁入人员绩效；状态机不重做 | `../../前端代码/th4-decoboon-app/docs/product/2026-09-22-企业负责人预约量房工作台2.0产品设计.md`；`_系统/协作/lanes/development/workspace/boss-measure-workspace-handoff.md`；App `measure-workbench.vue`、`assessment.vue`、`workbench/{model,api}.ts`；后端 `BossMeasurePeriod.java`、`BossMeasureReport.java`、`measure/report/support/Report.java` | acceptance,product-planning,user | independent_acceptance_blocked_pending_fix | 2026-09-22 |
| acceptance→development | 整改闭环 DEF-RECTIFY-001..004 返工独立复验完成：本地 19/0/0/0、全量 76 suites/234 tests 全绿，隔离 MySQL 8.4 迁移 47 幂等回填通过；目标环境迁移与真实微信真机仍 BLOCKED_EXTERNAL | `docs/acceptance/阶段验收整改闭环独立验收报告.md`；`_系统/协作/lanes/acceptance/workspace/rectification-closure/recheck2-results.json`；`src/test/java/com/th4/edge/decoboon/owner/OwnerClosureAcceptanceTest.java` | development,product-planning,user | conditional_pass_pending_external | 2026-09-22 |
| acceptance→development | 预约量房 2.0 P0 独立验收：派生状态和业务员数据范围通过，UI 返工静态通过；今日预约口径、签约客户日期事件、4h 逾期、绩效筛选传递和业务员入口权限仍有缺陷；App 构建 PASS，真机 BLOCKED | `docs/acceptance/预约量房工作台2.0独立验收报告.md`；`_系统/协作/lanes/acceptance/workspace/boss-measure-workspace/workspace2-results.json`；`src/test/java/com/th4/edge/decoboon/statistic/BossMeasureWorkspace2AcceptanceTest.java` | development,product-planning,user | blocked_pending_fix | 2026-09-22 |
| acceptance→development | 客户经营第四张指标卡独立验收：riskCustomer→measureCustomer；预约时间/measureTime/Cancelled/去重/四窗口/完成/待派单测试通过，App 构建 PASS；真实页面视觉 BLOCKED_EXTERNAL | `docs/acceptance/客户经营预约量房指标卡独立验收报告.md`；`src/test/java/com/th4/edge/decoboon/customer/CustomerOverviewContextTest.java`；`_系统/协作/lanes/acceptance/workspace/customer-measure-metric/summary.json` | development,product-planning,user | conditional_pass_pending_visual | 2026-09-22 |
| acceptance→development | 预约量房工作台结构收敛版独立复验完成：全企业统计仅业务员分组/组内 KPI 与列表一致，但全企业 periods 丢失已签约客户事件 P1、App 84h 逾期筛选 P1、绩效页月份/门店未传量房接口 P2；真机 BLOCKED | `docs/acceptance/预约量房工作台独立验收报告.md`；`_系统/协作/lanes/acceptance/workspace/boss-measure-workspace/latest-probe-summary.json` | development,product-planning,user | blocked_pending_fix | 2026-09-22 |
| acceptance→development | 用户纠正派单：客户经营页验收作废；日期维度与报价谈单式工作区应落在预约量房工作台。上一轮探针和 BLOCK 结论仅保留为误派单记录，不计缺陷、不作为验收依据 | `docs/acceptance/企业负责人首页与客户经营工作区独立验收报告.md`；`_系统/协作/lanes/acceptance/workspace/boss-customer-workspace/probe-summary.json` | development,product-planning,user | void_superseded_awaiting_new_handoff | 2026-09-22 |
| development | 量房经营 P0：房屋级预约/完成时间、预约量房转化聚合、经营角色量房工作台；全量 78 suites/236 tests，App release build PASS，迁移 48 隔离 MySQL 幂等验证通过 | 后端 `Measure.java`、`BossMeasureReport*`、`statistic/boss/measure/report`、`48-量房预约完成时间.sql`；App `measure-workbench.vue`、`workbench/{model,api}.ts`、角色路由入口 | product-planning,acceptance,user | implementation_done_self_tested_pending_acceptance | 2026-09-22 |
| business→development | 整改闭环知识卡回写完成（SCN-010 / BR-008 / DEC-007 + CAP-001 与 DEC-001 修订 + 索引重建） | `装得清业务智能体/01-知识库/{03-业务场景/SCN-010-阶段验收驳回后的整改与复验闭环.md,02-业务规则/BR-008-阶段验收整改循环与结论追加规则.md,09-已确认决策/DEC-007-整改只做质量事实.md}` | development,product-planning,acceptance,user | verified_knowledge_written_back | 2026-09-21 |
| development | 阶段验收整改闭环实现（轮次实体 + 时间线 + 整改跟进） | `src/main/java/com/th4/edge/decoboon/project/model/AcceptanceRound.java`、`project/service/acceptances/{round,rectify}/**`、`docs/design/migrations/47-阶段验收整改轮次.sql`、App `subpackages/project/pages/{accept/edit,rectify-board}.vue` | acceptance,product-planning,user | implementation_done_self_tested_pending_acceptance | 2026-09-21 |
| development | 阶段验收整改闭环业务流程与交互设计（口径已冻结，待实现） | `前端代码/th4-decoboon-app/docs/product/2026-09-21-阶段验收整改闭环业务流程与交互设计.md`、`_系统/协作/lanes/development/workspace/t05-rectification-closure.md` | product-planning,acceptance,user | draft_pending_kb_writeback | 2026-09-21 |
| development | 主材选材 · App 状态色映射 + 选材跟进台（G5/G4-App） | `th4-decoboon-app/src/styles/tokens.ts`、`src/subpackages/project/pages/selection-board.vue`、后端 `material/service/selections/board/**`、`_系统/协作/lanes/development/workspace/material-selection-app-ui-handoff.md` | acceptance,product-planning,user | implementation_done_self_tested_pending_acceptance | 2026-09-21 |
| development | 企业负责人「项目」Tab 运营视角改造（后端聚合 + App 经营台账，v1.1 含 4 项用户裁决） | 后端 `src/main/java/com/th4/edge/decoboon/project/service/overview/**`、`project/context/ProjectOverviewContext.java`；App `src/pages/project/list.vue`、`src/domains/project/**`；设计 `前端代码/th4-decoboon-app/docs/product/2026-09-21-企业负责人项目Tab运营视角产品设计.md`；需求 `BR-APP-035/036`、`APP-DATA-004` | product-planning,acceptance,user | implementation_done_self_tested_pending_acceptance | 2026-09-21 |
| development | 签约后主材选材业务流程与交互设计（产品+UI） | `前端代码/th4-decoboon-app/docs/product/2026-09-21-主材选材业务流程与交互设计.md` | product-planning,acceptance,user | draft_pending_acceptance | 2026-09-21 |
| development | 产品文档与UI设计索引（含缺口与重复梳理） | `docs/product/00-产品文档与UI设计索引.md` | product-planning,acceptance,user | draft_pending_review | 2026-09-21 |
| development | 签约后主材选材文本化业务流程 | `src/main/java/com/th4/edge/decoboon/material/**`、`docs/design/migrations/46-主材选材记录.sql`、`docs/design/20-主材选材文本化设计.md`、App `src/subpackages/project/pages/selection.vue` | acceptance,product-planning,user | implementation_done_self_tested_pending_acceptance | 2026-09-21 |
| development | 合同履约档案 + 三个工作台（选材/材料计划/增减项）+ 合同行快照与打印导出 | 后端 `project/service/contract/workbench/**`、`project/service/contractlines/**`、`document/context/ContractPdfRenderer.java`、`document/service/contract/**`；App `subpackages/project/pages/{contract,change,material-plan,sign-workbench}.vue` | product-planning,acceptance,user | implementation_done_self_tested_pending_acceptance | 2026-09-21 |
| product-planning | 企业负责人合同签约与履约产品设计（草案 v1.0） | 前端代码/th4-decoboon-app/docs/product/2026-09-21-企业负责人合同签约与履约产品设计.md | user,development,acceptance | draft_pending_user_freeze | 2026-09-21 |
| development | App 报价详情分项双视图与底部文字操作板 | App `src/subpackages/quote/pages/detail.vue`、`src/subpackages/quote/components/QuoteItemRow/QuoteItemRow.vue` | acceptance,user,product-planning | implementation_done_self_tested_pending_acceptance | 2026-09-21 |
| development | 报价打印稿字体嵌入、平方米单位、区域分组与业务命名 | 后端 `src/main/java/com/th4/edge/decoboon/document/context/QuotePdfRenderer.java`、`src/main/resources/fonts/`、App `src/subpackages/quote/pages/detail.vue` | acceptance,user | implementation_done_self_tested_pending_acceptance | 2026-09-21 |
| development | 小程序业务单据下载改走业务域名网关 | src/main/java/com/th4/edge/decoboon/common/file/context/FileSignatureContext.java | acceptance,user | implementation_done_self_tested_pending_acceptance | 2026-09-21 |
| development | 全角色启动引导 P0 开发交接 | _系统/协作/lanes/development/workspace/onboarding-role-guide-p0-handoff.md | product-planning,acceptance | implementation_done_self_tested_pending_acceptance | 2026-09-20 |
| development | 单据模板与报价 PDF 归档开发交接 | _系统/协作/lanes/development/workspace/document-template-quote-pdf-handoff.md | product-planning,acceptance,user | implementation_done_self_tested_pending_acceptance | 2026-09-20 |
| development | 合同付款计划与施工计划基线开发交接 | _系统/协作/lanes/development/workspace/contract-plan-baselines-handoff.md | product-planning,acceptance,user | implementation_done_self_tested_pending_acceptance | 2026-09-20 |
| development | 企业负责人首页改造开发交接 | _系统/协作/lanes/development/workspace/boss-homepage-redesign-handoff.md | product-planning,acceptance,user | implementation_done_self_tested_pending_acceptance | 2026-09-20 |
| development | 企业自定义付款与施工计划模板编辑器开发交接 | _系统/协作/lanes/development/workspace/enterprise-plan-template-editor-handoff.md | product-planning,acceptance,user | implementation_done_self_tested_pending_acceptance | 2026-09-20 |
| development | 客户经营聚合与负责人客户卡开发交接 | _系统/协作/lanes/development/workspace/customer-overview-aggregation-handoff.md | product-planning,acceptance,user | implementation_done_self_tested_pending_acceptance | 2026-09-20 |
| development | 装企历史数据承载三条渠道交互流程 | docs/product/2026-09-21-装企历史数据承载-三条渠道交互流程.md | product-planning,acceptance,user | suspended_not_planned | 2026-09-21 |
| development | 装企历史数据最小承载集字段冻结 | docs/plan/2026-09-21-装企历史数据最小承载集字段冻结.md | product-planning,acceptance,user | suspended_not_planned | 2026-09-21 |
| user | 装企历史数据承载业务裁决（DEC-004 / EV-024） | ../推广营销/装得清业务智能体/01-知识库/09-已确认决策/DEC-004-装企历史数据承载范围与服务通道.md | development,product-planning,acceptance | verified_pending_development | 2026-09-21 |

| from | title | path | audience | status | updated |
|---|---|---|---|---|---|
| product-planning | 装得清上线前业务功能与开发范围规划 | docs/plan/2026-09-15-上线前业务功能规划.md | user,development,acceptance | draft | 2026-09-15 |
| material | 装得清上线前素材盘点 | docs/marketing/material/装得清上线前素材盘点.md | topic-selection,content-writing,design,user | ready | 2026-09-15 |
| material | 装得清上线前素材索引 | docs/marketing/material/素材索引.md | topic-selection,content-writing,design | ready | 2026-09-15 |
| topic-selection | 装得清上线前选题规划 | docs/marketing/topics/装得清上线前选题规划.md | user,material,content-writing,design | draft | 2026-09-15 |
| acceptance | 上线前业主闭环 P0 验收方案 | docs/acceptance/上线前业主闭环验收方案.md | product-planning,development | ready | 2026-09-15 |
| development | 业主闭环 P0 后端交接 | _系统/协作/lanes/development/workspace/owner-closure-handoff.md | acceptance,user | ready | 2026-09-15 |
| development | 小程序密码设置闭环开发交接 | _系统/协作/lanes/development/workspace/miniapp-password-closure-handoff.md | acceptance,user | ready_pending_acceptance | 2026-09-15 |
| development | 阿里云 OSS 对象存储 | docs/design/migrations/22-OSS对象存储部署说明.md | acceptance,运维,development | configured_pending_acceptance | 2026-09-16 |
| development | OSS 存储开发交接 | _系统/协作/lanes/development/workspace/aliyun-oss-storage-handoff.md | acceptance,运维,user | ready_pending_acceptance | 2026-09-16 |
| development | 微信支付 API v3 配置 | docs/design/migrations/23-微信支付配置.md | acceptance,运维,development | configured_pending_acceptance | 2026-09-16 |
| development | 微信支付配置交接 | _系统/协作/lanes/development/workspace/wechat-pay-config-handoff.md | acceptance,运维,user | ready_pending_acceptance | 2026-09-16 |
| development | 生产发版包与配置 | _系统/协作/lanes/development/workspace/production-release-config-handoff.md | acceptance,运维,product-planning | deployed_pending_acceptance | 2026-09-16 |
| development | 生产后端哨兵与首个代码缺陷修复 | `_系统/协作/lanes/development/workspace/backend-prod-sentinel-handoff.md` | acceptance,运维,development | deployed_fixed_first_incident | 2026-09-16 |
| development | 装得清运营管理端（独立平台域） | ../../前端代码/th4-decoboon-admin/ | product-planning,acceptance,运维,user | platform_isolated_built_pending_acceptance | 2026-09-16 |
| development | 独立平台组织与用户体系设计 | docs/product/装得清运营管理端用户组织与权限设计.md | product-planning,acceptance,运维,user | implemented_pending_acceptance | 2026-09-16 |
| development | IDEA 平台运营启动与本地数据同步 | docs/operations/2026-09-16-IDEA平台运营启动与本地数据同步.md | user,运维,acceptance | local_running_verified | 2026-09-16 |

| acceptance | 小程序企业负责人全链路闭环验收报告 | docs/acceptance/小程序企业负责人全链路闭环验收报告.md | development,product-planning,user | pass_with_defects | 2026-09-16 |
| acceptance | 小程序全链路回归规范（含根因反思） | docs/acceptance/小程序全链路回归规范.md | development,product-planning,acceptance | ready | 2026-09-16 |
| acceptance | 小程序全路由巡检自动化资产 | _系统/协作/lanes/acceptance/workspace/miniapp-boss-pilot/ | development,acceptance | ready | 2026-09-16 |
| development | 生产懒加载缺陷修复（INC-20260916-a1233597b0） | src/main/java/com/th4/edge/tenant/billing/repository/SubscriptionRepository.java | acceptance,user,运维 | fixed_deployed | 2026-09-16 |
| development | 代码错误集与禁则（ECD-001 事务外懒加载） | _系统/协作/lanes/development/workspace/code-error-catalog.md | 全部 lane,user | ready | 2026-09-16 |
| development | 懒加载回归测试（OSIV=false） | src/test/java/com/th4/edge/tenant/billing/SubscriptionLazyLoadingRegressionTest.java | acceptance,development | passed | 2026-09-16 |
| development | 生产 OSIV 配置调整发版（open-in-view=true） | dist/releases/20260916-065823 | acceptance,user,运维 | deployed | 2026-09-16 |
| acceptance | 测试基线同步（路由 196→197、生产基线断言对齐） | src/test/java/com/th4/edge/decoboon/security/ | development,user | pass_83_0_0_0 | 2026-09-16 |
| development | 规范同步：生产 open-in-view 必须为 true | AGENTS.md、rules.md | 全部 lane,user | applied | 2026-09-16 |
| development | 本地开发密钥加载机制（IDEA 可直接启动） | scripts/gen-local-dev-config.sh、docs/deploy/LOCAL-DEV.md | user,development,acceptance | verified | 2026-09-16 |
| development | 小程序「未加入企业」页优化（邀请编号/链接 + 并排按钮） | ../../前端代码/th4-decoboon-app/src/pages/company/join.vue | acceptance,user | built | 2026-09-16 |
| development | 经营驾驶舱「设置目标」交互优化（保存按钮右对齐） | ../../前端代码/th4-decoboon-app/src/subpackages/statistic/pages/analysis.vue | acceptance,user | built | 2026-09-16 |
| development | 小程序 TabBar 遮挡修复（全局 jz-tabbar-gap） | ../../前端代码/th4-decoboon-app/src/styles/base.scss 等 6 页 | acceptance,user | built | 2026-09-16 |
| development | 成员管理页交互优化（分段筛选 + 卡片操作区） | ../../前端代码/th4-decoboon-app/src/subpackages/company/pages/members.vue | acceptance,user | built | 2026-09-16 |
| development | 新建客户表单对齐修复（性别/来源/地址右对齐 + 地址宽度） | ../../前端代码/th4-decoboon-app/src/subpackages/customer/pages/edit.vue | acceptance,user | built | 2026-09-16 |
| development | 客户列表层次感与客户卡片优化（三层结构） | ../../前端代码/th4-decoboon-app/src/pages/customer/list.vue | acceptance,user | built | 2026-09-16 |
| development | 客户详情链路体验优化（跳转页 + 量房前置校验 + 卡片分层） | ../../前端代码/th4-decoboon-app/src/subpackages/customer/pages/detail.vue | acceptance,user | built | 2026-09-16 |
| development | 新增房屋页 / 记录客户微信页 | ../../前端代码/th4-decoboon-app/src/subpackages/customer/pages/property-edit.vue、wechat-record.vue | acceptance,user | built | 2026-09-16 |
| development | 小程序 release 1.0.0 构建并上传微信平台 | ../../前端代码/th4-decoboon-app/dist/build/mp-weixin | acceptance,user,产品 | uploaded_pending_review | 2026-09-16 |
| development | 邀请页已授权自动进工作台 | ../../前端代码/th4-decoboon-app/src/pages/invite/accept.vue | acceptance,user | built_pending_upload | 2026-09-16 |
| development | 工作台首页顶部锚定/企业展示/微信头像/老板视角 | ../../前端代码/th4-decoboon-app/src/pages/index/index.vue 等 9 文件 | acceptance,user | built_pending_upload | 2026-09-16 |
| development | 门店维度（驾驶舱/成员/绩效 + JzStoreFilter） | 后端 user/store/list 等 + 前端 JzStoreFilter.vue | acceptance,user | built_pending_upload | 2026-09-16 |
| development | 客户列表门店维度（显式 Provider，B 方案） | 后端 customer/cust/page + 前端 customer/list.vue | acceptance,user | built_pending_upload | 2026-09-16 |
| development | 金额万元/元自适应 + 财务模块品牌色/表单对齐统一 | 前端 format.ts、JzMoney.vue、finance.scss 等 13 文件 | acceptance,user | built_pending_upload | 2026-09-16 |
| development | 财务配色与表单对齐二次修复（含开发者工具实测） | 前端 finance.scss、project.vue、base.scss | acceptance,user | verified_in_devtools | 2026-09-16 |
| development | 本地版本提交 1.0.1（后端 f5a0c57 / 前端 6b30de8） | 后端+前端仓库 | acceptance,user | committed_not_released | 2026-09-16 |
| development | 生产后端监控 2026-09-17 18:41（exit 20） | docs/operations/incidents/2026-09-17/20260917-184110/triage.md | 运维,development,user | open_needs_manual_triage | 2026-09-17 |

| product-planning | 客户标签/画像/分级实施规格（用户已冻结） | docs/plan/2026-09-18-客户标签画像与分级实施规格.md | development,acceptance,user | spec_frozen_pending_acceptance | 2026-09-18 |
| product-planning | 客户标签/画像/分级 P0 前端实施方案（App） | docs/plan/2026-09-18-客户标签画像与分级-P0前端实施方案.md | development,acceptance,user | plan_ready_pending_dev | 2026-09-18 |
| product-planning | 客户标签/画像/分级 P0-b 后端字段与 Provider 契约 | docs/plan/2026-09-18-客户标签画像与分级-P0b后端契约.md | development,acceptance,user | contract_ready_pending_backend | 2026-09-18 |
| development | 研发任务单：客户标签/画像/分级 P0-b | _系统/协作/lanes/development/workspace/p0b-customer-tag-portrait-dev-task.md | acceptance,product-planning,user | b1_b6_done_b7_pending | 2026-09-18 |
| development | 小程序 ICP 备案号登记与提审基线更新 | docs/design/migrations/20-小程序发布合规基线.md | acceptance,product-planning,user | filed_number_recorded_pending_verification | 2026-09-20 |

| product-planning | 材料计划业务梳理与产品设计 v1.2：签约选材确认→财务逐条下单（选供应商/下单价/调数量）→下单形成应付→到货回填账期→PM/设计负责人任一方到场确认；含增减项加/减联动（已到货扣减强制退货单）、损耗率来源于企业主材分类、采购损耗计成本/施工损耗只流转库存、预计成本取合同成本价、选材文本不参与供应商匹配、材料域供应商关系、采购明细行、迁移 51、AC-01～23；DEC-MP-01～10 全部裁决，无待裁决项 | docs/product/2026-09-23-材料计划业务梳理与设计.md | development,acceptance,user | all_decisions_closed_P0_ready | 2026-09-23 |

| development | 材料计划 P0 实现：MaterialPlan 增来源/选材快照/损耗字段与状态扩展、选材确认门禁、材料域供应商供应关系、确认下单即形成应付、到场回填账期与 PM/设计负责人确认、material/plan/workbench 工作台 Provider + App 页面、迁移 51 | `_系统/协作/lanes/development/workspace/material-plan-p0-handoff.md`；后端 `project/**`、`material/**`、`supply/**`、`finance/**`；App `subpackages/project/pages/material-plan-workbench.vue`、`domains/material/**`、`subpackages/supply/**`；`docs/design/migrations/51-材料计划与供应商下单.sql` | acceptance,product-planning,user | implementation_done_self_tested_pending_independent_acceptance | 2026-09-23 |

| development | 材料计划 P1（扣减与退货冲减）：三段式扣减（未下单先冲损耗再冲净用量 / 已下单调整采购单并按差额下调应付 / 已到货强制走退货单冲减应付与计划实际成本）、已付款转待协商、扣减后计划状态重算、MaterialReturn + material/return/page、迁移 52 | `_系统/协作/lanes/development/workspace/material-plan-p1-handoff.md`；后端 `project/context/MaterialDeductionContext.java`、`project/model/MaterialReturn.java`、`material/service/returnorder/**`；App `subpackages/project/pages/material-plan-workbench.vue`、`domains/material/**` | acceptance,product-planning,user | implementation_done_self_tested_pending_independent_acceptance | 2026-09-23 |

| development | 材料计划 P2：退货余款冲抵同供应商其他应付（余额转 PartnerCredit 往来余额）、PurchaseOrderItem 采购明细与按供应商合并下单 `project/purchase/batch`、多明细到场验收、主材分类 MaterialCategory 默认损耗率被计划行继承、计划状态统一 MaterialPlanStatusContext 派生、迁移 53 | `_系统/协作/lanes/development/workspace/material-plan-p2-handoff.md`；后端 `project/**`、`material/**`、`finance/**`；App `subpackages/project/pages/material-category.vue`、`material-plan-workbench.vue`、`domains/material/**` | acceptance,product-planning,user | implementation_done_self_tested_pending_independent_acceptance | 2026-09-23 |

| development | 材料计划 P3：供应商往来余额自动抵扣新应付（FIFO）、入仓退货统一入口（退货单按库存批次同步退回）、合并下单多选 UI（工作台多选→合并下单页→按供应商一次下单并形成应付） | `_系统/协作/lanes/development/workspace/material-plan-p3-handoff.md`；后端 `finance/context/PartnerCreditContext.java`、`project/service/purchases/order/**`、`project/context/MaterialDeductionContext.java`；App `subpackages/project/pages/material-batch-order.vue`、`material-plan-workbench.vue` | acceptance,product-planning,user | implementation_done_self_tested_pending_independent_acceptance | 2026-09-23 |

| development | App 企业负责人端接入材料计划工作台：BOSS 首页快捷入口「材料计划」+ 首页卡片（待下单/在途/逾期未到场，服务端聚合、全 0 自动隐藏）直达工作台；工作台按角色切换副文案、项目卡片可进项目详情 | App `src/pages/index/index.vue`、`src/roles/catalog.ts`、`src/styles/home.scss`、`src/subpackages/project/pages/material-plan-workbench.vue` | acceptance,product-planning,user | implementation_done_self_tested_pending_independent_acceptance | 2026-09-23 |

| development | 材料计划工作台 UI 交互优化：筛选带实时数量（0 值弱化）、项目卡片可折叠（默认只展开有逾期/待下单项目）、项目标签跟随筛选口径、退货冲减独立视图、「下一步」降级为状态标签保留主次按钮、多选态不可选置灰+全选本项目+底部已选项目名、一次拉全量本地筛选、空态给下一步行动 | App `src/subpackages/project/pages/material-plan-workbench.vue`、`src/domains/material/model.ts`、`src/domains/material/api.ts` | acceptance,product-planning,user | implementation_done_self_tested_pending_independent_acceptance | 2026-09-23 |

| development | App 返回入口补齐：JzPageHeader 新增 back 属性（栈内返回，直达时回首页兜底），9 个自定义导航页开启返回，contract.vue Hero 内补返回按钮并修复状态栏遮挡；全 App 自定义导航页审计 11 个需返回，仅 auth/login 有意例外 | App `src/components/JzPageHeader.vue`、`src/subpackages/project/pages/{sign-workbench,change,change-workbench,change-create,change-detail,material-plan,material-plan-workbench,material-category,material-batch-order,contract}.vue` | acceptance,product-planning,user | implementation_done_self_tested_pending_independent_acceptance | 2026-09-23 |

| development | 材料计划状态机与「已下单后能否再选材」规则印证（verify-only）：现状前后端均允许已下单后继续选材（仅留痕、不改采购事实）；发现缺陷 D1（确认新版本会无条件覆盖已下单计划行的选材快照，造成计划行与采购单口径不一致）与缺口 G1（下单后选材变更规则业务未冻结，GAP-005 待确认项4 已由 DEC-MP-05 裁决但未回写） | `_系统/协作/lanes/development/workspace/material-plan-selection-state-verification.md` | product-planning,business,acceptance,user | verification_done_pending_business_decision | 2026-09-23 |

| development | 材料计划「进入采购后禁止变更选材」：后端 save/confirm 门禁（Planned/ReadyToOrder 可改，其余 8 态锁定；同一合同行多条计划任一锁定即锁定）、修复 D1（已下单计划行不再回写选材快照）、orderStatus 补 ordering/partiallyOrdered；App 选材页锁定编辑并给出「去发起增减项」入口 | 后端 `material/context/MaterialSelectionContext.java`、`material/service/selections/{save,confirm}/support/**`、`project/repository/MaterialPlanRepository.java`、`test/.../material/MaterialSelectionGateTest.java`；App `subpackages/project/pages/selection-item.vue`、`selection.vue`、`domains/material/model.ts`；印证文档 `_系统/协作/lanes/development/workspace/material-plan-selection-state-verification.md` §七 | acceptance,business,product-planning,user | implementation_done_self_tested_pending_independent_acceptance | 2026-09-23 |

| development→business | 业务知识库回写（用户授权）：新增 DEC-008（材料计划选材门禁与换材路径）、BR-009（选材门禁规则）、GAP-009（材料计划采购应付口径待知识化）；更新 GAP-005（项4 收敛为必须、补禁止变更选材）；登记 EV-040~EV-043；CHANGELOG v0.9.0；validate 通过、索引已刷新 | 业务库 `01-知识库/09-已确认决策/DEC-008-*.md`、`01-知识库/02-业务规则/BR-009-*.md`、`01-知识库/08-冲突与缺口/GAP-00{5,9}-*.md`、`04-证据来源/registry.json`、`06-状态/CHANGELOG.md`、`06-状态/知识索引.*` | product-planning,acceptance,user | business_writeback_done | 2026-09-23 |

## Cross-Lane Requests

| id | from | to | request | status | host_delivery | link | updated |
|---|---|---|---|---|---|---|---|
|  | id | from | to | request |  | status |  |
| REQ-20260915-PRELAUNCH-PLAN | user | product-planning | 规划装得清上线前必须补齐的业务功能、优先级、开发任务和验收标准。 | delivered_via_codex_thread_tool | delivered_via_codex_thread_tool | _系统/协作/shared.md | 2026-09-15 |
| REQ-20260915-MATERIAL-INVENTORY | user | material | 盘点装得清上线宣传可用的素材、资料、证据基础和缺口。 | delivered_via_codex_thread_tool | delivered_via_codex_thread_tool | _系统/协作/shared.md | 2026-09-15 |
| REQ-20260915-DEV-OWNER-CLOSURE | product-planning | development | 实现邀请制种子客户试用版的业主邀请、身份授权、项目读取与验收闭环 P0。 | delivered_via_codex_thread_tool | delivered_via_codex_thread_tool | _系统/协作/shared.md | 2026-09-15 |
| REQ-20260915-ACCEPT-OWNER-CLOSURE | product-planning | acceptance | 制定业主闭环 P0 的独立验收方案、负向测试和可执行矩阵。 | delivered_via_codex_thread_tool | delivered_via_codex_thread_tool | _系统/协作/shared.md | 2026-09-15 |
| REQ-20260915-TOPIC-PRELAUNCH | material | topic-selection | 依据上线前素材盘点制定装得清上线前选题规划与排期。 | delivered_via_codex_thread_tool | delivered_via_codex_thread_tool | _系统/协作/shared.md | 2026-09-15 |
| REQ-20260915-ACCEPT-EXEC-OWNER | development | acceptance | 基于业主闭环后端实现执行独立自动化与负向验收并输出报告。 | delivered_via_codex_thread_tool | delivered_via_codex_thread_tool | _系统/协作/shared.md | 2026-09-15 |
| REQ-20260915-WRITE-BATCH1 | topic-selection | content-writing | 为首发可信选题 T01-T08 生成大纲、脚本、图文结构和设计交接清单。 | delivered_via_codex_thread_tool | delivered_via_codex_thread_tool | _系统/协作/shared.md | 2026-09-15 |
| REQ-20260915-DEV-T05 | product-planning | development | 完成业主闭环小程序接入并整理正式迁移文件。 | delivered_via_codex_thread_tool | delivered_via_codex_thread_tool | _系统/协作/shared.md | 2026-09-15 |
| REQ-20260915-DESIGN-BATCH1 | content-writing | design | 为 T01-T08 制作内部封面方案、分镜和视觉规范草案。 | delivered_via_codex_thread_tool | delivered_via_codex_thread_tool | _系统/协作/shared.md | 2026-09-15 |
| REQ-20260915-DEV-FIX-FILE-AUTH | acceptance | development | 修复 DEF-OWNER-001：关闭匿名 /upload/**，实现私有文件服务端鉴权和历史 URL 失效策略。 | delivered_via_codex_thread_tool | delivered_via_codex_thread_tool | _系统/协作/shared.md | 2026-09-15 |
| REQ-20260915-ACCEPT-RECHECK-FILE | development | acceptance | 复验 DEF-OWNER-001 修复、私有文件签名授权、全量回归和小程序构建。 | delivered_via_codex_thread_tool | delivered_via_codex_thread_tool | _系统/协作/shared.md | 2026-09-15 |
| REQ-20260915-DEV-P05-P07 | product-planning | development | 实现生产配置与迁移基线、Provider 安全和订阅权益封口。 | delivered_via_codex_thread_tool | delivered_via_codex_thread_tool | _系统/协作/shared.md | 2026-09-15 |
| REQ-20260915-ANALYTICS-FRAMEWORK | topic-selection | data-review | 建立上线前宣传内容的周/月指标、归因和数据质量标准。 | delivered_via_codex_thread_tool | delivered_via_codex_thread_tool | _系统/协作/shared.md | 2026-09-15 |
| REQ-20260915-ACCEPT-P05-P07 | development | acceptance | 独立验收生产配置、迁移 Runbook、Provider 安全和订阅闸门。 | delivered_via_codex_thread_tool | delivered_via_codex_thread_tool | _系统/协作/shared.md | 2026-09-15 |
| REQ-20260915-DEV-P06 | product-planning | development | 实现小程序 HTTPS 环境、协议隐私页面和正式发布合规基线。 | delivered_via_codex_thread_tool | delivered_via_codex_thread_tool | _系统/协作/shared.md | 2026-09-15 |
| REQ-20260915-DEV-FIX-P05 | acceptance | development | 修复危险回滚模板、迁移基线缺口和责任矩阵。 | delivered_via_codex_thread_tool | delivered_via_codex_thread_tool | _系统/协作/shared.md | 2026-09-15 |
| REQ-20260915-ACCEPT-P06 | development | acceptance | 独立验收小程序 HTTPS、协议隐私、权限和发布合规基线。 | delivered_via_codex_thread_tool | delivered_via_codex_thread_tool | _系统/协作/shared.md | 2026-09-15 |
| REQ-20260915-DEV-FIX-P06 | acceptance | development | 增加小程序发布构建门禁并拦截占位域名、AppID 和法务配置。 | delivered_via_codex_thread_tool | delivered_via_codex_thread_tool | _系统/协作/shared.md | 2026-09-15 |
| REQ-20260915-ACCEPT-RECHECK-P05-P06 | development | acceptance | 复验 PP-05 安全回滚与迁移基线，以及 PP-06 发布构建门禁。 | delivered_via_codex_thread_tool | delivered_via_codex_thread_tool | _系统/协作/shared.md | 2026-09-15 |
| REQ-20260915-DEV-FIX-P05-FINAL | acceptance | development | 修复迁移失败账本、校验和漂移、路径安全和存量库迁移策略边界。 | delivered_via_codex_thread_tool | delivered_via_codex_thread_tool | _系统/协作/shared.md | 2026-09-15 |
| REQ-20260915-ACCEPT-P05-FINAL | development | acceptance | 最终复验 PP-05 迁移失败账本、校验和漂移、路径安全和存量库边界。 | delivered_via_codex_thread_tool | delivered_via_codex_thread_tool | _系统/协作/shared.md | 2026-09-15 |
| REQ-20260915-ACCEPT-MINIAPP-PASSWORD | development | acceptance | 独立验证小程序首次设密、改密、密码登录闭环、审计和敏感字段边界。 | pending_acceptance | pending_acceptance | _系统/协作/lanes/development/workspace/miniapp-password-closure-handoff.md | 2026-09-15 |
| REQ-20260916-ACCEPT-OSS | development | acceptance | 独立验证 OSS 企业/项目隔离、跨租户拒绝、预签名过期、大文件/视频和历史文件迁移方案。 | pending_acceptance | pending_acceptance | _系统/协作/lanes/development/workspace/aliyun-oss-storage-handoff.md | 2026-09-16 |
| REQ-20260916-ACCEPT-WECHAT-PAY | development | acceptance | 独立验证微信支付 JSAPI 预下单、支付查询、真实回调验签解密、金额/商户/AppID 边界和重复通知。 | pending_acceptance | pending_acceptance | _系统/协作/lanes/development/workspace/wechat-pay-config-handoff.md | 2026-09-16 |
| REQ-20260916-ACCEPT-PROD-RELEASE | development | acceptance | 使用真实企业账号验证生产 API 全业务闭环、微信支付首笔回调和密钥/备份回滚。 | pending_acceptance | pending_acceptance | _系统/协作/lanes/development/workspace/production-release-config-handoff.md | 2026-09-16 |

| REQ-20260916-DEV-FIX-MP-BOSS | acceptance | development | 修复企业负责人小程序 4 项缺陷：001 设计师列表 Boss 角色分支调用店长接口、002 成员详情读权限被写保护拦截、003 已锁定方案洽谈页无法查看、004 页面加载异常无错误态。 | pending_fix | pending_fix | docs/acceptance/小程序企业负责人全链路闭环验收报告.md | 2026-09-16 |
| REQ-20260916-ACCEPT-VERIFY-PROD-LAZY-FIX | development | acceptance | 独立复验生产 PlanVersion 懒加载修复：发版后窗口无新增指纹，并核对错误集/规范是否可执行。 | pending_acceptance | pending_acceptance | _系统/协作/lanes/development/workspace/code-error-catalog.md | 2026-09-16 |
| development | 生产后端监控 2026-09-16 13:02（exit 20） | docs/operations/incidents/2026-09-16/20260916-130232/triage.md | 运维,development,user | open_needs_manual_fix | 2026-09-16 |

| REQ-20260917-PROD-MONITOR-1841 | development | 运维,acceptance | 人工复核离职员工会话循环、registry 中 fixed 基线被改写和公网扫描探测；确认后再决定是否人工修复。 | open_needs_manual_triage | pending | docs/operations/incidents/2026-09-17/20260917-184110/triage.md | 2026-09-17 |

| REQ-20260918-PROD-MONITOR-1301 | development | 运维,acceptance | 生产监控 exit 20 仅归档；人工处理离职员工会话、授权过期分类、扫描路径拦截及 registry fixed 基线改写。 | open_needs_manual_triage | pending | docs/operations/incidents/2026-09-18/20260918-130145/triage.md | 2026-09-18 |

| REQ-20260918-ONBOARDING-DEFECT-001 | development | product-planning, acceptance | 首次入驻三件套状态契约与验收冻结；供应商可选且以“有效供应商 + 品牌关系”为完成谓词；App FIX-004 代码保持不改。 | product_spec_frozen_pending_independent_acceptance | pending_acceptance | 输出/草稿/装得清首次入驻三件套-20260918/07-系统内引导与验收清单.md | 2026-09-18 |
| REQ-20260918-SMB-P0-FIX | development | acceptance, product-planning, zhuangdeqing-smb-reviewer | 中小装企实机审查已裁决 P0 修复：量房角色分离、PM 量房可见性、阶段开工幂等、业主确认归位、收款幂等、财务成本完整性统一、合同写权限一致及 5 项依赖修复。含迁移 34。合同签约最终角色/线下代录/人工材料补录仍 BLOCKED。 | implementation_done_self_tested_pending_independent_acceptance | pending_acceptance | _系统/协作/lanes/development/workspace/smb-p0-fix-handoff.md | 2026-09-18 |
| REQ-20260918-SMB-P0-FIX-2 | development | acceptance, zhuangdeqing-smb-reviewer | 验收 BLOCK rework：量房执行人权限(A4)、预约/完成态与多房屋投影(I1/I2/I4)、并发开工幂等(C4)、readiness 文案(B2)、合同 403 语义与重复签约幂等(F2/F4)、FinanceSummary 毛利率(G1)。 | implementation_done_self_tested_pending_independent_acceptance | pending_acceptance | _系统/协作/lanes/development/workspace/smb-p0-fix2-handoff.md | 2026-09-18 |
| REQ-20260918-SMB-P0-FIX-3 | development | acceptance, zhuangdeqing-smb-reviewer | 量房重复 completion 幂等（不覆盖 measureTime/latestMeasureAt/executor/designer/status/附件）与 executor.role 响应串值修复（JWT 占位主体改为重新加载完整员工实体）。 | implementation_done_self_tested_pending_independent_acceptance | pending_acceptance | _系统/协作/lanes/development/workspace/smb-p0-fix3-handoff.md | 2026-09-18 |
| REQ-BIZ-20260926-001 | development | product-planning, acceptance | 报价项目库批量导入 P0：PRD 冻结的 6 条业务路由 + 模板下载，批次/导入行、预览不落库、重复与编码冲突、错误行与部分导入、成本待维护、固定总价独立字段、24h 撤销、小程序导入页与结果页。 | implementation_done_self_tested_pending_independent_acceptance | pending_acceptance | _系统/协作/lanes/development/workspace/catalog-import-handoff.md | 2026-09-26 |
| REQ-BIZ-20260926-001-W1/W2 | development | product-planning, acceptance | 报价项目库维护可达性修复（批量导入工具移入 CatalogPicker 管理模式插槽，不再被全屏层覆盖）、卡片“更多”操作层级与删除二次确认、后端新增 `catalog/item/delete`（9 类下游引用阻断 + 导入行受控解除 + 同事务删除）。 | implementation_done_self_tested_pending_independent_acceptance | pending_acceptance | _系统/协作/lanes/development/workspace/catalog-import-ui-delete-handoff.md | 2026-09-26 |
| REQ-20260919-QUOTE-PACKAGE-CLOSURE | development | acceptance, product-planning | 收紧设计报价状态、套餐报价版本、驳回重报、App 报价上下文、套餐模板配置和套餐 Draft 调整。 | implementation_done_self_tested_pending_independent_acceptance | pending_acceptance | _系统/协作/lanes/development/worklog.md | 2026-09-19 |

| REQ-20260918-ACCEPT-ROUTE-COUNT-SYNC | development | acceptance | 新增 8 条 P0-b Provider 路由后同步路由断言 209→217、protected 197→205。验收 lane 会话不可达，经用户在本线程授权后由开发 lane 代为同步，**待验收 lane 复核**。 | synced_by_development_pending_acceptance_review | pending | src/test/java/com/th4/edge/decoboon/security/ProviderSecurityAcceptanceTest.java | 2026-09-18 |

| REQ-20260920-PROD-MONITOR-1301 | development | 运维,acceptance | 生产监控 exit 20 仅归档；窗口内无新增异常，人工处理扫描路径、auth/signin 未知入口及 registry fixed 基线/ID 改写。 | open_needs_manual_triage | pending | docs/operations/incidents/2026-09-20/20260920-130110/triage.md | 2026-09-20 |


| REQ-20260922-PROD-MONITOR-1301 | development | 运维,acceptance | 生产监控 exit 20 仅归档；窗口内无新增异常，人工处理扫描路径、auth/signin 未知入口、授权过期分类，以及 registry fixed 基线/ID 改写和 macOS 日期兼容性。 | open_needs_manual_triage | pending | docs/operations/incidents/2026-09-22/20260922-130146/triage.md | 2026-09-22 |

## Shared Agreements

| agreement | owner | status | link |
|---|---|---|---|
| 装得清产品开发与宣传、推广、营销全流程协作 | user | active | `_系统/上下文/当前项目.md` |

## App 全链路真实数据模拟（2026-09-23）

- 批次：`APP-BIZ-SIM-20260923`，基础项目 `麦鸿演示项目 37~44`，真实 Provider 构造，未直接写业务终态表。
- 覆盖：业务员录入/跟进/量房、设计、报价、签约、主材选材、材料计划、材料下单、材料到场/应付、增项、减项和数量变更。
- 每名可用员工均达到不少于 2 条有效数据；状态矩阵见 `_系统/协作/lanes/development/workspace/app-full-chain-simulation-20260923/README.md`。
- 闭环修复：材料下单因 `Payable.dueDate=null` 被 `NOT NULL` 阻断，已按 `DEC-MP-01` 改为到场后回填，新增迁移 55 并同步空库基线。
- 验证：定向后端测试 30/0/0 PASS；独立数据核验 `failures=[]`；App 读取接口 PASS。
- 结论状态：`implementation_and_self_test_done_pending_independent_acceptance`。

## 签约后项目立项与三角色协作业务规则梳理（2026-09-23）

- 产出：`docs/product/2026-09-23-签约后项目立项与三角色协作业务规则梳理.md`
- 覆盖：合同签约/生效 → 自动立项 → 施工计划快照阶段 → 工艺任务排期 → 影像节点留证 → 阶段验收与整改复验 → 应收/收款/应付/付款/结算；含设计师、项目经理、财务、业主四方协作推进与触发关系。
- 关键口径：立项唯一入口=已生效合同；`Signed ≠ Effective`；任务=阶段×区域×工艺×数量；阶段验收只能业主本人确认；成本不完整时利润返回 `null`。
- 待裁决缺口 6 项（G-01 阶段验收模式未落库、G-02 工艺库未绑定阶段/区域、G-03 阶段权重与工期口径、G-04 重排不回写快照、G-05 阶段日期无编辑入口、G-06 工艺非独立主数据）＋ `BR-002` 直接签约补录未实现。
- 状态：`analysis_done_ready_for_product_decision`。

## 麦鸿演示项目 37 端到端实测与 T05 咨询单（2026-09-23）

- 实测报告：`docs/acceptance/麦鸿演示项目37端到端实测报告.md`（开发自测，非独立验收结论）
- 证据：`docs/acceptance/evidence/project37-e2e-20260923/run.json`、`run_project37_e2e.py`、`owner-closure-acceptance-test.txt`
- T05 咨询单：`docs/product/consultations/REQ-BIZ-20260923-001-施工计划与阶段验收七项口径.md`，已投递装得清业务智能体 `02-采集与咨询/`
- 实测结论：员工侧主链（开工/任务完成/提交验收/收款/结算）可跑通；**发现 P0 `TaskNodeKey.getValue()` 缺失导致影像节点留证接口 100% 失败**，P1 收款幂等顺序、空任务阶段开工回退、取消任务计入验收门禁且 `Cancelled→Done` 无校验；业主人脸会话为外部阻断（无真实微信授权），业主驳回/复验/确认未能在真实 HTTP 链路执行。
- 状态：`self_test_done_with_defects_pending_fix_and_independent_acceptance`

## P0/P1 修复：影像节点留证 / 施工任务排期 / 收款幂等（2026-09-23）

- 修复：`TaskNodeKey` 补 `Enumerable#getValue()`；`tasknodes/save#parseKey` 空安全；`project/task/save` 补 `task.property`；`finance/receipt/save` 幂等键查找前移到余额校验之前。
- 新增测试：`TaskNodeSaveAcceptanceTest` 9/0/0、`ReceiptIdempotencyTest` 4/0/0；后端全量（排除历史无关失败类）284 tests / 0 failures / BUILD SUCCESS。
- 真实复验（项目 37，8093 修复后实例）：Start 节点留证成功、重复/非法节点 400；重复收款返回原收款；新事实超额仍 400；复验夹具与临时任务已清理。
- 报告与证据：`docs/acceptance/麦鸿演示项目37端到端实测报告.md` 附录 A、`docs/acceptance/evidence/project37-e2e-20260923/reverify-after-fix.json`。
- 待业务裁决：G-03 阶段权重与工期口径、G-07 取消任务与验收门禁（`REQ-BIZ-20260923-001`）。
- 状态：`p0_p1_fixed_pending_independent_acceptance`。

## 项目工地五项要求：权限与增减项窗口（2026-09-23）

- 已实现：施工任务推进与节点留证开放给企业负责人/店长/项目经理（店长限本门店）；增减项在项目未完工前允许，完工/结算及合同关闭/终止阻断。
- 已支持（未改代码）：成员切换（设计师/项目经理）、任务制定（项目经理/设计师）。
- 测试：`TaskNodeSaveAcceptanceTest` 11/0/0、`ContractChangeCompletionGateTest` 5/0/0；后端全量（排除历史无关失败类）291 tests / 0 failures / BUILD SUCCESS。
- 待裁决：成员切换权限与留痕、阶段开工是否开放店长、计划按排期自动生成口径（与 `REQ-BIZ-20260923-001` G-03/G-04 合并）、增减项边界。
- 记录：`docs/product/2026-09-23-项目工地需求对照与实施记录.md`。
- 状态：`partial_implemented_pending_business_decision`。

## 项目工地五项要求落地（2026-09-23）

- 用户裁决 → 已实现：成员切换仅 Boss + 本门店店长（PM 自行改派 403）+ 对内动态留痕；阶段开工开放店长；计划以排期为准自动生成（阶段=任务最值、项目=阶段汇总）并回写合同施工计划快照；增减项在待验收期间允许、店长可发起、完工/结算阻断。
- 测试：新增 `ProjectMemberSwitchAndStageOpenTest` 7/0/0、`ProjectPlanFromScheduleTest` 1/0/0；后端全量（排除历史无关失败类）299 tests / 0 failures / BUILD SUCCESS。
- 记录：`docs/product/2026-09-23-项目工地需求对照与实施记录.md`、`REQ-BIZ-20260923-001`（含用户裁决回执）。
- 状态：`implemented_self_tested_pending_independent_acceptance`。

## App 修复：新建任务负责人显示 undefined（2026-09-23）

- 根因：后端 `Employee` 无 `title` 字段（岗位名在 `role.name`），App 直接取 `raw.title` → 下拉渲染「张三（undefined）」。
- 修复：`normalizeEmployee` 从 `role.name` 派生 `title`（全局生效），任务负责人下拉/验收提交人文案补岗位回退；新增 `scripts/verify-employee-display-contract.mjs` 并接入 `verify:mp-weixin`。
- 验证：新契约校验 PASS、type-check PASS、mp-weixin 构建 DONE、分包检查 PASS（92 页 / 总包 2187039 B）；`check:home` 失败为既有问题（首页文案契约，另一会话在途）。
- 状态：`fixed_pending_device_check`。

## App 修复（续）：任务负责人候选含店长 + 任务保存映射（2026-09-23）

- 负责人候选加入店长；节点推进角色与服务端对齐（Boss/店长/PM）；店长补阶段开工能力。
- 关键修复：`scheduleApi.saveTask` 由「直接提交对象」改为扁平字段映射（`taskId/zoneId/ownerId/catalogId`），否则选择的负责人/区域不落库、编辑任务会变新建。
- 真实验证（8093 + 本地库）：任务保存后 `owner=王宏文（店长）`、`zone=客厅`；证据 `docs/acceptance/evidence/project37-e2e-20260923/task-owner-save-verify.json`。
- 门禁：`scripts/verify-employee-display-contract.mjs` 已扩展；type-check / build / 分包检查 PASS。
- 状态：`fixed_verified_by_live_api_pending_device_check`。

## 项目工地闭环与交互缺陷修复（2026-09-23）

- 修复：未指定项目经理警示与开工阻断提示、工地岗位切换（设计师/项目经理，Boss+本门店店长）、施工任务重复下发拦截（后端唯一性 + App「已下发」标记）、验收横幅阶段名、开工/保存失败提示、排期 catalogId 回退等 9 项。
- 真实证据：`docs/acceptance/evidence/project37-e2e-20260923/site-flow-verify.json`（首次下发成功 / 重复下发 400 / 换区域成功 / 店长切换项目经理成功）。
- 门禁：后端 300 tests / 0 failures（排除历史无关失败类）；App type-check / build / 分包检查 / 员工显示契约 PASS。
- 报告：`docs/acceptance/项目工地闭环与交互缺陷检查报告.md`。
- 状态：`fixed_verified_by_live_api_pending_device_and_acceptance`。

## 微信开发者工具 · 企业负责人真机闭环测试（2026-09-23）

- 用 DevTools + miniprogram-automator 以企业负责人（张伟）真实操作小程序：首页/项目列表/工地详情/成员切换/排期去重/负责人候选/验收横幅均通过；切换项目经理后 DB 落库并写入项目动态。
- 真机发现并修复 6 项缺陷（区域口径导致工艺库为空、已下发识别失效、未指定项目经理无提示、无切换入口、验收文案硬编码、保存失败卡 loading）。
- 报告 `docs/acceptance/微信开发者工具企业负责人业务闭环测试报告.md`；截图 `docs/acceptance/evidence/mp-devtools-20260923/`。
- 未覆盖：现场拍照节点留证、业主侧确认/驳回（需真实业主微信）、视觉审美评审（无图像识别）。
- 状态：`boss_role_closed_loop_verified_on_devtools_pending_device_and_visual_review`。

## 生产后端监控 2026-09-23（exit 20）

- 服务健康：`decoboon.service` active/running，版本 `/opt/decoboon/releases/20260921-050519`，HTTPS health 200，根盘 18%，35 分钟 journal 无条目。
- 窗口内无新增高置信代码缺陷；仅 `.env`、`.git/config` 扫描探测和 expected `tokenIsUndefined`，未修改代码、未重启、未构建、未发版。
- 监控工具仍有待人工修复的数据完整性问题：历史 fixed 懒加载事故被改写为 `INC-20260923-a1233597b0/open` 且计数重复；另有 macOS `date -Is` 兼容性导致失败归档时间/退出码失真。
- 证据与建议：`docs/operations/incidents/2026-09-23/20260923-130103/triage.md`。
- 状态：`open_needs_manual_tooling_fix`。

## 2026-09-24 麦鸿设计业务 App 全场景验收视图（product-planning → acceptance）

- 批次：`MH-APP-ACCEPT-20260924`（模式 `full_chain_simulation`，状态 `frozen`）
- 产出：
  - `docs/product/2026-09-24-麦鸿设计业务App全场景验收视图.md`
  - 冻结包 `_系统/协作/lanes/product-planning/workspace/PKT-20260924-MAIHONG-FULLCHAIN-001.json`（validator PASS）
- 复用夹具：麦鸿设计 `com_id=1789903520997907818`，演示项目 37–44，账号 `19900000001`–`19900000007`（口令 `Maihong2026`），来源 `_系统/协作/lanes/development/workspace/app-full-chain-simulation-20260923/`。
- 用途：acceptance lane 按 B1 主链 → B2 账务 → B3 负向 → B4 业主端全页 → B5 多机型分批验收；缺陷按视图 §10 模板登记。
- 不构成验收结论；✅ 仅为"已有实现/修复证据可复核"。

## 2026-09-24 业主邀请与注册交互重构方案（product-planning，待业务裁决）

- 产出：`docs/product/2026-09-24-业主邀请与注册交互重构方案.md` + T05 `docs/product/consultations/REQ-BIZ-20260924-001-业主邀请与注册身份判定口径.md`
- 结论：三类身份必须拆成三个独立入口；业主身份改由**手机号 + 客户档案**判定（不再依赖 token）；注册企业页去掉邀请码；允许"暂不属于任何企业"中间态；企业需支持注销。
- **风险提示（P0 级）**：同一手机号命中多家企业时"命中即进入"会造成跨企业串号——必须显式选择 + 逐项目授权。
- 可立即开发（无需裁决）：未加入企业页删邀请码改三选一、注册企业独立入口、业主页去 token/猜参数。
- 待业务答复（T05 五问）：业主是否仍需邀请、多企业可见范围、换号规则、防枚举、企业注销口径。

## 2026-09-26 版本管理与发布分支规范建立

- 总规范：`docs/operations/版本管理与发布分支规范.md`；发布记录模板：`docs/operations/releases/_发布记录模板.md`。
- 强制链：大功能 `feature/*`、小修复 `fix/*` 从 `dev` 创建并回并；正常发布 `dev -> test -> prod`，生产事故才走 `hotfix/*`。
- 当前迁移风险：Web 无独立 Git 仓库且被 `/Users/zhangwei/.git` 误纳管；后端/App 仍有混合未提交改动，需先拆分再建立 `dev/test/prod`。
- 状态：`norm_established_migration_pending`。
