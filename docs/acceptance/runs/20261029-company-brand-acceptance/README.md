# 企业信息、Logo 与品牌色投影 · 开发验证证据

> 状态：`DEV_VERIFIED_PENDING_USER`  
> 性质：本地 Web、微信开发者工具、Provider 文件上传与页面回读证据；最终感受验收由用户完成。

## 本批范围

- Batch：`BRAND-D1`
- 角色：企业负责人（BOSS）
- App：`/subpackages/company/pages/info`、`/pages/mine/index`
- Web：`/account/company`
- 小程序投影：员工邀请、业主首页、业主分享页

## 页面证据

| 场景 | 截图 | 结果 |
| --- | --- | --- |
| App 我的页 | `screenshots/app-mine-logout-bottom.png` | “退出登录”位于全部业务分组之后 |
| App 品牌色主题 | `screenshots/app-brand-theme-purple.png` | `#7C3AED` 应用于 page-meta、品牌预览与页面主题 |
| App 点击即时换肤 | `screenshots/app-brand-color-live-preview.png` | 点击“蓝图”后未保存即切换为 `#2563EB`，取消后恢复原主题 |
| Web 品牌色主题 | `screenshots/web-brand-theme-purple.png` | `#7C3AED` 应用于根 Token、Element Plus 与页面强调色 |
| Web 点击即时换肤 | `screenshots/web-brand-color-live-preview.png` | 点击“蓝图”后未保存即变为 `#2563EB`，取消后恢复默认 |
| App 正常字重 | `screenshots/app-company-info-normal-weight.png` | 内容 400、标签/标题 500，避免统一粗体 |
| App 首页主题覆盖 | `screenshots/app-home-theme-blue.png` | 图标底、统计正常状态点和主题强调跟随蓝色 |
| App 我的主题覆盖 | `screenshots/app-mine-theme-blue.png` | 用户 Hero、默认头像和图标底跟随蓝色 |
| App 财务主题覆盖 | `screenshots/app-finance-theme-blue.png` | 财务 Hero 使用派生品牌渐变 |
| App 企业 Hero Logo | `screenshots/app-company-theme-blue.png` | Logo 使用独立白色承载面，不与品牌 Hero 融合 |
| App 新版色板 | `screenshots/app-zcool-palette-default.png` | 装得清产品默认色保留，新增青瓷、松石墨绿、岩蓝、赤陶、胡桃、黛紫、墨灰 |
| App 岩蓝即时预览 | `screenshots/app-zcool-palette-rock-blue.png` | 选择 `#4F708C`，取消恢复装得清产品默认色 |
| Web 岩蓝即时预览 | `screenshots/web-zcool-palette-rock-blue.png` | Web 产品默认色 `#2F7D5A` → 岩蓝 `#4F708C` → 取消恢复 |

## 闭环验证

| 动作 | 结果 |
| --- | --- |
| Logo 图片上传到既有 `company/company/update` 的 `logoFile` | PASS，返回 `Company.logo` |
| 企业 Logo 匿名读取（邀请页需要） | PASS，`/api/file/private/download` 返回 PNG 200 |
| 品牌色保存与 `company/company/info` 回读 | PASS |
| App 企业信息回读 | PASS，显示“已上传企业Logo / 已自定义品牌色” |
| Web 企业信息回读 | PASS，Logo 实际加载且页面无横向溢出 |
| 测试品牌数据恢复 | PASS，当前企业 `logo` 已清空，`color` 已恢复为空 |
| 品牌色主题同步 | PASS，Company.color=`#7C3AED` 时 App/Web 读取同一品牌基色 |
| 主题派生色阶 | PASS，Web 根变量生成 `#7C3AED → #4A238E`；Element Plus 主色同步 |
| App 全页面主题注入 | PASS，构建产物 189 个页面 WXML 均包含 `page-meta` 主题绑定 |
| Web 跨端变更刷新 | PASS，Web 保持打开时后台修改 Company.color，窗口重新获得焦点后根主题自动从默认色切到 `#2563EB`，恢复后回到默认色 |
| Web 点击即时换肤 | PASS，点击“蓝图”后未保存状态下根 Token/Element Plus/主按钮立即变为 `#2563EB`；取消后恢复默认色 |
| App 点击即时换肤 | PASS，真实调用预设事件后 page-meta 从 `#128276` 立即变为 `#2563EB`；取消后恢复 `#128276` |
| 企业信息编辑 TabBar | PASS，编译产物无 `setTabBarStyle`，开发者工具编辑+换色无报错 |
| Logo 首字文字色 | PASS，`#16A394`（3.13）与 `#0E8A7D`（4.24）使用白字；浅黄色仍使用深色字 |
| Hero Logo 独立承载面 | PASS，企业信息、业主首页、分享页使用白色 Logo 面 |
| 我的用户 Hero | PASS，使用企业品牌浅色渐变 |
| 默认员工头像 | PASS，头像底色使用企业主题色，岗位色只保留状态点 |
| 图标/统计强调 | PASS，图标底、正常状态点、环比正增长文字使用企业色 |
| TabBar 边框 | PASS，顶部边框和阴影使用企业色透明阶 |
| 财务 Hero 渐变 | PASS，使用统一 `$jzb-gradient`，无硬编码绿色尾色 |
| 页面大背景 | PASS，继续使用装得清中性产品底色 `#F4F7F8`，不整页染色 |
| 站酷参考色板 | PASS，新增青瓷、松石墨绿、岩蓝、赤陶、胡桃、黛紫、墨灰 |
| 装得清产品默认色 | PASS，保持 `#16A394` 不被企业主题改版替换 |
| 岩蓝即时切换 | PASS，App `#16A394` / Web `#2F7D5A` 的产品默认色保留；选择岩蓝 `#4F708C` 后取消恢复各自产品色 |

