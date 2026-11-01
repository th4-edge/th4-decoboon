# 登录后交互系统、账号菜单与消息通知升级 · 开发验证证据

> 状态：`DEV_VERIFIED_PENDING_USER`  
> 风险级别：L1  
> 变更记录：`CHG-20261029-021-web-authenticated-interaction-system.md`

## 1. 目标

- 登录后页面切换、侧栏导航、账号菜单和消息通知使用同一套克制交互语言。
- 右上角账号菜单从裸文字升级为带身份摘要、图标和未读提示的操作菜单。
- 消息通知从“筛选卡 + 列表卡”改成消息处置队列。
- 统一内容页头部组件，并把英文眉题、营销空话和装饰性英文短语做成门禁。

## 2. 页面结构

- 壳层：侧栏 → 顶栏 → `role-frame__page-stage` → 内容页 → 页脚。
- 页面进入：`page-shift` 一次性淡入/上移，减少动态效果下关闭。
- 账号菜单：身份摘要 + 个人信息 / 企业信息 / 我的工资条 / 消息通知 / 退出登录。
- 消息通知：页面头部 → 状态与类型工具栏 → 单列表消息队列。
- 通知行：类型图标、业务类型、标题、正文两行截断、时间、未读文字与左侧状态线、进入箭头。
- 页面七态：正常列表、加载骨架、真实空态、筛选空态、错误态、长内容截断、响应式极端宽度。

![消息通知 1440](screenshots/notification-1440.png)

![账号菜单 1440](screenshots/account-menu-1440.png)

![消息筛选空态](screenshots/notification-empty-filter-1440.png)

## 3. 交互与动效

- 页面切换：`320ms` 淡入/上移；使用统一 `$motion-enter-fast` 与 `$motion-ease-out`。
- 侧栏：hover 显示背景反馈，选中项有品牌竖线和淡品牌底；分组展开有短促一次性展开。
- 账号菜单：Element Plus 负责菜单键盘语义，页面补充身份摘要、图标和未读数量。
- 通知列表：刷新时保留当前列表并降低透明度，不把已有数据替换成空白。
- 通知行：hover 显示背景与箭头位移；未读同时有文字、左侧线和整行浅品牌底，不只依赖颜色。
- 全部动效尊重 `prefers-reduced-motion`。

## 4. 真实浏览器证据

| 场景 | 文件 | 结果 |
| --- | --- | --- |
| 通知 1440×900 | `screenshots/notification-1440.png` | 6 行消息，3 条未读；单列表；工具栏与主行动清楚 |
| 通知 1280×800 | `screenshots/notification-1280.png` | 无横向溢出，行宽 978px |
| 通知 1024×768 | `screenshots/notification-1024.png` | 无横向溢出，侧栏切换后内容可用 |
| 通知 390×844 | `screenshots/notification-390.png` | 工具栏纵排，行宽 356px，无横向溢出 |
| 账号菜单 | `screenshots/account-menu-1440.png` | 5 个菜单项均有图标，消息通知显示 3 条未读 |
| 筛选空态 | `screenshots/notification-empty-filter-1440.png` | 显示“当前筛选下没有消息”和“清除筛选” |
| 路由切换 | `page-transition-evidence.json` | 从 `/messages` 切到 `/workbench`，页面 stage 存在，overflow 0 |

DOM 证据在 `notification-evidence.json`：1440 下工作区 `1140×651`、首行 `1138×98`、6 个消息图标、3 条未读；1280/1024/390 均无横向溢出。

## 5. AI 味门禁

新增命令：

```text
npm run check:ai-flavor:self-test
npm run check:ai-flavor
```

门禁范围：

- `src/views/**`
- `src/layouts/**`
- `src/components/**`

拦截：

- 英文眉题或英文装饰性短语，例如 `PROJECT INSIGHT`、`STAGE ACCEPTANCE`；
- 空泛营销词，例如 `一站式`、`全链路`、`赋能`、`数智化`、`智慧经营`；
- 纯英文装饰性对象标签，例如 `CHANGE DOCUMENT`。

负向自测样例故意包含上述三类问题，正向门禁要求生产 UI 零命中。

## 6. 验证命令

```text
npm run check:ai-flavor:self-test  PASS
npm run check:ai-flavor            PASS
npm run check:client-copy          PASS
npm run check:brand-theme          PASS
npm run type-check                 PASS
npm run build                      PASS
```

视觉模型 `qwen2.5vl:7b` 完成通知页 1440/390 与账号菜单预检；其提出的增加卡片背景、更多图标/状态建议未采纳，因为会重新制造模板卡片和装饰噪音。

## 7. 未闭环

- 账号菜单和通知页已完成 Web 真实渲染；尚未发布生产。
- 消息通知的权限拒绝态取决于 Provider 实际错误码，当前以“消息暂不可确认 + 重新加载”作为恢复路径。
- 侧栏分组内容、各角色页面存量视觉语言仍会继续分批清理；本轮先把公共壳层、页面头部、账号菜单、通知页和门禁做实。
