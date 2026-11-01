# Web 登录装企品牌记忆与官方 Logo · 开发验证证据

> 当前登录页文案与视觉已继续优化。最新实现和截图以 `docs/acceptance/runs/20261029-web-login-visual-refinement/` 与 `CHG-20261029-017` 为准；本目录保留品牌记忆和官方 Logo 的原始验证基线。

> 状态：`DEV_VERIFIED_PENDING_USER`

## 验证流程

1. 临时为企业设置测试 Logo 与品牌色 `#2563EB`。
2. 使用手机号和密码登录 Web。
3. 退出登录。
4. 登录页恢复上次装企名称、Logo、品牌色和手机号。
5. 检查本地缓存不包含密码与 token。
6. 恢复企业 Logo/品牌色，并清理测试浏览器存储。

## 结果

| 项目 | 结果 |
| --- | --- |
| 登录后落点 | `/workbench` |
| 上次企业 | 昭青装饰 |
| 手机号预填 | `18660219918` |
| 企业 Logo | `/api/file/private/download?...`，naturalWidth=256 |
| 登录页品牌色 | `#2563EB` |
| favicon | 登录后/退出后的企业品牌态直接使用企业 Logo URL |
| 平台文案 | 装企品牌态页面正文不含“装得清”文案 |
| 缓存密码 | 否 |
| 缓存 token | 否 |
| 工作台侧栏 Logo | 官方白色 Mark |
| favicon | `/favicon.ico`、官方 SVG、32px PNG、Apple Touch Icon 均正确返回 MIME |
| favicon 缓存刷新 | 入口使用 `?v=1.0.73`，避免浏览器继续使用旧图标 |

## 截图

- `screenshots/login-remembered-company.png`
- `screenshots/login-enterprise-logo-final.png`
- `screenshots/login-first-use-platform.png`
- `screenshots/workbench-official-logo.png`
- `screenshots/workbench-enterprise-only.png`

## 登录页双状态

| 状态 | 页面表现 |
| --- | --- |
| 首次使用、无本地记录 | 官方装得清 Logo、装得清默认品牌色、“登录企业工作台” |
| 登录过一次后 | 装企 Logo/名称、装企品牌色、“登录{装企名称}工作台” |
| 登录后工作台 | 侧栏企业 Logo/名称，浏览器标题“工作台 \| 企业名称”，不出现装得清文案 |
| favicon | 首次为官方装得清 Mark；登录缓存后同步为企业 Logo 或企业首字+品牌色 |

企业品牌态不再显示“上次登录企业”独立卡片；企业名称直接进入左侧主标题和右侧登录标题。

密码提示统一为：“请打开小程序「我的 → 账号密码」设置或修改密码，设置完成后返回这里登录。”

登录页加入克制的户型线稿和「量房 → 设计 → 报价 → 施工 → 验收」家装业务元素。

## 安全边界

- 本地缓存键：`zdq.web.last-login.v1`。
- 有效期：180 天。
- 只含手机号、员工姓名、企业公开品牌信息。
- 登录仍必须输入密码；缓存不授予任何访问权限。
