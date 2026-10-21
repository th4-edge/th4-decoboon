# OWNER-UI-V2-C · 微信开发者工具同屏一致性验收

- 日期：2026-10-21
- 环境：本地 `decoboon_owner_preview_acceptance` + Spring Boot `dev` + 微信开发者工具 `2.02.2609231 RC`
- 员工预览：BOSS「验收负责人」`13910000012`
- 业主真实会话：李思远 `18660219981`
- 项目：`236266510782300160`（华润凯旋门 9 栋 1601）
- 运行时：`375 × 667`，截图 `960 × 1708`
- 结论：10 个业主业务页面的可比业务内容一致；本批次仍不满足 `UI_REVIEWED` 全部门禁。

## 1. 页面结果

| 页面 | 业务区文本 | 预览截图 | 业主截图 |
| --- | --- | --- | --- |
| 我的家 | PASS | `screenshots/preview/home.png` | `screenshots/owner/home.png` |
| 进度 | PASS | `screenshots/preview/progress.png` | `screenshots/owner/progress.png` |
| 我的钱 | PASS | `screenshots/preview/money.png` | `screenshots/owner/money.png` |
| 家档案 | PASS | `screenshots/preview/archive.png` | `screenshots/owner/archive.png` |
| 装修历程 | PASS | `screenshots/preview/journey.png` | `screenshots/owner/journey.png` |
| 报价详情 | PASS | `screenshots/preview/quote.png` | `screenshots/owner/quote.png` |
| 合同详情 | PASS | `screenshots/preview/contract.png` | `screenshots/owner/contract.png` |
| 选材详情 | PASS | `screenshots/preview/selection.png` | `screenshots/owner/selection.png` |
| 材料计划 | PASS | `screenshots/preview/material-plan.png` | `screenshots/owner/material-plan.png` |
| 增减项详情 | PASS | `screenshots/preview/change.png` | `screenshots/owner/change.png` |

机器可读汇总：`parity-results.json`。最终报告由 6 个短批次结果合并，避免微信开发者工具自动化服务在高频截图时自行退出。

## 2. 允许差异

比较范围是业务内容区，已按业务裁决排除：

- 员工预览横幅、水印和返回项目入口；
- 业主本人才可执行的写动作；
- 业主意见与投诉详情。`T12-BIZ-20261021-001` 明确 P0 员工预览不展示该私有内容；
- 亲友分享入口与预览只读说明；
- TabBar 不计入本次业务区文本比较，但页面均正常渲染。

## 3. 本轮修正

- 修复身份切换时 `ownerPreview.active` 残留，确保业主真实会话不再被误当成员工预览。
- 修复 `dist/dev/mp-weixin/app.json` 旧构建缺少业主档案、材料计划、预览入口的问题。
- 修复「提出意见或投诉」被错误计入待办动作的问题；该入口是常驻服务能力，不再污染 `pendingActionCount` 和首页“此刻先看这一件事”。
- 业主首页新增独立的意见与投诉入口；员工预览仍按 P0 隐私边界隐藏反馈详情。

## 4. 业务投影证据

- `08-owner-journey-actions.json`：业主投影 `actions=[]`、`pendingActionCount=0`，包含 `feedback` section。
- `09-preview-actions.json`：员工预览 `actions=[]`、`pendingActionCount=0`、`openFeedbackCount=0`，不包含 `feedback` section，`previewMeta.readOnly=true`。
- `10-preview-action-scan.json`：真实渲染的预览首页与历程均未出现“提出意见或投诉”“生成亲友观看链接”等业主写动作，只读提示正常。

## 5. 尚未完成

- 390×844、430×932（及设计规范中的 375×812）微信运行时检查；
- 空态、接口失败、部分同步、超长内容与极端数值；
- 预览写动作禁用/隐藏的逐项操作验证；
- 真机或体验版验收、独立验收与发布。

因此本批次只能记录为“375 宽度下业务区同屏一致性通过”，不得标记 `UI_REVIEWED`。
