# 材料履约与材料业务助手 · 垂直切片计划

## 北向目标

材料计划、选材、作业单、采购、到场与结算形成可追溯闭环，并提供可解释、可校核的业务助手。

## 批次

| 批次 | 垂直切片 | 涉及角色 | 涉及对象 | 后端 | App/Web | 业务校核 | 退出条件 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| B1 | 业务卡核对、结构化蓝图、字段字典 | 产品/Boss/财务/设计/PM | MaterialPlan/Selection/Sheet/Purchase/Receipt/Settlement | 无或只读 blueprint definition | 无 | 现有 OBJ/BR/DEC/SCN 引用 | BLUEPRINT_READY | COMPLETE |
| B2 | Web 蓝图中心 + 项目材料业务体检 | Boss/Store/Finance | 项目/材料/检查 | blueprint runtime/check provider | Web | 选材/下单/到场/成本完整性 | DEV_VERIFIED + UI_REVIEWED | COMPLETE (2026-10-17) |
| B3 | App 业务体检 + 页面解释 | Designer/PM/Boss/Finance | Sheet/Plan/Receipt | assistant context provider | App | 字段/时间/下一步 | DEV_VERIFIED + UI_REVIEWED | COMPLETE (2026-10-17) |
| B4 | 只读材料助手 | 全角色（按权限） | 全部材料对象 | assistant orchestrator/tools | Web/App | 50 黄金问题、8–12 校核 | 权限/反向/证据 PASS | COMPLETE (2026-10-17) |
| B5 | App 单端材料下单与结算闭环 | Designer/PM/Finance/Boss/StoreManager | Plan/Sheet/Purchase/Payable/Receipt/Settlement | sheet/order 编排、PurchaseOrderItem、旧直采下线 | App | 下单/到场/结算/成本完整 | DEV_VERIFIED + UI_REVIEWED + RECONCILED | COMPLETE (2026-10-17) |
| B6 | 独立验收与对账 | independent | 全部 | 无实现 | App/Web | 正向/反向/越权/幂等/边界 | ACCEPTED | FAIL（2026-10-17；121/127） |
| B6.1 | 修复 B6 独立验收缺陷 | Finance/Designer/PM | Sheet/Purchase/Settlement/Payable | 字段投影、角色门禁、幂等、到场门禁、暂估应付投影 | App/Web | 权限/金额/幂等/边界 | DEV_VERIFIED + RECONCILED | COMPLETE (2026-10-17) |
| B6.2 | B6 缺陷修复复验 | independent | 受影响矩阵 + 回归 | 无实现 | App/Web | 独立黑盒复验 | ACCEPTED 或新缺陷 | FAIL（2026-10-17；149/150） |
| B6.3 | 修复 B62-DEF-001 多供应商价格投影 | Finance/Designer/PM | Sheet/Purchase/Settlement | 价格状态与采购明细聚合读模型 | App/Web | Mixed/Priced/Settled/Unpriced | DEV_VERIFIED + RECONCILED | COMPLETE (2026-10-17) |
| B6.4 | B62-DEF-001 聚焦独立复验 | independent | 多供应商拆单 + 回归 | 无实现 | App/Web | 独立黑盒复验 | ACCEPTED 或新缺陷 | FAIL（2026-10-17；139/140） |
| B6.5 | 修复 Web 作业单 Mixed 状态展示 | Finance/StoreManager/Boss | Sheet/Finance | Web 价格状态文案与角标 | Web | Mixed/Unpriced/Settled | DEV_VERIFIED + UI_REVIEWED + RECONCILED | COMPLETE (2026-10-17) |
| B6.6 | Web Mixed 状态聚焦独立复验 | independent | Web 主材下单页 + 回归 | 无实现 | Web/App | 独立黑盒复验 | ACCEPTED 或新缺陷 | COMPLETE（2026-10-17；157/157 PASS，ACCEPTED） |
| B7 | 材料业务助手上下文入口 | Boss/StoreManager/Finance/Designer/PM | Assistant/Project/Sheet/Settlement | 复用 `material/assistant/ask` | App/Web | 上下文/证据/拒答/权限/跨端 | DEV_VERIFIED + UI_REVIEWED + RECONCILED | PENDING |
| B8 | 助手 UI 聚焦独立验收 | independent | Assistant UI | 无实现 | App/Web | 权限/证据/拒答/上下文 | ACCEPTED | PENDING |
| B9 | 体验版/发布准备 | release | App/后端/Web | 构建/发布记录 | App 体验版 | 冒烟/回滚 | RELEASED 或决策包 | PENDING |

