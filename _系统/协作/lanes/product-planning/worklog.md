## 2026-09-26 T06冻结、A0.2启动与A2 PRD

- `ANS-BIZ-20260926-004` 已返回：正式点数账本机制可开发，真实票价、套餐点数和资源包价格继续暂缓 A0.4。
- A0.2 产品规格：`docs/product/2026-09-26-AI资源点数账本P0产品需求规格.md`。
- A0.2 开发分支：`feature/ai-point-ledger`，提交 `6fb2304`，全量后端测试 PASS。
- A2 产品规划已启动：`docs/product/2026-09-26-AI谈单官P0产品需求规格.md`。
- A0.1/A1 联合验收发现 4 项缺陷，开发已在后端 dev `0f78366` 修复并提交复验。
- 状态：A0.2 实现自测完成待验收；A2 PRD 待评审。

## 2026-09-26 AI 能力接入产品研发总计划与首波启动

- 依据用户要求，将 AI 能力接入总体设计方案正式转为研发项目。
- 产出 `docs/plan/2026-09-26-AI能力接入产品研发总计划.md`，明确 A0-A6 波次、依赖、责任人、权限边界和验收流程。
- 首波冻结为 A0.1 影子用量底账 + A1 AI 经营参谋 P0。
- 当前检查点：后端 A0.1 `9ed122c`、A1 `2218ce9`；Web AI 资源统计 `7257482`、经营简报 `0597914`。
- A0.1、A1 均完成开发自测和构建，下一步为联合联调与 acceptance 独立验收。
- T06 仍为正式点数、资源包和收费门禁。

## Current

`REQ-20260926-AI-DESIGN-P0.4-PRODUCT` 已由 development lane 实现并自测交付，当前状态 `implementation_done_self_tested_pending_independent_acceptance`。后端 `feature/design-ai-context-prompt` 已完成字段/角色收口与效果图持久化，App `feature/design-ai-generation-app` 已完成页面和入口；已转入 acceptance lane，等待独立验收，不得视为验收通过。

## Outputs

| title | path | audience | status |
| --- | --- | --- | --- |
| 装得清上线前业务功能与开发范围规划 | `docs/plan/2026-09-15-上线前业务功能规划.md` | user / development / acceptance | draft_for_review |
| 报价项目库批量导入 PRD | `docs/product/2026-09-26-报价项目库批量导入PRD.md` | user / development / acceptance | frozen_for_development |
| AI 设计生成 App 页面与状态规格 | `docs/product/2026-09-26-AI设计生成App页面与状态规格.md` | user / development / acceptance | implementation_done_self_tested_pending_independent_acceptance |

## Verification

- `REQ-BIZ-20260926-003`、`DEC-010 / AI-DEC-10~12`、`SOL-004` 与冻结包 `PKT-20260926-AI-DESIGN-001` 已读取。
- P0.1-P0.3 工作树 `/private/tmp/th4-decoboon-ai-design` 的 build/info/page/update、模型、状态枚举、上下文构建、Prompt 渲染和迁移 63 已逐项核对。
- App 设计分包路由、`domains/design` 类型/API、角色矩阵、路由守卫、方案详情 CAD 交互已核对；当前不存在 AI 生成页面或 API。
- 已确认 `info/update` 对外返回成本、模型参数、用量、请求号和原始失败信息，且 `info/page` 未复用设计写角色门槛；已作为 P0.4 阻断缺口写入规格。
- development 后端分支提交 `88fb6d4,b422ded,4a54790,668570f,ebda263,f2f1376,c143bd5,fc5bb9f` 与 App 分支提交 `f646468,5b19501` 已存在；后端 build/info/page/update 字段裁剪、`requireDesignWriteAccess`、TalkBrief 角色裁剪、私有效果图持久化与 Design 文件签名均已在源码中核对。
- development 报告后端 compile/package PASS、假模型端到端 PASS，App type-check/build/分包检查 PASS；375/390/430 真实截图、四角色黑盒和迁移隔离演练仍待 acceptance。
- 仅修改 `docs/product/**` 与 product-planning lane/协作记录，未修改 `src/**`、App 或生产配置。

## Next

1. acceptance lane 按规格第 12/14 节独立复验，重点覆盖四角色、TalkBrief 隔离、任务状态恢复、封面/区域回写、文件签名和交付门禁。
2. `ProviderSecurityAcceptanceTest` 路由基线从 341 更新为 345，并让字段最小化断言与当前响应契约一致；不得只改预期掩盖权限缺陷。
3. 迁移 63 需在隔离 MySQL 演练，`scripts/build-release.sh` 的 `MIGRATION_END` 由 release/acceptance 流程处理。
4. 正式 HTML/PDF、Prompt 生成前人工确认和客户发送仍未解锁，需用户另行裁决。

