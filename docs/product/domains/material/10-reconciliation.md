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
| REQ-MAT-016 | 全场景 | BR-045/OBJ-008 | material.assistant-context | `material/assistant/ask` + `material/assistant/suggestions` + 上下文入口 | App/Web 统一问材料抽屉 | B7 48/48 自测 PASS（API 14/Web 22/App 12） | B8 独立验收 | PASS（B7 自测，B8 待验） |

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
- B7：App/Web 单轮「问材料」上下文入口完成，48/48 自测 PASS；B8 独立验收待执行；
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
- 不可宣称：全品类材料门禁、助手可替代人工判断、B8 已验收；B6 独立验收结论以 `evidence/B6.6/` 为准；
- 下一批：B8 助手 UI 聚焦独立验收（权限、证据、拒答、上下文、跨端一致性）。


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
- 无 `UNMAPPED`/`STALE`；B7 `RECONCILED`，B8 独立验收待执行。
