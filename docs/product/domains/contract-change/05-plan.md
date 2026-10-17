# 合同与增减项履约 · 垂直切片计划

## 北向目标

合同生效、增减项审批与合同、任务、材料、应收和成本的同源联动形成可追溯闭环。

## 批次

| 批次 | 垂直切片 | 涉及角色 | 涉及对象 | 后端 | App/Web | 业务校核 | 退出条件 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| B1 | 业务对齐、场景、结构化蓝图与字段字典 | 产品/Boss/财务/设计/PM | ContractChange/Item/Contract/Line | 只读核对 | 无 | DEC-055/DEC-068/BR-041 | BIZ_ALIGNED + BLUEPRINT_READY | COMPLETE (2026-10-17) |
| B2 | 当前有效剩余、双入口收敛、目标快照与审批联动 | 设计/财务/Boss | 全部域对象 | batch-save/range/approve | 无 | 超减/并发/幂等/成本 | DEV_VERIFIED | PENDING |
| B3 | Web 变更工作台与合同联动解释 | 设计/Boss/财务 | 变更/合同/应收/材料/任务 | 复用 Provider | Web | 范围、状态、下一步 | DEV_VERIFIED + UI_REVIEWED | PENDING |
| B4 | App 三类动作与真机闭环 | 设计/PM/Boss/财务 | 变更/材料/成本 | 复用现有 | App 体验版 | 正反向、异常恢复 | DEV_VERIFIED + UI_REVIEWED | PENDING |
| B5 | 跨域对账与独立验收 | independent | 全部 | 无实现 | Web/App/API | 正向/反向/越权/幂等/边界 | ACCEPTED 建议 | PENDING |
| B6 | 发布准备 | release | 后端/Web/App | 构建/体验版 | App 体验版 | 冒烟/回滚 | RELEASED 或决策包 | PENDING |

## 依赖与顺序

- 必须先处理旧 `save` 与 `batch-save` 双入口，否则不能保证同一金额语义。
- B2 先于 B3/B4；页面不得自行计算可变更量。
- 材料域 B6/B7 与合同变更共用材料计划/采购/退货契约。
- 独立验收不得由实现者自批。

## 回滚

- 后端保留上一 release 与回滚脚本；
- Web 回退静态构建；
- App 回退上一体验版；
- 不新增 DDL 前必须先做业务裁决；涉及金额/成本变更必须全量测试。

## 门禁

- 本批结束停在：`BIZ_ALIGNED` 或 `BLUEPRINT_READY`，不得直接进入编码。
- 下一批开始前必须：确认双入口、range Provider、历史数据治理和成本完整性。
