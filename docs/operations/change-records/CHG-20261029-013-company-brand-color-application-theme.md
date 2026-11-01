# CHG-20261029-013 · 装企品牌色作为 App/小程序/Web 应用主题色

> 后续色板调整：推荐色板已由 `CHG-20261029-016` 按站酷 B 端研究重做；装得清产品默认色仍为 App `#16A394` / Web `#2F7D5A`，松石墨绿 `#2F6E63` 仅为企业可选主题。本记录保留机制验证过程。

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | `CHG-20261029-013` |
| 类型 | 产品规则 / 跨端主题改造 |
| 风险级别 | L2 |
| 状态 | `DEV_VERIFIED_PENDING_USER` |
| 负责人 | codex |
| 日期 | 2026-10-29 |
| 关联需求/事故/业务裁决 | `PI-20261029-002`、`USER-DEC-20261029-004`、`USER-DEC-20261029-006` |

## 2. 目标与范围

- 目标：删除无业务意义的对外署名配置；让 `Company.color` 成为 App、小程序和 Web 的统一运行时主题主源。
- 范围内：企业品牌色自定义、派生色阶、App 全页面主题注入、Web Token/Element Plus 主题、企业信息页预览、小程序业主/邀请页面。
- 非范围：合同/PDF/Excel/打印品牌快照、地图地址、门店品牌覆盖。
- 受影响端：后端（沿用既有 Company.color）、App、小程序、Web。

## 3. 业务与决策

- 装得清固定为软件提供者，装企固定为业务服务主体。
- 删除“对外展示主体: 装企/平台”和“记录存证署名开关”；软件提供者说明采用固定、非配置化表达。
- 企业品牌色是 App/小程序/Web 主题唯一输入，成功、警告、危险和业务状态色不参与换肤。
- 未配置品牌色时使用装得清默认品牌色，不写回企业自定义事实。

## 4. 验收条件

| 序号 | 场景 | 预期结果 | 证据 |
| --- | --- | --- | --- |
| 1 | BOSS 选择预设品牌色 | 企业品牌色保存并即时更新 Web 主题 | Web 根变量/Element Plus |
| 2 | BOSS 输入自定义色 | 校验 #RGB/#RRGGBB，生成浅色/深色/渐变/焦点/可读前景 | App/Web 页面 |
| 3 | 打开小程序任意页面 | 页面主题 CSS 变量继承企业品牌色 | 189/189 页面 WXML 含 page-meta |
| 4 | 打开 Web 任意页面 | 品牌 Token 与 Element Plus 主色跟随企业色 | 根变量与真实按钮计算样式 |
| 5 | 企业切换为紫色 `#7C3AED` | App/Web 同时读取同一 Company.color；Web 派生 `#4A238E` 深色 | 运行记录/截图 |
| 6 | 企业恢复默认色 | App/Web 回到装得清默认主题 | 运行记录 |
| 7 | 对外署名配置 | App/Web 不再出现该配置 | 页面源码/契约 |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon-app | `dev` | 未提交 | `brandTheme.ts`、`tokens.ts`、`uni.scss`、`vite.config.ts`、企业信息页、业主/邀请页面 |
| th4-decoboon-web | `dev` | 未提交 | `core/brandTheme.ts`、`tokens.scss`、`element-theme.scss`、`App.vue`、企业信息页 |
| th4-decoboon | `dev` | 未提交 | 沿用 `Company.color`，无新增 Provider/迁移 |

## 6. 验证

| 检查 | 结果 | 证据 |
| --- | --- | --- |
| App type-check/build/分包 | PASS | 主包 14 页，17 分包 / 175 页 |
| App 企业品牌/UI 契约 | PASS | `check:company-brand`、`check:ui` |
| Web type-check/build | PASS | `check:brand-theme`、`check:forms`、`check:client-copy` |
| App 全页面主题注入 | PASS | 189/189 WXML 含 `page-meta` 主题绑定 |
| App 紫色主题运行 | PASS | page-meta `--jzb-primary:#7C3AED` |
| Web 紫色主题运行 | PASS | root/Element Plus 均为 `#7C3AED`，派生深色 `#4A238E`，overflow=0 |
| Web 跨端刷新 | PASS | 页面保持打开时修改 Company.color，窗口 focus 后重新读取并切换主题 |
| Web 点击即时预览 | PASS | 点击“蓝图”后仅凭页面操作，根 Token/Element Plus 主色立即变为 `#2563EB`；取消后恢复默认色 |
| App 实时换色 | PASS | 微信开发者工具调用“蓝图”后 page-meta 从默认 `#128276` 立即变为 `#2563EB`；取消后恢复 |
| App 正常字重 | PASS | 企业信息内容 400、标签/标题 500；页面不再统一使用粗体 |
| Hero Logo 可读性 | PASS | 企业信息、业主首页、分享页改为独立白色 Logo 面 |
| 我的用户 Hero | PASS | 品牌浅色渐变替代固定角色底色 |
| 默认头像/图标/统计强调 | PASS | 默认员工头像、图标底、正常状态点、环比正增长文字使用企业主题色 |
| TabBar/财务 Hero | PASS | TabBar 边框阴影使用企业透明色阶；财务 Hero 使用统一品牌渐变 |
| 页面大背景 | PASS | 保持装得清中性产品色 `#F4F7F8`，不整页染成企业色 |
| 测试色恢复 | PASS | Company.color 已恢复为空 |
| 视觉预检 | PARTIAL | `qwen2.5vl:7b` 完成，不替代用户验收 |

## 7. 发布

- 暂不发布；等待用户本地感受验收。
- 数据库迁移：无。
- 配置/密钥变化：无。

## 8. 回滚

- 回滚条件：主题导致文字不可读、状态色错误、页面构建失败或企业品牌无法回读。
- 回滚步骤：回退 App/Web 主题运行时和 Token 映射；Company.color 清空即回默认主题。
- 回滚验证：App/Web 主按钮、导航选中态、页面背景和语义状态色。
- 实际是否回滚：否。

## 9. 成本与结论

- 最终结论：同一 Company.color 已驱动 App/小程序/Web 主题；待用户感受验收。

## 10. 复盘与沉淀

- 反模式：把品牌色只用于 Logo 背景；让业务语义色跟随品牌色；让企业配置软件提供者身份。
- 后续：补充任意自定义色的多机型视觉回归，并治理合同/PDF 品牌快照。
