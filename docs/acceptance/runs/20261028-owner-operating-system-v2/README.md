# 负责人经营管理整合版 V2 · 原型证据

> 性质：概念原型渲染与交互证据，不是生产 UI 验收、业务验收或发布结论。

## 原型

- 文件：`docs/product/prototypes/owner-operating-system/index-v2.html`
- 数据源：`docs/product/domains/owner-operating-system/current-capabilities.json`
- 设计说明：`docs/product/domains/owner-operating-system/13-integrated-v2-review.md`

## 体验路径

1. Web 经营总览：看今日判断、风险、业务停留点。
2. 业务推进：点击八个阶段，查看负责人观测点、当前对象和已有生产能力。
3. 点击能力 chip：查看该能力在生产分支的实现状态、Web/移动端覆盖和缺口。
4. 点击“亲自处理”：在当前对象中切换业务员、设计师、项目经理、财务或店长方式。
5. 企业经营：查看签约、回款、应收应付、成本完整性和口径待确认区域。
6. 运营标准：查看报价库、套餐、计划、模板和主数据。
7. 组织管理：查看组织地图、邀请、交接、角色范围与审计。
8. 移动端：看经营判断、风险对象、阶段和亲自处理。

## 截图

| 端 | 分辨率 | 截图 |
| --- | --- | --- |
| Web 经营总览 | 1440×1000 | `screenshots/web-overview-1440.png` |
| Web 业务推进 | 1440×1000 | `screenshots/web-progress-1440.png` |
| Web 企业经营 | 1440×1000 | `screenshots/web-operation-1440.png` |
| Web 运营标准 | 1440×1000 | `screenshots/web-standards-1440.png` |
| Web 组织管理 | 1440×1000 | `screenshots/web-organization-1440.png` |
| Web 亲自处理 | 1440×1000 | `screenshots/web-direct-processing-1440.png` |
| Web 能力详情 | 1440×1000 | `screenshots/web-capability-detail-1440.png` |
| Web 经营总览 | 1024×900 | `screenshots/web-overview-1024.png` |
| 移动端总览 | 390×844 | `screenshots/mobile-overview-390.png` |
| 移动端业务推进 | 390×844 | `screenshots/mobile-progress-390.png` |
| 移动端亲自处理 | 390×844 | `screenshots/mobile-direct-processing-390.png` |

## 验证

- JavaScript 语法检查 PASS。
- Chromium 渲染 PASS。
- 五页切换、八阶段切换、能力抽屉、亲自处理、角色动作和移动端导航 PASS。
- 控制台 0 error / 0 warning。
- 1440/1024/390 三档无页面级横向溢出。
- 可见文字最小 12px。
- 键盘焦点可见。

## 不覆盖

- 真实 Provider、账号、租户和数据。
- 审批、付款、删除、权限变更。
- 独立验收和正式发布。
