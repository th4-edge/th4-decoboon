# 材料履约与材料业务助手 · 实现记录

## B1 业务事实与蓝图

- Batch：B1
- 产物：`00-charter.md`、`01-scenarios.md`、`02-business-sync.md`、`03-blueprint.json` v1.0.0、字段字典与对账矩阵；
- 后端：核对 `97d5705` 已实现的模板字段投影、提交必填门禁与 `material/sheet/cancel`；
- 测试：`MaterialSheetOrderFlowTest`、`MaterialSheetTemplateFieldConfigTest`、`MaterialSheetEnumsProviderTest`、`MaterialSheetTemplateWhitelistTest`、`MaterialSheetResponseProjectionContractTest` 通过；
- 无 DDL、无迁移、无部署。

## B2 范围

- Batch：B2
- 垂直切片：Web 业务蓝图中心 + 项目材料业务体检；
- 角色：Boss / StoreManager / Finance；
- 不含：App、只读材料业务助手运行时、独立验收、正式发布；
- 不改金额/成本/状态/权限语义，不新增实体表或 DDL。

## B2 后端变更

- `blueprint/runtime/info`
  - 新增 `BlueprintRuntimeQuery`、`BlueprintRuntime`、`BlueprintRuntimeContext` 与 Provider；
  - 只允许 Boss / StoreManager / Finance；
  - 从 `src/main/resources/blueprints/material-v1.json` 读取发布副本，返回版本、状态、最近核对提交、来源数量和规范化蓝图 JSON；
  - 测试校验运行时资源与领域包 `03-blueprint.json` 完全同源。
- `blueprint/check/report`
  - 新增 `MaterialHealthQuery`、`MaterialHealthReport`、`MaterialHealthIssue`、`MaterialHealthRow`、`MaterialHealthContext` 与 Provider；
  - 复用 `MaterialPlanWorkbenchContext` 的计划/选材/下单/到场聚合；
  - 复用 `FinanceReportContext` / `ProjectCostContext` 的成本完整性、材料应付、实际成本与结算状态；
  - 采购单只读取有效状态，补充未定价与缺供应商校核；
  - 成本不完整时 `profitAmount=null`，不使用 0 冒充利润；
  - 通过 `TenantContext.requireEmployee` 先恢复当前员工上下文，再进入财务与材料范围校验，修复 Provider 注入员工懒关联导致的误判 404。
- `@Properties` 白名单覆盖报告、校核项和聚焦材料行；API 不返回 Project/Employee/Company 实体全量。

## B2 Web 变更

- 新增 `/business-blueprints`：
  - 阶段轴、阶段触发/产出/账务/角色/异常/来源；
  - 对象图谱与关键字段；
  - 字段字典搜索与对象筛选；
  - 规则、校核、例外、未决项和来源引用；
  - Boss / StoreManager / Finance 静态路由与菜单入口。
- 新增 `MaterialHealthPanel` 并接入 `/finance/projects/:projectId` 的“总览与成本”：
  - 当前阶段、下一步、结算状态；
  - 材料计划/选材/下单/到场/逾期指标；
  - 未定价、缺供应商、成本不完整校核；
  - 材料应付、预计成本、实际成本、利润；
  - 聚焦材料行与主材下单/结算入口；
  - 空态、失败态和重试恢复。
- 新增领域层 `src/domains/blueprint/{model,api}.ts`，不把业务状态在页面本地拼装。

## 验证

- 后端定向测试：
  `mvn -q -Dtest=MaterialBlueprintRuntimeTest,MaterialHealthContextTest,MaterialSheetOrderFlowTest,MaterialSheetTemplateFieldConfigTest,MaterialSheetEnumsProviderTest,MaterialSheetTemplateWhitelistTest,MaterialSheetResponseProjectionContractTest test`
  → PASS；
- 后端编译：`mvn -q -DskipTests compile` → PASS；
- Web 类型检查：`npm run type-check` → PASS；
- Web 构建：`npm run build` → PASS；
- Web 治理检查：`check:forms`、`check:concepts`、`check:states`、`check:actions`、`check:metrics`、`check:permissions`、`check:page-system`、`check:workbenches`、`check:workspaces` → 全部 exit 0（metrics 保留既有口径提示，与本批无关）。

