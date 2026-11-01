# PI-20261029-007 · 小程序企业编辑 TabBar 报错与 Logo 首字颜色错误

| 项 | 内容 |
| --- | --- |
| 问题 ID | `PI-20261029-007` |
| 当前状态 | `CLOSED / ADMIN_CYCLE_RESET`（不代表已验收或已解决） |
| 严重级别 | P1 |
| 发现日期 | 2026-10-29 |
| 来源 | 用户点击小程序企业信息“编辑”后出现 `setTabBarStyle:fail not TabBar page`；默认色/青绿色 Logo 首字为黑色 |
| 所属领域 | App 品牌主题、企业 Logo 占位 |
| 变更记录 | `CHG-20261029-014-brand-theme-runtime-regression.md` |

## 根因

1. App 使用自定义 `JzTabBar`，`pages.json` 没有原生 `tabBar`，但主题逻辑仍调用 `uni.setTabBarStyle`。该 API 在非原生 TabBar 页面异步返回失败，外层同步 `try/catch` 无法捕获。
2. Logo 首字使用普通正文的 4.5:1 白字阈值。平台默认青绿 `#16A394` 与白色对比度为 3.13，被误判为浅色而改用黑字；青绿色 `#0E8A7D` 为 4.24，也被误判。

## 处理

- 删除原生 `setTabBarStyle` 调用；自定义 `JzTabBar` 继续由 CSS Variables 和 `TOKEN_COLOR` 跟随主题。
- Logo 首字是大字号品牌标记，按 WCAG 大文本 3:1 阈值选择文字色。
- 保持浅黄色等真正浅色品牌使用深色首字。

## 验证

- 编译产物中不再包含 `setTabBarStyle`。
- 微信开发者工具进入企业信息、点击编辑并切换“蓝图”：无 TabBar 报错，主题 `#128276 → #2563EB`。
- `#16A394` 白字对比度 3.13、`#0E8A7D` 4.24，均使用白字；`#F5D547` 1.45，使用深色字。
- App 品牌/UI 契约、type-check、小程序构建和分包检查 PASS。

## 关闭条件

```text
企业信息编辑页不再出现 setTabBarStyle 报错 PASS
默认青绿与青绿色 Logo 首字显示白色 PASS
浅色自定义品牌仍能自动使用深色首字 PASS
用户确认交互 PASS
```

## 关闭记录（2026-11-02）

- 关闭类型：`ADMIN_CYCLE_RESET`，用户要求结束当前研发周期。
- 关闭前状态：`DEV_VERIFIED_PENDING_USER`。
- 本次关闭不代表问题已解决、已验收或已发布；原事实、证据、风险和未完成事项继续保留。
- 重启条件：用户重新立项，或出现新的失败用例、生产证据、业务裁决。
