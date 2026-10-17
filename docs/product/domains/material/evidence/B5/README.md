# B5 App 单端材料下单与结算 UI 走查证据

日期：2026-10-17

环境：

- 微信开发者工具 2.02.2609231，项目根 `th4-decoboon-app/dist/dev/mp-weixin`；
- DevTools 系统窗口 375×667，页面 `page.size()` 为 375×599；
- H5 390×844；
- 真实本地 dev 后端 8090 与造数企业 M 数据；
- Boss 账号临时登录，验收后密码哈希恢复。

## 缺陷与修复

走查首先发现 DevTools 编译错误：

`subpackages/project/pages/material-plan-workbench.wxml` 出现多余双引号，来源为
`material-plan-workbench.vue` 中 `mpw__item` 节点的孤立 `"`。

已做单行模板修复并重新构建；修复后 DevTools 模拟器正常编译、automator 可连接并读取页面。

## 真实页面证据

| 页面 | DevTools 截图 | H5 截图 | 结果 |
| --- | --- | --- | --- |
| 主材下单工作台 | `screenshots/b5-workbench-submitted.png` | `screenshots/b5-h5-workbench-submitted.png` | PASS：待财务下单作业单、去下单与结算入口 |
| 作业单下单 | `screenshots/b5-material-sheet-order.png` | `screenshots/b5-h5-material-sheet-order.png` | PASS：供应商、数量、可空单价、未定价提示 |
| 作业单详情 | `screenshots/b5-material-sheet-detail-submitted.png` | `screenshots/b5-h5-material-sheet-detail.png` | PASS：待下单、去下单、时间与模板说明 |
| 主材结算 | `screenshots/b5-material-settlement.png` | `screenshots/b5-h5-material-settlement.png` | PASS：待结算、逐单/月结、结算价输入 |
| 结算单空态 | `screenshots/b5-material-settlement-settlements-tab.png` | — | PASS：切换到“结算单”后明确空态 |
| 材料计划工作台 | `screenshots/b5-material-plan-workbench.png` | `screenshots/b5-h5-material-plan-workbench.png` | PASS：旧直采入口移除，保留主材作业单/结算入口 |
| 供应商协作 | `screenshots/b5-supply-index.png` | `screenshots/b5-h5-supply-index.png` | PASS：旧直采入口移除，保留历史采购与到场 |

## 交互

- 主材工作台 `去下单` 按钮实际 tap 后进入 `subpackages/project/pages/material-sheet-order`；
- 材料结算页“结算单” tab 实际 tap 后显示“还没有结算单”空态；
- 作业单下单页显示“未定价”“采购单价 可留空=未定价”“1 行未定价 · 成本待结算”；
- 材料计划工作台显示“下单统一从主材作业单发起”；
- 供应商协作页显示“主材下单工作台 / 材料计划工作台 / 主材结算”，无旧材料计划直采入口。

## 布局与 Console

- DevTools `page.size()`：工作台/下单/详情/结算/供应商 375×599；材料计划工作台 375×667；均无横向溢出；
- H5 六页 `document.documentElement.scrollWidth = innerWidth = 390`；
- H5 业务页面 Console Errors = 0；仅 `favicon.ico` 404，属非业务资源；
- DevTools 模拟器编译成功，无页面编译错误。

## 清理

- 临时作业单 `TILE-20261017-001` 已取消并删除；
- Boss/Designer/PM 本地测试密码哈希已恢复；
- 未执行正式小程序提审或发布。