## 2026-09-26 AI 设计生成 P0.4 development 交付转入 acceptance

- 交付状态：`DEV_FIXED_OR_IMPLEMENTED_PENDING_ACCEPTANCE`，只表示开发完成和自测通过，不代表独立验收结论。
- 后端分支：`feature/design-ai-context-prompt`；关键提交 `ebda263`（权限/响应收口）、`c143bd5`（效果图持久化与签名）、`fc5bb9f`（Shared Outputs）。
- App 分支：`feature/design-ai-generation-app`；关键提交 `f646468`（页面和入口）、`5b19501`（私有对象签名）。
- 产品基线：`docs/product/2026-09-26-AI设计生成App页面与状态规格.md`，状态更新为 `implementation_done_self_tested_pending_independent_acceptance`。
- 已核对实现：`DesignPromptPrivacyContext` 非 Designer 清空 `talkPrompt`；build/info/page/update 使用设计写角色门槛；效果图写入 `design/ai-render/{designId}/` 并按 Design 数据范围签名；App 新增路由、领域 API、页面及 list/scheme/workbench 入口。
- 待独立验收：真实 375/390/430 页面与安全区、四角色正反向、跨企业/门店/团队、Prompt 字段最小化、TalkBrief 隔离、状态恢复、重复任务、封面/区域回写、过期/失败版本、正式交付门禁。
- 已知开发自测项：后端 compile/package PASS；假模型 `PromptReady -> DraftReady` PASS；App type-check/build/分包检查 PASS；`ProviderSecurityAcceptanceTest` 仅因路由总数 Expected 341 / Actual 345 失败。
- 风险：真实 Seedream 受账户余额和开通状态影响；CAD 几何绝对一致性未承诺；迁移 63 未隔离演练；375/390/430 真实截图和四角色黑盒未完成。
- 交接：`HANDOFF-AI-DESIGN-P0.4-ACCEPTANCE-20260926-001` 已转交 acceptance lane。

## 2026-09-26 AI 设计生成 App 页面与状态规格（product-planning → development）

- 业务输入：`REQ-BIZ-20260926-003`、`DEC-010 / AI-DEC-10~12`、`SOL-004`、冻结包 `PKT-20260926-AI-DESIGN-001`。
- 产出：`docs/product/2026-09-26-AI设计生成App页面与状态规格.md`，状态 `frozen_for_development`。
- 页面冻结：新增 `/subpackages/design/pages/ai-generation`；设计列表方案卡和方案详情提供入口；一个页面承载条件检查、上下文摘要、Prompt 预览、状态恢复、版本对比和确认。
- 权限冻结：入口和 API 读取统一限定 BOSS、店长、团队负责人、设计师，并继续叠加企业/门店/团队/本人数据范围；TalkBrief 仅 Designer 可见，业务员、项目经理、财务、业主不得读取生成任务或 Prompt。
- 状态冻结：`PromptReady` 等待、`Generating` 等待、`DraftReady` 可确认、`DesignerConfirmed` 可切换版本、`Failed` 映射恢复；当前 `build` 提交后立即异步生成，Prompt 为冻结快照，不是生成前确认页。
- 回写冻结：确认封面替换 `Design.coverUrl`；确认区域向 `Space.images` 追加。确认后必须重新读取 `design/scheme/info`。
- 交付门禁：仅 `DesignerConfirmed + Confirmed + deliverable=true + 当前设计目标匹配` 可作为未来正式 HTML/PDF 输入；当前无 HTML/PDF Provider，App 不提供可用正式交付按钮。
- 关键缺口：`info/update` 当前下发 `costAmount/costCurrency/usageJson/requestId/modelParametersJson/failureMessage` 并向所有可读角色返回 `talkPrompt`；`info/page` 未复用 `requireDesignWriteAccess`，均必须在 P0.4 开发前收口。
- 交接：`HANDOFF-AI-DESIGN-P0.4-APP-20260926-001` 已写入规格第 14 节，等待 development lane 确认接手。

## 2026-09-26 报价项目库批量导入 PRD（product-planning → development）

- 业务输入：`ANS-BIZ-20260926-001` 只窄口径解锁报价项目库主数据导入；标准模板 V1.0；历史项目、合同、报价和财务迁移继续暂缓。
- 产出：`docs/product/2026-09-26-报价项目库批量导入PRD.md`，状态 `frozen_for_development`。
- 冻结内容：模板下载、微信聊天文件选择、上传解析、预览、整批/部分导入、结果页、错误清单、批次状态、重复策略、成本待维护、固定总价和撤销规则。
- 现有实现差距：`catalog/item/import` 仅支持接口列表或 classpath JSON，逐条默认值写入、成本缺失写 0、无预览/批次/重复/撤销，不能直接作为小程序闭环。
- 模型影响：新增 `CatalogImportBatch`、`CatalogImportRow`；`CatalogItem.costPrice` 改为可空并新增固定总价/来源批次；同步报价、套餐、合同和成本完整性上下文。
- 验收要求：550 条数据、重复文件、跨企业、成本空白与 0、固定总价冲突、错误行、部分导入、24 小时内无引用撤销、导入后立即选材。
- 交接：`HANDOFF-CATALOG-IMPORT-20260926-001` 已写入 PRD；直接发送 development session 返回 archived，当前只算“已交接资料准备完成”，尚未确认接手。

