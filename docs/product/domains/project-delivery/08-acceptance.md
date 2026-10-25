# 项目施工履约与进度 · 独立验收

## 范围

- Batch：B4（未开始）
- 角色：independent
- 环境：本地/dev；生产另行确认
- 数据/租户：真实项目，临时数据必须清理

## 用例

| 类型 | 用例 | 期望 | 实际 | 证据 | 结果 |
| --- | --- | --- | --- | --- | --- |
| 正向 | 合同施工行→任务→排期→节点→验收 | 全链路可追溯 | 待执行 | 待采 | PENDING |
| 反向 | 手工任务/合同外直接排期 | 阻断并提示走增减项 | 待执行 | 待采 | PENDING |
| 越权 | 跨项目/跨门店 | 404/403 | 待执行 | 待采 | PENDING |
| 幂等 | 重复开工/节点提交 | 不重复 | 待执行 | 待采 | PENDING |
| 边界 | 超窗/空阶段 | 偏差/本项不涉及 | 待执行 | 待采 | PENDING |
| 跨端 | App/Web/业主 | 进度与预计完工一致 | 待执行 | 待采 | PENDING |

## 结论

- 状态：PENDING
- 未覆盖：全部实现与页面
- 遗留风险：GAP-034
- 是否允许进入下一批：待独立验收

## PI-20261022-014 独立验收矩阵（待执行）

| 切片 | 正向 | 反向/越权 | 幂等/并发 | 跨端/恢复 | 状态 |
| --- | --- | --- | --- | --- | --- |
| PROJ-STATE-D1 | 三模式继承且可追溯 | 按名称猜测、来源缺失自动完成 | 重复立项不重复阶段 | 阶段详情/列表同源 | DEV_EVIDENCE_READY / INDEPENDENT_PENDING |
| PROJ-STATE-D2 | None 自动、ConstructionDone 内部确认、AcceptanceDone 业主确认 | 无任务完成、员工代业主、未完成提交 | 重复提交/确认 | 业主待办与内部完成一致 | PENDING |
| PROJ-STATE-D3 | PM 申请、店长/BOSS 审批、复工重推 | PM 自批、财务审批、跨店/跨企业 | 重复申请/审批/复工 | 三端停工原因、日期、下一步一致 | PENDING |
| PROJ-STATE-D4 | 到场、既有 Pending 验收、合同变更、财务事实例外 | 停工期间推进施工/新下单/新提交验收 | 并发停工/复工、通知重试 | 状态、金额、应收到期同源 | PENDING |
| PROJ-STATE-D5 | 真实 App/Web/业主完整旅程 | 员工预览代业主、其他项目访问 | 重放与断网恢复 | 390/430/375 与 Web 宽屏 | PENDING |

- 执行方：independent，不得由实现方自批。
- 证据：真实账号、真实项目、截图/录屏、接口回读、SQL/金额对账、越权和失败态。
- 未执行前不得将任何切片标记为 `ACCEPTED` 或对外宣称已上线。

## PI-20261022-015 / PD-X1 独立验收矩阵（待执行）

| 用例 | 期望 | 当前开发证据 | 独立验收 |
| --- | --- | --- | --- |
| 同角色 App/Web 动作集合 | Designer 可排期、不可发布日报；PM 可排期/节点/日报；BOSS/店长可兜底 | Web 路由/菜单/页面 + App `act:report` 矩阵静态检查通过 | PENDING |
| 三类节点留证 | Start/Conceal/Finish 均要求照片/视频；隐蔽未完成阻断 Finish | `TaskNodeSaveAcceptanceTest` 13/13 PASS；Web/App 同 Provider | PENDING（真实 UI） |
| 日报发布权限 | PM/BOSS/店长成功；Designer/Finance/Marketer 被拒 | 角色上下文 + Provider 入口 4/4、StoreManager 范围 1/1、`ProviderSecurityAcceptanceTest` 3/3 PASS | PENDING（真实 HTTP/UI） |
| 排期调整 | 只调整合同/变更来源任务的阶段、日期、责任人、备注 | `ProjectPlanFromScheduleTest` 1/1 PASS；手工新增/物理删除继续阻断 | PENDING（真实 UI） |
| 失败恢复 | 上传/保存失败保留上下文并可重试；重复节点不重复 | 页面状态与后端幂等门禁已实现，需真实操作 | PENDING |
| 跨企业/跨门店/非项目参与 | 403/404 | 服务端数据范围沿用既有门禁，需独立负向证据 | PENDING |
| 真实页面 | Web 1440/1024；App 390/375 | Web 构建与 App 构建通过；真实截图/真机未采 | PENDING |

- 当前结论：`DEV_VERIFIED`，不得标记 `ACCEPTED`。
- 执行方：independent；实现方不得自批。