## 依赖与顺序

- B1 先于所有实现；没有蓝图层和字段映射，不做页面。
- B2 的检查规则必须复用领域事实，不在页面计算。
- B3 的上下文助手不能直接查库。
- B4 必须依赖 B1 的蓝图、B2 的校核和 B3 的对象上下文。
- B5 完成 App 单端闭环后，B6 必须独立于实现，不能由执行者自验。
- B6.2 确认 6 项关闭并发现 B62-DEF-001；B6.3 修复后 B6.4 确认后端关闭但发现 Web UI P2；B6.5 修复、B6.6 独立复验 157/157 PASS 后，B6 系列 `ACCEPTED`，可进入助手 UI。
- B7/B8 完成材料助手上下文入口及聚焦独立验收；B9 只做体验版，正式版由用户完成。

## 回滚

- 后端/Web 使用 release 目录和 `.previous`；
- App 保留上一体验版；
- 蓝图/助手关闭开关可回退到静态说明；
- 不产生不可逆数据迁移。

## 门禁

- B1 已停在：`BLUEPRINT_READY`，蓝图 v1.0.0 + 对账矩阵已建立；
- B2 已在 2026-10-17 完成 `DEV_VERIFIED + UI_REVIEWED + RECONCILED`：真实浏览器证据见 `evidence/B2/`；
- B2 不宣称独立验收通过；REQ-MAT-001/002/004 的独立验收仍留 B6；
- B3 已在 2026-10-17 完成 `DEV_VERIFIED + UI_REVIEWED + RECONCILED`：微信开发者工具与 H5 证据见 `evidence/B3/`；
- B3 不宣称独立验收通过，跨端黑盒仍留 B6；
- B4 已完成只读助手运行时、50 黄金问题、权限负向和审计证据，停在 `RECONCILED`；
- B5 已在 2026-10-17 完成 App 单端材料下单与结算闭环，旧直采路线下线；真实 DevTools/H5 页面、交互和布局证据见 `evidence/B5/`；B6 独立验收不得由实现者自批。

## B2 实施门槛（PLAN_READY，2026-10-17）

### 垂直切片

只交付一条 Web 只读链路：登录员工从「业务蓝图中心」理解材料计划→选材→作业单→采购→到场→结算的业务对象、状态、字段和来源引用；从「项目核算 → 材料业务体检」看到当前项目由真实 Provider 聚合出的材料进度、异常与下一步。不包含 App、不同步推进助手问答、不做写操作。

### 依赖

- 业务事实：`02-business-sync.md` 的 `OBJ-008 / BR-009 / BR-044 / BR-045 / SCN-021 / DEC-072 / DEC-073`；
- 蓝图：`03-blueprint.json` v1.0.0，节点/字段/检查/UI 绑定均已建立；
- 后端只读能力：复用 `MaterialPlanWorkbenchContext`、`FinanceReportContext`、`ProjectCostContext` 和采购/材料计划数据；
- Web 现有壳层：角色静态路由、`RoleAreaShell`、项目核算页和财务项目报告；
- 权限：业务蓝图与体检仅对 Boss、StoreManager、Finance 开放；项目范围继续由财务访问上下文和材料工作台上下文裁决。

### 后端契约

- `blueprint/runtime/info`：返回材料蓝图运行态（版本、来源、角色、成本可见性、规范化蓝图 JSON）；数据来自随构建发布的蓝图资源，并由测试校验与领域包 `03-blueprint.json` 同源；
- `blueprint/check/report`：入参 `projectId`，返回 `MaterialHealthReport`：项目摘要、计划/选材/下单/到场计数、采购缺供应商/未定价、成本完整性、当前阶段、下一步、校核项和聚焦明细；
- 不新增实体表、不新增 DDL、不改变金额/成本/状态/权限语义。

### Web 页面

- `/business-blueprints`：阶段轴、对象图谱、字段字典、规则/校核/例外、来源引用与缺口；
- `/finance/projects/:projectId`：在既有项目核算页内增加「材料业务体检」区块，展示真实项目材料事实，链接到主材下单、主材结算和蓝图中心；
- 空态、失败态、无权限继续走既有错误/空态组件，不用 0 或空数组冒充完成。

### 回滚

- 后端移除两个只读 Provider/上下文/资源即可，无数据迁移；
- Web 回退到上一静态构建；页面入口是加法，不影响既有财务页面动作；
- 不涉及生产写入、自动审批、付款、删除或权限放宽。

### 退出条件