## 2026-09-23 企业负责人 Web 端「经营管控中心」高保真交互设计方案

- 输入：用户提供的原型 `/Users/zhangwei/Downloads/deepseek_html_20260922_fe83a9.html`（装得清 · 企业经营管控中心，6 个一级页面）。
- 产出：`docs/product/2026-09-23-企业负责人Web端经营管控中心高保真交互设计方案.md`（v1.0 梳理稿，约 1150 行）。
- 交付性质：**只做设计，不含代码**（按用户"先不用代码实现"要求）。
- 核对范围：后端 13 大类 Provider/Context 实测路径、`decoboon` 包模型字段、前端 `th4-decoboon-web【备份】` 的 `AGENTS.md`/`rules.md` 与 `src/th4/**`、Admin 可复用组件与令牌、业务智能体 `MET-001/MET-002/DEC-001/DEC-002/GAP-002/GAP-004`。
- 关键结论：
  1. 结构与交互可 1:1 实现；数据分 A/B/C 三级（约 40% / 40% / 20%）。
  2. **发现最高优先级冲突**：原型一级导航（经营总览/业务分析/项目监管/财务经营/组织效能/风险中心）与已冻结决策 `DEC-002`（今日主线/客户经营/项目工地/商务财务/经营分析/更多）不一致，必须先由用户裁决（方案 A 保留 `DEC-002` 并做子页映射）。
  3. 6 项必须裁决的口径阻断：净利润无源（伪利润风险）、综合毛利率未标口径、报价接受率双指标未落地、成本不完整 null 态、组织口径（无部门/工长/物料员角色）、品牌 Logo 与主色。
  4. 产出 10 条开放问题（§17）、15 组验收用例（§15）、P0–P3 实施分批（§16）。
- 验证：本稿未改动任何代码，未执行构建；所有"已存在"的后端能力均以源码逐条核对，未凭记忆断言。
- 状态：`draft_for_review`，等待用户确认 IA（Q0）与 §10 的口径裁决。
- 下一步：用户确认后，由 development lane 承接 P0（布局骨架 + 今日主线/经营总览 + 项目工地/项目监管）。


## 2026-09-23 材料计划业务梳理与产品设计（product-planning）

- 输入：用户口述的材料计划业务诉求——合同签约后设计师选材确认 → 财务在材料计划工作台按项目逐条下单（选供应商/选下单价/调数量/确认下单）→ 形成应付计划 → 材料到场由 PM/设计师确认；下单来源包含合同选材确认与项目增减项主材；扣减项需新增扣减计划并回改原材料下单与应付计划；主材与供应商关联必须落在材料业务域，不得依赖报价项目库。
- 产出：`docs/product/2026-09-23-材料计划业务梳理与设计.md`（v1.0 梳理稿，约 533 行）。
- 内容：6 个真实中小装企场景推演、现状代码盘点、9 条差距（GAP-MP-01～09）、概念边界澄清、合同/增减项两条主流程与状态机、材料计划工作台信息架构与交互、供应商匹配与下单价格口径、角色权限矩阵、金额/成本/应付口径、数据模型（MaterialPlan 增字段、新增 MaterialSupplier 与 PurchaseOrderItem、Payable/GoodsReceipt 调整、迁移 51）、Provider 路由建议、序列化契约、异常边界、15 组验收用例、P0–P2 分批与 7 条待裁决项。
- 核对范围：`material/**`、`project/**`（MaterialPlan/PurchaseOrder/GoodsReceipt/ContractChangeFulfillmentContext）、`supply/**`（Partner/PartnerPrice/PartnerPriceContext）、`finance/**`（Payable/账期）、`statistic/context/BossContractChangeContext`、迁移 26/27/29/46/49/50，以及 App `domains/material`、`subpackages/project/pages/material-plan.vue`、`selection*.vue`、`subpackages/supply/**`。
- 关键结论：
  1. 现有链路是"签约即生成计划行 → 单行采购单 → 到货才生成应付"，缺"材料计划工作台"中枢与"选材确认才可下单"门禁。
  2. 采购单头行一体、一单一条材料，无法支撑"按供应商合并下单 + 一条材料拆多供应商"，建议新增采购单明细行。
  3. 主材-供应商关联只有 `Partner.brands` 与 `CatalogItem` 级 `PartnerPrice`，与用户要求不符，建议新增材料级"材料-供应商供应关系"。
  4. **最高优先级口径冲突**：应付形成时机（下单 vs 到货）。建议区分"应付（下单即形成）"与"实际成本（到货确认）"，登记为 GAP-MP-05 / GAP-MP-D1。
  5. 扣减项需三段式：未下单直调、已下单采购调整+应付冲减、已到货退货+成本回冲。
