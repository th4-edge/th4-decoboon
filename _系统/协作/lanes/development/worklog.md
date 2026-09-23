# Development Worklog

## 2026-09-24 预约量房 2.0 P1 修复 + 发版候选基线

- 来源：`docs/acceptance/预约量房工作台2.0独立验收报告.md`，本轮为发版前清理历史 P1，不改变业主端核心范围。
- 后端：
  1. 量房经营报告不再先按客户当前状态截断，已签约/服务中/已完工客户的历史量房事实会进入日期窗口；
  2. “今日预约”改按 `scheduledAt` 自然日统计，纳入今天已完成和今天未来时段预约；
  3. `BossMeasureQuery` 支持 `month`、`storeId`，人员绩效页可把月份/门店筛选传到量房业绩。
- App：逾期阈值统一为 4 小时；人员绩效页传月份/门店；业务员补 `view:measure-workbench`，可进入“我的量房”。
- 发版脚本：`build-release.sh` 迁移打包范围从 `24..55` 修正为 `24..59`，缺任一增量迁移会直接失败。
- 迁移：`56` 改为 information_schema 预检，可安全重试；临时 MySQL 空库连续执行两次 PASS。
- 验证：后端全量 `319 tests / 0 failures / 0 errors / BUILD SUCCESS`；`ProviderSecurityAcceptanceTest` 312 路由（public 12 + protected 300）；App `verify:mp-weixin` PASS（主包 688228 B / 最大分包 529360 B / 总包 2371557 B / 103 页）；App release build PASS。
- 状态：`release_candidate_ready_pending_business_agent_ack_and_deployment`。


## 2026-09-24 业主详情纳入「对外可见投影必须显式组装」规范

- 用户要求：把业主详情也纳入既有"投影必须显式组装"规范，避免类似问题只修单页、未沉淀为跨投影约束。
- 规范收口：`rules.md` §5.6 明确四条业主/员工同源投影；`AGENTS.md` 新增强制条目；`code-error-catalog.md` 新增 `ECD-011`，记录"直出实体 + 派生列漂移 + 前端兜底掩盖"的完整事故链。
- App台账：`business-rules.md` §14.5、`log.md` FIX-020 同步，明确 `customer/project/info`（业主详情）与首页、旅程、员工侧同源字段必须使用同一冻结口径。
- 冻结口径：当前阶段按 `Doing/Accepting` 优先否则首阶段；业主状态按 Pending 验收单、阶段开工与终态事实推导；禁止用 `projectStageText()` 前端兜底替代后端组装。
- 回归要求：同一项目在首页/详情/旅程三处 `status`、`currentStageName` 必须相等；验收驳回后三处同步从"待验收"变为"施工中"。
- 状态：`norm_synced_pending_cross_projection_regression`（本轮仅规范与台账收口，未新增业务代码）。

## 2026-09-24 业主端「事实页」展示重构（量房原样/设计CAD+区域尺寸/报价按区域）

- 用户反馈：业主端功能结构没问题，但所有入口都指向同一条客户旅程，展示以文本为主，不像装企服务业主该有的细节与用心。
- 后端数据契约：`CustomerJourney.Item` 新增 `media/regions/priceGroups` 三类结构化字段；`CustomerJourneyContext` 装配量房原样照片（带拍摄时间）、设计 CAD/效果/结构图纸、区域长宽高与面积、按区域分组的报价清单（服务端算区域小计与总计）；`customer/project/journey/info` 白名单同步放开。
- 文件授权：业主 Design 范围新增 `cadUrl`、区域图片、分项图片；量房 `structureImages` 已可授权，业主可回看自家房屋结构与设计图纸。
- App：新增 8 个业主专属页面 `owner-measure/design/quote/selection/change/acceptance/payment/contract`；新增共享组件 `OwnerFactHero`、`OwnerPhotoGrid`（按时间分组的照片墙）、`OwnerRegionList`（区域尺寸卡）、`OwnerPriceGroups`（区域切换 + 清单明细 + 区域小计）、`OwnerFactList`；`useOwnerJourney` 统一事实装配。
- 首页：8 个快捷入口从「跳旅程滚动到分区」改为各自独立页面；合同档案进入网格，意见投诉提升为独立强调入口；「项目旅程」降级为「查看我的家全貌」总览。
- 验证：`CustomerJourneyEnrichmentTest`（照片带时间/设计 CAD+区域尺寸/报价按区域）1/1 PASS；`OwnerClosureAcceptanceTest` 24/24、`OwnerFileExposureAcceptanceTest` 9/9 PASS；后端全量（排除历史无关失败类）PASS；App `type-check`、`build:mp-weixin`、`check:subpackages` PASS（主包 671266 B，最大分包 499658 B，总包 2322686 B，103 页）。
- 设计说明：`docs/product/2026-09-24-业主端事实页展示设计.md`。
- 状态：`implementation_and_self_test_done_pending_device_and_acceptance`。

## 2026-09-23 业主端设计方案状态随商务主链闭环（报价/合同/施工不再停留待谈单）

- 用户反馈：业主端设计方案一直显示“待谈单”，但报价已完成、合同已签约、项目已进入施工。
- 根因（状态机断链，非展示问题）：`DesignStatus` 只有设计师在 `design/scheme/build` 手动推进；`QuoteContext.accept`、`ProjectContractContext.markSigned/markEffective`、`ProjectContext.createFromEffectiveContract` 都没有回写设计方案状态，方案可永久停在 `Ready(待谈单)`。
- 修复：新增 `DesignLifecycleContext.markSelected(Design)`，只向 `Ready/Presented -> Selected` 单向推进，`Selected/Archived` 幂等不回退，`Draft` 与无关联方案的套餐报价（`Quote.design == null`）忽略；在客户接受报价、合同签约、合同生效、项目立项四处接入。
- 历史数据：新增 `docs/design/migrations/58-设计方案状态闭环回填.sql`，按报价 `Accepted/Converted`、合同、项目三类事实把历史 `Ready/Presented` 方案回填为 `Selected`，幂等且不动 `Draft/Selected/Archived`。
- 测试：`DesignLifecycleClosureTest`（全链路：设计报价 → 客户接受 → 合同签约/生效 → 立项，断言全程状态）1/1 PASS；`DesignLifecycleContextTest`（转移/回退/空值/幂等）6/6 PASS；`QuoteContextBusinessRulesTest` 5/5、`ProjectContractContextTest` 7/7、`DecoboonClosedLoopTest` 1/1 PASS；后端编译 PASS。
- 文档：`docs/design/14-客户报价项目状态驱动逻辑.md` §3.1.1、`docs/design/10-设计转化域设计说明.md` §3.2 补充状态随商务主链闭环规则。
- 状态：`implementation_and_self_test_done_pending_independent_acceptance`；生产迁移 58 未执行。

## 2026-09-23 业主端同频监管闭环与客户意见投诉实现

- 完成完整的业主项目生命周期聚合：`customer/project/journey/info` 聚合量房、设计、报价、合同、增减项、材料、施工、验收、款项、意见投诉十段事实，服务端排除内部成本和利润。
- 新增业主意见投诉领域：`CustomerFeedback`、提交/列表/关闭、员工列表/处理、图片授权；迁移 `57-业主意见投诉.sql` 并同步空库基线。
- App 新增项目旅程页、意见投诉列表和提交/处理页；业主项目详情增加“项目旅程”，员工项目详情增加“客户事项”，阶段详情增加“客户整改要求”，阶段驳回展示“整改中”。
- 文件上传：企业员工继续全业务上传；业主仅可在 `Acceptance`/`Feedback` 且项目授权有效时上传，跨项目 404、其他范围 403。
- 验证：后端编译 PASS；`OwnerClosureAcceptanceTest`、`OwnerFileExposureAcceptanceTest`、`ProviderSecurityAcceptanceTest` PASS（路由 311）；后端全量 92 suites / 306 tests / 0 failures / 0 errors PASS；App type-check、build:mp-weixin、check:subpackages PASS（95 页）。

## 2026-09-23 业主单项目资料首页 2.0

- 业主首页移除企业经营指标卡，改为“我的家”单项目概览和八个资料快捷入口。
- 项目旅程分区增加明细 Items：量房照片、设计方案区域/分项、报价逐项清单、选材版本、增减项明细、施工阶段/现场动态、验收问题和付款计划节点。
- 业主文件授权扩展：可读取授权项目的量房照片和设计图纸，可上传验收整改/意见投诉图片。
- 验证：后端 compile PASS；Owner 相关定向测试 PASS；后端全量 92 suites / 306 tests PASS；App type-check、build:mp-weixin、check:subpackages PASS（主包 646536 B，最大分包 452559 B，总包 2250857 B，95 页）。

## 2026-09-23 业主端同频监管业务闭环审计

- 用户反馈：业主提交整改意见后，员工业务端查不到整改意见；阶段被显示为“进行中”；缺少意见投诉和客户感知内容。
- 审计结论：整改事实已写入 `Acceptance/AcceptanceRound`，但员工端项目详情、阶段详情、项目经理待办没有投影；整改入口仅隐藏在“我的 -> 整改跟进”；`ProjectStage` 在驳回后被设为 `Doing`，前端显示“进行中”，业务语义失真；当前没有独立业主意见/投诉对象和处理状态。
- 产出：`_系统/协作/lanes/development/workspace/owner-supervision-closure-20260923/OWNER-SUPERVISION-CLOSURE-DRAFT.md`；咨询单 `docs/product/consultations/REQ-BIZ-20260923-005-业主端同频监管与意见投诉闭环.md`。
- 建议 P0：项目/阶段详情投影整改事实、项目经理首页待整改、阶段展示“整改中”、业主可见处理进度；P1 再建统一意见/投诉闭环。

## 2026-09-23 业主整改责任人默认项目经理

- 用户口径：业主端提交整改要求时，整改责任人默认当前项目项目经理，不再让业主选择。
- 后端：`customer/acceptance/update` 删除 `rectificationOwnerId` 入参，驳回时服务端固定取 `acceptance.project.manager`；项目经理为空时返回 `400 当前项目未指定项目经理，无法提交整改要求`。该阶段的业主填写截止日期已由后续“整改类型、多图和服务端截止日期”覆盖。
- App：业主验收驳回表单将“整改责任人”改为只读展示项目经理，移除人员选择器；接口不再发送责任人 ID。
- 验证：后端 `compile` PASS；`OwnerClosureAcceptanceTest` PASS（新增“提交人与项目经理不同仍归属项目经理”和“项目经理为空阻断”回归）；App `type-check` PASS。
- 业务知识冲突：`BR-008`、`SCN-010` 原写“项目侧指定整改责任人”；已登记 `docs/product/consultations/REQ-BIZ-20260923-003-业主整改责任人默认项目经理.md`，请求业务智能体按用户决策更新。
- 交接：`_系统/协作/lanes/development/workspace/owner-rectification-owner-default-20260923/HANDOFF.md`。

## 2026-09-23 整改类型、多图和服务端截止日期

- 用户口径：整改支持多图片；业主和业务不能设置截止日期；系统按整改类型自动计算，施工整改 3 天；新增设计整改类型；项目经理必须重视整改时限。
- 后端新增 `AcceptanceRectificationType`：施工整改 3 天、设计整改暂定 5 天；驳回时由 `ProjectContext` 计算截止日期，业主/员工 Provider 均不接受截止日期。
- `Acceptance`、`AcceptanceRound` 新增整改类型和问题图片集合；新增迁移 56 和空库基线同步；整改中清单增加类型、问题图片数量。
- App 业主驳回表单支持选择整改类型、上传最多 9 张问题图片，删除日期选择器并显示系统规则；整改看板显示类型、图片数量和截止日期。
- 验证：后端 compile PASS；`OwnerClosureAcceptanceTest`、`ProviderSecurityAcceptanceTest` PASS（路由总数 305）；后端全量（排除已知在飞 `BossMeasureWorkspace2AcceptanceTest`）92 suites / 303 tests / 0 failures / 0 errors PASS；App type-check、build:mp-weixin、check:subpackages PASS。
- 业务待确认：设计整改暂定 5 天，咨询单 `REQ-BIZ-20260923-004`。

## 2026-09-23 业主验收轮次时间线身份域拆分

- 日志：业主会话调用 `project/acceptance/round/page`，`Credentials` 被注入 `@User Employee` 导致 500。
- 新增 `customer/acceptance/round/page`；使用 `@User User<?>` + `CustomerAccessContext.requireCredential/requireAccess`，按验收单的 Active 项目授权返回轮次。
- App `acceptanceRoundApi.rounds` 按角色分流，业主不再调用 `project/acceptance/round/page`。
- 验证：`OwnerClosureAcceptanceTest` PASS；`ProviderSecurityAcceptanceTest` PASS（305 路由）；后端全量 92 suites / 305 tests / 0 failures / 0 errors PASS；App type-check、build:mp-weixin、check:subpackages PASS。

## 2026-09-23 业主验收整改图片上传授权修复

- 用户复现：业主为阶段验收整改图片调用文件上传，被 `FileStorageContext` 的通用“仅企业员工可以上传文件”规则阻断。
- 修复：为 `Credentials` 业主增加验收专项上传通道，仅在 `FileScope.Acceptance`、`projectId` 有 Active CustomerAccess 时允许；施工单位、设计、变更等范围继续为 403。
- 文件读取：Acceptance 授权白名单加入 `rectificationIssueImages`，业主提交后可重新签名预览。
- App：恢复业主驳回表单的整改问题图片多图上传与提交。
- 验证：`OwnerFileExposureAcceptanceTest` 覆盖业主本人项目上传成功、跨项目 404、非验收范围 403；`OwnerClosureAcceptanceTest` 覆盖整改图片落库；后端全量 92 suites / 305 tests / 0 failures / 0 errors PASS。

## 2026-09-23 业主合同履约档案身份域拆分（DEF-OWNER-CONTRACT-001）

- 现象：业主微信会话调用 `project/contract/workbench` 时 500，框架把 `Credentials` 注入 `@User Employee employee`，报 `Can not set Employee ... to Credentials`。
- 根因：业主端错误复用员工域 Provider，且员工合同履约响应包含成本、利润、合同成本快照和内部备注，不能通过放宽主体类型修复。
- 后端：新增 `customer/contract/workbench`、`customer/contract-line/page`；新增 `CustomerContractContext`，按 `Credentials` + `CustomerAccess` Active 授权校验 `projectId/contractId`，合同 ID 不能绕过项目授权。
- 字段裁剪：业主工作台白名单排除 `cost`、`signedCost`、`quote`、`createdBy`、`remark` 等字段，并在业务代码中防御性设置 `cost=null`；业主合同行只返回范围、数量、金额、品牌规格等合同可见字段。
- App：合同履约工作台和合同行 API 按 `customer` 角色分流到 `customer/*`；业主页面隐藏登记收款、合同打印、原始报价、材料计划、选材工作台和增减项员工动作。
- 验证：后端 `compile` PASS；`OwnerClosureAcceptanceTest` PASS；`ProviderSecurityAcceptanceTest` PASS（304 路由）；`DetailResponseWhitelistContractTest,DataScopeRequestFilterIntegrationTest` PASS；后端全量（排除已知在飞 `BossMeasureWorkspace2AcceptanceTest`）92 suites / 301 tests / 0 failures / 0 errors PASS；App `type-check`、`build:mp-weixin`、`check:subpackages` PASS。
- 交接：`_系统/协作/lanes/development/workspace/owner-contract-api-20260923/HANDOFF.md`；生产部署与真实业主微信端到端验收未执行。

## 2026-09-22 启动失败修复（ECD-010）

- 现象：Spring 启动扫描实体时 `ClassNotFoundException: SnowflakeEntityListener`；干净重编译后进一步暴露 `CustomerOverviewContext` 多构造器导致 Spring 无法实例化。
- 处理：执行 `mvn -o -q clean compile` 恢复完整 `target/classes`；移除 `CustomerOverviewContext` 旧构造器，只保留包含 `MeasureRepository` 的单一构造器；用 `BossMeasureReportContextTest` 验证 Spring 上下文可启动。
- 防复发：登记 `ECD-010`，禁止并发 `mvn clean` 与 IDE 启动争用 `target/`，Spring 组件优先单一构造器，启动问题必须用上下文测试验证。

## 2026-09-22 客户经营指标卡改为预约量房客户

- BOSS「客户经营」第四张指标卡由“风险客户”改为“预约量房客户”。
- 后端新增 `customer/customer/overview` 的 Measure 聚合：按 `Measure.scheduledAt`（历史兼容 `measureTime`）统计本月预约量房去重客户数，排除 Cancelled；卡片日期窗口提供日增/昨日/近7日增/月增。
- 卡片副文案改为“本月完成 X 户 · 待派单 Y 户”；风险仍在客户卡等级、风险标签和负责人分组风险数中保留，不再占经营指标卡。
- 测试影响：`CustomerOverviewContextTest` 旧断言仍期待 `riskCustomer`，需 acceptance 更新为 `measureCustomer` 并补预约/完成/待派单窗口用例；开发未改测试。
- App `type-check`、`build:mp-weixin`、`check:subpackages` PASS；总包 2068482 B。

## 2026-09-22 预约量房工作台日期窗口与分组工作区

- 用户口径纠正：日期维度和报价谈单式工作区应落在「预约量房工作台」；客户页改造保留但暂停验收，后续单独讨论。
- 首页入口保持「预约量房」+ `ruler` 图标；预约量房页标题改为「预约量房工作台」/「我的预约量房」。
- 后端量房报告新增 `periods`：日增、昨日、近7日增、月增分别统计 `Measure.scheduledAt` 和 `Measure.completedAt` 的预约/完成事件数。
- App 量房工作台按确认后的职责收敛：全企业统计固定顶部，客户/房屋列表只按业务员分组，右侧客户工作区只保留客户数、待预约、已预约、已完成和风险；报价谈单式内滚动与当前卡片高亮保留。
- 业务员量房分析迁入「人员绩效考核」页：新增业务员量房业绩区块，展示日增/昨日/近7日增/月增、预约率、完成率、逾期率、待指派和排名基础数据。
- 预约量房 P0 场景稿落盘：`../../前端代码/th4-decoboon-app/docs/product/2026-09-22-企业负责人预约量房工作台2.0产品设计.md`；明确不做接单/拒单、不重做状态机，经营看板/异常待办/业务员作业列表基于现有 Measure 事实。
- 页面补充：今日预约、待派单、近7日预约、已完成、完成率、交设计率看板；待派单/预约逾期/待交设计/未预约异常条；列表主按钮按派生状态显示约定量房/立即派单/改约/查看预约/查看量房记录，次按钮联系客户。
- UI 修复：返回经营概览时重置工作区聚焦并抑制程序化滚动触发的自动聚焦，统计区恢复可见；统计日期窗口由横向滚动改为四宫格，避免横向溢出。
- 客户卡片优化：新增头像、客户/状态同一视线、电话/来源/业务员副行；房屋信息独立卡片；预约/完成时间成为主信息 chip；设计人/执行人降级为标签；下一步使用主色提示区；主操作改为品牌色，联系客户降为次操作。
- 卡片操作区再优化：查看记录/联系客户改为轻量胶囊操作，置于卡片底部右侧；主操作浅品牌底、次操作浅灰底，不再使用抢视觉的大按钮；风险左边框改为内缩渐变细条，降低突兀感。
- 验证：后端 `compile` PASS；`BossMeasureReportContextTest` PASS；排除 acceptance lane 管理的旧整改测试后 79 suites / 222 tests 全绿；App `type-check`、`build:mp-weixin`、`check:subpackages` PASS；总包 2068482 B。
- 交接：`_系统/协作/lanes/development/workspace/boss-measure-workspace-handoff.md`；待 acceptance 独立复验跨日边界、三种分组、条件叠加与真机内滚动。

## 2026-09-22 企业负责人客户经营工作区与日期窗口

- 用户要求：BOSS 首页「量房预约」补图标并改名「预约量房」；客户指标增加日增/昨日/近7日增/月增；客户筛选、负责人分组和客户列表同屏，参考报价谈单 UI。
- 首页：BOSS 快捷入口改名「预约量房」；`JzIcon` 新增 `ruler` 尺子图标。
- 后端客户聚合：`CustomerMetricCard` 增加 `periods`，对新增客户、有效跟进、签约客户分别返回今天/昨天/近7日/本月四个既有事实日期窗口；风险客户因缺少风险形成时间不回填历史，不制造趋势。
- 负责人分组：新增 `CustomerOwnerGroup` 和 `ownerId` 服务端筛选（空=全部、0=未分配）；分组摘要返回客户数、风险数、待量房、签约客户、报价额和签约额，右侧列表仍按服务端分页。
- App 客户页：BOSS 改为「条件 + 负责人分组 + 客户列表 + 内滚动」工作区；搜索/门店/状态/排序/分组叠加刷新，长列表当前客户高亮，支持放大客户区和返回经营概览；其他角色保持原列表。
- 验证：后端 `compile` PASS；`CustomerOverviewContextTest`、`ProviderSecurityAcceptanceTest` PASS；排除待升级的 `OwnerClosureAcceptanceTest` 后 77 suites / 217 tests 全绿；App `type-check` / `build:mp-weixin` / `check:subpackages` PASS（总包 2048887 B）。
- 交接：`_系统/协作/lanes/development/workspace/boss-customer-workspace-handoff.md`；真机跨日边界、分组叠加与内滚动视觉待独立 acceptance。

## 2026-09-22 阶段验收整改闭环返工（DEF-RECTIFY-001..004）

- 依据验收报告 `BLOCK` 和 `SCN-010` / `BR-008` / `DEC-007` 完成 4 项返工。
- `DEF-RECTIFY-001`：迁移 47 增加幂等存量回填，为每张历史 Acceptance 补建第 1 轮并复制状态、结论、问题、证据和整改要求。
- `DEF-RECTIFY-002`：提交/复验改为先锁 `ProjectStage` 再读写 Acceptance；已有 Pending 直接返回已有事实，不再抛「已有待确认验收单」，并发后到请求排队后走同一幂等分支。
- `DEF-RECTIFY-003`：`customer/acceptance/update` 新增 `rectificationOwnerId` / `rectificationDueDate`；驳回必须同时提供非空问题清单、当前项目团队成员责任人和截止日期。
- `DEF-RECTIFY-004`：App 业主驳回改为完整表单（问题清单 / 责任人 / 截止日期）；业主验收详情补充项目团队人员候选；员工提交表单移除“需整改项”，避免绕开业主驳回事实。
- 并发锁顺序统一为「阶段 -> 验收单」，防止复验与业主确认交叉等待。
- 验证：后端 `compile` PASS；排除暂未升级的 `OwnerClosureAcceptanceTest` 后 75 suites / 215 tests / 0 failures / 0 errors；`ProviderSecurityAcceptanceTest` PASS（路由 277）；App `type-check` / `build:mp-weixin` / `check:subpackages` PASS。
- 已知红灯：`OwnerClosureAcceptanceTest` 3 个旧契约用例未传问题/责任人/截止日期，按新契约应失败；由 acceptance lane 更新测试后再全量复验，development 未改测试。
- 待办：迁移 47 目标环境执行与回填预演、MySQL 8 并发复验、真实微信授权/真机驳回表单走查。交接已更新至 `_系统/协作/lanes/development/workspace/rectification-closure-handoff.md`。

## 2026-09-21 阶段验收整改闭环实现（4 步）

- 依据用户裁决（阶段级 / 复用同一验收单 / 不限循环 / 保留并追加结论 / 只做质量事实不碰账）实现整改闭环。
- 后端：新增 `AcceptanceRound`（append-only 轮次，迁移 47）与 `AcceptanceRoundRepository`；`ProjectContext.submitAcceptance` 改为「不合格复用同一张验收单 + 追加轮次」，第 2 轮起强制整改说明与整改后照片；`confirmAcceptance` 回填当前轮次结论，历史轮次不可修改；新增 `project/acceptance/round/page`（时间线）与 `project/acceptance/rectify/page`（整改中清单，含轮次/责任人/逾期天数）；`project/acceptance/save` 与 `customer/acceptance/update` 补齐整改输入与业主问题清单。
- App：`domains/acceptance` 增加轮次与整改清单 API；阶段验收页新增轮次时间线与复验表单（整改说明+整改后照片必填），状态色改用 `STATUS_COLOR`；新增整改跟进页 `rectify-board.vue` 与「我的 → 经营工具 → 整改跟进」入口。
- 验证：后端 `compile` PASS；`ProviderSecurityAcceptanceTest` 3/3 PASS（路由 277 = public 12 + protected 265）；`MaterialSelectionFlowTest` 6/6 PASS；App `type-check` / `build:mp-weixin` / `check:subpackages` PASS（主包 596321 B，分包 14 个 / 86 页）。
- 待办：轮次闭环端到端独立验收（驳回→复验→通过、连续 3 次驳回后通过、追加不覆盖、越权、幂等）、迁移 47 执行、业务知识库回写 `SCN-010`/`BR-008`/`DEC-005`。

## 2026-09-21 主材选材 · 产品文档/UI 梳理 + App 选材跟进台

- 产品文档梳理：新增《主材选材业务流程与交互设计》（产品+UI 统一稿）与《产品文档与UI设计索引》（分层真源、19+1 篇领域设计、5 项缺口、3 类重复收敛建议）。
- G5 · App 状态色映射：`tokens.ts` 新增 `STATUS_COLOR`（14 档）+ `uni.scss` 对应 `$jzb-status-*`，取值与现状一致（零视觉变化）；`domains/{project,schedule,acceptance,crm,finance,site}/meta.ts` 裸 hex 归零；`docs/03-视觉与设计规范.md` §9 补状态色板/各域对照/使用规则/待收敛表；顺带补齐另一 lane 缺失的 `$jzb-primary-base`、`$jzb-radius-md`。
- G4 · 选材跟进台（App 优先）：新增受保护 Provider `material/selection/board`（数据范围内「在跑项目 × 选材未客户确认」，含责任设计师与 nextAction）、投影模型 `SelectionBoardItem`、App 页面 `selection-board.vue` 与「我的 → 经营工具」入口
- 验证：后端 `compile` PASS；`ProviderSecurityAcceptanceTest` 3/3 PASS（路由 275 = public 12 + protected 263）；App `type-check`、`build:mp-weixin`、`check:subpackages` PASS（主包 590440 B，分包 14 个 / 85 页）。

## 2026-09-21 企业负责人「项目」Tab 运营视角改造

- 用户要求：企业负责人项目 Tab 要按企业运营视角展示，先做产品 UI 设计梳理再做功能研发，交互水平对齐既有「报价谈单」页面。
- 产品设计：`../../前端代码/th4-decoboon-app/docs/product/2026-09-21-企业负责人项目Tab运营视角产品设计.md`（场景 / 信息架构 / 四项指标口径 / 状态分段 / 经营卡片 / 权限 / 异常态 / 13 条验收矩阵 / 边界）。
- 场景一（负责人周一盘点在施盘子）：打开项目 Tab 先看到在建项目、在建合同额、回款率、风险项目四张指标卡，再按状态分段下钻。
- 场景二（负责人发现工地要拖期）：按「风险优先」排序找到该工地，卡片直接给出风险等级、原因、责任人、逾期天数与建议下一步。
- 场景三（负责人兼财务查回款最慢）：切「回款最慢」排序，按回款率升序查看应收/已收/逾期与下钻入口。
- 后端：新增 `project/project/overview` 受保护 Provider 与 `ProjectOverviewContext`；新增 `ProjectOverviewReport / ProjectMetricCard / ProjectStatusCount` 与 `ProjectRiskLevel / ProjectRiskType`；`Project` 新增 11 个 `@Transient` 只读投影字段。
- 口径复用：风险完全复用 `BusinessAlertContext`（逾期应收/停工与工期逾期/材料超领/变更率异常）；成本与毛利复用 `ProjectCostContext`（`costComplete=false` 时毛利为 null）；应收/已收来自 `Receivable`；合同金额优先 `ProjectContract.currentAmount`。`BusinessAlertType#nextAction()` 抽为首页与项目台账共用文案。
- App：`domains/project` 新增概览模型与 `projectApi.overview`；`pages/project/list.vue` 新增 BOSS 运营分支（指标带 / 经营排序 / 状态分段带计数 / 风险条 / 资金与交付 / 成本未完整 / 下一步），其他角色保持原施工列表。
- 验证：后端 `ProjectOverviewContextTest` 3/3 PASS、`ProviderSecurityAcceptanceTest` 3/3 PASS（路由 273 = public 12 + protected 261）；App `verify:mp-weixin` PASS（主包 587,405 B / 最大分包 241,523 B / 总包 1,943,128 B）。
- 交接：`_系统/协作/lanes/development/workspace/boss-project-tab-operations-handoff.md`。
- 用户裁决落地（同日第二轮）：① 第四张指标卡改为**本月新签项目**（含上月对比、本月签约额与环比），风险改由项目卡风险条 + 「风险优先」默认排序承载；② 门店筛选条**本轮不做**，登记 `BR-APP-035` / `APP-DATA-004`，后期按 `company → store → team → owner` 全局铺开门店概念；③ 毛利行按**实际成本口径**展示（实际成本 · 毛利 · 毛利率），成本不完整只显示「成本未完整」；④ 新增**合同维度分段**，采用「按项目状态 / 按合同履约」维度切换，合同分段为 全部 / 待签约 / 履约中 / 已完成（`ProjectContractStage` + `contractCounts`）。
- 交互补充（同日第三轮）：项目 Tab 增加**滚动居中卡片高亮** —— 视口垂直居中附近的项目卡获得品牌色边框 + 品牌色阴影（与客户经营 Tab 同一套写法）；锚点绑定「当前查询维度实际渲染的列表」，维度切换（项目状态 / 合同履约）、分段筛选、经营排序、下拉刷新和分页追加后统一重测，避免高亮停留在已消失的卡片；风险态卡片居中时品牌色优先，风险信息仍由风险条承载。
- 验证（裁决后复跑）：后端 4 套 12 用例全绿（`ProjectOverviewContextTest` 4/4、`ProviderSecurityAcceptanceTest` 3/3 且路由 273）；App `verify:mp-weixin` PASS（主包 593,271 B / 最大分包 286,590 B / 总包 1,994,650 B）。

## 2026-09-21 签约后主材选材业务流程

- 业务依据：`SCN-006` / `BR-004` / `DEC-003`（业务智能体 verified）；未冻结项 `GAP-005` 按临时建议实现，不升级为强制门禁。
- 新增 `material` 域与实体 `MaterialSelection`（表 `db_material_selection`）：文本化保存选材确认，绑定客户 + 房屋 + 合同/项目，按版本保留历史，不改报价、合同、成本、材料计划和采购事实。
- 新增 4 个受保护 Provider：`material/selection/page|info|save|confirm`；未确认版本原地保存，已确认版本保留并形成新版本；客户确认记录确认人姓名、操作员工与确认时间，重复确认幂等。
- 权限：查看走房屋数据范围或项目参与关系；维护与确认限设计师/房屋负责人/BOSS/店长/团队负责人/PM/履约；业务员与财务不可改选材事实；越权统一 404。
- App：新增 `materialSelectionApi` 与「项目详情 -> 主材选材」页面（文本编辑、提交待确认、客户确认、历史版本），`projectSelection` 路由常量与分包注册。
- 迁移：`docs/design/migrations/46-主材选材记录.sql`；设计文档：`docs/design/20-主材选材文本化设计.md`；交接：`_系统/协作/lanes/development/workspace/material-selection-handoff.md`。
- 验证：后端 `MaterialSelectionFlowTest` 6/6 PASS；`ProviderSecurityAcceptanceTest` PASS（路由 272 = public 12 + protected 260）；App `type-check`、`build:mp-weixin`、`check:subpackages` PASS。后端全量测试、迁移执行与独立 acceptance 待执行。

## 2026-09-21 报价详情分项视图与底部操作区优化

- 用户反馈：同一客户多版本报价卡片层级不直观；分项报价需要同时支持按区域和按分类查看；分类视图中的同类分项需要合并；底部按钮高度不一致，纯图标入口首次使用缺少文字提示。
- 后续反馈：分项报价分组及切换区域占篇幅过大；切换改为紧凑工具图标；所有区域/分类分组默认收起，避免大量分项把下方统计推到首屏之外。
- 场景一（设计师解释报价）：客户追问“主卧为什么比次卧贵”时，设计师在报价详情切换到“按区域”，按空间说明每个区域的分项和金额；客户追问“同一种瓷砖全屋用了多少”时，切换到“按分类”，看到跨区域合并后的数量、金额和覆盖区域。
- 场景二（装企负责人首次查看）：底部操作区统一为 92rpx 高度，并按“查看方案 / 谈单 PPT / 打印稿”显示图标和文字；“提交客户确认、调整套餐、驳回、客户已接受、转为施工项目、改版重报”等主流程动作按报价状态展示，减少首次使用的猜测成本。
- 实现：改造 `src/subpackages/quote/pages/detail.vue`；客户报价多版本卡改为版本号、状态、金额和“当前查看/查看此版本”选择器；新增区域/分类分段切换；分类视图按 `type + category` 分组，并按名称、品牌、规格、单位、计价方式、单价、角色、增减性质和组件一致性合并同类明细，合并行展示数量、金额和区域明细数；`QuoteItemRow.vue` 增加合并展示参数。
- 信息密度收敛：分项报价头部改为单行标题、组数和两个紧凑图标工具（区域 / 分类）；分组行压缩为单行标题、进度/合并数、金额和展开箭头；默认折叠状态通过 `isGroupCollapsed(key)` 统一控制，首次进入不展开任何分组。
- 边界：只改变前端视图结构和展示，不改变 `QuoteLine` 快照、金额、成本、权限或报价状态；区域/分类切换不触发接口写入，不改变报价事实。
- 验证：`npm run type-check` PASS；`npm run build:mp-weixin` PASS；`npm run check:subpackages` PASS（主包 563084 B，最大分包 229433 B，总包 1795328 B）；此前的 H5 构建 PASS。
- 风险：微信开发者工具/真机上的横向多版本卡滑动体验、弱网加载、不同长名称区域合并文案和底部两行操作区遮挡仍需 acceptance 复验；独立验收未执行。

## 2026-09-20 客户经营聚合与负责人客户卡

- 新增受保护 Provider `customer/customer/overview`，一次返回四张 BOSS 客户经营指标卡、完整客户阶段计数和分页客户列表。
- 指标固定为新增客户、有效跟进、签约客户、风险客户；统计由服务端按企业/门店范围计算，不再依赖 App 当前分页列表。
- 客户卡新增只读经营投影：负责人、停留天数、风险等级、风险原因、下一步、最近报价金额、签约金额和关联项目。
- App 客户 Tab 对 BOSS 展示四卡、风险/停留/金额排序和服务端阶段计数；其他角色仍保留原业务执行视图。
- 客户详情页“编辑资料”“新增房屋”“编辑房屋”入口统一为“完善画像”样式：操作图标和文字使用品牌主色，房屋卡文案改为“编辑房屋”。
- 修复客户 Tab 导航不一致：`pages/customer/list` 改为自定义导航，补齐 `JzPageHeader`、`JzTabBar` 和底部安全区，和项目/财务 Tab 使用同一页面骨架。
- 设计文档：`docs/design/19-客户经营聚合与负责人客户卡设计.md`；开发交接：`_系统/协作/lanes/development/workspace/customer-overview-aggregation-handoff.md`。
- 验证：后端 63 suites / 182 tests 全绿；App type-check、构建和分包检查通过；真实本地 BOSS 会话与空库分页回读通过。

## 2026-09-20 企业自定义付款与施工计划模板编辑器

- 新增企业付款计划/节点和企业施工计划/阶段四张表及迁移 43，平台基线保持只读，企业可复制、编辑、启用、停用和设默认。
- 新增 12 个受保护 Provider：付款模板和施工模板各 page/save/update/publish/disable/default；写操作仅允许 BOSS，企业归属由登录员工覆盖。
- 合同签约可用目录优先返回企业默认/已启用模板，再回退平台基线；签约仍复制为企业合同快照，历史合同不随模板修改。
- App「我的 -> 经营工具」新增付款计划模板、施工计划模板，完成列表、复制、编辑、启停、默认设置及节点/阶段编辑器。
- 设计文档：`docs/design/18-企业自定义付款与施工计划模板设计.md`；开发交接：`_系统/协作/lanes/development/workspace/enterprise-plan-template-editor-handoff.md`。
- 验证：后端 62 suites / 181 tests 全绿；App `verify:mp-weixin`、类型检查、构建和 13 分包检查 PASS；本地真实 BOSS 会话完成复制、启用、设默认和合同目录回读闭环。
- 迁移 43 已在本地开发库执行，目标测试/生产环境及微信页面级独立验收待执行。

## 2026-09-20 企业负责人首页统计区状态点收敛

- 按用户反馈将 BOSS 首页四张统计卡的状态文字胶囊和常驻说明收敛为单一彩色状态点；点击状态点后通过弹窗查看当前状态和判断依据，卡片边框不再重复承载状态色。
- 保留后端四项指标的判定阈值，仅补充未设置目标时的解释文案：签约额明确“未设置目标，按环比判断”，回款额不再误显示“目标达成 0%”，在建项目明确数量目标不参与健康判定。
- 验证：后端 `mvn -o -q test` PASS；App `npm run check:home`、`npm run type-check`、`npm run build:mp-weixin`、`npm run check:subpackages`、`npm run build:h5` PASS。
- 风险：真实 BOSS 账号下的状态点弹窗、弱网错误态和微信开发者工具/真机视觉交互仍待 acceptance 复验。

## 2026-09-20 企业负责人首页改造

- 按用户确认口径收敛 BOSS 首页：四项经营指示灯、六个高频功能域、风险项目、经营动态和“我的”入口边界。
- 新增受保护 Provider `statistic/boss/workbench`，聚合四项指示灯、风险项目及客户/报价/合同/项目/验收/收款/付款经营事件；事件读取限制每类最近 200 条并过滤 180 天窗口。
- App 首页新增状态灯和健康原因、风险项目原因/责任人/下一步、经营动态；新增经营动态详情页及四类筛选；原同频动态更名为工地同频动态。
- “我的”移除 BOSS 重复的经营驾驶舱、人员绩效、设计调度、履约驾驶舱和全公司项目入口，保留企业、账号、订阅、组织、配置和服务支持。
- 产品设计：`../../前端代码/th4-decoboon-app/docs/product/2026-09-20-企业负责人首页产品设计.md`。
- 开发交接：`_系统/协作/lanes/development/workspace/boss-homepage-redesign-handoff.md`。
- 验证：后端 61 suites / 177 tests / 0 failures / 0 errors；Provider 安全路由 241（protected 229）；App `npm run verify:mp-weixin`、首页契约、类型检查、构建和分包检查 PASS。
- 运行态修复：经营动态空库时改为明确空态和起步动作；新增开发环境 `VITE_API_BASE` 校验，启动前拒绝 localhost/127.0.0.1，避免微信开发者工具直接阻断请求。
- 未新增迁移；真实 BOSS 账号、微信开发者工具/真机视觉交互和独立 acceptance 待执行。

## 2026-09-20 合同付款计划与施工计划基线

- 新增 3 套付款计划基线和 2 套住宅施工计划基线；新增合同级付款/施工快照、阶段编码及应收触发关联。
- 合同创建/签约支持 `paymentPlanCode + constructionPlanCode`；签约时按最终签约价重算付款节点，生效时按施工快照生成阶段并绑定应收。
- App 签约入口已增加计划选择与预览；旧合同保留无快照回退逻辑。
- 验证：后端 59 suites / 175 tests / 0 failures / 0 errors；App 小程序全量验证 PASS
- 迁移 `42-付款计划与施工计划快照.sql` 未执行，等待目标环境验收。

## 2026-09-20 全角色启动引导与报价 PDF 归档

- 完成 `REQ-20260920-DEV-ONBOARDING-ROLE-GUIDE`：新增 `statistic/onboarding/report`，服务端按真实领域对象推导 Boss 三件套、可选步骤及 8 类员工/业主岗位首任务；修复客户+房屋全量谓词、必做源异常 unknown、可选源异常不阻塞和 App 跨页刷新。
- 完成 `REQ-20260920-DEV-DOCUMENT-QUOTE-PDF`：新增平台行业单据模板基线、企业模板继承/复制/受控字段样式、企业抬头资料、报价客户版 PDF、私有归档、SHA-256 和重打轨迹；App 报价详情新增打印稿入口。
- 验证：后端 `mvn -o -q test` 57 suites / 173 tests / 0 failures / 0 errors；App `npm run verify:mp-weixin` PASS
- 迁移 `41-单据模板与PDF归档.sql` 未执行；真实 MySQL、OSS、微信打开 PDF 和独立 acceptance 待执行。

## Current

`REQ-20260920-DEV-MINIAPP-ICP` 已登记小程序备案号：`皖ICP备2021009474号-2X`（来源：小程序管理员提供，工信部备案系统 https://beian.miit.gov.cn/ ）。`docs/design/migrations/20-小程序发布合规基线.md` §2 增加备案信息登记与主体一致性待核验项，§5 提审清单拆出“备案号已提供/主体一致/域名备案与证书”三项，§7 发布阻断项同步更新为“备案主体一致性待核验 + API 域名备案与证书链待归档”。备案号属于小程序本体，不等同于 `decoboon.joytsing.com` 的域名 ICP 备案；主体一致性核验与验收由 acceptance/运维执行。


`REQ-20260915-DEV-MINIAPP-PASSWORD` 已实现小程序密码设置闭环：现有受保护的 `auth/session/password-change` 在未设置密码时执行首次设密并记录 `PasswordSet`，已设置时必须校验原密码，同时兼容小程序微信身份令牌与 员工令牌；`auth/session/info` 增加 `passwordSet`；小程序新增“我的 -> 账号密码”设置/修改页。后端全量 `79 tests / 0 failures / 0 errors`，`npm run verify:mp-weixin` 通过


真实浏览器闭环验证补充：已通过 `18660219918` 登录 `127.0.0.1`，验证施工/主材/软装目录新增、编辑、停用；设计 Excel 两行粘贴、保存和报价生成；报价提交、接受、签约、合同生效立项；增减项 700 元双明细批量录入、审批和客户确认；项目完工、6 阶段验收、收款和资金流水。过程中修复客户确认响应膨胀、合同变更履约 property 空值、锁组件条件注册、报价状态推进后详情丢失、方案锁定提示、完整目录分页、通用编辑 ID 注入、目录更新 itemId 映射、财务金额/百分比/枚举显示等问题。



`REQ-20260916-DEV-WECHAT-PAY` 已完成微信支付 API v3 参数部署：核对商户号 `1750623009`、AppID `wxe5cc5fec9e50a9a9`、证书序列号 `24A00BDC638A9B5BE07C8E6BF396096BA379AE07` 和微信支付公钥 ID。商户证书、私钥、PKCS#12 和公钥已放入 Git 忽略目录 `config/wechat-pay/`，API v3 密钥和商户参数放入权限 600 的 `.env.wechat-pay.local`；代码仓库不再保留敏感默认值。正式交易查询接口验签通过，Spring `WechatPayClient` 调用通过，新增启动期配置和密钥文件校验。

`REQ-20260916-DEV-PROD-RELEASE` 已完成生产部署并复核：`application-prod.yml`、`cmd.sh`、systemd、Nginx HTTPS/SPA 和发布脚本生效；后端 `20260916-0415` 已上线。空库完成 88 张表和 `01/10/11/12/17/21` BASELINE 账本，`decoboon.service` active/enabled，`cmd.sh check` 通过，Nginx `nginx -t` 通过并监听 80/443，HTTP 301、HTTPS 首页/静态 JS/API 鉴权/支付回调路由冒烟通过；TLS 证书有效期至 2026-12-13 UTC。

验证记录：小程序 `verify:mp-weixin` 通过；迁移与恢复脚本 `bash -n` 通过。

`REQ-20260915-DEV-FIX-P05-FINAL` 已把迁移策略收敛为：新空库正式支持 `baseline`，只执行 `00-迁移账本.sql` + `01-空库基线.sql`；存量库不得自动跑不完整 `upgrade`，必须 DBA 审计后 `adopt` 登记，差异迁移另行完整设计。`10-设计转化域迁移.sql` 已标记为不完整历史草稿并禁止自动执行，密码登录迁移为 `21-用户密码登录.sql`。`baseline` 失败会尝试写入 `FAILED`，脚本对 `RUNNING`/`FAILED` 拒绝自动重跑；迁移文件和恢复模板增加 SHA-256 兼容校验、漂移检测及 `realpath` 规范化危险根路径判断。真实 MySQL 8 基线、差异迁移和恢复演练仍为 BLOCKED，静态/故障注入通过不等于生产演练通过。

最终本地验证：三个模板 `bash -n` 通过；fake mysql 故障注入 `baseline` 返回退出码 `4`、账本为 `FAILED|...` 且输出 `FAILED 01`；checksum 漂移返回退出码 `8` 和 `CHECKSUM_DRIFT`；`upgrade` 返回退出码 `9` 和 `UPGRADE_DISABLED`；`RESTORE_ROOT=/var/..` 经规范化后返回退出码 `2`；`mvn -o -q test` 退出码 `0`，Surefire `72 tests / 0 failures / 0 errors / 0 skipped`。





`REQ-20260916-DEV-PROD-SENTINEL` 已建立生产后端哨兵闭环：新增只读采集与分析脚本、项目级 `backend-prod-sentinel` Skill、受控发布/回滚脚本和事故 registry；监控首次运行捕获 `auth/session/info` 的 `LazyInitializationException`，已通过 `CredentialsRepository` 的 `@EntityGraph` 加载员工、门店、团队和企业关系，新增非事务回归测试 `AuthSessionInfoLazyAcceptanceTest`。排除两个既有陈旧静态断言后全量测试通过；发布包部署到 `/opt/decoboon/releases/20260916-053143`，首次部署因就绪等待不足触发自动回滚，修正后重新发版成功，HTTPS 健康检查返回 200。

## Outputs

| title | path | audience | status |
|---|---|---|---|
| 业主闭环 P0 后端交接 | `_系统/协作/lanes/development/workspace/owner-closure-handoff.md` | product-planning / acceptance / 前端 | ready |
| 业主闭环生产迁移建议 | `_系统/协作/lanes/development/workspace/owner-closure-migration.sql` | acceptance / 运维 | migration_required |
| 业主闭环正式迁移 | `docs/design/migrations/17-业主闭环.sql` | acceptance / 运维 | ready |
| 私有文件访问发布说明 | `docs/design/migrations/18-私有文件访问发布说明.md` | acceptance / 运维 / 前端 | ready |
| 生产发布基线 Runbook | `docs/design/migrations/19-生产发布基线.md` | acceptance / 运维 | ready_with_blockers |
| Provider 安全扫描报告 | `_系统/协作/lanes/development/workspace/provider-security-scan.md` | acceptance / 运维 | ready |
| Provider 安全逐路由清单 | `_系统/协作/lanes/development/workspace/provider-security-scan.csv` | acceptance / 运维 | ready |
| 小程序发布合规基线 | `docs/design/migrations/20-小程序发布合规基线.md` | acceptance / 运维 / 产品 | ready_with_blockers |
| 小程序发布门禁 | `../../前端代码/th4-decoboon-app/scripts/verify-release-env.mjs` | acceptance / 运维 / 前端 | ready |
| P0-A 权限状态账务修复交接 | `_系统/协作/lanes/development/workspace/p0a-confirmed-fixes-handoff.md` | acceptance / product-planning | ready_for_acceptance |
| 员工密码会话 | `auth/session/password-login`、`auth/session/password-setup` 公开；`password-change`、`logout` Security | acceptance / 运维 | ready |
| 小程序密码设置闭环开发交接 | `_系统/协作/lanes/development/workspace/miniapp-password-closure-handoff.md` | acceptance / user | ready_pending_acceptance |
| 阿里云 OSS 对象存储 | `docs/design/migrations/22-OSS对象存储部署说明.md` | acceptance / 运维 / development | configured_pending_acceptance |
| OSS 存储开发交接 | `_系统/协作/lanes/development/workspace/aliyun-oss-storage-handoff.md` | acceptance / 运维 / user | ready_pending_acceptance |
| 微信支付 API v3 配置 | `docs/design/migrations/23-微信支付配置.md` | acceptance / 运维 / development | configured_pending_acceptance |
| 微信支付配置交接 | `_系统/协作/lanes/development/workspace/wechat-pay-config-handoff.md` | acceptance / 运维 / user | ready_pending_acceptance |
| 生产发版包与配置 | `_系统/协作/lanes/development/workspace/production-release-config-handoff.md` | acceptance / 运维 / product-planning | deployed_pending_acceptance |
| 生产后端哨兵 | `_系统/协作/lanes/development/workspace/backend-prod-sentinel-handoff.md` | acceptance / 运维 / development | deployed_fixed_first_incident |
| 用户密码迁移 | `docs/design/migrations/21-用户密码登录.sql` | DBA / acceptance | ready |
| 空库基线 DDL | `docs/design/migrations/01-空库基线.sql` | DBA / acceptance | generated_unverified |
| 迁移账本 | `docs/design/migrations/00-迁移账本.sql` | DBA / 运维 | ready |
| 迁移执行模板 | `docs/design/migrations/run-migrations.sh.template` | DBA / 运维 | ready_unverified |
| 安全恢复模板 | `docs/design/migrations/rollback-safe.sh.template` | DBA / 运维 | ready_unverified |

## Requests

| to | request | status | link |
|---|---|---|---|
| acceptance | 独立验证 OSS 企业/项目隔离、跨租户拒绝、预签名过期、大文件/视频和历史文件迁移方案。 | pending_acceptance | `_系统/协作/lanes/development/workspace/aliyun-oss-storage-handoff.md` |
| acceptance | 独立验证微信支付 JSAPI 预下单、支付查询、真实回调验签解密、金额/商户/AppID 边界和重复通知。 | pending_acceptance | `_系统/协作/lanes/development/workspace/wechat-pay-config-handoff.md` |
| acceptance | 使用真实企业账号验证生产 API 全业务闭环、微信支付首笔回调和密钥/备份回滚。 | pending_acceptance | `_系统/协作/lanes/development/workspace/production-release-config-handoff.md` |

## Notes

- 职责：负责功能实现、生产代码修改、自测证据和按授权提交代码，不负责最终验收结论。
- 跨出写入范围前，必须先登记共享请求或取得用户明确授权。
- 交接必须包含可核验的文件、验证结果、风险和下一步。

## Next

- acceptance lane 独立验证邀请消费、显式项目授权、会话身份隔离、owner-only 读取和验收状态机。
- acceptance lane 复验匿名 URL、签名过期、跨企业/跨项目文件授权和业务对象路径绑定。
- acceptance lane 复验 Provider Security 扫描、公开白名单、订阅写闸门和 prod profile fail-fast。
- acceptance lane 和产品/法务确认小程序正式域名、AppID、协议正文、隐私政策和真机提审材料。
- DBA 在隔离 MySQL 8 环境验证 `baseline`、DBA 审计后的 `adopt` 和 `rollback-safe` 模板并留存账本证据；任何存量库差异迁移必须先形成独立完整迁移并通过评审。
- acceptance lane 独立验证小程序首次设密 -> 密码登录闭环，并执行微信开发者工具/真机交互回归。
- acceptance lane 独立验证 OSS 上传、私有读取、预签名过期、跨企业和跨项目拒绝；运维确认历史 `upload/**` 文件迁移与生产 AccessKey 注入。
- acceptance lane 在真实支付环境验证 JSAPI 预下单、查询、回调验签解密、订单绑定和异常金额/重复通知；运维建立证书与微信支付公钥到期监控。
- acceptance lane 使用生产真实 Designer/Boss/Finance 账号验证设计明细粘贴保存、报价生成导出、增减项批量提交审批，以及批量接口的跨企业越权负向用例。
- 运维执行生产数据库备份恢复与上一版本回滚演练，并建立 TLS、微信支付证书和 API v3 密钥到期监控。
- 法务、产品和小程序管理员提供正式协议文本、AppID、域名与备案材料后执行 `build:mp-weixin:release` 提审门禁。
- 微信真机授权、弱网、401/邀请失效和真机页面回归。

## 2026-09-16 生产懒加载缺陷修复（INC-20260916-a1233597b0）

- 事故：`PlanVersion` `LazyInitializationException`，命中 `project/project/page`、`project/report/page`、`collab/notice/count`，共 42 次（05:40 - 06:04）。
- 根因：生产 `application-prod.yml` 为 `spring.jpa.open-in-view: false`，而 `SubscriptionAccessRequestFilter`（事务外）→ `SubscriptionContext.requireModule` → `effectiveVersion` → `subscription.getPlanVersion().getModules()` 访问未初始化代理；dev/test 默认 OSIV=true 掩盖了该缺陷。
- 源码快照：`dist/source-backups/th4-decoboon-source-20260916-064737.tar.gz`（sha256 `1de1025d1088def66d306bc7ffa8e97f2672a5fc7be2896604dc12dc828026b8`）。
- 修复：`src/main/java/com/th4/edge/tenant/billing/repository/SubscriptionRepository.java` 为 `findByCompany_ComId` 增加 `@EntityGraph(attributePaths = {"planVersion", "plan", "company"})`（单文件、单根因）。
- 回归测试：新增 `src/test/java/com/th4/edge/tenant/billing/SubscriptionLazyLoadingRegressionTest.java`，显式 `spring.jpa.open-in-view=false` 且不加 `@Transactional`；修复前 3/3 失败，修复后 3/3 通过。
- 定向测试：`SubscriptionLazyLoadingRegressionTest`(3) + `SubscriptionBillingClosedLoopTest`(1) + `SubscriptionAccessFilterAcceptanceTest`(4) 全部通过。
- 全量测试：83 项，3 项历史无关失败（`ProductionReleaseBaselineAcceptanceTest` 2 项部署文档断言、`ProviderSecurityAcceptanceTest` 1 项路由数 196→197），已单独记录并证明与本次改动无关。
- 发版：release `dist/releases/20260916-065142` → 生产 `/opt/decoboon/releases/20260916-065204`（上一版 `20260916-053143`），配置校验通过。
- 发版后验证：`decoboon.service` active、HTTPS health 200；10 分钟窗口采集无该指纹新增（最近一次仍为发版前 06:04:02）。
- 规范沉淀：新增 `_系统/协作/lanes/development/workspace/code-error-catalog.md`（ECD-001）；`rules.md` 新增 §4.5 事务边界与懒加载禁则并写入 §8 自检清单；`AGENTS.md` 新增「事务边界与懒加载禁则（强制）」。

## 2026-09-16 生产 OSIV 配置调整发版（open-in-view=true）

- 决策：项目负责人要求将生产 `spring.jpa.open-in-view` 由 `false` 调整为 `true`（配置文件 06:55 修改，未改其他文件）。
- 测试：全量 `mvn -o -q test` 83 项，失败仍为同一组 3 项历史问题（`ProductionReleaseBaselineAcceptanceTest` ×2、`ProviderSecurityAcceptanceTest` ×1），无新增回归；`SubscriptionLazyLoadingRegressionTest` 继续在 `open-in-view=false` 下通过。
- 构建：`dist/releases/20260916-065823`，已校验包内 `config/application-prod.yml: open-in-view: true`。
- 发版：生产 `/opt/decoboon/releases/20260916-065845`（上一版 `20260916-065204`），配置校验通过。
- 复核：服务器生效配置 `open-in-view: true`、release 指向新版本、service active、HTTPS health 200、进程 06:59:33 启动；10 分钟窗口无 LazyInitializationException 新增。
- 风险：OSIV=true 会占用请求级数据库连接（Hikari 默认上限 10）并掩盖懒加载设计问题；`@EntityGraph` 修复保留，规范禁则不放宽。

## 2026-09-16 本地开发密钥加载机制（IDEA 可直接启动）

- 问题：IDEA 直接运行 `com.th4.edge.Application` 不加载 `.env.*.local`，导致小程序微信登录返回 `503 微信小程序 appId/appSecret 尚未配置`；同时 OSS 降级为本地存储、微信支付保持 disabled。代码未变，仅启动方式不同。
- 方案：利用 Spring Boot 自动加载 `file:./config/application-dev.yml`（优先级高于 classpath）的特性，把 `.env.*.local` 转成 Spring 格式的外置 dev 配置，**无需修改 `src/main/resources`**。
- `.gitignore` 新增 `config/application-dev.yml`、`.env.dev.local`。
- 新增文档 `docs/deploy/LOCAL-DEV.md`（IDEA/命令行启动、依赖、自检命令、安全约束）。
- 验证（`env -i` 完全干净环境启动，等同 IDEA 不注入变量）：
  - 启动成功 `Started Application in 10.159s`；
  - `auth/session/login` → `400 微信登录失败：invalid code`（不再是 503 未配置）✅
  - `order/callback` 空请求 → `403 微信支付回调签名缺失`（不再是"商户号...尚未配置"）✅
  - OSS：生成配置 `provider=oss` + access-key 已配置，与支付同文件同源加载；端到端上传验证见 `docs/deploy/LOCAL-DEV.md` §5。
- 安全提醒：`.env.dev.local` 目前仍被 git 索引跟踪（`git ls-files` 可见），仓库尚无 commit；建议 `git rm --cached .env.dev.local` 并轮换其中密钥。

## 2026-09-16 生产后端监控（exit 20，仅归档）

- 运行时间：2026-09-16 13:02:32 +08:00；命令：`bash scripts/prod-monitor.sh collect "35 minutes ago"`。
- 结果：退出码 20；服务 enabled/active/running，HTTPS health 200，根盘 15%，未发现基础设施异常。
- 历史复核：`INC-20260916-a1233597b0` 保持 fixed，最近发生 06:04:02，本次 35 分钟窗口无新增。
- 新发现：`invite/invitation/save` 因请求字段 `telNo` 与 Provider 注入名 `invitePhone` 不一致，导致 `Invite.telNo` 非空约束失败；另行记录授权过期和 `config` 扫描探测两个 unknown。
- 门禁：collect 返回 20，自动修改源码、快照、测试、构建和发版均禁止；未改源码、未重启、未发版。
- 证据与建议：`docs/operations/incidents/2026-09-16/20260916-130232/triage.md`，邀请字段绑定问题保持 open 等待人工确认或符合自动修复门禁的运行。

## 2026-09-16 本地 dev 库数据清空（用户要求）

- 目标：本地 `127.0.0.1:3306/decoboon`（主机 `MacBook-Pro-3.local`，**非生产**）。
- 备份：`/private/tmp/decoboon-backup/decoboon-20260916-131526.sql`（977,939 bytes，mysqldump `--single-transaction --routines --triggers`）。
- 执行：生成 `/private/tmp/truncate-decoboon-all.sql`（`SET FOREIGN_KEY_CHECKS=0` + 87 张表 `TRUNCATE`），执行 0 错误。
- 校验：87 张表结构完整；关键表 company/employee/credential/store/customer/project/quote/catalog_item/plan/notice 全部为 0；对估算非空的 8 张表逐表精确 count，结果全部为空。
- 影响与恢复：
  - 企业/员工/凭证已清空 → 小程序登录将进入"首次登录授权手机号 → 注册企业"流程；
  - 套餐（plan/plan_version/plan_addon）清空 → 重启后端后 `SubscriptionPlanInitializer` 会自动重新 seed；
  - 报价项目库 `catalog_item`（原 554 条）清空 → 需重新执行 `scripts/import-maihong-catalog.py` 导入；
  - 需要恢复时：`mysql -uroot -pmysqladmin decoboon < /private/tmp/decoboon-backup/decoboon-20260916-131526.sql`。

## 2026-09-16 小程序「未加入企业」页优化

- 文件：`../../前端代码/th4-decoboon-app/src/pages/company/join.vue`（重写）。
- 邀请入口：恢复并扩展为三种输入——邀请编号（纯数字）、邀请链接（自动解析 `inviteId`/`token`）、纯 token 串；员工邀请走 `invite/invitation/info`，业主邀请走 `identity/customer-invite/info`，自动分流后跳转 `pages/invite/accept`。
- 按钮改版：原「创建新企业」（primary）+「切换微信账号」（plain）两个裸露按钮，改为底部**并排**两个次要按钮（`jz-btn--ghost` 创建新企业 / `jz-btn--neutral` 切换微信账号），主次分明。
- 规范修正：原页面有 2 个主色实心按钮，违反 `AGENTS.md §5.4`「单页面主色实心按钮 ≤ 1 个」，现仅保留「查看邀请」一个主 CTA。
- 视觉与交互：身份卡（头像首字 + 昵称 + 手机号 + 未归属企业徽标）、邀请卡（标题/说明/输入/主按钮/提示）、回车提交、提交 loading + disabled 防重复、切换账号二次确认；间距均为 8 的倍数，圆角 2 种（卡片 20rpx / 控件 12rpx），卡片具备 background+radius+shadow。
- 验证：`npm run type-check` PASS；`npm run build:mp-weixin` PASS（DONE Build complete）；`npm run check:subpackages` PASS（主包 395KB / 最大分包 155KB / 总包 1.13MB）；dev watch 已将产物刷新到 `dist/dev/mp-weixin`。

## 2026-09-16 小程序经营驾驶舱「设置目标」交互优化

- 文件：`../../前端代码/th4-decoboon-app/src/subpackages/statistic/pages/analysis.vue`。
- 根因：全局 `.jz-btn` 使用 `display: inline-flex`（宽度自适应内容），而 `.ana-target__save` 只有 `margin-top`、没有任何对齐设置，导致「保存目标」按钮渲染在面板**左下角**且为孤立小按钮，视觉突兀。
- 改法：
  - 目标面板底部改为**右对齐操作区**：`[取消 · jz-btn--neutral] [保存目标 · jz-btn--primary]`，保存按钮位于右侧；
  - 面板头部补充月份标识（标题 + 右侧 `2026-09`）；
  - 四个目标字段增加单位后缀（元 / 个），输入控件统一为「容器背景 + 圆角 + 内嵌 input」结构；
  - `saveTarget` 增加 `savingTarget` 状态，按钮 `:loading` + `:disabled` 防重复提交，并新增 `closeTarget()` 取消路径；
  - 间距修正为 8 的倍数：grid gap `18→16rpx`、`margin-top 22→24rpx`、输入高度 `78→80rpx`、label 间距 `10→8rpx`，面板 padding `24→32rpx`。
- 验证：`npm run type-check` PASS；`npm run build:mp-weixin` PASS；dev watch 已刷新 `dist/dev/mp-weixin/subpackages/statistic/pages/analysis.wxml`。

## 2026-09-16 小程序 TabBar 遮挡内容修复（全 TabBar 页面）

- 根因：`JzTabBar.vue` 为 `position: fixed; bottom: 0`，实际高度 = 8rpx(padding) + 92rpx(min-height) + 8rpx + `safe-area-inset-bottom` ≈ **108rpx + 安全区**；而各 TabBar 页面底部占位普遍只有 24–64rpx，导致内容滚到底仍被 TabBar 遮挡（首页、我的、动态、项目、财务、人员绩效均受影响）。
- 统一方案：在 `src/styles/base.scss` 新增全局占位类
  `.jz-tabbar-gap { height: $jzb-tabbar-height; padding-bottom: $jzb-safe-bottom; }`
- 逐页修复：
  - `src/pages/index/index.vue`：新增 `<view class="jz-tabbar-gap" />`（原无占位）；
  - `src/pages/finance/index.vue`：新增占位（原无占位）；
  - `src/pages/mine/index.vue`：`.mine__pad`（height 30rpx）→ 全局占位类，并删除原样式；
  - `src/subpackages/statistic/pages/performance.vue`：`.perf__pad`（height 24rpx）→ 全局占位类，并删除原样式；
  - `src/pages/dynamic/index.vue`：`.dy__body` 底部 `40rpx` → `calc($jzb-tabbar-height + $jzb-safe-bottom + 24rpx)`；
  - `src/pages/project/list.vue`：`.pl__body` 底部 `64rpx` → 同上 calc 表达式。
- 验证：`npm run type-check` PASS；`npm run build:mp-weixin` PASS；dev watch 已刷新产物，`app.wxss` 含全局类、4 个页面 wxml 含占位节点。

## 2026-09-16 小程序「成员管理」页交互优化

- 文件：`../../前端代码/th4-decoboon-app/src/subpackages/company/pages/members.vue`（重写，209 行）。
- 筛选栏：三个独立主色实心方块（选中态深绿底白字、`gap: 14rpx`）→ **分段控件**（白底容器 + 8rpx 内边距 + 选中态 `$jzb-primary-lighter` 浅底 + 主色文字），点击区域由 `<text>` 改为 `<view>`（min-height 72rpx），新增 `switchFilter()` 避免同项重复请求。
- 成员卡片：原右侧竖排三个 22rpx 裸文字（点击区域极小、与信息抢位）→ 拆分为「信息区 + 底部等宽操作区」：
  - 信息区：头像 + 姓名 + 状态标签 + 角色/门店/团队 + `chevron-right`，整块可点进详情；
  - 操作区：`border-top` 分隔，每项 `flex:1`、`min-height: 88rpx`、带图标，项间 1rpx 分隔线，`:active` 背景反馈；
  - 状态分支：在职=`交接/停用/注销`，停用=`恢复在职/注销`，已离职=`已离职，仅可查看`（只读提示），危险操作（注销）使用 `$jzb-danger`。
- 其他：卡片由「一张大卡片包多行」改为「每成员一张卡片 + 16rpx 间距」，卡片具备 background+radius+shadow；间距统一 8 的倍数（原 14rpx/26rpx/12rpx/10rpx/6rpx 全部修正）。
- 验证：`npm run type-check` PASS；`npm run build:mp-weixin` PASS；`npm run check:subpackages` PASS；dev watch 已刷新 `members.wxml`。

## 2026-09-16 小程序「新建/编辑客户」表单对齐与地址展示修复

- 文件：`../../前端代码/th4-decoboon-app/src/subpackages/customer/pages/edit.vue`。
- 根因 1（性别/客户来源值不靠右）：`<picker>` 作为 flex 子项没有 `flex: 1`，宽度收缩到内容宽度，导致内部 `.ce__picker { justify-content: flex-end }` 没有可分配空间，值只能贴着 label 显示。
- 根因 2（房屋地址显示不全且不靠右）：地址行使用 `.ce__row--col`（`flex-direction: column; align-items: flex-start`），input 在交叉轴宽度收缩，且与其他字段布局不一致。
- 修复：
  - 新增 `.ce__row picker { flex: 1; min-width: 0; }`，picker 撑满剩余宽度，选中值真正右对齐；
  - `.ce__picker` 增加 `gap: 8rpx` 与文本溢出保护（`overflow/text-overflow/white-space`）；
  - 房屋地址改回横向行（与姓名/手机号/意向小区一致），input 沿用 `flex: 1; min-width: 0; text-align: right`，宽度从"内容宽"变为"占满剩余宽度"；`maxlength=200`，placeholder 精简为"详细地址，转待量房前必填"；
  - 纵向行样式仅保留给「备注」textarea；间距修正：`padding: 18→16rpx`、提示条 `padding: 20→24rpx`。
- 验证：`npm run type-check` PASS；`npm run build:mp-weixin` PASS；dev watch 已刷新 `edit.wxml/edit.wxss`（含 `.ce__row picker` 规则与新 placeholder）。

## 2026-09-16 小程序「客户列表」层次感与客户卡片优化

- 文件：`../../前端代码/th4-decoboon-app/src/pages/customer/list.vue`（仅模板展示结构与样式，业务逻辑未改动）。
- 客户卡片重构为**三层结构**：
  1. 主行 `cl__item-head`：头像 + 姓名 + 阶段标签 + 电话（左），**跟进状态右对齐独立显示**（逾期/今日/下次，按 tone 着色）；
  2. 信息行 `cl__item-facts`：`border-top` 分隔，图标 + 小区 · 面积 · 预算（无值时降级占位，预算为空不显示）；
  3. 标签行 `cl__item-tags`：微信/到店/量房/设计（或待指派设计）/已转项目，最多 5 枚，`gap: 8rpx` 换行；
  4. 营销角色额外一行 `cl__item-foot`：黄金时段标签 + 录入天数。
  - 删除原右侧竖排的 `chevron-right + 已转项目 + 跟进文案` 混排结构（信息竞争、阅读跳跃），以及不再使用的 `.cl__who/.cl__concept-tags/.cl__right/.cl__meta-line/.cl__project/.cl__gender` 样式（残留检查为 0）。
- 页面层次与间距：
  - 卡片由「flex 横排 + 内嵌多行」改为「上下分层 + 分隔线」，单卡片信息分三档字号/颜色，主次清晰；
  - 卡片间距 `18→16rpx`、内边距 `26→24rpx`；搜索框高 `84→88rpx`；筛选 chip `62→64rpx`、右间距 `14→16rpx`；跟进指标条 padding/margin 全部归一；
  - 全页非 8 倍数间距（12/18/22/14/26/10/7/6/4/28/150rpx）统一修正为 8 的倍数（16/24/32/152rpx 等）。
- 验证：`npm run type-check` PASS；`npm run build:mp-weixin` PASS；`npm run check:subpackages` PASS（主包 395KB / 总包 1.13MB）；dev watch 已刷新 `list.wxml`（含 `cl__item-head`、`cl__item-facts`、`cl__item-tags`）。

## 2026-09-16 小程序客户详情链路体验优化（详情/跟进 + 2 个新页面）

- 全局修复：`src/styles/base.scss` 的 `.jz-section__more` 原为纯文本样式（无 `display:flex`），区块标题右侧的「新增房屋」「编辑资料」出现**图标与文字换行**；改为 `inline-flex + align-items:center + gap:8rpx + flex-shrink:0`，全站同类入口一并修复。
- 客户详情 `src/subpackages/customer/pages/detail.vue`：
  - 顶部客户卡片重构为**两层**：上层「头像 + 姓名 + 阶段标签 + 电话（带图标）」；下层 `border-top` 分隔的「负责顾问 / 接手设计师」双列归属区，替代原先平铺 4 行文本；
  - **约定量房前置校验**：无房屋档案时弹窗引导「请先建立房屋档案 → 新增房屋」，不再允许在无地址情况下直接进入量房约定；
  - 「新增房屋」「记录加微信」由 `uni.showModal` 内联弹窗改为**跳转独立页面**；
  - 「记录加微信 / 记录到店」由 `jz-btn--text` 小文字按钮改为**等宽卡片按钮**（`jz-btn--ghost` + `jz-btn--neutral`，带图标，flex:1）；
- 新增页面（已在 `pages.json` 分包注册、`config` 增加路径常量）：
  - `src/subpackages/customer/pages/property-edit.vue`「新增房屋」：顶部客户信息卡（姓名/电话）+ 详细地址（必填）/小区/楼栋/房号/面积/户型/备注，提交 loading+disabled 防重复；
  - `src/subpackages/customer/pages/wechat-record.vue`「记录客户微信」：顶部客户信息卡（含当前微信/到店状态）+ 微信号输入 + 说明，支持首次记录与更新。
- 跟进页 `src/subpackages/customer/pages/follow.vue`：
  - 顶部展示**客户基础信息**（头像、姓名、电话、小区、阶段标签）；
  - 新增**上次跟进卡片**（跟进方式、时间、内容、上次约定的下次跟进日期），并默认带出上次约定的跟进日期作为本次选择值；
  - 跟方式 chip 与表单间距统一为 8 的倍数（原 12/14/30/20rpx 等）。
- 验证：`npm run type-check` PASS；`npm run build:mp-weixin` PASS；`npm run check:subpackages` PASS（主包 396KB / 分包页 51→53 / 总包 1.15MB）；dev watch 已生成两个新页面产物。

## 2026-09-16 小程序 release 构建并上传微信平台

- 命令：`npm run build:mp-weixin:release`（type-check → check:release-env → build → check:subpackages），四道门禁全部 PASS。
- 产物：`../../前端代码/th4-decoboon-app/dist/build/mp-weixin`，主包 396,161 B、最大分包 155,523 B、总包 1,146,678 B，主包 12 页 + 分包 12 个 / 53 页。
- 生产配置：`VITE_APP_ENV=production`、`VITE_API_BASE=https://decoboon.joytsing.com/api/`、法务主体「安徽昭青网络科技有限公司」/`decoboon@joytsing.com`、`VITE_LEGAL_REVIEWED=true`、AppID `wxe5cc5fec9e50a9a9`。
- 上传：`cli upload --version 1.0.0 --desc "客户详情/跟进链路优化、成员管理交互重构、客户列表卡片分层、TabBar遮挡修复、新增房屋与记录微信独立页面"` → 返回 `✔ upload`（微信平台开发版本）。
- 本次版本包含：客户详情卡片分层与量房前置校验、新增「新增房屋」「记录客户微信」独立页面、跟进页客户信息+上次跟进、成员管理分段筛选与卡片操作区、客户列表三层卡片、TabBar 底部遮挡修复（全局 `jz-tabbar-gap`）、客户表单右对齐修复、经营驾驶舱目标按钮右对齐、`.jz-section__more` 图标换行全局修复。
- 待人工：微信公众平台 → 版本管理 → 设为体验版 / 提交审核 / 发布（需管理员账号操作，CLI 不具备该权限）。

## 2026-09-16 邀请页「已授权自动进工作台」

- 文件：`../../前端代码/th4-decoboon-app/src/pages/invite/accept.vue`。
- 背景：员工通过邀请链接授权手机号并加入企业后，前端会 `reLaunch` 到工作台；但**再次点开同一邀请链接**时，后端返回 `status=Accepted`，页面只显示「邀请已使用」且无任何操作入口，用户被困住。
- 改动（`onLoad` 内新增两段判定）：
  1. **业主侧**：`isCustomerInvite && session.token && session.role === 'customer'` → 直接 `reLaunch(P.home)`（已完成绑定不再重复授权）；
  2. **员工侧**：`loadInvite()` 成功后，若 `!isCustomerInvite && session.token && session.role && invite.status === 'accepted'` → 直接 `reLaunch(P.home)`；
  3. 邀请仍为 `pending` 时保持原流程（含"已在他企 → 确认切换企业"分支），不破坏切换企业能力。
- 依据核验：后端 `MembershipContext.requireInvite()` 不校验邀请状态（仅处理过期），状态校验只在 `accept()` 中，因此已接受邀请仍可查询到 `Accepted`，前端 `normalizeInvite` 映射为 `accepted`，判定可靠。
- 验证：`npm run type-check` PASS；`npm run build:mp-weixin` PASS；`npm run check:subpackages` PASS；dev 产物 `pages/invite/accept.js` 已含 `session.role` 判定与 `reLaunch`。
- 待办：本次改动尚未上传微信平台（上一版 1.0.0 已上传），如需生效需重新执行 release 构建 + 上传。

## 2026-09-16 工作台首页：顶部锚定 + 企业展示强化 + 微信头像 + 老板视角

- 组件增强：
  - `src/components/JzAvatar.vue`：新增 `src` 属性（微信头像），`<image mode="aspectFill">` 渲染，加载失败自动回退姓名首字，`watch(src)` 重置失败态；
  - `src/components/JzPageHeader.vue`：新增 `fixed` 属性，固定模式下 `.jz-header__bar` 为 `position: fixed` 并自带渐变底色与投影，滚动时不随内容滚走。
- 首页 `src/pages/index/index.vue` + `src/styles/home.scss`：
  - Header 启用 `fixed`（顶部锚定），副标题改为 `orgLine`（企业全称）强化企业展示；
  - 原「角色横幅」升级为**个人信息卡**：微信头像（112rpx）+ 姓名 + 角色标签 + 企业行（building 图标）+ 门店/团队行（users 图标），角色文案区分「全公司经营视图 / 门店经营视图 / 团队经营视图」；
  - 项目区块标题按角色动态：BOSS=`企业项目 · 全公司`、店长=`门店项目`、团队长=`团队项目`、其他=`我的项目`、业主=`我的家 · 施工进度`；
  - 新增 computed：`avatarUrl`（emp.user → session.user 回退）、`orgLine`、`orgSubLine`、`projectSectionTitle`。
- 微信头像接入（员工侧 6 处，客户头像保持字母，因客户无微信头像）：
  `pages/index/index.vue`、`pages/mine/index.vue`、`subpackages/team/pages/list.vue`、`subpackages/team/pages/designers.vue`（设计师/绩效 2 处）、`subpackages/project/pages/members.vue`、`subpackages/project/pages/detail.vue`（2 处）、`components/JzReportCard.vue`（动态作者）。
  - 注：`designers.vue` 的「待指派客户」列表属客户对象，已回退为字母头像（type-check 捕获后修正）。
- BOSS 视角核验：
  - 数据层：`domains/project/scope.ts` 的 `isProjectVisible` 对 `boss/storeManager/teamLeader/finance` 直接返回 true，故项目与动态本就为全公司范围，本次修正的是「我的项目」文案误导；
  - 权限层：`roles/catalog.ts` 中 BOSS 快捷入口为经营驾驶舱/成员管理/人员绩效/客户管理/合同签约/邀请成员/我的团队/财务总览/消息通知/全部项目，**不含任何设计类业务**（设计调度属店长/团队长，设计业绩属设计师），符合"BOSS 不做设计师业务"。
- 验证：`npm run type-check` PASS；`npm run build:mp-weixin` PASS；`npm run check:subpackages` PASS（主包 398KB / 53 页）；组件产物 `JzPageHeader.wxss` 含 `jz-header--fixed`，`JzAvatar.wxml` 含 `jz-avatar__img`。
- 待办：本次改动尚未上传微信平台，如需生效需重新 release 构建 + 上传。

## 2026-09-16 门店维度（经营驾驶舱 / 成员管理 / 人员绩效）

- 后端能力：
  - 新增 `user/store/list` Provider（`tenant/user/service/user/store/list`）：企业负责人返回企业全部门店，店长/团队长/普通员工仅返回本门店，支持 `currentOnly` 强制单店；
  - `BossDashboardQuery` 增加 `storeId`，`BossDashboardContext.dashboard()` 按门店过滤项目/合同/回款/应付（四类实体均有 `store_id` 关联）；
  - `PerformanceQuery` 增加 `storeId`，`PerformanceReportContext.report()` 按门店过滤人员与报价/客户/项目口径（用 final 变量避免 lambda 捕获问题）；
  - `company/member/page` 增加 `storeId` 过滤，在既有角色数据范围（scoped）内再限定门店，不突破权限边界。
- 前端接入：
  - 新增 `components/JzStoreFilter.vue`：门店选择器（`全部门店 + 各门店`），**门店数 ≤ 1 时自动不渲染**；
  - `identityApi.storeList()` 与 `memberPage({ storeId })`、`statisticApi.bossDashboard(month, storeId)`、`statisticApi.performance(month, role, storeId)`；
  - 接入页面：经营驾驶舱 `subpackages/statistic/pages/analysis.vue`、成员管理 `subpackages/company/pages/members.vue`、人员绩效 `subpackages/statistic/pages/assessment.vue`。
- 测试与回归：
  - `ProviderSecurityAcceptanceTest` 路由总数同步 197→198、protected 186→187（新增 store/list）；
  - **发现并修复本地密钥机制副作用**：`config/application-dev.yml`（含 OSS 配置）会被未指定 profile 的测试加载，导致 `OwnerFileExposureAcceptanceTest` 期望的本地签名链接变成 OSS 预签名 URL；已在该测试显式固定 `th4.edge.components.storage.provider=local`；
  - 结果：`mvn -o -q test` → 83 tests / 0 failures / 0 errors；前端 `type-check`、`build:mp-weixin` 均 PASS。
- 待办：客户列表（`customer/customer/page`）门店维度未接入——该 Provider 走框架 `AbstractPage<Customer>`，需单独评估分页扩展点后再加 `storeId` 条件。

## 2026-09-16 B 方案：客户分页显式 Provider + 门店维度

- 背景：客户分页原继承框架 `AbstractPage<Customer>`，无法安全接收门店筛选（直接暴露 `storeIdEq` 缺少服务端强校验）。按既有已验证模式（同成员分页）改为显式 Provider。
- 后端：重写 `decoboon/customer/service/cust/page/support/Page.java`
  - 通过 `DataScopeContext.requireScope()` 复用与 `DataScopeRequestFilter` 完全一致的数据范围：企业负责人=全公司、店长=本门店、团队长=本团队、普通员工=本人负责客户（分别调用 `CustomerRepository` 对应的四种查询）；
  - 新增 `@AutoInject Long storeId`，在企业负责人数据范围内按门店收窄；非企业负责人忽略前端传值、固定本门店（避免越权）；
  - 内存分页（与成员分页同构），页大小上限 200。
- 前端：`crmApi.page` 的 `CustomerPageQuery` 增加 `storeId` 并透传查询参数；`pages/customer/list.vue` 接入 `JzStoreFilter`（门店 ≤1 自动隐藏）+ 首屏/翻页均带 `storeId`。
- 已回退的方案：注解 + 过滤器的通用门店拦截（`@StoreScope` + `StoreScopeCriteria` + `StoreScopeRequestFilter`）在启用后触发 `ApplicationContext` 加载异常（`dataScopeContext` Lookup method resolution failed / `FileAuthorizationContext$Authorization` NoClassDefFoundError，clean 重编译后仍复现），为保持代码库可发布状态已回退，未留半成品。
- 验证：`mvn -o -q clean test` → 84 tests / 0 failures / 0 errors；前端 `type-check`、`build:mp-weixin` 均 PASS。

## 2026-09-16 装得清运营管理端（历史方案，已被独立平台组织重构取代）

- 输入：用户要求构建 `th4-decoboon-admin`，覆盖全平台企业运营、订阅人工管控、营销推广支持和系统运维监控。
- 前端：完成 Vite + Vue 3 + TypeScript + Element Plus 工程，含运营总览、客户企业、订阅续费、增长运营、套餐策略、系统运维、操作审计 7 个模块。
- 后端：新增 `com.th4.edge.platform`，实现企业分页/详情、运营总览、套餐列表、人工订阅调整、操作审计、系统状态 7 条 Provider。
- 安全：`Company.platform=true` + `EmployeeRole.Boss` 白名单；订阅写操作强制原因与审计；普通租户请求返回 403。
- 数据：新增 `Company.platform` 和 `db_platform_operation_log`，同步增量迁移与空库基线。
- 验证：前端 `type-check`/`build` PASS，浏览器 7 路由及订阅调整交互 PASS；后端 `mvn -o -q clean test` 为 29 suites / 84 tests / 0 failures / 0 errors；Provider Security 路由总数为 205。
- 交接：`_系统/协作/lanes/development/workspace/admin-console-handoff.md`。
- 风险：尚未在生产执行迁移、创建平台运营账号和做真实 HTTPS 验收。

## 2026-09-16 金额单位自适应 + 财务模块视觉统一

- 金额展示（全局）：
  - `core/format.ts` 的 `money()` 改为**万元/元自适应**：`|n| ≥ 10000` 显示 `12.34万`，否则显示 `¥9,800`；新增 `moneyParts()` 返回 `{ value, unit, text, unitKey }`；
  - 新增 `components/JzMoney.vue`：数值与单位分离渲染，**万元单位用品牌色、元单位用次要灰**（单位字号 0.72em），满足"单位颜色差距"要求；
  - 由于 `money()` 是全局函数，25 个引用文件一次性获得单位自适应能力。
- 财务工作台（`pages/finance/index.vue`）：风险待办的「催收 / 待付」由两个裸文字 + 内联 `#3568C7` 改为**分段控件**（`.fin-urgent-tabs`：灰底容器 + 选中态品牌浅底 + 品牌色文字，间距 8rpx），样式落在 `styles/finance.scss`；撤收/待付卡片金额改用 `<JzMoney>`。
- 品牌色统一：批量替换 `#3568C7 → #0E8A7D`（品牌深色）、`#EEF3FF / #EEF3FC → #E8F7F5`（品牌浅底），共 **10 个文件**，消除财务卡片与主题色不一致的蓝色。
- 表单对齐（`styles/finance.scss`）：根因是 `.fin-field input` 缺少对齐声明（picker 的 `.fin-field__value` 已右对齐），导致"标题靠左、录入靠左"的错位；现改为 **input 右对齐、textarea 保持左对齐**，与 `新建客户` 表单口径一致。
- 验证：`npm run type-check` PASS；`npm run build:mp-weixin` PASS；`npm run check:subpackages` PASS（主包 / 53 页）。

## 2026-09-16 财务配色与表单对齐（二次修复 + 开发者工具验证）

- 用户反馈上次修复未生效，复查后定位到两个真实根因：
  1. **财务卡片蓝色并非 `#3568C7`**：实际来源是蓝紫渐变 `#2F54B8` / `#243E8A`（`styles/finance.scss` 财务头部渐变与筛选选中态、`subpackages/finance/pages/project.vue` 的表头/结算卡渐变）；本次统一替换为品牌色 `#0E8A7D` + `#096C64`，阴影 rgba 一并调整；
  2. **表单右对齐漏在全局 `jz-field`**：上次只改了财务专用的 `.fin-field input`，而其它模块的新增/录入表单用的是全局 `.jz-field`（`styles/base.scss`），其 `input` 同样缺少对齐声明；本次给 `.jz-field input` 补 `text-align: right`（textarea 保持左对齐）。
- 验证（微信开发者工具实机访问，`cli auto` + miniprogram-automator）：
  - `pages/finance/index` 正常渲染：当前账面现金余额 / 项目合同额 / 逾期 / 待收工程款 / 本月回款 / 待付账款 等指标卡可读；
  - `subpackages/finance/pages/entry`（新增应收节点）正常渲染，字段为「所属项目 / 节点名称 / 施工节点 / 应收类型 / 应收金额（元）/ 约定收款日 / 需要开票 / 备注」，标题与录入分离；
  - 截图：`/private/tmp/mp-automator/out/finance-index.png`、`finance-entry.png`。
- 产物核对：`dist/dev/mp-weixin/subpackages/finance/pages/project.wxss` 中品牌色 11+3 处、`#2F54B8` 0 处；`app.wxss` 含 2 处 `text-align: right`；全仓 `grep` 已无 `#2F54B8 / #243E8A / #3568C7` 残留。
- 说明：本地数据库已清空，页面金额均为 0（<1 万 → 元单位），万元分支逻辑在 `core/format.ts` 中实现但未在本次实机覆盖。

## 2026-09-16 财务表单 picker/switch 右对齐（第三次修复，含开发者工具实测）

- 用户指出「所属项目 / 应收类型 / 约定收款日 / 需要开票」仍未靠右。根因与客户表单同源：
  1. `<picker>` 作为 `.fin-field` 的 flex 子项**没有 `flex: 1`**，宽度收缩到内容宽度，其内部 `.fin-field__value { flex:1; text-align:right }` 无处伸展 → 值贴左；
  2. 「需要开票」用的是 `<switch>`，**没有 `margin-left: auto`**，紧贴标签靠左。
- 修复：`styles/finance.scss` 增加 `.fin-field picker { flex:1; min-width:0 }`、`.fin-field switch { margin-left:auto; flex-shrink:0 }`；`styles/base.scss` 同步为 `.jz-field picker { flex:1; min-width:0 }` 并补 `.jz-field__value` 右对齐 + 省略号（全站表单统一）。
- 验证：
  - 编译产物 `dist/dev/mp-weixin/subpackages/finance/pages/entry.wxss` 已含 `.fin-field picker.data-v-*`、`.fin-field switch.data-v-*` 规则（21:10 刷新）；
  - 微信开发者工具实测（miniprogram-automator）：`subpackages/finance/pages/entry` 正常渲染，窗口宽 390px，「所属项目」label.left=26（标题靠左）、value 元素 left=119 且 `flex:1 + text-align:right`（录入靠右），值文本「企业通用账务」；
  - 截图：`/private/tmp/mp-automator/out/entry-align-2.png`。

## 2026-09-16 运营管理端真实后端化与组织设计

- 移除 `VITE_DEMO_MODE`、mock Provider、演示账号和断网回退。
- 新增 `npm run verify:real-api` 构建门禁，校验 7 个平台 Provider 和真实 API 配置。
- 新增默认关闭的 `PlatformBootstrap`：显式提供 `PLATFORM_BOOTSTRAP_*` 时创建独立平台组织、部门和 SuperAdmin，不使用租户 Employee/Company。
- 新增 `docs/product/装得清运营管理端用户组织与权限设计.md`，覆盖独立组织、岗位、权限矩阵、数据范围、职责分离和账号生命周期。
- 新增账号初始化和实时后端对接运维文档。
- 验证：前端 `verify:real-api` + `type-check` + `build` PASS；后端编译和平台/Provider 安全测试 PASS。

## 2026-09-16 本地版本提交 1.0.1（后端 + 前端双仓库）

- 后端仓库 `后端代码/th4-decoboon`（根目录正确）：
  - `00ba624 chore(release): 本地版本提交 1.0.1`（首提交，44 文件）
  - `f5a0c57 chore(release): 补全 1.0.1 本地版本（源码/文档/示例配置）`（补全 995 文件，含 src/main、src/test、docs、_系统、.agents 与 `.env.*.example` 模板）
  - 提交前补充 `.gitignore`：`logs/`、`data/`、`__pycache__/`、`*.pyc`；清理已暂存的 `scripts/__pycache__`
  - 安全核验：`.env.dev.local`、`.env.oss.local`、`.env.wechat-pay.local`、`config/application-dev.yml`、`config/wechat-pay/*.pem` 全部 IGNORED；提交的 `.env.*.example` 为模板且不含真实密钥
  - 最终工作区：干净（0 未提交）
- 前端仓库 `前端代码/th4-decoboon-app`：
  - 首次确认时该目录**没有 `.git`**，`git rev-parse --show-toplevel` 指向 `/Users/zhangwei` 主目录仓库（存在误提交主目录的高风险）；随后确认已具备独立 `.git`（根目录校验通过）后执行首次提交
  - `6b30de8 chore(release): 初始化前端工程并提交 1.0.1`（232 文件），工作区干净
  - `.gitignore` 已忽略 `node_modules`、`dist`、`unpackage`、`*.local`、`.env.local`；敏感内容扫描（appsecret / access-key / private-key）无命中
  - 版本号：`src/manifest.json` → `versionName 1.0.1` / `versionCode 101`
- 状态：**仅本地提交，未发版**（用户要求先完成企业负责人功能梳理）

## 2026-09-16 运营管理端独立平台组织重构（架构修正）

- 用户要求：运营业务领域必须与多租户装企业务体系彻底隔绝，数据不得交织和关联。
- 平台认证：改为独立 `platform/auth/login|info|logout`，token audience 为 `PlatformUser`；租户 Employee token 不再能承载平台身份。
- 平台组织：新增独立 `PlatformOrg`、`PlatformDepartment`、`PlatformUser`、`PlatformRole`、`PlatformPermission` 和平台独立 BCrypt 密码。
- 数据隔离：删除 `Company.platform`；平台表不建立指向 `db_tenant_company`、`db_tenant_employee`、`db_tenant_credentials` 的外键。
- 审计隔离：`PlatformOperationLog` 只引用平台组织和平台用户；租户企业ID、企业名称和订阅ID仅保存标量快照。
- 跨域边界：租户业务只允许由 `TenantDataAdapter` 转换为 `PlatformTenant/PlatformSubscription/PlatformPlan` 快照，平台实体不持有租户实体。
- 初始化：`PlatformBootstrap` 只创建平台独立组织、部门、SuperAdmin 和 BCrypt 密码。
- 验证：`mvn -o -q clean test` = 29 suites / 85 tests / 0 failures / 0 errors；`PlatformAdminAcceptanceTest` 验证平台表无租户外键；Provider Security = 208 routes（public 12 + protected 196）。

## 2026-09-16 订阅中心：订单记录操作按钮降级

- 反馈：订单与续费记录下的「查询结果 / 继续支付」按钮突兀。
- 根因：每笔待支付订单都渲染一个 `jz-btn--primary`（主色实心）按钮，导致页面出现多个主 CTA（违反 `AGENTS.md §5.4` 单页主色实心按钮 ≤ 1），且与订单金额挤在同一行、`gap:10rpx` 非 8 倍数。
- 修复（`subpackages/subscription/pages/index.vue`）：
  - 「继续支付」→ `jz-btn--ghost`（描边，次要）；
  - 「查询结果」→ `jz-btn--text`（纯文字，最轻）；
  - 操作区间距 `10rpx → 16rpx`，新增 `flex-shrink: 0` 防止被金额挤压；
  - 订单卡片的层级恢复为「套餐/订单号/状态 → 明细 → 金额+时间 → 操作」。
- 验证：`npm run type-check` PASS；`npm run build:mp-weixin` PASS；dev 产物 `index.wxml` 已含 `subscription__order-query` 与 `jz-btn--ghost`。
- 备注：支付相关后端修复见提交 `a004f15`（微信支付 amount.total 被 JacksonLongConfigure 序列化为字符串的根因）。

## 2026-09-16 订阅中心：hero 卡片与头部间距/配色层次

- 反馈：当前订阅卡片与头部没有间距，且颜色与头部显得突兀。
- 根因（`subpackages/subscription/pages/index.vue`）：
  1. `.subscription__content { padding: 0 $jzb-page-pad; }` 上下无内边距 → hero 卡片紧贴 `JzPageHeader` 的渐变头部；
  2. hero 背景 `linear-gradient(135deg, #0E766E, #16A394, #52BFAF)` 与头部 `linear-gradient(135deg, #169C90, #16A394, #0C776E)` 同为深绿渐变 → 两块深绿直接相接，视觉上像被切开。
- 修复：
  - `.subscription__content` 顶部内边距 `0 → 24rpx`（8 的倍数），卡片与头部之间露出页面底色；
  - hero 渐变改为**更亮的中绿** `#12A08F → #2BC4B4 → #6FD3C6`，与深绿头部形成「深→浅」层次；阴影由 `0 20rpx 50rpx rgba(14,118,110,.20)` 调整为 `0 16rpx 36rpx rgba(14,138,125,.16)`，避免与头部阴影叠加显得厚重。
- 保留：卡片圆角、`hero-glow` 装饰、白字与内部结构（用户反馈卡片设计本身没问题）。
- 验证：`npm run type-check` PASS；`npm run build:mp-weixin` PASS；dev 产物 `index.wxss` 已含新 padding 与新渐变、旧深绿 `#0E766E` 计数为 0。

## 2026-09-16 IDEA 平台运营启动与本地数据同步

- IDEA：新增并选择 `平台运营开发环境` Spring Boot 配置；首次账号创建使用 `平台运营初始化` 配置。
- 数据：清理本地旧 `db_tenant_company.platform` 和融合审计表，执行独立 `db_platform_*` 迁移；清理前备份到 `/private/tmp/decoboon-before-platform-sync.sql`。
- 账号：创建独立 `PlatformOrg / PlatformDepartment / PlatformUser`，手机号 `18660219918`，岗位 `SuperAdmin`，密码只保存在本地 IDEA 配置且已 Git 忽略。
- 验证：IDEA 中后端运行、`platform/auth/login` 成功、管理端真实登录成功、租户列表显示本地“麦鸿设计”、系统运维加载 MySQL/Redis/应用进程；平台到租户外键数 0。
- 文档：`docs/operations/2026-09-16-IDEA平台运营启动与本地数据同步.md`。

## 2026-09-16 报价项目库 P0：编号后端自动生成

- 规则：`{类型前缀}-{企业内 4 位序号}`（施工 SG / 主材 ZC / 软装 RZ / 电器 DQ / 综合 ZH / 定制 DZ），序号按「企业 + 类型」独立递增。
- 后端：
  - 新增 `catalog/context/CatalogCodeContext`：`nextCode(comId, type)` 取同企业同类型最大编号 +1；非本前缀的自定义编号回退序号 0 后 +1；
  - `CatalogItemRepository` 增加 `findFirstByCompany_ComIdAndTypeOrderByCodeDesc`；
  - `catalog/item/save` 的 Save Provider：**code 为空/空白时后端生成**，前端传入则保留（兼容企业自定义命名）；
  - 迁移脚本 `docs/design/migrations/24-报价项目编号自动生成.sql`：空串规范为 NULL + `(com_id, type, code)` 唯一约束，并附历史重复数据自查 SQL。
- 前端：app `catalog/manage.vue` 编号占位文案改为「编码（留空自动生成）」；web `resources/index.ts` 表单字段 label 同步为「编码（留空自动生成）」。
- 验证：后端 `mvn -o -q compile` PASS；`DecoboonClosedLoopTest` 1 test 0 failures；app / web `type-check` 均 PASS。
- 提交：后端 `e58c82b`；app 单独提交（web 因无独立仓库未提交，见仓库结构说明）。
- 下一步：报价库 P1（拼音缩写 + 拼音检索）。

## 2026-09-16 报价项目库 P1：拼音首字母与拼音检索

- 依赖：引入 `pinyin4j 2.5.1`（pom.xml）。
- 数据模型：`catalog_item.name_initials VARCHAR(32)`（@Keyword）+ 索引 `idx_catalog_item_initials (com_id, name_initials)`。
- 后端：
  - 新增 `catalog/context/PinyinContext`：中文取拼音首字母（「轻钢龙骨石膏板吊平顶」→ `QGLGSGBDDP`），字母数字保留大写；
  - `catalog/item/save` 与 `catalog/item/update` 均在写入 name 后自动刷新 `nameInitials`；
  - `catalog/item/page` 的 keyword 检索由 5 字段扩展为 7 字段 OR：新增 `nameInitials`（拼音检索）与 `series`（原先遗漏），大小写不敏感。
- 迁移：`docs/design/migrations/25-报价项目拼音检索.sql`（加列 + 索引 + 存量回填说明，拼音需应用层回填）。
- 验证：`mvn -o -q compile` PASS；`DecoboonClosedLoopTest` 1 test / 0 failures。
- 提交：后端 `feat(catalog): 报价项目拼音首字母与拼音检索（P1）`。
- 下一步：报价库 P2（分类明细动态渲染）。

## 2026-09-17 报价项目库 P3/P4 完成

- P3 成本价角色保护（后端 `23399c1` / app `90dfa5f`）：
  - 新增 `CatalogCostContext`：仅 Boss / StoreManager / Finance 可见成本价；
  - `catalog/item/page|save|update` 返回前统一裁剪，无权角色 `costPrice` 置空 →
    配合 `non_null` 序列化策略，**响应中不出现该字段**（服务端裁剪，非前端隐藏）；
  - app 端成本价从三连输入行拆出为独立敏感区块（锁图标 + 仅管理层可见 + 橙色警示底 + 说明文案），无权限角色不渲染。
- P4 价格模式展示（app，前端）：
  - 新增 `pricePreview`：按 `pricingMode` 即时展示对外报价口径
    （`材料 ¥120 + 人工 ¥45 = ¥165/㎡` / `材料 ¥120/㎡` / `人工 ¥45/㎡` / `固定总价 ¥1,200（不按工程量折算）`）；
  - 在材料/人工价输入下方展示「报价展示」预览行。
- 验证：后端 `mvn -o -q compile` PASS；app `type-check` + `build:mp-weixin` PASS。
  之后进入供应链 P0（供应商应付台账）。

## 2026-09-17 供应链 P0（供应商应付台账）后端 + 小程序完成

- **P0-1 数据模型**（`0a041c5`）：新增 `PayableSettlement`（付款×应付多对多核销明细，刻意区别于项目结算 `Settlement`）；`Payment` 增加 `partner`；迁移 `26-供应商应付核销.sql`。
- **P0-2 账期规则**（`afde8fa`）：`SettlementMode`（ReceiptDays / MonthlyCutoff）；`Partner` 增加 settlementMode/settlementDays/cutoffDay；`Payable` 增加 receiptDate + 账期快照；`PayableDueDateContext` 统一算到期日；迁移 `27-供应商账期规则.sql`。
- **P0-3 多应付核销**（`715a28c`）：`finance/payment/save` 支持 `payableIds[]`（与单张二选一）；按到期日先后分摊、写核销明细、回写应付状态；`PayableSettlement.amount` 统一为 BigDecimal（元）。
- **P0-4 账期接入 + 台账报表**（`f02b3aa`）：收货生成应付改由 `PayableDueDateContext` 计算（替代原 supplierDays 硬编码）；`finance/payable/page` 支持 `partnerId`；新增 `finance/partner-ledger/report`（供应商 × 应付/已付/待付/逾期/未结清笔数/最近到期日，按待付倒序，支持 overdueOnly）。
- **P0-5 小程序前端**（`cd40fe6`）：财务首页新增「供应商往来」入口；新增 `subpackages/finance/pages/partners`（待付/逾期汇总 + 全部/仅看逾期筛选 + 账期文案 + 卡片进入应付明细）；分包页 53 → 54。
- 与并行开发的交集：`ProviderSecurityAcceptanceTest` 已被平台会话更新为 208 条路由；本次基于最新值同步为 **209**（protected 197），未覆盖对方改动。

## 2026-09-17 排期、验收证据与角色待办中心

- 阶段排期：项目确认开工/完工日期后按阶段权重自动拆分 plannedStart/plannedEnd；总工期不得短于阶段数，最后阶段锁定项目计划完工日。
- 验收证据：任务 Done 前强制上传照片/视频；阶段验收强制影像，支持问题清单、整改责任人和整改截止日期。
- 量房附件：现场照片、结构图/原始图纸、其他附件均进入 Measure 实体和文件授权体系。
- 待办中心：新增 `/todo`，聚合客户跟进、量房、任务验收、阶段验收和逾期/临期回款。
- 数据迁移：`docs/design/migrations/25-阶段排期与验收证据.sql`。

## 2026-09-17 业务对象树形选择器

- 新增 `BusinessObjectPickerDrawer`：左侧树形分类、右侧搜索列表、业务摘要和单选确认。
- 客户选择：按客户阶段分组、按负责人二级分组。
- 项目工地选择：按项目状态分组、按项目经理二级分组。
- 报价项目库选择：按项目类型和分类二级分组。
- 资源页所有 entity 筛选和表单不再使用下拉，统一打开业务对象选择抽屉；依赖对象仍保留上下游联动。
- 量房、合同和验收工作台的客户/项目入口同步替换。
- 设计分项报价库改为选择器，双击结果可直接确认并回填项目价格。

## 2026-09-17 供应商采购价与可靠毛利闭环

- 供应商采购价：主材、全屋定制、软装、电器、施工等报价项目统一按供应商维护采购价、人工/班组价、起订量、交期、账期、有效期和启停状态；有效采购价自动形成报价库标准成本。
- 销售价与成本价分离：报价库新增/更新不再接受前端直接提交 `costPrice`；标准成本只由供应商采购价刷新。
- 成本完整性：新增 `ProjectCostContext` 统一估算成本、实际成本和缺失项数；结算、项目财务、财务汇总和经营驾驶舱在成本缺失时返回 `null` 利润并标记 `costComplete=false`，App 统一显示“成本未完整”。
- 成本回算：实际成本优先采购入库等实际发生值；缺少实际值时按当前有效采购价回算。实际材料成本只在到货入库累计，领料/损耗不再重复累计。
- 套餐边界：免费版、基础版、专业版、旗舰版统一开放全部业务模块，版本差异只保留门店数和席位数。
- 数据迁移：`docs/design/migrations/28-成本完整性结算快照.sql`，允许结算利润/毛利率为空。
- 验证：后端 `mvn -q test` 96 tests / 0 failures / 0 errors；App `verify:mp-weixin` PASS。真实 MySQL 麦鸿回归：`ZC-0001` 采购价 68→75→68 时，项目估算/实际成本 8160→9000→8160，成本缺失时利润保持 `null`。
- 错误集：新增 ECD-002 成本缺失伪利润、ECD-003 采购与领料重复累计、ECD-004 敏感成本裁剪上下文错误，并同步 `rules.md`、`AGENTS.md`。

## 2026-09-17 首页金额格式与工地详情报错修复

- Boss/财务首页金额卡改用统一 `JzMoney` 口径：金额 `>= 10000` 显示万元，`< 10000` 显示元，单位颜色区分；90242.83 显示 `9.02万`，9800 显示 `¥9,800元`。
- 阶段详情：定位为 `project/stage/info` 误序列化 `project.property` 的循环归属图，真实响应达 240KB+ 且嵌套数百层；裁剪 Project 白名单后降至 462B。
- 任务详情：同步裁剪 Project 的深层 `property/designer/manager`，真实响应降至 1.4KB。
- 前端容错：阶段详情的项目、任务、日报、验收改为独立容错；任务详情的项目/阶段/日报失败不再覆盖已加载的任务主数据。
- 回归：新增 `ProjectDetailSerializationContractTest`；App `verify:mp-weixin` PASS；后端全量测试通过。
- 提交：后端 `b42dd58`、App `1aa0e65`。

## 2026-09-17 首页项目卡片与最新动态优化

- 阶段操作业务说明：阶段操作是项目负责人推进“待开工 → 施工中 → 待验收 → 已完成”的状态入口；包含阶段开工、现场汇报、提交/查看验收和添加施工任务，所有动作进入阶段动态与验收链路。
- 页面表达优化：阶段操作区仅在当前角色存在可执行动作时展示，并补充“推进施工状态并沉淀现场记录”的说明，避免空操作块和语义不清。
- 首页项目卡片重构：从“封面+简单文字”改为项目身份、状态、当前阶段、进度、地址、计划完工、项目经理和进入工地动作的分层卡片；整卡保持可点击，并强化按压反馈。
- 最新动态修复：Boss 首页此前刷新时直接执行 `reports=[]`，而同频动态页单独调用 `siteApi.allReports()`，因此出现“首页为空、点击全部有数据”。首页现直接加载同一全量动态接口后再按首页项目范围筛选。
- 验证：Boss 动态接口真实返回 5 条、首条“阶段开工 · 油漆”；App `verify:mp-weixin` PASS。
- 提交：App `e823963`。

## 2026-09-17 动态卡片与成本未完整图标统一

- 动态卡片：统一优化头像、作者/角色、时间、类型标签、正文、图片宫格、类型强调条和按压反馈；单图全宽、多图双列，动态类型色贯穿卡片。
- 覆盖范围：经营驾驶舱、老板/财务工作台统计卡、财务总览累计利润、项目利润/毛利率列、项目结算资源表。
；App `verify:mp-weixin` PASS。

## 2026-09-17 人员绩效、我的分组与关于装得清

- 人员绩效：定位 `statistic/performance/report` 员工摘要误带出 store/team 循环归属图，原始响应 230KB+ 且无法解析；裁剪员工为 `empId/name/role` 后真实响应降至 1.6KB，3 条绩效数据正常。
- 我的页面：功能导航按“账号与企业、组织与权限、经营工具、我的业务、服务与支持”分组，增加分组标题和说明，降低长列表识别成本。
- 关于装得清：新增独立营销页面，基于产品白皮书表达“报价即预算、项目可追溯、经营可复制”，展示行业问题、核心模块、业务链路和适用客群。
- 客服咨询：关于页提供微信在线客服 `open-type=contact`、复制官方客服邮箱、复制包含当前用户/角色/企业/版本的问题反馈模板；未编造微信号或电话。
- 验证：后端 98 tests / 0 failures；App `verify:mp-weixin` PASS。
- 提交：后端 `d7e43df`、App `b180274`。

## 2026-09-17 三端品牌 Logo 统一

- 以 `logo-export/logo-mark-white@2x.png` 和同套 `logo-mark-color@2x.png` 为唯一母版，生成 256px 运行时资源并校验 SHA-256。
- App：新增 `JzBrandLogo`，登录、邀请和关于页改用统一 Logo；资源位于 `src/static/brand/`。
- Admin：新增 `BrandLogo`，登录和工作台侧边栏改用统一 Logo，同时设置 favicon；admin 当前无独立 Git 仓库，文件已在工作区更新。
- 规范：`rules.md` §12 和 `AGENTS.md` 已写入资源路径、明暗背景变体、SHA-256 和同步验收要求。

## 2026-09-17 店长数据范围、详情响应与品牌色修复

- 店长首页：后端最新动态分页原先缺少 `StoreManager` 分支，导致门店项目有数据但动态为 0；补齐店长、团队长、财务和履约角色项目范围后，店长真实动态返回 5 条。
- 首页项目：店长项目接口有 1 条，前端增加门店业绩 `recentProjects` 回退，避免列表异常时“门店项目”空白。
- 报价详情：定位 `createdBy/Property.salesman/designer` 未配置 Employee 摘要白名单，响应 286KB 且不可解析；限制为 `empId/name/role` 后报价详情 41KB 且可正常解析。
- 全量详情审计：递归扫描所有 `info/support` Provider 的关联字段，补齐验收详情的 Property 白名单；新增详情白名单契约测试与店长动态范围回归测试。
- 门店业绩：补齐新增/跟进客户、报价状态、项目状态、待办、已完工、未读消息、最近客户、最近报价及设计师全部经营指标。
- 视觉：首页统计、动态、报价池和门店业绩统一品牌色；全局状态筛选按钮改为品牌浅底/渐变选中态，修复方案报价状态筛选样式。
- 我的页面：退出登录改为明确的品牌描边按钮，避免底部弱文本不可见。
- 验证：后端 101 tests / 0 failures；App `verify:mp-weixin` PASS。
- 提交：后端 `d8f1947`、App `4987eb9`。

## 2026-09-17 报价详情与门店业绩 UI 重构

- 报价详情：头部、合同衔接卡、项目衔接卡、合同空态、谈单入口、项目衔接指标和报价推进提示统一为品牌渐变/品牌浅底。
- 门店经营结构：由纵向提醒列表重构为 2 列品牌指标卡，覆盖成员、客户、报价、项目、风险和经营沉淀，并补充子指标。
- 门店业绩：保持并强化最近客户、最近报价、最近项目及设计师完整经营指标；最近报价状态统一映射为中文。
- 验证：App `verify:mp-weixin` PASS。
- 提交：App `7795a28`。

## 2026-09-17 门店业绩首屏层级优化

- 店长业绩页头部改为更深品牌渐变，增加独立阴影和与概览区之间的 34rpx 留白。
- 新增独立白色“门店经营概览”容器，内部指标卡改为品牌浅底、白底图标和轻边框，解决头部与卡片颜色相近、直接贴合的问题。
- 增加“实时”状态标签和概览说明，强化视觉层级与交互识别。
- 验证：App `verify:mp-weixin` PASS。
- 提交：App `eb9cae9`。

### 微信开发者工具复验修正

- 通过微信开发者工具实际进入店长“门店业绩”页后发现：新增的白色概览容器与头部、经营结构形成连续多层卡片，视觉层级过重。
- 最终移除额外白色概览容器，改为“深色头部 → 34rpx 间距 → 概览标题 → 白底指标卡 → 门店经营结构”的平面结构。
- 指标卡保留品牌浅底图标、白色卡片、品牌细边框和按压反馈；微信开发者工具已重新编译复验。
- 提交：App `453df66`。

## 2026-09-17 客户诉求品牌卡片优化

- 设计师客户诉求列表与单个客户诉求详情统一使用品牌渐变头部、品牌浅底卡片、品牌色左侧强调条和品牌标签。
- 诉求数量改为品牌色徽标，搜索框、操作分隔和展开详情统一品牌浅底。
- 高、中、低优先级继续保留层级差异，但全部收敛到品牌色梯度。
- 验证：App `verify:mp-weixin` PASS。
- 提交：App `3d8ad06`。

## 2026-09-17 项目经理履约驾驶舱品牌主题色优化

- `src/subpackages/team/pages/workbench.vue` 的 Hero 从项目经理橙色渐变统一为装得清品牌渐变，阴影同步切换为品牌青绿色。
- 指标卡、快捷入口图标底、Tab 激活态、项目/任务/验收/材料/客户/报价卡片统一品牌色梯度；普通状态使用品牌青绿，逾期、驳回、延迟继续保留红色语义。
- 履约业绩卡片移除暖黄色，改为白色到品牌浅底渐变；金额、查看详情和客户头像统一品牌色，并补充卡片按压反馈。
- 验证：`npm run verify:mp-weixin` PASS；微信开发者工具使用项目经理账号进入履约驾驶舱，实际核对 Hero、指标卡、快捷入口、Tab 和项目卡片背景及尺寸正常。
- 提交：App `67ca1cf`。

## 2026-09-17 生产后端监控（exit 20，仅归档）

- 运行时间：2026-09-17 18:41:11 +08:00；命令：`bash scripts/prod-monitor.sh collect "35 minutes ago"`。
- 结果：退出码 20；服务 enabled/active/running，PID 39976，HTTPS health 200，根盘 16%，未发现基础设施异常。
- 重点：离职/停用员工会话持续触发 150 条业务拒绝；另见成员自操作、邀请冷却、授权过期和 `.env`/GraphQL/config/phpunit 等扫描探测，均无高置信源码根因，未进入修复门禁。
- 历史：`Invite.telNo` 本次无新增，保持 open 等待人工修复；懒加载旧事故无新日志，但 registry 中历史 fixed 记录被脚本改写为今日 open，需人工恢复基线并修监控脚本。
- 门禁：collect 返回 20，不修改源码、不创建修复快照、不测试发布、不重启、不发版。
- 证据与建议：`docs/operations/incidents/2026-09-17/20260917-184110/triage.md`。

## 2026-09-18 报价项目库成本维护入口回归修复

- `REQ-20260918-CATALOG-COST-ENTRY`：定位供应商采购价改造后报价库成本概念缺失。
- App `catalog/manage.vue`：恢复只读标准采购成本区块，BOSS/店长/财务可见；新增“维护供应商采购价”入口，携带目录项 ID 和类型跳转并预选项目。
- App `CatalogPicker.vue`：管理列表为有权限角色展示“成本 ¥x / 成本未维护”。
- 根因是成本来源迁移只删旧输入未补新链路，不是后端成本模型丢失。新增 ECD-006 硬性禁则。
- 验证：App `verify:mp-weixin` PASS；微信开发者工具 BOSS 账号实测报价库列表显示“成本未维护”，采购价维护页预选 `单口变压器35W` 成功。

## 2026-09-18 供应商协作 P0（直送工地优先）

- 先从中小装企真实场景出发完成《供应商协作业务设计与操作手册》，明确询价、项目下单、到货、月结、退换货和定制/劳务六类场景。
- 后端 `PurchaseOrder` 新增 `DeliveryMode`，默认 `Direct` 直送工地；`GoodsReceipt.Save` 仅对 `Warehouse` 采购写入库存，直送直接形成项目实际成本和应付。
- BOSS/财务创建采购单自动确认，项目经理/材料员提交后走审批；采购分页支持按当前数据范围跨项目查看，并补齐材料、供应商摘要。
- App 将“供应链与库存”改为“供应商协作”，项目采购设为默认入口，新增采购单和到货确认页面。
- 迁移：`docs/design/migrations/29-采购交付方式.sql`；本地测试库已执行。
- 规范：`AGENTS.md`、`rules.md` 新增“实际业务场景先行”；错误集新增 ECD-007，禁止按完整 ERP 平铺中小装企主流程。
- 验证：后端 `mvn -q test` PASS；App `npm run verify:mp-weixin` PASS

## 2026-09-18 供应商协作三角色实际验收

- 在微信开发者工具中按真实密码登录令牌完成 BOSS、项目经理、材料员三角色切换和协作闭环。
- 流程：BOSS 维护采购价 → PM 从材料计划创建采购 → BOSS 确认采购 → PM 确认下单 → 材料员确认到货 → BOSS 查看自动应付。
- 实测数据：采购单 `PO1789668147033`，10 个，单价 160 元，直送工地；到货后应付 1,600 元，到期日 2026-10-18；库存和库存流水均为 0。
- 发现并修复 DEF-SUP-001 P0：材料计划分页携带 `Property -> Store/Team/Leader` 循环归属图，导致材料计划显示 0；响应由异常膨胀降至约 650B，并新增白名单契约测试。
- 发现并修复 DEF-SUP-002 P2：`Count` 等技术单位改为中文展示。
- 新增仅开发环境生效的 `devToken` 员工会话切换入口，使用真实服务端登录令牌解决微信开发者工具多角色联调问题。
- 明确“采购执行价”可向采购执行角色展示，但“标准采购成本/供应商报价历史/项目毛利”仍仅管理层和财务可见。
- 验收报告：`docs/acceptance/小程序供应商协作三角色验收报告.md`。
- 提交：后端 `539e79e`、App `e5ea5e1`。

## 2026-09-18 P0-A 已确认修复包

- 输入：`输出/草稿/P0应用开发第一批-20260918/06-开发冻结包-已确认修复.md`，业务口径引用 `BR-001`、`BR-002`、`DEC-001`、`GAP-003`。
- 合同/项目创建：新增 `ProjectAccessContext.requireQuote`，报价主键与当前员工 Company/Store/Team/Self 范围在同一查询中匹配；合同创建继续限制 Boss/Designer，项目创建继续要求 Effective 合同；删除无范围的长整型报价创建旁路。
- 房屋门禁：`PropertyContext` 不再由客户地址自动建房；量房和指定设计师要求显式 `propertyId`；App 无房屋时只引导建立房屋档案。
- 客户状态：New 只能先写首次跟进；量房仅允许 Following/PendingMeasure；报价接受保持 Quoted，合同签约进入 Signed，合同生效进入 Serving。
- 客户来源：App 契约改为 `walkin -> WalkIn`、`old -> OldCustomer`，并新增 `scripts/verify-customer-source-contract.mjs` 接入 `verify:mp-weixin`。
- 新增/更新测试：四角色合同签约能力与报价范围守卫、房屋不自动创建、New/已量房量房门禁、合同三阶段客户状态、空报价分项成本不完整；更新闭环集成测试和 ProjectContractContext 构造。
- 验证：`mvn -o -q test` PASS：42 suites / 120 tests / 0 failures / 0 errors；App `verify:mp-weixin` PASS，主包 480520 B、最大分包 164914 B、总包 1313312 B。
- 交接：`_系统/协作/lanes/development/workspace/p0a-confirmed-fixes-handoff.md`。

## 2026-09-18 首次入驻引导完成状态缺陷与教训沉淀

- 用户反馈：BOSS 完成成本价格、供应商维护后，首页“开始使用装得清”仍未完成。
- 根因：供应商步骤硬编码 `done:false`；引导只在首次挂载取数，子页面返回首页不刷新；必做/可选完成谓词未冻结。
- 修复：供应商状态接入 `partner/page`，按有效材料供应商且至少一个品牌关系计数；首页 `onShow` 调用 `reload()` 重新收敛状态。
- 验证：App `npm run type-check` PASS。
- 沉淀：新增 `FIX-004`、`ECD-008`、`LESSON-001` 和产品研发反馈包。
- 交接：`_系统/协作/lanes/development/workspace/onboarding-completion-defect-feedback.md`。
- 说明：未宣称独立验收通过；供应商完成谓词和必做步骤口径仍需 product-planning 冻结。

## 2026-09-18 首次入驻状态契约与验收冻结（REQ-20260918-ONBOARDING-DEFECT-001）

- 输入：实际 BOSS 使用反馈、`FIX-004`、`ECD-008`、成本模型 V2 用户决策。
- 产品冻结：首次入驻必做分母固定为 3 项，即有效报价项目、至少一项正数标准成本、至少一个客户并关联房屋；首个报价/签约不计入首次入驻分母。
- 供应商步骤：保持可选，完成谓词冻结为 `status=active && type=supplier && brands.length > 0`；只建供应商未关联品牌不算完成，也不阻塞首次入驻。
- 状态契约：每一步明确 `key / label / required / source_provider / completion_predicate / count / refresh_triggers / next_action / 异常态`，禁止静态 `done`、演示数据和接口成功冒充完成。
- 验收矩阵：新增 15 个场景，覆盖初始态、动作成功返回、可选完成、必做未完成、接口异常、删除回退、重复进入、租户隔离和角色可见性。
- 防复发：`AGENTS.md` 新增“可完成状态投影（强制）”，`rules.md` 新增 §13；适用范围扩展到实施清单、待办、资料完整度、节点进度、审批进度和质量检查清单。
- 规格输出：`输出/草稿/装得清首次入驻三件套-20260918/07-系统内引导与验收清单.md`、`输出/草稿/装得清首次入驻三件套-20260918/01-新手企业操作引导手册.md`。
- 当前差距：App 客户步骤仍需证明“客户 + 房屋”谓词；成本和供应商计数需证明分页全量或补 Provider 统计能力
- 验收状态：`pending_independent_acceptance`，未宣布通过。

## 2026-09-18 SMB-P2-01 blocked 动作语义收口

- `NextActionCard` 与客户详情主按钮统一：blocked 有恢复路径时仅显示“查看处理条件”，无路径时禁用并显示“待业务确认”，非 blocked 保持执行文案。
- 客户详情 `runNextAction()` 增加 blocked 前置分支，不再通过主按钮直接打开实际业务抽屉；多房屋选择继续只滚动到房屋列表。
- `verify-system.mjs` 增加 blocked 可执行回退守卫。

## 2026-09-18 报价项目效果图与目录多级筛选

- 后端 `CatalogItem.images` 已有模型与保存链路，本次补充分页响应中的图片字段、`series` 查询条件和 `category -> brand -> series -> spec` tree facet；无 DDL/迁移。
- App 报价项目编辑支持最多 9 张效果图，拍照/相册多选、上传、缩略图、预览、删除；上传走 `file/private/grant` 的 `Catalog` OSS 业务目录。
- 验证：Backend `mvn -o -q test` PASS；App `type-check / build:mp-weixin / git diff --check` PASS。
- 说明：App `verify:mp-weixin` 仍受既有主包引用子包 `OnboardingChecklist` 问题影响；本次品牌/图片构建已通过。

## 2026-09-18 报价项目目录筛选改为逐行选项

- 选择上级后自动刷新下级候选并触发查询；保留模糊输入和 Enter 查询。

## 2026-09-18 麦鸿报价项目库清理与 70% 成本导入

- 用户需求：清空麦鸿现有报价项，将《麦鸿装饰报价9.5.xlsx》中的报价项导入，并将默认成本价设为售价的 70%。
- 目标：本机开发库 `decoboon` 的麦鸿装饰企业（`com_id=1789680913513045799`，BOSS 张伟）。
- 导入前核对：现有报价项 2 条，无设计、报价、采购、库存、任务等业务引用；已生成完整数据库备份 `/private/tmp/decoboon-before-maihong-catalog-70-20260918.sql`。
- 数据口径：售价 = 材料价 + 人工价；成本价 = 逐条 `round(售价 × 70%, 2)`。
- 先执行事务级干跑并回滚，验证 SQL、外键、编号和明细插入均成功，随后正式提交。
- 结果：删除 2 条旧报价项，导入 557 条；施工 176、主材 259、综合 14、定制 108；售价合计 188,636.44 元，成本合计 132,045.50 元。
- 校验：557 条逐条成本比例不一致为 0；编号重复 0；四类明细记录数量与主表一致。
- 结果记录：`_系统/协作/lanes/development/workspace/maihong-catalog-70-20260918/README.md`。

## 2026-09-18 报价项目筛选工具栏行式布局

- 目录类型、分类、品牌、系列、规格全部改为逐行选项，不使用下拉框。
- 模糊查询与查询/重置按钮放在同一行，输入靠左、按钮靠右；点击选项立即联动查询，Enter 查询保留。

## 2026-09-18 业务员营销交接边界

- 业务员保留获客、跟进、客户/房屋、约定量房和本人项目只读查看；约定量房后营销主责转移设计师。
- App 移除业务员报价写入、设计编辑和谈单入口；主页不再生成“报价待推进”待办。
- 后端报价分页对业务员按客户负责人或房屋业务员只读返回，报价详情和明细允许该范围读取；`quote/save`、`quote/update` 对业务员明确 403。
- 验证：Backend `mvn -o -q test` PASS；App `type-check / build:mp-weixin` PASS。

## 2026-09-18 业务员业绩页信息层级重构

- 业务员业绩页按“结果 → 今日行动 → 客户健康度 → 客户概览 → 最近客户”重排。
- 删除汇总卡里的重复“待跟进”，仅保留今日待办中的今日/逾期拆分。
- 黄金营销时段改为横向条形，并明确按客户建档时长解释 0-3、4-7、8-15、15+ 天。
- 最近客户增加详情跳转，新增“去跟进客户”主 CTA。
- 卡片色值和图标统一使用 `TOKEN_COLOR` 与 SCSS Token，逾期、风险使用错误色。
- 验证：App `type-check / build:mp-weixin / git diff --check` PASS。

## 2026-09-18 业务员业绩经营指标与房屋地址同步
- `MarketerReport` 新增客户录入、量房转化、跟进数的日/周/月当前值、上期值和增长/变化率。
- 新增业务员企业排名、门店排名、我的企业排名、我的门店排名和公司业务员/门店总数。
- App 业务员业绩页新增客户录入趋势、量房率趋势、跟进活跃度、我的排名、企业业绩榜和门店排名。
- `property/property/save` 新增房屋时将房屋地址同步回客户地址。
- 验证：Backend `mvn -o -q test` PASS；App `type-check / build:mp-weixin` PASS。

## 2026-09-18 客户 360 操作职责收口
- 客户列表卡片移除所有业务操作按钮，卡片只展示客户经营事实并整卡进入客户详情。
- 客户详情新增编辑客户资料、登记到店、添加微信入口，和原有跟进、量房、房屋录入共同形成操作闭环。
- 手机号首联后锁定规则保持不变。

## 2026-09-18 客户标签/画像/分级需求冻结（用户决策）
- 用户确认 5 项决策：线索不做独立对象；标签字典仅老板/店长维护、业务员只能选用；画像最小集 8-12 字段；客户分级 A/B/C 由规则自动计算；标签/画像可驱动提醒并支持筛选。
- 输出冻结规格：`docs/plan/2026-09-18-客户标签画像与分级实施规格.md`（画像 10 字段、A/B/C 评分模型、标签分类与权限、R1-R5 提醒规则、验收标准、待确认阈值 Q1-Q5）。
- 未改动任何生产代码；未写入业务智能体知识库目录（该目录不在本会话可写范围），正式 DEC/BR 卡需由业务智能体登记。
- 状态：需求已冻结，实现待排期。

## 2026-09-18 客户标签/画像/分级阈值冻结 + P0 前端方案
- 用户确认 Q1–Q5：预算区间 高≥20万/达标10-20万/低<10万（企业可配置）；分级 A≥75、B 50–74；权重 30/25/20/15/10；R1「3天」按自然日；画像字段设计师可编辑（限 `customer.designer` 为当前员工的客户）。
- 规格更新：`docs/plan/2026-09-18-客户标签画像与分级实施规格.md` 待确认项全部转为「已确认阈值」，5.2 权限表补充画像编辑与分级只读。
- 新增实施方案：`docs/plan/2026-09-18-客户标签画像与分级-P0前端实施方案.md`，拆为 P0-a（纯前端：系统标签、R2/R3/R4 提醒、筛选、排版）与 P0-b（需后端：标签字典、画像 10 字段、A/B/C 分级）。
- 未改动任何生产代码与接口；未写入业务智能体知识库目录（不在可写范围）。
- 状态：需求与阈值已冻结，P0-a 可开工，P0-b 待后端排期。

## 2026-09-18 P0-b 后端字段与 Provider 契约
- 输出 `docs/plan/2026-09-18-客户标签画像与分级-P0b后端契约.md`：新增 CustomerTag / CustomerTagLink / CustomerPortrait 三个实体、14 组枚举、Customer 只读投影字段 tags/portrait/grade。
- 明确 Provider 路由：customer/customer-tag（page/list/save/update/delete）、customer/customer-tag-link（save/delete）、customer/customer-portrait/save，并在 customer/customer/page|info 装配 tags/grade。
- 明确分级计算契约：配置来自 Company.setting.gradeConfig，输出必须含 breakdown（weight/score/reason），lost 恒判 C。
- 明确权限矩阵、联合索引（com_id 最左）、迁移号 31、8 条验收标准与 4 项待确认（B1 团队负责人权限、B2 字段级审计、B3 是否缓存、B4 系统标签是否入库）。
- 未编写实现代码；未改动任何现有 Provider 与迁移。
- 状态：契约就绪，待后端 lane 实施。

## 2026-09-18 P0-b1 客户标签/画像数据基座
- 用户确认 B1–B4：团队负责人按现有四级数据范围；画像不做字段级审计（仅 updatedBy/updateTime）；分级不落库；系统标签不入库。B1–B4 已写入契约第九节。
- 新增 14 个枚举（`customer/configure`）：TagCategory / TagScope / TagStatus / PortraitDecisionRole / PortraitPurpose / PortraitHouseCondition / PortraitStartWindow / PortraitStyle / PortraitEco / PortraitHousehold / PortraitPayment / PortraitConcern / PortraitCompetitor / CustomerGradeLevel。
- 新增实体：`CustomerTag`（cust_tag）、`CustomerTagLink`（cust_tag_link）、`CustomerPortrait`（cust_portrait + cust_portrait_concern element collection）；新增只读投影模型 `CustomerGrade`（含可解释 breakdown）。
- 新增仓储：`CustomerTagRepository`、`CustomerTagLinkRepository`、`CustomerPortraitRepository`。
- 新增迁移：`docs/design/migrations/31-客户标签画像.sql`（4 张表，com_id 最左联合索引 + 唯一键 + 外键，表名 db_tenant_company/db_tenant_employee/cust_customer 已核对基线）。
- 数据范围策略：标签字典与企业级对象不冗余 store/team/owner，标签关联与画像继承 Customer 归属、由 Provider 显式校验，避免客户转交后范围失真。
- 验证：`mvn -o -q -DskipTests compile` PASS；`mvn -o -q test` PASS。
- 剩余：P0-b2~b7（字典 Provider、标签关联 Provider、画像 Provider、分级计算、page/info 装配、权限回归测试）。

## 2026-09-18 P0-b2~b4 标签字典 / 标签关联 / 画像 Provider
- P0-b2 标签字典：`customer/customer-tag/page|list|save|update|delete` 五个 Provider；字典维护限 Boss/StoreManager（403 拒绝其他角色），系统标签 scope=System 拒绝改名/停用，delete 为软删。
- P0-b3 标签关联：`customer/customer-tag-link/save|delete`；仅允许绑定人工且启用的标签，唯一键保证幂等；设计师可通过 `customer.designer = 当前员工` 额外访问指派客户。
- P0-b4 画像：`customer/customer-portrait/save` 按客户 upsert，10 个字段全部可空，写入 updatedBy。
- 统一权限：允许维护营销数据的角色为 Boss/StoreManager/TeamLeader/Marketer/Designer（Pm/ProjectSupport/Finance 403）。
- 验证：`mvn -o -q -DskipTests compile` PASS；`mvn -o -q test` 出现 1 项**预期失败**——`ProviderSecurityAcceptanceTest` 路由总数断言 209 需同步为 217（新增 8 条路由）；该文件属验收 lane `src/test/**` 写范围，已登记跨 lane 请求 REQ-20260918-ACCEPT-ROUTE-COUNT-SYNC，未越权修改。
- 剩余：P0-b5 分级计算、P0-b6 page/info 装配 tags/portrait/grade。

## 2026-09-18 P0-b5 分级计算 + P0-b6 装配 + 前端切服务端口径
- P0-b5：新增 `CustomerGradeContext`，按冻结规则计算（需求明确30/预算25/时间20/互动15/时效10 − 负向扣分；A≥75、B50–74、C<50；Lost 恒 C），输出可解释 `breakdown(weight/score/reason)`。
- 企业可配置：`Setting` 新增 `grade_config`（key=value;…，可空），迁移 `32-企业分级配置.sql`；未配置用冻结默认值，非法配置项忽略不阻断。
- P0-b6：新增 `CustomerTagContext`（系统标签按事实派生不入库 + 人工标签读取关联，支持批量装配避免 N+1）；`Customer` 新增只读投影字段 `tags/portrait/grade`；`customer/customer/info` 返回 tags+portrait+完整 grade，`customer/customer/page` 批量装配 tags+grade(level/score)；`CustomerFollowRepository` 新增批量计数查询。
- 未完成：P0-b7 权限与越权回归测试；`ProviderSecurityAcceptanceTest` 路由总数断言 209→217、protected 197→205 属验收 lane 范围，跨 lane 请求 REQ-20260918-ACCEPT-ROUTE-COUNT-SYNC 待处理。

## 2026-09-18 P0-b 前端可用化（画像编辑 + 标签维护）
- 客户 360：画像卡片改为常驻，未填写显示空态 + 「完善画像」入口；新增画像编辑抽屉（9 个下拉 + 主要顾虑多选）。
- 客户 360：标签区新增「标签」管理入口，支持人工标签绑定/解绑；老板/店长可在抽屉内新建标签字典项（名称 + 分类）。
- 权限：画像编辑与标签绑定按 canEditPortrait 控制（pm/finance/projectSupport 不显示）；字典新建仅 boss/storemanager 可见（服务端仍强校验）。
- 已知限制：画像字段本批只支持填写/修改，清空已有选项需后端支持显式置空（当前 @AutoInject null 表示不修改）。
- 未完成：P0-b7 权限与越权回归测试；标签字典改名/停用 UI；路由总数断言待验收 lane 同步。

## 2026-09-18 执行迁移 31/32 + 路由断言同步
- 本地库 `127.0.0.1:3306/decoboon`（MySQL 8.0.24）执行 `docs/design/migrations/31-客户标签画像.sql`：创建 cust_tag / cust_tag_link / cust_portrait / cust_portrait_concern。
- 执行 `docs/design/migrations/32-企业分级配置.sql`：db_tenant_company 新增 grade_config varchar(512) NULL。
- 结构核对：四张表列名与实体映射逐一一致（含 create_*/update_* 审计列）。
- 冒烟验证（事务内插入标签+关联+画像+顾虑后 ROLLBACK）：读写正常，回滚后 cust_tag/cust_portrait 均为 0，无残留数据。
- 路由断言同步（经用户本线程授权，验收 lane 会话不可达）：`ProviderSecurityAcceptanceTest` 209→217、protected 197→205；跨 lane 请求 REQ-20260918-ACCEPT-ROUTE-COUNT-SYNC 标记待验收复核。
- 验证：`mvn -o -q test` 全绿。
- 说明：dev/test 为 ddl-auto=update，prod 为 validate；生产上线前需按 adopt 流程执行 31/32 并校验结构。

## 2026-09-18 缺陷修复：客户画像不显示 + 评级显示 [OBJECT OBJECT]
- 现象：客户 360「客户画像」卡片看似为空；客户列表与客户 360 的评级徽标显示 `[OBJECT OBJECT]`。
- 根因：本项目**枚举序列化为对象**（如 `{value:'A'}`）。P0-b 新增的 `normalizeCustomer` 未复用既有 `enumValue()` 解包器，直接 `String(raw.grade.level)` 把枚举对象转成 `"[OBJECT OBJECT]"`；画像字段同样因未解包导致 `portraitLabels` 映射全部落空，被 `filter` 掉后只剩空态，表现为"画像没数据"。**数据其实已取到，是解析层缺陷，不是后端返回缺失。**
- 修复：
  1. `tags.category/scope/status`、`portrait.*`、`portrait.concerns[]`、`grade.level` 全部改用 `enumValue()` 解包；新增 `enumOrUndefined()` 保持未填写为 undefined。
  2. `enumValue()` 增加 `name` 回退（`value ?? key ?? name`），与 `statistic/api.ts` 的 `valueOf` 口径统一，避免跨域枚举形状差异再次踩坑。
  3. 画像空态由页面级 `EmptyState`（52px 内边距 + 56px 图标，窄栏内形似损坏）改为一行紧凑提示。
- 复发防护（纳入后续 checklist）：新增任何枚举字段的 normalization 必须走 `enumValue()`，禁止 `String(枚举对象)`；前端取值后需用真实接口报文校验，不能只依赖 type-check。

## 2026-09-18 App 端同步客户标签/画像/分级
- 模型：App `crm/model.ts` 新增 `CustomerTag / CustomerPortrait / CustomerGrade(+Item)`，`Customer` 增加只读投影字段 `tags/portrait/grade`。
- API：新增 `tagList / tagPage / tagSave / tagUpdate / tagDelete / tagBind / tagUnbind / savePortrait`，与后端 8 条 P0-b 路由对应。
- 客户详情：头部新增评级徽标（A/B/C 着色）、标签行（来源/行为/意向/风险分类着色）；新增「评级依据」卡片（展示 breakdown 各维度得分与原因）；新增「客户画像」卡片（9 项 + 主要顾虑中文映射，未填写显示紧凑提示）。
- 客户列表：卡片新增评级徽标。
- 验证：App `npm run type-check` PASS；`npm run build:mp-weixin` PASS；`git diff --check` PASS。
- 未完成：App **画像编辑页与标签管理页（写操作 UI）未做**（API 已就绪）；App 标签/提醒筛选未做。

## 2026-09-18 App 端画像编辑页 + 标签管理页
- 新增 `subpackages/customer/pages/tag-manage.vue`：系统标签（只读）／已选人工标签（点 × 解绑）／可添加标签（点 + 绑定）／新建标签（仅 boss/storemanager 可见，名称 + 分类）；对应 `customer/customer-tag-link/{save,delete}` 与 `customer/customer-tag/save`。
- 页面注册：`pages.json` 新增两页；`config/index.ts` 新增 `customerPortraitEdit` / `customerTagManage` 路由常量。
- 客户详情新增入口：「标签」按钮（标签行内）与「编辑画像 / 完善画像」按钮（画像卡片头），按角色控制（pm/finance/projectSupport 不显示）；详情页 `onShow` 会重新拉取，返回后自动刷新。
- 权限：服务端仍强校验（字典维护 403、系统标签不可改删、非指派设计师 404），前端仅控制入口可见性。
- 验证：App `npm run type-check` PASS；`npm run build:mp-weixin` PASS；`git diff --check` PASS。
- 至此 App 在标签/画像/分级的**读 + 写**能力对齐。

## 2026-09-18 P0-b 收口三件套（清空语义 / 字典维护 UI / 权限回归）
；App 标签管理页同步新增字典维护区块（`uni.showModal` editable 改名 + 确认停用）。
- **权限回归**：`ProviderSecurityAcceptanceTest` 新增 8 条 P0-b 路由的未授权断言（`customer/customer-tag/{page,list,save,update,delete}`、`customer/customer-tag-link/{save,delete}`、`customer/customer-portrait/save`），未携带 token 一律 401。
- 验证：后端 `mvn -o -q test` 全绿（123 tests / 0 failures / 0 errors）；App `type-check` + `build:mp-weixin` PASS；`git diff --check` PASS。
- 未覆盖（需验收 lane 基础设施）：按角色（业务员/设计师调字典写接口 403）、按租户/门店/团队越权、打标签幂等、系统标签不可改删、分级 level 与 score 一致性的端到端断言，仍待验收 lane 用带 token 的夹具补齐。

## 2026-09-18 待办中心改为日历 + 清单同屏
- 取消「清单 / 日历」Tab 切换，日历与清单**同页面同时呈现**：日历在上，清单在下。
- 点击日历日期 → 下方清单按该日 `TodoItem.due` 筛选；**再次点击同一天取消筛选**；并显示「已选日期 X · N 项 / 查看全部」筛选条。
- 选中的日历格高亮（品牌底色 + 内描边）；hover 反馈。
- 分类筛选（全部/客户跟进/量房任务/任务验收/阶段验收/回款提醒）继续作用于清单，与日期筛选叠加。
- 无截止时间的待办提示改为「仅在未筛选时显示」，避免用户误以为丢失；筛选后空态文案区分"该日期没有待办"与"当前没有待办"。

## 2026-09-18 待办中心改为左右分栏（日历左 / 清单右）
- 布局：`todo-layout` 两列网格 —— **左列业务日历（minmax 0/1.05fr）、右列清单（minmax 0/1fr）**，间距 20px，`align-items: start`；≤1280px 自动降为上下堆叠。
- 分类筛选（全部/客户跟进/量房任务/任务验收/阶段验收/回款提醒）随清单移入右列顶部，与日期筛选条、清单同列。
- 日历提示文案改为"点击日期筛选右侧清单"。
- **缺陷修复**：清单此前 `v-if` 用 `listItems.length` 但 `v-for` 仍遍历 `visibleItems`，导致点击日期后筛选不生效、仍渲染全部；已改为遍历 `listItems`。

## 2026-09-18 待办中心分栏细节收口
- **日历列固定宽度**：`grid-template-columns: 420px minmax(0, 1fr)`，右列自适应剩余空间；≤1280px 降为单列。
- **日历固定不随页面滚动**：左列 `position: sticky; top: 16px`。
- **右列清单内部滚动**：`.todo-layout__list` 用 flex 列 + `max-height: calc(100vh - 128px)`，工具栏与日期筛选条 `flex: 0 0 auto` 固定，`.todo-list` `flex: 1 + overflow-y: auto` 独立滚动；窄屏取消高度限制与内部滚动，恢复整页滚动。
- **空白日期轻提示**：点击无任务的日期时不再渲染整块 `EmptyState`，改为一行轻提示「XXXX-XX-XX 当天没有待办事项，可点击其他日期，或选择查看全部」；仅在未筛选且确无待办时才显示完整空态。

## 2026-09-18 量房记录瘦身（去除与方案设计重叠的明细字段）
- 业务口径：**保留量房记录**（谁、何时、在哪套房屋完成了量房），但**去除量房记录中的明细字段**——量房尺寸、原始结构、客户诉求、设计师结论、结构图、现场照片与其他附件；这些内容统一在「方案设计」沉淀，避免同一份现场资料两处维护、口径不一致。
- App `subpackages/customer/pages/measure-record.vue`：移除 5 组明细表单项（客户诉求复核 / 原始结构 / 区域尺寸 / 现场照片地址 / 设计师结论）及"区域尺寸必填"校验；提交载荷只保留 `propertyId + measureTime + status`；页面新增说明告知资料归属；标题改为「登记量房记录」。
- 兼容性：后端 `property/measure/save` 这些字段本就无 `@NotEmpty/@NotNull` 约束，前端停止采集不影响历史数据与接口契约；历史记录中的旧数据保留可查。
- 未改动：量房记录对象、`act:scheduleMeasure`（业务员约定量房）、量房率指标口径。
- 验证：App `type-check` + `build:mp-weixin` PASS

## 2026-09-18 量房记录恢复现场照片 + 客户素材归属确认
- 按用户口径修正上一轮：量房记录**保留「现场照片」**，其余（原始结构 / 量房尺寸 / 客户诉求 / 设计师结论与结构图 / 其他附件）仍不在量房记录中采集。
- App `measure-record.vue`：恢复现场照片地址表单项、状态回填与提交载荷 `sitePhotos`；说明文案同步调整。
- 承载确认：目标对象 `Design` 已具备承载能力——`sitePhotos`（现场照片）、`surveyPhotos`（量房照片）、`originalPlanUrl`（原始结构图）、`spaces`（空间尺寸 length/width/height/area/perimeter）。**因此"客户素材"无需后端新增字段，只缺前端呈现区块。**
- 验证：App `type-check` PASS

## 2026-09-18 方案原始结构图改多图（后端 + 迁移，前端待接）
- 后端：
  - `Design` 新增 `originalPlanImages`（`@ElementCollection` + `db_design_original_plan_images`），**保留原 `originalPlanUrl` 单图字段**用于历史数据，避免破坏性变更。
  - `design/scheme/save`、`design/scheme/update` 支持 `originalPlanImages` 入参并落库；`design/scheme/info` 响应白名单加入该字段。
  - 迁移 `33-方案原始结构图多图.sql` 已在本库执行（表已创建）。
  - 验证：后端 `mvn -o -q -DskipTests compile` PASS。
- 前端待办（本轮未做，避免半成品）：
  1. App `design/model.ts` 补 `originalPlanImages?: string[]`；
  2. App `design/api.ts` 的 `signScheme` 路径签发、`saveScheme` 载荷、`SchemeInput` 类型补该字段；

## 2026-09-18 客户素材接入前置：发现 design/scheme/update 全量覆盖风险
- 已完成第 1 步（两端 model + api，含 signScheme 签发链路）：
  - App `design/model.ts` 补 `originalPlanImages?: string[]`。
  - App `design/api.ts`：`normalizeScheme` 补字段；`signScheme` 把 `originalPlanImages` 纳入签名申请与 `next()` 消费序列（插在 surveyPhotos 与 spacePaths 之间，保证索引对齐）；`saveScheme` 载荷补 `originalPlanImages`（含 `fileApi.storagePath` 转换）；`SchemeInput` 补类型。
  - 验证：两端 `type-check` PASS。
- **阻断发现的严重风险**：后端 `design/scheme/update`（`design/service/schemes/update/support/Update.java`）**无条件全量覆盖**：
  ```java
  design.setName(name); ... design.setSitePhotos(sitePhotos);
  design.setOriginalPlanImages(originalPlanImages); design.setSurveyPhotos(surveyPhotos);
  ```
  即：若"客户素材"保存只提交素材字段，`name/style/layout/description/coverUrl/cadUrl/...` 会被 `null` **清空**，造成方案主数据丢失。
- 处理建议（下一步先做，再写 UI）：
  1. 把 `design/scheme/update` 改为**仅覆盖非空字段**（与 `customer-portrait/save` 的口径一致），或显式区分"未提交"与"清空"；
  2. 同时核对 `fileApi.storagePath` 能否把 `signScheme` 返回的**签名 URL** 还原为存储路径，否则保存会把签名 URL 写库；
  3. 上述两项确认后再实现 App「客户素材」区块，避免"传图变清空主数据"。
- 未完成：App 方案设计「客户素材」UI 区块（上传/预览/删除/保存）。

## 2026-09-18 方案局部更新安全化（决策 A）+ 客户素材接入前置验证
- **A 已实施**：`design/scheme/update` 由全量覆盖改为**仅覆盖显式提交字段**（null = 不修改，空集合 = 清空），覆盖 name/style/layout/description/coverUrl/cadUrl/originalPlanUrl/demolitionUrl/masonryUrl/optimizationUrl/sitePhotos/originalPlanImages/surveyPhotos。消除"保存客户素材导致方案主数据被清空"的数据丢失风险。
- 验证：后端 `mvn -o -q test` 全绿（123 tests / 0 failures / 0 errors）；两端 `type-check` PASS。
- **至此「客户素材」接入的前置条件已全部满足**：后端多图字段就绪（迁移 33 已执行）、局部更新安全、路径签发与还原闭环、两端 model+api 就绪。

## 2026-09-18 生产后端监控（13:01，exit 20，仅归档）
- 执行 `bash scripts/prod-monitor.sh collect "35 minutes ago"`，退出码 `20`；按安全门禁只归档证据，未修改业务源码、未创建源码快照、未重启、未构建、未发版。证据见 `docs/operations/incidents/2026-09-18/20260918-130145/triage.md`。
- 生产服务 enabled/active/running，HTTPS health `200`，根盘 16%，内存 available 约 5.2 GiB；`journalctl` 在 35 分钟窗口内无条目。
- 时间窗口内只有监控自身触发的 expected `tokenIsUndefined`；最近一条真实应用异常为前一日 `23:54:36.942` 的 `mcp` 扫描探测，不在窗口内。退出码 `20` 主要由历史日志和 unknown 分组造成。
- 人工待办：清理离职员工 token/轮询、将授权过期与明确业务规则纳入 expected、在 WAF/Nginx 拦截 `.env`/`graphql`/`config`/`mcp` 等探测路径、修复 registry 合并逻辑并恢复 `INC-20260916-a1233597b0` fixed 基线。

## 2026-09-18 REQ-20260918-SMB-P0-FIX 中小装企实机审查已裁决 P0 修复
- 输入：`输出/草稿/装得清中小装企全链路实机审查-20260918/T06-ANS-BIZ-20260918-004.md`、`SMB-REVIEW-20260918-002.md`；仅修已裁决 P0，合同签约最终角色、线下业主代录、人工材料计划补录继续保持 BLOCKED。
- P0-01 量房角色分离：`Measure.designer` 只表达设计师事实并允许为空，新增 `executor`（量房执行人）；预约不再把当前操作人写成设计师；完成量房前必须已有指派设计师（否则 400 且不落库）；量房完成不再回写 `Property.designer`，设计节点保持"待指定/待设计"。
- P0-02 PM 量房可见性：`ProjectAccessContext.requireParticipantProperty` 按显式项目参与关系（designer/manager/support/报价创建人）只读房屋量房事实，`property/measure/page` 在房屋数据范围未命中时按项目参与回退，越权仍 404；PM 不再因房屋权限无法判断开工 readiness。
- P0-03 阶段开工幂等与返回一致性：重复开工直接返回同一事实（不重复写动态/不覆盖实际开工时间）；开工前置校验（Effective 合同、项目经理、开工与计划完工日期、Completed 量房）全部前置于写库，失败整体回滚；返回脱离持久化上下文的阶段快照，消除"数据库已提交但接口返回失败"的懒加载序列化路径。
- P0-06 财务成本完整性统一：抽取唯一 `totalCost = max(实际成本, 应付成本)` 口径供汇总/项目报告/结算共用；汇总按项目逐个汇总成本完整性；补齐 `finance/summary/report` 的 `@Properties` 白名单（此前漏 expose `costComplete/actualCost/estimatedCost/costIncomplete*`，导致财务总览恒显示"成本未完整"）。
- 依赖项：资源页新增 `writeCapability`，收款/应收/应付/付款/合同写入口按能力显示（不再依赖 `editable` 布尔）；结算列表列改 `settlement.status` 且后端项目财务分页支持结算状态筛选；Finished/Settled 项目不再展示逆向 readiness；`MarketerReport` 的 follows/customers 空集合兜底，消除"未定义错误信息:{0}"。
- 迁移：新增 `docs/design/migrations/34-量房角色分离与收款幂等.sql`，并同步 `01-空库基线.sql`；本地 dev 库已执行 `MODIFY designer_id NULL` 步骤（其余列/索引/唯一约束由 Hibernate update 建立）。

## 2026-09-18 REQ-20260918-SMB-P0-FIX-2 验收 rework（A4/I/C/B/F/G 缺陷修复）
- 输入：`docs/acceptance/中小装企P0实机复验报告.md`（BLOCK）、`_系统/协作/lanes/acceptance/workspace/smb-p0/{api-probe-final,concurrent-stage-probe}.json`。
- DEF-SMB-P0-001 量房执行人权限：`property/measure/save` 在房屋已指派设计师时，只允许该指定设计师完成量房，其他角色（业务员/其他设计师/管理者）返回 `403 该房屋已指定设计师，量房完成必须由指定设计师执行` 且不落库；无设计师时仍 `400` 要求先指派。顺带消除重复提交导致的重复量房记录（优先复用 Scheduled，其次复用已有 Completed 行）。
- DEF-SMB-P0-002/003 预约态与多房屋：量房完成事实只认 `Measure.status=Completed`（房屋 `latestMeasureAt` 或量房记录），客户级 `measureAt`（预约时间）不再作为"已量房"依据；新增"已预约量房"系统标签；`customer/customer/info` 支持可选 `propertyId`，标签与分级按 activeProperty 重算；客户列表标签/筛选改用后端投影（新增"已预约量房"筛选项）。
- DEF-SMB-P0-004 并发开工：`project/stage/update` 改为先 `SELECT ... FOR UPDATE` 行级锁定阶段（`findLockedByStageId`），并发请求串行化后走幂等分支返回同一快照；并发首次开工会话实测两个响应均 `success=true` 且为同一事实，无 `undefinedErrorCode`、无死锁、StageOpen 仅 1 条。
- DEF-SMB-P0-005 readiness 文案：项目详情 readiness 增加 `pending` 文案，未完成项显示"量房未完成/项目经理未确定…"，不再出现"开工前仍需完成：量房结果已完成"。
- DEF-SMB-P0-006 合同负向语义：`project/contract/update` 先判角色（非 Boss 一律 403）；`project/contract/save` 在同企业但超出数据范围时同样返回 403，跨企业/不存在报价保持 404（不泄露对象存在性）。实测 StoreManager/Marketer/Pm/Designer 两个接口均 403，缺失报价 404。
- DEF-SMB-P0-008 重复签约幂等：`project/contract/update` 对已签约（Signed 及后续履约状态）的合同重复提交 `Signed` 直接返回同一合同事实，不重复推进状态、不重复写数据。

## 2026-09-18 REQ-20260918-SMB-P0-FIX-3 量房幂等与执行人角色响应
- DEF-SMB-P0-009 重复 Completed 覆盖历史时间：`property/measure/save` 在房屋已有 `Completed` 量房事实且无待完成预约时，直接返回原 Measure，不再写 `measureTime / property.latestMeasureAt / executor / designer / status / 附件`；权限校验仍先于幂等返回（业务员重复提交仍 403）。
- DEF-SMB-P0-010 executor.role 串值：根因是框架 `JwtSecurityManager.authenticate` 用 `BeanUtils.instantiateClass(Employee.class)` 生成只带 `empId` 的占位主体（`Employee.role` 字段默认值恰为 `Marketer`、`name` 为空），原实现把该占位对象直接写入 `Measure.executor`，导致响应 `executor.role=Marketer`。修复：`PropertyContext.requireEmployee(principal)` 通过 `DataScopeContext/TenantContext` 重新加载完整员工实体，量房完成只用完整实体写 executor 与后续归属。

## 2026-09-19 谈单 PPT 移除双方案硬门槛
- 用户决策：取消“同一客户至少完成两套设计方案才能生成谈单 PPT”的限制。
- 后端：`TalkContext` 删除同客户可谈方案计数与 `DesignRepository` 无用法依赖；保留主推方案必须为 `Ready/Presented/Selected`、必须关联本方案有效报价的校验。
- 文档：同步设计转化域、状态驱动逻辑、App 设计转化闭环和后端流程说明，移除“两套方案”门槛表述。
- 验证：后端 `TalkContextTest` PASS；除既有 `ProviderSecurityAcceptanceTest`（路由数期望 217、当前 218）外，后端其余测试全部 PASS；App `npm run type-check` PASS；相关文件 `git diff --check` PASS。
- 风险：`ProviderSecurityAcceptanceTest` 失败由当前工作区未同步的 `catalog/item/import` 路由计数导致，与本次谈单门槛修改无关；未修改验收断言。

## 2026-09-19 设计方案完整复制
- App 设计转化列表每个方案新增“复制”操作，复制成功后刷新列表并进入新方案编辑页。
- 后端 `design/scheme/save` 支持 `copyFromId`：复制主方案主数据、图纸/图片、全部区域、全部分项及分项区域映射；报价和谈单记录不复制。
- 新方案名称自动避重为“原名（副本）/（副本N）”，版本固定从 `1` 开始，状态重置为 `Draft`；仍复用现有 `Security` 与 `DataScope` 权限边界。
- 验证：`DesignCopyContextTest` PASS；App `type-check`、`build:mp-weixin`、`check:subpackages` PASS；后续 Provider 路由计数同步后由后端全量测试覆盖。
- 风险：`npm run verify:mp-weixin` 的既有客户来源静态契约在 `roleCatalog.marketer` 包含 `P.quoteList` 时失败，与本次复制功能无关；未修改该角色契约。

## 2026-09-19 未完成设计方案直接删除
- App 设计转化列表中仅 `Draft` 方案显示“删除”，二次确认后调用 `design/scheme/delete`，成功后刷新列表。
- 后端新增事务化 `DesignDeleteContext`：按数据权限加载方案，删除方案区域、分项和图片；已生成报价或谈单记录时拒绝删除。
- `Ready/Presented/Selected/Archived` 不允许删除；只有尚未完成且没有下游商务事实的 `Draft` 可删除。
- Provider 路由安全清单同步为 total 219 / protected 207，并增加 `design/scheme/delete` 未携带 token 必须 401 的断言。
- 验证：`DesignDeleteContextTest`、`ProviderSecurityAcceptanceTest` PASS；后端 `mvn -q test` 全量 PASS；App `type-check`、`build:mp-weixin`、`check:subpackages` PASS。
- 风险：`npm run verify:mp-weixin` 仍被既有 `roleCatalog.marketer` 包含 `P.quoteList` 的客户来源静态契约阻断，与本次删除功能无关。

## 2026-09-19 设计报价上下文、套餐版本与套餐配置闭环
- 后端强规则：自定义报价只允许从 `Ready/Presented/Selected` 设计方案生成；`reject()` 收紧为只允许 `Submitted -> Rejected`。
- 报价版本：套餐报价按“同一客户 + 同一套餐模板”已有报价数递增，不再固定为版本 1；自定义报价继续按设计方案递增。
- 驳回重报：`quote/save + sourceQuoteId` 基于 Rejected 报价生成新的 Draft 快照，原报价保持不可修改；App 报价详情新增“改版重报”。
- App 上下文：从设计方案进入报价时，客户、房屋、设计方案锁定只读
- 套餐语义：`PackageTemplate.price` 明确为标准面积对应的折前牌价，`discount` 单独应用，禁止把 `price` 视为已折扣价。
- App 套餐配置：新增「我的 -> 套餐模板」及列表/编辑页，覆盖基本信息、分区、报价项目行、数量系数、计价方式、包含、允许扣减、升级/替换/差价规则说明和成本预览。
- 套餐报价调整：新增 `quote/line/save`、`quote/line/delete`；Draft 报价可扣减基础行、升级/替换目标项目、追加增项，并重新计算小计、税金、金额和成本。
- 后端配置路由：新增 `pack/section/{save,delete}`、`pack/line/{save,delete}`、`pack/template-item/save`；Provider 安全清单同步为 total 226 / protected 214。
- 验证：新增 `QuoteContextBusinessRulesTest`、`QuoteAdjustmentContextTest` PASS；后端 `mvn -q test` 全量 PASS；App `type-check`、小程序构建、分包检查 PASS
- 边界：套餐“升级/替换”采用“扣减原基础行 + 追加新项目”的净差价模型；规则文本用于展示和配置说明，尚未扩展到按品牌/规格自动匹配差价公式。

## 2026-09-19 修复套餐与报价行 Provider 请求实体注入
- 现象：App 新增套餐调用 `pack/template/save` 报 `not-null property references a null or transient value: PackageTemplate.name`；继续操作后 `pack/section/save` 报同类 `PackageSection.name` 异常。
- 根因：套餐创建/更新、套餐分区、套餐项目行、套餐差价项以及报价草稿调整 Provider 的 `call(xxx request)` 未统一使用 `@ModelSaveInject`，请求字段没有进入实体参数。
- 修复：`pack/template/{save,update}`、`pack/section/save`、`pack/line/save`、`pack/template-item/save`、`quote/line/save` 全部补 `@ModelSaveInject`；模板保存继续通过 `@AutoInject + @NotEmpty/@NotNull` 校验名称、标准面积和套餐牌价。
- 防复发：新增 `PackageProviderInjectionContractTest`，通过反射校验上述写接口必须声明 `@ModelSaveInject`，同类漏洞再次出现时测试直接失败。
- 验证：`mvn -q -DskipTests compile` PASS；`PackageProviderInjectionContractTest` PASS；后端 `mvn -q test` 全量 PASS。

## 2026-09-19 套餐设计详细说明
- 新增 `docs/product/装得清-套餐设计详细说明.md`，面向企业负责人说明套餐概念、字段、配置步骤、报价算法、应用流程、调整规则、权限、示例、验收和当前边界。
- 文档明确套餐是模板、报价是快照；`套餐牌价`为折前价，`discount` 单独应用；套餐报价先 Draft，提交后不可改，驳回后生成新版本。
- 同步在套餐设计说明中增加产品手册入口。

## 2026-09-19 导入麦鸿报价项目并创建两套套餐
- 数据源：`/Users/zhangwei/Desktop/麦鸿装饰报价9.5.xlsx`。
- 项目库：生成 550 条去重报价项目并事务导入，导入后企业项目库 553 条；施工 163、主材 254、软装 13、综合 15、全屋定制 108。
- 套餐：创建 `麦鸿现代简约套餐`（templateId `102085718435693645`）和 `麦鸿品质升级套餐`（templateId `102085718435693727`）；均为 118㎡、98% 默认折扣、7 个分区、74 个项目行。
- 金额：现代简约牌价/标准成本 92,392.04；品质升级牌价/标准成本 95,539.30。
- 幂等验证：重复执行导入 SQL 后，项目库仍 553、麦鸿套餐仍 2、套餐行仍 148。
- 记录：`docs/operations/2026-09-19-麦鸿报价9.5导入记录.md`；导入包位于 `data/import/maihong-9.5/`。
- 风险：Excel 无独立真实成本，本次沿用既有口径 `costPrice=材料基价+人工基价`，不得直接作为正式利润口径。

## 2026-09-20 排查 App 启动问题
- 复现：`npm run dev:mp-weixin` 编译成功，生成 `dist/dev/mp-weixin`，并进入 watching。
- 微信开发者工具 CLI：已登录，调用 `cli open` 可正常打开产物；模拟器日志显示 `App onLaunch/onShow` 正常，当前自动进入登录页。
- 配置核对：`pages.json` JSON 有效，12 个分包，新增套餐管理、套餐配置、报价调整页面均进入 `app.json` 和对应分包。
- 为兼容常见启动命令，`package.json` 新增 `npm run dev` 和 `npm start` 别名，均映射到 `dev:mp-weixin`。
- 验证：`npm run dev`、`npm run dev:mp-weixin`、`npm run type-check` 均通过。

## 2026-09-20 设计方案使用套餐（Hybrid 报价）
- 数据模型：`QuoteLine` 新增 `differenceType`、`differenceStatus`，记录套餐行和设计分项差异及确认状态；`Quote` 新增 `costComplete`。迁移 `37-混合报价设计差异.sql`、`38-报价成本完整性.sql` 已在本库执行。
- 后端：新增 `HybridQuoteBuilder`。Hybrid 报价同时关联 `Design` 和 `PackageTemplate`，以套餐为基线，按 CatalogItem、空间类型、分类/名称匹配设计分项。
- 差异：支持 Matched、PackageOnly、DesignOnly、QuantityChanged、Deduct、Upgrade、Replace；存在 Pending 差异时 `submit()` 拒绝提交客户。
- 调整：`quote/line/save` 支持确认/忽略差异，并继续支持扣减、升级、替换和增项；金额、税金和成本重新计算。
- App：设计报价确认页可选择套餐生成 Hybrid 报价；报价调整页显示差异类型并支持确认、忽略、扣减、升级和替换。
- 验证：`HybridQuoteBuilderTest`、`QuoteContextBusinessRulesTest`、后端 `mvn -q test` 全量 PASS；App 小程序构建和分包检查 PASS
- 风险：自动匹配无法判断的差异会保留为 Pending，需要设计师人工确认；不会自动删除套餐独有项目。


## 2026-09-20 生产后端监控（13:01，exit 20，仅归档）
- 执行 `bash scripts/prod-monitor.sh collect "35 minutes ago"`，退出码 `20`；服务 enabled/active/running，PID `47233`，HTTPS health `200`，根盘 17%，无基础设施异常。
- 35 分钟窗口的 `journalctl` 无条目；唯一应用日志是健康探测 expected `tokenIsUndefined`，非预期异常最近时间为同日 `04:20:50`，不在窗口内。
- 主要 unknown 为 `.env`、`graphql`、`config`、`mcp` 等扫描探测和 `auth/signin` 陈旧/未知入口；无高置信源码根因，未进入自动修复门禁。
- registry 再次把历史已修复懒加载事故改写为 `INC-20260920-a1233597b0/open`；已定位到分析器当前日期重建 ID 并覆盖 fixed 状态，但本次只记录，不修改监控脚本。
- 门禁：未修改源码、未创建源码快照、未测试发布、未重启、未构建、未发版；证据见 `docs/operations/incidents/2026-09-20/20260920-130110/triage.md`。

## 2026-09-20 设计师工作台整改
- 需求输入：设计师工作台审查结论 `DW-P0-01～04`、`DW-P1-01～05`；业务按 `BR-005`、`DEC-002` 和“成本默认不可见/不完整不计算利润”规则执行。
- 设计工作台：后端改为按“客户 + 房屋”聚合，分别读取该房屋的量房、诉求和设计方案；不再取客户第一套房屋；App 跳转携带 `propertyId`。
- 报价工作台：`Quote` 新增 `submittedAt`、`rejectedAt`、`acceptedAt`、`convertedAt`，状态推进写入不可变时间；工作台按状态时间计算停留天数，调整次数按同房屋报价版本计算。迁移 `39-设计师工作台状态与成本口径.sql`。
- 签约工作台：查询范围由仅 `Accepted` 扩为 `Accepted + Converted`，通过合同状态和唯一项目区分待签约/已签约；已生效合同不会因报价转项目而从工作台消失。
- 设计工作台收口：候选房屋存在 `Accepted/Converted` 报价时按房屋排除，解决客户确认报价后仍停留在设计工作台的问题；客户其他未确认房屋仍独立保留。
- 成本权限：新增 `quote/designer/page` 和 `quote/designer/info`，从服务端响应白名单移除 `cost/costComplete/costAmount/costPrice`；App 按 `view:cost` 或 `catalog:cost` 控制展示，成本不完整时禁止计算利润。
- 设计交互：新建设计项改为选择企业报价项目库并提交 `catalogItemId + quantity`，价格由后端项目库覆盖；量房关注点支持按 `requirementId` 更新，编辑前先加载原值，避免重复新增。
- 验证：后端 `mvn -q test` 150/0/0/0 PASS；App `type-check`、`build:mp-weixin`、`check:subpackages` PASS
- 风险：合同签署/生效的角色矩阵仍按最小权限只允许 BOSS，设计师工作台显示“查看签约进度/等待签约”，不得据此放宽合同写权限；真实小程序和历史数据迁移仍需 acceptance 复验。

## 2026-09-20 合同最终签约价格闭环
- 金额语义：`baseline_amount` 继续表示客户确认报价快照；新增 `signed_amount` 表示签约时确认并由客户作为最终依据的合同价格；`current_amount` 表示最终签约价叠加已确认合同变更后的当前金额。
- 后端：合同创建或签约更新必须传入大于 0 的 `signedAmount`；签约时冻结 `signedAmount` 并同步 `currentAmount`，后续合同变更只改 `currentAmount`，不回写最终签约价。
- 下游：项目立项金额、应收计划和合同对客展示统一读取 `currentAmount`；签约后其初始值等于最终签约价，变更后继续叠加变更金额。
；App 合同签约页新增“最终签约价”，待签合同可直接签约后生效，不再要求先按报价金额形成已签事实。
- 迁移：新增 `docs/design/migrations/40-合同最终签约价格.sql`；历史已签约合同按当时报价基线回填 `signed_amount`。
- 验证：后端 `mvn -q test` 153/0/0/0 PASS；App type-check、小程序构建、分包检查 PASS
- 待验收：真实签约输入最终价、项目合同额、应收合计及后续合同变更同步；迁移 40 尚未对目标 MySQL/生产执行。

## 2026-09-20 生产清库与 App/后端发版
- 生产清库前备份：`/opt/backups/th4-decoboon/pre-data-reset-20260920-141326/decoboon-before-reset.sql.gz`，SHA-256 `2c3eab8ce97cc2bb4f408cd4200c70003c3c6da072de12777ade10dbe980cd31`。
- 历史库 52 张 `db_decoboon_*` 表已迁移为当前 `db_*` 表名；应用迁移 39/40 并补齐 36–38 缺失列；除 `db_schema_migration` 外全部表清空，Redis DB 清空。
- 后端发布：`/opt/decoboon/releases/20260920-142357`，systemd `active`，HTTPS health `200`，无 token 返回预期 `tokenIsUndefined`。
- 清库后业务数据核验：企业 0、员工 0、客户 0、报价 0、合同 0；套餐初始化 4 条；迁移账本 14 条。
- App 发布构建 `1.0.1` 已通过 release gate，并上传到微信开发者工具后台；正式提审和发布仍需管理员在微信公众平台操作。
- 发布记录：`docs/operations/2026-09-20-生产清库与发版.md`、`docs/operations/2026-09-20-微信小程序上传记录.md`。

## 2026-09-20 麦鸿设计本地组织与角色账号
- 本地开发库在 `麦鸿设计 / 总店` 下建立 Boss 张伟、店长王宏文、业务员黄贤杰、设计师谢玲玲/董夏鹏、财务吴濛濛共 6 人组织关系。
- 五名新增员工补齐本地验收手机号 `19900000001`–`19900000005`、Password 凭证和 BCrypt 密码哈希，并修正 DateTime 创建时间投影。
- 验证：五人密码登录与 `auth/session/info` 均 PASS；店长成员分页返回总店 6 人；设计师成员管理返回 403。
- 边界：未指定团队负责人，未创建 Team；账号手机号为本地合成数据，不得用于短信、邀请或生产登录。
- 风险：基础版年费 5 席，当前在职 6 人；需负责人决定加购 1 席或升级专业版，未擅自修改订阅金额和套餐。

## 2026-09-20 企业负责人设计经营视图
- 产品收敛：BOSS 首页功能域不再使用含糊的“客户管理”，改为客户经营；补齐方案设计、报价与谈单、合同签约等经营阶段入口。
- 后端新增 `statistic/boss/design/report`，以客户房屋聚合设计诉求、方案区域/分项、最新报价、谈单 PPT、客户确认、阶段停留、风险和设计师负荷。
- 修复 BOSS 设计调度页无报表导致“设计师 0、负荷全空”的问题；在职设计师零任务也返回零负荷行。
- 签约工作台改为 BOSS 全企业、店长门店、团队长团队、设计师/业务员本人的数据范围，并增加设计负责人字段。
- 验证：后端全量 `mvn -o -q test` PASS；App `type-check`、`build:mp-weixin`、`check:subpackages` PASS；本地 BOSS 接口成功、Designer 越权 403。
- 风险/待验收：真实设计数据布局、报价确认到签约状态迁移和团队范围回归待 acceptance 独立执行。

## 2026-09-21 企业负责人报价谈单客户聚合
- 首页功能域和方案设计经营页快捷入口统一为“报价谈单”。
- 新增 `statistic/boss/quote/report`：BOSS 按客户查看多套房、多版本报价、最新有效金额、谈单 PPT、客户确认、风险和下一步；历史报价最多返回 10 版。
- BOSS 报价页改为客户卡聚合，客户下再展开房屋和报价版本；设计师继续使用原报价单列表。
- 验证：后端全量 65 suites / 184 tests PASS；App type-check、build:mp-weixin、check:subpackages PASS；真实 HTTP 夹具验证同一客户 2 房 3 报价只返回 1 个客户，历史版本顺序正确，验证后清理。
- UI 二次重构：按客户经营页节奏重排为经营说明、四张指标卡、风险入口、搜索筛选排序、客户身份与经营时间线、金额结果、下一步、房屋报价卡和版本历史。
- 最终构建：App type-check、build:mp-weixin、check:subpackages PASS，总包 1728193 B。
- 待验收：真机检查客户多房多报价的信息密度、版本浏览和状态组合。

## 2026-09-21 装企历史数据承载范围裁决接入与设计冻结

- 用户三项决策（`DEC-004`）：只承载在跑业务 + 主数据、已完工历史只做档案不导入；实施/客服代导入固化为正式服务能力；历史导入数据不参与经营指标。
- 业务侧产出：`DEC-004`、`GAP-006`（已关闭）、`GAP-001` 第 5 条关闭、`EV-024`、`REQ-BIZ-20260921-001`；`knowledge.py validate` 通过（29 卡 / 24 证据），索引已重建。
- 项目侧产出：`docs/product/2026-09-21-装企历史数据承载-三条渠道交互流程.md`、`docs/plan/2026-09-21-装企历史数据最小承载集字段冻结.md`。
- 关键判断：把"客户把数据翻译成我们的模板"改为"客户只提供原始数据 + 确认结果，系统与服务侧承担格式转换"；业务对象走引导式建档，主数据走原表映射导入，服务侧代导入兜底。
- 冻结范围：批次 1 组织与员工、批次 2 客户与房屋、批次 3 报价项目库/成本/供应商/品牌、批次 4 合同基线与在跑项目、批次 5 期初应收应付；已完工项目、已结清往来、离职员工、历史采购流水不导入。
- 未进入本轮范围：`ImportBatch` 代码实现、渠道 B 列映射引擎、渠道 C 服务工单均未开发，本轮只完成业务裁决、范围与字段冻结。
- 待确认：服务工单归属域、原表同义词库首批范围、历史来源标记字段与索引、期初余额重导时间窗口。

## 2026-09-21 装企历史数据导入产品化搁置

- 用户同日调整方向：历史数据导入不做产品化，产品以新客户为准。
- 存量装企老数据改由技术人员手动导入，不作为产品承诺、不承诺客户自助、不承诺任意格式。
- 业务侧同步：`DEC-004` 增加修订记录并取代原“服务侧代导入为正式服务能力”；原承载范围、统计隔离口径保留，作手动导入口径；`REQ-BIZ-20260921-001` 转为搁置；`GAP-006` 保持关闭并补记。knowledge.py validate 通过（29 卡 / 24 证据）。
- 项目侧同步：三渠道交互流程与最小承载集字段冻结两份文档标记为 suspended，转备查，不进入研发排期。
- 结论：本轮不产生开发任务；`ImportBatch`、列映射引擎、服务工单均不启动。

## 2026-09-21 麦鸿设计多阶段业务演示数据
- 导入麦鸿报价 9.5：550 条项目库、2 套套餐模板。
- 新增业务推演脚本，通过真实 Provider 从客户登记推进到量房、设计、报价、客户确认、合同、项目和验收待确认。
- 44 个案例覆盖 13 个阶段；设计后每阶段 4 条，谢玲玲、董夏鹏各 2 条；两人各 18 套方案、32 份报价。
- 全量核对：44 客户/房屋/量房、36 诉求/方案、108 区域、216 分项、64 报价、12 合同、8 项目、24 应收、4 回款、4 流水。
- 修复多房屋客户诉求保存错误调用单房屋推断的问题，并补 `DesignContextMultiPropertyTest`。
- 项目停在验收待业主确认，不伪造业主身份确认。
- 记录：`docs/operations/2026-09-21-麦鸿业务演示数据构造记录.md`。
- 最终验证：后端 66 suites / 185 tests PASS；App type-check、小程序构建、分包检查 PASS；数据核对脚本 failures=0。

## 2026-09-21 成本缺失状态图标化
- App 新增 `JzCostIncomplete`，点击图标用 Modal 展示成本缺失原因，覆盖首页毛利、报价详情、财务项目/项目账本、项目详情、设计师报价工作台、经营驾驶舱和报价行卡片。
；App type-check + build:mp-weixin + check:subpackages PASS（总包 1,726,815 B）。

## 2026-09-21 报价谈单设计师分组与锚点
- 客户唯一负责设计师规则落地；`customer/customer/handoff` 支持更换并同步客户、全部房屋、预约量房、未归档方案和设计诉求，完成量房与归档方案保留历史。
- `statistic/boss/quote/report` 返回 `designerGroups`，待指派设计优先，其他设计师按风险、待确认、报价金额和最近活动排序。
- App 报价谈单页改为报价项目库式左右分栏：左侧设计师分组，右侧当前设计师客户列表，并在右侧顶部固定客户锚点，当前客户卡显示品牌色边框和阴影；首次滚动客户列表自动收起概览进入全屏工作区，设计师侧栏缩小为 156rpx。
- 验证：新增 `HandoffDesignerChangeTest`；后端 67 suites / 186 tests PASS；App 构建 PASS，总包 1726815 B；真实接口验证客户换设计师后从原分组迁移到新分组。

## 2026-09-21 小程序报价打印稿下载域名修复（业务域名网关）

- 现象：App 报价详情点击生成打印稿后报 `downloadFile: fail url not in domain list`，PDF 无法下载打开。
- 根因：`document/quote/build` 返回的 `DocumentRecord.url` 是 OSS 预签名地址 `https://decoboon.oss-cn-hangzhou.aliyuncs.com/...`；小程序 `uni.downloadFile` 只允许已备案的业务域名，第三方 OSS 域名无法作为业务 `downloadFile 合法域名` 配置。
- 方案（用户裁决选 B）：业务单据下载固定走业务域名网关，由后端校验签名后回源对象存储，客户端不再直连 OSS。
- 改动：
  - `src/main/java/com/th4/edge/decoboon/common/file/context/FileSignatureContext.java`：新增 `signGateway()`（始终返回 `/file/private/download?...` 网关相对地址）与 `materialize()`（对象存储回源落盘到 `<upload-dir>/.download-spool`，按 10 分钟 TTL 自动清理；本地存储直接解析原文件）。
  - `src/main/java/com/th4/edge/decoboon/document/context/DocumentArchiveContext.java`：`signed()` 改用 `signGateway`，覆盖 `document/quote/build`、`document/record/page`、`document/reprint/save`。
  - `src/main/java/com/th4/edge/decoboon/common/file/service/grants/download/support/Download.java`：改用 `materialize`，`file/private/download` 支持 OSS 回源。
- 验证：`mvn -o test` 全量 191 tests / 0 failures / 0 errors（新增 `FileSignatureContextTest` 5 例，覆盖网关签名、签名校验回源闭环、本地解析与附件直连不变；`DocumentArchiveContextTest` 同步 `signGateway` 契约；`OwnerFileExposureAcceptanceTest` 本地存储签名下载回归通过）。
- 待办（外部阻断）：微信公众平台 `downloadFile 合法域名` 增加业务域名 `decoboon.joytsing.com`；开发者工具本地调试需勾选「不校验合法域名」。
- 风险：网关下载由后端回源对象存储，增加后端带宽与磁盘 I/O；回源文件保留 10 分钟，长于 300 秒签名有效期，保证下载读取期间不被清理；并发下载用临时文件原子替换避免读到半个文件。

## 2026-09-21 方案设计推进交互统一
- 老板端方案设计经营页将设计师负荷前置到方案推进之前。
- 方案设计推进改为报价谈单同款交互：左侧设计师分组，右侧当前设计师客户，固定客户锚点，滚动自动全屏，当前客户卡品牌色高亮。
- 客户卡按客户聚合方案时间线、报价确认、多房屋设计内容、报价、谈单 PPT 和下一步。
- 验证：App type-check、build:mp-weixin、check:subpackages PASS，总包 1744327 B。

## 2026-09-21 报价打印稿：字体乱码、平方米单位、区域分组与下载命名

- 用户反馈：导出的报价打印稿字体/单位乱码，要求按“装企企业_设计师_客户_小区_面积_价格_版本”命名、报价明细按区域分组。
- 根因（用 `11.pdf` 原始对象验证）：
  - `/FontFile*` 出现 0 次、`/ToUnicode` 出现 0 次：PDF 使用 `BaseFont.NOT_EMBEDDED` + `STSong-Light/UniGB-UCS2-H`，字体未嵌入，手机阅读器缺字即整段乱码，且复制/提取文本必然乱码。
  - `Unit.Area` 的值是 `m²`（U+00B2），非 CJK 字形，在 STSong-Light 下缺字，即用户说的“平方米乱码”。
  - 报价明细按 `itemId` 平铺，没有按施工区域分组。
- 改动：
  - 新增 `src/main/resources/fonts/DroidSansFallback.ttf`（CJK）与 `Roboto-Regular.ttf`（西文数字），均为 Android AOSP 内置、Apache-2.0 可嵌入；`fonts/NOTICE.txt` 记录来源与授权。
  - `pom.xml` 增加独立 `filtering=false` 资源项：二进制字体禁止过滤，否则字体会被 `${}` 过滤损坏（实测 4.11MB 被改成 4.93MB 无法加载）。
  - `QuotePdfRenderer`：改用 `FontSelector` 组合 CJK+Roboto 并嵌入子集；`Unit.Area/Metre/Centimeter` 输出“平方米/米/厘米”，文本统一把 `m²/㎡/m2` 归一为“平方米”；明细按施工区域（`QuoteLine.space` 的 sort/name）分组并输出区域小计。
  - `DocumentArchiveContext`：打印稿文件名改为 `装企企业_设计师_客户_小区_面积_价格_版本.pdf`，片段内的 `_` 换成 `-`，去分隔符与控制字符，单段截断 40 字符。
  - `FileSignatureContext.signGateway(path, name)` 附带展示文件名，`materialize(path, name)` 按业务名落盘，`file/private/download` 增加 `name` 参数，使网关下载的 `Content-Disposition` 是业务文件名而不是哈希名。
  - App `subpackages/quote/pages/detail.vue`：下载后先按业务文件名 `saveFile` 到 `USER_DATA_PATH` 再 `openDocument`，保证转发/保存到手机时不是随机临时名；沙箱不可用时回退临时路径。
- 验证：
  - 后端 `mvn -o test`：194 tests / 0 failures / 0 errors。
  - 新增/加强断言：渲染结果含 `/FontFile2` 与 `/ToUnicode`；提取文本含“平方米”“118.00 平方米”“168000.00/6400.00/3200.00”且不含 `m²`；文件名断言为 `合肥示例装饰工程有限公司_0平方米_10元_V1.pdf`；网关 URL 含业务 `name` 且 `materialize` 落盘名去分隔符。
  - 端到端样例：`/tmp/quote-sample.pdf`，提取文本确认“报价合计：￥179238.40”“计价面积 50.54 平方米”“客厅（2 项）小计 ￥36102.40”等区域小计与数字均正常。
  - App `npm run type-check`、`npm run build:mp-weixin`、`npm run check:subpackages` 全部通过（总包 1,744,804 B）。
- 风险与约束：
  - 明细里“分类”列仍是单行分类，区域是小计分组节点；如需区域做表格合并单元格需另立需求。
  - 字体为 AOSP Apache-2.0，如需替换品牌字体必须同时覆盖中文与西文且可嵌入。
  - 图片/附件预览仍走 OSS 预签名地址，与本次单据下载链路无关。
- acceptance_needed：真机验证报价打印稿下载打开、文件名、区域分组与金额显示；覆盖 Designer/Boss 角色与历史报价（含 `m²` 存量单位）。

## 2026-09-21 客户 Tab 当前卡片高亮
- 客户列表增加滚动位置测量，当前客户卡仅显示品牌色边框和阴影，不改变背景色。
- 当前客户按卡片中心与屏幕中心距离最近识别，确保屏幕中间客户成为当前卡。
- 验证：App type-check、build:mp-weixin、check:subpackages PASS，总包 1745929 B。

## 2026-09-21 启动报错修复：run-dev 运行态缺少 OpenPDF

- 现象：`./run-dev.sh` 启动失败，`NoClassDefFoundError: com/lowagie/text/Element` → `quotePdfRenderer` 创建失败 → `UnsatisfiedDependencyException`。
- 根因：`pom.xml` 的 `openpdf` 依赖是在上次 `mvn package`（target/lib 与 boot jar 均为 2026-09-20 14:15）之后才加入的；`maven-dependency-plugin:copy-dependencies` 绑定在 `prepare-package`，而 `run-dev.sh` 只在 `target/*.jar` 不存在时才执行 `mvn package`，因此 `target/lib` 从未包含 openpdf。此前用 IntelliJ 的 Spring Boot 运行配置走的是 Maven 全量 classpath，所以一直没有暴露。
- 修复：执行 `mvn -o package -DskipTests` 重建 `target/lib` 与 boot jar；确认 `target/lib/openpdf-1.3.27.jar` 就位、`BOOT-INF/classes/fonts/*.ttf` 已进 jar。
- 验证：启动日志中 `NoClassDefFoundError`/`UnsatisfiedDependencyException` 消失，应用推进到 JPA 初始化；`java -cp "target/lib/*:target/classes"` 下 `com.lowagie.text.Element`、`QuotePdfRenderer` 与两个字体资源均可解析。本地 `127.0.0.1:3306` 连接失败属沙箱网络限制，非代码问题。
- 结论：**只要 `pom.xml` 依赖发生变化，必须重新 `mvn package`（或至少 `prepare-package`）**，否则 `run-dev.sh` 会以旧 `target/lib` 启动并报类找不到。

## 2026-09-21 凭证套打与工人档案后端实现

- 目标：按 `DEC-005`/`DEC-006` 与字段冻结清单 v2.0 落地工人档案与凭证生成后端。
- 改动：
  - 新增 `labor` 域：`Worker` 实体（企业级共享、身份证企业内唯一）、`WorkerPrivacyContext`（敏感字段按角色裁剪）、`labor/worker/page|save|update` 三条 Provider（维护限项目经理）。
  - `document` 域扩展凭证：`VoucherRecord`（凭证号、金额、大写、对方、模板版本、PDF、状态）、`VoucherItem`（来源单号、日期、账户、金额、类别、备注）、`VoucherContext`（合并判定、编号生成、落库、重打幂等）、`VoucherPdfRenderer`（OpenPDF + 内嵌 CJK 字体）、`VoucherAmountWords`（金额大写）。
  - 新增路由 `document/voucher/build`、`document/voucher/page`；`Payment` 增加可选 `worker` 关联；`LedgerRepository` 增加按 ID 集合查询。
  - 新增迁移 `44-工人档案与凭证表.sql`（`db_worker`、`db_voucher_record`、`db_voucher_item`、`db_finance_payment.worker_id`）。
  - 测试：`VoucherAmountWordsTest`、`WorkerPrivacyContextTest`、`VoucherBuildAcceptanceTest`；`ProviderSecurityAcceptanceTest` 路由总数 256→261 并补 5 条未授权断言。
- 验证：`mvn -o test` = **201 tests / 0 failures / 0 errors / 0 skipped**。
- 风险：收款单（Receipt）分支未端到端验证；L2 模板配置界面未做；合并凭证到账账户未逐行快照；敏感字段额外对项目经理放开（与冻结清单原文有差异，待确认）；迁移未执行。
- acceptance_needed：四角色 × 敏感字段 × 跨企业负向用例；真实数据"建档 → 付款 → 出凭证 → 重打"闭环。
- 交接文档：`_系统/协作/lanes/development/workspace/voucher-worker-handoff.md`

## 2026-09-21 App 端：工人档案与收付凭证

- 目标：把 `labor/worker/*` 与 `document/voucher/*` 接入微信小程序，供项目经理维护工人、财务打印凭证。
- 改动（`th4-decoboon-app`）：
  - 新增领域 `src/domains/labor/{model.ts,api.ts}`：工人档案分页 / 新增 / 编辑（服务端已按角色裁剪身份证号与银行账号）。
  - 新增分包 `subpackages/labor`：`pages/workers.vue`（列表 + 搜索 + 项目经理新增/编辑入口）、`pages/worker-edit.vue`（建档 / 编辑 / 停用）。
  - 新增 `subpackages/finance/pages/vouchers.vue`：凭证列表 + 下载后按业务文件名落盘并 `openDocument` 打印/转发。
  - `subpackages/finance/pages/ledger.vue`：新增"收付凭证"入口与"选记录出凭证"多选模式（底部合计栏 + 生成凭证），对应服务端合并判定。
  - `domains/finance/{model.ts,api.ts}`：新增 `Voucher`/`VoucherItem` 类型与 `vouchers()`、`buildVoucher(ledgerIds)`。
  - `roles/permission.ts`：新增 `view:worker` / `edit:worker`，授予 BOSS、店长、财务、设计师（查看）与项目经理（查看 + 维护）。
  - `roles/catalog.ts`：项目经理工作台新增"工人档案"；财务工作台新增"收付凭证"。
  - `config/index.ts` + `pages.json`：新增 3 个路由常量与 `subpackages/labor` 分包注册、`finance/pages/vouchers` 页面注册。
- 验证：`npm run type-check` 通过；`npm run build:mp-weixin` 构建成功；`npm run check:subpackages` 通过（主包 563,084 B / 最大分包 228,417 B / 总包 1,794,312 B，14 个分包 / 76 页）。
- 已知问题（非本次改动引入）：`npm run check:home` 失败——`verify-homepage-contract.mjs` 期望 BOSS 首页六项文案为 `客户管理|设计调度|方案报价|合同签约|履约驾驶舱|财务总览`，而 `roles/catalog.ts` 中已是 `客户经营|方案设计|报价谈单|合同签约|履约驾驶舱|财务总览`（本次改动前即为该状态，本会话未改 BOSS 块）。因此 `verify:mp-weixin` 全量门禁仍红，需产品确认文案后统一。
- acceptance_needed：真机验证项目经理建档 → 财务付款 → 台账选记录生成凭证 → 打开/转发 PDF；设计师与店长登录确认身份证号/银行账号为脱敏值。

## 2026-09-21 凭证模板 L2 配置（后端键对齐 + App 横向配置页）

- 后端：
  - `DocumentTemplateContext` 新增 `VOUCHER_FIELDS` 白名单（`showVoucherNo`/`showVoucherDate`/`showPartyName`/`showWorkerIdCard`/`showWorkerTrade`/`showWorkerPhone`/`showProject`/`showAmount`/`showAmountWords`/`showSignBlock`/`showDetail*`），Receipt 与 Payment 均允许。
  - `DocumentTemplateBaseline` 收款单/付款单基线改为凭证字段键（付款单默认开身份证与工种，收款单默认关）。
  - `VoucherPdfRenderer` 字段键统一为 `showXxx`；表头字段改为成对守卫以保证 PDF 表格列数平衡；字号范围对齐平台 9–14；默认主色改为 `#0E8A7D`。
  - 测试：`DocumentTemplateContextTest` 新增"凭证字段键可保存"与"未知键被拒"两个用例。
- App：
  - `scripts/verify-homepage-contract.mjs`：BOSS 首页文案断言同步为当前代码文案（用户确认）。
  - `src/domains/document/api.ts`：新增 `templateApi.templates()` 与 `saveTemplate()`（JSON 字符串双向转换）。
  - `roles/permission.ts`：新增 `edit:doc-template`（仅 BOSS，与后端 `requireBoss` 一致）。
  - 新增 `subpackages/finance/pages/voucher-template.vue`：**横向**双栏模板配置页（左：类型切换/凭证名称/抬头与字段勾选/字号/主色/保存；右：凭证实时预览），`pages.json` 配 `pageOrientation: landscape`。
  - `subpackages/finance/pages/vouchers.vue`：新增"模板配置"入口。
  - `config/index.ts` + `pages.json`：新增 `financeVoucherTemplate` 路由常量与页面登记。
- 验证：
  - 后端 `mvn -o test`：**203 tests / 0 failures / 0 errors**。
  - App `npm run verify:mp-weixin`：**全绿**（来源契约 / onboarding / 单据 / 计划 / 首页 / dev-api / type-check / build / 分包），主包 564,032 B、最大分包 229,433 B、总包 1,810,945 B，14 分包 / 77 页。
- 风险：模板保存仅 BOSS；店长与财务在模板页为只读预览（与后端一致，如产品要求放开需改后端 `requireBoss`）。横向页在真机的旋转与滚动需真机确认。
- acceptance_needed：真机横向验证字段勾选与预览一致性；BOSS 保存后重出凭证 PDF 确认字段生效与版本自增。

## 2026-09-21 App 补齐：模板权限放开 + 工资付款关联工人

- 依据：用户确认"店长、财务也能改模板"，并要求把 App 剩余功能做完。
- 后端：
  - `DocumentTemplateContext.save` 权限由 `requireBoss` 放开为 `requireRole(Boss, StoreManager, Finance)`；同步修正 `DocumentTemplateContextTest` 打桩。
  - `finance/payment/save` 新增 `workerId`：按企业校验工人档案后写入 `Payment.worker`；收款人姓名优先级为"显式传入 > 工人姓名 > 应付收款方"；单张与多应付分摊两条路径均支持；响应白名单新增 `worker`。
  - 测试：`FinanceClosedLoopTest` 新增"工资付款关联工人并以工人姓名作为收款人"用例。
- App：
  - `roles/permission.ts`：`edit:doc-template` 追加给店长与财务。
  - `subpackages/finance/pages/entry.vue`：付款登记在"工资类应付"时显示工人选择器，未选工人禁止提交；提交携带 `workerId`。
  - `domains/finance/api.ts`：`createPayment` 支持 `workerId`。
  - `pages/mine/index.vue`：按 `view:worker` / `view:finance` 增加"工人档案""收付凭证"入口（覆盖 BOSS、店长、财务、设计师），避免改动 BOSS 首页固定六项。
- 验证：
  - 后端 `mvn -o test`：**204 tests / 0 failures / 0 errors**。
  - App `npm run verify:mp-weixin`：**全绿**（含 build 与分包检查），主包 564,398 B、最大分包 229,433 B、总包 1,812,739 B，14 分包 / 77 页。
- 风险：`edit:doc-template` 前端放开范围与后端一致；模板保存仍是"覆盖式版本自增"，无审批流。
- acceptance_needed：店长/财务登录改模板并保存；工资付款选择工人后生成凭证，确认身份证号/工种/到账账户带入。

## 2026-09-21 工人档案页查询框交互修复

- 问题：工人档案页（企业负责人查看）查询框直接套用 `.fin-head-actions`（按钮用 flex 行），缺少搜索图标与胶囊容器，只能回车搜索、无清空入口，且"无结果"与"无数据"共用同一空态文案。
- 修复（`subpackages/labor/pages/workers.vue`）：
  - 查询框改为 `customer/list.vue` 的搜索规范：胶囊容器 + 搜索图标 + `placeholder-class`。
  - 输入即搜（320ms 防抖）+ `confirm-type="search"` 立即搜索 + 有词时显示清空按钮。
  - 新增请求序号守卫，防止防抖/下拉刷新/onShow 并发时旧响应覆盖新结果。
  - 空态区分：有关键词为"没有匹配的工人"，无关键词为"暂无工人档案"。
- 验证：`npm run verify:mp-weixin` 全绿（含 build 与分包检查）；主包 564,398 B、最大分包 229,540 B、总包 1,814,462 B，14 分包 / 77 页。

## 2026-09-21 新页面交互复查（收付凭证 / 工人编辑）

- 收付凭证页：原本**没有搜索能力**，凭证一多只能翻。补齐：
  - 后端 `document/voucher/page` 新增 `keyword`，匹配凭证号、对方名称、工人姓名与项目名称。
  - App 按既有搜索规范加搜索框（胶囊 + 图标 + 占位色）、320ms 防抖、回车即时搜、清空按钮、请求序号守卫，并区分"没有匹配的凭证"与"暂无凭证"两种空态。
  - 测试：`VoucherBuildAcceptanceTest` 新增关键词过滤用例。
- 工人编辑页：原本用 `laborApi.workers()` 拉全量列表再 `find` 出单条——**工人超过一页（50 条）后编辑页会报"档案不存在"**。修复：
  - 后端新增 `labor/worker/info`（按主键 + 企业单条加载，返回前按角色裁剪）。
  - App 改用 `laborApi.worker(id)` 单条读取。
  - 防御：接口返回脱敏值（含 `*`）时表单对应字段留空，避免把掩码当真实值回写。
  - 测试：新增 `WorkerInfoAcceptanceTest`（同企业设计师脱敏 + 跨企业 404）。
- 验证：后端 `mvn -o test` = **207 tests / 0 failures / 0 errors**；App `npm run verify:mp-weixin` 全绿（主包 564,482 B、最大分包 229,540 B、总包 1,816,578 B，14 分包 / 77 页）。
- 路由总数：261 → **262**（public 12 + protected 250），`ProviderSecurityAcceptanceTest` 已同步并补 `labor/worker/info` 未授权断言。

## 2026-09-21 内置常用凭证模板库

- 需求：凭证模板基线要内置企业常用凭证模板，可直接套用。
- 后端：
  - `DocumentTemplateBaseline` 重构为"基线 + 常用凭证模板库"：新增 6 条凭证预设（`P-SALARY` 工资发放凭证、`P-LABOR` 班组结算凭证、`P-SUPPLIER` 材料付款凭证、`P-EXPENSE` 费用报销付款单、`R-STANDARD` 客户收款收据、`R-DEPOSIT` 定金收据）；基线版本 1 → 2；`template(type)` 取该类型第一个预设，报价/材料/合同仍走原行业基线。
  - `DocumentTemplate` 新增瞬态 `presetCode` / `presetNote`。
  - 新增只读 Provider `document/baseline/list`。
  - 测试：新增 `DocumentTemplateBaselineTest`（预设数量/编码/字段差异）；`DocumentTemplateContextTest` 新增"逐条保存全部内置预设不违反白名单"用例。
- App：
  - `domains/document/api.ts`：新增 `templateApi.presets()` 与预设字段。
  - `subpackages/finance/pages/voucher-template.vue`：新增"常用模板"横向选择条（按类型过滤）+ 适用说明，点选即套用名称、字段与样式，保存后才形成企业模板。
- 验证：后端 `mvn -o test` = **214 tests / 0 failures / 0 errors**；App `npm run verify:mp-weixin` 全绿（主包 564,703 B、最大分包 229,671 B、总包 1,818,688 B，14 分包 / 77 页）。
- 路由总数：262 → **263**（public 12 + protected 251）。

## 2026-09-21 修复：凭证模板保存 Data too long for column 'field_config'

- 现象：App 横向模板配置页保存企业模板报 `Data truncation: Data too long for column 'field_config' at row 1`。
- 根因（ECD-009）：`db_document_template.field_config/style_config` 在开发库由 Hibernate `ddl-auto: update` 早期建为 `varchar(255)`；`update` 只加列不改列类型，实体后来改成 `@Lob` 也不会纠正既有列。迁移 `41-单据模板与PDF归档.sql` 已声明 `longtext`，但该库从未执行迁移。
- 影响面：**任何凭证模板保存都会超长**，非预设特有——`normalizeFields` 会把白名单字段全部补齐，落库 JSON 约 500 字符（旧 8 字段基线补齐后同样 513 字符），`varchar(255)` 从来放不下。此前无人保存过收款/付款模板，因此一直未暴露。
- 修复：
  - 新增迁移 `docs/design/migrations/45-单据模板JSON列扩容.sql`：`alter table db_document_template modify column field_config/style_config longtext not null;`（可重复执行）。
  - 登记 `ECD-009`（现象、触发链、为什么没拦住、禁则、必须写法、检测方法），并把禁则同步写入 `rules.md` 与 `AGENTS.md`。
- 待执行：开发库需手动执行迁移 45（`ddl-auto: update` 不会自动修正）；生产库若尚未建表，直接执行迁移 41 即为 longtext。
- 验证：`show create table db_document_template;` 应显示两列为 `longtext`；随后在 App 保存凭证模板应成功且版本自增。

## 2026-09-21 修复：凭证查看 downloadFile 报 url not in domain list

- 现象：App 收付凭证页点"查看 / 打印"报 `downloadFile:fail url not in domain list`。
- 根因：后端 `FileSignatureContext.gateway()` 签发的是**相对路径** `/file/private/download?path=...&expires=...&signature=...`。
  - 报价链路在 `domains/document/api.ts` 的 `normalize()` 里用本地 `absoluteUrl()` 补成了绝对地址，所以正常；
  - 本次新增的凭证链路在 `domains/finance/api.ts` 的 `normalizeVoucher()` 里直接透传 `raw.url`，把相对路径交给了 `uni.downloadFile`，微信判定为非法域名。
- 修复：
  - `core/http.ts` 新增并导出共享 `absoluteUrl()`（相对地址补 `runtime.apiBase`，已是 http(s) 的原样返回）。
  - `finance/api.ts` 的 `normalizeVoucher()` 用共享辅助绝对化凭证 `url`。
  - `document/api.ts` 删除重复的本地实现，改用共享辅助（行为不变，消除两套实现）。
- 验证：App `npm run type-check` 与 `npm run verify:mp-weixin` 全绿（主包 564,741 B、最大分包 232,861 B、总包 1,821,916 B）。
- 提醒：修复后下载地址变为 `http://<apiBase>/file/private/download?...`；开发者工具需开启"不校验合法域名"，生产需在小程序后台把 `decoboon.joytsing.com` 配入 **downloadFile 合法域名**，否则仍会报同样的错。

## 2026-09-21 凭证列表多维筛选 + 模板列表/详情 + 使用引导

- 需求：凭证列表过于简单、凭证变多后无法查找；要增加项目/月份/年份/日期维度；凭证模板要有列表与详情页；要加使用引导；交互参考报价谈单页。
- 后端：`document/voucher/page` 新增 `type` 筛选（Receipt/Payment）；测试补"按类型筛选"用例。
- App：
  - `vouchers.vue` 重构：
    - 首次使用引导卡（三步 + "去资金台账出凭证"/"看常用模板"两个入口），仅在无数据且无筛选时出现。
    - 汇总卡：筛选结果的合计金额、张数、收款/付款分列。
    - 维度筛选：关键词搜索（防抖 + 清空 + 请求序号守卫）、类型（全部/收款/付款）、时间（全部/本月/上月/今年/选月份）、项目（picker）；有筛选时出现"重置"。
    - 列表按**月份分组**，组头显示当月张数与合计。
    - 空态区分"没有匹配的凭证"（带筛选）与"暂无凭证"。
  - 新增 `voucher-templates.vue`（模板列表）：使用引导、付款类/收款类常用模板、我的模板（仅已落库企业模板，显示企业版本号）。
  - `voucher-template.vue` 改造为**模板详情/编辑页**：支持 `?preset=CODE`（从常用模板进入，套用后可改名保存）与 `?type=Receipt|Payment`（编辑企业模板）；移除页内模板选择条（已上移到列表页）。
  - 复用全局 `jz-toolbar` 作为筛选 chips，与报价谈单页保持同一视觉语言。
  - `config/index.ts` + `pages.json`：新增 `financeVoucherTemplates` 路由常量与页面登记；`financeApi.vouchers()` 支持 `type/startDate/endDate`。
- 验证：后端 `mvn -o test` = **215 tests / 0 failures / 0 errors**；App `npm run verify:mp-weixin` 全绿（主包 564,900 B、最大分包 232,861 B、总包 1,845,923 B，14 分包 / 78 页）。

## 2026-09-21 凭证列表筛选记忆

- 需求：财务常按"本月 + 某项目"看账，每次进入都回到"全部"太费事，需要记住上次筛选。
- 实现（`subpackages/finance/pages/vouchers.vue`）：
  - 本地存储键 `voucher_list_filter`，记忆**类型 / 时间维度 / 自定义年月 / 项目**四项维度。
  - **不记关键词**：旧的搜索词会在重进时把列表清空，容易误判为"没有数据"。
  - `onLoad` 恢复，维度变更（选类型/时间/年月/项目/重置）时立即保存。
  - 缓存健壮性：逐项校验枚举值，缓存被改坏时退回默认而不是出现"没有任何 chip 选中"；`period=custom` 但月份为空时退回"全部"。
  - 项目有效性：项目列表加载后，若记忆的项目已被删除或超出数据范围，自动回退"全部项目"并同步保存。
  - 汇总卡文案随筛选状态切换（"筛选结果" / "全部凭证"），有筛选时保留"重置"入口作为明确退出点。
- 验证：App `npm run type-check`、`npm run verify:mp-weixin` 全绿（主包 564,900 B、最大分包 232,861 B、总包 1,846,597 B，14 分包 / 78 页）。
- 说明：本轮只改 App，未触后端，故未重跑后端测试（上次为 215 tests / 0 failures）。

## 2026-09-21 修复：凭证多维筛选的交互与布局

- 问题（用户反馈）：
  1. 类型、时间、项目等维度被塞进同一行横向滚动条，屏幕窄时看不全、也看不清有几组维度。
  2. 项目筛选用系统滚轮 picker，装企常有几十个项目，滚轮找项目极不友好。
- 方案（参考报价谈单页的分区 + 列表选择思路）：
  - 维度改为**每行一个维度**的筛选卡片：`类型` / `时间` / `项目` 各占一行，左侧固定小标签，右侧 chips **自动换行**（不再横向滚动），全部选项一眼可见。
  - 自定义年月 chip 放入时间维度的 chips 容器内，随其他 chip 一起换行，避免挤出屏幕。
  - **项目筛选改为可搜索弹层**（复用 `JzSheet`）：顶部搜索框（项目名/编号/客户/小区即时过滤）、首行"全部项目"、列表项显示 `项目名 + 编号·客户·小区`、当前项打勾；弹层内列表可滚动。
  - 筛选卡底部保留"重置筛选"，仅在有筛选时出现。
- 验证：App `npm run type-check`、`npm run verify:mp-weixin` 全绿（主包 564,900 B、最大分包 236,239 B、总包 1,850,850 B，14 分包 / 78 页）。
- 说明：纯 App 改动，未触后端；同时产出可交互原型 `voucher-filter-redesign.html` 供评审（列表态 + 项目弹层态并排）。

## 2026-09-21 凭证筛选增强：滚动时间窗 + 项目定位分层

- 用户反馈：项目筛选仍不够快（装企项目多）；时间维度需要近 3/7/15/30 天。
- 时间维度：`periodOptions` 增加 `近3天/近7天/近15天/近30天`（`d3/d7/d15/d30`），区间按"含今天往前推 N 天"计算；与原有 全部/本月/上月/今年/选月份 并存，chips 自动换行；筛选记忆同步支持新枚举值。
- 项目弹层定位分层：
  - 新增 **状态** 快速筛选：全部 / 在跑（待开工+施工中+验收中）/ 已完工（已完工+已结算）/ 暂停，chip 上带数量。
  - 新增 **设计师** 快速筛选：从已加载项目里去重出真实有项目的设计师，避免空选项。
  - 三维叠加：状态 + 设计师 + 关键词；显示"共 N 个项目，当前筛选出 M 个"。
  - 排序：**在跑优先**，其次按创建时间倒序，符合"先找在做的项目"的实际习惯。
  - 项目行加状态标签（施工中/验收中/待开工/已完工等），meta 改为 `编号 · 客户 · 设计师`（原为小区，改为更能区分项目的信息）。
  - 每次打开弹层重置定位条件，避免上次的状态/设计师残留造成"项目不见了"。
- 验证：App `npm run type-check`、`npm run verify:mp-weixin` 全绿（主包 564,900 B、最大分包 240,276 B、总包 1,854,887 B，14 分包 / 78 页）；原型同步更新为列表态 + 项目弹层态并排。

## 2026-09-21 修复：voucher-filter-redesign.html 可视化超时

- 现象：内联可视化报 `TimeoutError: MCP sandbox RPC timed out`。
- 诊断（本地 Chrome 无头实测）：
  - 文件结构本身正常（div/span 标签平衡、无 script/外链/动画），能渲染。
  - 根因是**画布过高**：两个手机框各 `min-height: 620px`，窄宽下 `flex-wrap` 竖排堆叠，文档高度达 **1257px**（旧版 15KB / 109 节点），沙箱栅格化超预算。
- 修复：
  - 移除全部 `min-height` 与大尺寸手机外壳包裹，只保留**筛选区本体**（本可视化的目的就是看筛选布局，不必画完整凭证卡片）。
  - 去掉 `light-dark()`（8 处）改为固定浅色值——App 界面本身是浅色，固定主题更贴合语义也少一层求值。
  - 精简到 8.5KB / 45 个 div / 90 个节点，CSS 规则收敛为单块。
- 复核（本地 Chrome 无头实测）：
  - 736px 宽：两栏并排，根元素高 **437px**，无横向溢出；
  - 500px 宽：两栏竖排，根元素高 **862px**（原 1257px），无横向溢出；
  - 沙箱包装器 `render.py` 可正常包裹渲染。

## 2026-09-21 项目选择改服务端检索 + 常用项目

- 需求：项目选择器不再一次性拉全量在前端过滤，改为服务端检索；并增加"常用项目"。
- 后端：
  - `project/project/page` 新增 `statuses`（状态集合，用于"在跑/已完工"分组）与 `designerId`（设计师）两个筛选参数；原 `status`/`keyword`/`customerId` 保持不变（均为可空，向后兼容）。
  - 新增只读 Provider `document/frequent-project/list`（`List<Project>`）：口径为**近 90 天内按当前员工数据范围出凭证最多的项目**，最多 8 个；90 天内无凭证时回退"最近出过凭证的项目"，仍为空则返回空列表（不编造常用项目，前端据此隐藏该分组）。
  - 数据范围复用 `VoucherRecord` 上的 `@DataPermission`（BOSS/财务按企业，其余按 `DataScopeSpecification`），因此常用项目天然不会越权。
  - 测试：`FinanceClosedLoopTest` 新增"常用项目按凭证频次返回 + 项目分页服务端筛选（关键词/状态集合/设计师）"用例。
- App：
  - `projectApi.page` 支持 `keyword/statuses/designerId`，服务端分页返回。
  - `financeApi.frequentProjects()` 接入常用项目。
  - 项目弹层改为**服务端检索**：搜索（320ms 防抖）、状态分组、设计师（来源 `user/designer/list`）、`scrolltolower` 加载更多、加载/失败/重试态；移除"拉 200 条在前端过滤"的实现。
  - 弹层顶部新增"常用"分组（仅未输入关键词且未加状态/设计师筛选时展示），点选即用；筛选记忆同时保存 `projectId` 与 `projectName`，重启后筛选卡仍显示项目名。
- 验证：App `npm run type-check` 与 `npm run verify:mp-weixin` 全绿（主包 587,405 B、最大分包 241,523 B、总包 1,943,128 B，14 分包 / 78 页）；后端相关用例单独运行 **27 tests / 0 failures / 0 errors**。
- 注意：全量 `mvn test` 当前不稳定——同一工作区有其它 lane 在并发编译，`target/` 被互相覆盖，表现为随机的 `ApplicationContext failure threshold` / `NoClassDefFoundError`（同一测试类单独运行可通过）；另有 `CustomerOverviewContextTest` 的失败来自其它 lane 的在建功能，非本次改动。

## 2026-09-22 量房经营 P0：预约量房转化与经营工作台

- 用户确认：房屋级 `Measure.scheduledAt`；P0 保持客户级设计责任人；量房完成必填实际时间+现场照片；预约超过 4h 未完成标逾期；完成超过 48h 未进入设计标待交设计；获客成本不进入本轮。
- 后端数据：`Measure` 新增 `scheduledAt`、`completedAt`，`measureTime` 保留兼容；新增迁移 `docs/design/migrations/48-量房预约完成时间.sql` 并支持历史回填和重复执行。
- 写入链路：`property/measure/save` 完成量房时保留预约时间、写实际完成时间，重复完成仍幂等；`customer/customer/measure` 同步写房屋级预约时间。
- 经营聚合：新增 `statistic/boss/measure/report`，返回预约量房数/率、完成数/率、4h 逾期、48h 待交设计、业务员分组、客户来源分组和设计师执行分组。
- App：新增 `subpackages/customer/pages/measure-workbench.vue`，BOSS/店长/团队负责人/业务员入口；展示业务员预约转化、来源转化、风险项目、下一步、责任人和量房记录下钻。
- 测试：新增 `BossMeasureReportContextTest`（预约转化聚合）与 `MeasureScheduleFactTest`（预约/完成时间分离、幂等）；Provider 安全路由总数 278 → 279。
- 验证：
  - 后端全量：78 suites / 236 tests / 0 failures / 0 errors / 0 skipped。
  - App：`npm run build:mp-weixin:release` PASS；主包 601924 B，最大分包 302211 B，总包 2034309 B，14 分包 / 87 页。
- 迁移验证：隔离 MySQL 8.4 重复执行迁移 48，Scheduled/Cancelled 回填 `scheduled_at`，Completed 回填 `completed_at` 并关联最近 Scheduled 计划时间；修复 MySQL 1093 同表更新问题。
- 未完成：迁移 48 目标库执行、真实微信账号/真机验收；获客成本作为后续独立需求，不纳入 P0。
- 状态：`implementation_done_self_tested_pending_acceptance`。

## 2026-09-22 设计师方案设计工作台：原型梳理 + 交互逻辑整合定稿

  - 六条规范冲突 C1–C6：**C1 管理费 8%/税 3.41% 硬编码**（L343-344/L640）违反 AGENTS 金额口径；C2 `Function()` 求值（L582）；C3 缺报价状态机（仅 `quoteConfirmed` 布尔）；C4 阶段/风险硬编码；C5 无 costPrice（**符合**设计师不可见成本，但需明确为销售价拆分）；C6 缺成本完整性提示。
  - 三条实现缺陷 D1–D3：**D1 L626-629 输入事件触发 `renderQuoteTbody()` 整表重建 innerHTML → 输一个字符即失焦**，连续录入不可用；D2 缺全屋漏项核对；D3 确认方案与生成报价语义边界未说明。
  - **阶段派生谓词表**（8 阶段，服务端唯一口径，前端不得推导）+ `BR-005` 阈值同源。
  - **三段式联动契约**：6 条动作 → 三栏变化；核心要求"任何一栏写入后另两栏必须说实话"；生成报价后卡片离开本工作台。
  - **队列逻辑**：排序改分层（①今天有预约 ②高风险 ③停留天数 ④预约日 ⑤金额/意向），理由是"时间刚性、风险滞后"；卡片关键行与 hover 快捷动作**随阶段变化**；批量仅允许同阶段低风险动作（禁止批量确认方案/生成报价）。
  - **详情逻辑**：默认 Tab 跟随阶段；下一步卡必须带原因；轨迹只读。
  - **报价器逻辑**：表达式手感（实时求值/非法保留原文/回车下一行/Excel 两列粘贴/保存表达式原文+结果）；**补全屋漏项核对**；费率必须企业配置；门禁四态文案。
  - **状态同步表**、异常边界（含并发版本冲突禁止静默覆盖）、键盘效率（明确禁止重建 DOM 致失焦）、权限可见性。
  - 登记 `GAP-WB-006~009`（待绘图是否独立、费率口径、确认是否可撤回、面积变更是否自动重算）。
- 未做（用户明确暂缓）：原型 D1/C1 修复；工作台视觉设计；实现。
- 状态：`interaction_logic_frozen_pending_user_confirm`。

## 2026-09-22 设计师方案设计工作台界面设计 v5（按交互逻辑落地）

- 定位：**按已冻结的《交互逻辑》逐条落成界面**，不是照抄 v4 视觉。与 v4 的差异即为交互逻辑的落地。
- 落地的交互逻辑（与文件内实现一一对应）：
  1. **分层排序**（今天有预约 › 高风险 › 停留最久），注释写明"服务端已排好序，前端不再排"。
  2. **卡片关键行随阶段变化**（keyline）：待量房="今天 15:00 现场量房"、待方案="已录 3 个区域 · 缺厨房分项"、待绘图="缺 CAD · 已等待 3 天"。
  3. **hover 快捷动作随阶段匹配，第一个为主动作**（quick 数组，待预约=打电话、待录量房=录量房资料、待报价=生成报价）。
  4. **默认 Tab 跟随阶段**（默认 Tab 上打「默认」角标）：待录量房→量房资料、待绘图/待方案/待确认→方案与CAD、待报价→报价历史。
  5. **全屋漏项核对**（新增「全屋核对」tab）：区域 × 常用分项矩阵，空区域整行标红并汇总"会被少算钱"的区域名。
  6. **状态同步**：确认方案后同时更新队列阶段/关键行/快捷动作/门禁（不再是死队列）；生成报价后该客户**移出队列**。
  7. **禁止批量确认/生成报价**：确认走 `confirm()` 二次确认并说明"版本冻结、不可撤回"。
  8. **门禁四态可解释**：无方案 / 有无效算式（报行数）/ 未确认（报缺几个区域）/ 已确认可用。
- 技术修正（写进新设计，未改动 v4）：
  - **工程量输入不再重建行**：改为只更新该行 `eq` 与合价单元格，**输入焦点保持**（修复 v4 D1）。
  - **管理费/税金来自 `CONFIG` 并标注「⚙ 企业配置」**，UI 上显式声明非前端写死（对应 C1）。
  - **求值换为自写四则运算解析器**，全文无 `Function`/`eval`（对应 C2）；冒烟验证注入串 `1;alert(1)` 返回不合法。
  - 报价表仅销售价拆分（材料+人工），**无成本价**（符合设计师不可见成本）。
- 自检：`node --check` 语法通过；纯逻辑冒烟通过——客户 7 户、阶段计数合计 15、`3.6*2.8-1.2=8.88`、`(6.2+4.6)*2*2.9-2.4=60.24`、非法式与注入串均拒绝、陈先生全屋直接费 11907.49 / 含费税 13298.62、无分项区域正确识别为厨房+卫生间。
- 排障记录：首版 JS 语法错（id:3 对象少一个闭合大括号）已定位并修复。
- 边界：这是**原型**，未接后端；`stage/riskLevel/stayDays/threshold/todoCount/defaultTab/quick` 真实实现必须由服务端下发（BR-005）。
- 状态：`ui_design_v5_done_pending_user_review`。

## 2026-09-22 设计师方案设计工作台 v6（事件处置台重构，P0+P1 落地）

- 前置说明：上一轮我只输出了"做 v6"的文字、**未实际产出任何文件**，属空承诺；本轮补齐。
- 按评判建议落地的改造：
  - **S1（P0）右栏由常驻报价器改为「处置面」，内容随 8 个阶段变**：待熟悉→诉求/标签/跟进；待预约→联系+预约；待量房→现场准备清单；待录量房→照片+描述；待绘图→CAD+结构面积；待方案/待确认→报价器；待报价→生成报价。并新增**算量台全宽模式**（收起左右栏，左侧 CAD+结构面积、中间分项表、右侧区域+汇总+门禁）。
  - **S2（P0）队列由平铺列表改为「按处置类型聚合的事件流」**：7 个分组（今天现场量房 / 超阈值未约量房 / 方案待确认超 3 天 / 缺 CAD / 缺量房资料 / 待报价 / 待熟悉），每组显示户数、聚合原因、"其中 N 户已超阈值"，可展开收敛到客户卡；组级提供批量加跟进与整组展开。
  - **S3（P1）时间轴提为脊柱**：中栏拆为「常驻纵向时间轴（132px）+ 当前阶段工作面」，时间轴不再藏在 Tab 里。
  - **S6（P1）顶部加目标视角**：本月签约 3/5 单 · 还差 2 单 + 进度条。
  - **S4/S5（P2）一并做**：卡片「· 新」标记 + 顶部「自上次查看以来 4 条变化」（点击展开含变化的分组）；键盘 `J/K` 切换事件、`1/2/3` 触发该阶段快捷动作、`Esc` 退出算量台，底部常驻帮助条。
- 保留的 v5 资产（未丢）：阶段派生 + 停留/阈值双显、关键行随阶段变、快捷动作第一个即主动作、门禁四态可解释、全屋漏项核对。
- 技术修正延续：工程量输入**局部更新不失焦**；费率 `CONFIG` 并标注「⚙ 企业配置」；自写四则运算解析器，全文无 `Function`/`eval`；报价表仅销售价拆分无成本价。
- 新增 `#console` 深链，可直接进入算量台状态，便于评审与分享。
- 自检：`node --check` 通过；`getElementById` 静态目标无缺失；纯逻辑冒烟通过（12 户 / 7 组、客户 group 全部命中分组、阶段计数合计 15、`8.88`、`60.24`、非法式与注入串 `1;alert(1)` 均拒绝、陈先生直接费 11907.49、无分项区域厨房+卫生间）。
- 边界：仍是**原型**，未接后端；`stage/prio/stay/threshold/todo/keyline/quick/defaultTab/group` 及**聚合分组规则**真实实现必须由服务端下发（BR-005）。
- 状态：`v6_done_pending_user_review`。

## 2026-09-22 生产后端监控（35 分钟窗口）

- 执行 `bash scripts/prod-monitor.sh collect "35 minutes ago"`，退出码 `20`；按安全门禁只归档证据，未修改业务源码、未重启、未构建、未发版。
- 生产服务 active，HTTPS health 200，根盘 18%，内存正常；当前版本 `/opt/decoboon/releases/20260921-050519`，`journalctl` 窗口无条目。
- 窗口内唯一应用日志是监控自身健康检查产生的 expected `tokenIsUndefined`；非预期异常最近为 `10:27:23`，在窗口外。主要为 `.env`/`.git/config`/`mcp`/phpunit 等扫描探测、`auth/signin` 未知入口和授权过期业务结果。
- registry 数据完整性仍异常：历史 fixed 懒加载事故被改写为 `INC-20260922-a1233597b0/open`，但保留原 fixed 元数据；当前 49 条记录，需人工修 `prod-monitor-analyze.py` 的 stable ID/状态/计数逻辑并补回归测试。
- 证据与建议：`docs/operations/incidents/2026-09-22/20260922-130146/triage.md`。本次无新高置信代码缺陷，不进入修复发版。

## 2026-09-22 生产数据变更：账号退役（18105692379）

- 用户诉求：注销线上 `18105692379`，让该手机号可通过微信重新注册。
- 只读侦察结论：该账号**不是**误注册空壳，而是「装饰」企业的 **Boss**（员工「麦鸿」，emp_id 1789989046754654238），当天 16:25 仍登录过；企业数据量 员工1/客户0/项目0。用户确认企业可弃用、账号不再需要登录。
- 三个关键分析（决定了不能用"注销"这个动作）：
  1. **`status = Deleted/Disabled` 不解锁手机号**——`UserIdentityContext.identify()` 按手机号查到凭证后，`status != Active` 直接抛 403「当前用户已停用」；`type == Wx` 抛 400「该手机号已绑定其他微信」。查询不过滤状态。
  2. **物理删凭证会被外键挡住**——7 张表引用 `db_tenant_credentials`，该账号已有 1 条 `db_company_join_limit` 引用；且 `db_tenant_company` 被 80 张表引用，企业级删除不可手写 SQL。
  3. **只清 phone 会被自动重绑**——旧微信再登录时命中 openId，`phone == null` 会重新写回手机号。
- 执行的方案（最小安全动作，避开 80 张表）：单行 UPDATE 退役凭证——`phone = NULL`、`id_key = CONCAT('retired:', cre_id)`、`id_secret = NULL`、`status = 'Deleted'`。
- 前置保护：**全库备份** `/opt/decoboon/backups/pre-retire-18105692379-20260922-162959.sql`（290K / 122 表 / Dump completed），执行前记录该行完整原值，并提供回滚 SQL。
- 验证：手机号占用 **0 条**、旧 openId 命中 **0 条**、该行已置 Deleted。
- 残留（已登记）：空企业「装饰」+ Boss 员工未删（不动 80 张表）；`db_company_join_limit` 仍持有该手机号，**仅影响"接受他人企业邀请"路径**（30 天冷静期），**不影响重新注册新企业**（register 不调 validateJoinLimit）。
- **发现产品缺陷（建议登记）**：`UserIdentityContext.identify()` 手机号占用判断不过滤状态，导致 `Disabled`/`Deleted` 账号的手机号**永久无法重新注册**，无自助恢复路径，只能运维改库。本次生产变更即由此坑触发。建议手机号查询排除 `Deleted`，或为 `Deleted` 提供重新注册分支。
- 记录：`docs/operations/data-changes/2026-09-22-账号退役-18105692379.md`
- 状态：`production_data_change_done_verified`。

## 2026-09-22 企业负责人增减项工作台（development）

- 批次 `REQ-20260922-BOSS-CONTRACT-CHANGE-WORKBENCH`（feature_iteration）：把合同变更从"单项目列表"升级为企业负责人经营工作台，只读聚合在途审批、待客户确认、客户已确认后的材料下单与变更款应收、合同金额与预计毛利投影；审批/客户确认复用既有 Provider，不改状态机与金额口径。
- 后端新增 `statistic/boss/contract-change/report`（`BossContractChangeQuery/Report/Group/Item/Period` + `BossContractChangeContext`），在同一 Specification 内应用 `ProjectAccessContext.visibleChild` 数据范围，成本按 `CatalogCostContext` 角色裁剪；材料计划/应收按项目集合批量取，避免 N+1 主体关联。
- App 新增 `change-workbench.vue`：4 张经营指标、异常条（审批超48h/确认超72h/材料未下单/变更款逾期/成本未完整）、日期窗口、合同当前额→在途→预计合同额、项目分组与变更单卡片、BOSS 审批与客户确认动作；项目经营页新增入口。
- 验证：`mvn -q -DskipTests compile` PASS；`mvn test -Dtest=ProviderSecurityAcceptanceTest` PASS（3/0/0，BUILD SUCCESS，验证 Spring 上下文/新路由/仓储派生查询）；App `type-check`、`build:mp-weixin`、`check:subpackages`（89 页）PASS。执行中一度受并发会话清理 `target/classes` 随机缺类影响（ECD-010 同源），并发构建结束后重跑通过。
- 发现并修复自身缺陷：`CustomerConfirmed.name().toLowerCase()` 会得到 `customerconfirmed`，已改为显式状态映射，避免客户已确认过滤/分组失效。
- 并发基线：检测到另一会话移除 `EmployeeRole.ProjectSupport` 并同步调整 `CatalogCostContext`（成本可见=Boss/StoreManager/Finance），本工作台已按当前枚举适配；`ProviderSecurityAcceptanceTest` 路由基线 282→283 已同步，需 acceptance 复核。
- 交接：`_系统/协作/lanes/development/workspace/boss-contract-change-workbench-handoff.md`；产品稿 `../../前端代码/th4-decoboon-app/docs/product/2026-09-22-企业负责人增减项工作台产品设计.md`。
- 状态：`implementation_done_self_tested_pending_independent_acceptance`。

## 2026-09-22 角色统一：履约专员并入财务

- 需求：装得清员工角色统一为 `Boss / StoreManager / Designer / Marketer / Pm / Finance / TeamLeader` 七类；原履约专员职责并入财务，不再保留独立材料员/履约专员岗位。
- 后端：`EmployeeRole` 移除 `ProjectSupport`，`Finance` 数据范围保持 `Company`；报价项目库、成本、材料计划、主材选材、采购下单/审批、库存、供应商、项目/动态查询和工地工作台统一按财务承接履约协同权限。
- 数据迁移：新增 `docs/design/migrations/50-合并履约专员与财务角色.sql`，先将 `db_tenant_employee.role`、`db_tenant_invite.role`、`db_project_report.author_role` 的 `ProjectSupport` 回填为 `Finance`，再收紧枚举；空库基线同步为七类角色。上线必须先执行迁移再启动新后端。
- App：身份类型、角色元信息、Tab、工作台动作和权限矩阵移除 `projectSupport`，财务继承履约项目、供应商协作、材料、整改和动态能力；邀请/成员调整只提供七类岗位，并兼容旧缓存把 `projectSupport` 自动迁移为 `finance`。
- 验证：后端 `mvn -q -DskipTests compile` PASS；`EmployeeRoleContractTest`、`PurchaseCreationPolicyTest`、`OnboardingGuideContextTest`、`DataScopeRequestFilterIntegrationTest` PASS（干净构建后 24 tests / 0 failures / 0 errors）。App `npm run type-check`、`npm run build:mp-weixin`、`npm run check:subpackages` PASS（14 分包 / 89 页）。完整 `verify:mp-weixin` 被既有 BOSS 首页“六项功能域”旧契约阻断（当前在途首页已是七项，与本次角色合并无关）；全量 `mvn test` 受同工作区并发 Maven/IDE 进程与 target 互相覆盖影响，未作为有效证据。
- changed_files：`EmployeeRole.java`、`DataScopeContext.java`、角色相关 Provider/Context、`roles/catalog.ts`、`roles/permission.ts`、身份模型/API/会话、邀请与成员页、迁移 50、基线 01。
- risks：财务合并后拥有原履约协同范围，采购创建按现有策略自动审批；`Project.support` 字段仅保留数据库列名，业务文案已改为“履约负责人（财务）”。
- next_action：目标环境先执行迁移 50，再部署后端与 App；由 acceptance 独立验证历史履约账号、待邀请、动态作者迁移和财务三角色权限。
- acceptance_needed：是。
- 状态：`implementation_done_self_tested_pending_independent_acceptance`。

## 2026-09-23 增减项工作台 App 可发现性修正（development）

- 用户反馈“增减项工作台没有在 App 里看到”。定位：原入口 `v-if="isBoss"` 且位于项目 Tab 4 张指标卡下方；合同页“增减项工作台”仍跳项目内 `change.vue`；非 boss 账号完全不可见。
- 修正：项目 Tab 顶部固定入口并放宽到 boss/storeManager/teamLeader/finance；工作台首页快捷入口新增“增减项”（同上角色）；合同履约档案“变更（增减项）→ 增减项工作台”在经营角色下跳企业级工作台；designer/marketer/pm 继续使用项目内工作台。
- 验证：App `type-check`、`build:mp-weixin`、`check:subpackages` PASS（89 页，总包 2,140,493 B）；dev/build 产物均含 `pages/change-workbench` 与入口。
- 排障提示已写入交接：账号角色、开发者工具指向目录与重新编译、体验版/线上包需重新上传。

## 2026-09-23 新建 th4-decoboon-web 工程 + P0（前端骨架 / 经营总览 / 项目监管）

- 依据：`docs/product/2026-09-23-企业负责人Web端经营管控中心高保真交互设计方案.md` v1.1 与用户 2026-09-23 的 5 项决策。
- 新建工程：`前端代码/th4-decoboon-web`（Vue3 + TS + Vite6 + Element Plus + Pinia + Vue Router，全新工程，非从【备份】复制）。
- 前端路由：`src/router/boss/console.ts` + `src/router/boss/menu.ts`（同源），**不做后端 @Role/@View 注册**；认证沿用 `auth/session/password-login|info|password-logout`。
- 布局：`src/th4/layout/console/ConsoleLayout.vue` + `src/th4/layout/widgets/{topbar/brand,topbar/search,topbar/actions,riskbar/alert,rail/nav}`。
- 组件：`src/components/console/` 13 个（PageHead/MetricCard/PanelCard/SegControl/StatusPill/ProgressMini/CostIncomplete/RiskList/NoticeBar/StateBlock/ConsoleTable/JobTrack/GanttStrip）+ `components/brand/BrandLogo.vue`。
- 页面：`views/boss/statistic/overview/OverviewView.vue`（5 KPI + 财务健康度 + 待处理风险 + 风险详情）、`views/boss/project/supervision/ProjectSupervisionView.vue`（4 KPI + 状态/合同阶段分布 + 重点项目台账 + 排期甘特图）、`views/login`、`views/shared`、`views/boss/shared`（P1/P2 批次占位）。
- 设计令牌：`src/styles/tokens.scss`（结构与间距 1:1 沿用原型，品牌主色替换为 `#16A394`）；品牌 Logo 资源 SHA-256 与 `rules.md` §12.3 校验值一致。
- 数据来源全部为**已存在 Provider**，未新增任何后端指标：`statistic/boss/dashboard`、`finance/summary/report`、`statistic/alert/report`、`project/project/overview`、`collab/notice/count`。
- 验证：`npm run type-check` PASS；`npm run build` PASS（index 62.75 kB / vue 111.99 kB / element-plus 1089.98 kB，已配 manualChunks）。
- 运行验证：本地 mock API（端口 8090）+ vite dev（8091）走通「密码登录 → 经营总览 → 项目监管」全流程；页面文本、KPI、风险条、台账 8 行、甘特 8 行均正确渲染，**修复后控制台无 error/warn**。
- 修复记录：曾用变量名 `console` 承载 Pinia store，命中 Vue 模板编译器全局白名单导致模板解析到 `window.console`，AlertRail 三个 props 全为 undefined（Vue warn）。已重命名为 `consoleStore` 并在文件内加注释说明。
- 边界（本批次不做，已在工程 README 与设计方案登记）：经营总览 5 张 KPI（原型 6 张，「本月签约单数/平均客单」无 Provider 字段）；项目台账展示「毛利润」而非「毛利率」（`Project` 无 `grossMargin` 投影字段）；风险条 4 条（客户超时/报价超时/毛利跌破/投诉/合规无源）；本季本年为禁用态（Provider 仅支持按月）；交付里程碑、账龄、角色产能、协作效率、全局搜索、导出均为 P1/P2；Element Plus 全量引入（按需为 P1 优化）。
- 截图证据：`~/.codex/visualizations/2026/09/22/01a0c9da-fa03-7ee0-b736-c540344eecfa/th4-decoboon-web-p0/{01-overview,02-project-supervision}.jpg`。

## 2026-09-23 补齐 P0 两个缺口：毛利率投影 + 项目「有风险」筛选（后端 + 前端）

- 背景：P0 交付时登记的两个缺口 —— 项目台账缺「毛利率」、项目监管缺「有风险」分段。
- 后端改动（3 个文件）：
  1. `project/model/Project.java`：新增只读投影 `grossMargin`（`@Transient` + `@Unit("%")`），语义同 `grossProfit`——成本不完整时为 `null`。
  2. `project/context/ProjectOverviewContext.java`：`applyProfit` 补算毛利率（`(合同额-实际成本)/合同额`，复用既有 `percentage`；成本不完整时同时置空）；`report(...)` 新增 `riskLevel` 入参与 `parseRiskLevel/isRiskOnly/matchesRiskLevel`，仅作用于列表（指标与分段计数仍为全企业口径）。
  3. `project/service/overview/support/Overview.java`：新增 `@AutoInject riskLevel` 并透传；`Project` 允许输出字段补 `grossMargin`。
- 筛选取值：`risk`/`abnormal` = 有风险（`riskLevel != Normal`）；`Normal`/`Attention`/`High`（或中文名）= 按等级精确筛选；空值/`all` = 不过滤。
- 前端改动：`api/types.ts` 补 `Project.grossMargin`；`api/project.ts` 补 `riskLevel` 查询参数；`ProjectSupervisionView.vue` 新增「毛利率」列（<15% 标红，成本不完整显示 `—`）与「有风险」分段；README 两条边界标记闭环。
- 验证：
  - `ProjectOverviewContextTest` **5/5 PASS**（新增 `shouldFilterByRiskLevelForRiskSegment`；原用例补 `grossMargin` 断言：合同 20 万 / 成本 16 万 → 毛利 4 万 / 毛利率 20.0%；成本不完整时毛利与毛利率同时为 `null`）。
  - `ProviderSecurityAcceptanceTest` **3/3 PASS**（路由数不变）。
  - 前端 `vue-tsc --noEmit` PASS、`vite build` PASS。
  - 端到端（本地 mock API + vite dev）：项目台账出现「毛利率」列；「有风险」分段下 8 个项目收敛为 5 个风险项目，甘特同步收敛；控制台无 error/warn。截图：`~/.codex/visualizations/2026/09/22/01a0c9da-fa03-7ee0-b736-c540344eecfa/th4-decoboon-web-p0/02-project-supervision-risky.jpg`。
- 全量 `mvn -o test` 结果：251 用例，**6 个失败与本改动无关**（`BossMeasureWorkspace2AcceptanceTest` 2 例、`PurchaseDeliveryModeTest` 4 例）。判定依据：两个测试类均不引用 `grossMargin`/`riskLevel`/`ProjectOverviewContext`；且其失败域（量房日期窗口、材料到场确认权限）对应的生产文件 `property/model/Measure.java`、`property/service/measures/**`、`project/service/goodsreceipts/save/support/Save.java`、`ProjectPermissionContext.java`、`MaterialPlan*` 在当前工作区**存在其他 lane 未提交的在途修改**，属并发在途工作导致，非本次改动引入。**建议 development lane 在收口在途改动后复跑这两组用例。**

## 2026-09-23 材料计划 P0 实现（development）

- 依据：`docs/product/2026-09-23-材料计划业务梳理与设计.md`（v1.2，DEC-MP-01～10）；用户已授权开始研发。
- 交付（P0）：
  1. `MaterialPlan` 增 `sourceType/selection/selectedBrand/selectedSpec/selectedModel/materialKey/netQuantity/lossRate/lossQuantity/reduceOfPlan/remark`；状态新增 `ReadyToOrder/PartiallyOrdered/Cancelled`。
  2. 选材客户确认 → 计划行推进 `ReadyToOrder` 并回填选材快照；采购创建前校验已确认选材（DEC-MP-05）。
  3. 新增材料域"材料-供应商供应关系"`MaterialSupplier` + `supply/material-supplier/{page,save,match}`；材料标识只用结构化字段，选材自由文本不参与匹配（DEC-MP-03/08）。
  4. 确认下单即形成应付（`project/purchase/order`），按采购单号与来源单号双重幂等；到场只回填账期起算日/到期日，不再重复建应付（DEC-MP-01）。
  5. 到场确认限定本项目 PM 或设计负责人任一方，企业负责人/财务兜底（DEC-MP-07）。
  6. 新增 `material/plan/workbench` 工作台 Provider（按项目聚合待下单/在途/待到货/逾期，下单与到场进度由采购单实时派生）与 App 页面 `subpackages/project/pages/material-plan-workbench.vue`，并在供应商协作页加入口。
  7. 迁移 `51-材料计划与供应商下单.sql`；空库基线用仓库 `generate-baseline.sh.template` 重新生成。
- 验证：
  - 后端 `mvn -o -DskipTests compile` PASS；
  - `mvn -o test -Dtest='!BossMeasureWorkspace2AcceptanceTest'` → 247 tests / 0 failures / 0 errors PASS；
  - `ProviderSecurityAcceptanceTest` 3/0/0（路由 283→287，protected 271→275）；
  - App `type-check`、`build:mp-weixin`、`check:subpackages` PASS（主包 627,943 B，14 分包 / 90 页）。
- changed_files：见 `_系统/协作/lanes/development/workspace/material-plan-p0-handoff.md` §2。
- risks：迁移 51 须先执行；空库基线整体重生成会纳入其他在途会话此前缺失的表，`db_schema_migration` 标记口径需复核；`PurchaseOrderItem`、扣减退货冲减、主材分类损耗率属下一批（P1）；`BossMeasureWorkspace2AcceptanceTest`（另一会话未跟踪用例）2 条量房断言失败，与本批无关。
- next_action：目标环境执行迁移 51 → 部署 → 由 acceptance lane 按 AC-01～AC-23 独立验收；development 下一批做 `PurchaseOrderItem` + 增减项扣减/退货 + 主材分类损耗率。
- 状态：`implementation_done_self_tested_pending_independent_acceptance`。

## 2026-09-23 材料计划 P1：扣减与退货冲减（development）

- 依据：设计稿 v1.2 §4.2/§12 与 DEC-MP-02（已到货扣减必须走退货单）、DEC-MP-10（下单量不得超过净用量+损耗）。
- 交付：
  1. 新增 `MaterialDeductionContext`，按三段式扣减：①已到货→退货单冲减应付与计划实际成本；②已下单未到货→调整采购单数量金额并按差额下调应付；③未下单→**先冲损耗量再冲净用量**，归零则计划行 `Cancelled`。
  2. 新增 `MaterialReturn` 退货单实体/仓储/状态枚举与 `material/return/page` 查询；迁移 52 + 空库基线同步。
  3. 已付款部分不可直接冲减 → 退货单转 `Pending` 并记录"待协商"说明。
  4. 扣减后按下单进度重算计划行状态（ReadyToOrder / PartiallyOrdered / Ordered / Cancelled）。
  5. 变更履约接入：`Remove` 全额扣减；`Quantity` 减少按差额扣减、增加并入净用量。
  6. App：`materialReturnApi` + 工作台"退货冲减"区（Boss/财务可见）。
- 验证：
  - 后端 `mvn -o test -Dtest='!BossMeasureWorkspace2AcceptanceTest'` → **252 tests / 0 failures / 0 errors**；
  - 新增 `MaterialDeductionContextTest` 5/0/0（先冲损耗、全部冲减取消、已到货退货冲减、已付款转待协商、已下单调整采购单）；
  - `ContractChangeFulfillmentContextTest` 4/0/0（Remove 断言由 Closed 改为 Cancelled + 数量归零）；
  - `ProviderSecurityAcceptanceTest` 3/0/0（路由 287→288，protected 275→276）；
  - App `type-check` / `build:mp-weixin` / `check:subpackages` PASS（主包 628,475 B，90 页）。
- changed_files：见 `_系统/协作/lanes/development/workspace/material-plan-p1-handoff.md` §3。
- risks：迁移 52 须先执行；退货单人工确认/驳回 Provider 未提供（自动置 Confirmed）；已付款差额仅转"待协商"，未实现冲抵其他应付/往来余额；未新增负向扣减计划行（避免遮蔽原计划行，审计由变更单+退货单承载）；入仓退货仍走既有 `stock/supplier-return`，入口待统一。
- next_action：执行迁移 52 → 部署 → acceptance 按 AC-06/07/08/18 独立验收；下一批做退货单人工确认、冲抵其他应付、`PurchaseOrderItem`、主材分类损耗率。
- 状态：`implementation_done_self_tested_pending_independent_acceptance`。

## 2026-09-23 P1：财务经营 + 组织效能（2 个新 Provider + 2 个前端页面）

- 依据：设计方案 v1.1 §8.6/§8.7/§9.2/§16 的 P1 批次与用户 2026-09-23 决策（毛利口径、角色+门店/团队、工长→项目经理交付准时率）。

### 后端（新增 2 个聚合 Provider，共 10 个新文件 + 1 个测试计数更新）
1. **应收应付账龄** `finance/aging/report`（`FinanceAgingContext`）
   - 模型：`FinanceAgingReport` + `AgingBucket` + `FinanceAgingQuery`。
   - 口径：金额取未结清余额（应收=金额-已收、应付=金额-已付）；已收清/已付清/已关闭不参与；`dueDate` 为空的历史数据单独归入「待确认账龄」，不并入正常档（设计方案 §11.2）。
   - 分档：应收 5 档（未到期 / 0-30 / 31-60 / 61-90 / 90+）+ 待确认；应付 4 档（未到期 / 0-30 / 31-60 / 60+）+ 待确认；每档带 `amount / count / level`。
   - 派生：`receivableOverdueRate`、`payableOverdueRate`（逾期档 ÷ 未结清合计，排除未到期与待确认档）。
   - 数据范围：复用 `FinanceAccessContext.requireReader` + `visible(...)`，禁止裸查。
2. **组织效能** `statistic/organization/report`（`OrganizationContext`）
   - 模型：`OrganizationReport` + `RoleCapacity` + `OrganizationQuery`。
   - 口径：在职 = `EmployeeStatus.Active`；本月新增 = `joinedAt` 落在统计月；人均产值 = 本月签约额 ÷ 在职人数；人均单产 = 本月新签项目数 ÷ 在职人数。
   - 角色产能按设计方案 §10.4 决策实现为「角色 + 人数 + 该角色绩效均值」（设计师=报价接受率、业务员=签约率、项目经理=准时交付率），**不新增部门对象**。
   - 三张排行复用 `PerformanceReportContext`（同源，不另立考核规则）；样本 < 3 时返回 `sampleSufficient=false` 供前端提示。
   - 流动率与待优化岗位需人事事件记录，本期不返回。

### 前端（2 个页面 + 2 个组件）
- 新增 `HBarRow.vue`、`RankList.vue`；页面 `FinanceOverviewView.vue`、`OrganizationView.vue`；路由从占位页切到真实页面；`api/console.ts` 新增 `financeAging` / `organizationReport`；`api/types.ts` 补 6 个类型 + `FinanceSummary` 四个应付类型字段。

### 验证
- `FinanceAgingContextTest` **2/2 PASS**（分档边界：正好 30/60/90 天归属；无到期日独立成档；已收清/已关闭排除；逾期率 65.9% / 45.9%）。
- `OrganizationContextTest` **2/2 PASS**（在职 4 人排除离职与停用；本月新增 1 人；人均产值 100000.00；人均单产 0.50；角色产能人数与均值；三张排行按各自指标倒序；topN 生效；样本不足标记）。
- `ProjectOverviewContextTest` **5/5 PASS**、`ProviderSecurityAcceptanceTest` **3/3 PASS**（路由总数更新为 290 = public 12 + protected 278，并补两条负向用例）。
- 前端 `vue-tsc --noEmit` PASS、`vite build` PASS；端到端（本地 mock + vite dev）财务经营与组织效能两页数据渲染正确、**控制台无 error**。截图：`~/.codex/visualizations/2026/09/22/01a0c9da-fa03-7ee0-b736-c540344eecfa/th4-decoboon-web-p1/{01-finance,02-organization}.jpg`。

### 口径调整（已在设计方案中回写）
- **利润构成瀑布图暂不做**：现有成本科目取自应付单费用类型，与项目成本口径（`ProjectCostContext`）无法对齐，产出会是对不上的利润结构；改为「成本/应付结构 + 四个服务端口径比率」。
- **关键财务比率改为 4 项且全部取服务端值**（毛利率 / 回款率 / 应收逾期率 / 应付逾期率），避免前端自算金额比率；人均产值属组织指标，已在组织效能页呈现。

### ⚠️ 阻塞报告：当前工作区 test-compile 被其他 lane 的在途改动打断（与本批次无关）
- 现象：`mvn -o test` 现在**无法编译测试源**——`PurchaseCreationPolicyTest`、`ContractChangeFulfillmentContextTest`、`MaterialDeductionContextTest` 引用的构造器参数个数与生产代码不一致。
- 证据：`MaterialDeductionContext.java`（mtime 00:52:39）与 `project/service/purchases/save/support/Save.java`（00:52:05）在本批次最后一次改动（00:49:48）之后被其他 lane 修改，新增了 `PartnerCreditRepository` 参数，而未同步更新上述测试。三个测试文件均为其他 lane 的已修改/未跟踪文件。
- 影响：本批次自身 4 个测试类在 test-compile 可用时均为全绿；`mvn -o clean compile`（主代码）当前 **PASS**。
- 建议：请并发 lane 收口后同步修复这三个测试类的构造器参数，再复跑全量 `mvn -o test`（预期仍只剩 `BossMeasureWorkspace2AcceptanceTest` 2 例与 `PurchaseDeliveryModeTest` 4 例的历史失败）。

## 2026-09-23 P2：业务分析 + 风险中心（读侧；2 个新 Provider + 2 个前端页面）

- 依据：设计方案 v1.1 §8.4/§8.8/§9.2/§16 与 §17 开放问题默认值（Q3 按现有 CustomerSource 枚举、Q4 本期不做业务线、Q6 本期不做投诉/合规）。
- **前置条件已由其他 lane 满足**：`Quote` 已有 `submittedAt / acceptedAt / rejectedAt / convertedAt`，设计方案 §9.3 假设的「Quote 时间戳迁移」无需重做。

### 后端
1. `statistic/boss/business/report`（`BusinessAnalysisContext`）
   - 漏斗：线索（本月建档）/ 到店（本月有到店）/ 量房（本月有量房）/ 报价（本月提交）/ 签约（本月签约）+ 整体转化率。
   - **报价接受率严格按 MET-001 + DEC-001 双指标**：机会 = 客户 + 房屋；草稿 / 待决策 / 已被替代不进分母；`Submitted` 只计入「待决策报价量」单独展示；最终接受率取该机会最后一次决策，首版接受率取 version 最小的一条。
   - 渠道：按 `CustomerSource` 现有 6 值输出（含 0 值渠道，便于横向对比）；区域按 `community` 聚合 TOP N；客单 5 桶 + 中位数 + 最高。
2. `statistic/risk/report`（`RiskCenterContext`）
   - 复用 `BusinessAlertContext` 4 类预警，**不新增第二套风险规则**；处置建议统一取 `BusinessAlertType#nextAction()`（与首页、项目台账同源）。
   - 输出：分级计数、类型分布（含描述与高风险数）、责任人分布、清单（含时效文案与色调）。

### 前端
- 新增 `BusinessAnalysisView.vue`、`RiskCenterView.vue`；路由从占位页切到真实页面；`api/console.ts` 新增 `businessAnalysis` / `riskCenter`，`api/types.ts` 补 3 个类型。

### 验证
- `BusinessAnalysisContextTest` **2/2 PASS**：漏斗/渠道/区域/客单聚合；MET-001 双指标用例覆盖「首版被拒→终版接受」「首版直接接受」「草稿+替代稿不构成机会」「待决策不进分母」，最终接受率 100%、首版接受率 50%、待决策 1 条。
- `FinanceAgingContextTest` 2/2、`OrganizationContextTest` 2/2 复跑 PASS。
- `ProviderSecurityAcceptanceTest` **3/3 PASS**（路由 295 = public 12 + protected 283，并补 2 条新路由负向用例；与另一 lane 的材料计划 P2 3 条路由合并计数）。
- 前端 `vue-tsc` + `vite build` PASS；端到端两页渲染正确、**控制台无 error**。截图：`~/.codex/visualizations/2026/09/22/01a0c9da-fa03-7ee0-b736-c540344eecfa/th4-decoboon-web-p2/{01-business-analysis,02-risk-center}.jpg`。

### 未完成（明确登记为 P2.1）
- **风险处置写侧**（指派 / 催办 / 关闭 / 批量指派）需要新增 `RiskItem` 实体 + 处置日志 + 迁移 + 多状态流转，属于"新业务对象"，本批未做；前端已把该边界写在页内提示里。
- **风险趋势**（近 6 个月新增）需要历史快照表，未做。
- **客户投诉 / 合规到期 / 毛利跌破阈值** 三类风险需先建对象或扩展 `BusinessAlertType`（§10.5），未做。
- **业务线结构**（Q4）与**获客成本**（无费用采集）未做，已在前端标注。

## 2026-09-23 材料计划 P2：冲抵其他应付 / 采购明细拆单 / 主材分类损耗率（development）

- 依据：DEC-MP-02 延伸、DEC-MP-03、DEC-MP-09。
- 交付：
  1. **冲抵其他应付**：退货余款先冲减本采购单应付，再按到期日冲抵同供应商其他未结清应付，仍未冲完转 `PartnerCredit` 供应商往来余额；已付款部分不改历史流水。
  2. **PurchaseOrderItem 采购明细**：一张采购单一个供应商、可含多条材料；同一条材料可拆多供应商；新增 `project/purchase/batch` 按供应商合并下单；到场验收明细化（单明细自动匹配、多明细必须指定 `purchaseItemId`）；计划进度与状态统一由 `MaterialPlanStatusContext` 按明细派生。
  3. **主材分类损耗率继承**：新增 `MaterialCategory`（企业主数据）与 `material/category/page|save`；立项/变更生成计划行时按分类继承损耗率，`quantity = 净用量 + 净用量×损耗率%`；App 新增"主材分类与损耗"维护页。
  4. 迁移 53（`db_purchase_order_item` + 存量补明细、`db_material_category`、`db_finance_partner_credit`、`db_goods_receipt.purchase_item_id`）+ 空库基线重新生成。
- 验证：
  - 后端 `mvn -o test -Dtest='!BossMeasureWorkspace2AcceptanceTest'` → **258 tests / 0 failures / 0 errors**；
  - `MaterialDeductionContextTest` 5/0/0（含已付清→冲抵其他应付 1500 + 转往来余额 500）；
  - `ContractChangeFulfillmentContextTest` 4/0/0、`PurchaseDeliveryModeTest` 4/0/0、`PurchaseCreationPolicyTest` 3/0/0、`MaterialSelectionFlowTest` 7/0/0；
  - `ProviderSecurityAcceptanceTest` 3/0/0（路由 288→293，protected 276→281）；
  - App `type-check` / `build:mp-weixin` / `check:subpackages` PASS（主包 628,941 B，14 分包 / 91 页）。
- changed_files：见 `_系统/协作/lanes/development/workspace/material-plan-p2-handoff.md` §3。
- risks：迁移 53 须先执行且含存量补明细；**合并下单多选 UI 未接入 App**（Provider/API 已就绪）；供应商往来余额只记录不自动抵扣；主材分类按名称匹配合同分类名；入仓退货仍走既有 `stock/supplier-return`；并发 Maven 构建曾导致 target 覆盖伪失败（重跑恢复）。
- next_action：按 51→52→53 执行迁移并部署；acceptance 独立验收冲抵其他应付、合并下单、多明细到场、损耗继承；下一批做合并下单多选 UI、往来余额自动抵扣、入仓退货入口统一。
- 状态：`implementation_done_self_tested_pending_independent_acceptance`。

## 2026-09-23 P2.1 风险处置闭环（后端核心：数据模型 + 领域上下文 + 7 个 Provider + 迁移）

- 前置：用户 2026-09-23 明确"按建议走"，7 项待裁决全部按建议默认值冻结（SLA 高 4h/24h、中 24h/48h、低 48h/7d；关闭不限角色但必填结果；同级复现不自动重开；凭证不强制；客户投诉本期不做；批量上限 50 + 二次确认；超时 1 倍 SLA 通知上一级）。设计稿：`docs/product/2026-09-23-风险处置闭环设计方案-P2.1.md`。
- 数据模型（新增）：`risk/configure/{RiskStatus,RiskAction}.java`、`risk/model/{RiskItem,RiskDispositionLog,RiskAssignRequest,RiskBatchAssignRequest,RiskDispositionRequest,RiskLogView}.java`、`risk/repository/{RiskItemRepository,RiskDispositionLogRepository}.java`。
  - `RiskItem`：`@DataPermission` + `@SnowflakeId` + 四层归属（company/store/team/owner）；唯一约束 `(com_id, alert_key)`；6 条联合索引（均以 `com_id` 最左）。
  - `RiskDispositionLog`：append-only 流水（对齐 `AcceptanceRound`/`DEC-007`），含 action、from/to status、operator、note、evidenceUrls。
- 领域上下文：`risk/context/RiskDispositionContext.java`
  - `sync(principal, alerts)`：把实时预警与处置单对齐——新预警建单、**级别升级自动重开（轮次+1、保留历史）**、**同级复现仅累计 `recurrenceCount` 不重开**、**条件消失自动 `Void`（不计闭环率）**。
  - 动作：`assign / claim / batchAssign（≤50）/ record / urge / close（必填结果）/ reopen（必填原因、轮次+1）/ logs`。
  - 权限：Boss/Finance 全企业、StoreManager 本店、其余角色仅本人负责的风险；加载一律校验企业归属，禁用裸 `findById` 后判断。
  - SLA：`dueAt` 按级别落库（高 24h / 中 48h / 低 7d）；不碰任何账务事实。
- Provider（新增 7 条路由）：`risk/item/assign`、`risk/item/claim`、`risk/item/batch-assign`、`risk/item/record`（`urgent=true` 即催办）、`risk/item/close`、`risk/item/reopen`、`risk/item/log/page`。
- 迁移：`docs/design/migrations/54-风险处置单与处置流水.sql`（含 `db_risk_item`、`db_risk_disposition_log`、`db_risk_log_evidence` 三张表与索引/唯一键）。
- 验证：`mvn -o -q compile` **PASS**（主代码，含 7 个新 Provider）；`ProviderSecurityAcceptanceTest` 的路由计数已更新为 **302（public 12 + protected 290）** 并补 2 条新路由负向用例。
- ⚠️ 未完成（本批次因预算/时间截断，明确登记）：
  1. `statistic/risk/report` 尚未 join `RiskItem`（清单里还看不到 `riskId/status/dueAt/轮次/复现次数`），因此**前端动作按钮还接不上**；
  2. 前端风险中心的操作列仍是只读建议文案，**未接入指派/催办/关闭/时间线抽屉**；
  3. `RiskDispositionContextTest` 未编写（闭环规则：升级重开/同级不重开/条件消失 Void/关闭必填/越权指派 尚未被测试锁定）；
  4. 超时升级通知（Q7）未实现；
  5. 当前工作区 `BossMeasureReportContextTest` 被其他 lane 改动打断 test-compile，导致测试态无法复跑全量。
- 结论：**P2.1 后端核心已落地并可编译，但"处置闭环可写"这一出口标准尚未达成**（缺读侧 join + 前端 + 测试），不得对外宣称已支持风险处置闭环。

## 2026-09-23 P2.1 收口：读侧 join + 前端动作 + 闭环测试（本轮补齐）

- **读侧打通**：`RiskCenterReport.RiskItemView` 增 `riskId/status/statusLabel/disposerName/dueAt/dueText/dueTone/roundNo/recurrenceCount`；`RiskCenterContext` 注入 `RiskDispositionContext`，在读取清单时按稳定 `alertKey` 调 `sync` 对齐处置单。`support/Report` 由 `@Transactional(readOnly=true)` 改为 `@Transactional`（sync 会建单/重开/消解，不再只读）。
- **测试锁定（6/6 PASS）**：`RiskDispositionContextTest` 覆盖用户裁决的六条硬规则 —— ①同一 alertKey 建单且 SLA 落库（高风险 24h）；②**级别升级自动重开**（轮次+1、保留 `closeResult`、原因含"升级"）；③**同级复现只累计不重开**（`recurrenceCount=1`、状态仍 Closed、轮次不变）；④**条件消失自动 Void**（`closedAt` 为空，不计闭环率）；⑤关闭必填结果 / 重开必填原因；⑥跨企业指派被拒 + 批量上限 50 被拒。
- **前端打通**：`api/console.ts` 新增 `riskAssign/riskClaim/riskBatchAssign/riskRecord/riskUrge/riskClose/riskReopen/riskLogs/employeeOptions`；`ConsoleTable` 支持 `selectable` 多选与 `selection-change`；`RiskCenterView` 新增「处置状态」「处置时限」列、行内「指派/关闭」按钮、批量指派（多选 + 超过 1 条二次确认）、处置抽屉（时间线 + 写处置记录 + 催办 + 重开）。
- **端到端验证**（本地 mock + vite dev）：风险中心清单正确渲染处置状态（处理中/待响应）、复现次数、处置时限（剩余 6 小时/剩余 2 天）与操作按钮；**行内「指派」弹窗可打开且从 `company/member/page` 正确加载受理人候选**；控制台无 error。
- **遗留（未完成，不得对外宣称闭环已验收）**：
  1. 行点击打开处置抽屉的交互**未验证成功**（自动化点击落在类型分布文本上，未触发行点击；需人工确认或改为显式「详情」按钮）；
  2. 迁移 54 **未执行**（dev/prod 均未跑）；
  3. 超时升级通知（Q7）未实现；
  4. 独立验收未做（acceptance lane）。
- 状态：**后端闭环 + 前端动作入口已可用；待迁移执行、抽屉交互修复与独立验收后收口**。

## 2026-09-23 材料计划 P3：合并下单 UI / 往来余额自动抵扣 / 入仓退货统一（development）

- 交付：
  1. **往来余额自动抵扣**：新增 `finance/context/PartnerCreditContext`，确认下单形成应付后按供应商往来余额 FIFO（ID 升序）抵扣未付部分；余额不足全额抵扣、多余留存，不改历史付款流水。
  2. **入仓退货统一入口**：`MaterialDeductionContext` 新增 `postInventoryReturn`，退货单生成时若采购为"入仓库"则按库存批次 FIFO 同步退回；直送工地不产生库存流水。
  3. **合并下单多选 UI**：工作台新增多选态（按项目约束、仅可选可下单材料）+ 底部"已选 N 项/合并下单"；新增 `material-batch-order.vue`（选供应商、交付方式、逐行数量单价、合计、确认下单）；Boss/财务确认即形成应付。
- 验证：
  - 后端 `mvn -o test -Dtest='!BossMeasureWorkspace2AcceptanceTest'` → **264 tests / 0 failures / 0 errors**；
  - 新增 `PartnerCreditContextTest` 3/0/0（FIFO 抵扣 800、余额留存、已付清跳过）；
  - App `type-check` / `build:mp-weixin` / `check:subpackages` PASS（主包 629,140 B，14 分包 / 92 页）。
- changed_files：见 `_系统/协作/lanes/development/workspace/material-plan-p3-handoff.md` §3。
- risks：入仓退货库存不足时只退可用部分、退货单仍全额冲减应付（需业务确认）；合并下单限同一项目；`PartnerCredit` 暂无财务手工抵扣入口；并发 Maven 构建覆盖 target 导致过一次伪失败（重跑恢复）。
- next_action：执行迁移 51→52→53 → 部署 → acceptance 独立验收合并下单/余额抵扣/入仓退货；待确认"部分退库+全额冲减应付"口径。
- 状态：`implementation_done_self_tested_pending_independent_acceptance`。

## 2026-09-23 P2.1 补充：详情入口修复 + 交互验证（部分达成）

- 修复：`RiskCenterView` 操作列新增显式「详情」按钮（不再依赖整行点击），列宽 150→188px。`vue-tsc` PASS。
- 交互验证（本地有状态 mock + vite dev，截图 `~/.codex/visualizations/2026/09/22/01a0c9da-fa03-7ee0-b736-c540344eecfa/th4-decoboon-web-p21/`）：
  - ✅ **详情按钮可打开处置抽屉**，抽屉内「处置时间线」正确渲染（含指派流水）→ `01-drawer-open.jpg`
  - ✅ **关闭生效**：填处置结果后清单状态变为「已关闭」→ `02-closed.jpg`
  - ✅ 指派弹窗可打开且受理人候选从 `company/member/page` 正确加载（前一轮已验证）
  - ⚠️ **未验证**：`写入处置记录` 是否即时进入时间线（自动化断言为 false）；`重开（轮次+1）` 按钮未命中（自动化点到了另一条仍处于处理中的行，抽屉正确渲染的是「处理中」分支），故重开路径**尚未取得交互证据**。
- 结论：**禁止把「风险处置闭环」表述为已验证完成**；重开与处置记录两条路径需要人工或用更精确的行定位再验一次。
- 仍未完成：迁移 54 未执行、超时升级通知（Q7）未实现、独立验收未做。

## 2026-09-23 App 企业负责人端材料计划工作台接入（development）

- 需求：在装得清 App「企业负责人」端构建材料计划工作台入口与经营视角。
- 交付：
  1. **首页快捷入口**：`roles/catalog.ts` 的 `ROLE_ACTIONS.boss` 新增「材料计划」（icon `package` → `P.materialPlanWorkbench`）；财务同步新增（capability `view:supply`）。
  2. **首页材料计划卡片**：BOSS 首页快捷入口下方新增卡片，实时展示**待下单 / 在途 / 逾期未到场**三项指标（数据来自 `materialPlanWorkbenchApi.list()`，服务端按数据范围聚合），点击直达工作台；三项全为 0 时自动隐藏，不占位。
  3. **工作台企业负责人视图**：标题副文案按角色切换（BOSS=全公司材料计划；财务=履约协同）；项目卡片头部可点进入项目详情。
- 验证：App `npm run type-check`、`npm run build:mp-weixin`、`npm run check:subpackages` 全部 PASS（主包 632,292 B，14 分包 / 92 页）。
- 范围：本批仅改 App（`src/pages/index/index.vue`、`src/roles/catalog.ts`、`src/styles/home.scss`、`src/subpackages/project/pages/material-plan-workbench.vue`），未改后端。
- risks：首页卡片依赖 `material/plan/workbench` 接口，接口异常时按空数据隐藏卡片（不阻塞首页其它内容），符合"状态不可确认不假装完成"约束。
- next_action：真机/开发者工具验证 BOSS 账号首页卡片与工作台下单、到场确认链路；由 acceptance lane 按 AC-01～AC-23 覆盖。
- 状态：`implementation_done_self_tested_pending_independent_acceptance`。

## 2026-09-23 迁移 54 已在本地 dev 库执行（P2.1）

- 本地 `decoboon` 库执行 `54-风险处置单与处置流水.sql` 成功，`db_risk_item` / `db_risk_disposition_log` / `db_risk_log_evidence` 三张表就位，字段与索引与实体一致。
- 生产库未执行；按规范须走 `scripts/deploy-prod.sh` 并单独取得授权。
- P2.1 剩余：处置记录入时间线与重开两条路径的交互验证、超时升级通知（Q7）、独立验收。

## 2026-09-23 材料计划工作台 UI 交互优化（development / ux-optimization·redesign·standard）

- 依据：`ux-optimization` Skill（intent=redesign，visual_depth=standard）；对象=App 企业负责人/财务共用的材料计划工作台。
- Design Read：用户=财务与企业负责人，手机碎片时间、常在工地；主路径=打开找待办（待下单/待到货/逾期）→ 下单或到场确认 → 状态回落；保护区=金额口径与角色可见性、合并下单同项目约束、下单/到场动作语义。
- 已修复的交互问题（P1）：
  1. 筛选无数量、需点开才知道有无数据 → 每个分组显示实时数量，0 值分组视觉弱化。
  2. 项目内材料行全部展开，长项目淹没首屏 → 项目卡片可折叠，**默认只展开有逾期或待下单的项目**，折叠态仍显示"展开 N 条"。
  3. 项目标签与筛选口径错位（筛"待下单"却显示"待到货 3"）→ 项目标签统计跟随当前筛选集。
  4. "退货冲减"挂页面底部、脱离项目上下文 → 改为独立筛选视图（有退货单时才出现），已处理完自动回到"全部"。
  5. 多选态下不可选行无区分、点不动易困惑 → 不可选行置灰+虚线勾选框；新增多选提示条与"全选本项目"；底部栏显示已选项目名并可取消。
  6. 行内"下一步"与按钮重复、主次不清 → "下一步"降级为标签+文案，动作按钮保留主/次层级（去下单=主，到场确认=次）。
- 其他优化：一次拉全量+本地筛选（切换分组零等待、计数与列表同源）；待办优先排序（逾期→待下单→待到货→部分到场）；工具栏显示"N 个项目 · M 条材料"；空态给出下一步行动（前往供应商协作 / 查看全部材料）；错误态保留重试。
- 验证：
  - App `npm run type-check` PASS；
  - App `npm run build:mp-weixin` DONE；`npm run check:subpackages` PASS（主包 632,358 B，14 分包 / 92 页）；
  - **新增** `npm run build:h5` DONE（跨端编译通过）。
  - 视觉/运行时证据：**STATIC_ONLY + 构建验证**；未做真机或微信开发者工具截图核验。
- changed_files：`src/subpackages/project/pages/material-plan-workbench.vue`（重写）；`src/domains/material/model.ts`、`src/domains/material/api.ts`（退货记录补 `projectId`）。
- risks：未在真机验证长列表/超长材料名/弱网下的折叠与多选表现；`jz-btn` 在 `view` 内点击区依赖全局样式，未做无障碍专项回归。
- next_action：微信开发者工具（企业负责人账号）核验折叠、多选合并下单、退货视图；由 acceptance lane 覆盖长列表与弱网异常态。
- 状态：`implementation_done_self_tested_pending_independent_acceptance`。

## 2026-09-23 P2.1 真后端端到端验证（重大发现：mock 掩盖了 3 个真实缺陷）

- 环境：本地 MySQL/Redis 均在跑；**8090 上已有运行中的 dev 后端且已包含本批 P2.1 代码**（无需另起）。登录账号用本地验收号 `19900000005` / `Maihong2026`（Finance 角色，种子脚本默认密码）。
- ✅ **真实验证通过的部分**：
  1. `statistic/risk/report` 返回**真实数据**：9 条逾期应收预警、责任人分布（张伟×8）。
  2. 读取清单时 `sync` **真实建单成功**：返回真实 `riskId=228203199415189504`，状态「待响应」、处置时限「剩余 6 天」→ 证明 `db_risk_item` 落库链路可用、`(com_id, alert_key)` 幂等生效。
- ❌ **真实暴露的 3 个缺陷（mock 全部掩盖）**：
  1. **`risk/item/assign` 无法注入受理人**：报 `assigneeId不能为空`。根因已定位——`support/Assign` 上用 `@AutoInject private Long assigneeId`，但请求模型 `RiskAssignRequest` **没有 assigneeId 字段**，框架按请求模型取不到值。修法：把 `assigneeId` 加进 `RiskAssignRequest` 并由 provider 从 request 读取（前端已按 `{riskId, assigneeId, note}` 发送，无需改）。
  2. **`record` / `close` / `reopen` 全部失败且错误被吞**：返回 `undefinedErrorCode: 未定义错误信息:{0}`，真实原因不可见。三个动作都走到 `RiskDispositionContext` 的 `load()/requireDisposer()/requireOpen()` 分支，怀疑 `dataScopeContext.requireEmployee` 或自定义 `DefaultErrorCode("400"/"403"/"404", ...)` 的消息键未注册；**必须先解决错误码可读性再定位**。
  3. **错误码规范问题**：自定义数字码（400/403/404）在真实链路上被渲染成"未定义错误信息"，导致排障不可进行。需改用已注册的错误码键（参考 `ACCOUNT_LOCKED`、`AUTH_FAILED` 这类语义键）。
- ⚠️ 结论：**P2.1 仍不可交付**。真实验证证明「读侧 + 建单」可用，但「指派/记录/关闭/重开」四个写动作在真后端全部失败；此前基于 mock 的"动作入口可用"结论**不成立**，已作废。
- 经验（建议入 LESSON）：**涉及新表 + 新写接口的批次，mock 端到端只能验证渲染与调用形状，不能作为闭环可用性证据；必须在真实库上跑一遍写路径。**

## 2026-09-23 P2.1 真机排障：根因确认为 @AutoInject 位置错误（非错误码格式）

- 先按假设改了错误码（数字码→语义键 `RISK_*`）+ 修 assigneeId 注入（字段补进 `RiskAssignRequest`），重启真实后端重跑 —— **仍然全部 `undefinedErrorCode`**，说明错误码格式**不是**根因（该假设已作废，语义键保留无害）。
- **从 `logs/log_error.log` 拿到真实错误**：
  - `risk/item/record处理失败: The given id must not be null`
  - `risk/item/log/page处理失败: The given id must not be null`
  - `risk/item/assign处理失败: assigneeId不能为空`（旧代码，provider 字段校验生效）
- **根因**：`The given id must not be null` 是 Spring Data `findById(null)` 的报错 —— 说明 `request.getRiskId()` 为 **null**。
  本工程的 `@AutoInject` 约定是**声明在 Provider(support) 类的字段上**（参照 `project/service/overview/support/Overview.java` 的 `@AutoInject private Integer current;`），由框架从请求体注入到**Provider 字段**。
  我把 `@AutoInject` 写在了 **request model 的字段**上（`RiskDispositionRequest` / `RiskAssignRequest` / `RiskBatchAssignRequest`），**框架不会注入**，因此 `request.getRiskId()` 恒为 null → `findById(null)` → 框架把该异常兜底成 `undefinedErrorCode`。
- **修复方案（机械、7 个 provider 同改）**：把 `riskId / assigneeId / riskIds / note / result / reason / evidenceUrls` 全部改为在 **support 类**上用 `@AutoInject` 字段声明（必要时 `@NotNull`），Provider 从自身字段取值后传给 `RiskDispositionContext`；request model 只保留为非注入的入参载体或直接改用现有实体。
- 状态：**P2.1 写路径在真机仍不可用**；本次未完成修复（预算耗尽）。已停止原 8090 进程并以其修复版重启（当前 8090 由本机 run-dev.sh 托管）。

## 2026-09-23 P2.1 真机排障第二轮：@AutoInject 迁移后仍不注入（错误已变干净，但未解决）

- 改动：把 7 个 risk Provider 的入参字段全部从 request model 迁到 **support 类**并用 `@AutoInject`（+ 必要 `@NotNull`）声明；request model 去掉误导性注解。`mvn -o compile` PASS。
- 重启真后端重跑 `risk_e2e.sh`，错误形态**发生变化**：
  - 改前：`risk/item/record处理失败: The given id must not be null`（框架兜底 `undefinedErrorCode`，根因被吞）
  - 改后：`validationNotNull: riskId不能为空`（**校验生效、错误可读**）
  - → 证明「Provider 字段 + `@NotNull`」已被框架识别，但 **`@AutoInject` 仍没有把请求体里的 `riskId` 注入进来**。
- 顺带证伪了另一个假设：调用 `project/project/overview` 用 Finance 账号返回 `{"code":"403","message":"当前账号无权执行该操作"}` —— **数字码能正常渲染**，说明 `undefinedErrorCode` 是框架对**非预期异常**的兜底信号（不是错误码格式问题）。故上一轮改的语义键**无必要但无害**，可保留。
- 尚未查明：`@AutoInject` 的取值来源。已排除「声明在 request model」与「声明在 Provider 字段」两种猜测；下一步应**直接阅读框架实现**（`th4-framework` 中 `@AutoInject` 对应的 RequestFilter/Processor）确认它到底从 request 的哪个载体按什么规则取值，停止继续猜测。
- 状态：**P2.1 写路径在真机仍不可用**；本轮未达成目标。8090 当前由本机 `run-dev.sh` 托管（跑的是本修复版，写路径仍失败）。

## 2026-09-23 P2.1 真机排障第三轮：读到框架实现，定位到 RequestContext 变量机制（仍未修好）

### 读框架得到的确定结论
- `javap` 反编译 `th4-framework`：
  - `AutoInjectFieldInject.inject(...)`：取 `@AutoInject.value()`，**为空则用字段名**，然后 `requestContext.lookupVariable(name)`；**取不到（null）直接跳过注入，不报错**。
  - `StandardRequestContext.lookupVariable` 继承 `StandardEvaluationContext` → 即 **SpEL 变量查找**。
  - 结论：`@AutoInject` 不是"从 JSON body 绑定"，而是**查 SpEL 变量**；变量由框架其它 filter 写入 `RequestContext.variables`。
- 已工作的对照组：`statistic/performance/report` 的 request model `PerformanceQuery` 是**普通模型**，provider 是 `call(PerformanceQuery request)` 后**直接 `request.getMonth()`**——完全不依赖 `@AutoInject`。

### 三次尝试的实测结果（均失败）
| 尝试 | 写法 | 真机结果 |
| --- | --- | --- |
| 1 | `@AutoInject/@NotNull` 放在 **request model** 字段上，provider 读 `request.getXxx()` | `The given id must not be null`（`findById(null)`）→ 说明 model 字段**未被绑定** |
| 2 | 挪到 **Provider 字段**上，provider 读自身字段 | `validationNotNull: riskId不能为空`（校验生效但**未注入**） |
| 3 | 移除全部 `@AutoInject`，provider 读 `request.getXxx()`（对齐 `PerformanceQuery` 写法） | **仍 `The given id must not be null`** → 说明 request model **依然没有被 JSON body 绑定** |

### 尚未查明（下一步该做的确切动作）
- 三个尝试都指向同一个未解问题：**请求体没有绑定到我的 request model**。下一步应直接读框架的**请求体绑定/入参反序列化**实现（`ProviderFieldInjectRequestFilter` 上下游、以及创建 request 实例的那个 filter/processor），并**逐字段对比 `PerformanceQuery` 与本批 `RiskDispositionRequest`**（类注解、字段注解、getter/setter、包位置、是否被框架登记为可绑定类型），找出差异点。
- 停止继续猜测式改动。

### 状态
- **P2.1 写路径在真机仍不可用**。读侧 + `sync` 建单可用（真库已验证）。
- 8090 当前由本机 `run-dev.sh`（后台）托管，跑的是第三轮修复版。

## 2026-09-23 P2.1 纯诊断轮：对照组实验跑不起来，但暴露了更根本的问题

### 已确认
1. `@AutoInject` = SpEL 变量查找（字节码确认），**不是** JSON body 绑定；取不到变量时静默跳过。
2. 工程内**可工作的写法**是「普通 request model + Provider 直接 `request.getXxx()`」（对照组 `statistic/performance/report` + `PerformanceQuery`）。
3. 我的 `RiskDispositionRequest` 与 `PerformanceQuery` **结构一致**（普通字段 + `@Note` + getter/setter），理论上应能被绑定。
4. 但真机上 `request.getRiskId()` **仍为 null** → 请求体没有到达我的 request 对象。

### 对照组实验失败（关键）
- 想用「已知可工作 Provider + body 参数」验证扁平 JSON 是否绑定，但两个候选都被 403 挡住：
  - `statistic/performance/report`（Finance 无权，需 Boss/店长）
  - `company/member/page`（Finance 无权）
- 当前唯一可用的账号是 **Finance（19900000005）**；**Boss 张伟没有 Password 凭证（只有 Wx）**，无法登录做对照实验。

### 由此暴露的更根本问题
**我这套「扁平 JSON body 调 Provider」的调用约定，从未在真后端被验证过** —— 截至目前所有"调通"的接口都是 mock。`statistic/risk/report` 能返回数据，但它的 `asOf` 是可选参数，**不能证明绑定成立**。因此不能排除：**Web 端 api 层的调用形状本身就是错的**（若如此，影响面是 P0/P1/P2 全部页面的写接口，而不仅是风险闭环）。

### 下一步（仍不改业务代码）
1. **对照小程序端**：读 `th4-decoboon-app` 的 http/api 实现，看它调用 Provider 时**请求体与鉴权头的真实形状**（它是长期可用、已验收的调用方），与我 web 端 `src/api/http.ts` 逐项对比。
2. **建立可用的对照账号**：给 Boss 张伟设置 Password 凭证（或申请一个 Boss 测试号），用它跑 `project/project/overview` 的 size 变化对照实验，确认扁平 body 是否绑定。
3. 结论明确后再动 risk 代码。

## 2026-09-23 P2.1 诊断：对照小程序端调用实现 —— Web 端调用约定被证明是对的

### 事实（`th4-decoboon-app/src/core/http.ts` 第 83–113 行）
已验收的小程序端调用形状：
```ts
uni.request({
  url: `${runtime.apiBase}${uri}`,   // base + "域/对象/能力"
  method: 'POST',
  data,                               // ← 扁平 JSON body
  header: { 'content-type': 'application/json', token: ... }  // ← 扁平 token 头
})
```
与我 web 端 `src/api/http.ts`（`post(route, body)` + `token` 头 + `/api/` 前缀）**完全一致**。

### 由此可以排除的方向
- ❌ **不是**「扁平 JSON body 调用约定错误」——长期可用、已验收的小程序端就是这么调的。
- ❌ **不是** `@AutoInject` 用法差异那么简单（已在字节码层面确认它是 SpEL 变量查找）。
- ❌ **不是** 错误码格式（已实测数字码 `403` 能正常渲染）。

### 现在的确定边界
调用约定正确 + 对照模型结构一致（`PerformanceQuery`）却仍绑不上 `RiskDispositionRequest` → **缺陷被收窄到「我这批 Provider 自身的接线」**，而不是调用方式或框架整体约定。

### 下一步（最小化隔离实验，仍建议先不改业务代码）
新增**一个最小 Provider**（普通 request model + 一个必填字段，例如 `risk/debug/echo`）部署到真机，用一个可用账号调用：
- 若能绑定 → 说明框架行为正常，差异必在 risk 这批 provider/model 的某处（逐个对齐即可定位）；
- 若也不能绑定 → 说明存在框架级要求（如 request 类型需被登记 / 需 `@Model` / 需特定包路径），再拿**确认被调用过的** App provider 做逐项比对。

## 2026-09-23 App 全链路真实数据模拟与材料下单阻断修复

- 批次：`APP-BIZ-SIM-20260923`，企业麦鸿设计，基础项目 `麦鸿演示项目 37~44`。
- 构造方式：真实 Provider 顺序推进，不使用 mock，不直接写业务终态表；账号使用店长、业务员、两名设计师和财务本地密码账号，未改动 BOSS 微信凭证。
- 数据覆盖：每项目完成签约后主材选材与客户确认；37~39 材料到场，40~42 已下单待到货，43~44 保持可下单；每项目至少一个增项和一个减项，项目 38 保留待审批增项，项目 43/44 增加主材数量变更。
- 闭环测试发现：`project/purchase/order` 创建应付时 `dueDate=null`，而 `Payable.dueDate` 与数据库列为 `NOT NULL`，真实下单事务失败。按 `DEC-MP-01` 修复为“下单形成应付、到场后回填账期”：`Payable.dueDate` 改为可空；新增 `docs/design/migrations/55-应付账期到场回填.sql`；同步空库基线。
- 验证证据：后端 `mvn -o -DskipTests compile` PASS；定向测试 `30 tests / 0 failures / 0 errors` PASS；独立核验 `verify_app_full_chain.py` 返回 `failures=[]`；App 实际读取接口（材料计划工作台、选材分项、项目采购、合同变更）PASS。
- changed_files：`src/main/java/com/th4/edge/decoboon/finance/model/Payable.java`、`docs/design/migrations/55-应付账期到场回填.sql`、`docs/design/migrations/01-空库基线.sql`、`_系统/协作/lanes/development/workspace/app-full-chain-simulation-20260923/**`。
- 状态：`implementation_and_self_test_done_pending_independent_acceptance`。

## 2026-09-23 P2.1 决定性对照实验：扁平 body + Provider 字段 @AutoInject **确认可用**

### 实验（用 Boss 账号 18660219918 / admin123，真机）
对 `project/project/overview` 传不同 `size`：
```
size=1  -> 返回 1 条，响应 size 字段 = 1
size=5  -> 返回 5 条，响应 size 字段 = 5
size=12 -> 返回 9 条（全量），响应 size 字段 = 12
```
该 Provider 的写法是 `@AutoInject private Integer size;`（**Provider 字段**，无 `@NotNull`），请求类型是实体 `Project`。

### 由此确定
- ✅ **扁平 JSON body 能绑定**（调用约定正确，Web 端 api 层无问题）
- ✅ **`@AutoInject` 声明在 Provider 字段上能注入**（框架机制正常）
- ❌ 所以本批 risk Provider 绑不上的原因**既不是调用方式、也不是注入机制本身**，而是**我这批接线里某个具体差异**

### 新的第一嫌疑（基于本轮证据，尚未验证）
对比 `Overview`（通）与我的 `Assign`（不通）的差异：`Overview` 的注入字段**没有 `@NotNull`**；我的加了 `@NotNull`。
现象上第 2 轮尝试正是「加了 `@NotNull` → 直接报 `riskId不能为空`」，说明**校验可能发生在注入之前**（先校验、后注入），从而在校验阶段就把请求拒掉了，注入根本没机会执行。
→ 下一步实验：把 risk Provider 上的 `@NotNull` 去掉，只留 `@AutoInject`，重启后重跑 `risk_e2e.sh`。（若成立，则是校验与注入的先后顺序问题，需改为在 context 内做必填校验并抛 `DefinedException`。）

### 环境交接
- 8090 已停止，交还用户 IDE 接管（Boss 账号 18660219918 / admin123 已确认可登录）。
- 本地 dev 库留有验证数据：riskId=228203199415189504（风险处置单，状态待响应）。

## 2026-09-23 App 自定义导航页返回入口补齐（development）

- 起因：用户反馈"新增页面是不是缺少返回上一步的按钮"，要求全量排查。
- 根因（一次查清）：`JzPageHeader` **本身不提供返回按钮**，而全 App 所有 `navigationStyle: custom` 页面都依赖它 —— 自定义导航会移除原生返回按钮，用户只能依赖系统手势返回，属于全 App 级交互缺陷。
- 审计结果（脚本扫描 pages.json + 页面源码）：
  - 总页面 105，自定义导航 19，其中 Tab 根页 8（home/project/dynamic/mine/finance/design/quote + customer）无需返回；
  - 需返回入口的自定义导航页 **11** 个：sign-workbench、contract、change、change-workbench、change-create、change-detail、material-plan、**material-plan-workbench（新增）**、**material-category（新增）**、**material-batch-order（新增）**、auth/login；
  - `auth/login` 为 reLaunch 直达的根页（栈内无上一页），**有意不加返回**。
- 修复：
  1. `components/JzPageHeader.vue` 新增 `back` 属性与返回实现：栈内 >1 页则 `uni.navigateBack()`，被直达（分享/扫码，栈内仅当前页）时回工作台首页 `P.home`，并抛出 `back` 事件便于页面拦截；返回按钮 64rpx 热区 + 按压态。
  2. 9 个使用 `JzPageHeader` 的页面开启 `back`（sign-workbench / change / change-workbench / change-create / change-detail / material-plan / material-plan-workbench / material-category / material-batch-order）。
  3. `contract.vue` 原本**连 PageHeader 都没有**（Hero 卡直接顶到页面顶部，且未避让状态栏）→ 在 Hero 内加入带状态栏避让的返回按钮（`useSystem` + `padTop`），顺带修复了标题被状态栏遮挡的问题。
- 验证：
  - 复跑审计脚本：需返回入口的 11 个页面中，**10 个已具备返回入口，仅 auth/login 为有意例外**；
  - App `npm run type-check` PASS；`npm run build:mp-weixin` DONE；`npm run check:subpackages` PASS（主包 633,166 B，92 页）；`npm run build:h5` DONE。
- 证据等级：源码扫描 + 构建验证（STATIC_ONLY）；未在真机核验点击返回与手势返回并存时的表现。
- next_action：真机抽查 3 个页面（contract / material-plan-workbench / material-batch-order）的返回行为，含"扫码直达后返回"的回首页兜底；后续新增自定义导航页请统一使用 `<JzPageHeader ... back />`。
- 状态：`implementation_done_self_tested_pending_independent_acceptance`。

## 2026-09-23 P2.1 修复：@AutoInject 去 @NotNull + 必填校验下沉到 context

- 依据决定性对照实验（`project/project/overview` 的 size=1/5/12 真机生效）得出的结论：
  扁平 body 能绑定、`@AutoInject` 声明在 **Provider 字段**上能注入；本批失败的差异点是**我在注入字段上加了 `@NotNull`**——现象（第 2 轮尝试直接报 `riskId不能为空`）表明**校验先于注入执行**，字段还没来得及注入就被校验拒绝。
- 修复内容（7 个 risk Provider 统一）：
  1. 注入字段回到 **Provider(support) 类**，只保留 `@AutoInject`，**移除全部 `@NotNull`**；
  2. 必填校验**下沉到 `RiskDispositionContext`**，抛可读的 `DefinedException`（`RISK_ID_REQUIRED` / `RISK_ASSIGNEE_REQUIRED` / `RISK_NOTE_REQUIRED` / `RISK_CLOSE_RESULT_REQUIRED` / `RISK_REOPEN_REASON_REQUIRED` 等）；
  3. `Close` 里避免 `String result = result;` 自引用编译错误。
- 状态：`mvn -o compile` **PASS**。
- ⚠️ **尚未真机验证**：我在 8093 起的验证实例因后台进程未 detach，在命令结束时被杀（8093 DOWN），未能跑通 `risk_e2e.sh`。
- ⚠️ **用户 IDE 的 8090 实例仍是旧代码**：需重启 IDE 后端才能加载本次修复，之后用下面的命令验证。
- 验证命令（改端口即可）：`sed 's#8090#8093#' /private/tmp/risk_e2e.sh > /tmp/e2e.sh && bash /tmp/e2e.sh`，预期 ①→⑦ 全绿。

## 2026-09-23 材料计划状态机与"已下单后能否再选材"规则印证（development / verify-only）

- 用户问题：已下单的材料计划，是不是不允许客户再进行选材？
- 事实源：装得清业务智能体 `BR-004`（签约后主材选材文本化规则）、`DEC-003`、`SCN-006`、`GAP-005`；设计稿 v1.2（DEC-MP-05/07/08）。
- 印证结论（源码逐条核对，未改代码）：
  1. **当前实现允许**已下单/已到货后继续选材：`material/selection/save` 与 `confirm` 均不校验材料计划状态与采购单；App 选材页仅显示提示文案，保存按钮仍可点。
  2. 该次变更**不影响已发生的采购事实**（采购单/应付/到场保留原值），符合 BR-004「选材不改变材料计划/采购/库存事实」与 DEC-003「变更形成新版本、不覆盖历史」。
  3. **但"下单后是否允许再变更选材"这条规则业务知识库尚未冻结**：`GAP-005` 只冻结了"变更形成新版本"，其待确认项 4（采购前是否强制选材）已由用户 2026-09-23 的 **DEC-MP-05** 裁决为"必须"，该裁决尚未回写业务知识库。
- 印证中发现：
  - **缺陷 D1（P1，数据一致性）**：`Confirm.promoteLinkedPlan` 对已下单/已到货的计划行仍会**无条件覆盖** `selection/selectedBrand/selectedSpec`（只有状态改判是条件更新），导致"计划行选材快照"与"采购单实际材料"口径不一致；状态不会被回退这一点是正确的。
  - **缺口 G1（P2，规则未冻结）**：下单后能否再选材无业务裁决；若判"禁止"需在保存/确认增加门禁 + App 提示改为"去发起增减项"。
- 产出：`_系统/协作/lanes/development/workspace/material-plan-selection-state-verification.md`（含状态机、9 条规则实现状态表、D1/G1、修复建议、T05 咨询单草稿、源码位置）。
- 验证：本次为 verify-only，**未改动业务代码**；结论均标注源码位置可复核。
- next_action：请用户/业务裁决"下单后选材变更规则"（建议：禁止换材料，改走增减项；无论裁决如何先修 D1）；business 智能体回写 DEC-MP-05 并新建 GAP-006。
- 状态：`verification_done_pending_business_decision`。

## 2026-09-23 材料计划：下单后禁止改选材（门禁 + D1 修复 + App 入口）（development）

- 裁决输入：用户 2026-09-23 裁决"判禁止"——已进入采购的材料不允许再改选材，更换材料走增减项。
- 后端交付：
  1. `MaterialSelectionContext` 新增 `selectionLocked(plan)`：仅 `Planned`/`ReadyToOrder` 可改选材，其余（已请购/已审批/部分下单/已下单/部分到场/已到场/已关闭/已取消）一律锁定；新增列表口径 `selectionLocked(List)` 与 `plansOf(project, contractLine)`，配合 `MaterialPlanRepository.findByProjectAndContractLine`，保证**同一合同行的原计划与变更计划任一条进入采购即锁定**。
  2. `material/selection/save` 与 `material/selection/confirm` 增加门禁，拒绝文案含"如需更换材料请发起增减项"。
  3. 修复上轮印证的缺陷 **D1**：`Confirm.promoteLinkedPlan` 对已进入采购的计划行**不再回写** `selection/selectedBrand/selectedSpec`，消除"计划行选材与采购单材料口径不一致"。
  4. `applyOrderStatus` 补齐 `ordering`（已请购）/`partiallyOrdered`（部分下单），使 App 与后端门禁同口径。
- App 交付：
  1. `selection-item.vue`：`locked = orderStatus !== 'none'`、`canEditNow = canEdit && !locked`；锁定后禁用输入、隐藏保存与客户确认按钮，并把提示改为"该分项已进入采购（…），不能再修改选材"，新增 **「去发起增减项」** 按钮（带 projectId/contractId 跳 change-create）。
  2. `selection.vue`：统计/筛选统一为"已进入采购"，补齐新状态文案。
  3. `domains/material/model.ts`：`MaterialOrderStatus` 增加 `ordering`/`partiallyOrdered`。
- 验证：
  - 新增 `MaterialSelectionGateTest` 4/0/0（放行 2 态、锁定 8 态、抛错文案、同合同行多计划任一锁定）；
  - 后端 `mvn -o test -Dtest='!BossMeasureWorkspace2AcceptanceTest'` → **271 tests / 0 failures / 0 errors**；
  - App `type-check` / `build:mp-weixin` / `check:subpackages`（92 页）/ `build:h5` 全部 PASS。
- changed_files：后端 `material/context/MaterialSelectionContext.java`、`material/service/selections/{save,confirm}/support/*.java`、`project/repository/MaterialPlanRepository.java`、`test/.../material/MaterialSelectionGateTest.java`；App `subpackages/project/pages/selection-item.vue`、`selection.vue`、`domains/material/model.ts`。
- risks：`Planned/ReadyToOrder` 之外的锁定包含历史兼容值 `Requested/Approved`，存量若存在这两种状态的未采购计划，将无法再改选材（需业务确认是否可接受）；本次未做真机回归。
- next_action：business 智能体回写 GAP-005 项4 与新增"下单后禁止改选材"决策；acceptance lane 覆盖"已下单后保存/确认选材被拒 + 去发起增减项"用例。
- 状态：`implementation_done_self_tested_pending_independent_acceptance`。

## 2026-09-23 麦鸿设计新增 2 名项目经理（本地数据变更）

- 依据用户指示「先不用考虑订阅规则，麦鸿下面增加2个项目经理，徐勇，张少康」，在本地开发库 `decoboon` 的麦鸿设计（`com_id=1789903520997907818` / 总店 `1789903521001328357`）下新增 2 名 `Pm` 项目经理：徐勇 `19900000006`、张少康 `19900000007`。
- 方式：单事务写入 `db_tenant_employee` 2 行 + `db_tenant_credentials` 2 行（`Password` 凭证、BCrypt `Maihong2026`），`create_*` 时间维度列与既有成员同口径。脚本 `docs/operations/data-changes/2026-09-23-麦鸿新增项目经理.sql`，回滚脚本同名 `-rollback.sql`。
- 验证：两人 `auth/session/password-login` 成功，`auth/session/info` 返回麦鸿设计/总店/项目经理（Pm）/在职；店长视角 `company/member/page` `totalElements=8` 且包含两人。
- 边界：按用户指示**未执行** `SubscriptionContext.requireSeatCapacity`，企业仍为基础版 `maxSeats=5`、现 8 人（`subscription/info` 显示超席），仅为本地演示事实；未指定团队、未挂演示项目；生产环境无改动。
- 记录：`docs/operations/data-changes/2026-09-23-麦鸿新增项目经理.md`。
- 状态：`data_change_done_verified`。

## 2026-09-23 业务知识库回写：材料计划选材门禁（business 回写 / 用户授权）

- 触发：用户在"材料计划状态机与选材规则印证"结论后要求业务智能体执行回写。
- 回写位置：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/推广营销/装得清业务智能体`（项目目录外，经用户授权执行）。
- 回写内容：
  1. 新增 `DEC-008 材料计划选材门禁与换材路径`（status=approved）：下单前必须有已确认选材；进入采购后 8 态禁止改选材；换材料走增减项；同一合同行多条计划任一进入采购即锁定；已进入采购后不回写选材快照。
  2. 新增 `BR-009 材料计划选材门禁规则`（status=approved）：按 T03 模板含规则声明、规则字段、例外边界、验收用例。
  3. 新增 `GAP-009 材料计划采购与应付口径待知识化`（status=draft）：登记 DEC-MP-01～10 中尚未转为 BR 的 9 项，并标注入仓退货库存不足口径待确认。
  4. 更新 `GAP-005`：待确认项 4（采购前是否强制选材）收敛为"必须"；补充"进入采购后禁止变更选材"结论；剩余项 1～3 保留待正式审核。
  5. 登记证据 `EV-040`（用户裁决）、`EV-041`（后端实现）、`EV-042`（App 实现）、`EV-043`（DEC-MP-01～10 批次），证据总数 39 → 43。
  6. `06-状态/CHANGELOG.md` 新增 `v0.9.0`。
- 校验：`python3 05-自动化/knowledge.py validate` → **校验通过**（知识卡 46 张、证据 43 条；1 条既有 WARN 为 EV-007 路径不可达，与本次无关）；`knowledge.py index` → 知识索引.json/md 重新生成，DEC-008/BR-009/GAP-009 均已入索引。
- 边界：卡片状态为 `approved`/`draft`（未标 `verified`），因目标环境部署、真机回归与独立验收未完成。
- 状态：`business_writeback_done`。

## 2026-09-23 麦鸿演示项目 37 端到端实测（开工→节点→验收→收款→结算）

- 实测报告：`docs/acceptance/麦鸿演示项目37端到端实测报告.md`；证据 `docs/acceptance/evidence/project37-e2e-20260923/`。
- 执行方式：本地 dev 后端真实 Provider + 真实角色 token（PM 徐勇、财务 吴濛濛、设计师 谢玲玲）+ 本地 MySQL；现场证据为真实上传 PNG（`file/private/grant`）。
- 通过：阶段开工（含重复开工幂等）、任务完成、提交阶段验收（Pending + 第 1 轮）、应收核销 + 收款 + 资金台账、结算快照、员工代业主确认 403、无 token/员工 token 访问业主接口被拒。
- **P0 缺陷 DEF-PROJ37-001**：`project/task-node/save` 全部失败，根因 `TaskNodeKey` 未实现 `Enumerable.getValue()` → `parseKey` NPE（`undefinedErrorCode`）；影像节点留证（施工进行/隐蔽/施工完成）真实链路不可用。
- **P1 缺陷**：收款重复提交在余额校验后才查幂等键 → 重复请求返回 400「收款金额不能超过应收余额」；`refreshProject` 把无任务阶段从 Doing 回退为 Todo（实测开工交底 `actual_start` 有值而状态 Todo）；`submitAcceptance` 把 `Cancelled` 任务计入验收门禁且 `project/task/update` 允许 `Cancelled→Done`。
- 外部阻断：本地无真实微信业主授权会话，业主驳回/复验/确认未在真实 HTTP 链路执行；替代证据 `OwnerClosureAcceptanceTest` 19/0/0（2026-09-23 02:25）。
- 夹具：项目 37 临时指派徐勇为项目经理、已取消任务临时置 Done（用于绕过门禁），**均已在实测后回滚**。
- 业务咨询：`REQ-BIZ-20260923-001` 已投递业务智能体（G-01~G-07：阶段验收模式、工艺库绑定、阶段权重与工期、重排与快照、阶段日期入口、工艺主数据形态、取消任务与验收门禁）。
- 状态：`self_test_done_with_defects_pending_fix_and_independent_acceptance`。

## 2026-09-23 P0/P1 修复：影像节点留证、施工任务排期落库、收款幂等（DEF-PROJ37-001/002/005）

- 变更文件：
  - `src/main/java/com/th4/edge/decoboon/project/configure/TaskNodeKey.java`：实现 `Enumerable#getValue()`（返回枚举名），修复 `TaskNodeKey.getValue()` 为 null 导致节点解析 NPE。
  - `src/main/java/com/th4/edge/decoboon/project/service/tasknodes/save/support/Save.java`：`parseKey` 改为先按枚举名匹配、`getValue()` 空安全比较，空白值单独报错。
  - `src/main/java/com/th4/edge/decoboon/project/service/tasks/save/support/Save.java`：新建任务补 `task.setProperty(project.getProperty())`，修复 `ProjectTask.property` 非空列约束导致排期接口全部失败（本轮复验新发现）。
  - `src/main/java/com/th4/edge/decoboon/finance/service/receipts/save/support/Save.java`：幂等键查找与 `sameBusiness` 判定前移到应收余额校验之前；非重复提交仍校验余额。
- 新增测试：`TaskNodeSaveAcceptanceTest`（9 例：三节点留证、证据必填、重复节点、隐蔽顺序、非 PM 403、大小写/非法/空节点、排期落库、更新不重复）、`ReceiptIdempotencyTest`（4 例：重复提交返回原收款、客户端幂等键、新事实超额、非正金额）。
- 验证：`mvn -o -Dtest=TaskNodeSaveAcceptanceTest test` → 9/0/0；`mvn -o -Dtest=ReceiptIdempotencyTest test` → 4/0/0；`mvn -o test -Dtest='!BossMeasureWorkspace2AcceptanceTest'` → **284 tests / 0 failures / 0 errors, BUILD SUCCESS**；全量含该类的 286 tests 仅剩其历史既有 2 例失败（与本次改动无关）。
- 真实链路复验：在 8093 起修复后实例 + 本地库，对项目 37 执行 `project/task/save` 新建临时任务 → `project/task-node/save` Start 成功（重复 400、非法 400）→ 财务重复提交同一收款返回原 `receiptId`、新事实超额仍 400；临时任务与「项目经理=徐勇」夹具均已清理/回滚。证据 `docs/acceptance/evidence/project37-e2e-20260923/reverify-after-fix.json`。
- 未处理（待业务裁决）：空任务阶段开工后状态回退（G-03）、`Cancelled` 任务计入验收门禁（G-07），见 `REQ-BIZ-20260923-001`。
- 状态：`p0_p1_fixed_verified_by_self_test_pending_independent_acceptance`。

## 2026-09-23 项目工地权限与增减项窗口（用户五项要求）

- 依据用户 2026-09-23 要求：切换设计师/指定与切换项目经理、施工任务支持企业负责人+店长+项目经理、未完工前支持增减项、计划默认按排期自动制定并支持项目经理调整、任务由项目经理/设计师制定。
- 本次实现：`project/task/update` 与 `project/task-node/save` 增加 `StoreManager`（受本门店数据范围约束）；`project/contract-change/save` 增加窗口门槛（项目 `Finished/Settled` 阻断、合同 `Closed/Terminated` 阻断）。
- 已支持无需改动：成员切换（`project/project/update` Boss/PM，`project/member/save` Boss）、任务制定（`project/task/save` Boss/PM/项目设计师）。
- 测试：`TaskNodeSaveAcceptanceTest` 11/0/0（新增店长同门店成功、跨门店 404）、`ContractChangeCompletionGateTest` 5/0/0（新增）、`ReceiptIdempotencyTest` 4/0/0；后端全量（排除历史无关失败类）**291 tests / 0 failures / BUILD SUCCESS**。
- 待裁决：成员切换权限归属与留痕（Q-A/Q-B）、阶段开工是否开放店长（Q-C）、计划按排期自动生成口径（Q-D，与 `REQ-BIZ-20260923-001` G-03/G-04 合并）、增减项边界与店长发起权（Q-E）。
- 记录：`docs/product/2026-09-23-项目工地需求对照与实施记录.md`。
- 状态：`partial_implemented_pending_business_decision_and_independent_acceptance`。

## 2026-09-23 项目工地五项要求落地（成员切换/阶段开工/计划口径/增减项）

- 用户裁决并实现：
  1. 成员切换（设计师/项目经理）仅企业负责人 + 本门店店长，**禁止项目经理自行改派**；切换写一条对内项目动态（原→新 + 操作人 + 时间，`visibleToCustomer=false`），立即生效、无接手确认。
  2. 阶段开工 `project/stage/update` 开放给店长（限本门店数据范围）。
  3. 计划口径：阶段计划 = 该阶段排期任务 `min(plannedStart)/max(plannedEnd)`，项目计划 = 全部阶段汇总；与合同施工计划快照冲突时**以排期为准并回写快照**；无排期任务的阶段保留原窗口；PM 可手工调整且不要求留痕。
  4. 增减项：阶段待验收期间允许；店长可发起（Boss/店长/PM/项目设计师）；仅项目 `Finished/Settled` 与合同 `Closed/Terminated` 阻断。
- 代码：`ProjectAccessContext`（`requireMemberEditor`/`requireChangeInitiator`/`sameStore`）、`projects/update`、`members/save`、`stages/update`、`contract-change/save`、`ProjectContext.recalculatePlanFromSchedule`、`ContractPlanContext.syncConstructionPlanFromProject`、`tasks/save|delete`（排期变更触发重算）。
- 测试：`ProjectMemberSwitchAndStageOpenTest` 7/0/0、`ProjectPlanFromScheduleTest` 1/0/0、`TaskNodeSaveAcceptanceTest` 11/0/0、`ContractChangeCompletionGateTest` 5/0/0、`ReceiptIdempotencyTest` 4/0/0；后端全量（排除历史无关失败类）**299 tests / 0 failures / BUILD SUCCESS**。
- 记录：`docs/product/2026-09-23-项目工地需求对照与实施记录.md`；`REQ-BIZ-20260923-001` 追加用户裁决回执（Q-A~Q-E）。
- 待复核：成员切换动态是否需要对业主公开、无任务阶段工期折算规则、阶段验收模式/工艺库绑定/取消任务门禁（G-01/G-02/G-07）。
- 状态：`implemented_self_tested_pending_independent_acceptance`。

## 2026-09-23 App 修复：新建任务「选择负责人」显示 undefined（BUG-OWNER-UNDEFINED）

- 现象：App 项目 → 新建任务 → 选择负责人，下拉显示「张三（undefined）」。
- 根因：后端 `user/employee/page`（`Employee`）不返回 `title` 字段（实测 `title: null`），岗位名在 `role.name`；App `normalizeEmployee` 直接取 `raw.title` 得到 undefined，`task/edit.vue` 又直接拼接 `name + '（' + title + '）'`。
- 修复（App 仓库 `th4-decoboon-app`）：
  - `src/domains/identity/api.ts`：新增 `roleDisplayName()`，`normalizeEmployee` 改为 `title: raw.title ?? roleDisplayName(raw.role)`（全局生效：任务负责人下拉、任务详情、项目成员、团队列表、我的/首页岗位文案）。
  - `src/subpackages/project/pages/task/edit.vue`：下拉文案统一走 `ownerDisplay()`，岗位缺失回退 `ROLE_META[role].short` 或角色 key，不再直接拼接 title。
  - `src/subpackages/project/pages/accept/edit.vue`：验收提交人岗位文案补 `|| '-'` 兜底。
  - 新增回归门禁 `scripts/verify-employee-display-contract.mjs`（含反向保护：禁止 `o.name + '（' + o.title + '）'` 写法），并接入 `npm run verify:mp-weixin`。
- 验证：`node scripts/verify-employee-display-contract.mjs` PASS；`npm run type-check` PASS；`npm run build:mp-weixin` DONE；`npm run check:subpackages` PASS（主包 633294 B / 总包 2187039 B / 92 页）；真实接口核对 `user/employee/page` 返回 `title=null, role.name=企业负责人|项目经理…`；本地复刻映射：修复前「徐勇（null）」→ 修复后「徐勇（项目经理）」。
- 已知无关失败：`npm run verify:mp-weixin` 仍在 `check:home` 失败（BOSS 首页六项功能域文案与 `roles/catalog.ts` 不一致），worklog 2026-09-23 第 1312 行已登记为既有问题，且 `src/pages/index/index.vue` 存在其他会话在途修改。
- 状态：`fixed_pending_device_check`。

## 2026-09-23 App 修复（续）：任务负责人候选加店长 + 任务保存字段映射（BUG-OWNER-NOT-SAVED）

- 用户要求：店长加入任务负责人候选。
- 变更（App `th4-decoboon-app`）：
  - `subpackages/project/pages/task/edit.vue`：负责人候选改为 `boss / storeManager / pm / designer / marketer`。
  - `subpackages/project/pages/task/detail.vue`：节点留证/推进角色与服务端对齐为 `boss / storeManager / pm`（设计师只排期，不确认节点，避免 403）。
  - `roles/permission.ts`：店长补 `act:startStage`（阶段开工，2026-09-23 用户裁决 Q-C）。
  - `domains/schedule/api.ts`：**修复任务保存字段映射**——后端 Provider 入参是扁平字段（`taskId/zoneId/ownerId/catalogId`），前端 Task 是对象形态，历史实现 `post('project/task/save', task)` 直接把对象发过去，导致「选择的负责人 / 区域」丢失、编辑任务丢失 taskId 变成新建。现映射为 `taskId: task.id`、`zoneId: task.zone?.id`、`ownerId: task.owner?.id`、`spec: task.spec || task.acceptStandard` 等。
  - `scripts/verify-employee-display-contract.mjs` 扩展门禁：断言负责人候选含店长、节点角色含店长、店长有 `act:startStage`、saveTask 映射三字段，并反向禁止直接 `post('project/task/save', task)`。
- 真实链路复验（8093 修复后实例 + 本地库，项目 37）：`project/task/save`（App 映射后的 payload，`ownerId=王宏文/店长`）→ 成功；`project/task/info` → `owner=王宏文（StoreManager）`、`zone=客厅`；临时任务已删除、项目 37 项目经理夹具已还原。证据 `docs/acceptance/evidence/project37-e2e-20260923/task-owner-save-verify.json`。
- 验证：`node scripts/verify-employee-display-contract.mjs` PASS；`npm run type-check` PASS；`npm run build:mp-weixin` DONE；`npm run check:subpackages` PASS（92 页 / 总包 2187079 B）。
- 状态：`fixed_verified_by_live_api_pending_device_check`。

## 2026-09-23 项目工地闭环与交互缺陷修复（App + 后端）

- 用户反馈三类问题全部修复：① 未指定项目经理无提示（根因：无提示 + 开工异常无 try/catch 静默失败）② 无切换设计师/项目经理入口 ③ 任务可重复下发（前端只挡购物车内重复、后端无校验）。
- 后端：`project/task/save` 新增下发唯一性校验（同「项目×阶段×区域×工艺条目」只允许一条未取消任务，重复下发 400 并提示在原任务上调整数量）；新增仓储查询 `findByProject_ProjectIdAndStage_StageIdAndCatalogItem_ItemId`；测试 `TaskNodeSaveAcceptanceTest` 增 1 例（12/0/0）。
- App：工地详情新增未指定项目经理警示条 + 工地岗位卡（项目经理/设计师 + 指定/更换入口）+ 无项目经理时开工按钮改为跳转指定 + 开工异常提示 + 验收横幅改用真实阶段名；项目成员页新增「工地岗位」切换（员工选择弹层，按角色过滤，仅 Boss/本门店店长）+ 加载兜底；排期页加载既有任务标记「已下发」并拦截重复选择、保存逐条异常提示；`normalizeTask` 补 catalogId 回退；任务编辑保存失败不再卡 loading。
- 真实验证（8093 + 本地库，项目 37）：首次下发成功、重复下发被拒（`400 该工艺任务已在「木作吊顶 · 客厅」下发过…`）、换区域可下发、店长切换项目经理成功（张少康/Pm，验证后切回并还原夹具）、临时任务清理完成。证据 `docs/acceptance/evidence/project37-e2e-20260923/site-flow-verify.json`。
- 门禁：后端全量（排除历史无关失败类）300 tests / 0 failures / BUILD SUCCESS；App type-check PASS、build:mp-weixin DONE、check:subpackages PASS（92 页）、员工显示契约 PASS。
- 报告：`docs/acceptance/项目工地闭环与交互缺陷检查报告.md`（含 6 项顺带修复的 SITE-01~06 与 5 项未闭环项）。
- 状态：`fixed_verified_by_live_api_pending_device_and_acceptance`。

## 2026-09-23 微信开发者工具 · 企业负责人真机闭环测试

- 打通工具链：DevTools CLI `auto --auto-port 9420` + `miniprogram-automator`（node 22，`page.$$`/`evaluate` 驱动）；dev 构建需指向本机可达地址（127.0.0.1 在模拟器内不可达，DHCP 变更会失效）；`project.config.json` 需 `miniprogramRoot=./`、`urlCheck=false`。
- 登录：小程序「微信登录」真实换取 code，命中已绑定凭证 → 以 **张伟（企业负责人）** 身份进入，无需手机号授权。
- 实测通过：首页经营驾驶舱、项目列表 9 个、工地详情、未指定项目经理警示、工地岗位切换（UI→DB 落库 + 项目动态留痕）、排期页「已下发」去重标记、负责人候选含店长、验收横幅真实阶段名。
- 真机发现并修复的缺陷：`MP-01` 自由文本区域（客餐厅/主卧）导致工艺库整片为空、`MP-02` 已下发识别失效（catalogId/区域键口径不一致）、`MP-03` 未指定项目经理无提示且开工静默失败、`MP-04` 无成员切换入口、`MP-05` 验收横幅硬编码、`MP-06` 保存失败卡 loading。
- 数据准确性抽查：首页指标、项目卡片金额/回款率、切换后 manager、排期去重数量与库内一致。
- 报告：`docs/acceptance/微信开发者工具企业负责人业务闭环测试报告.md`；截图证据 `docs/acceptance/evidence/mp-devtools-20260923/`。
- 未覆盖：现场拍照节点留证（需真机相机授权）、业主侧确认/驳回（需真实业主微信）、视觉审美评审（本会话无图像识别能力）。
- 状态：`boss_role_closed_loop_verified_on_devtools_pending_device_and_visual_review`。

## 2026-09-23 REQ-BIZ-20260923-002 沉浸式审计修复 · 研发独立复核
- 审查候选改动集（后端 11 文件 + App 3 文件），**未新增代码改动**（只读审查 + 跑测试）。
- 权限审查：业务员只读 Specification（`visibleReadableProject` / `visibleReadableDesign`）仅用于读取路径、company 等值约束、非业务员回退原加载；设计师客户详情按 `custId + comId + designer.empId` 三重约束；`requireMemberEditor` 为**收权**（PM 不再可自行改派项目成员）。未发现权限扩大或跨租户放宽。
- **发现 F-1（主因，状态 DEV_IN_PROGRESS）**：项目**列表**对业务员仅按 `project.owner` 查询，而**详情/阶段/任务**按 `project.owner OR customer.owner OR quote.createdBy`。因 `project.owner = property.owner ?: customer.owner`，当 `property.owner ≠ customer.owner` 时"列表看不到、详情能打开"，与业务裁决「以客户负责人关系或房屋业务员为准」不一致。
- **发现 F-2（需业务澄清）**：新增 `requireChangeInitiator` 允许 `Boss/Pm/StoreManager/项目设计师` 发起增减项，已接入 `contract-change/save`；交接单未含此项，属权限判定变更。
- 测试：后端 `compile` PASS；定向权限测试 **15/15 PASS**（DataScopeRequestFilterIntegrationTest、ProviderSecurityAcceptanceTest、DetailResponseWhitelistContractTest、EmployeeRoleContractTest、DesignContextMultiPropertyTest）；后端全量 **304 tests / 2 failures**，两处失败均在未跟踪的在飞文件 `BossMeasureWorkspace2AcceptanceTest`（statistic measure 域），**不属本候选改动**；App `type-check` PASS、`build:mp-weixin` PASS。
- 前端核验：`Accepting` → 待验收（App meta，全仓无遗留"竣工验收"标签）；`¥¥` 已清；设计列表改 `Promise.allSettled` 且仅清空失败项；邀请页新增 `loadError` 错误态与返回入口。
- 未完成/BLOCKED：微信开发者工具真机页面回归、正向/反向端到端可见性与越权矩阵、采购应付付款对账复核、生产验证 —— 均未执行，不得以本地测试冒充。XA-006 保持 BLOCKED。
- restart/deploy evidence：无（未启动服务、未部署、未提交）。
- 交接：`_系统/协作/lanes/development/workspace/immersion-audit-fix-20260923/HANDOFF.md`（含 changed_files / tests / risks / acceptance_needed / learning_request）。
- learning_request：建议对 `zhuangdeqing-smb-reviewer` 沉淀"只读可见性必须单一事实源（列表与详情同源）"审查项与错误集条目。

## 2026-09-23 生产后端监控：受控巡检（exit 20）

- 首次采集受 macOS `date -Is` 兼容性和沙箱 SSH 限制影响返回 `2`；使用 `/tmp` 临时日期 shim 并授权只读 SSH 后，`collect "35 minutes ago"` 返回 `20`。
- 服务 `decoboon.service` enabled/active，当前版本 `/opt/decoboon/releases/20260921-050519`；HTTPS health 200 + expected `tokenIsUndefined`；根盘 18%，内存正常，35 分钟 journal 无条目。
- 窗口内仅 `.env`、`.git/config` 两次扫描探测和本次健康检查噪音；无新高置信代码缺陷，按安全门禁未改业务代码、未创建快照、未测试/构建/发版。
- 继续发现监控工具 registry 数据完整性缺陷：历史 fixed 懒加载事故 ID 被改写为 `INC-20260923-a1233597b0/open`、计数重复；`canonicalize_existing()` 为主要根因线索，需人工修脚本并补回归测试。
- 证据：`docs/operations/incidents/2026-09-23/20260923-130103/triage.md`；副本 `analysis.json`、`summary.md`、`raw.txt`。
- next_action：人工修 analyzer/registry stable ID 与状态保持逻辑；WAF 拦截常见扫描路径；将授权过期及明确业务规则拒绝纳入分类规则。

## 2026-09-24 业主端审计修复批次 MH-APP-FIX-20260924

- 来源：`docs/acceptance/20260924-麦鸿设计-业主端沉浸式体验审计/08-问题与整改建议.md`（用户指定 4 项整改顺序）。
- 改动：
  1. `customer/context/CustomerJourneyContext.java`：新增 `resolveOwnerStatus()`，业主可见项目状态改为**按事实推导**（Pending 验收单 ⇔ 待验收；已开工阶段 ⇔ 施工中；停工/终态保留），不再信任可能漂移的 `db_project.status`（对应 XA-20260924-001）；
  2. 同文件 `date()` 增补 `LocalDateTime/LocalDate` 分支（XA-002：journey 量房摘要 ISO）；
  3. 同文件 `acceptanceItem()` 增补 `整改责任人 / 整改类型 / 整改截止 / 整改轮次` facts（XA-006：整改闭环透明）；
  4. App `subpackages/project/pages/detail.vue`：`pd__hero-stage` 改用 `projectStageText()`（XA-003）。
- 验证：后端 compile PASS + 重打包重启 PASS；前端 `vue-tsc` PASS + `uni build` PASS + 已同步 `dist/dev/mp-weixin`。
- **未完成**：小程序内效果复验（automator 连接因 rsync 触发重编译而超时）；"待我处理可展开"（2c）、"整改完成请复验"入口（3）、业主发起通道（4）未开发。
- 待办：① 重连 automator 跑 `raw/verify-fix.cjs` 取复验证据；② 解析员工密码登录路由后复现 P1 写路径；③ 2c/3/4 按门禁（3、4 需业务裁决）排期。

## 2026-09-24 业主端修复杂项 · 复验阻塞记录

- 重连微信开发者工具自动化（`cli auto`）成功；但**业主会话无法重建**：清空存储后点「微信登录」，后端返回 `PHONE_REQUIRED`，登录页切到「首次使用，授权手机号继续」（DevTools 无法完成手机号授权）。
- 交叉核验：该 openid（`oeNUzxkb…`）在库中确有 `Credentials`（`cre_id=1790138481695640868`，type=Wx，Active）+ 1 条 Active `cust_customer_access`；我自己换码得到的 openid 与旧值一致 → **不是"该微信没绑定"**，而是后端未按 openid 命中既有凭证（或未拿到 openid）。
- 新增缺陷 `XA-20260924-007`（P1，阻塞复验）。主要假设：**`run-dev.sh` 不加载 `.env.dev.local`**，重启后微信小程序配置缺失 → 换码得不到 openid → 走"首次登录"分支抛 PHONE_REQUIRED。次要假设：构建重编译后 code 与后端 AppID 不一致。
- 因此 MH-APP-FIX-20260924 的三处改动**仍为"待小程序内复验"**；复验脚本 `docs/acceptance/20260924-麦鸿设计-业主端沉浸式体验审计/raw/verify-fix.cjs` 已就绪。
- 建议下一步：① 在 `run-dev.sh` 加载 `.env.dev.local` 并打印"微信配置是否就绪"；② 后端把"配置缺失/换码失败/首次登录"拆成不同错误码；③ 恢复会话后跑 verify-fix.cjs 取证据。

## 2026-09-24 登录问题定位与恢复 + 三项修复真机复验通过

- 登录问题：`run-dev.sh` 显式加载 `.env.dev.local`（历史坑：重启即丢微信小程序配置）；`auth/session/login` 增加**指纹诊断日志**（SHA-256 前 8 位 + 命中凭证布尔 + 是否带 phoneCode，不记录敏感原文）。
- 恢复证据：直连登录返回 token（`sub` = 业主凭证 `1790138481695640868`）；**App 内真实微信登录成功**（落地 `pages/index/index`、`jzb_token` 写入）。
- 复验通过（开发者工具真机页面文本）：① 项目旅程状态 **待验收→施工中**（P1 读侧权威生效）；② 量房摘要 **ISO→`2026-09-10 05:03:07`**；③ 项目详情"当前阶段" **空白→"待验收"**（回退口径生效）；④ 验收整改显示 **整改责任人/类型/截止/轮次**。
- 另一项：业主邀请链接生成已改指向 `pages/owner/enter`（`subpackages/project/pages/invite.vue`）。
- 待办：项目详情投影补 `currentStageName`；2c 待我处理可展开；"整改完成请复验"与业主发起通道待业务裁决。
