# 装得清产品体系基线 · 实现记录

## 本批范围

- Batch：`B2C-1`
- 垂直切片：`UOV-V2` 后端报价 REGION/TYPE 双投影
- 不包含：App/Web/业主页面接线、合同/项目事实链、PDF 分组生成、DDL、权限放宽、正式发布

## 变更

### 后端

- `QuoteViewContext`：同一原始行生成 REGION/TYPE 双投影；TYPE 跨区域按服务端合并键合并，保留来源行/区域并稳定排序。
- `Quote.typeGroups`：新增瞬态 TYPE 分组投影；`QuoteGroup.groupMode` 新增分组方式标识。
- `QuoteViewContract`：分类为空时按 `ItemType` 展示名规范化，避免空分类和显式同名分类拆成两组。
- `QuoteLineRepository`：预取 `space/components`，避免投影在无事务边界逐行懒加载。
- 报价详情、设计师详情、项目经理报价详情、报价保存、批量保存和工作簿导入响应白名单：增加 `typeGroups/groupMode/typeRowId/sourceLineIds/sourceRegionIds/sourceRegionNames/sourceCount`。
- 项目经理视图保留 REGION/TYPE 分组事实，但按 `PROJECT` 场景继续不返回原始 `items`。

### App / Web

- 无代码变更。App/Web 仍消费旧字段；UOV-V3 才切换统一投影消费并删除前端合并/回退。

### 迁移 / 配置

- 无 DDL。
- 无生产配置变更。

## 契约行为

1. REGION 与 TYPE 来自同一 `quoteId + version` 的同一批原始报价行。
2. TYPE 固定 `ItemType → Category`；空分类回退类型展示名。
3. TYPE 合并键使用 `QuoteViewContract`：目录项身份、类型/分类/名称/品牌/规格/单位/计价方式/销售单价/固定总价/角色/差异类型与状态/是否包含/折扣/工艺/组件销售签名。
4. 数量、金额、成本按原始行权威快照求和；不使用合并单价重算。
5. `typeRowId` 由 `quoteId + version + canonicalMergeKey` 生成；来源行按 `itemId` 升序，来源区域按空间排序稳定去重，空区域归“综合项目”。
6. TYPE 行仅作展示；原始行 ID 和 quoteId/version 仍是编辑、接受和下游引用依据。

## 测试与证据

- 定向契约：`QuoteViewContextTest`、`QuoteViewContractTest` 通过。
- 报价相关回归：9 个测试类、52 个测试，0 失败、0 错误、0 跳过。
- 覆盖：REGION/TYPE 来源、跨区域合并、差异状态拆行、来源排序、空区域归组、组件销售签名与成本汇总、经理场景无原始行、既有报价保存/工作簿/成本/PDF 回归。
- 构建：`mvn -o -q -DskipTests package` 通过。
- 验证方式：因其他并行会话共享 workspace `target/`，使用隔离源码副本执行测试和打包，避免构建目录竞争。
- 真实页面/真机：未执行；UOV-V2 是后端契约切片，不得冒充 UI 验收。
- 部署/重启：未执行；U2 已完成重新对账，当前为 `RECONCILED / INDEPENDENT_ACCEPTANCE_PENDING`。

## 风险与未闭环

- `GAP-065` 仍打开；前端仍可能自行合并或回退分组。U2/UOV-V2 必须先独立验收，之后才能启动 UOV-V3 切换消费。
- 场景字段/动作的完整跨端对齐仍需 UOV-V3；本批只完成服务端分组和响应字段基础。
- 合同事实链、项目 PHASE、PDF `version + groupMode` 归档仍分别属于 UOV-V4/V5/V6。
- RQ-04/RQ-05 仍为领域级未决项；如 UOV-V3 涉及新的权限或外部身份语义，必须先回到业务裁决。

## COST-D1 项目成本三态投影契约（ACCEPTED，2026-10-23）