- 验证：本稿为业务设计稿，**未改动任何代码、未执行构建**；所有"现状"结论均以源码逐条核对。
- 边界：`docs/product/**` 内的设计产物，未写业务智能体知识库、未改生产代码。
- 状态：`draft_for_review`，等待用户裁决 §15（尤其 D1/D3/D4/D6）。
- 下一步：用户裁决后由 development lane 承接 P0（字段与状态 + 选材门禁 + 供应关系 + 采购明细 + 工作台 + 下单生成应付）。

## 2026-09-23（续）设计方案 v1.1 —— 并入用户决策

用户就 v1.0 的 5 项待裁决内容给出明确决策，已全部并入 `docs/product/2026-09-23-企业负责人Web端经营管控中心高保真交互设计方案.md`（v1.1，1192 行）：

1. **Web 一级导航**：采用原型 IA（经营总览/业务分析/项目监管/财务经营/组织效能/风险中心）；业务智能体 `DEC-002` 的 Web 一级导航不再约束本端。§10.6 记录该决策，并列出 `DEC-002` 其余内容（对象阶段轨道、`Signed≠Effective`、`ProjectStage.sort+name`、"待业务确认"、终态展示）继续有效；待业务智能体补登新 `DEC-*`（仅覆盖 IA 部分）。
2. **财务口径**：按毛利润/毛利率，**不做净利润**。§8.6 财务经营 KPI 改为「本月毛利润」；利润构成瀑布改为「营收 → 主材 → 人工 → 项目管理 → 毛利润」；关键财务比率五项改为「毛利率 / 回款率 / 成本率 / 应收周转天数 / 人均产值」；费用率与净利率不展示。
3. **组织口径**：按「角色 + 门店/团队」，「部门产能分析」改为「角色产能」；「工长效能排行」改为「项目经理交付准时率排行」（口径 = 按期完工 ÷ 已完工，取 `Project.planEndDate/actualEndDate/manager`）。
4. **Web 工程目录**：确认为 `前端代码/th4-decoboon-web`（当前不存在，需先把工程从 `th4-decoboon-web【备份】` 落地过去）。
5. **路由方式**：**不新建后端 `ViewSettingConfigure`，路由与菜单完全由前端控制**。新增 §5.5 定义前端路由/菜单/角色常量/URL 状态/权限边界；§14.1 明确后端视图注册为「无」；§14.2 列出前端落地清单。

关键约束补充：`rules.md` §3「路由 = 后端注册；前端禁止新增业务路由」在本端为**显式例外**（仅限 `console` 布局 + `boss` 角色）；前端路由只决定可见性，接口鉴权与数据范围仍必须服务端强校验。

- 验证：未改动任何代码；文档结构与表格一致性已校验（0 处异常）。
- 状态：`draft_for_review`；剩余待裁决 5 项（§17 Q1/Q3/Q4/Q6–Q10），均有建议默认值，不阻断 P0。

## 2026-09-23 材料计划设计 v1.1：用户裁决落稿（product-planning）

- 输入：用户对本设计 §15 待裁决项的口头裁决——①下单形成应付、到货回填账期、成本仍在到货确认；②已到货的扣减必须走退货单；③供应商由财务在下单时决定；④瓷砖类材料必须有损耗。
- 产出：`docs/product/2026-09-23-材料计划业务梳理与设计.md` 升级为 **v1.1**（533 → 597 行），新增"决策记录 DEC-MP-01～05"。
- 主要更新：
  1. DEC-MP-01 落地：§8 应付口径改为"确认下单形成"，到货仅回填 `receiptDate/dueDate`；§9.4 明确到场不新增应付、只更新账期与状态；应付 ≠ 成本的约束显式写入。
  2. DEC-MP-02 落地：§4.2 增减项流程标注已到货扣减强制走退货单；§12 异常表升级为"必须走退货单，禁止直接改应付"；§13 新增 AC-18。
  3. DEC-MP-03 落地：§6.1 增加"供应商只能由财务在下单时决定"前置声明；§7 强制要求新增"设计师/PM 不得指定或锁定供应商"。
  4. DEC-MP-04 新增 **§4.5 损耗设计**：区分"采购损耗（计入成本，进下单数量）"与"施工损耗（只流转库存，不重复计成本）"；冻结 `quantity = netQuantity + lossQuantity`、损耗率默认 0 由企业按材料/品类配置、先冲损耗后冲净用量等规则；§9.1 新增 `netQuantity/lossRate/lossQuantity` 字段；§13 新增 AC-16/AC-19；P0/P1 分批同步纳入。
  5. §15 拆分为"15.1 已裁决（DEC-MP-01～05）"与"15.2 仍待裁决（D2/D4/D5/D7/D8）"；§16 next_action 改为"DEC 已生效，P0 可立即进入开发"。
