# 材料履约与材料业务助手 · 设计—蓝图—实现—页面—验收对账

状态：`PASS` / `PENDING` / `UNMAPPED` / `STALE` / `GAP` / `BLOCKED`

| REQ | 场景 | 业务规则 | 蓝图节点 | 字段/API | 页面 | 测试 | 验收 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-MAT-001 | A/B/C | OBJ-008/BR-044 | material.end-to-end | blueprint definition/runtime | Web/App | 蓝图契约 | 独立验收 | PENDING |
| REQ-MAT-002 | A/B/C | BR-009/BR-045 | material.health | blueprint/check/report | Web 项目/App 项目 | 计数/空态/失败 | 独立验收 | PENDING |
| REQ-MAT-003 | A/B/C | SCN-021/DEC-073 | material.object-explain | material/sheet/info + assistant | 作业单详情 | 正常/无权限/异常 | 独立验收 | PENDING |
| REQ-MAT-004 | A/B/C | BR-044/BR-045 | material.field-dictionary | blueprint fields | Web/App | 字段搜索/权限裁剪 | 独立验收 | PENDING |
| REQ-MAT-005 | A | BR-045/DEC-072 | material.money | Payable/ProjectCostContext | Web 财务 | 无价/成本不完整 | 独立验收 | PENDING |
| REQ-MAT-006 | B | SCN-021/DEC-MP-07/BR-044 | material.measurement-receipt | material/sheet/measure + template field gate | App 详情/到货 | 定向测试 PASS；UI 未验收 | 独立验收 | PENDING |
| REQ-MAT-007 | C | DEC-MP-02/DEC-073 | material.change-linkage | ContractChangeFulfillmentContext | Web/App 变更 | 计划/采购/退货三分支 | 独立验收 | PENDING |
| REQ-MAT-008 | 全场景 | BR-027/权限规范 | material.assistant-permission | assistant tools + Provider | Web/App 助手 | 跨租户/越权 | 独立验收 | PENDING |
| REQ-MAT-009 | 全场景 | BR-045/OBJ-008 | material.assistant-grounding | RAG + rule engine | Web/App 助手 | 黄金问题/拒答 | 独立验收 | PENDING |
| REQ-MAT-010 | 全场景 | domain protocol | material.reconciliation | state/reconciliation | 无 | 矩阵完整性 | 独立验收 | PENDING |
| REQ-MAT-011 | B | OBJ-008/BR-044 | material.order-sheet | material/sheet/enums headerFields/lineFields | App/Web 建单/详情 | 模板字段定向测试 PASS；UI 未验收 | 独立验收 | PENDING |
| REQ-MAT-012 | B | OBJ-008/DEC-MP-02 | material.order-sheet.sheet-cancel | material/sheet/cancel | App/Web 作业单详情 | 取消/拒绝取消测试 PASS；UI 未验收 | 独立验收 | PENDING |

## 已对齐

- `97d5705` 已实现模板字段投影、提交必填门禁、未采购前取消 Provider；
- 蓝图 v1.0.0 已补模板对象、字段字典、取消分支、测量门禁校核和逐阶段来源引用；
- 测量、取消、时间字段的状态已从旧 `gap` 改为“代码事实 / 待页面与独立验收 / 待业务裁决”。

## 未映射

- 暂无；B1 的蓝图节点和字段映射已建立。

## 已过期

- “测量字段按品类模板显隐/必填 = gap”：已过期，改由 `97d5705` + REQ-MAT-011 表达；
- “作业单独立取消 Provider = gap”：已过期，改由 `97d5705` + REQ-MAT-012 表达。

## 未决业务问题

- `GAP-MAT-TIME-SEMANTICS`：是否新增“请购时间/采购下单时间”独立字段；当前不新增，保持 `createTime` 与 `orderedAt` 现状。

## 本批结论

- B1：`BLUEPRINT_READY`；
- 已对齐：材料核心事实、模板配置、测量门禁、取消分支、字段映射；
- 待对齐：页面绑定、真实 UI 证据、业务助手运行时；
- 下一批：B2 Web 蓝图中心 + 项目材料业务体检。
