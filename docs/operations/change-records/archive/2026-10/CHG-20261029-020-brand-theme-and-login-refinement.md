# CHG-20261029-020 · 装企主题色板呈现与 Web 登录页视觉收敛

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | `CHG-20261029-020` |
| 类型 | UI / 品牌主题 / 登录页视觉 |
| 风险级别 | L1 |
| 状态 | `DEV_VERIFIED_PENDING_USER` |
| 负责人 | codex |
| 日期 | 2026-10-29 |
| 关联需求/事故/业务裁决 | `USER-DEC-20261029-006`、`CHG-20261029-013`、`CHG-20261029-016`、`CHG-20261029-018`、`PI-20261029-009` |

## 2. 目标与范围

- 目标：在既有 `Company.color → 派生色阶 → App/Web 主题` 基础上，把装企可选主题从单色点升级为可理解、可判断的完整主题选项；继续优化 Web 登录页的家装项目语境和大字号构图，并加入克制、一次性的业务入场动效。
- 范围内：App/Web 推荐主题文案、完整色阶展示、Web 企业信息主题选择器、Web 登录页结构/响应式/入场动效、站酷登录页与参考动效专项学习和证据。
- 非范围：登录 Provider、密码校验、权限、`Company.color` 数据结构、金额/成本/状态语义、历史文件品牌快照和正式发布。
- 受影响端：Web、微信小程序/App H5。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：否。
- 业务事实来源与裁决编号：沿用 `USER-DEC-20261029-006`；企业品牌色仍是唯一主题输入。
- 尚存 GAP 或假设置：无。
- 视觉参考：站酷公开登录页与 ERP 样本；只提炼浅中性画布、单一强调色、大字阶、留白和业务叙事原则，不复制具体页面或素材。
- 登录页唯一主行动：登录。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | 企业负责人选择主题（Web） | 企业信息编辑态 | 8 个主题均显示浅色/主色/深色色阶和业务气质说明，选中态明确 | `screenshots/account-brand-edit-1440.png` |
| 2 | 企业负责人选择主题（小程序） | 企业信息编辑态 | 同一主题集合与说明，两列主题卡片可读，点击即时预览 | App 源码 + build/contract |
| 3 | 企业品牌态登录页 | `last-login` 有企业品牌 | 企业 Logo/首字、名称、品牌色、项目主线和登录表单同屏；不出现平台概念 | `screenshots/round2-login-enterprise-1440.png` |
| 4 | 首次使用登录页 | 无 `last-login` | 官方装得清 Logo 与默认品牌色；保持同一项目主线 | `screenshots/login-platform-1440.png` |
| 5 | 桌面响应式 | 1440/1280/1024 | 无横向溢出，登录卡片、项目主线和主按钮完整可见 | `login-round2-evidence.json` |
| 6 | 窄屏响应式 | 390×844 | 无横向溢出；移动端删除重复说明，登录按钮完整进入首屏 | `screenshots/round2-login-enterprise-390.png` |
| 7 | 多企业主题 | 青瓷/岩蓝/赤陶/胡桃 | root Token、Element Plus、登录按钮、企业标识同步换色 | `login-palette-evidence.json` |
| 8 | 对比度 | 7 个推荐基色 | 基色与白字对比度均 ≥ 4.5:1 | App/Web 品牌契约命令 |
| 9 | 视觉复核 | 1440/390/主题编辑页 | 至少两轮截图和视觉预检；不接受增加装饰图标、辅助色和 KPI 模板的建议 | 三份 `vision-review-*.md` |
| 10 | 登录页入场动效 | 正常动态偏好 | 品牌落位、主标题入场、项目主线绘制、阶段依次出现、表单稳定落位；所有动画在 700ms 内结束 | `motion/motion-evidence.json`、`motion/login-motion-sequence.gif` |
| 11 | 循环与注意力边界 | 正常动态偏好 | 不出现无限巡游光点、鼠标跟随柔光、呼吸或循环装饰 | DOM 动画审计 |
| 12 | 减少动态效果 | `prefers-reduced-motion: reduce` | 登录页入场动画数量为 0，品牌、表单可见，项目主线使用最终静态形态 | `motion/motion-evidence.json`、`motion/motion-reduced-1440.png` |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | `dev` | 未提交 | 本变更记录、站酷研究规则、主题色板文档、验收证据 |
| th4-decoboon-web | `dev` | 未提交 | `src/core/brandPresets.ts`、`src/views/account/AccountCenterView.vue`、`src/views/auth/LoginView.vue`、`src/styles/tokens.scss`、`scripts/audit-web-brand-theme.mjs` |
| th4-decoboon-app | `dev` | 未提交 | `src/domains/identity/brand.ts`、`src/subpackages/company/pages/info.vue`、`scripts/verify-company-brand-contract.mjs` |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| Web 文案门禁 | `npm run check:client-copy` | PASS | 命令输出 |
| Web 品牌主题契约 | `npm run check:brand-theme` | PASS | 命令输出 |
| Web 类型检查与构建 | `npm run type-check && npm run build` | PASS | 命令输出 |
| App 品牌/文案/UI 契约 | `npm run check:company-brand && npm run check:client-copy && npm run check:ui` | PASS | 命令输出 |
| App 类型检查与小程序构建 | `npm run type-check && npm run build:mp-weixin` | PASS | 命令输出；Sass 既有弃用 warning 不影响构建 |
| Web 登录页真实渲染 | 1440/1280/1024/390 | PASS，横向溢出 0；390 登录按钮底部 711px < 844px | `login-round2-evidence.json` |
| Web 企业主题选择器 | 1440/1024/390 | PASS，横向溢出 0；1440 两列、390 单列 | `account-brand-editor-evidence.json`、`account-brand-responsive-evidence.json` |
| 主题运行时 | 默认/青瓷/岩蓝/赤陶/胡桃 | PASS，root/Element Plus/按钮/企业标识同步 | `login-palette-evidence.json` |
| 视觉预检 | `qwen2.5vl:7b` | PASS（预检，不代替人工验收） | `vision-review-*.md` |
| 登录页动效时序 | 真实浏览器 DOM | PASS，13 个一次性动画，最晚 680ms 结束，无限动画 0 | `motion/motion-evidence.json` |
| 减少动态效果 | 浏览器媒体模拟 | PASS，登录页动画 0，内容可见，主线为静态最终形态 | `motion/motion-evidence.json` |
| 动效响应式 | 1440/1280/1024/390 | PASS，横向溢出 0 | `motion/motion-final-*.png` |