- 验证：本轮仅改动 `docs/product/**` 设计文档与协作记录，**未改任何生产代码、未执行构建**。
- 状态：`decisions_decided_P0_ready`，等待 development lane 承接 P0。
- 下一步：development lane 按 P0（MaterialPlan 字段含损耗 + 状态重定义 + 迁移 51 → 选材门禁 → MaterialSupplier → PurchaseOrderItem → 工作台 → 下单生成应付 → PM/设计师到场确认）分批实现。

## 2026-09-23 材料计划设计 v1.2：第二轮裁决落稿，待裁决清零（product-planning）

- 输入：用户第二轮裁决——①预计成本以合同成本价；②到场确认 PM/设计负责人任一方；③选材文本不影响供应关系；④损耗率在主材分类中设置；⑤不认可"超过净用量+损耗二次确认"这一场景。
- 产出：`docs/product/2026-09-23-材料计划业务梳理与设计.md` 升级为 **v1.2**（597 → 611 行），新增 `DEC-MP-06～10`。
- 主要更新：
  1. DEC-MP-06：§8 金额口径将"计划参考成本"改为"**项目预计成本 = 合同成本价（签约成本快照）**，与下单价无关"；下单价只影响应付与实际成本。
  2. DEC-MP-07：§7 权限表与 §9.5 到场验收改为"本项目 PM 或设计负责人**任一方**确认即可，不要求双签"。
  3. DEC-MP-08：§6.1 明确**选材正文/JSON 不参与供应关系匹配**，匹配只用结构化字段 `目录项 + 品牌 + 规格/型号 + 单位`；选材文本仅用于展示/导出/确认留痕。
  4. DEC-MP-09：损耗率来源改为**企业主材分类**（§4.5 规则 2、§9.1 `lossRate` 说明、新增"主材分类与损耗率"段），分类未设置默认 0，产品不预置行业比例。
  5. DEC-MP-10：§4.5 新增规则 8、§12 异常表统一为"**下单量上限 = 净用量 + 损耗量，超出即拦截**"，删除"超订二次确认"表述，避免歧义。
  6. §13 验收矩阵扩充至 AC-23（新增预计成本取合同成本价、损耗率继承主材分类、超需求拦截、任一方到场确认）。
  7. §15.2 待裁决清单**清零**，状态改为 P0 可开发。
- 验证：仅改 `docs/product/**` 设计文档与协作记录，未改生产代码、未执行构建。
- 状态：`all_decisions_closed_P0_ready`。
- 下一步：development lane 承接 P0；建议紧接着输出 P0 开发任务单（字段/迁移/接口/页面/测试）。

## 2026-09-23 风险处置闭环设计方案（P2.1，设计稿 + 业务咨询请求）

- 产出：`docs/product/2026-09-23-风险处置闭环设计方案-P2.1.md`（304 行 / 14 张表），**不含代码**。
- 核心结论：P2.1 要解决的不是"再算一遍风险"，而是**把人对风险的处理过程记下来**；因此必须区分「预警 = 实时算出来的（不落库）」与「处置 = 人产生的（落库、不复制预警内容）」，两者用稳定 `alertKey` join。
- 关键设计：`RiskItem`（`(com_id, alert_key)` 唯一，重开不新建单只加轮次）+ `RiskDispositionLog`（append-only，对齐 `AcceptanceRound`/`DEC-007`）；状态机 `待响应 → 处理中 → 已关闭` + `已消解 Void` 终态。
- 关键边界：① 预警条件消失 = `Void`，**不计入闭环率**；② 同级复现**不自动重开**（否则逾期应收永远关不掉，闭环率失真）；③ 级别升级才自动重开并保留历史轮次；④ 处置单**不碰账**（同 `BR-008` 不变量）。
- 待裁决 7 项（§14 Q1–Q7），其中 Q1（SLA 数值）与 Q2（关闭权限）直接决定 `dueAt` 与权限矩阵。
- 按 `AGENTS.md` 与业务智能体 §7「强制咨询协议」：涉及新业务对象 / 状态机 / 权限 / 数据范围，须走 **T05 → T06 → DEC/BR → 开发 → 独立验收**，T06 答复前不得编码、不得对外宣称。

## 2026-09-23 签约后项目立项与三角色协作业务规则梳理

