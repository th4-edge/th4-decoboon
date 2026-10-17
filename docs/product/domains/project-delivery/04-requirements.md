# 项目施工履约与进度 · 需求与验收条件

| ID | 用户故事 | 前置条件 | 权限 | 数据/状态 | 页面/交互 | API/事实 | 验收 | 来源 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-PD-001 | 设计师能从合同施工行创建任务 | 合同生效、项目已立项 | 设计师/PM/Boss | ContractLine→ProjectTask | App/Web 排期页 | project/task/save | 无来源任务被阻断 | DEC-049 |
| REQ-PD-002 | Boss/店长能指定 PM | 项目已立项 | Boss/本店店长 | Project.manager | 项目详情 | project/project/update | PM 未指定不得开工 | GAP-034 |
| REQ-PD-003 | 用户能区分合同基准和预计完工 | 合同计划已生成 | 项目成员 | ProjectContract.planEndDate/Project.expectedEndDate | 项目详情 | project/info | 排期不改基准 | DEC-048 |
| REQ-PD-004 | PM 能完成开工/隐蔽/完工留证 | 任务已排期 | PM/Boss/店长 | TaskNode | 任务详情 | project/task-node/save | 证据缺失阻断完成 | SCN-003 |
| REQ-PD-005 | 系统能识别计划偏差 | 排期超窗 | 项目成员 | ProjectPlanDeviation | 项目进度 | project/plan-deviation | 超窗必须生成偏差 | DEC-051 |
| REQ-PD-006 | 空阶段不被静默跳过 | 标准阶段无合同施工行 | PM/Boss | StageStatus.NotApplicable | 阶段详情 | project/stage/update | 分类、留痕、进度剔除 | DEC-057 |
| REQ-PD-007 | 合同外施工先走增减项 | 客户提出新增施工 | 设计师/PM/Boss | ContractChange | 变更入口 | batch-save | 不生成手工任务 | DEC-049 |
| REQ-PD-008 | 项目进度不受跨租户越权影响 | 任意访问 | 当前登录员工 | Project/Stage/Task | App/Web | DataScope | 404/403 | BR-001 |
| REQ-PD-009 | 重复排期/节点提交幂等 | 重复操作 | 当前角色 | Task/TaskNode | App/Web | 幂等 | 不重复生成 | domain protocol |
| REQ-PD-010 | 预计完工和动态对业主可见 | 项目有效 | 业主本人 | 可见字段 | 业主端 | owner project | 无成本/内部备注 | DEC-048 |

## 非功能要求

- 进度/统计使用服务端投影，不在前端循环计算。
- 计划偏差和节点证据必须可追溯。
- 成本缺失时利润 null，不用 0。
- 跨端状态与下一步同源。

## 明确不做

- 不做手工任务。
- 不做工艺库直接下发。
- 不做自动验收、自动完工、自动顺延合同基准。