- 状态：`PASS / ACCEPTED（仅 COST-D1）`。前两次独立验收 FAIL 证据保留，第三次独立复验通过；PI-017 整体仍 `overall PENDING`，COST-D2~D5 未授权，`GAP-071` OPEN。
- 决策依据：`ANS/T12-BIZ-20261022-017`、`DEC-092`、`BR-058`、`MET-002/006`、`SOL-002`、`GAP-071`。
- changed_files：`ProjectCostProjection`、`ProjectCostProjectionTest`；`ProjectCostStage` 沿用既有三态枚举。
- 独立验收缺口修复：
  - `formulaVersion` 只接受唯一常量 `ProjectCostProjection.CURRENT_FORMULA_VERSION = project-cost-projection-v1`，未来生产方必须使用该常量。
  - `sourceSnapshotVersions` 必须非空，键非空且版本值为正；record compact constructor 复制并返回不可变 Map。
  - 任一阶段 revenue `<= 0` 时 margin 必须为 null；完整阶段有 revenue 时校验 profit=revenue-cost，正收入时校验 margin=profit/revenue×100%（允许 0.01 个百分点舍入差）。
  - revenue 无法确认（null）时 profit 与 margin 必须同时为 null；第二次复验发现的 `complete=true + revenue=null + profit!=null` 旁路已关闭。
  - 新增 `dynamicCostProvisional` 表达 Dynamic P2 暂估；Estimated/Final 携带 true 时拒绝。
- 兼容：预计阶段 `actualCost=null`；动态/最终阶段映射当前阶段成本；旧利润/成本字段必须结合 `costStage`。
- D1 新契约测试：`ProjectCostProjectionTest` 19/19，0F/0E/0S，退出码 0。
- 相关回归：`ProjectCostProjectionTest` 19/19、`ProjectCostContextTest` 14/14、`ProjectCostUnpricedCompletenessTest` 3/3，合计 36/36。
- 编译：Oracle JDK `22.0.1+8-16`，`mvn -o -q -DskipTests compile` PASS。
- 全量：Oracle JDK `22.0.1+8-16`，`mvn -o -q clean test` PASS，261 个 Surefire 报告，1,069 tests / 0 failures / 0 errors / 0 skipped，退出码 0。
- 证据边界：D1 新合同证据为 `ProjectCostProjectionTest` 19/19；`ProjectCostContextTest` 与 `ProjectCostUnpricedCompletenessTest` 是未修改旧实现的回归基线，不能证明生产侧三态规则已落地。
- 独立证据：`docs/acceptance/runs/20261023-cost-d1-independent/README.md`；快照 `/tmp/cost-d1-reaccept3.RbOikn`。
- 未做：未接 Provider、未替换 `ProjectCostContext`/`FinanceReportContext` 生产公式、未改权限/App/Web/结算/历史快照、无 DDL；`GAP-071` 保持 OPEN，COST-D2~D5 未实现。

## B2E / PI-20261022-016 / FIN-WRITE-D2

- 批次：`B2E-PI-016`
- 垂直切片：按 ID 写入目标门店范围统一
- 业务依据：`ANS/T12-BIZ-20261022-010`、`DEC-089`、`BR-055`
- 变更：应收/应付/付款/员工工资提成发放/退款/结算/质保释放/台账更新/凭证生成统一叠加目标数据范围；付款和退款继承来源门店；多应付付款和多流水凭证要求同门店；员工工资付款与 Ledger 继承来源应付门店；空门店历史来源保持空归属。
- 测试：隔离副本中 `FinanceStoreScopeHttpAcceptanceTest#idWritesRejectCrossStoreTargetsAndMixedStoreVouchers` 1/1 PASS；`ReceiptRefundContextTest` 2/2 PASS；完整 D2 HTTP 回归此前 21/21 PASS。
- 证据：`docs/acceptance/runs/20261022-fin-write-d2/01-dev-evidence.md`、`http-targeted-report.txt`、`receipt-refund-report.txt`。
- 风险：`PaymentIdempotencyTest` 并发同键用例存在时序波动，待独立复验确认；无真实 MySQL、浏览器、真机证据。
- 状态：`DEV_VERIFIED / FULL_TEST_PASS / INDEPENDENT_REACCEPTANCE_PENDING`，未发布。

### FIN-WRITE-D2 / COST-D1 全量门禁复核（2026-10-23）

- 隔离快照：`/tmp/full-test-d2-cost.AFPSq5/repo`，包含当前 `pom.xml + src + docs/design/migrations + docs/operations/releases`。
- 命令：Java 22 `mvn -o -q clean test`。
- 结果：`253` 个测试类、`1027` tests、`0 failures`、`0 errors`、`0 skipped`、退出码 `0`。
- 技术收口：删除 `QuoteLineRepository` 未使用的五属性宽查询；`ExpenseRepository` 将详情/分页的大图谱拆成主对象窄查询与人员辅助查询；费用生命周期测试 8/8 PASS。
- 闭环夹具：`DecoboonClosedLoopTest` 与 `DesignLifecycleClosureTest` 显式传入 `STANDARD_30_40_30` / `RESIDENTIAL_STANDARD_8`，不再依赖“没有默认模板时自动猜测”的旧测试路径。
- 证据：`docs/acceptance/runs/20261022-fin-write-d2/04-full-test-green.md`。
- 边界：这是开发侧全量门禁证据；仍需独立复验人基于新证据重新判定 D2 整体验收，不能由开发自批。