- 定向测试通过：蓝图资源同源、健康报告计数/成本不完整/角色边界；
- 后端编译与 Web `type-check`/构建通过；
- 真实浏览器渲染 `/business-blueprints` 和 `/finance/projects/:projectId`，至少 1440×900 与 1024×768 截图并检查空态/失败态；
- `06-implementation.md`、`07-ui-review.md`、`10-reconciliation.md` 与 `state.json` 更新；
- 停在 `UI_REVIEWED`，不宣称 `ACCEPTED`，独立验收留 B6。


## B3 实施门槛（PLAN_READY，2026-10-17）

### 垂直切片

只交付 App 侧一条只读业务解释链路：

- 项目详情中，Boss / StoreManager / Finance 查看与 Web 同源的 `blueprint/check/report` 材料业务体检；
- 主材作业单详情解释测量/配送/安装/要求到场/实际到场的时间语义、价格状态、模板必填和取消边界；
- 主材作业单建单页说明行来源与选材门禁；
- 使用帮助和错误解释补充材料作业单时间与取消主题。

不做：新 Provider、新业务规则、DDL、金额/成本口径变化、App 正式版提审/发布、B4 助手问答、B6 独立验收。

### 依赖

- `blueprint/check/report`（B2 已完成，Boss/StoreManager/Finance 可读）；
- App 现有 `material/sheet/info`、`material/sheet/enums`、`project/material-plan/page`、`material/plan/workbench`；
- 业务事实：`OBJ-008`、`BR-044`、`BR-045`、`SCN-021`、`DEC-MP-07`、`DEC-072`、`DEC-073`；
- App 设计 Token、`JzPageHeader`、`PageState`、`JzMoney`、`JzCostIncomplete`。

### 页面与状态

- 项目详情材料体检：加载/空态/失败态/重试；成本不完整时利润显示“成本未完整”；
- 作业单详情：当前状态说明、时间口径、价格状态、模板必填提示、取消边界；
- 作业单建单：只可加入材料计划行；合同外/标配外先走增减项；
- 不根据前端布尔伪造完成态，不把“已到场”写成“已结算”。

### 测试与证据

- `npm run type-check`、`npm run build:mp-weixin`；
- `npm run check:ui`、`npm run check:subpackages`、`npm run check:help`（若不存在则不新增命令）；
- 现有 `scripts/material-sheet-walkthrough.mjs` 或等价 H5/开发者工具路径做页面冒烟；
- 真实截图落 `docs/product/domains/material/evidence/B3/`；
- 至少覆盖：项目材料体检、作业单详情时间说明、建单页来源说明、空态/失败态。

### 回滚

- App 回退本批页面组件与文档；
- 不涉及数据、迁移、权限或生产配置；
- 保留旧版体验包，不发布正式版。


## B4 实施门槛（PLAN_READY，2026-10-17）

### 垂直切片

只交付材料业务助手的只读运行时与 50 个黄金问题验证：

- `material/assistant/ask` Provider；
- 意图识别、蓝图检索、真实项目/作业单上下文、确定性校核；
- 证据引用、只读工具清单、下一步、审计记录；
- 50 个黄金问题、权限负向、成本不完整与无证据拒答。

不做：新业务规则、LLM 自主写入、审批/付款/删除/改权限、DDL、正式小程序发布。

### 依赖

- B1 蓝图 v1.0.0、B2 `blueprint/check/report`、B3 页面上下文；
- `MaterialSheetContext`、`MaterialHealthContext`、`MaterialPlanWorkbenchContext`、`ProjectAccessContext`；
- 既有 AI 调用底账 `AiUsageContext`（仅记录调用元数据，不存问题/回答正文）。

### 退出条件

- 50 个黄金问题全部命中预期意图并带证据或明确拒答；
- 跨项目/越权由既有数据范围拒绝；
- 无价/成本不完整时利润不输出 0；
- Provider 编译、定向测试、HTTP 冒烟；
- 证据落 `evidence/B4/` 并更新对账矩阵；
- 停在 `RECONCILED`，独立验收留 B6。

## B5 实施门槛（PLAN_READY，2026-10-17）

### 垂直切片

只交付一条 App 单端材料业务闭环：