- 产出：`docs/product/2026-09-23-签约后项目立项与三角色协作业务规则梳理.md`（424 行 / 13 节）。
- 依据：装得清业务智能体规范事实源（`OBJ-001`、`BR-001~004`、`BR-008`、`SCN-002/003/007/010`、`DEC-001/002/007`、`MET-002`）+ 后端代码逐条核对 + 本地麦鸿演示项目 37 真实数据抽样。
- 核心结论：① 立项唯一入口是已生效合同（禁止无报价旁路）；② `Signed ≠ Effective`，只有 `Effective` 建项目；③「施工计划节点」分三层（合同施工计划快照阶段 → 项目阶段 → 工艺任务 → 影像节点），设计图/事实源术语必须按此区分；④ 排期角色 = 项目设计师/项目经理/Boss（`requirePlanEditor`）；⑤ 阶段验收确认/驳回仅业主本人，员工端确认接口 403。
- 记录缺口 6 项：阶段 `acceptanceMode` 未落 `ProjectStage`（G-01）、工艺库未绑定阶段/区域（G-02）、阶段权重与工期口径不一致（G-03）、重排不回写合同快照（G-04）、阶段日期无单独编辑入口（G-05）、工艺为文本字段非主数据（G-06）；另有直接签约补录未实现（`BR-002`/`DEC-001`）与合同签署生效角色矩阵 BLOCKED。
- 状态：`analysis_done_ready_for_product_decision`（无代码改动，仅新增梳理文档与协作记录）。

## 2026-09-24 麦鸿设计业务 App 全场景验收视图（full_chain_simulation，已冻结）

- 批次：`MH-APP-ACCEPT-20260924`；模式 `full_chain_simulation`；当前状态 `frozen`（无阻塞业务问题）。
- 产出：
  - `docs/product/2026-09-24-麦鸿设计业务App全场景验收视图.md`（317 行 / 11 节）
  - 冻结包 `_系统/协作/lanes/product-planning/workspace/PKT-20260924-MAIHONG-FULLCHAIN-001.json`，已通过 `validate_iteration_packet.py` → `PASS: ... tenants=3 scenarios=5 blocking_questions=0`。
- 做法：**复用既有真实夹具**（`APP-BIZ-SIM-20260923` 的麦鸿演示项目 37–44 + 7 个角色账号），不新造数据、不直接写终态表；把散落在各处的实测/缺陷/裁决文档收敛成一张"场景 → 真实路由 → 账号 → 前置 → 操作 → 预期 → 状态"验收视图。
- 覆盖：主链 15 段（线索/客户/房屋/量房/设计/报价/合同/项目/工地/增减项/任务/验收/整改/财务/结算）共 72 条场景；8 个角色任务卡；7 条返工链；9 条跨企业/越权负向链；账务事实链核对表；验收批次 B1–B5。
- 关键复用结论：项目 37–44 每个状态**专用于不同场景**（40/41/42 已下单待到货 → 到场场景；43/44 数量变更 → 增减项回写材料计划；38 三种变更态 → 变更分支）。
- 已标注"不要重复上报"的已知项：EXT-01（真实微信业主会话）、GAP-01~06（REQ-BIZ-20260923-001 待裁决）、DEF-PROJ37-003/004、BOUND-01~03。
- 边界：本视图**不构成验收结论**；✅ 仅表示"已有实现/修复证据可复核"，需 acceptance lane 独立确认。
- 下一步：acceptance lane 按 B1→B5 分批领取；用户安排真实业主会话补齐 A-03/A-04/A-06 与 B4；业务智能体答复 G-01/02/05/06 + 取消任务门禁后回归。

## 2026-09-24 业主邀请与注册交互重构方案（P0 可先做 / P1 待业务裁决）

- 触发：用户真实操作反馈——客户邀请、员工邀请及注册使用同一页面；业务邀请进入未加入企业页还要手填邀请码，同页下方还有"创建企业"，判定身份还会让业主/员工走错流程。
- 产出：
  - `docs/product/2026-09-24-业主邀请与注册交互重构方案.md`（239 行 / 11 节）：现状证据 → 目标原则 → 5 个真实场景 → 目标流程与文案 → 对象与状态 → **权限与安全（跨企业串号风险）** → 异常 → 验收标准 → 分期实施 → 待裁决 5 问 → 跨智能体同步；
  - T05 咨询单 `docs/product/consultations/REQ-BIZ-20260924-001-业主邀请与注册身份判定口径.md`。
- **研发侧识别的最重要风险**：把业主身份判定从 `token` 改为"手机号 → 客户档案"后，**同一手机号可能在多家企业都有客户档案**；若实现为"命中即进入"会造成**跨企业串号（P0）**。方案要求：命中多家时必须由用户显式选择、逐项目授权，禁止静默取第一条。
- 分期：**P0（可立即做，纯交互）**= 未加入企业页删掉"填写邀请码"改为三选一、注册企业独立入口、业主页去 token 字样与猜参数逻辑；**P1**（口径确认后）= 手机号 + 客户档案进入 + 多企业选择 + 授权留痕；**P2** = 邀请降级为可选、换号/解绑、防枚举；**P3** = 企业注销。
- 同步：业务智能体（T05 五问）、审查/自学习智能体（LEARN-REQ-20260924-001：入口复用 + 身份判定依赖前端参数的教训）。
- 状态：`frozen_design_waiting_for_biz_decision`（P0 不需要裁决即可开发；P1 前必须拿到 T06）。

