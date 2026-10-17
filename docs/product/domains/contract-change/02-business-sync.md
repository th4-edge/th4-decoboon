# 合同与增减项履约 · 业务智能体同步

## 咨询请求

- 请求编号：DOMAIN-CONTRACT-CHANGE-20261017-001
- 发起角色：product-planning
- 业务问题：合同变更/增减项域当前应如何划分边界、动作、状态、权限和跨域联动？
- 涉及对象：ContractChange、ContractChangeItem、ProjectContract、ContractLine、CatalogItem、ProjectTask、MaterialPlan、Receivable、ProjectCost
- 涉及状态：Pending、历史 Approved、CustomerConfirmed（对外已生效）、Rejected、Cancelled
- 涉及权限：设计师/PM 发起或代录，Boss/财务/本店店长审批，客户只读
- 涉及金额/成本：当前有效剩余、待审批占用、三类差额、合同当前额、成本完整性
- 候选方案：沿用已裁决的 `DEC-055 / DEC-068 / BR-035 / BR-041`，不新增规则

## 业务答复

- 答复编号：引用已批准知识卡
- 结论状态：approved
- 证据：`OBJ-001`、`OBJ-008`、`BR-003`、`BR-035`、`BR-041`、`BR-045`、`SCN-004`、`SCN-021`、`DEC-055`、`DEC-068`、`DEC-072`、`DEC-073`、`GAP-038`、`GAP-049`
- 标准口径：三类动作；原合同行不改；当前有效剩余扣待审批占用；目标 `CatalogItem` 快照；审批即生效；合同/任务/材料/应收/成本同事务联动；客户只读同频。
- 例外：历史 `Price/Quantity` 只读；历史不可推导范围阻断新变更；已生效异议走反向变更；目标成本缺失则不完整。
- 不可以怎样理解：不是客户在线审批；不是直接修改合同行；不是按原合同全量重算；不是调价入口；不是自动审批。
- 权限与数据范围：沿用项目与企业数据权限；成本只对 Boss/店长/财务开放。
- 金额/成本/指标：当前有效剩余和差额由服务端计算；成本缺失时 `costComplete=false`，利润 `null`。
- 页面/API/迁移影响：App/Web 变更工作台、合同与项目核算、材料计划、任务、应收；当前代码另有 `batch-save`、`range` 和旧 `save` 路径需对账。

## 未决项

| GAP/问题 | 需要谁裁决 | 影响 | 临时处理 | 状态 |
| --- | --- | --- | --- | --- |
| 旧 `project/contract-change/save` 与 `batch-save` 双入口 | 研发技术冻结 | 可能产生两套语义 | 以 `batch-save` 和 range 为准，旧入口登记待收敛 | 待实现核对 |
| Web 是否使用 `project/contract-change/range` 展示可变更量 | 产品/研发 | 页面可能展示过期范围 | B2 做代码与页面证据 | 待核对 |
| 历史 `Approved` / `CustomerConfirmed` 展示 | 已有 DEC-068 | 存量单据可能误读 | 只读展示，不改历史 | 待验收 |
| 历史缺少目标快照 | BR-041 已规定只读不重算 | 新变更可能被阻断 | 标记暂不可确认 | 待验收 |

## 同步状态

- 上次同步：2026-10-17
- 业务知识卡版本/更新时间：BR-035 2026-10-17；GAP-038/GAP-049 2026-10-13
- 蓝图是否 stale：初始 draft，待结构化
- 需要重新同步的节点：旧入口收敛、Web range 使用、历史状态展示

## 实现现状核对（2026-10-17）

| 能力 | 代码事实 | 结论 |
| --- | --- | --- |
| 三类动作批量建单 | `project/contract-change/batch-save`；App `change-create` 与 Web 变更页均有调用 | 已实现路径存在，待独立验收 |
| 可变更范围 | `project/contract-change/range`；`ContractChangeRangeContext` 投影剩余、占用、可变更量 | 已实现路径存在，待独立验收 |
| 草稿预览 | `project/contract-change/preview` 与 batch-save 同源 | 已实现路径存在 |
| 审批即生效 | `ProjectContractContext.approve` 直接进入已生效并联动合同/项目/应收/任务/材料/成本 | 已实现路径存在 |
| 旧单入口 | `project/contract-change/save` 仍存在，App 报价详情仍可调用 | 双入口风险，必须收敛 |
| 历史客户确认 | `customer-confirm` 保留历史收口；前端 API 也存在但不应作为新动作 | 历史兼容，待存量与 UI 证据 |
| 目标快照 | `ContractChangeItem.target_*` 与来源剩余快照字段存在 | 新批次路径已具备，待历史数据治理 |
| Web range | Web API 已接入 range/batch-save/preview；页面真实数据证据待 B3 | 代码存在，UI 未验收 |

### 测试状态

定向测试首次执行时，当前工作区已被其他未完成改动污染，Spring 上下文因缺少
`com/th4/edge/decoboon/finance/service/aging/report/support/Report.class` 启动失败。
该错误不是本域业务断言失败；在干净基线和未完成改动收敛前，不得记为本域测试通过。