- 项目/财务入口进入「主材下单」工作台，查看已提交请购作业单；
- 设计师建作业单、PM 测量、提交请购；
- Finance/Boss/StoreManager 在 App 选择供应商、按行填数量和可空单价，调用 `material/sheet/order` 下单；
- 系统生成采购单及采购明细、形成应付/暂估应付，推进作业单与材料计划状态；
- PM/设计师在 App 确认到场；
- Finance/Boss/StoreManager 在 App 做逐单或月结，确认结算价、修正应付、收敛实际成本；支持撤销结算；
- 旧 `project/purchase/save`、`project/purchase/batch` 与 App 材料计划直采入口下线。

不做：新业务对象、新状态机、新金额公式、DDL、自动审批/付款/删除、正式小程序提审和发布。

### 依赖

- 业务事实：`SCN-021`、`OBJ-008`、`BR-044`、`BR-045`、`DEC-072`、`DEC-073`、`DEC-074` 及 2026-10-17 用户裁决；
- 后端既有：`MaterialSheetContext`、`MaterialPurchaseContext`、`MaterialSettlementContext`、`GoodsReceipt`、`Payable`；
- App 既有：`material-sheet` 系列页面、`supplyReceiptEdit`、`financeApi`、供应商/项目数据范围；
- 权限：Boss/StoreManager/Finance 可下单和结算；Designer/PM 只建单/测量/到场；客户只读。

### 后端契约

- `material/sheet/order`：一次调用按供应商生成一张采购单，行写入 `PurchaseOrderItem`；支持无价 `purchasePrice=null`、部分行下单；
- 采购单 `priceState` 正确表达 `Priced/Unpriced/Mixed`；混合价形成暂估应付；
- `project/purchase/save`、`project/purchase/batch` 不再注册为下单入口；历史采购单查询、到场保留；
- `material/settlement/preview|confirm|cancel` 直接复用，不新增结算规则；
- 到场回写作业单时按采购明细汇总，不按采购单头错误汇总。

### App 页面

- `/subpackages/project/pages/material-sheet`：项目模式/全公司工作台；
- `/subpackages/project/pages/material-sheet-order`：App 下单；
- `/subpackages/finance/pages/material-settlement`：App 主材结算；
- `material-sheet-detail`：提交请购后给财务“去下单”入口；
- `material-plan-workbench`、`supply/index`：移除直接采购入口，保留进度/历史/到场。

### 回滚

- 旧直采 Provider 删除前保留当前提交与上一 release；App 回退上一体验版；
- 本批不涉及 DDL/迁移；采购/结算数据写入口径需全量测试与备份；
- 生产发布前由用户确认，发布使用 `scripts/deploy-prod.sh`，失败回滚。

### 退出条件

- 后端定向测试 + 全量测试（涉及金额/成本）通过；App `type-check`/`build:mp-weixin` 通过；
- App 真实页面走查：建单→请购→App 下单→到场→结算→撤销结算；
- 旧直采入口在 App 不可达；历史采购单仍可查询/到场；
- 无价、部分下单、多供应商、超量、重复结算、越权场景有证据；
- `06-implementation.md`、`07-ui-review.md`、`10-reconciliation.md`、`state.json` 更新；
- 停在 `RECONCILED`，不宣称独立验收通过，B6 由独立角色执行。


## B6 独立验收结果（2026-10-17）

- 独立验收角色：未参与 B1–B5 实现的独立任务；
- 环境：后端 `af12b9f`、App `225b963`、本地 dev/H5 390×844；
- 结果：127 项中 121 PASS / 6 FAIL，结论 FAIL，不支持 `ACCEPTED`；
- 通过：主链拆单/无价/到场/结算/撤销、旧路线下线、50 黄金问题、跨端读数、UI 布局；
- 阻断：3 个成本/金额读权限泄漏、重复下单重复应付、未到场可结算、暂估应付显示为 0；
- 证据：`evidence/B6/`。

## B6.1 修复记录（COMPLETE，2026-10-17）

- 修复 B6-DEF-001~006，先以最小变更恢复已裁决权限/金额/结算口径，不新增业务规则；
- 权限：Designer/PM 的作业单、采购、结算读模型必须按角色裁剪成本/金额/供应商敏感字段；
- 幂等：`material/sheet/order` 增加稳定业务幂等键，重复请求不得重复采购/应付；
- 结算：`receivedQuantity>0` 是候选与确认前置门禁；
- 暂估应付：无价读模型必须可识别暂估/待定价，禁止用 0 冒充未知；
- 定向测试 + 全量测试（金额/权限）+ App/Web 构建；停在 `RECONCILED`，B6.2 独立复验。

## B7 材料业务助手上下文入口（队列）

