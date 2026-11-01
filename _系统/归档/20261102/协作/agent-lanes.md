# Agent Lanes

## Lanes

| lane | purpose | current_session | write_scope | worklog | workspace |
|---|---|---|---|---|---|

## Archived Lanes（2026-10-28）

当前无活跃 lane，项目按试运营单人模式切换为单 Agent 工作模式。
以下智能体均已归档或退役，不再接收新任务。历史 worklog、workspace、shared requests 和业务产出均保留；恢复前必须经用户明确确认并重新绑定会话。

| lane | status | archived_at | reason | session_id | historical_worklog |
|---|---|---|---|---|---|
| product-planning | archived | 2026-10-28 | 用户要求归档，切换单 Agent 模式 | `01a0a51f-cab9-7c02-8709-7cf6c9d28648` | `_系统/协作/lanes/product-planning/worklog.md` |
| development | archived | 2026-10-28 | 用户要求归档，切换单 Agent 模式 | `01a0d9e7-fdda-7d33-b4a1-0218d1dcbccd` | `_系统/协作/lanes/development/worklog.md` |
| acceptance | archived | 2026-10-28 | 用户要求归档，切换单 Agent 模式 | `01a0a51f-d47a-71d1-817e-dc719276654c` | `_系统/协作/lanes/acceptance/worklog.md` |
| material | retired | 2026-09-15 / cleaned 2026-10-28 | 已禁用、未绑定，营销素材职责暂停 | — | `_系统/协作/lanes/material/worklog.md` |
| topic-selection | retired | 2026-09-15 / cleaned 2026-10-28 | 已禁用、未绑定，营销选题职责暂停 | — | `_系统/协作/lanes/topic-selection/worklog.md` |
| content-writing | retired | 2026-09-15 / cleaned 2026-10-28 | 已禁用、未绑定，营销内容职责暂停 | — | `_系统/协作/lanes/content-writing/worklog.md` |
| design | retired | 2026-09-15 / cleaned 2026-10-28 | 已禁用、未绑定，营销设计职责暂停 | — | `_系统/协作/lanes/design/worklog.md` |
| data-review | retired | 2026-09-15 / cleaned 2026-10-28 | 已禁用、未绑定，营销数据复盘职责暂停 | — | `_系统/协作/lanes/data-review/worklog.md` |

## Web 端规范与门禁（2026-10-01）

Web 仓库 `../../前端代码/th4-decoboon-web` 与 App 同级纳入本工作台约束：

- 产品技能：`.agents/skills/zhuangdeqing-product-web-development`（已禁用 2026-10-28；`SKILL.md` 已改为 `.disabled`，不再自动调用）。
- UI/交互技能：`.agents/skills/zhuangdeqing-web-ui-interaction`（已禁用 2026-10-28；历史规范和 references 保留，仅作为人工参考）。
- 沉浸审计技能：全局 `zhuangdeqing-web-immersion-audit`（已禁用 2026-10-28；后续 Web 审计由主 Agent 直接执行）。
- 工程规范：`../../前端代码/th4-decoboon-web/AGENTS.md`、`docs/development/WEB-FRONTEND-STANDARD.md`。
- 设计系统：`docs/product/WEB-DESIGN-SYSTEM.md`。
- 页面门禁：`docs/product/WEB-PAGE-DELIVERY-GATE.md`（G1 开工门禁 / G2 交付门禁）。
- 页面门禁：`docs/product/WEB-PAGE-DELIVERY-GATE.md`（G1 开工 / G1.5 设计冻结 / G2 交付 / G3 视觉门禁）。
- UI 规范：`docs/product/WEB-UI-INTERACTION-SPEC.md`、`docs/product/WEB-COMPONENT-SPEC.md`、`docs/product/WEB-VISUAL-DEBT-PLAN.md`。
- 自动守卫：`npm run verify:ui`（UI Craft Guard，棘轮策略，基线 `scripts/ui-craft-baseline.json`）。
- 验收口径：`docs/acceptance/WEB-沉浸式业务闭环审计协议.md`、`docs/acceptance/WEB-RENDER-ACCEPTANCE.md`。

硬性要求：Web 页面交付必须附真实浏览器渲染证据（1440×900 / 1280×800 / 1024×768）；静态守卫（`verify:*`）、类型检查与构建成功不构成渲染证据；未通过 G1/G1.5 不得编码，未通过 G2/G3 不得表述为"页面已完成"；视觉债务只减不增。