## 三端收敛验证（2026-10-23）

- 快照：`/tmp/zhuangdeqing-converge.JgGxh1`
- 后端：Java 22 `mvn -o -q clean test`，`261` 个测试类、`1056` tests、`0 failures`、`0 errors`、`0 skipped`，退出码 `0`。
- App：`npm run verify:mp-weixin` PASS；TypeScript、小程序构建、分包体积/分包规范、文案、导航、权限、财务 IA、Owner Preview、数字输入、UI 统一化均 PASS。
- Web：TypeScript + 生产构建 + 动作/权限/状态/工作台/页面系统/表单/概念/枚举/指标/门店范围检查全部 PASS。
- 修复：`GoodsReceiptRepository` 将错误的 `purchaseOrder` 派生属性改为 `purchase`；支付并发测试改为验证同一 `paymentId` 和唯一副作用；App 修复研发型文案、TeamLeader 预览契约、数值输入规范与生命周期原生模态。
- 证据：`docs/acceptance/runs/20261023-three-surface-convergence/README.md`。
- 边界：仍是开发侧三端收敛验证，不代表独立验收或发布通过；当前工作区改动仍须分域提交。

## EXPENSE-D1 · 企业费用与员工报销对象与审批

- 状态：`D1_ACCEPTED_BACKEND_UI_BLOCKED`
- 业务依据：`DEC-091`、`BR-057`、`ANS/T12-BIZ-20261022-012`
- 后端：四表实体/Repository/Provider、费用分类初始化、状态机、版本快照、文件授权、动作投影和防自审。
- 迁移：`126-企业费用与员工报销D1.sql`，`MIGRATION_END=126`。
- App：费用列表、草稿编辑、证据上传、详情、审批/退回/重提。
- Web：费用列表、表单/详情抽屉、审批/退回/重提。
- 验证证据：`docs/acceptance/runs/20261022-expense-d1/01-dev-evidence.md`
- 未实现：应付、付款、流水、凭证、利润。
- 未完成：真实 Web/App 浏览器/真机、真实 OSS；后端 HTTP + 隔离 MySQL 已独立验收。

## EXPENSE-D2 · 审批应付、付款核销与关闭

- 状态：`D2_PASS / EXPENSE-D2 ACCEPTED`。
- 业务依据：`DEC-091`、`BR-057`、`ANS/T12-BIZ-20261022-012`。
- 实现：审批唯一生成 `Payable(Expense)`；部分/全额付款；`Payment + PayableSettlement + Ledger(Expense)`；付清关闭；并发幂等与防自付。
- P1-001 修复：`finance/project/report|page` 将 `Property` 限制为 `propertyId/community/address`；项目财务详情预取 `payable.payeeEmployee/property` 与 `receivable.property`，消除 `open-in-view=false` 下的 JSON 截断。
- P1-002 修复：新增财务项目访问专用查询 `JOIN FETCH customer/customer.store`，`resolveProject` 在只读事务内完成门店范围判定；本店店长/门店财务和 BOSS/总部财务合法访问，跨店/跨企业 404，无数据库异常。
- 验证：隔离 D2 HTTP 18/18 PASS；Java 22 主源码 2086 文件编译 PASS；Java 22 隔离完整仓库 261 suites / 1069 tests，0 failure/error/skip。
- 证据：`docs/acceptance/runs/20261022-expense-d1/04-dev-evidence-d2.md`、`evidence/d2/reaccept-p1-002/`。
- 独立验收：第三轮 18/18 PASS，P1-001 PASS、P1-002 CLOSED；真实 Web/App/OSS 仍 BLOCKED。

## EXPENSE-D3 · 付款凭证来源快照与历史回读

- 状态：`PASS / ACCEPTED`；迁移 130 隔离应用/回滚 PASS；P1-003 CLOSED。
- 业务依据：`DEC-091` §8、`BR-057` §7、`T12-BIZ-20261022-012`。
- 范围：已付款费用生成凭证；来源链 `VoucherRecord -> Payment -> Payable(Expense) -> Expense`；费用/审批/付款/模板版本/PDF 哈希快照；历史 PDF 只读回读。
- 迁移：`130-企业费用D3凭证来源快照.sql`，在 `db_voucher_record` 增加 `source_type/source_id/source_snapshot` 与 `(com_id, source_type, source_id)` 索引；回滚保留快照备份表。
- 实现：费用付款凭证来源快照、`E-TRAVEL/E-REIMBURSEMENT/P-EXPENSE` 模板路由、同付款重打同号同哈希、费用详情凭证摘要与 `expenseId` 过滤。
- 页面：费用详情、凭证列表/详情和 App/Web 同源展示；不新增凭证权限。
- 任务书：`docs/product/domains/product-system-baseline/EXPENSE-D3-付款凭证来源快照与历史回读研发任务.md`。
- 验证：D3 第二轮独立 13/13；完整测试 attempt2 265 suites / 1078 tests / 0/0/0；迁移130 apply/rollback PASS；App/Web 构建 PASS。
- 门禁：D3 ACCEPTED；D4 计划待 PI-017/GAP-071，D5 继续受 `GAP-070` 阻塞。

