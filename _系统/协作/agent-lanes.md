# Agent Lanes

## Lanes

| lane | purpose | current_session | write_scope | worklog | workspace |
|---|---|---|---|---|---|
| product-planning | 负责需求拆解、PRD/规格、范围优先级和验收标准定义，覆盖 App 与 Web，不负责生产代码实现。 | codex:01a0a51f-cab9-7c02-8709-7cf6c9d28648 | docs/product/**,docs/plan/**,../../前端代码/th4-decoboon-web/docs/product/**,../../前端代码/th4-decoboon-web/docs/plan/**,../../前端代码/th4-decoboon-web-v2/docs/product/**,../../前端代码/th4-decoboon-web-v2/docs/plan/**,_系统/协作/lanes/product-planning/** | lanes/product-planning/worklog.md | lanes/product-planning/workspace |
| development | 负责后端、App 与 Web 功能实现、生产代码修改、自测证据和按授权提交代码，不负责最终验收结论。 | codex:01a0d9e7-fdda-7d33-b4a1-0218d1dcbccd | src/main/**,pom.xml,docs/design/migrations/**,../th4-decoboon-v2-quote/src/main/**,../th4-decoboon-v2-quote/src/test/**,../th4-decoboon-v2-quote/docs/design/migrations/**,../../前端代码/th4-decoboon-app/src/**,../../前端代码/th4-decoboon-app/scripts/**,../../前端代码/th4-decoboon-app/package.json,../../前端代码/th4-decoboon-web/src/**,../../前端代码/th4-decoboon-web/scripts/**,../../前端代码/th4-decoboon-web/package.json,../../前端代码/th4-decoboon-web/AGENTS.md,../../前端代码/th4-decoboon-web/docs/development/**,../../前端代码/th4-decoboon-web-v2/src/**,../../前端代码/th4-decoboon-web-v2/scripts/**,../../前端代码/th4-decoboon-web-v2/package.json,../../前端代码/th4-decoboon-web-v2/AGENTS.md,../../前端代码/th4-decoboon-web-v2/docs/development/**,_系统/协作/lanes/development/** | lanes/development/worklog.md | lanes/development/workspace |
| acceptance | 负责测试用例设计、独立验证、缺陷结论和验收报告，覆盖后端、App 与 Web，不代替开发修改生产代码。 | codex:01a0a51f-d47a-71d1-817e-dc719276654c | src/test/**,docs/acceptance/**,../../前端代码/th4-decoboon-web/docs/acceptance/**,../../前端代码/th4-decoboon-web-v2/docs/acceptance/**,_系统/协作/lanes/acceptance/** | lanes/acceptance/worklog.md | lanes/acceptance/workspace |
| material | 负责收集、整理和登记产品推广素材与业务知识，覆盖业务设计、业务场景、文本和图片资料，重点沉淀短视频及图片素材。 | unbound | docs/marketing/material/**,_系统/协作/lanes/material/** | lanes/material/worklog.md | lanes/material/workspace |
| topic-selection | 负责登记选题、维护选题状态，结合市场行业方向、推广素材和数据复盘结论分析选题方向与内容。 | unbound | docs/marketing/topics/**,_系统/协作/lanes/topic-selection/** | lanes/topic-selection/worklog.md | lanes/topic-selection/workspace |
| content-writing | 负责依据已通过的选题生成内容大纲、短视频口播脚本和配套文案。 | unbound | docs/marketing/scripts/**,_系统/协作/lanes/content-writing/** | lanes/content-writing/worklog.md | lanes/content-writing/workspace |
| design | 负责依据已确认的脚本和素材制作封面、插图及视频，不擅自改变选题和脚本核心内容。 | unbound | docs/marketing/design/**,_系统/协作/lanes/design/** | lanes/design/worklog.md | lanes/design/workspace |
| data-review | 负责按周和按月分析平台数据变化及背后因素，输出复盘结论、证据和改进建议。 | unbound | docs/marketing/analytics/**,_系统/协作/lanes/data-review/** | lanes/data-review/worklog.md | lanes/data-review/workspace |

## Web 端规范与门禁（2026-10-01）

Web 仓库 `../../前端代码/th4-decoboon-web` 与 App 同级纳入本工作台约束：

- 产品技能：`.agents/skills/zhuangdeqing-product-web-development`（项目级，已登记 `.starwork/skills.json`）。
- UI/交互技能：`.agents/skills/zhuangdeqing-web-ui-interaction`（项目级，视觉语言、页面构图、组件规范、渲染批判循环与视觉门禁）。
- 沉浸审计技能：全局 `zhuangdeqing-web-immersion-audit`（浏览器真实页面证据链，与 `zhuangdeqing-miniapp-immersion-audit` 同级）。
- 工程规范：`../../前端代码/th4-decoboon-web/AGENTS.md`、`docs/development/WEB-FRONTEND-STANDARD.md`。
- 设计系统：`docs/product/WEB-DESIGN-SYSTEM.md`。
- 页面门禁：`docs/product/WEB-PAGE-DELIVERY-GATE.md`（G1 开工门禁 / G2 交付门禁）。
- 页面门禁：`docs/product/WEB-PAGE-DELIVERY-GATE.md`（G1 开工 / G1.5 设计冻结 / G2 交付 / G3 视觉门禁）。
- UI 规范：`docs/product/WEB-UI-INTERACTION-SPEC.md`、`docs/product/WEB-COMPONENT-SPEC.md`、`docs/product/WEB-VISUAL-DEBT-PLAN.md`。
- 自动守卫：`npm run verify:ui`（UI Craft Guard，棘轮策略，基线 `scripts/ui-craft-baseline.json`）。
- 验收口径：`docs/acceptance/WEB-沉浸式业务闭环审计协议.md`、`docs/acceptance/WEB-RENDER-ACCEPTANCE.md`。

硬性要求：Web 页面交付必须附真实浏览器渲染证据（1440×900 / 1280×800 / 1024×768）；静态守卫（`verify:*`）、类型检查与构建成功不构成渲染证据；未通过 G1/G1.5 不得编码，未通过 G2/G3 不得表述为"页面已完成"；视觉债务只减不增。
