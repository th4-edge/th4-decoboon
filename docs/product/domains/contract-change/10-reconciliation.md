# 合同与增减项履约 · 设计—蓝图—实现—页面—验收对账

状态：`PASS` / `PENDING` / `UNMAPPED` / `STALE` / `GAP` / `BLOCKED`

| REQ | 场景 | 业务规则 | 蓝图节点 | 字段/API | 页面 | 测试 | 验收 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-CC-001 | A | BR-041/DEC-055 | contract-change.draft | batch-save | App/Web 发起页 | API/单测路径存在；环境未验证 | B5 | PENDING |
| REQ-CC-002 | A/B | BR-041 | contract-change.source-select | project/contract-change/range | 来源选择 | API/单测路径存在；环境未验证 | B5 | PENDING |
| REQ-CC-003 | A/B | DEC-055/BR-041 | contract-change.draft | target_* snapshot | 目标选择 | API/单测路径存在；环境未验证 | B5 | PENDING |
| REQ-CC-004 | A/B | BR-035/DEC-068 | contract-change.approve | approve/settle | 审批页 | API/单测路径存在；环境未验证 | B5 | PENDING |
| REQ-CC-005 | A/B | DEC-068 | contract-change.effective | owner change API | 业主端 | API/单测路径存在；环境未验证 | B5 | PENDING |
| REQ-CC-006 | 全场景 | DEC-055/BR-041 | contract-change.source-select | confirmable=false | 详情/列表 | API/单测路径存在；环境未验证 | B5 | PENDING |
| REQ-CC-007 | B | DEC-073/BR-041 | contract-change.link | fulfillment | 材料联动 | API/单测路径存在；环境未验证 | B5 | PENDING |
| REQ-CC-008 | 全场景 | BR-045 | contract-change.link | ProjectCostContext | 项目核算 | API/单测路径存在；环境未验证 | B5 | PENDING |
| REQ-CC-009 | 全场景 | BR-035 | contract-change.approve | 幂等 | 三端 | API/单测路径存在；环境未验证 | B5 | PENDING |
| REQ-CC-010 | 全场景 | BR-043 | contract-change.effective | 同源状态 | App/Web/业主 | API/单测路径存在；环境未验证 | B5 | PENDING |

## 未映射

- 本批蓝图与代码/API 已建立初步映射；页面真实交互和验收证据尚未闭环。

## 已过期

- 暂无。

## 未决业务问题

- 旧 `save` 与 `batch-save` 双入口收敛方案（`GAP-CC-DUAL-ENTRY`）；
- App 报价详情旧调用入口的迁移；
- 历史 `Approved` / `CustomerConfirmed` 的存量治理；
- 历史缺目标快照记录的阻断清单；
- 定向测试必须在干净工作区重新执行。

## 本批结论

- 已对齐：业务事实、角色、状态、金额边界、结构化蓝图 v1.0.0 和字段字典；
- 已映射：batch-save、range、preview、approve、App/Web 入口；
- 待对齐：双入口收敛、真实页面证据、干净基线的定向测试、独立验收；
- 本批停在：`BLUEPRINT_READY`。B2 实现需用户确认。