## EXPENSE-D4 · 项目/企业归属与手工成本边界（计划）

- 状态：`PLANNED / BLOCKED_PI_017`；未改代码或 DDL。
- D4 需要与 `PI-20261022-017` 的 `ProjectCostContext` 成本公式对齐，确认项目费用应付与估算/实际成本的组合方式。
- 同时需要冻结手工 `ProjectIncidentalCost`/`CompanyExpense` 防复制规则和错误码。
- 当前阻断：PI-017 overall PENDING、GAP-071 OPEN；D4 未授权编码，D5 继续 BLOCKED。
- 任务书：`docs/product/domains/product-system-baseline/EXPENSE-D4-项目企业归属与手工成本边界研发任务.md`。

## COST-D2 · 项目成本来源、分类与完整性（计划）

- 状态：`PLAN_READY / AUTHORIZATION_REQUIRED`；未改生产公式或 DDL。
- 业务依据：`DEC-092`、`BR-058`、`T12-BIZ-20261022-017`。
- 范围：P1/P2/P3 三态、材料/工人/工资/提成/项目费用/其他应计分类、完整性、项目 Expense 与手工零星成本二选一、移除 `max(actualCost,payableCost)`。
- 当前门禁：等待用户明确授权 COST-D2 编码；COST-D3~D5 未授权，`GAP-071` OPEN，不发布。
- 任务书：`docs/product/issue-governance/dev-tasks/DEV-PI-20261022-017-D2.md`。

## U3 修复切片（2026-10-23）

- 状态：`DEV_VERIFIED / REMEDIATED_PENDING_REACCEPTANCE`
- P1-01：App 行金额、业主小计/税额/总额缺失不补 0；后端 journey `money(null)=null`。
- P1-02：App `hasSourceLines` 区分空清单与分组暂不可确认；Web `toSheetGroups` 在原始行存在而分组缺失时返回 `null`，页面显示重新读取。
- P2-01：合同读取失败/读取中时，队列、Hero、阶段轨道、合同金额和签约进度统一显示暂不可确认/读取中，不显示“待发起合同”。
- 验证：后端 7 类 18/18；App type-check + mp-weixin build；Web type-check + production build；三端 diff check。
- 证据：`docs/acceptance/runs/20261023-unified-object-view-u3-remediation/README.md`。
- 未完成：独立复验；未修改 U4/U5/U6，不宣称 U3 PASS。

## U5 项目 PHASE 准备（历史，2026-10-23）

- 当时状态：`PREPARE_ONLY`；准备单已完成，后续 U5-A、U5-B1 已进入 `DEV_VERIFIED`。
- 准备单：`docs/product/domains/product-system-baseline/11-uov-v5-project-phase-plan-v0.1.md`。
- 已映射：来源、售前、签约、履约、变更、验收、财务、售后 8 阶段及现有项目/App/Web/业主数据来源。
- 已冻结：建议复用 `BusinessFactNode`；在 `project/project/info` 增加瞬态事实索引；节点排序/权限/导航/折叠规则。
- 当时未完成：U5 后端聚合、App/Web/业主接线、契约测试、真实页面和独立验收。
- 历史门禁：U1/U2/U3/U4 已 PASS；当时 U5 实现未授权，U6 未启动。

## U5-A 项目事实契约（2026-10-23）

- 状态：`DEV_VERIFIED / ACCEPTANCE_PENDING`。
- 新增 `ProjectFactViewMode`、`ProjectFactTrace`、`ProjectFactTraceContract`。
- `Project` 增加瞬态 `factTrace` 字段，尚未加入 Provider 白名单，未输出接口。
- 固定 8 阶段、PHASE 默认视图、稳定排序、业主可见性、来源缺口和绑定校验。
- 测试：`ProjectFactTraceContractTest` + `BusinessFactContractTest` 共 9/9 PASS；JDK22 package PASS。
- 证据：`docs/acceptance/runs/20261023-unified-object-view-u5a/README.md`。
- 未完成：U5-B 聚合、页面接线、U5 独立验收；无 DDL。