## 真实 Provider 冒烟

- `blueprint/runtime/info`：PASS，`material.end-to-end` v1.0.0，9 阶段 / 12 对象 / 26 字段；
- `blueprint/check/report`：PASS，真实项目 `P1791842095079` 返回 1 条部分到场材料、1 张未定价采购、成本不完整 1 项，阶段为“到场确认”；
- 非法蓝图 ID：404；
- Designer 访问蓝图：单元测试断言 403。

## 风险与未闭环

- 独立验收未执行，B5 才能宣称 `ACCEPTED`；
- 测量模板门禁只证明 Tile 基线，不能宣称全品类；
- `GAP-MAT-TIME-SEMANTICS` 仍未裁决；
- 蓝图运行时资源是领域包的发布副本，后续修改蓝图必须同步更新并通过同源测试；
- 本地验收使用造数账号，临时重置密码哈希后已恢复原哈希；不涉及生产、权限、金额或业务数据。

## B3 范围

- Batch：B3
- 垂直切片：App 业务体检 + 页面上下文说明；
- 角色：Boss / StoreManager / Finance（材料体检）；Designer / Pm（作业单时间与测量上下文）；
- 不含：新 Provider、新业务规则、DDL、金额/成本语义变化、B4 助手、B6 独立验收、正式小程序发布。

## B3 App 变更

- `src/domains/material/model.ts`、`src/domains/material/api.ts`
  - 新增 `MaterialHealthReport` / `MaterialHealthIssue` / `MaterialHealthRow` 只读模型；
  - 新增 `materialHealthApi.report(projectId)`，消费 B2 的 `blueprint/check/report`。
- `src/components/MaterialHealthCard/MaterialHealthCard.vue`
  - 项目详情展示阶段、指标、材料应付/实际成本、成本不完整、利润为空、校核项和聚焦行；
  - 提供刷新、材料计划、主材作业单入口；
  - 加载、空态、失败态与重试。
- `src/components/MaterialSheetContextGuide/MaterialSheetContextGuide.vue`
  - 解释作业单当前状态与下一步；
  - 解释测量、配送/安装、要求到场/实际到场的时间口径；
  - 标出模板必填测量字段、未定价/部分结算、已进入采购锁定与取消边界。
- `src/subpackages/project/pages/material-sheet-detail.vue`
  - 接入上下文说明卡。
- `src/subpackages/project/pages/material-sheet-create.vue`
  - 增加材料行来源、合同外走增减项、模板字段、作业单不承载价格说明。
- `src/subpackages/project/pages/detail.vue`
  - Boss/Store/Finance 项目详情接入材料体检；与财务报告并行读取，体检失败不阻断项目主体。
- `src/domains/material/meta.ts`
  - 新增纯解释型 `materialSheetGuidance` 与时间口径常量。
- `src/core/errorHelp.ts`、`src/content/helpCenter.ts`
  - 新增“作业单时间语义”和“作业单取消边界”帮助主题及错误匹配。

## B3 验证

- `npm run type-check` → PASS；
- `npm run build:mp-weixin` → PASS；
- `npm run check:ui` → PASS；
- `npm run check:subpackages` → PASS（主包 894877 B / 最大分包 643571 B / 总包 3583639 B）；
- 微信开发者工具 automator：
  - 项目详情材料体检真实数据 PASS；
  - 作业单详情上下文说明 PASS；
  - 建单页来源说明 PASS；
  - 帮助主题 PASS；
- H5 390×844：
  - 项目详情、作业单详情、建单页无横向溢出；
  - 作业单详情显示模板必填提示；
  - 建单页显示行来源与价格边界；
  - 正常页面 Console Errors = 0；
- 失败态：H5 网络拦截 `blueprint/check/report` 返回 500，页面显示“材料业务暂不可确认”与重试。

## B3 证据与清理

- 证据目录：`docs/product/domains/material/evidence/B3/`；
- OCR 文本：`evidence/B3/ocr-text.txt`；
- 临时 Tile 草稿已删除；
- 本地造数账号密码哈希临时重置后已恢复；
- 无 DDL、无迁移、无生产发布。

## B4 范围

