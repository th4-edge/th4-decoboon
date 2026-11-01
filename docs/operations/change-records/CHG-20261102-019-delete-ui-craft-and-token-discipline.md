# CHG-20261102-019 · 删除 UI Craft 并固定 Token 读取纪律

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261102-019 |
| 类型 | 重构 / 治理 |
| 风险级别 | L0 |
| 状态 | 已关闭 |
| 负责人 | Codex |
| 日期 | 2026-11-02 |
| 关联需求/事故/业务裁决 | 用户要求删除 `zhuangdeqing-ui-craft` 智能体并落地固定读取纪律 |

## 2. 范围

- 删除活跃 `zhuangdeqing-ui-craft` Skill，归档到 `_系统/归档/20261102/技能/删除/`。
- 更新后端、Web、App 的 AGENTS、Skill README、Disabled Registry 和上下文阅读规范。
- 增加固定读取纪律：按 ID 查台账、限定领域包、按章节读 rules、单 reference、验收摘要、构建/监控失败行、一个任务一个主 Skill。
- 非范围：运行时代码、业务规则、Provider、权限、金额、DDL、数据库和生产环境。

## 3. 决策

- `ui-craft` 不再作为活跃 Skill，也不再被 Web UI 入口引用。
- Web UI 规则收敛到 Web 仓库 `src/AGENTS.md` 和 `docs/standards/**`。
- 仅做静态文档/Skill 治理，不产生运行时变更。

## 4. 验证

| 检查 | 命令/结果 | 状态 |
| --- | --- | --- |
| 活跃 Skill 目录 | 不再包含 `zhuangdeqing-ui-craft` | PASS |
| Skill Registry | `skills.json` 不再包含 `ui-craft` | PASS |
| Disabled Registry | 指向归档路径且 `deleted_by_user=true` | PASS |
| 后端/Web/App AGENTS | 固定读取纪律已写入 | PASS |
| 活跃引用 | 除删除说明和历史归档外不再引用 ui-craft | PASS |
| 格式 | 三仓 `git diff --check` | PASS |

## 5. 结论

`ui-craft` 已从活跃 Agent 集合删除，并保留归档恢复路径。当前活跃项目 Skill 为 6 个；固定读取纪律已进入项目入口和上下文规范。
