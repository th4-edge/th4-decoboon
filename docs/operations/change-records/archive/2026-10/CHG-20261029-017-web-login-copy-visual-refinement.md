# CHG-20261029-017 · Web 登录页文案与视觉优化

> 后续反馈修正：本记录中的装饰户型图、品牌/密码自述及平台化概念已由 `CHG-20261029-018` 移除；当前实现以 CHG-018 为准。

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | `CHG-20261029-017` |
| 类型 | 文案 / 视觉 |
| 风险级别 | L0 |
| 状态 | `DEV_VERIFIED_PENDING_USER` |
| 负责人 | codex |
| 日期 | 2026-10-29 |
| 关联需求/事故/业务裁决 | `PI-20261029-009`、`USER-DEC-20261029-001` |

## 2. 目标与范围

- 目标：优化 Web 登录页文案与整体视觉，使页面更像可信、克制、快速进入的企业工作台入口。
- 范围内：`th4-decoboon-web/src/views/auth/LoginView.vue` 的模板、文案、样式与响应式；品牌主题契约脚本的登录页断言。
- 非范围：登录接口、密码校验、Provider、权限、last-login 字段、企业资料、favicon 切换和正式发布。
- 受影响端：Web。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：否。
- 业务事实来源与裁决编号：沿用 `PI-20261029-009` 与 `USER-DEC-20261029-001`，本变更不新增业务事实。
- 尚存 GAP 或假设置：无。首次平台态允许展示装得清品牌；企业品牌态不显示装得清文案。
- 页面意图：登录页；目标为已入驻企业员工；唯一主行动为“登录工作台”。
- 设计事实来源：`zhuangdeqing-ui-craft`、`references/zcool-research.md`、`references/zcool-product-ux-research.md`、`references/visual-system.md`。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | 首次使用员工 | 无 last-login 缓存 | 官方装得清 Logo、默认品牌色、明确平台标题 | `login-platform-1440.png` |
| 2 | 已登录过一次的员工 | last-login 有企业品牌 | 企业 Logo/首字、名称、品牌色、企业工作台标题；正文不含装得清 | `login-enterprise-1440.png` |
| 3 | 桌面员工 | 1440/1280/1024 | 主表单完整、无横向溢出、家装项目主线可见 | 三档截图与 DOM |
| 4 | 窄屏员工 | 390×844 | 单列无横向溢出，登录卡片进入首屏范围 | `login-enterprise-390.png` |
| 5 | 密码设置理解 | 任意品牌态 | 明确提示在小程序「我的 → 账号密码」设置/修改密码 | DOM/页面 |
| 6 | 错误密码 | 已预填手机号 | 停留登录页并显示“手机号或密码错误” | `login-enterprise-error-1280.png` |
| 7 | 有效密码 | 测试企业账号 | 进入 `/workbench`，标题与正文使用企业品牌 | `workbench-login-success-1280.png` |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon-web | `dev` | 未提交 | `src/views/auth/LoginView.vue`、`scripts/audit-web-brand-theme.mjs` |
| th4-decoboon | `dev` | 未提交 | `docs/operations/change-records/CHG-20261029-017-web-login-copy-visual-refinement.md`、`docs/acceptance/runs/20261029-web-login-visual-refinement/**` |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| 类型检查 | `npm run type-check` | PASS | 命令输出 |
| 品牌主题契约 | `npm run check:brand-theme` | PASS | 命令输出 |
| 客户端文案门禁 | `npm run check:client-copy` | PASS | 命令输出 |
| 生产构建 | `npm run build` | PASS | Vite build 输出 |
| 1440 平台态 | 真实浏览器 | PASS | `screenshots/login-platform-1440.png` |
| 1440 企业态 | 真实浏览器 | PASS | `screenshots/login-enterprise-1440.png` |
| 1280/1024 响应式 | 真实浏览器 DOM | PASS，无横向溢出 | `screenshots/login-enterprise-1280.png`、`login-enterprise-1024.png` |
| 390 响应式 | 真实浏览器 DOM | PASS，`scrollWidth=390` | `screenshots/login-enterprise-390.png` |
| 错误密码 | 真实浏览器 | PASS，显示“手机号或密码错误” | `screenshots/login-enterprise-error-1280.png` |
| 有效密码 | 真实浏览器 | PASS，落点 `/workbench`，正文无装得清 | `screenshots/workbench-login-success-1280.png` |
| 视觉预检 | `qwen2.5vl:7b` | PASS（预检，不代替人工验收） | 三份 `vision-review-*.md` |

- 未执行项及原因：未进行生产发布和独立人工验收；按 L0 交用户验收。
- 已知失败及是否与本次相关：无页面运行错误；浏览器控制台冷启动存在既有 Vue Router `/login` warning，与本次样式和文案变更无关。

## 7. 发布

| 字段 | 内容 |
| --- | --- |
| Release ID | 不适用，未发布 |
| 制品路径/SHA-256 | 不适用 |
| 生产 release/版本 | 不适用 |
| 上一版本/回滚点 | Web `dev` 中本变更前提交 |
| 健康检查 | 本地 Web `/login` 200、真实浏览器登录 PASS |
| 发布后观察 | 待用户验收后再安排 |

- 数据库迁移：无
- 配置/密钥变化：无

## 8. 回滚

- 回滚条件：登录页品牌态回退、官方 Logo/企业品牌色失效、窄屏横向溢出、登录流程异常。
- 回滚命令/步骤：回退 `LoginView.vue` 与对应品牌主题断言，保留 CHG-016 的认证与缓存逻辑。
- 回滚验证：首次态官方 Logo、企业态品牌恢复、有效密码登录、错误密码提示。
- 实际是否回滚：否

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集；使用 `zhuangdeqing-ui-craft` 规范与本地 `qwen2.5vl:7b` 视觉预检。
- 人工耗时：未采集
- 一次通过：否
- 返工原因：第一轮深色品牌面积过大、信息重复；第二轮收回局部裸色值并压缩 390px 信息密度。
- 最终结论：待用户验收

## 10. 复盘与沉淀

- 是否出现重复问题：否
- 是否需要新增门禁：否；复用现有品牌主题和客户端文案门禁。
- 候选 `EXP/ANT/PATH`：ANT：用整屏深色渐变和多个卖点把登录页做成营销海报；PATH：登录页保持浅中性画布、企业身份、项目流程语境和唯一主行动。
- 后续动作与负责人：用户通过本地 `http://localhost:8081/login` 验收；通过后再决定是否发布。
