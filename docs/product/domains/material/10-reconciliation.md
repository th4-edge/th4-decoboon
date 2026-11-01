# 材料履约与材料业务助手 · 设计—蓝图—实现—页面—验收对账

状态：`PASS` / `PENDING` / `UNMAPPED` / `STALE` / `GAP` / `BLOCKED`

| REQ | 场景 | 业务规则 | 蓝图节点 | 字段/API | 页面 | 测试 | 验收 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-MAT-001 | A/B/C | OBJ-008/BR-044 | material.end-to-end | `blueprint/runtime/info` + 同源资源 | `/business-blueprints` | 同源/角色/真人浏览器 | B6 独立验收 | PASS（B2 范围，验收待 B6） |
| REQ-MAT-002 | A/B/C | BR-009/BR-045 | material.health | `blueprint/check/report` | `/finance/projects/:id` + App 项目详情 | Web/App 计数/成本/空态/失败态 | B6 独立验收 | PASS（B2/B3 范围，验收待 B6） |
| REQ-MAT-003 | A/B/C | SCN-021/DEC-073 | material.object-explain | `material/assistant/ask` + sheet info | 作业单详情/助手运行时 | 正常/无权限/异常 | B6 独立验收 | PASS（B4 运行时，UI 验收待 B6） |
| REQ-MAT-004 | A/B/C | BR-044/BR-045 | material.field-dictionary | `runtime/blueprintJson.fields` | `/business-blueprints` 字段字典 | 搜索/对象筛选 | B6 独立验收 | PASS（B2 范围，验收待 B6） |
| REQ-MAT-005 | A | BR-045/DEC-072 | material.money | `material/assistant/ask` + ProjectCostContext | Web/App 助手 | 无价/成本不完整 | B6 独立验收 | PASS（B4 运行时，UI 验收待 B6） |
| REQ-MAT-006 | B | SCN-021/DEC-MP-07/BR-044 | material.measurement-receipt | material/sheet/measure + template field gate | App 详情/到货 | 定向测试 PASS；App 上下文截图与模板必填提示 PASS | B6 独立验收 | PASS（B3 范围，验收待 B6） |
| REQ-MAT-007 | C | DEC-MP-02/DEC-073 | material.change-linkage | `material/assistant/ask` + change rules | Web/App 助手 | 计划/采购/退货三分支 | B6 独立验收 | PASS（B4 运行时，UI 验收待 B6） |
| REQ-MAT-008 | 全场景 | BR-027/权限规范 | material.assistant-permission | assistant tools + Provider | Web/App 助手 | 跨租户/越权 | B6 独立验收 | PASS（B4 运行时） |
| REQ-MAT-009 | 全场景 | BR-045/OBJ-008 | material.assistant-grounding | blueprint retrieval + rule engine + Provider tools | Web/App 助手 | 50 黄金问题/拒答 | B6 独立验收 | PASS（B4 运行时） |
| REQ-MAT-010 | 全场景 | domain protocol | material.reconciliation | state/evidence | 无 | 矩阵完整性 | B6 独立验收 | PASS（B2 范围） |
| REQ-MAT-011 | B | OBJ-008/BR-044 | material.order-sheet | material/sheet/enums headerFields/lineFields | App/Web 建单/详情 | 模板字段定向测试 PASS；App 建单来源说明与模板字段提示 PASS | B6 独立验收 | PASS（B3 范围，验收待 B6） |
| REQ-MAT-012 | B | OBJ-008/DEC-MP-02 | material.order-sheet.sheet-cancel | material/sheet/cancel | App/Web 作业单详情 | 取消/拒绝取消测试 PASS；App 取消边界说明 PASS | B6 独立验收 | PASS（B3 范围，验收待 B6） |
| REQ-MAT-013 | A/B | SCN-021/DEC-072/DEC-073 | material.order-sheet.order | material/sheet/order | App 主材下单工作台/下单页 | B6.6 157/157 独立复验 PASS；四态与拆单/部分结算/撤销回归 PASS | B6.6 独立验收 | PASS（B6.6 独立） |
| REQ-MAT-014 | A/B | BR-045/DEC-073/DEC-074 | material.settlement | material/settlement/preview|confirm|cancel | App 主材结算 | B6.2 角色门禁/到场门禁/逐单月结/撤销独立 PASS | B6.4 回归 | PASS（B6.2 独立） |
| REQ-MAT-015 | 全场景 | 2026-10-17 用户裁决 | material.single-route | 旧 `project/purchase/save`/`batch` 下线 | App 材料计划/供应商 | Provider 已删除，App 入口已清理 | B6 独立验收 | PASS（B6 独立验证） |
| REQ-MAT-016 | 全场景 | BR-045/OBJ-008 | material.assistant-context | `material/assistant/ask` + `material/assistant/suggestions` + 上下文入口 | App/Web 统一问材料抽屉 | B7 48/48 自测 PASS；B8 110/110 PASS | B8 独立验收 | PASS（ACCEPTED） |

