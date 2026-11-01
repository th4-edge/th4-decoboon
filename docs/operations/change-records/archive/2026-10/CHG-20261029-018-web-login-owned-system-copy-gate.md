# CHG-20261029-018 · Web 登录页自有系统文案与展示门禁

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | `CHG-20261029-018` |
| 类型 | 缺陷 / 文案 / 视觉 / 构建门禁 |
| 风险级别 | L0 |
| 状态 | `DEV_VERIFIED_PENDING_USER` |
| 负责人 | codex |
| 日期 | 2026-10-29 |
| 关联需求/事故/业务裁决 | `PI-20261029-009`、`CHG-20261029-017`、`USER-DEC-20261029-001` |

## 2. 目标与范围

- 目标：移除登录页平台化、AI 自述和设计说明文案；将页面理解为装企自有系统；删除不成熟的家装装饰图片；增加防回归门禁。
- 范围内：`LoginView.vue` 文案与视觉、登录路由标题、客户端文案门禁、品牌主题契约断言、验收证据。
- 非范围：登录接口、密码校验、Provider、权限、last-login 数据结构、favicon 逻辑和正式发布。
- 受影响端：Web。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：否。
- 业务事实来源与裁决编号：沿用 `PI-20261029-009` 与 `USER-DEC-20261029-001`。
- 尚存 GAP 或假设置：无。
- 用户明确口径：登录页按装企自有系统表达，不暴露平台/管理概念；设计元素无法达到质量要求时直接移除。
- 页面唯一主行动：登录。

## 4. 验收条件

| 序号 | 场景 | 预期结果 | 证据 |
| --- | --- | --- | --- |
| 1 | 静态文案 | 不出现“企业品牌标识与主题色来自企业资料设置。” | 门禁 |
| 2 | 静态文案 | 不出现“本机只记住企业品牌和手机号，不会保存密码。” | 门禁 |
| 3 | 静态文案 | 不出现企业、装企、平台、产品、工作台、SaaS 等概念 | 门禁 + DOM |
| 4 | 左侧设计 | 不出现户型图、家具图、尺寸线或 `<svg>` 装饰图片 | 门禁 + DOM |
| 5 | 门禁负向验证 | 人为包含被禁文案或装饰 SVG 时能被识别 | `--self-test` |
| 6 | 1440/1280/1024 响应式 | 无横向溢出，表单完整 | 三档截图与 DOM |
| 7 | 390 响应式 | 无横向溢出，登录卡片完整落在 844px 首屏 | `login-company-390.png` |
| 8 | 错误密码 | 停留 `/login` 并显示“手机号或密码错误” | `login-error-1280.png` |
| 9 | 正确密码 | 落到 `/workbench`，企业态正文无装得清 | `workbench-success-1280.png` |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon-web | `dev` | 未提交 | `src/views/auth/LoginView.vue`、`src/core/router/index.ts`、`src/roles/routes.ts`、`scripts/check-client-copy.mjs`、`scripts/audit-web-brand-theme.mjs` |
| th4-decoboon | `dev` | 未提交 | `docs/operations/change-records/CHG-20261029-018-web-login-owned-system-copy-gate.md`、`docs/acceptance/runs/20261029-web-login-owned-system/**` |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| 门禁负向自测 | `node scripts/check-client-copy.mjs --self-test` | PASS | 命令输出 |
| 客户端文案门禁 | `npm run check:client-copy` | PASS | 命令输出 |
| 品牌主题契约 | `npm run check:brand-theme` | PASS | 命令输出 |
| 类型检查 | `npm run type-check` | PASS | 命令输出 |
| 生产构建 | `npm run build` | PASS | Vite build 输出 |
| 1440/1280/1024/390 | 真实浏览器 | PASS，无横向溢出 | `docs/acceptance/runs/20261029-web-login-owned-system/screenshots/` |
| 错误/正确密码 | 真实浏览器 | PASS | 错误态与工作台截图 |
| 视觉预检 | `qwen2.5vl:7b` | PASS（预检） | `vision-review-company-1440.md` |

- 未执行项及原因：未发布生产、未做独立人工验收。
- 已知失败及是否与本次相关：无页面运行错误；冷启动仍有一条既有 Vue Router `/login` warning。

## 7. 发布

| 字段 | 内容 |
| --- | --- |
| Release ID | 不适用，未发布 |
| 制品路径/SHA-256 | 不适用 |
| 生产 release/版本 | 不适用 |
| 上一版本/回滚点 | Web `dev` 中本变更前提交 |
| 健康检查 | 本地 `/login` 200、错误/正确密码链路 PASS |
| 发布后观察 | 待用户验收后决定 |

- 数据库迁移：无
- 配置/密钥变化：无

## 8. 回滚

- 回滚条件：品牌态异常、登录流程异常、窄屏溢出或门禁误伤合法页面。
- 回滚命令/步骤：回退本轮 `LoginView.vue`、路由标题和两个校验脚本；保留 CHG-016/017 的品牌记忆基础。
- 回滚验证：首次 Logo、昭青装饰品牌态、错误密码、正确登录。
- 实际是否回滚：否

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集；使用 `zhuangdeqing-ui-craft` 与本地 `qwen2.5vl:7b` 预检。
- 人工耗时：未采集
- 一次通过：否
- 返工原因：用户明确否定平台自述、企业概念和左侧家装图片，追加门禁并移除图片。
- 最终结论：待用户验收

## 10. 复盘与沉淀

- 是否出现重复问题：是，平台自述与装饰图在同一登录页连续出现。
- 是否需要新增门禁：是；已加入 `check:client-copy`，并提供 `--self-test` 负向验证。
- 候选 `ANT`：登录页用平台解释、安全自述和装饰性户型图填充版面。
- 候选 `PATH`：自有系统入口只保留品牌、业务范围、项目主线和登录动作；质量不足的视觉元素直接删除。
- 后续动作与负责人：用户本地验收 `http://localhost:8081/login`。