测试 Logo 只用于本轮链路验证，不在企业档案中保留。

## 保存后的真实反应

| 小程序表面 | 反应 |
| --- | --- |
| 我的 → 企业信息 | 显示企业 Logo；无 Logo 显示简称首字；显示所选品牌色 |
| 员工邀请 | 邀请企业区域显示同一 Logo、简称占位和品牌色 |
| 业主首页 | 服务方标识使用企业 Logo/占位和品牌色 |
| 业主分享页 | 分享页服务方使用企业 Logo/占位和品牌色 |
| 当前页面数据 | 保存后立即回读；重新进入和重新登录读取同一 Company 主档 |
| App/小程序全局主题 | 主按钮、页头、页签、选中态、品牌卡片和业主端品牌强调按派生色阶切换 |
| Web 全局主题 | 侧边栏强调、按钮、链接、选中态、Element Plus 组件和品牌工作面按派生色阶切换 |

## 明确未闭环

- 已生成的合同、报价、凭证和历史文件不会被当前品牌主档静默回写。
- 合同/PDF/Excel/打印的 Logo、地址、税号和品牌快照仍属于 `BRAND-D3`。
- 门店品牌覆盖、地图点位、资质附件和历史版本仍待业务裁决。

## 验证结果

- 后端定向测试：`CompanyBrandContextTest`、`AvatarDownloadProviderTest` PASS。
- App：type-check、首页/注册/邀请/导航/企业品牌/UI 契约、小程序构建、分包检查 PASS。
- Web：type-check、build、client-copy、forms、concepts、states、actions、metrics、permissions、page-system、workbenches、workspaces 检查 PASS。
- Web 真实渲染：console 0 error；页面横向溢出 0；Logo 图片实际加载。
- 微信开发者工具：企业信息品牌状态与“我的”页退出入口回读 PASS。
- `run-dev.sh`：JDK22 + `target/classes:target/lib/*` 启动 PASS。
- `qwen2.5vl:7b` 视觉预检完成，不替代用户人工感受验收。
- 品牌色主题预检：`vision-review-app-theme.md`、`vision-review-web-theme.md`。
- 正常字重预检：`vision-review-app-normal-weight.md`。

## 品牌主题测试数据

- 装得清产品默认色：App `#16A394`，Web 保持原产品色 `#2F7D5A`。
- 临时主题验证色：岩蓝 `#4F708C`。
- 历史功能验证曾使用 `#7C3AED` / `#2563EB`，不代表当前推荐色板。
- Web 根变量：`--zdq-brand-source=#7C3AED`、`--zdq-brand-500=#7C3AED`、`--zdq-brand-700=#4A238E`、`--el-color-primary=#7C3AED`。
- App page-meta：`--jzb-primary:#7C3AED`，并由全局派生色阶覆盖主包与分包页面。
- 验证后已恢复 `Company.color` 为空，回到装得清默认品牌色。

## 本地验收入口

- Web：`http://localhost:8081`
- App H5：`http://localhost:8080`
- 后端：`http://127.0.0.1:8090/api/`
- 微信开发者工具项目：`/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/前端代码/th4-decoboon-app/dist/dev`