## B6 独立验收结果（2026-10-17）

- 共 127 项：121 PASS / 6 FAIL；独立验收结论 **FAIL**，不支持 `ACCEPTED`；
- 通过：主链正向、反向拦截、旧路线、50 黄金问题、跨端读数、11 张 H5 UI 证据；
- 阻断：`B6-DEF-001` 作业单成本泄漏、`B6-DEF-002` 采购金额/供应商泄漏、`B6-DEF-003` 结算金额/供应商泄漏、`B6-DEF-004` 重复下单重复应付、`B6-DEF-005` 未到场可结算、`B6-DEF-006` 暂估应付显示 0；
- 证据：`evidence/B6/`；没有 `UNMAPPED`/`STALE`，但存在 5 个 P1 和 1 个 P2 开放缺陷；
- 下一批：B6.1 修复后进入 B6.2 独立复验；B7 材料助手上下文入口保持队列。

## B6.1 修复结果（2026-10-17）

- `B6-DEF-001~003`：Designer/PM 作业单、采购、结算读权限已修复；Finance/Boss/StoreManager 保持商业字段可见；
- `B6-DEF-004`：重复下单返回确定性结果，只生成 1 张采购单、1 条采购明细、1 条应付；
- `B6-DEF-005`：未到场采购明细不进入预览，确认返回“尚未到场”；
- `B6-DEF-006`：暂估应付返回 `estimated=true`，App/Web 显示“待定价/待结算”；
- 全量后端 871/0/0/0 PASS；App/Web 构建 PASS；证据 `evidence/B6.1/`；
- 无 `UNMAPPED`/`STALE`；B6.2 独立复验待执行。

## B6.2 独立复验结果（2026-10-17）

- 150 项：149 PASS / 1 FAIL；原 B6 六项缺陷 6/6 独立关闭；
- 新 P2 `B62-DEF-001`：多供应商拆单后作业单行 `priceState` 仍为 `Unpriced`，采购明细事实为 Priced + Settled；
- 证据：`evidence/B6.2/`；无 `UNMAPPED`/`STALE`，但 B62-DEF-001 为开放 FAIL；
- 下一批：B6.3 修复，B6.4 聚焦独立复验。

## B6.3 修复结果（2026-10-17）

- `B62-DEF-001` 已修复：作业单行/单价格状态按采购明细聚合，Priced+Unpriced/Settled 显示 Mixed；
- 实测 Mixed → 结算后 Mixed → 撤销后 Mixed；采购/应付/结算金额事实不变；
- 后端全量 876/0/0/0 PASS；App H5 390×844 真实页面 PASS；证据 `evidence/B6.3/`；
- 无 `UNMAPPED`/`STALE`；B6.4 聚焦独立复验待执行。

## B6.4 独立复验结果（2026-10-17）

- 140 项：139 PASS / 1 FAIL；B62-DEF-001 的 23 项聚焦检查全部通过，后端读模型关闭；
- 新 P2 `B64-UI-WEB-SHEET-01`：Web `/finance/material-ordering` Mixed 作业单未展示价格状态/成本待结算；
- App H5 同单据显示“部分已结算”；Web 无横向溢出、Console Errors 0；
- 证据：`evidence/B6.4/`；下一批 B6.5，B6.6 独立复验。

## B6.5 修复结果（2026-10-17）

- Web `/finance/material-ordering` 按服务端 `priceState` 显示 Priced/Unpriced/Mixed/Settled；
- Mixed 显示“部分已结算 · 成本待结算”；B6.4 回归脚本 140/140 PASS；
- 证据：`evidence/B6.5/`；无 `UNMAPPED`/`STALE`；B6.6 独立复验 157/157 PASS。

## 已对齐

