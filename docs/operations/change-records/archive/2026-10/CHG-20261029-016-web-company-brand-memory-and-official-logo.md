# CHG-20261029-016 · Web 登录品牌记忆与官方 Logo

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | `CHG-20261029-016` |
| 类型 | 需求 / Web 品牌体验 |
| 风险级别 | L1 |
| 状态 | `DEV_VERIFIED_PENDING_USER` |
| 负责人 | codex |
| 日期 | 2026-10-29 |
| 关联需求/事故/业务裁决 | `PI-20261029-009`、`USER-DEC-20261029-006` |

## 2. 目标与范围

- 目标：登录页记住上次登录企业和手机号，使用装企 Logo/品牌色；Web 平台 Logo 改用官方资产。
- 范围内：Web 登录页、last-login 本地缓存、角色壳层 Logo、favicon。
- 范围内补充：favicon 随企业 Logo 动态切换；登录后工作台隐藏装得清文案和描述。
- 非范围：自动登录、保存密码、跨浏览器同步、企业业务数据缓存。
- 受影响端：Web。

## 3. 业务与决策

- 装企是业务服务主体，装得清是软件提供者。
- 登录页可以展示上次企业品牌，但不得绕过密码认证。
- 本地缓存只用于体验，不作为权限或事实来源；进入系统后仍以 `/auth/session/info` 为唯一会话事实。

## 4. 验收条件

| 序号 | 场景 | 预期结果 | 证据 |
| --- | --- | --- | --- |
| 1 | 首次打开登录页 | 使用官方装得清 Logo 和默认主题 | 页面/资产哈希 |
| 2 | 登录并退出 | 登录页显示上次装企 Logo、名称、品牌色和手机号 | 真实浏览器流程 |
| 3 | 检查本地存储 | 无密码、无 token | localStorage 记录 |
| 4 | 企业换 Logo/品牌色后再次登录 | last-login 缓存随 `/auth/session/info` 更新 | loadProfile 回写 |
| 5 | 工作台壳层 | 使用官方装得清白色 Mark | 真实渲染 |
| 8 | 浏览器 favicon | `favicon.ico`、SVG、32px PNG 与 Apple Touch Icon 均来自官方彩色 Mark，并使用版本参数刷新缓存 | HTTP/资源检查 |
| 9 | 企业品牌 favicon | 有企业 Logo 时 favicon 指向同一 Logo；无 Logo 时使用企业首字与品牌色 | 真实浏览器 |
| 10 | 首页无产品文案 | 登录后浏览器标题与页面正文不出现“装得清” | DOM/标题检查 |
| 6 | 装企品牌态 | 左侧主品牌为企业Logo/名称，右侧标题包含企业名称，无独立“上次登录企业”卡片 | 登录/退出真实浏览器 |
| 7 | 家装元素 | 户型线稿与量房/设计/报价/施工/验收路径可见且不干扰表单 | 1440px 真实渲染 |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon-web | `dev` | 未提交 | `core/lastLogin.ts`、`views/auth/LoginView.vue`、`BrandLogo.vue`、各角色 Layout、官方 Logo 资产 |

## 6. 验证

| 检查 | 结果 |
| --- | --- |
| Web type-check/build | PASS |
| 品牌主题契约 | PASS |
| 登录记忆真实浏览器流程 | PASS |
| 企业 Logo 加载 | PASS，naturalWidth=256 |
| 平台 Logo 加载 | PASS，naturalWidth=256 |
| 缓存无密码/token | PASS |
| 官方 Logo SHA-256 | PASS |
| favicon.ico MIME/尺寸 | PASS，`image/x-icon`，16/32/48/64 多尺寸 |
| SVG favicon | PASS，`image/svg+xml` |
| favicon 缓存刷新 | PASS，入口使用 `?v=1.0.73` |
| 企业 favicon 同步 | PASS，企业 Logo URL 与侧栏 Logo 同源 |
| 首页产品文案 | PASS，标题“工作台 \| 昭青装饰”，正文不含“装得清” |

## 7. 发布

- 暂不发布，等待用户验收。
- 数据库迁移：无。
- 配置/密钥变化：无。

## 8. 回滚

- 回滚条件：登录页错误恢复企业品牌、缓存敏感信息或官方 Logo 加载失败。
- 回滚步骤：删除 last-login 读取与 BrandLogo 替换，恢复原登录页。
- 实际是否回滚：否。

## 9. 成本与结论

- 最终结论：登录品牌记忆和官方 Logo 已通过真实 Web 流程验证。

## 10. 复盘与沉淀

- 反模式：为了“记住品牌”而缓存密码或 token；用文字方块冒充产品 Logo；把本地展示缓存当登录凭据。
