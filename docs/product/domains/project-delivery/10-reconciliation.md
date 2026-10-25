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

- 本批仅完成业务与初始蓝图；PI-20261022-014 已完成 T06/T12，代码/页面映射待研发切片。

## 已过期

- 暂无。

## 未决业务问题

- GAP-034 阶段计划算法；
- GAP-034 G-02/G-06 工艺库与阶段/区域绑定。

已收敛：`GAP-034 G-01` 与 `G-07` 由 `DEC-087 / BR-053` 冻结；现场日报发布角色由 `DEC-088 / BR-054` 冻结为 PM + BOSS/本店店长。实现缺口分别转入 `GAP-066 / PI-20261022-014` 与 `GAP-067 / PI-20261022-015`。

## 本批结论

- 已对齐：核心任务来源、工期层次、节点、空阶段与偏差规则；
- 待对齐：GAP-034 剩余子项、结构化字段字典、实现与页面；
- PI-014/015 业务已对齐；`PROJ-STATE-D1` 已取得真实 MySQL/API/Web 证据，待独立验收和 App 真机；`PROJ-STATE-D2`～`D5` 未授权，`PD-X1` 为并行实施批次。
- 原 B1 业务门禁仍受 GAP-034 剩余项影响。

## PI-20261022-014 业务裁决对账（2026-10-22）

| REQ | 业务规则 | 蓝图/对象 | 字段/API | 页面 | 测试 | 验收 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| PI14-D1 | `DEC-087 / BR-053` | ProjectStage.acceptanceMode | 合同快照→项目阶段 | App/Web 阶段读投影 | 隔离定向测试/构建 PASS | 独立验收待执行；MySQL 迁移待执行 | DEV_VERIFIED / ACCEPTANCE_PENDING |
| PI14-D2 | `DEC-087 / BR-053` | None/ConstructionDone/AcceptanceDone | 阶段完成分流、取消任务门禁 | App/Web/业主端 | D2 用例待建 | 独立验收待执行 | BIZ_ALIGNED / UNMAPPED |
| PI14-D3 | `DEC-087 / BR-053` | ProjectSuspension | 申请/审批/复工/审计/通知 | App/Web/业主端 | D3 用例待建 | 独立验收待执行 | BIZ_ALIGNED / UNMAPPED |
| PI14-D4 | `DEC-087 / BR-053` | 停工动作矩阵与下游联动 | 项目/任务/材料/变更/偏差/应收 | 三端状态与恢复路径 | D4 用例待建 | 独立验收待执行 | BIZ_ALIGNED / UNMAPPED |
| PI14-D5 | `DEC-087 / BR-053` | 三端一致性 | 同源服务端投影 | App/Web/业主端 | 跨端/越权/幂等待建 | 独立验收待执行 | BIZ_ALIGNED / UNMAPPED |

说明：D1 已进入 `DEV_VERIFIED`；D2-D5 仍为 `BIZ_ALIGNED / UNMAPPED`。MySQL 迁移、真实页面和独立验收完成前不得关闭问题。

## 独立切片对账：业主视角只读预览 P0（2026-10-21）

| REQ | 场景 | 业务规则 | 蓝图节点 | 字段/API | 页面 | 测试 | 验收 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-OP-001 | 员工查看本项目业主投影 | USER-DECISION-001/002、T12-001 | owner-preview.employee-projection | project/owner-preview/info | App owner-preview | OwnerPreviewInfoAcceptanceTest | 真实员工 token/真机待补 | PENDING |
| REQ-OP-002 | 预览与业主投影同源 | T12-001/002 | owner-preview.journey-mode | CustomerJourney.EmployeeOwnerPreview | App 首页/全过程/验收 | CustomerJourneyOwnerPreviewModeTest、OwnerPreviewInfoAcceptanceTest | 页面视觉字段待补 | PENDING |
| REQ-OP-003 | 预览只读无副作用 | T12-001 | owner-preview.read-only | actions=[]；previewMeta.readOnly | 无业主写按钮 | OwnerPreviewInfoAcceptanceTest | 真机交互待补 | PENDING |
| REQ-OP-004 | 最小审计 | USER-DECISION-002、T12-001/002 | owner-preview.audit | db_project_owner_preview_log | 后台无页面 | 集成测试 + MigrationSchemaParityTest | MySQL 迁移/查询验收待补 | PENDING |
| REQ-OP-005 | 角色与项目范围拒绝 | USER-DECISION-001、T12-001 | owner-preview.access | 5 角色白名单 + requireProject | 入口/守卫 | 集成测试 + App 契约 | 独立越权验收待补 | PENDING |

### 本切片结论

- 设计与实现已映射：路由、能力键、投影模式、Provider、审计迁移、App 页面均有代码与测试证据。
- 未映射/过期：本切片无。
- 未决：真实 MySQL 迁移、真实微信员工会话、真机 UI、独立验收与发布。
- 本结论不推进原 B1 `GAP-034` 门禁。

## 业主端产品化 UI V1（OWNER-UI-V1，2026-10-21）

