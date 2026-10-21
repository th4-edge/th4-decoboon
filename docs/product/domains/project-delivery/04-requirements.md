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