- 目标：在项目、作业单、采购/结算上下文打开统一「问材料」抽屉；
- 首版单轮问答，复用 `material/assistant/ask`、发布蓝图建议问题和证据，不新增事实源；
- 展示结论、证据引用、只读工具、下一步、建议追问、只读边界；NO_EVIDENCE/REFUSAL 不兜底；
- 不做多轮记忆、不做万能聊天框、不改金额/成本/权限语义、不做 DDL；
- 完成后进入 B8 独立验收，不能由实现者自批。


### B6.1 完成记录

- 后端：材料商业字段按角色裁剪；采购/结算读权限补齐；下单确定性幂等；结算到场门禁；暂估应付读模型暴露 `estimated/settledAmount`；
- App/Web：应付列表对暂估应付显示“待定价/待结算”，禁止在定价前付款；
- 测试：后端全量 871/0/0/0 PASS；App type-check/MP 构建/UI/分包 PASS；Web type-check/构建 PASS；
- UI：Web 1440×900 与 App H5 390×844 真实渲染，暂估文案可见、无横向溢出、Console Errors 0；
- 证据：`evidence/B6.1/`；停在 `RECONCILED`，B6.2 独立复验。


## B6.2 独立复验结果（2026-10-17）

- 独立任务在精确提交上执行：后端 `8cc1d11`、App `46a5be4`、Web `04ba3b4`；
- 150 项：149 PASS / 1 FAIL；原 B6 六个缺陷 6/6 独立验证关闭；
- 新缺陷 `B62-DEF-001`（P2）：同一作业单行拆为 Priced 3@100 与 Unpriced/后续 Settled 5 后，作业单投影仍为 `Unpriced`，采购明细本身正确；
- 影响 SCN-021 多供应商价格分批的读模型提示；采购/应付金额事实未错；
- 证据：`evidence/B6.2/`；不得进入 `ACCEPTED`，下一批 B6.3。

## B6.3 修复记录（COMPLETE，2026-10-17）

- 只在读模型中按采购明细聚合作业单行价格状态：Priced / Unpriced / Settled / Mixed 的组合要正确；
- 不改变采购、应付、结算金额公式，不做 DDL；
- 增加多供应商拆单、部分/全部结算、撤销后读模型回归；
- 定向测试 + 全量测试 + App/Web 构建；停在 `RECONCILED`，B6.4 独立复验。


### B6.3 完成记录

- 新增 `MaterialSheetPriceProjectionContext`：按采购明细聚合 Priced/Unpriced/Settled/Mixed；
- 读取时重算作业单行/单价格状态，下单/结算/撤销后同步冗余投影；
- 实测：3@100 Priced + 2 Unpriced → Mixed；2 到场按 120 结算后仍 Mixed；撤销后仍 Mixed；
- 后端全量 876/0/0/0 PASS；App H5 390×844 显示“部分已结算”，无横向溢出、Console Errors 0；
- 证据：`evidence/B6.3/`；停在 RECONCILED，B6.4 独立复验。


## B6.4 独立复验结果（2026-10-17）

- 140 项：139 PASS / 1 FAIL；`B62-DEF-001` 的 23 项聚焦检查全部 PASS，后端读模型关闭；
- 新缺陷 `B64-UI-WEB-SHEET-01`（P2）：Web `/finance/material-ordering` 的多供应商 Mixed 作业单只显示“已到场”，未展示“混合/部分已结算/成本待结算”；
- App H5 同单据正确显示“部分已结算”，Web 无横向溢出、Console Errors 0；
- 证据：`evidence/B6.4/`；下一批 B6.5，B6.6 独立复验。

## B6.5 修复记录（COMPLETE，2026-10-17）

- 仅修 Web 主材下单页展示：Mixed/Unpriced/Settled 状态文案和视觉标记；
- 以 `sheet.priceState` / `item.priceState` 为唯一数据源，不在前端推导业务口径；
- 覆盖空态、全价、无价、Mixed、全部结算、权限与失败态；
- 不做后端语义/金额公式变更，不做 DDL；
- 真实 Web 1440×900 截图 + 构建；停在 RECONCILED，B6.6 独立复验。


### B6.5 完成记录

- Web `FinanceMaterialOrderingView` 按 `sheet.priceState` 展示 Priced/Unpriced/Mixed/Settled；
- Mixed 显示“部分已结算 · 成本待结算”，不再只显示履约状态；
- B6.4 复用黑盒脚本重跑：140/140 PASS；Web 1440×900 目标行文本正确，Console Errors 0；
- Web type-check/build PASS；证据 `evidence/B6.5/`；停在 RECONCILED，B6.6 独立复验。
