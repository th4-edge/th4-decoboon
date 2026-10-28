# 装得清负责人 Web / App Dig UI 重构原型 · 证据

> 概念原型渲染与交互证据，不是生产 UI 验收、业务验收或发布结论。

## 原型

- 文件：`docs/product/prototypes/owner-operating-system/dig-v1.html`
- 审查与设计说明：`docs/product/domains/owner-operating-system/15-dig-ui-review-and-redesign.md`
- 当前能力数据：`docs/product/domains/owner-operating-system/current-capabilities.json`

## 体验

1. Web 经营总览：KPI、负责人决策、经营信号、业务推进、事件流。
2. 业务推进：搜索、筛选、对象表格、阶段观测。
3. 项目对象：对象身份条、唯一主行动、任务、关系、观测、时间线。
4. 亲自处理：在对象内选择业务员、设计师、项目经理、财务或店长方式。
5. 能力详情：查看当前能力状态、Web/移动端入口和缺口。
6. 顶部 UI 基线：查看 Token、组件状态和布局规则。
7. dark/light 与中/EN 关键控件切换。
8. Web/App 预览切换。

## 截图

| 端 / 状态 | 分辨率 | 截图 |
| --- | --- | --- |
| Web 总览 dark | 1440×1000 | `screenshots/web-overview-dark-1440.png` |
| Web 业务推进 dark | 1440×1000 | `screenshots/web-progress-dark-1440.png` |
| Web 项目对象 dark | 1440×1000 | `screenshots/web-object-dark-1440.png` |
| Web 总览 light | 1440×1000 | `screenshots/web-overview-light-1440.png` |
| Web 总览 light / EN | 1440×1000 | `screenshots/web-overview-light-en-1440.png` |
| Web 亲自处理 | 1440×1000 | `screenshots/web-direct-drawer-1440.png` |
| Web UI 基线 | 1440×1000 | `screenshots/web-ui-baseline-1440.png` |
| Web 总览 dark | 1024×900 | `screenshots/web-overview-1024.png` |
| App 总览 dark | 390×844 | `screenshots/app-overview-dark-390.png` |
| App 业务推进 dark | 390×844 | `screenshots/app-progress-dark-390.png` |
| App 项目对象 dark | 390×844 | `screenshots/app-object-dark-390.png` |
| App 亲自处理 | 390×844 | `screenshots/app-direct-drawer-390.png` |

## 验证

- HTML 脚本语法 PASS。
- Chromium 渲染 PASS。
- Web 三页切换、筛选区、对象进入、亲自处理、能力详情、UI 基线抽屉 PASS。
- Web/App 切换 PASS。
- dark/light 切换 PASS。
- 中/EN 关键控件切换 PASS。
- 1440/1024/390 无页面级横向溢出。
- 可见文字最小 12px。
- 浏览器 console 0 error / 0 warning。

## 不覆盖

- 真实 Provider、真实账号和真实业务写入。
- 生产组件实现。
- 独立验收和正式发布。
