# CHG-20261029-027 · 待我设计与设计工作台 UI 升级

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261029-027 |
| 类型 | 需求 |
| 风险级别 | L1 |
| 状态 | 待验证 |
| 负责人 | Codex |
| 日期 | 2026-10-29 |
| 关联需求/事故/业务裁决 | `WEB-DESIGNER-PENDING-DESIGN-OPTIMIZATION-BRIEF.md`、`INFO-DESIGN-001~005`、`INFO-QUOTE-001~004`、`INFO-MEASURE-003`、`BR-052`、`BR-059`、`DEC-094`、`BR-060`、`DEC-095` |

## 2. 目标与范围

- 目标：减少待我设计与设计工作台的重复信息，收拢唯一主行动，让设计分项成为首屏主工作面，并改善响应式和可访问性。
- 范围内：`/designer/design`、`/designer/design/:designId` 的页面结构、文案、样式、折叠区、展示顺序和 DOM 语义。
- 非范围：后端 Provider、接口、权限、状态机、金额公式、自动保存、表单语义、业务动作和路由行为。
- 受影响端：Web。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：否。页面继续按现有 Provider 展示事实，不新增或改写业务规则。
- 业务事实来源与裁决编号：沿用 `INFO-DESIGN-001~005`、`INFO-QUOTE-001~004`、`INFO-MEASURE-003`、`BR-052`、`BR-059`、`DEC-094`、`BR-060`、`DEC-095`。
- 尚存 GAP 或假设置：无新增业务 GAP。验收数据来自本地 mock Provider，尚待用户用目标环境确认视觉与真实长数据。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | 设计师进入待我设计 | 已登录且队列有数据 | 先看到待设计队列、当前对象、当前判断和一个主行动 | `docs/acceptance/runs/20261029-pending-design-ui-upgrade/` |
| 2 | 设计师打开设计工作台 | 方案有分项 | 设计分项位于首屏主工作面，支持资料默认折叠 | 同上 |
| 3 | 1440/1280/1024/390 渲染 | 本地 Vite + mock Provider | 无整页横向溢出，可见按钮命中区 ≥ 24px | 同上 |
| 4 | 主行动链路 | 当前待设计方案 | “打开设计工作区”进入对应方案，页面级主行动唯一 | `evidence/interaction-evidence.json` |
| 5 | 金额展示 | Provider 返回金额/不可用 | 只展示服务端 `calculatedAmount`；不可用时显示“暂不可确认” | `evidence/round2-evidence.json` |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | 当前工作区 | 未提交，待用户验收 | `docs/acceptance/runs/20261029-pending-design-ui-upgrade/`、本变更记录、专项 brief |
| th4-decoboon-web | 当前工作区 | 未提交，待用户验收 | `src/views/designer/DesignerCustomersView.vue`、`src/views/designer/DesignerDesignWorkspaceView.vue`、专项 brief |
| th4-decoboon-app | 不适用 | 不适用 | 不适用 |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| 类型检查 | `npm run type-check` | PASS | 本地命令输出 |
| AI 味门禁 | `npm run check:ai-flavor` | PASS | 本地命令输出 |
| 内部编号门禁 | `npm run check:identifier-exposure` | PASS | 本地命令输出 |
| 客户端文案门禁 | `npm run check:client-copy` | PASS | 本地命令输出 |
| 品牌主题门禁 | `npm run check:brand-theme` | PASS | 本地命令输出 |
| 生产构建 | `npm run type-check` + `npx vite build` | PASS（最终代码） | 本地命令输出 |
| 全量门禁 | `npm run build` | 首次 PASS；最终复跑被并行修改的 `StoreCustomerView.vue` 分页/文案门禁拦截 | 与本批无关，未修改该文件 |
| 真实渲染 | `/designer/design`、`/designer/design/d1` | PASS（正常态） | `docs/acceptance/runs/20261029-pending-design-ui-upgrade/screenshots/` |
| 键盘/减少动效 | 主行动 focus、`prefers-reduced-motion: reduce` | PASS | `evidence/accessibility-motion-evidence.json` |
| Console | 14 个页面宽度组合 | 0 error | `evidence/round2-evidence.json` |

- 未执行项及原因：未使用生产租户和真实长数据；由用户在目标环境完成视觉与业务体验验收。
- 已知失败及是否与本次相关：最终全量 `npm run build` 被并行任务修改的 `src/views/workbench/StoreCustomerView.vue` 拦截（新增固定大页查询和研发型文案）；该文件不属于本批范围。本批页面文件最终 `type-check` 与 Vite production build 通过。

## 7. 发布

| 字段 | 内容 |
| --- | --- |
| Release ID | 不适用（未发布） |
| 制品路径/SHA-256 | 不适用（用户验收前不构建发布制品） |
| 生产 release/版本 | 不适用 |
| 上一版本/回滚点 | 当前 Web 工作区 |
| 健康检查 | 不适用 |
| 发布后观察 | 不适用 |

- 数据库迁移：无
- 配置/密钥变化：无

## 8. 回滚

- 回滚条件：用户视觉验收不通过，或页面出现阻断设计的回归。
- 回滚命令/步骤：还原 `DesignerCustomersView.vue` 与 `DesignerDesignWorkspaceView.vue` 的本批改动；无数据迁移需要回滚。
- 回滚验证：重新渲染两个页面，确认原页面可进入。
- 实际是否回滚：否

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集
- 人工耗时：未采集
- 一次通过：否（完成两轮视觉预检与响应式修正）
- 返工原因：首轮 1440 对象条按钮被挤压、1024 单列顺序不合理、辅助文字动作命中区不足。
- 最终结论：待用户验收

## 10. 复盘与沉淀

- 是否出现重复问题：否
- 是否需要新增门禁：否
- 候选 `EXP/ANT/PATH`：无
- 后续动作与负责人：用户验收后决定是否微调并发布；如通过，再由用户指定发布批次。
