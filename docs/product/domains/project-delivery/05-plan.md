# 项目施工履约与进度 · 垂直切片计划

## 北向目标

合同施工范围通过阶段、任务、排期、节点留证和进度投影形成可追溯的履约闭环，不脱离合同或已生效变更独立造任务。

## 批次

| 批次 | 垂直切片 | 涉及角色 | 涉及对象 | 后端 | App/Web | 业务校核 | 退出条件 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| B1 | 业务对齐、场景与蓝图 | 产品/设计/PM/Boss | Project/Stage/Task/Node/Deviation | 只读核对 | 无 | DEC-048/049/051/057 | BIZ_ALIGNED + BLUEPRINT_READY | IN_PROGRESS |
| B2 | 任务来源与排期收敛 | 设计/PM/Boss | ContractLine/Task/Stage | 待用户授权 | App/Web | 无来源/超窗/幂等 | DEV_VERIFIED | PENDING |
| B3 | 节点留证与进度投影 | PM/业主 | TaskNode/Report/Project | 待用户授权 | App/Web | 证据/权限/跨端 | UI_REVIEWED | PENDING |
| B4 | 独立验收 | independent | 全部 | 无实现 | App/Web | 正反向/越权/边界 | ACCEPTED 建议 | PENDING |
| B5 | 发布准备 | release | 后端/Web/App | 构建/体验版 | App 体验版 | 冒烟/回滚 | RELEASED 或决策包 | PENDING |

## 依赖与顺序

- 依赖合同变更、验收整改和材料域的跨域契约。
- B1 在业务冲突未闭合前不得进入编码。
- B2/B3 必须等材料域释放代码租约。
- 独立验收不得由实现者自批。

## 回滚

- 后端上一 release；
- Web 静态构建回退；
- App 上一体验版；
- 不新增迁移；涉及金额/成本/权限时另行确认。

## 门禁

- 本批结束停在：`BIZ_ALIGNED` 或 `BLUEPRINT_READY`。
- 下一批开始前必须：解决 GAP-034 未决子项、确认任务来源、阶段算法和现场汇报角色。

## 业主端产品化 UI V2 垂直切片

| 批次 | 垂直切片 | 角色 | 页面/对象 | 退出条件 | 状态 |
| --- | --- | --- | --- | --- | --- |
| OWNER-UI-V2-A | Owner Token、页面壳、五槽导航、预览安全层 | 业主/预览员工 | Owner Token、OwnerFactHero、OwnerTabBar、OwnerSection、OwnerSignoffCard | type-check/build/contract PASS；UI review pending | DEV_VERIFIED |
| OWNER-UI-V2-B | 五类业务详情页 + 材料计划独立页 | 业主/预览员工 | quote/contract/selection/material-plan/change | 页面与事实已接入；待 UI review | DEV_IN_PROGRESS |
| OWNER-UI-V2-C | 同源 UI 对账与微信开发者工具走查 | 业主/预览员工 | 全部 Owner 页面 | 三档渲染、无横向溢出、业务区一致 | PENDING |
| OWNER-UI-V2-D | 真机/业主账号验收 | independent | 全部 Owner 页面 | 用户确认与独立证据 | PENDING |

### 本批开发边界

- 不新增 DDL。
- 不改变业务状态机、金额、成本、权限。
- 先做主页面组件层和五类详情页；不改员工内部工作台业务页。
- 所有预览页面复用业主页面，不新增平行预览页面。

## PI-20261022-014 阶段验收与停复工切片

| 批次 | 垂直切片 | 角色 | 对象 | 后端 | App/Web | 业务规则 | 退出条件 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| PROJ-STATE-D1 | 合同验收模式继承到项目阶段 | 项目成员/业主只读 | ContractConstructionStage/ProjectStage | acceptanceMode + 回填 + 回读 | 阶段详情/列表 | DEC-087/BR-053 | 迁移、定向测试、构建、真实页面；模式同源 | DEV_VERIFIED / 独立验收待执行 |
| PROJ-STATE-D2 | 三模式完成分流 | PM/店长/BOSS/业主 | ProjectStage/Acceptance/Task | 自动完成、内部完工、业主验收 | App/Web/业主 | DEC-087/BR-053 | 假待办、代确认、轮次和取消任务负向通过 | PENDING |
| PROJ-STATE-D3 | 停复工生命周期 | PM/店长/BOSS | ProjectSuspension/Notice | 申请/审批/通知/审计/复工重推 | App/Web/业主摘要 | DEC-087/BR-053 | 权限、幂等、四目标状态、历史补录通过 | PENDING |
| PROJ-STATE-D4 | 停工动作矩阵与下游联动 | PM/财务/材料 | Task/验收/材料/合同/偏差/应收 | 阻断与例外门禁 | 三端回读 | DEC-087/BR-053/DEC-051 | 阻断/例外、计划应收、并发补偿通过 | PENDING |
| PROJ-STATE-D5 | 三端一致性与独立验收 | independent | 全部 | 无新增实现 | App/Web/业主 | DEC-087/BR-053 | 真实页面/真机、越权、恢复、签署 | PENDING |
| PD-X1 | 施工排期、节点留证、现场日报双端写能力 | Designer/PM/Boss/店长 | Task/TaskNode/Report | 统一动作权限与 Provider | App/Web 同源写页面 | DEC-049/DEC-088/BR-054 | 同角色动作一致、证据/日报角色、失败恢复、独立验收 | DEV_VERIFIED / ACCEPTANCE_PENDING |

- 一次只做一个垂直切片；D1 未通过不得直接展开 D2-D5。
- D3/D4 涉及 DDL、权限、状态机和应收联动，必须全量验证、备份、回滚与用户确认。
- `PD-X1` 来自 `PI-20261022-015 / T12-BIZ-20261022-009`；未授权编码，不能与停工/复工切片合并验收。
- 业务裁决完成不等于编码授权；当前没有代码、迁移或发布改动。