- **P0 已完成（2026-09-24）**：新增业主专用页 `pages/owner/enter`；`company/join` 重写为"选择您的身份"四选一（邀请码输入默认收起、注册企业独立入口）；`invite/accept` 删除业主分支（残留 0）并做历史链接兼容重定向。真机验证：选择页与业主页 ✅；**历史邀请链接自动重定向未通过**（已记录原因与建议）。

## 2026-10-02 Web V2 前置冻结包（设计师最后一公里）

- 输入：用户确认 Web 比 App 更重，要求把控重量成为亮点；设计师的设计、报价、合同、增减项、选材和客户沟通必须解决最后一公里转化问题。
- 产出：
  - `前端代码/th4-decoboon-web/docs/product/web-v2/00-README.md`：冻结包入口、P0 范围、明确不做与当前状态。
  - `前端代码/th4-decoboon-web/docs/product/web-v2/01-product-charter-and-ia.md`：Web 定位、L1/L2/L3 重量模型、双壳层、一级导航、页面原型和角色对象变形。
  - `前端代码/th4-decoboon-web/docs/product/web-v2/02-designer-last-mile.md`：设计师“一单一世界”转化工作台、三栏骨架、沟通闭环、版本比较、合同/变更/选材连续链路。
  - `前端代码/th4-decoboon-web/docs/product/web-v2/03-object-action-state-contract.md`：对象上下文、动作、写入刷新、状态、权限、账务和沟通投影契约。
  - `前端代码/th4-decoboon-web/docs/product/web-v2/04-provider-data-acceptance.md`：Provider 缺口、三租户数据、G1-G3 和第一阶段验收。
  - `前端代码/th4-decoboon-web/docs/product/web-v2/05-t05-business-consultation.md` + `docs/product/consultations/REQ-BIZ-20261002-001-Web V2设计师最后一公里.md`：5 个阻断性业务问题。
  - `前端代码/th4-decoboon-web/docs/product/web-v2/iteration-packet.json`：校验 PASS，`tenants=3 scenarios=3 web_ux=4 desktop_ux=4 blocking_questions=5`。
- 关键判断：
  1. Web 重量必须来自对象上下文、版本比较、客户反馈、跨角色交接、风险和对账，不来自更多卡片与指标。
  2. Web V2 P0 核心是统一对象工作空间，设计师最后一公里是第一个垂直样板。
  3. Web 与 App 仍必须使用同一业务事实、Provider、状态机和金额口径。
  4. 客户沟通当前存在事实冲突和对象缺口，不得在 T06 前新增正式状态、消息渠道或 AI 事实。
  5. `ProjectSupport` 第 8 角色与固定 7 类员工角色冲突，继续开发前必须裁决。
- 验证：Web 产品迭代包校验器 PASS；本批次只写产品文档与咨询单，未修改生产代码，未执行页面验收。
- 状态：`iterating_waiting_for_biz_consultation`
- 下一步：业务智能体回复 T06；随后冻结 G1，选择设计师转化工作台作为首个页面样板上设计与浏览器验证。
- T06 已完成（2026-10-02）：`docs/product/consultations/ANS-BIZ-20261002-001-Web V2设计师最后一公里五问答复.md`。裁决 Web 为设计师转化指挥台和沟通记录中心；P0 使用 `CustomerFollow` + 现有版本对象引用，不新增独立沟通状态机；AI 谈单辅助保持 P1；正式客户确认和合同生效按正式权限；正式员工角色维持 7 类，`ProjectSupport` 作为岗位能力/项目参与关系。冻结包状态更新为 `frozen_for_G1`，迭代包校验 PASS（blocking_questions=0）。


## 2026-10-02 Web V2 设计师 Excel 式报价工作空间（重点需求）

- 用户新增重点需求：面向主要以 Excel 做方案报价的设计师，Web 报价页必须支持按区域报价、已报价分项、实时价格计算、类似 Excel 单元格的工程量公式、导入导出，并兼容既有 Excel 操作习惯，以更优的桌面 UI 呈现。
- 产出：
  - `前端代码/th4-decoboon-web/docs/product/web-v2/07-designer-excel-quote-workspace.md`：页面定位、三栏骨架、区域模型、单元格公式、实时计算、Excel 导入导出、版本、权限、App 兼容和验收重点。
  - `docs/product/consultations/REQ-BIZ-20261002-002-设计师Excel式报价工作空间.md`：区域、公式白名单、导入导出、设计师价格权限、服务端重算与快照 5 个业务问题。
  - `iteration-packet.json` 更新为 `iterating`，校验 PASS：`scenarios=4 web_ux=7 desktop_ux=7 blocking_questions=5`。
