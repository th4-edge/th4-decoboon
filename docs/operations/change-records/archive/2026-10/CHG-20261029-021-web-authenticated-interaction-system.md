# CHG-20261029-021 · 登录后交互系统、账号菜单与消息通知升级

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | `CHG-20261029-021` |
| 类型 | UI / 交互系统 / 文案门禁 |
| 风险级别 | L1 |
| 状态 | `DEV_VERIFIED_PENDING_USER` |
| 负责人 | codex |
| 日期 | 2026-10-29 |
| 关联需求/事故/业务裁决 | `CHG-20261029-020`、`USER-DEC-20261029-006`、`PI-20261029-009` |

## 2. 目标与范围

- 目标：让登录后的页面进入、侧栏导航、账号菜单和消息通知形成同一套克制交互语言；统一内容页头部；删除生产界面中的英文眉题和空泛营销话术，并建立防回归门禁。
- 范围内：Web 主壳层、路由页面过渡、侧栏导航反馈、右上角账号菜单、消息通知页、账户中心/客户页/业务工作台页面头部、全局动效 Token、AI 味门禁。
- 非范围：通知 Provider 字段、通知业务规则、权限、登录认证、金额/成本/状态语义和正式发布。
- 受影响端：Web。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：否。
- 业务事实来源与裁决编号：不适用；通知分类、未读状态和路由跳转沿用现有 Provider 事实。
- 尚存 GAP 或假设置：无。
- 页面意图：消息通知是行动队列与业务入口，不是 KPI 卡片墙。
- 唯一主行动：优先处理未读消息；刷新、全部已读和筛选均为辅助动作。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | 登录后页面切换 | 已登录、从通知页进入其他业务页 | 页面以 320ms 一次性淡入/上移切换；减少动态效果时关闭 | `page-transition-evidence.json`、DOM |
| 2 | 侧栏导航 | 用户 hover/选中/展开分组 | hover、active、group-open 使用同一 Token、焦点可见、选中态有稳定指示 | 真实页面/DOM |
| 3 | 账号菜单 | 点击右上角用户区 | 菜单显示员工、企业、角色/数据范围；5 个菜单项均有图标；消息通知显示未读数 | `screenshots/account-menu-1440.png` |
| 4 | 账号菜单键盘 | 点击/键盘打开 | 保留 Element Plus 菜单焦点与 Escape 关闭；按钮声明 `aria-haspopup` | DOM |
| 5 | 消息通知正常态 | 有真实消息 | 单列表消息队列，未读有文字、左侧标记、类型图标与时间；工具栏可筛选、刷新、全部已读 | `screenshots/notification-1440.png` |
| 6 | 消息筛选空态 | 未读 + 无对应类型 | 显示“当前筛选下没有消息”，给出“清除筛选”恢复路径 | `screenshots/notification-empty-filter-1440.png` |
| 7 | 消息加载态 | 首次加载 | 保持列表结构骨架，不显示空态或 0 事实 | DOM |
| 8 | 消息失败态 | Provider 失败 | 显示“消息暂不可确认”、错误原因和重新加载入口 | DOM/源码 |
| 9 | 消息行内容 | 长标题/长正文 | 标题截断、正文两行截断，时间等宽数字，不横向溢出 | `notification-evidence.json` |
| 10 | 响应式 | 1440/1280/1024/390 | 无横向溢出；390 工具栏纵排、消息行保持可读 | 四档截图 |
| 11 | AI 味门禁 | `npm run check:ai-flavor` | 禁止英文眉题、营销空话、装饰性英文短语；负向自测可命中 | 命令输出 |
| 12 | 页面头部统一 | 账户中心/客户/业务工作台/通知 | 使用 `PageHeader` 统一标题、说明、操作区语义和间距 | 源码/DOM |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon-web | `dev` | 未提交 | `src/layouts/RoleAreaShell.vue`、`src/components/shell/PageHeader.vue`、`src/components/shell/NavIcon.vue`、`src/components/shell/RoleSideNav.vue`、`src/views/notice/NotificationCenterView.vue`、`src/styles/tokens.scss`、`src/styles/index.scss`、`scripts/audit-web-ai-flavor.mjs`、`package.json`、`AGENTS.md` |
| th4-decoboon | `dev` | 未提交 | 本变更记录、验收证据、AI 味门禁说明 |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| AI 味负向自测 | `npm run check:ai-flavor:self-test` | PASS | 命令输出 |
| AI 味正向门禁 | `npm run check:ai-flavor` | PASS | 命令输出 |
| 客户端文案门禁 | `npm run check:client-copy` | PASS | 命令输出 |
| 品牌主题契约 | `npm run check:brand-theme` | PASS | 命令输出 |
| 类型检查 | `npm run type-check` | PASS | 命令输出 |
| 生产构建 | `npm run build` | PASS | 命令输出 |
| 通知页真实渲染 | 1440/1280/1024/390 | PASS，无横向溢出 | `notification-evidence.json` |
| 账号菜单真实渲染 | 1440 | PASS，5 个菜单项均含图标；消息通知显示 3 条未读 | `screenshots/account-menu-1440.png` |
| 筛选空态 | 1440 | PASS，提供清除筛选动作 | `screenshots/notification-empty-filter-1440.png` |
| 路由切换 | 通知页 → 工作台 | PASS，落到 `/workbench`，`.role-frame__page-stage` 存在，overflow 0 | `page-transition-evidence.json` |
| 视觉预检 | `qwen2.5vl:7b` | PASS（预检，不代替人工） | `vision-*.md` |