- Batch：B4
- 垂直切片：只读材料业务助手运行时；
- 角色：登录员工（数据范围继承），成本类实时数据仅 Boss/店长/财务；
- 不含：UI 页面新增、LLM 自主写入、审批/付款/删除/改权限、DDL、正式小程序发布。

## B4 后端变更

- 新增 `material/assistant/ask` Provider：
  - `MaterialAssistantQuery`、`MaterialAssistantAnswer`、`MaterialAssistantCitation`、`MaterialAssistantToolCall`；
  - `MaterialAssistantContext`：意图识别、蓝图检索、实时项目/作业单工具、确定性回答；
  - `MaterialAssistantIntentResolver`：规则基线意图识别；
  - `MaterialAssistantKnowledge`：读取发布蓝图与 50 个黄金问题；
  - `MaterialAssistantAuditContext`：复用既有 `AiUsageContext` 记录调用元数据，不保存问题/回答正文。
- 读取真实上下文：
  - `ProjectAccessContext.requireReadableProject`；
  - `MaterialSheetContext.info` + `MaterialSheetTemplateContext.requiredHeaderFields`；
  - Boss/店长/财务使用 `MaterialHealthContext`；Designer/PM 使用 `MaterialPlanWorkbenchContext`，不读取成本；
  - 跨项目/跨企业由既有数据范围返回 404。
- 无新增实体、无 DDL、无迁移。

## B4 验证

- 50 个黄金问题全部命中预期意图，50/50 带证据或明确拒答，全部 `readOnly=true`、审计 RECORDED；
- 真实项目 `235520576473530368` 材料体检回答包含阶段、计数、校核项、成本未完整和利润 `null`；
- Designer 本人项目只返回履约状态，不读取成本；跨项目 404；
- Boss 跨企业项目 404；
- 越界审批请求拒答；未知问题 `NO_EVIDENCE`；
- 重复 `requestId + idempotencyKey` 返回同一审计记录；
- 定向测试与 `mvn -q -DskipTests compile` PASS。

## B4 证据

- `docs/product/domains/material/evidence/B4/golden-questions.json`
- `golden-question-results.json`
- `api-samples.json`
- `audit-summary.json`
- `test-summary.json`
- `README.md`

---

# B5 App 单端材料下单与结算闭环

## 范围

- Batch：B5
- 目标：App 内完成 `MaterialPlan → MaterialOrderSheet → 提交请购 → App 下单 → PurchaseOrder/PurchaseOrderItem → Payable → GoodsReceipt → MaterialSettlement`；
- 旧“材料计划直接采购”路线下线；
- 不新增业务对象、状态机、金额公式或 DDL；
- Web 不作为必经节点。

## 后端变更

- `material/context/MaterialSheetContext.java`
  - `order(...)` 改为一次调用按供应商生成 **1 张采购单 + N 条采购明细**；
  - 增加 `Submitted/Pricing/PartiallyOrdered` 状态门禁、同一作业单行去重、作业单行剩余数量校验；
  - 支持部分行下单与 `purchasePrice=null` 无价下单；
  - 采购单头 `priceState` 表达 `Priced/Unpriced/Mixed`；
  - 作业单只有所有行数量全部下单后才进入 `Ordered`，部分数量保持 `PartiallyOrdered`；
  - 到场回写改为按 `PurchaseOrderItem` 汇总，不再用采购单头跨明细错误汇总。
- `project/context/MaterialPurchaseContext.java`
  - `priceState=Mixed` 与 `Unpriced` 均形成暂估应付，避免混合价被误判为确定应付。
- `material/context/MaterialSettlementContext.java`
  - 结算候选继续按采购明细取数；
  - 增加当前企业 + 当前供应商的采购明细归属校验，阻断跨租户/跨供应商结算；
  - 店长结算候选/确认/列表/详情统一限制本店范围；
  - 结算候选材料名支持目录项、合同行、报价行回退；
  - 撤销结算时，全部行有采购价的应付回到确定金额，只有仍有未定价行时才保留暂估。
- `material/service/settlement/*`
  - 结算单投影补 `supplier`，并在 Page/Info/Confirm/Cancel 内初始化供应商摘要，保证关闭 OSIV 时序列化稳定。