| REQ | 场景 | 业务规则 | 蓝图节点 | 字段/API | 页面 | 测试 | 验收 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-OUI-001 | 业主日常打开 | SCN-003、DEC-015、DEC-048 | owner-ui.home | project/owner-preview/info、customer/project/journey/info | App 我的家 | verify:owner-preview-product-ui | 真机/用户确认待补 | UI_REVIEWED |
| REQ-OUI-002 | 查看施工节点 | BR-008、DEC-048 | owner-ui.progress | journey construction section | App 进度 | type-check/build/四页走查 | 真机待补 | UI_REVIEWED |
| REQ-OUI-003 | 核对付款 | 业主金额口径：应付/已付/待付 | owner-ui.money | journey payment section | App 我的钱 | 金额事实与页面走查 | 真机待补 | UI_REVIEWED |
| REQ-OUI-004 | 回看装修档案 | USER-DECISION-20261014-002 | owner-ui.archive | journey sections | App 家档案 | 八类档案走查 | 真机待补 | UI_REVIEWED |
| REQ-OUI-005 | 浏览完整历程 | SCN-003、DEC-015 | owner-ui.journey | customer/project/journey/info | App 装修历程 | 四主 Tab + 中央入口走查 | 真机待补 | UI_REVIEWED |
| REQ-OUI-006 | 员工预览 | USER-DECISION-001/002 | owner-preview.read-only | project/owner-preview/info | 同业主页面 | 15/15 自动化 + 水印/只读 | 独立验收待补 | UI_REVIEWED |
| REQ-OUI-007 | 未来 7 天计划 | 待业务裁决 | owner-ui.upcoming | 待补 | 首页/进度 | 未实现 | 待裁决 | GAP |

### 本批结论

- 10-14 原型的四主 Tab + 中央装修历程骨架已落地。
- 首页、进度、我的钱、家档案、装修历程均复用真实业务事实。
- 未伪造未来 7 天、质保和逐项验收数据。
- 下一步是用户视觉确认与真实业主账号走查；正式验收仍需独立证据。

## 业主端产品化 UI V2（OWNER-UI-V2，2026-10-21）

| REQ | 业务规则 | 蓝图节点 | 数据投影 | 页面 | 测试 | 状态 |
| --- | --- | --- | --- | --- | --- | --- |
| REQ-OUI2-001 | T12-20261021-001/002 | owner-ui-v2.shell | CustomerJourney + previewMeta | 同业主页面 | parity contract | DEV_VERIFIED |
| REQ-OUI2-002 | USER-DECISION-20261014-004 | owner-ui-v2.quote | quote facts/items | owner-quote | page state/build | DEV_VERIFIED |
| REQ-OUI2-003 | BR-002、DEC-015 | owner-ui-v2.contract | contract facts/items | owner-contract | page state/build | DEV_VERIFIED |
| REQ-OUI2-004 | DEC-056、BR-009 | owner-ui-v2.selection | material selection items | owner-selection | page state/build | DEV_VERIFIED |
| REQ-OUI2-005 | USER-DECISION-20261014-003 | owner-ui-v2.material-plan | material plan items | owner-material-plan | no-confirm assertion | DEV_VERIFIED |
| REQ-OUI2-006 | DEC-068 | owner-ui-v2.change | change facts/items | owner-change | pending amount hidden | DEV_VERIFIED |
| REQ-OUI2-007 | T12-20261021-001/002 | owner-ui-v2.shell | previewMeta/readOnly | OwnerPageShell | write action scan | DEV_VERIFIED |
| REQ-OUI2-008 | domain protocol | owner-ui-v2.shell | loading/error/empty | all owner pages | state contract | DEV_PARTIAL |

### OWNER-UI-V2-C 同屏审查回写（2026-10-21）

| 对账项 | 证据 | 结论 |
| --- | --- | --- |
| 10 页业务区同源 | `docs/acceptance/runs/20261021-owner-ui-v2-c/parity-results.json` | PASS（375×667） |
| 业主/预览并排截图 | `screenshots/preview/*.png`、`screenshots/owner/*.png` | PASS |
| 真实待办口径 | `08-owner-journey-actions.json`、`09-preview-actions.json` | PASS：两侧 actions/pendingActionCount 均为 0 |
| 反馈隐私边界 | T12-BIZ-20261021-001；预览 section keys | PASS：预览不展示 feedback section |
| 预览安全层 | banner、watermark、readOnly、写动作 | PASS（静态/渲染层） |
| 多分辨率 | 390×844、430×932、375×812 | PENDING |
| 状态矩阵 | 空态、错误态、部分同步、长内容、极端数值 | PENDING |

结论：设计已冻结，A/B 主要代码已进入 DEV_VERIFIED，并完成 375×667 微信开发者工具首轮同屏审查；多分辨率、异常状态和独立验收未完成，不得标记 UI_REVIEWED。

## PI-20261022-015 跨端写能力（BIZ_DECIDED / 未实现）

| REQ | 业务规则 | 蓝图/对象 | 字段/API | 页面 | 测试 | 验收 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-PD-004 | `SCN-003/DEC-088` | TaskNode | project/task-node/save | App 任务详情/Web `/workbench/project-execution` | 三类节点状态夹具走查 + 构建通过；真实写入待独立验收 | 独立验收待执行 | DEV_VERIFIED |
| REQ-PD-011 | `DEC-049/DEC-088` | ProjectTask | project/task/save | App 排期/Web 统一履约工作面 | 任务来源/删除门禁保持；Web 构建/可达性 PASS | 独立验收待执行 | DEV_VERIFIED |
| REQ-PD-012 | `DEC-088/BR-054` | ProjectReport | project/report/save | App 日报/Web 统一履约工作面 | 权限定向 2/2 PASS + 对话框夹具走查 | 独立验收待执行 | DEV_VERIFIED |

说明：`PD-X1` 已进入实现；Web 静态页面、App 权限对齐和构建/定向门禁通过，但真实页面操作与独立验收尚未完成。

