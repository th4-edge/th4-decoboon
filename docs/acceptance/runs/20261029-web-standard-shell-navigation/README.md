# Web 标准壳层与角色分组导航升级 · 开发验证证据

> 状态：`DEV_VERIFIED_PENDING_USER`  
> 风险级别：L1（跨角色公共壳层升级，无业务逻辑变化）  
> 变更记录：`CHG-20261029-019-web-standard-shell-navigation.md`

## 1. 设计理念

本轮不是重做页面，而是升级所有角色共享的 Web 壳层。

### 1.1 先稳定骨架，再承载角色差异

统一结构：

```text
Layout
├─ Sidebar：品牌 + 当前角色菜单
└─ Body
   ├─ Header：页面上下文 + 门店维度 + 通知 + 账号
   ├─ Main Container：业务页面内容
   └─ Footer：品牌归属 + 系统版本
```

所有角色共用同一套壳层，菜单仍由 `workbenchConfigOf(session.role)` 从角色静态配置读取。没有新增角色判断、没有改路由权限，也没有把页面业务规则搬进壳层。

### 1.2 菜单分组，而不是一次平铺

参考站酷 B 端作品中“核心功能前置、导航收敛、上下文连续”的做法：

- 业务工作台固定为首页入口；
- 其余能力按业务组收纳；
- 同一时间只展开一个业务组；
- 回到某个业务页面时，其所属分组自动展开；
- 只有单项的分组直接显示入口，不再额外套一层标题；
- 菜单数量与权限不减少，减少的是一次铺开造成的认知负担。

### 1.3 浅色、低噪音、单一品牌强调

- 侧栏由深色长列表改为浅色分组导航；
- 品牌色只用于当前分组、当前菜单和聚焦态；
- Header、Container、Footer 共用一致的最大宽度与水平留白；
- 不增加装饰插画、彩色分组背景或营销文案；
- Footer 保持轻量，只承担品牌归属与版本信息。

### 1.4 页面切换保持 SPA

菜单使用 Vue Router `router-link`：

```text
点击菜单
→ 更新 URL / 当前菜单 / 页面标题
→ 只替换 Main Container 内的 router-view
→ Header、Sidebar、Footer 不重新加载
```

## 2. 角色菜单覆盖

| 角色 | 菜单项 | 业务组 | 可展开组 | 说明 |
| --- | ---: | ---: | ---: | --- |
| Boss | 23 | 6 | 4 | 经营、客户、商务财务、项目、分析、帮助 |
| StoreManager | 22 | 5 | 3 | 本店客户、履约、经营与工具 |
| Marketer | 7 | 4 | 3 | 我的客户、量房、项目流转、工具 |
| Designer | 17 | 6 | 4 | 客户设计、报价合同、项目、经营工具 |
| Pm | 7 | 4 | 2 | 项目履约、材料变更、协同 |
| Finance | 24 | 9 | 6 | 收付、往来、凭证、薪酬、项目核算 |
| TeamLeader | 0 | 0 | 0 | 冻结角色，不恢复业务菜单 |

`npm run check:workbenches` 与 `npm run check:workspaces` 均通过，说明角色菜单、角色路由和落地页可达性没有被本次视觉升级破坏。

## 3. 菜单滚动验证

| 场景 | 升级前 | 升级后 |
| --- | --- | --- |
| 1440×900，Boss 侧栏 | `scrollHeight=1066`、`clientHeight=717`，内部溢出约 349px | 折叠态 `827/827`；展开“商务财务”仍为 `827/827`，无内部滚动 |
| 1280×720，展开“商务财务” | 未优化时 `664/647`，仍有 17px 溢出 | `647/647`，无内部滚动 |
| 390×844，移动抽屉 | 长菜单直接铺开 | 分组折叠，抽屉导航 `771/771`，无横向溢出 |
| 1024×768 | 侧栏直接隐藏，菜单入口不够明确 | 保留 Header 菜单按钮，打开统一抽屉导航 |

## 4. 标准壳层证据

浏览器 DOM：

```text
Header：true
Main：true
Footer：true
Content Container：true
Sidebar：桌面可见 / 1024 以下收起为抽屉
```

截图：

- `screenshots/shell-workbench-1440.png`
- `screenshots/shell-business-group-open-1440.png`
- `screenshots/shell-business-group-open-1280x720.png`
- `screenshots/shell-workbench-1024.png`
- `screenshots/shell-workbench-390.png`
- `screenshots/shell-nav-drawer-390.png`

升级前基线：

- `screenshots/before-workbench-1440.png`

## 5. SPA 无刷新导航证据

真实浏览器操作：

```text
清空 shell marker
→ 在“商务财务”组点击“待我付款”
→ URL：/finance/payables
→ 页面标题：应付账款 | 昭青装饰
→ window.__shellSpaMarker：kept
→ performance navigation entries：1
→ Header / Main / Footer 仍存在
→ 当前菜单：待我付款
→ 展开分组：商务财务
```

截图：`screenshots/shell-spa-navigation-payables-1440.png`

## 6. 结构门禁

新增：

```text
scripts/audit-web-shell-layout.mjs
npm run check:shell-layout
```

门禁检查：

- 壳层必须存在 `aside/header/main/footer`；
- 必须有统一 `container`；
- 页面必须通过 `router-view` 加载；
- 菜单必须使用 `router-link`；
- 禁止在壳层使用 `window.location` 或 `location.href` 做整页跳转；
- 菜单必须使用角色配置和单项展开逻辑。

门禁已加入 `npm run build`。

## 7. 验证命令

```text
npm run check:shell-layout   PASS
npm run check:workbenches    PASS
npm run check:workspaces     PASS
npm run check:client-copy    PASS
npm run check:brand-theme    PASS
npm run type-check           PASS
npm run build                PASS
```

## 8. 视觉预检

- `vision-review-desktop-1440.md`
- `vision-review-mobile-drawer-390.md`

预检确认：

- Header、Container、Footer 对齐；
- 移动抽屉宽度可读；
- Header 与内容无遮挡；
- 无横向溢出；
- 分组菜单展开关系可识别。

视觉模型仍建议继续合并或隐藏菜单项、增加图标或背景装饰。本轮未采纳：合并会改变现有角色信息架构，装饰性背景和额外图标也不是本次升级目标。

## 9. 范围与结论

- 未修改业务 Provider、权限、金额、成本、路由集合和业务页面。
- 未减少菜单能力，只调整分组展示和壳层布局。
- 当前状态：`DEV_VERIFIED_PENDING_USER`。
- 待用户验收后再决定是否进入发布流程。