- 未执行项及原因：未发布；没有新增独立真机/生产验收。
- 已知失败及是否与本次相关：Web 构建存在既有 Sass `@import` / legacy API 弃用 warning，不影响通过。

## 7. 发布

| 字段 | 内容 |
| --- | --- |
| Release ID | 不适用，未发布 |
| 制品路径/SHA-256 | 不适用 |
| 生产 release/版本 | 不适用 |
| 上一版本/回滚点 | Web `dev` 中本变更前提交 |
| 健康检查 | 本地 `/messages`、`/workbench` 真实浏览器 |
| 发布后观察 | 待用户视觉验收后决定 |

- 数据库迁移：无
- 配置/密钥变化：无

## 8. 回滚

- 回滚条件：账号菜单不可打开/不可键盘操作、通知事实错误、消息无法打开、页面切换导致内容丢失、AI 味门禁误伤合法业务文案。
- 回滚命令/步骤：回退主壳层、PageHeader、通知页和门禁脚本；保留登录页动效与品牌主题改动。
- 回滚验证：主壳层路由、账号菜单、通知列表/空态/错误态、构建与门禁负向自测。
- 实际是否回滚：否

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集；使用 `zhuangdeqing-ui-craft`、本地视觉预检和真实浏览器。
- 人工耗时：未采集
- 一次通过：否
- 返工原因：首版 AI 味门禁把类名和日期格式误判为英文眉题，已改为提取可见文本并排除技术日期格式；同时将历史英文眉题统一中文化。
- 最终结论：待用户验收

## 10. 复盘与沉淀

- 是否出现重复问题：是，英文眉题和模板式页面头部散落在多个角色页。
- 是否需要新增门禁：是；新增 `check:ai-flavor`，带 `--self-test` 负向验证，并纳入 Web build 与 AGENTS 必跑清单。
- 候选 `EXP/ANT/PATH`：ANT：用英文眉题、泛化副标题和卡片墙制造“专业感”；PATH：统一页面头部、对象队列、单一主行动、Token 动效和真实恢复路径。
- 后续动作与负责人：用户确认账号菜单、通知页与页面切换观感后，再安排剩余页面的存量 AI 味清理。
