# 材料履约与材料业务助手 · 设计—蓝图—实现—页面—验收对账

状态：`PASS` / `PENDING` / `UNMAPPED` / `STALE` / `GAP` / `BLOCKED`

| REQ | 场景 | 业务规则 | 蓝图节点 | 字段/API | 页面 | 测试 | 验收 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-MAT-001 | A/B/C | OBJ-008/BR-044 | material.end-to-end | `blueprint/runtime/info` + 同源资源 | `/business-blueprints` | 同源/角色/真人浏览器 | B5 独立验收 | PASS（B2 范围，验收待 B5） |
| REQ-MAT-002 | A/B/C | BR-009/BR-045 | material.health | `blueprint/check/report` | `/finance/projects/:id` + App 项目详情 | Web/App 计数/成本/空态/失败态 | B5 独立验收 | PASS（B2/B3 范围，验收待 B5） |
| REQ-MAT-003 | A/B/C | SCN-021/DEC-073 | material.object-explain | `material/assistant/ask` + sheet info | 作业单详情/助手运行时 | 正常/无权限/异常 | B5 独立验收 | PASS（B4 运行时，UI 验收待 B5） |
| REQ-MAT-004 | A/B/C | BR-044/BR-045 | material.field-dictionary | `runtime/blueprintJson.fields` | `/business-blueprints` 字段字典 | 搜索/对象筛选 | B5 独立验收 | PASS（B2 范围，验收待 B5） |
| REQ-MAT-005 | A | BR-045/DEC-072 | material.money | `material/assistant/ask` + ProjectCostContext | Web/App 助手 | 无价/成本不完整 | B5 独立验收 | PASS（B4 运行时，UI 验收待 B5） |
| REQ-MAT-006 | B | SCN-021/DEC-MP-07/BR-044 | material.measurement-receipt | material/sheet/measure + template field gate | App 详情/到货 | 定向测试 PASS；App 上下文截图与模板必填提示 PASS | 独立验收 | PASS（B3 范围，验收待 B5） |
| REQ-MAT-007 | C | DEC-MP-02/DEC-073 | material.change-linkage | `material/assistant/ask` + change rules | Web/App 助手 | 计划/采购/退货三分支 | B5 独立验收 | PASS（B4 运行时，UI 验收待 B5） |
| REQ-MAT-008 | 全场景 | BR-027/权限规范 | material.assistant-permission | assistant tools + Provider | Web/App 助手 | 跨租户/越权 | B5 独立验收 | PASS（B4 运行时） |
| REQ-MAT-009 | 全场景 | BR-045/OBJ-008 | material.assistant-grounding | blueprint retrieval + rule engine + Provider tools | Web/App 助手 | 50 黄金问题/拒答 | B5 独立验收 | PASS（B4 运行时） |
| REQ-MAT-010 | 全场景 | domain protocol | material.reconciliation | state/evidence | 无 | 矩阵完整性 | B5 独立验收 | PASS（B2 范围） |
| REQ-MAT-011 | B | OBJ-008/BR-044 | material.order-sheet | material/sheet/enums headerFields/lineFields | App/Web 建单/详情 | 模板字段定向测试 PASS；App 建单来源说明与模板字段提示 PASS | 独立验收 | PASS（B3 范围，验收待 B5） |
| REQ-MAT-012 | B | OBJ-008/DEC-MP-02 | material.order-sheet.sheet-cancel | material/sheet/cancel | App/Web 作业单详情 | 取消/拒绝取消测试 PASS；App 取消边界说明 PASS | 独立验收 | PASS（B3 范围，验收待 B5） |

## 已对齐

- B1：材料核心事实、模板配置、测量门禁、取消分支、字段映射；
- B2：蓝图运行态与领域包蓝图同源；项目材料体检从真实材料计划、采购、财务成本与结算状态派生；
- B2：Boss/StoreManager/Finance 路由与菜单可达；Designer 访问蓝图在 Provider 层被拒绝；
- B2：1440×900 / 1024×768 真实浏览器截图、阶段切换、字段搜索、空态、失败态、重试恢复均留证；
- B3：项目材料体检、作业单时间/取消上下文、建单来源说明、帮助主题已在微信开发者工具和 H5 真实渲染；
- B3：`blueprint/check/report` 在 App/Web 同源，成本不完整时利润为空，未展示伪值；
- B4：只读材料助手运行时、50 个黄金问题、证据引用、权限负向、审计记录和成本边界验证完成；
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
- B2 证据：`evidence/B2/`；
- B3 证据：`evidence/B3/`；
- B4 证据：`evidence/B4/`；
- 不可宣称：全品类材料门禁、B5 独立验收通过、助手可替代人工判断；
- 下一批：B5 独立验收。