- 删除旧直采 Provider：
  - `project/purchase/save`
  - `project/purchase/batch`
  - 对应 `PurchaseCreationPolicyTest` 删除；历史采购单查询/到场 Provider 保留。
- `material/context/MaterialPlanWorkbenchContext.java`
  - 待下单下一步改为“从主材作业单下单”，不再提示直接采购。

## App 变更

- 新增 `/subpackages/project/pages/material-sheet-order`
  - 选择供应商、交付方式、到场时间；
  - 按作业单行填写本次数量与可空采购单价；
  - 无价行明确提示形成暂估应付。
- 新增 `/subpackages/finance/pages/material-settlement`
  - 待结算明细按供应商/采购单分组；
  - 逐单结算、月结、撤销结算；
  - 未定价行必须填结算价；已结算明细不再进入候选集。
- `material-sheet` 页面
  - 支持项目内视图与全公司财务工作台；
  - 增加“待财务下单 / 建单测量中 / 已进入采购”分组；
  - 财务可直接从待下单卡片进入 App 下单。
- `material-sheet-detail` 页面
  - 提交请购后给 Finance/Boss/StoreManager 显示“去下单”；
  - 展示剩余未下单行数。
- `material-plan-workbench`、`supply/index`
  - 移除材料计划直采、合并下单、从材料计划创建采购单入口；
  - 保留需求/进度/历史采购/到场；
  - 增加主材下单与主材结算入口。
- 删除页面/API：
  - `material-batch-order.vue`
  - `supply/purchase-edit.vue`
  - `supply/material-plan-picker.vue`
  - `material-plan-picker.ts`
  - `materialBatchPurchaseApi`、`materialApi.savePurchase/orderPurchase/approvePurchase`
- 新增模型/API：
  - `SettlementPreviewRow`、`MaterialSettlement`；
  - `materialSettlementApi.preview/list/info/confirm/cancel`；
  - `materialSheetApi.order`。

## 验证

- 后端：`mvn -q test` → **854 / 0 / 0 / 0 PASS**；
- 后端定向：`MaterialSheetOrderFlowTest`、`MaterialGoodsReceiptFlowTest`、`MaterialSettlementFlowTest`、`MaterialSheetLineValidationTest` PASS；
- 关键新增用例：
  - 同一供应商多行生成 1 张采购单 + 多条采购明细；
  - 混合价采购单形成暂估应付；
  - 跨企业采购明细结算被拒绝；
  - 到场按采购明细推进作业单状态。
- App：
  - `npm run verify:mp-weixin` → PASS（含选择器、动作、财务 IA、数字输入、UI、类型检查、构建、分包）；
  - `npm run type-check` → PASS；
  - `npm run build:mp-weixin` → PASS；
  - `npm run check:ui` → PASS；
  - `npm run check:numeric-inputs` → PASS；
  - `npm run check:subpackages` → PASS（主包 922026 B / 最大分包 651400 B / 总包 3608914 B）。
- 真实 UI 走查（2026-10-17）：
  - 微信开发者工具 2.02.2609231，项目根 `dist/dev/mp-weixin`，完成主材下单工作台、作业单下单、作业单详情、主材结算、结算单空态、材料计划工作台、供应商协作 7 页的真实渲染；
  - H5 390×844 完成主材下单工作台、作业单下单、作业单详情、主材结算、材料计划工作台、供应商协作 6 页渲染；
  - “去下单”实际进入 `subpackages/project/pages/material-sheet-order`；结算单 tab 实际进入空态；“未定价/可空单价/成本待结算”文案正确；旧材料计划直采入口不可达；
  - DevTools 各页宽度 375、无横向溢出；H5 页面 `scrollWidth=innerWidth=390`，业务 Console Errors 为 0；
  - 发现并修复 `B5-UI-001`：材料计划工作台模板孤立引号导致 DevTools 编译失败，删除后重新构建通过；
  - 证据：`evidence/B5/README.md`、`evidence/B5/browser-metrics.json`、`evidence/B5/ui-text.txt`、`evidence/B5/screenshots/`。
- 未执行：
  - App 真机全链数据验证；
  - B6 独立验收（本实现不得自批 `ACCEPTED`）。


---

# B6 独立验收记录

