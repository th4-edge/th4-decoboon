# 材料履约与材料业务助手 · 设计—蓝图—实现—页面—验收对账

状态：`PASS` / `PENDING` / `UNMAPPED` / `STALE` / `GAP` / `BLOCKED`

| REQ | 场景 | 业务规则 | 蓝图节点 | 字段/API | 页面 | 测试 | 验收 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-MAT-001 | A/B/C | OBJ-008/BR-044 | material.end-to-end | blueprint definition/runtime | Web/App | 蓝图契约 | 独立验收 | PENDING |
| REQ-MAT-002 | A/B/C | BR-009/BR-045 | material.health | blueprint/check/report | Web 项目/App 项目 | 计数/空态/失败 | 独立验收 | PENDING |
| REQ-MAT-003 | A/B/C | SCN-021/DEC-073 | material.object-explain | material/sheet/info + assistant | 作业单详情 | 正常/无权限/异常 | 独立验收 | PENDING |
| REQ-MAT-004 | A/B/C | BR-044/BR-045 | material.field-dictionary | blueprint fields | Web/App | 字段搜索/权限裁剪 | 独立验收 | PENDING |
| REQ-MAT-005 | A | BR-045/DEC-072 | material.money | Payable/ProjectCostContext | Web 财务 | 无价/成本不完整 | 独立验收 | PENDING |
| REQ-MAT-006 | B | SCN-021/DEC-MP-07 | material.measurement-receipt | measure/goods-receipt | App 详情/到货 | 部分到场/超量 | 独立验收 | PENDING |
| REQ-MAT-007 | C | DEC-MP-02/DEC-073 | material.change-linkage | ContractChangeFulfillmentContext | Web/App 变更 | 计划/采购/退货三分支 | 独立验收 | PENDING |
| REQ-MAT-008 | 全场景 | BR-027/权限规范 | material.assistant-permission | assistant tools + Provider | Web/App 助手 | 跨租户/越权 | 独立验收 | PENDING |
| REQ-MAT-009 | 全场景 | BR-045/OBJ-008 | material.assistant-grounding | RAG + rule engine | Web/App 助手 | 黄金问题/拒答 | 独立验收 | PENDING |
| REQ-MAT-010 | 全场景 | domain protocol | material.reconciliation | state/reconciliation | 无 | 矩阵完整性 | 独立验收 | PENDING |

## 未映射

- 暂无；B1 完成后补齐蓝图节点和字段映射。

## 已过期

- 暂无。

## 未决业务问题

- 测量字段模板化门禁（gap，不阻塞只读助手/蓝图中心）；
- 作业单独立取消 Provider（gap，不实现）；
- 请购时间/采购下单时间新增字段（gap，需业务裁决）。

## 本批结论

- 已对齐：材料核心事实引用；
- 待对齐：结构化蓝图、页面绑定、助手运行时；
- 下一批：B1 完成 BLUEPRINT_READY。