- B1：材料核心事实、模板配置、测量门禁、取消分支、字段映射；
- B2：蓝图运行态与领域包蓝图同源；项目材料体检从真实材料计划、采购、财务成本与结算状态派生；
- B2：Boss/StoreManager/Finance 路由与菜单可达；Designer 访问蓝图在 Provider 层被拒绝；
- B2：1440×900 / 1024×768 真实浏览器截图、阶段切换、字段搜索、空态、失败态、重试恢复均留证；
- B3：项目材料体检、作业单时间/取消上下文、建单来源说明、帮助主题已在微信开发者工具和 H5 真实渲染；
- B3：`blueprint/check/report` 在 App/Web 同源，成本不完整时利润为空，未展示伪值；
- B4：只读材料助手运行时、50 个黄金问题、证据引用、权限负向、审计记录和成本边界验证完成；
- B5：App 单端材料下单与结算代码已落地；旧直采 Provider 与 App 入口已移除；后端全量测试 854/0/0、App verify:mp-weixin PASS；
- B5：微信开发者工具与 H5 真实页面、交互和布局走查完成，`B5-UI-001` 已修复；REQ-MAT-013/014/015 达到 `RECONCILED`，独立验收待 B6；
- B4：助手只调用既有只读上下文，不直接查库，不执行审批/付款/删除/改权限。

## 未映射

- 暂无。

## 已过期

- “Web 蓝图中心 = 未实现”：已过期，B2 已实现 `/business-blueprints`；
- “项目材料体检 = 未实现”：已过期，B2 已接入 `/finance/projects/:id`；
- “材料业务体检 = 前端拼接”：已过期，B2 改由 `blueprint/check/report` 聚合；
- “App 作业单时间/取消语义 = 未解释”：已过期，B3 已加入上下文说明与帮助主题；
- “App 项目材料体检 = 未接入”：已过期，B3 已接入同源只读 Provider；
- “材料业务助手 = 未实现”：已过期，B4 已实现 `material/assistant/ask` 只读运行时；
- “助手会调用生产库或执行高风险动作”：已过期，B4 使用只读上下文并在越界请求时拒答。

## 未决业务问题

- `GAP-MAT-TIME-SEMANTICS`：是否新增“请购时间/采购下单时间”独立字段；当前保持 `createTime` / `orderedAt` 现状，不进入代码。

## 本批结论

- B1：`BLUEPRINT_READY`，源码事实与蓝图已对齐；
- B2：`DEV_VERIFIED + UI_REVIEWED + RECONCILED`；
- B3：`DEV_VERIFIED + UI_REVIEWED + RECONCILED`；
- B4：`DEV_VERIFIED + RECONCILED`（无新增 UI 表面）；
- B5：App 单端材料下单与结算闭环，旧直采路线下线；后端全量测试、App 构建与真实 UI 走查 PASS；
- B6：B6.4 独立复验确认 B62-DEF-001 关闭；B6.5 修复 Web Mixed 展示；B6.6 独立复验 157/157 PASS，B6 系列 `ACCEPTED`；
- B7：App/Web 单轮「问材料」上下文入口完成，48/48 自测 PASS；B8 独立验收 110/110 PASS；
- B2 证据：`evidence/B2/`；
- B3 证据：`evidence/B3/`；
- B4 证据：`evidence/B4/`；
- B5 证据：`evidence/B5/`；
- B6.1 证据：`evidence/B6.1/`；
- B6.2 证据：`evidence/B6.2/`；
- B6.3 证据：`evidence/B6.3/`；
- B6.4 证据：`evidence/B6.4/`；
- B6.5 证据：`evidence/B6.5/`；
- B6.6 证据：`evidence/B6.6/`；
- B7 证据：`evidence/B7/`；
- 不可宣称：全品类材料门禁、助手可替代人工判断；B8 独立验收结论以 `evidence/B8/` 为准；
- 下一批：B9 体验版与发布准备；正式提审/发布由用户完成。


## B6.6 独立复验结果（2026-10-17）

- 精确提交：后端 `b66a33b` / App `46a5be4` / Web `3ef4974`；
- 157 项 **157 PASS / 0 FAIL / 0 BLOCKED**；`B62-DEF-001`、`B64-UI-WEB-SHEET-01` 均 CLOSED；
- Web Mixed 行显示“部分已结算 · 成本待结算”并保留履约状态；四态文案、pill、API/XHR 同源；
- B6 六项回归 6/6、50 黄金问题 50/50、权限/幂等/到场门禁/暂估应付/旧路线/跨企业/跨端 UI 全部 PASS；
- 无 `UNMAPPED`/`STALE`；环境瞬态 `ENV-B66-001` 已记录为 Vite 依赖优化瞬态，非产品缺陷；
- 证据：`evidence/B6.6/`；B6 系列 `ACCEPTED`，最终正式接受/发布由用户确认；
- 下一批：B7 材料业务助手上下文入口。


## B7 材料业务助手上下文入口结果（2026-10-17）

