# 项目施工履约与进度 · 设计—蓝图—实现—页面—验收对账

状态：`PASS` / `PENDING` / `UNMAPPED` / `STALE` / `GAP` / `BLOCKED`

| REQ | 场景 | 业务规则 | 蓝图节点 | 字段/API | 页面 | 测试 | 验收 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-PD-001 | A/B | DEC-049 | project-delivery.source-task | project/task/save | App/Web 排期 | 待补 | B4 | PENDING |
| REQ-PD-002 | A | GAP-034 | project-delivery.pm-assign | project/project/update | 项目详情 | 待补 | B4 | PENDING |
| REQ-PD-003 | A | DEC-048 | project-delivery.progress | project/info | 项目详情 | 待补 | B4 | PENDING |
| REQ-PD-004 | A | SCN-003 | project-delivery.evidence | project/task-node/save | 任务详情 | 待补 | B4 | PENDING |
| REQ-PD-005 | A | DEC-051 | project-delivery.progress | project/plan-deviation | 项目进度 | 待补 | B4 | PENDING |
| REQ-PD-006 | A | DEC-057 | project-delivery.progress | project/stage/update | 阶段详情 | 待补 | B4 | PENDING |
| REQ-PD-007 | B | DEC-049 | project-delivery.source-task | contract-change/batch-save | 变更入口 | 待补 | B4 | PENDING |
| REQ-PD-008 | 全场景 | BR-001 | project-delivery.project-start | DataScope | 三端 | 待补 | B4 | PENDING |
| REQ-PD-009 | 全场景 | protocol | project-delivery.work | 幂等 | 三端 | 待补 | B4 | PENDING |
| REQ-PD-010 | A | DEC-048 | project-delivery.progress | owner project | 业主端 | 待补 | B4 | PENDING |

## 未映射

- 本批仅完成业务与初始蓝图，代码/页面映射待 B1 完成。

## 已过期

- 暂无。

## 未决业务问题

- GAP-034 阶段计划算法；
- acceptanceMode 落 ProjectStage；
- 现场汇报/日报角色；
- 取消任务是否进入阶段验收门禁；
- 工艺库与阶段/区域绑定。

## 本批结论

- 已对齐：核心任务来源、工期层次、节点、空阶段与偏差规则；
- 待对齐：GAP-034 未决子项、结构化字段字典、实现与页面；
- 当前停在：`SCENARIOS_READY`，下一步 BIZ_ALIGNED。
