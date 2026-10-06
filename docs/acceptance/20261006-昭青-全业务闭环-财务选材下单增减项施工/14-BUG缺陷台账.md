# BUG / 缺陷台账（2026-10-06 全角色闭环轮次）

统一编号 `XA-YYYYMMDD-NNN`。状态：OPEN / FIXING / FIXED_PENDING_VERIFY / FIXED_VERIFIED / BLOCKED / WONTFIX。

| ID | 类别 | 严重度 | 状态 | 标题 | 页面/位置 | 最小复现 | 期望 | 实际 | 证据 | 修复 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| XA-20261006-001 | SEC/环境 | P1 | FIXED_PENDING_VERIFY | dev 构建产物 API 指向生产域名 | `dist/dev/mp-weixin/config/index.js` | 打开 `dist/dev/mp-weixin`，读取 `runtime.apiBase` | 本地 dev 构建应指向 `http://127.0.0.1:8090/api/` | 产物为 `https://decoboon.joytsing.com/api/`，dev 页面实际请求生产后端 | `raw/preflight.json`、本文件 §1 | 已用 `.env` 重建 `dist/dev`（现为 127.0.0.1:8090）；建议把产物校验并入 `verify-dev-api-base.mjs` |
| XA-20261006-002 | BUG/兼容 | P2 | FIXED_PENDING_VERIFY | iOS 日期解析失效导致跟进/到期判断失真 | `pages/customer/list.vue` followMeta/ageDays；`subpackages/team/pages/workbench.vue` taskDue/materialDue | 客户有 `nextFollowAt="2026-09-29 00:00:00"` 时进入客户列表 | 正确显示「逾期 N 天 / 今日跟进 / N 天后」 | `new Date('YYYY-MM-DD HH:mm:ss')` 在 iOS 为 Invalid Date，天数为 NaN，逾期与今日统计失效 | `raw/console/console-and-exceptions.json`（435 条 iOS 日期告警） | 改用 `@/core/format` 的 `diffDays`（内部 `YYYY/MM/DD` 归一化）+ 安全解析 `ageDays`；`vue-tsc` PASS；复测日期告警 435→0 |
| XA-20261006-003 | UX/BUG | P3 | OPEN | 缺少必填参数深链打开项目页直接报错 | `subpackages/project/pages/{plan,detail,report/daily}` | 无参 `reLaunch` 到项目相关页 | 提示「请选择项目」并返回/引导 | 后端返回「projectId不能为空 / 项目索引不能为空」，页面显示加载失败 | 后端 `logs/log_error.log` 2026-10-06 04:13–04:19 多条 | 建议前端 onLoad 缺参时走空态引导，不下发必填缺失请求 |
| XA-20261006-004 | DOC/兼容 | P3 | OPEN | 使用已废弃 API `wx.getSystemInfoSync` | 全局（`system` composable 等） | 进入任意页 | 使用 `wx.getWindowInfo/getDeviceInfo/getAppBaseInfo` | 控制台持续告警 | `raw/verify-date-fix.json` | 低风险，纳入后续统一升级 |
| XA-20261006-005 | DATA/BIZ | P2 | OPEN | 客户来源取值静默落为「其他」 | `customer/customer/save` | 传 `source=Walkin` 保存客户 | 保存为「自然到店」 | 实际保存为 `Other`（其他） | API 探针返回 `source:{key:"Other"}` | 需核对前端 `sourceMap` 与后端 `CustomerSource` 枚举名是否一致——疑似枚举名不匹配被静默兜底 |

## §1 证据与影响（XA-20261006-001）

- 目录 `dist/dev/mp-weixin/config/index.js` 中 `environment:"development"`、`isProduction:false`，但 `apiBase` 为生产 HTTPS 域名——名称与行为矛盾，极易让人误以为在用本地环境。
- 影响：任何基于该产物的「本地测试」都会真实读写生产数据，属于高风险测试安全问题。
- 处置：重建 dev 产物后，客户列表等页面确认请求本地 127.0.0.1:8090，UI 数据与本地 `decoboon` 一致。

## 未修复/待确认

- XA-20261006-005 需产品/后端确认来源枚举口径后修复。
- XA-20261006-002 需真机 iOS 复验（DevTools 只能证明告警消失）。
