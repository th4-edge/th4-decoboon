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
