# CHG-20261029-025 · UI Craft 对话知识沉淀与待我设计优化 Brief

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | `CHG-20261029-025` |
| 类型 | UI Craft 知识 / 页面设计 Brief / 文档 |
| 风险级别 | L0 |
| 状态 | `DOCS_READY` |
| 负责人 | codex |
| 日期 | 2026-10-29 |
| 关联需求/事故/业务裁决 | `CHG-20261029-020`、`CHG-20261029-021`、`CHG-20261029-022`、`BR-052`、`BR-059`、`BR-060`、`DEC-094`、`DEC-095` |

## 2. 目标与范围

- 目标：把本对话中形成的页面 UI 判断、动效边界、账号菜单、通知队列、内部编号规则和门禁沉淀为下一批页面优化可读取的知识。
- 范围内：UI Craft 对话 playbook、视觉学习索引、Web「待我设计」页面优化 brief、设计菜单文档引用。
- 非范围：页面代码、Provider、状态机、金额/成本/利润、权限、DDL、数据库迁移和生产发布。
- 受影响端：Web UI 设计与后续研发规划。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：否，仅引用既有业务规则，不新增业务规则。
- 业务事实来源与裁决编号：`BR-052`、`BR-059`、`BR-060`、`DEC-094`、`DEC-095`、`INFO-DESIGN-001~005`、`INFO-QUOTE-001~004`、`INFO-MEASURE-003`。
- 尚存 GAP 或假设置：设计工作区当前 `items.reduce` 计算方案金额与服务端派生值规则存在待核验风险；待我设计优化前必须确认服务端权威金额投影。
- 页面意图：
  - 列表页：设计任务队列 + 当前房屋设计工作面；
  - 工作区：单方案对象详情工作台。

## 4. 验收条件

| 序号 | 场景 | 预期结果 | 证据 |
| --- | --- | --- | --- |
| 1 | 后续 Agent 启动待我设计优化 | 能读取统一 UI playbook 与页面 brief，不需要重新翻聊天记录 | 文件存在且互相引用 |
| 2 | 页面结构规划 | Brief 明确页面类型、目标角色、第一问题、三秒结论、唯一主行动、三层信息和七态 | Brief §5/§6/§7/§9 |
| 3 | 业务口径 | 不新增设计状态、报价条件、金额公式或权限；引用既有事实源 | Brief §7/§12/§13 |
| 4 | 编号规则 | 后续页面不得重新展示内部 ID 或系统编号 | `BR-060` / `check:identifier-exposure` |
| 5 | AI 味防护 | 后续页面不得用卡片墙、英文 eyebrow、循环动效或装饰性 3D 代替任务结构 | `check:ai-flavor` / Brief §4/§8 |
| 6 | 真实渲染门禁 | 优化实现阶段必须完成三档截图、两轮批判和七态验证 | Brief §10/§14 |
| 7 | 文档完整 | playbook、visual learning index、页面 brief 和菜单引用形成交叉链接 | `git diff --check` |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | `dev` | 未提交 | `.agents/skills/zhuangdeqing-ui-craft/references/conversation-playbook-20261029.md`、`visual-learning-loop.md`、`SKILL.md`、本变更记录 |
| th4-decoboon-web | `dev` | 未提交 | `docs/product/designer/WEB-DESIGNER-PENDING-DESIGN-OPTIMIZATION-BRIEF.md`、`WEB-DESIGNER-MENU-FROZEN.md`、`WEB-DESIGNER-BUSINESS-WORKBENCH-G1-ALIGNMENT.md` |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| 文档格式 | `git diff --check` | PASS / 文档无尾随空格 | 命令输出 |
| 交叉引用 | 人工检索 playbook、brief、菜单文档 | PASS | 文件内容 |
| 页面代码 | 无修改 | N/A | — |
| 构建/测试 | 不适用（L0 文档） | N/A | — |

- 未执行项及原因：本批只沉淀知识，不实现页面，因此不运行 Web 构建、真实渲染或视觉评审。
- 已知失败及是否与本次相关：无。

## 7. 发布

- 无需发布；不适用。
- 数据库迁移：无
- 配置/密钥变化：无

## 8. 回滚

- 回滚条件：知识文档与冻结规范冲突、页面 brief 被误当作业务裁决、引用路径失效。
- 回滚步骤：删除新增 playbook 和 brief，恢复菜单文档引用；不涉及生产代码。
- 实际是否回滚：否

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集
- 人工耗时：未采集
- 一次通过：是
- 最终结论：知识就绪，待用户确认后进入「待我设计」页面设计切片

## 10. 复盘与沉淀

- 是否出现重复问题：否
- 是否需要新增门禁：否；复用现有 AI 味、编号、品牌和真实渲染门禁
- 候选 `EXP/ANT/PATH`：PATH：先沉淀任务链和页面母型，再做页面；ANT：直接重画卡片或增加 KPI
- 后续动作与负责人：从 `WEB-DESIGNER-PENDING-DESIGN-OPTIMIZATION-BRIEF.md` 的 `Pending-Design-D1` 开始