- 现状核对：现有后端有 `Quote/QuoteGroup/QuoteLine/QuoteComponent`、`Space` 关联、`formula` 与 `QuoteCalcContext`；Web 只有简单数量公式输入和 CSV 导出，尚不支持单元格引用、区域范围、通用函数和 `.xlsx` 报价工作簿。
- 关键约束：新增批量/公式/导入导出能力只能采用兼容加法，保留 App 单行报价接口、状态机和金额行为；公式预览可由前端提供，正式金额由服务端重算并冻结。
- 状态：`frozen_for_G1`，未编码、未验收。
- T06 已完成（2026-10-02）：`docs/product/consultations/ANS-BIZ-20261002-002-设计师Excel式报价五问答复.md`。冻结区域来源、公式白名单与精度、导入导出兼容范围、设计师销售侧价格权限、前端预览/服务端权威重算；迭代包恢复 `frozen`（blocking_questions=0）。
- 下一步：业务智能体答复 5 问；随后与设计师最后一公里共同通过 G1/G1.5，再进入首个报价样板实现。


## 2026-10-02 设计师角色页面盘点到分批开发计划

- 产出 `前端代码/th4-decoboon-web/docs/product/web-v2/08-designer-role-page-inventory-and-rollout.md`。
- 盘点当前设计师专属路由和共享页面，区分为保留、重构、合并、归档候选；明确材料选材/增减项不能继续以“复用施工页面入口”冒充独立职责。
- 建议顺序：B1 设计师对象工作空间骨架 → B2 Excel 式区域报价工作空间 → B3 客户反馈/合同交接 → B4 增减项/选材 → B5 量房/设计/施工收敛 → B6 业绩与全量清理。
- 结论：设计师业务裁决已足够，可立即开始页面梳理和 G1/G1.5 设计；开发前置阻断为 Web/App 未提交基线、稳定测试账号、App 线上回归基线，以及公式引擎/区域快照/导入导出/批量保存的技术设计。


## 2026-10-02 设计师 B1/B2 静态高保真构图

- 产出 `design/web-v2/designer-one-case-workspace.html`：一单一世界三栏对象工作空间、阶段轨道、客户决策台和事实时间线。
- 产出 `design/web-v2/designer-excel-quote-workspace.html`：区域报价表、公式栏、实时预览、服务端重算、区域小计、版本差异、导入导出入口和客户反馈。
- 产出 `docs/product/web-v2/09-designer-b1-b2-g1.5-design-freeze.md` 与 `docs/product/web-v2/10-designer-b1-b2-visual-critique-round1.md`。
- 已完成 1440/1280/1024 三档 Chromium 截图与两轮布局批判；实测无页面横向溢出、无小于12px文本；1024 下右栏改为横向决策摘要。
- 关键设计修正：公式错误时总额显示“暂不可确认”，保留上次已保存金额，避免伪完整报价。
- 状态：静态设计可评审，未接 Provider、未运行真实业务、未进入生产代码重构。


## 2026-10-02 Web V2 独立开发基线与设计师代码骨架

- 建立独立工作树 `前端代码/th4-decoboon-web-v2`，分支 `feature/web-v2-designer`，初始标签 `web-v2-designer-baseline-20261002`；原 Web 工作树保持不动。
- 将当前 Web 实际工作状态同步到 V2 工作树，避免从落后 `dev` 重建造成 2 万行架构倒退；正式发布仍必须回到 feature -> dev -> test -> prod 流程。
- App 兼容基线：线上 `1.0.7`，tag `v2026.09.26.2`，commit `a27fc5c`；App 不开发，仅后续回归。
- 新增 `/v2/designer/cases` 一单对象工作空间，使用现有 `useDesignerWorkbench`、客户沟通和真实报价/项目事实。
- 新增 `/v2/designer/quotes/:quoteId` Excel 式报价工作空间骨架，读取真实 Quote/QuoteItem/区域分组；编辑、公式服务、批量和导入导出明确未接入。
- 验证：`type-check` PASS、`build` PASS、`verify:ui` PASS。`verify:system`/`verify:roles` 仍为现有 Web 基线历史失败，需在导航切换批次统一修复。
- 当前提交：`1a684db`。
- 后续提交：`9d97870` 修正三档布局溢出、金额两位小数和最小字号；`b22653d` 记录运行验收。本地 Designer `19900000005` 登录及 B1/B2 真实 Provider 三档浏览器检查通过：横向溢出 0、<12px 文本 0、运行时错误 0。
