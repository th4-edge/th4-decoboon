# 装企信息与品牌投影 · UI 与交互审查

## 审查对象

- 页面/路由：App `/subpackages/company/pages/info`、Web `/account/company`、App `pages/mine/index`
- 角色：企业负责人（BOSS）
- 设备/分辨率：微信开发者工具 812×1756；Web 1200px 全页
- 业务蓝图节点：`BRAND-D1` 企业品牌维护与小程序投影

## 证据

| 场景 | 截图/路径 | 结果 | 问题 |
| --- | --- | --- | --- |
| 正常 | `app-company-info-with-test-brand.png`、`web-company-info-edit-with-test-brand.png` | 分组、示例、字段帮助、Logo/品牌色预览可见 | 用户感受验收中 |
| 空态 |  |  |  |
| 失败 |  |  |  |
| 无权限 |  |  |  |
| 长内容 | `web-company-info-edit-with-test-brand.png` | 帮助文案块级布局，无页面级横向溢出 | 用户感受验收中 |
| 极端数值 |  |  |  |

## 缺陷

| ID | 页面 | 现象 | 期望 | 级别 | 状态 |
| --- | --- | --- | --- | --- | --- |
|  |  |  |  |  |  |

## 跨端一致性

- App 与 Web 字段/状态/下一步是否一致：同一 `company/company/update`、Company 字段和占位规则一致。
- 差异是否已登记：App 为单列渐进式、Web 为完整编辑；属于设备密度差异。
- 视觉预检：`qwen2.5vl:7b` 完成，不替代用户最终人工验收。
