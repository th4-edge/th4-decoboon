# 项目施工履约与进度 · 需求与验收条件

| ID | 用户故事 | 前置条件 | 权限 | 数据/状态 | 页面/交互 | API/事实 | 验收 | 来源 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-PD-001 | 设计师能从合同施工行创建任务 | 合同生效、项目已立项 | 设计师/PM/Boss | ContractLine→ProjectTask | App/Web 排期页 | project/task/save | 无来源任务被阻断 | DEC-049 |
| REQ-PD-002 | Boss/店长能指定 PM | 项目已立项 | Boss/本店店长 | Project.manager | 项目详情 | project/project/update | PM 未指定不得开工 | GAP-034 |
| REQ-PD-003 | 用户能区分合同基准和预计完工 | 合同计划已生成 | 项目成员 | ProjectContract.planEndDate/Project.expectedEndDate | 项目详情 | project/info | 排期不改基准 | DEC-048 |
| REQ-PD-004 | PM 能在 App/Web 完成开工/隐蔽/完工留证 | 任务已排期 | PM；Boss/店长兜底 | TaskNode | 双端任务详情 | project/task-node/save | 证据缺失阻断完成；双端同源 | SCN-003/DEC-088 |
| REQ-PD-005 | 系统能识别计划偏差 | 排期超窗 | 项目成员 | ProjectPlanDeviation | 项目进度 | project/plan-deviation | 超窗必须生成偏差 | DEC-051 |
| REQ-PD-006 | 空阶段不被静默跳过 | 标准阶段无合同施工行 | PM/Boss | StageStatus.NotApplicable | 阶段详情 | project/stage/update | 分类、留痕、进度剔除 | DEC-057 |
| REQ-PD-007 | 合同外施工先走增减项 | 客户提出新增施工 | 设计师/PM/Boss | ContractChange | 变更入口 | batch-save | 不生成手工任务 | DEC-049 |
| REQ-PD-008 | 项目进度不受跨租户越权影响 | 任意访问 | 当前登录员工 | Project/Stage/Task | App/Web | DataScope | 404/403 | BR-001 |
| REQ-PD-009 | 重复排期/节点提交幂等 | 重复操作 | 当前角色 | Task/TaskNode | App/Web | 幂等 | 不重复生成 | domain protocol |
| REQ-PD-010 | 预计完工和动态对业主可见 | 项目有效 | 业主本人 | 可见字段 | 业主端 | owner project | 无成本/内部备注 | DEC-048 |
| REQ-PD-011 | 同角色能在 App/Web 新增/调整/删除施工任务 | 合同施工行/生效变更存在 | 设计师/PM/Boss/店长 | ProjectTask | 双端排期页 | project/task/save\|update\|delete | 双端动作集合相同；来源/状态/错误恢复同源 | DEC-049/DEC-088 |
| REQ-PD-012 | PM/Boss/店长能在 App/Web 发布现场日报 | 项目有效、任务/阶段存在 | PM + BOSS/本店店长 | ProjectReport | 双端日报页 | project/report/save | 设计师/财务/业务员只读；业主仅授权摘要 | DEC-088/BR-054 |

## 非功能要求

- 进度/统计使用服务端投影，不在前端循环计算。
- 计划偏差和节点证据必须可追溯。
- 成本缺失时利润 null，不用 0。
- 跨端状态与下一步同源。

## 明确不做

- 不做手工任务。
- 不做工艺库直接下发。
- 不做自动验收、自动完工、自动顺延合同基准。

## 业主端产品化 UI V2