- 范围：App/Web 单轮「问材料」抽屉；项目、作业单、采购/结算上下文；复用 `material/assistant/ask`；
- 新增只读 `material/assistant/suggestions`，建议问题只来自同一 `MaterialAssistantKnowledge` 黄金问题源；
- 后端 API 14/14、Web UI 22/22、App UI 12/12，合计 **48/48 PASS**；
- 覆盖项目/作业单上下文、Designer 权限口径、REFUSAL、NO_EVIDENCE、空/超长问题、错误重试、清除/恢复上下文；
- Web 1440×900/1024×768、App H5 390×844/375×667 无横向溢出，Console Errors 0；
- 证据：`evidence/B7/`；临时作业单已删除，材料计划恢复 `ReadyToOrder`；无 DDL/迁移/生产/正式小程序发布；
- 无 `UNMAPPED`/`STALE`；B7 `RECONCILED`，B8 独立验收完成。


## B8 助手 UI 聚焦独立验收结论（2026-10-17）

- 精确提交：后端 `302193e` / App `67e6da0` / Web `d15d088`；
- 110 项 **110 PASS / 0 FAIL / 0 BLOCKED**；
- API 上下文/建议同源、answer/citations/toolCalls/nextActions/readOnly/auditStatus、成本角色可见性、跨企业/跨项目 404、前端字段伪造、REFUSAL/NO_EVIDENCE、审计幂等与不保存正文、50 黄金问题、只读业务表均通过；
- Web 项目/采购/结算 1440×900/1024×768、App 项目/真实作业单/结算 390×844/375×667、Web/App 加载/长内容/错误重试/清除恢复/焦点/溢出的真实 UI 证据通过；
- 微信开发者工具 `miniprogram-automator@0.12.1` 真实进入项目与作业单并完成建议问题、回答、证据、只读提示、上下文绑定验证；375×667，业务 Console Errors/exception 0；
- API/Web/App/DevTools 同一项目与作业单的 answer、状态/计数/阶段/下一步及核心 citations 一致；
- 无新增缺陷，`defects.json=[]`；P3 观察项 `OBS-B8-001` 登记为 suggestions 元数据分组边界，不构成权限泄漏；
- 临时作业单、材料计划、项目负责人、测试密码哈希已清理/恢复；无 DDL/迁移/生产/正式小程序发布；
- 无 `UNMAPPED`/`STALE`；B8 满足 `ACCEPTED` 用户决策证据条件，最终正式接受/发布由用户确认。

## U1 App 作业单详情工作台化（2026-10-18）

- 状态：`UI_REVIEWED`（自测，不是独立验收）
- 范围：`subpackages/project/pages/material-sheet-detail.vue`
- 设计↔实现：首屏收敛为“状态 + 还差什么 + 一个主行动”；大段说明折叠为“为什么这样填？”；Designer/PM 不显示商业价格状态；提交动作自动保存现场字段后再提交。
- 页面证据：`evidence/U1/README.md`、`evidence/U1/screenshots/`、`evidence/U1/ui-metrics-draft.json`、`evidence/U1/ui-metrics-submitted.json`。
- 角色：Boss/Designer/PM/Finance 390×844；Designer 帮助态 375×667；无横向溢出，业务 Console Errors 0。
- 未闭环：U2 财务下单/结算工作台化；U3 老板材料风险页、助手技术语言与蓝图入口；独立用户可用性走查 U4。

## U2 App/Web 主材下单与结算工作台化（2026-10-18）

- 状态：`UI_REVIEWED`（自测，不是独立验收）
- 范围：App `material-sheet-order.vue`、`material-settlement.vue`；Web `FinanceMaterialOrderingView.vue`、`FinanceMaterialSettlementView.vue`
- 设计↔实现：首屏摘要化；规则折叠；下单/结算各自只有一个主行动；Web 待下单行增加显式“去下单”。
- 页面证据：`evidence/U2/README.md`、`evidence/U2/screenshots/`、`evidence/U2/ui-metrics-web.json`、`evidence/U2/ui-metrics-app.json`。
- 角色/分辨率：Finance 390×844/375×667（App）、1440×900/1024×768（Web）；无横向溢出，业务 Console Errors 0。
- 未闭环：U3 老板材料风险页/助手/蓝图入口；U4 独立用户可用性走查。

## U3 老板材料风险页、助手语言与材料说明入口（2026-10-18）

