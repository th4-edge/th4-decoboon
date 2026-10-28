# 装企负责人经营管理全景 · 高保真概念原型

打开：`index.html`

## 可体验内容

- 顶部切换 `Web / App / 微信小程序`
- Web 左侧切换：经营总览、业务推进、企业经营、运营工具、组织管理
- 业务推进：点击八个阶段，查看负责人观测点、责任与下一步
- 风险与阶段卡：打开对象处置抽屉，查看事实、判断、回读项和边界
- 岗位视角：切换业务员、设计师、项目经理、财务、店长视角，身份与数据范围不变
- App / 微信小程序：使用同一对象和同一口径展示移动端职责

## 事实边界

- 全部为演示数据，不接真实 Provider。
- 成本不完整时项目毛利为空。
- 公司/门店经营利润显示“口径待确认”，不展示确定金额。
- 微信小程序是否独立成壳仍为 `GAP-BOS-001`。
- 本原型不是生产页面、不是独立验收、不是发版候选。

## 现状能力对照

- 当前生产实际提供给负责人的 62 个能力组：`../../domains/owner-operating-system/11-current-capability-inventory.md`
- 原型用于表达未来信息架构，不替代当前生产能力清单。

## 整合版 V2

- `index-v2.html`：基于当前 62 个生产能力组，采用“经营总览 + 业务对象 + 亲自处理”三层模型。
- 设计评审：`../../domains/owner-operating-system/13-integrated-v2-review.md`
- 交互证据：`../../../acceptance/runs/20261028-owner-operating-system-v2/README.md`

## UI Baseline V1

- `index-v3.html`：经营总览、业务推进、项目对象三个样板页，验证“工程蓝图 + 经营账本”的视觉方向。
- 设计基线：`../../domains/owner-operating-system/14-ui-baseline-v1.md`
- 截图证据：`../../../acceptance/runs/20261028-owner-operating-system-ui-baseline-v1/README.md`

## Dig UI 重构版

- `dig-v1.html`：使用 dig catalog 的负责人 Web/App UI 重构概念，含 dark/light、中/EN、经营总览、业务推进和项目对象。
- 审查与设计：`../../domains/owner-operating-system/15-dig-ui-review-and-redesign.md`
- 证据：`../../../acceptance/runs/20261028-owner-dig-ui-review/README.md`