- 未执行项及原因：未发布；小程序端未做本轮真机视觉截图，只完成类型检查、契约和微信小程序构建。最终审美与业务适配由用户验收。
- 已知失败及是否与本次相关：无新增失败；Web/App 构建均存在既有 Sass `@import` / legacy API 弃用 warning。

## 7. 发布

| 字段 | 内容 |
| --- | --- |
| Release ID | 不适用，未发布 |
| 制品路径/SHA-256 | 不适用 |
| 生产 release/版本 | 不适用 |
| 上一版本/回滚点 | 各仓库 `dev` 中本变更前提交 |
| 健康检查 | 本地 Web `/login`、`/account/company` 真实渲染 PASS |
| 发布后观察 | 待用户确认色板与登录页后安排 |

- 数据库迁移：无
- 配置/密钥变化：无

## 8. 回滚

- 回滚条件：主题选择器不可读、颜色派生错误、登录页布局溢出、企业品牌记忆失效、登录链路异常，或动效造成内容延迟/减少动态效果失效。
- 回滚命令/步骤：回退 App/Web 本次 UI 与文案改动；保留 `Company.color → 派生色阶` 基础主题机制和 `CHG-20261029-018` 登录展示门禁。
- 回滚验证：默认/推荐主题切换、登录页三档渲染、首次与企业品牌态、App 构建。
- 实际是否回滚：否

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集；使用站酷公开浏览、`zhuangdeqing-ui-craft` 与本地 `qwen2.5vl:7b` 视觉预检。
- 人工耗时：未采集
- 一次通过：否
- 返工原因：首轮 390 同时显示主标题、重复说明和五阶段路径，视觉任务不够集中；第二轮删除移动端重复说明，登录按钮上移 64px。补充动效时删除参考稿的 16 秒无限光点和鼠标跟随柔光，并将全部入场压缩到 700ms 内。
- 最终结论：待用户视觉验收

## 10. 复盘与沉淀

- 是否出现重复问题：否
- 是否需要新增门禁：否；沿用品牌主题和客户端文案门禁，补充推荐主题对比度检查。
- 候选 `EXP/ANT/PATH`：ANT：用色点代替完整主题预览、用背景图证明行业、用无限循环光点制造生命感；PATH：展示色阶和业务气质，用一次性项目主线绘制承担登录页业务语境。
- 后续动作与负责人：用户确认色板取舍、登录页视觉方向与小程序真机观感后再决定发布。