- 状态：`UI_REVIEWED`（自测，不是独立验收）
- 范围：Web `MaterialHealthPanel.vue`、`MaterialAssistant.vue`、`BusinessBlueprintCenterView.vue`、`workbenchConfig.ts`、`routes.ts`；App `MaterialHealthCard.vue`、`MaterialAssistant.vue`、`project/pages/detail.vue`
- 设计↔实现：首屏新增“最急材料”（材料/空间、卡点、责任设计/项目、下一步、一个处理按钮）；助手移除技术元数据并转业务语言；蓝图移至“帮助与说明 → 材料业务说明”。
- 页面证据：`evidence/U3/README.md`、`evidence/U3/screenshots/`、`evidence/U3/ui-metrics-web.json`、`evidence/U3/ui-metrics-app.json`。
- 角色/分辨率：Finance/Designer；Web 1440×900/1024×768、App 390×844/375×667；无横向溢出，业务 Console Errors 0。
- 未闭环：U4 真实用户可用性走查；助手单轮、固定建议题库、无自动执行。

## MAT-X2A Web 主材作业单维护生命周期（2026-11-02，DEV_VERIFIED）

- 精确提交：Web `489948c`；后端 Provider 无新增。
- 范围：Designer/Boss/StoreManager 新建、编辑、测量、提交、取消、导出；Pm 测量；Finance 查看与取消；财务下单继续复用既有 `/finance/material-ordering`。
- 数据源：`project/material-plan/page`、`material/sheet/save|update|measure|submit|cancel|export|page|info`；无第二套接口。
- 真实验证：Designer 完成新建→编辑→测量→提交→导出→取消；Pm/Finance 越权建单业务 403；Web 1440/1280/1024 无横向溢出；内部编号与技术文案未出现。
- 证据：`docs/acceptance/runs/RUN-20261101-CORE-P0-002/evidence/TC-MAT-DESIGNER-SHEET-002/20-*` 至 `34-*`。
- 边界：视觉人工评审因当前会话无图像输入标 `VISUAL_REVIEW_UNAVAILABLE`；App 真机、到场确认、独立验收和发布未完成。
- 下一片：`MAT-X2B` Web 到场确认，再进入独立验收。

## MAT-X2B Web 到场确认（2026-11-02，DEV_VERIFIED）

- 精确提交：Web `ec3da3b`；后端 Provider 无新增。
- 范围：Designer/Pm/Boss/StoreManager/Finance 在 Web 作业单详情确认到场；支持部分到场和尾量续收。
- 数据源：`project/purchase/page`、`project/goods-receipt/save|page`；复用既有到场、计划、采购、应付和成本联动。
- 真实验证：8㎡部分到场、13㎡超量拒绝、12㎡尾量到场；作业单最终 `Arrived`、采购/明细 `Received`；应付仍1条1760，receiptDate/dueDate回填；实际成本1760。
- 证据：`docs/acceptance/runs/RUN-20261101-CORE-P0-002/evidence/TC-MAT-FINANCE-ORDER-004/40-*` 至 `53-*`。
- 边界：App 真机、独立验收和异常退换未完成；视觉人工评审因当前会话无图像输入标 `VISUAL_REVIEW_UNAVAILABLE`。
- 下一步：MAT-X2 独立验收与 App 真机；之后再进入 MAT-X3。

## PI-20261022-015 跨端与异常闭环（BIZ_DECIDED / 未实现）

| REQ | 业务规则 | 蓝图/对象 | 字段/API | 页面 | 测试 | 验收 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-MAT-017 | `DEC-088/BR-054` | Sheet/Purchase/Receipt | sheet/*、goods-receipt/save | App/Web 作业单与到场 | MAT-X2A + MAT-X2B Web 自测通过 | 独立验收待执行 | DEV_VERIFIED |
| REQ-MAT-018 | `DEC-088/BR-054` | Sheet/Purchase/Receipt/Adjustment/Settlement | 服务端状态推导 | App/Web 作业单详情 | 待建 | 独立验收待执行 | UNMAPPED |
| REQ-MAT-019 | `DEC-088/BR-054` | MaterialAdjustmentOrder 及下层事实 | adjustment/* | App/Web 异常处置 | 待建 | 独立验收待执行 | UNMAPPED |
| REQ-MAT-020 | `DEC-088/BR-045` | P1/P2/P3 只读投影 | 服务端聚合 | App/Web 作业单详情 | 待建 | 独立验收待执行 | UNMAPPED |
| REQ-MAT-021 | `DEC-088/PI-010` | 五个材料事实节点 | PI-010 统一对象视图 | 项目事实追踪 | 待建 | 独立验收待执行 | UNMAPPED |

说明：业务已对齐；`UNMAPPED` 只表示代码、迁移、页面和测试尚未进入本批，不得解释为业务未决。
