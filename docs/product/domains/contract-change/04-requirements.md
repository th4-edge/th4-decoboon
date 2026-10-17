# 合同与增减项履约 · 需求与验收条件

| ID | 用户故事 | 前置条件 | 权限 | 数据/状态 | 页面/交互 | API/事实 | 验收 | 来源 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-CC-001 | 设计师能理解新增/减项/替换边界 | 合同生效、项目未完工 | 设计师/授权代录 | ContractChange | App/Web 发起页 | batch-save + range | 三类动作无调价入口 | DEC-055 |
| REQ-CC-002 | 系统能计算当前有效剩余和待审批占用 | 合同行或已生效目标范围存在 | 当前数据范围 | RangeSnapshot | 来源选择 | project/contract-change/range | 超减与并发占用阻断 | BR-041 |
| REQ-CC-003 | 新增/替换只能使用启用目录项 | 目标 CatalogItem 有效 | 发起人 | CatalogItem 快照 | 目标选择 | batch-save | 手填名称/价格被拒绝 | DEC-055 |
| REQ-CC-004 | 审批一次生效并联动 | 变更单 Pending | Boss/财务/本店店长 | 合同/应收/任务/材料/成本 | 审批页 | approve/settle | 同事务、无半套数据 | BR-035 |
| REQ-CC-005 | 客户能只读同频 | 变更单存在 | 客户本人项目 | 审批中/已生效 | 业主端 | owner change API | 待审批不显示金额 | DEC-068 |
| REQ-CC-006 | 历史数据不伪算 | 历史 Price/Quantity/缺快照 | 只读 | 历史记录 | 详情/列表 | range confirmable=false | 显示暂不可确认 | DEC-055 |
| REQ-CC-007 | 已采购事实不被篡改 | 已下单/到场 | 发起人/财务/PM | Purchase/Receipt/Return | 材料联动 | fulfillment | 已到货走退货 | DEC-073 |
| REQ-CC-008 | 成本缺失不产生伪利润 | 目标成本缺失 | Boss/店长/财务 | costComplete、profit | 项目核算 | ProjectCostContext | 利润 null | BR-045 |
| REQ-CC-009 | 重复点击不重复生效 | 重复请求 | 当前角色 | 变更/派生对象 | App/Web | batch-save/approve | 幂等 | BR-035 |
| REQ-CC-010 | 跨端对变更状态口径一致 | App/Web/业主端 | 当前角色 | Pending/已生效/驳回/取消 | 三端 | 同源 Provider | 状态和金额一致 | BR-043 |

## 非功能要求

- 当前有效剩余与差额必须由服务端聚合/投影，不在前端计算。
- 审批联动必须在同一事务内完成，失败不产生半套数据。
- 并发占用使用 RedissonLock，不使用数据库悲观锁。
- 客户同频不泄露成本、内部审批意见或其他客户数据。
- 审计保留发起人、审批人、明细和差额快照。

## 明确不做

- 不做报价阶段清单修订。
- 不做价格单独调整入口。
- 不修改原报价/合同行。
- 不做自动审批、自动付款、删除或改权限。
