# CHG-20261029-019 · Web 标准壳层与角色分组导航升级

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | `CHG-20261029-019` |
| 类型 | 需求 / 公共壳层 / 导航体验 |
| 风险级别 | L1 |
| 状态 | `DEV_VERIFIED_PENDING_USER` |
| 负责人 | codex |
| 日期 | 2026-10-29 |
| 关联需求/事故/业务裁决 | `USER-DEC-20261029-001`、`CHG-20261029-018` |

## 2. 目标与范围

- 目标：在现有基础上升级 Web 公共壳层，形成标准 `Layout + Header + Footer + Container`，按角色展示分组菜单，减少侧栏滚动，保持菜单点击无刷新。
- 范围内：`RoleAreaShell.vue`、`RoleSideNav.vue`、壳层结构门禁、Web build 脚本、验收证据。
- 非范围：业务 Provider、权限、数据范围、角色菜单内容、业务页面、金额/成本口径、DDL 和发布。
- 受影响端：Web。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：否。
- 业务事实来源与裁决编号：角色菜单继续使用 `src/roles/workbenchConfig.ts` 与 `src/roles/routes.ts` 的既有静态真源。
- 尚存 GAP 或假设置：无。
- 设计原则：统一骨架、角色数据驱动、单项展开、SPA 内容切换、浅色低噪音、品牌色只标记当前状态。
- 菜单内容不因展示优化而删减；TeamLeader 冻结状态不变。

## 4. 验收条件

| 序号 | 场景 | 预期结果 | 证据 |
| --- | --- | --- | --- |
| 1 | 标准壳层 | 存在 Sidebar/Header/Main Container/Footer | DOM + `check:shell-layout` |
| 2 | 角色菜单 | 每个角色使用自己的静态菜单配置，无死链 | `check:workbenches`、`check:workspaces` |
| 3 | 菜单过多 | 同一时间只展开一个业务组；Boss 23 项不再全部铺开 | 分组截图 |
| 4 | 1440×900 | 折叠态与展开“商务财务”均无侧栏内部滚动 | DOM 测量 |
| 5 | 1280×720 | 展开“商务财务”无侧栏内部滚动 | DOM 测量 |
| 6 | 1024×768 | 桌面侧栏收起，Header 菜单按钮打开统一抽屉 | 真实浏览器 |
| 7 | 390×844 | 无横向溢出，抽屉导航可读，无内容遮挡 | 移动截图 |
| 8 | 页面切换 | 点击菜单 URL 与内容变化，文档不刷新 | marker + navigation entries |
| 9 | 页面标题 | 路由标题继续使用当前页面 + 企业名称 | 浏览器标题 |
| 10 | Footer | 显示企业名称与系统版本，不抢占内容区 | DOM/截图 |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon-web | `dev` | 未提交 | `src/layouts/RoleAreaShell.vue`、`src/components/shell/RoleSideNav.vue`、`scripts/audit-web-shell-layout.mjs`、`package.json` |
| th4-decoboon | `dev` | 未提交 | `docs/operations/change-records/CHG-20261029-019-web-standard-shell-navigation.md`、`docs/acceptance/runs/20261029-web-standard-shell-navigation/**` |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| 壳层结构门禁 | `npm run check:shell-layout` | PASS | 命令输出 |
| 角色菜单自洽 | `npm run check:workbenches` | PASS | 命令输出 |
| 角色入口可达 | `npm run check:workspaces` | PASS | 命令输出 |
| 客户端文案 | `npm run check:client-copy` | PASS | 命令输出 |
| 品牌主题 | `npm run check:brand-theme` | PASS | 命令输出 |
| 类型检查 | `npm run type-check` | PASS | 命令输出 |
| 生产构建 | `npm run build` | PASS | Vite build 输出 |
| 菜单无刷新 | 真实浏览器 | PASS | `shell-spa-navigation-payables-1440.png` |
| 桌面/移动渲染 | 1440/1280/1024/390 | PASS，无横向溢出 | `docs/acceptance/runs/20261029-web-standard-shell-navigation/screenshots/` |

关键实测：

```text
升级前 Boss 侧栏：scrollHeight=1066 / clientHeight=717
升级后 Boss 折叠：827 / 827
展开“商务财务”：827 / 827
1280×720 展开“商务财务”：647 / 647
SPA marker：kept
document navigation entries：1
```

- 未执行项及原因：未发布生产、未做独立人工验收。
- 已知失败及是否与本次相关：无页面运行错误；冷启动存在既有 Vue Router `/login` warning，与本次壳层无关。

## 7. 发布

| 字段 | 内容 |
| --- | --- |
| Release ID | 不适用，未发布 |
| 制品路径/SHA-256 | 不适用 |
| 生产 release/版本 | 不适用 |
| 上一版本/回滚点 | Web `dev` 中本变更前提交 |
| 健康检查 | 本地登录、角色菜单、页面 SPA 切换 PASS |
| 发布后观察 | 待用户验收后决定 |

- 数据库迁移：无
- 配置/密钥变化：无

## 8. 回滚

- 回滚条件：角色菜单不可达、壳层遮挡、窄屏横向溢出、点击菜单触发整页刷新。
- 回滚命令/步骤：回退 `RoleAreaShell.vue`、`RoleSideNav.vue`、`audit-web-shell-layout.mjs` 与 `package.json` build 变更。
- 回滚验证：Boss/Finance 菜单可达、桌面与移动壳层、SPA 菜单切换。
- 实际是否回滚：否

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集；使用 `zhuangdeqing-ui-craft` 与本地 `qwen2.5vl:7b` 视觉预检。
- 人工耗时：未采集
- 一次通过：否
- 返工原因：1280×720 首次展开最大业务组仍有 17px 内部溢出，已压缩分组头与导航底部留白修正。
- 最终结论：待用户验收

## 10. 复盘与沉淀

- 是否出现重复问题：否
- 是否需要新增门禁：是；新增 `check:shell-layout` 并纳入 build，防止标准骨架、RouterLink 和分组导航被后续改回。
- 候选 `ANT`：把全部角色菜单平铺在固定侧栏，用隐藏滚动条掩盖信息架构问题。
- 候选 `PATH`：统一壳层骨架 + 角色配置驱动 + 单项分组展开 + RouterLink 内容切换。
- 后续动作与负责人：用户本地验收 `http://localhost:8081/workbench`。