## U5-B1 项目当前事实聚合（2026-10-23）

- 状态：`DEV_VERIFIED / ACCEPTANCE_PENDING`。
- 新增 `ProjectFactTraceContext`，接入 `project/project/info` 的瞬态 `factTrace`。
- 覆盖当前客户/房屋/项目来源、量房/设计/报价、报价接受/合同、项目/阶段/任务/日报/选材/材料/采购/到货、变更、验收、应收/收款、成本、售后意见。
- BOSS/店长/财务才生成 Cost/Payable/Payment；普通角色不生成成本事实。
- 来源缺失进入 `sourceGaps`，不生成占位对象。
- 测试：4 个契约/聚合测试类共 12/12 PASS；JDK22 package PASS。
- 证据：`docs/acceptance/runs/20261023-unified-object-view-u5b/README.md`。
- 当时未完成：历史版本折叠、利润/结算节点、页面接线和独立验收；无 DDL。


## U5-B2 历史折叠与利润/结算节点（2026-10-23）

- 状态：`DEV_VERIFIED / ACCEPTANCE_PENDING`。
- 契约：`BusinessFactNode.current` 明确当前/历史；`ProjectFactTraceContract.nodeKeyGroups()` 按稳定 `nodeKey` 折叠。
- 历史：报价、设计、主材选材、采购、合同变更、验收轮次和冲正收款保留来源对象，输出 `current=false` 与历史说明。
- 利润/结算：仅 BOSS/店长/财务且存在 `Settlement` 快照时生成 `ProfitSettlement`；直接继承 `finalAmount/profitAmount/grossMargin`，不重算。
- 成本不完整：结算快照利润为空时输出 `costComplete=false`、利润/毛利率为空。
- 测试：4 个契约/聚合测试类 18/18 PASS；编译和 JDK package PASS。
- 证据：`docs/acceptance/runs/20261023-unified-object-view-u5b2/README.md`。
- 边界：无 DDL、无 Provider 新增、无权限放宽、无金额/成本/利润公式变化；`GAP-071` 仍开放。
- 后续：U5-C App 项目详情 PHASE、U5-D Web 项目执行 PHASE 均已独立验收 PASS；U5-E 仅允许重新对账后准备，U5-F、越权负向和真实页面证据未完成；U6 未启动。

## U5-C App 项目详情 PHASE 视图（2026-10-23）

- 状态：`PASS / ACCEPTED`（第二轮独立验收）。
- 数据契约：App `Project` 接入 `factTrace`，归一化节点、来源缺口、版本、当前/历史、金额、利润和锚点字段。
- 页面：员工项目详情新增“事实”分段；业主端 tab 不变，Owner 投影留 U5-E。
- 交互：固定 8 阶段；当前/异常阶段默认展开；同 `nodeKey` 历史版本卡内折叠；缺口显示必须补齐/可选。
- 跳转：按来源对象复用现有 App 路由；携带 `returnPath/returnAnchor`，返回后重新进入事实分段并定位原节点。
- 验证：`npm run type-check`、`npm run build:mp-weixin`、`npm run check:subpackages`、`npm run check:navigation` 和 UI source guard PASS。
- 已知无关失败：`check:ui` 命中既有 `src/App.vue:66`；`check:owner-preview` 命中既有 TeamLeader 权限差异。
- 证据：`docs/acceptance/runs/20261023-unified-object-view-u5c/README.md`。
- 未完成：真实页面/真机、多机型、越权与 U5-E/F 最终验证；无 DDL、无后端/Web 改动。

## U5-D Web 项目执行 PHASE 视图（2026-10-23）

- 状态：`PASS / U5-D ACCEPTED`（第二轮独立验收；首轮 FAIL 历史保留）。
- 文件：Web project model/API、`ProjectExecutionView.vue`、`ProjectFactPhasePanel.vue`、U5-D source guard，以及项目执行页 4 角色路由/菜单接线。
- 修复：历史事实项绑定 stable DOM anchor，返回历史时展开阶段/历史并滚动定位；Purchase 在合法角色路由接入 `finance-material-ordering`；MaterialPlan/GoodsReceipt 及无路由角色显式摘要-only。
- 验证：独立 SSR/DOM + 角色路由探针 35/35；type-check/client-copy/actions/source guard/Web build PASS；无 DDL、Provider、权限、金额或状态机变更。
- 证据：`docs/acceptance/runs/20261023-unified-object-view-u5d-reacceptance/README.md`。
