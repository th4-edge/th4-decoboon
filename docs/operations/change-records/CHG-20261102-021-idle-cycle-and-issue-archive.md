# CHG-20261102-021 · 空闲周期、领域关闭与问题台账归档

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261102-021 |
| 类型 | 重构 / 治理 |
| 风险级别 | L0 |
| 状态 | 已关闭 |
| 负责人 | Codex |
| 日期 | 2026-11-02 |
| 关联需求/事故/业务裁决 | 用户要求按轻量 AI 工作目录建议继续收口 |

## 2. 执行内容

- `PROGRAM-BOARD.json` 切为 `IDLE`，清空 activeLease、queue、blockers。
- 11 个领域 state 切为 `CYCLE_CLOSED`，归档 openDecisions/blockers，活跃数组清空。
- `scripts/domain_state.py` 支持 `CYCLE_CLOSED`。
- `scripts/program_watch.py` 在 IDLE 周期返回 `IDLE`，不再建议全量深检。
- 完整问题 registry 与 40 份问题档案移入 `docs/product/issue-governance/archive/20261102-cycle-close/`。
- 活跃 `registry.json` 变为空台账，旧 `issues/` 保留归档符号链接。
- 新增 `docs/operations/AI工作索引.md`，作为新需求、需求变更、BUG、实现、验收、发布和事故的统一入口。

## 3. 验证

| 检查 | 结果 |
| --- | --- |
| 11 个领域 `phase=CYCLE_CLOSED` | PASS |
| `domain_state.py show` 活跃 open/blockers 为 0 | PASS |
| `program_watch.py --json` 返回 `IDLE` | PASS |
| 活跃 issue registry `issues=[]` | PASS |
| 历史 40 条问题完整归档 | PASS |
| AI 工作索引路径和命令存在 | PASS |
| 三仓 `git diff --check` | PASS |

## 4. 结论

装得清当前进入无活跃任务、无活跃问题、无领域写租约的空闲周期。历史事实和证据保留，但默认不读；新任务必须从 AI 工作索引重新立项。
