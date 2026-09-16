# Agent Lanes

## Lanes

| lane | purpose | current_session | write_scope | worklog | workspace |
|---|---|---|---|---|---|
| product-planning | 负责需求拆解、PRD/规格、范围优先级和验收标准定义，不负责生产代码实现。 | codex:01a0a51f-cab9-7c02-8709-7cf6c9d28648 | docs/product/**,docs/plan/**,_系统/协作/lanes/product-planning/** | lanes/product-planning/worklog.md | lanes/product-planning/workspace |
| development | 负责功能实现、生产代码修改、自测证据和按授权提交代码，不负责最终验收结论。 | codex:01a0a51f-cf38-7e23-bc39-54516fab56ec | src/main/**,pom.xml,docs/design/migrations/**,../../前端代码/th4-decoboon-app/src/**,../../前端代码/th4-decoboon-app/scripts/**,../../前端代码/th4-decoboon-app/package.json,../../前端代码/th4-decoboon-web/**,_系统/协作/lanes/development/** | lanes/development/worklog.md | lanes/development/workspace |
| acceptance | 负责测试用例设计、独立验证、缺陷结论和验收报告，不代替开发修改生产代码。 | codex:01a0a51f-d47a-71d1-817e-dc719276654c | src/test/**,docs/acceptance/**,../../前端代码/th4-decoboon-web/**,_系统/协作/lanes/acceptance/** | lanes/acceptance/worklog.md | lanes/acceptance/workspace |
| material | 负责收集、整理和登记产品推广素材与业务知识，覆盖业务设计、业务场景、文本和图片资料，重点沉淀短视频及图片素材。 | unbound | docs/marketing/material/**,_系统/协作/lanes/material/** | lanes/material/worklog.md | lanes/material/workspace |
| topic-selection | 负责登记选题、维护选题状态，结合市场行业方向、推广素材和数据复盘结论分析选题方向与内容。 | unbound | docs/marketing/topics/**,_系统/协作/lanes/topic-selection/** | lanes/topic-selection/worklog.md | lanes/topic-selection/workspace |
| content-writing | 负责依据已通过的选题生成内容大纲、短视频口播脚本和配套文案。 | unbound | docs/marketing/scripts/**,_系统/协作/lanes/content-writing/** | lanes/content-writing/worklog.md | lanes/content-writing/workspace |
| design | 负责依据已确认的脚本和素材制作封面、插图及视频，不擅自改变选题和脚本核心内容。 | unbound | docs/marketing/design/**,_系统/协作/lanes/design/** | lanes/design/worklog.md | lanes/design/workspace |
| data-review | 负责按周和按月分析平台数据变化及背后因素，输出复盘结论、证据和改进建议。 | unbound | docs/marketing/analytics/**,_系统/协作/lanes/data-review/** | lanes/data-review/worklog.md | lanes/data-review/workspace |