| ID | 用户故事 | 前置条件 | 权限 | 数据/状态 | 页面/交互 | API/事实 | 验收 | 来源 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-OUI2-001 | 业主与员工预览看到同一业务 UI | 员工具备预览权限 | 业主本人/预览白名单角色 | CustomerJourney 同源 | 同路由同组件 | journey/info、owner-preview/info | 除安全层外布局一致 | USER-DECISION-20261021-001/002 |
| REQ-OUI2-002 | 业主能完整查看报价详情 | 项目存在 | 业主本人 | quote section | owner-quote | acceptedBy/acceptedAt/priceGroups | 版本、构成、差异与登记来源完整 | USER-DECISION-20261014-004 |
| REQ-OUI2-003 | 业主能完整查看合同详情 | 合同生效 | 业主本人 | contract section | owner-contract | signed/current/signedAt/effectiveAt/registeredBy | 金额、付款、附件与签署登记完整 | BR-002、DEC-015 |
| REQ-OUI2-004 | 业主能完整查看选材详情 | 已形成选材 | 业主本人 | material section | owner-selection | selection facts | 品牌、规格、版本与设计师登记完整 | DEC-056、BR-009 |
| REQ-OUI2-005 | 业主能完整查看材料计划 | 已形成计划 | 业主本人 | material section | owner-material-plan | plan facts | 状态、数量、到场日期完整；无确认按钮 | USER-DECISION-20261014-003 |
| REQ-OUI2-006 | 业主能查看增减项结果 | 有变更单 | 业主本人 | change section | owner-change | status/effective facts | 审批中不显示金额，生效后显示结果 | DEC-068 |
| REQ-OUI2-007 | 预览不出现写动作 | 预览激活 | 员工预览 | readOnly/previewMeta | OwnerPageShell | owner-preview/info | 确认/驳回/分享/上传不出现 | T12-BIZ-20261021-001/002 |
| REQ-OUI2-008 | 详情页在空/错/部分同步时可恢复 | 任意 | 业主/预览 | loading/empty/error | 详情页 | PageState | 三态与重试可用 | UI gate |

## PI-20261022-014 阶段验收与停复工需求

| ID | 用户故事 | 前置条件 | 权限 | 数据/状态 | 页面/交互 | API/事实 | 验收 | 来源 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-PI14-001 | 项目阶段能显示并追溯合同验收模式 | 合同计划快照存在 | 项目可见角色 | ContractConstructionStage→ProjectStage.acceptanceMode | 阶段详情/列表 | 立项继承 | 日期重排不改模式；历史缺失显示待核验 | DEC-087 / BR-053 |
| REQ-PI14-002 | `None` 阶段完成后无业主假待办 | 有效任务与证据完整 | 项目成员 | StageStatus.Done | 阶段详情/业主端 | 服务端自动完成 | 无任务不得自动完成 | BR-053 |
| REQ-PI14-003 | `ConstructionDone` 由内部确认完成 | 有效任务与证据完整 | PM；BOSS/本店店长可代 | StageStatus.Done | 阶段详情 | 内部完工 Provider | 无法确认成功；无业主待办 | BR-053 |
| REQ-PI14-004 | `AcceptanceDone` 由业主本人确认或驳回 | 员工提交且证据完整 | 业主本人 | Acceptance/AcceptanceRound/StageStatus | 业主验收 | customer/acceptance/update | 员工代确认拒绝；原验收单追加轮次 | BR-008 / DEC-015 / DEC-087 |
| REQ-PI14-005 | PM 发起停工、店长/BOSS 审批 | 项目 Preparing/Ongoing/Accepting | PM 发起；店长/BOSS 审批 | ProjectSuspension/ProjectStatus.Suspended | 项目停复工页 | suspension save/approve | PM 自批拒绝；重复审批幂等 | BR-053 |
| REQ-PI14-006 | 停工冻结施工但不冻结物理/财务事实 | 停工 Active | 项目/材料/财务角色 | 阶段/任务/验收/材料/合同/应收 | 三端状态 | 动作门禁 | 新施工阻断；到场/收款/合同变更允许 | BR-053 |
| REQ-PI14-007 | 复工按当前事实重推项目状态 | 停工原因解决 | PM 申请；店长/BOSS 审批 | ResumePending→Ended | 项目详情/业主端 | suspension resume | 可重推 Preparing/Ongoing/Accepting/Finished | BR-053 |
| REQ-PI14-008 | 停工不自动改合同基准和应收到期 | 停工/复工 | 项目/财务角色 | Project/Contract/Receivable/Deviation | 项目/合同/财务 | 来源链回读 | 仅正式合同变更或确认偏差可重算 | DEC-048 / DEC-051 / DEC-087 |