- 独立验收任务在干净提交上执行：后端 `af12b9f`、App `225b963`；
- 执行 6 个角色账号、127 项黑盒/真实页面检查；结果 **121 PASS / 6 FAIL**，结论 **FAIL**，不支持 `ACCEPTED`；
- 正向主链、旧路线下线、50 黄金问题、跨端读数与 H5 UI 均通过；
- 开放缺陷：
  - `B6-DEF-001` Designer/PM 可读作业单成本字段（P1）；
  - `B6-DEF-002` Designer/PM 可读采购金额与供应商（P1）；
  - `B6-DEF-003` Designer/PM 可读结算金额与供应商（P1）；
  - `B6-DEF-004` 重复下单生成重复采购/应付（P1）；
  - `B6-DEF-005` 未到场采购明细可结算（P1）；
  - `B6-DEF-006` 暂估应付读模型显示 0 且不暴露 estimated（P2）；
- 临时供应商、作业单、采购/到场/结算数据已清理并逐列恢复；本地测试密码哈希已恢复；
- 证据：`evidence/B6/`；没有实现修复、DDL、生产发布或正式小程序动作。


---

# B6.1 独立验收缺陷修复

- Batch：B6.1；目标只修复 B6-DEF-001~006，不新增业务规则或 DDL。
- 权限裁剪：
  - 新增 `MaterialCommercialPrivacyContext`：Boss/StoreManager/Finance 保留商业字段；Designer/PM 的作业单响应移除报价成本/采购价/结算价，采购响应移除金额、单价、供应商与合同敏感字段；
  - `material/sheet/info|page` 与 `project/purchase/page` 在 `@Transactional(readOnly=true)` 中裁剪，先通过 `ProjectAccessContext.requireEmployee` 恢复完整登录员工，避免 `@User` 仅带索引导致 Finance 被误裁剪；
  - `material/settlement/page|info` 统一调用 `MaterialPurchaseContext.requireOperator`。
- 幂等：
  - `material/sheet/order` 增加稳定 `idempotencyKey` 入参、作业单+供应商分布式锁和确定性采购单号；
  - 无客户端键时以业务 payload + 当日日期生成 fallback；重复请求返回已生成结果，内容不一致返回 409；
  - 不新增表或迁移。
- 结算门禁：
  - 未到场（`receivedQuantity<=0`）的采购明细不进入 preview；
  - confirm 拒绝未到场明细，结算数量默认取到场数量且不得超过到场数量。
- 暂估应付：
  - `finance/payable/page` 暴露 `estimated`、`settledAmount`；
  - App/Web 应付列表对 `estimated=true` 显示“待定价/待结算”，不显示 0 元，不允许在定价前付款。
- 验证：
  - 后端全量 `mvn -q test` → **871 / 0 / 0 / 0 PASS**；
  - 定向覆盖隐私裁剪、重复下单、未到场结算、暂估应付投影；
  - App `type-check`、`build:mp-weixin`、`check:ui`、`check:subpackages` PASS；
  - Web `type-check`、`build` PASS；
  - 真实 UI：Web 4175 / 1440×900、App H5 8080 / 390×844；Console Errors 0，横向溢出 0。
- 证据：`evidence/B6.1/README.md`、`defect-verification.json`、`api-samples.json`、`browser-metrics.json`、`screenshots/`。
- 边界：临时作业单/采购/应付已删除，物料计划恢复 `ReadyToOrder`，测试账号密码哈希已恢复；未执行 DDL、生产、正式小程序发布；未宣称 `ACCEPTED`。


---

# B6.2 独立复验记录

- 独立任务在精确提交上执行：后端 `8cc1d11`、App `46a5be4`、Web `04ba3b4`；
- 150 项：**149 PASS / 1 FAIL**；原 B6 六项缺陷 **6/6 PASS / CLOSED**；
- 新缺陷 `B62-DEF-001`（P2，OPEN）：多供应商拆单后作业单价格投影仍为 `Unpriced`，未反映 Priced + Settled 的组合；
- 采购/应付金额事实、权限、幂等、到场门禁、50 黄金问题、旧路线、跨端与 UI 回归均通过；
- 实际页面证据：Web `/finance/payables` 1440×900、App H5 390×844；暂估文案和禁用付款正确；
- 临时业务数据、计划状态、项目经理和测试凭据已恢复；
- 证据：`evidence/B6.2/`；结论不支持 `ACCEPTED`，下一批 B6.3 修复 B62-DEF-001。


