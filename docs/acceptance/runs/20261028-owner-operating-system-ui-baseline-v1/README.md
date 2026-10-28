# 负责人工作台 UI Baseline V1 · 原型证据

> 概念原型渲染与交互证据，不是生产 UI 验收、业务验收或发布结论。

## 原型

- 文件：`docs/product/prototypes/owner-operating-system/index-v3.html`
- 设计基线：`docs/product/domains/owner-operating-system/14-ui-baseline-v1.md`
- 能力数据：`docs/product/domains/owner-operating-system/current-capabilities.json`
- 样板页：经营总览、业务推进、项目对象
- 端：Web、移动端

## 体验路径

1. 经营总览：看今日经营判断、3 个决策、4 个指标和业务停留。
2. 业务推进：切换阶段，查看负责人观测、当前对象和当前能力。
3. 项目对象：查看对象身份条、唯一主行动、任务、对象关系、负责人观测和时间线。
4. 点击“亲自处理”：在当前对象内选择业务员、设计师、项目经理、财务或店长方式。
5. 点击能力：查看该能力在生产分支的成熟度、Web/移动端入口和缺口。
6. 点击顶部“查看 UI 基线”：查看 Token 和页面规则。
7. 切换移动端：查看总览、推进、对象和亲自处理。

## 截图

| 端 | 分辨率 | 截图 |
| --- | --- | --- |
| Web 经营总览 | 1440×1000 | `screenshots/web-overview-1440.png` |
| Web 业务推进 | 1440×1000 | `screenshots/web-progress-1440.png` |
| Web 项目对象 | 1440×1000 | `screenshots/web-object-1440.png` |
| Web UI 基线抽屉 | 1440×1000 | `screenshots/web-ui-baseline-1440.png` |
| Web 亲自处理 | 1440×1000 | `screenshots/web-direct-processing-1440.png` |
| Web 能力详情 | 1440×1000 | `screenshots/web-capability-detail-1440.png` |
| Web 经营总览 | 1024×900 | `screenshots/web-overview-1024.png` |
| 移动端总览 | 390×844 | `screenshots/mobile-overview-390.png` |
| 移动端业务推进 | 390×844 | `screenshots/mobile-progress-390.png` |
| 移动端项目对象 | 390×844 | `screenshots/mobile-object-390.png` |
| 移动端亲自处理 | 390×844 | `screenshots/mobile-direct-processing-390.png` |

## 验证

- JavaScript 语法检查 PASS。
- Chromium 两轮真实渲染 PASS。
- 三页 Web 切换、八阶段切换、能力详情、亲自处理、岗位动作、UI 基线抽屉 PASS。
- 控制台 0 error / 0 warning。
- 1440/1024/390 无页面级横向溢出。
- 可见文字最小 12px。
- 键盘焦点可见。

## 不覆盖

- 真实 Provider、账号、租户和业务写操作。
- 独立验收和正式发布。
- 企业经营、运营标准、组织管理的完整 UI 迁移。