---

# B6.3 B62-DEF-001 修复

- 新增 `MaterialSheetPriceProjectionContext`，按材料计划批量读取采购明细并聚合价格状态：
  - 全部 Priced → Priced；
  - 全部 Unpriced → Unpriced；
  - 全部 Settled → Settled；
  - 任一 Mixed 或 Priced/Unpriced/Settled 组合 → Mixed；
- `material/sheet/info|page` 读取时重算行与单状态，不回写；
- 下单后、结算确认后、撤销结算后同步冗余投影；
- 修正单级 `refreshSheetPriceState`，Mixed 不再被误判为 Unpriced；
- 不新增实体/表/DDL，不改变采购、应付、结算金额公式。
- 验证：
  - 后端全量 `mvn -q test` → **876 / 0 / 0 / 0 PASS**；
  - 定向：`MaterialSheetPriceProjectionContextTest`、`MaterialSheetOrderFlowTest`、`MaterialSettlementFlowTest` PASS；
  - 真实 H5 390×844：拆单+部分结算页面显示“部分已结算”，无横向溢出，Console Errors 0；
  - 证据：`evidence/B6.3/`。
- 临时作业单/采购/到场/结算已清理，材料计划恢复 ReadyToOrder，测试密码哈希已恢复；未发布。


---

# B6.4 独立复验记录

- 精确提交：后端 `b66a33b`、App `46a5be4`、Web `04ba3b4`；
- 140 项：**139 PASS / 1 FAIL**；`B62-DEF-001` 聚焦 23/23 PASS，后端价格投影关闭；
- 新缺陷 `B64-UI-WEB-SHEET-01` P2：Web 主材下单页未展示 Mixed 价格状态；App H5 正确显示“部分已结算”；
- 回归：主链、权限、幂等、到场门禁、暂估应付、50 黄金问题、旧路线、跨企业通过；
- 证据：`evidence/B6.4/`；不支持 `ACCEPTED`，下一批 B6.5。


---

# B6.5 Web Mixed 状态展示修复

- `FinanceMaterialOrderingView` 新增服务端 `priceState` 映射：
  - Priced → 已定价；
  - Unpriced → 未定价 · 成本待结算；
  - Mixed → 部分已结算 · 成本待结算；
  - Settled → 已结算；
- 在待下单/已进入采购作业单行显示价格状态 pill，不改变履约状态标签；
- 未改后端、权限、金额/成本口径，不做 DDL；
- 验证：Web type-check/build PASS；复用 B6.4 黑盒脚本 140/140 PASS；Web 1440×900 Mixed 文案真实可见、无横向溢出、Console Errors 0；
- 证据：`evidence/B6.5/`；B6.6 独立复验 157/157 PASS，`B64-UI-WEB-SHEET-01` CLOSED。

---

# B6.6 独立复验记录

- 性质：独立黑盒与真实浏览器复验；执行任务未修改实现，也未参与 B6.5 修复；
- 精确提交：后端 `b66a33b`、App `46a5be4`、Web `3ef4974`；
- 结果：157 项 **157 PASS / 0 FAIL / 0 BLOCKED**；
- `B62-DEF-001` **CLOSED**（23/23 聚焦检查）；`B64-UI-WEB-SHEET-01` **CLOSED**；
- B6 原六项缺陷回归 6/6 PASS；50 个黄金问题 50/50 PASS；权限、幂等、未到场结算门禁、暂估应付、旧路线、跨企业、App/Web UI 全部 PASS；
- Web 1440×900 / App H5 390×844 无横向溢出，Console Errors 0；
- 环境瞬态 `ENV-B66-001`：首轮 Vite `504 (Outdated Optimize Dep)`，同问题重试 1 次后全 PASS，不属于产品缺陷；
- 证据：`evidence/B6.6/`（README、summary.json、verification-results.json、ui-metrics.json、attempts.json、environment.json、run-b66-verification.mjs、9 张截图）；
- 结论：独立验收 PASS，满足 `ACCEPTED` 证据条件；独立验收方未代替用户做最终正式接受。
