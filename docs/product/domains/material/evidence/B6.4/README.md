# 材料域 B6.4 · B62-DEF-001 聚焦独立复验

- 日期：2026-10-17
- 性质：独立黑盒复验与证据采集，不是 B6.3 实现者自测。
- 环境：后端精确提交 `b66a33b`（8091）、App `46a5be4`（H5 8080）、Web `04ba3b4`（4175）、MySQL `127.0.0.1:3306/decoboon`。
- 数据范围：本地 dev，企业 M `com_id=999200000000000001`；未访问生产。
- 结论：**FAIL**（139 / 140 PASS，1 个 P2 UI 缺陷开放）。
- B62-DEF-001：**CLOSED**，后端 23/23 聚焦检查通过。

## 1. 独立性与边界

- 未修改实现代码、`state.json`、`08-acceptance.md`、`10-reconciliation.md` 或 B6/B6.1/B6.2/B6.3 证据。
- 只在本目录写证据；临时供应商、作业单、采购、到场、结算、应付全部清理。
- 材料计划数量、状态、选品/到场、价格/成本逐列恢复；临时项目经理恢复为原值 `NULL`。
- 未执行 DDL/迁移、生产部署、正式小程序提审或发布；未记录密码、哈希、密钥或真实令牌。

## 2. 分类结果

| 类别 | PASS | FAIL | 合计 |
| --- | ---: | ---: | ---: |
| 会话与实时数据前置 | 9 | 0 | 9 |
| B6 主链回归 | 7 | 0 | 7 |
| B6.4 聚焦（B62-DEF-001） | 23 | 0 | 23 |
| 幂等/重复 | 4 | 0 | 4 |
| 金额/成本/到场边界 | 5 | 0 | 5 |
| 角色权限 | 15 | 0 | 15 |
| 跨企业 | 7 | 0 | 7 |
| 旧路线 | 4 | 0 | 4 |
| 材料助手 | 52 | 0 | 52 |
| App/Web UI | 9 | 1 | 10 |
| 清理与恢复 | 4 | 0 | 4 |
| **合计** | **139** | **1** | **140** |

材料助手 52 项包含 50 个黄金问题、1 个越界/无证据拒答、1 个 Designer 成本不可见检查；50 个黄金问题 **50/50 PASS**。

## 3. B62-DEF-001 逐项结果

测试单据：同一作业单行 8㎡，先给供应商 A 下单 `3 @ 100`，再给临时供应商 B 下单 `5` 且价格为空；随后供应商 A 部分到场 2/3、供应商 B 全部到场 5/5，再月结无价部分 `5 @ 120`，最后撤销结算。

| 检查 | 期望 | 实测 | 结果 |
| --- | --- | --- | --- |
| 两次不同供应商下单后 `material/sheet/info` | sheet/item `Mixed` | `Mixed/Mixed` | PASS |
| 同一时点 `material/sheet/page` | sheet/item `Mixed` | `Mixed/Mixed` | PASS |
| 采购事实 | Priced + Unpriced | 3@100 `Priced`，5 `Unpriced` | PASS |
| 部分到场（2/3 + 5/5） | 仍 `Mixed` | `PartiallyArrived + Mixed/Mixed` | PASS |
| 无价部分结算 5@120 后 `info` | 不得回退 `Unpriced`，应为 `Mixed` | `Mixed/Mixed` | PASS |
| 结算后 `page` | `Mixed` | `Mixed/Mixed` | PASS |
| 结算后采购明细 | Priced + Settled(120) | `Priced` + `Settled/120` | PASS |
| 全部到场后 | `Arrived + Mixed` | `Arrived + Mixed/Mixed` | PASS |
| 撤销结算后 `info` | 仍 `Mixed` | `Mixed/Mixed` | PASS |
| 撤销结算后 `page` | 仍 `Mixed` | `Mixed/Mixed` | PASS |
| 撤销后采购明细 | Priced + Unpriced | `Priced` + `Unpriced(null)` | PASS |
| 全有价投影 | `Priced/Priced` | `Priced/Priced` | PASS |
| 全无价投影 | `Unpriced/Unpriced` | `Unpriced/Unpriced` | PASS |
| 全部 Settled（同价 100） | `Settled/Settled` + `settlementPrice=100` | 符合 | PASS |

结论：**B62-DEF-001 多供应商拆单价格投影已关闭**；`material/sheet/info|page` 与采购明细事实同源。

## 4. 回归结果

### 主链与金额

- Designer 建单 → PM 测量/到场 → Designer 提交 → Finance 下单/结算/撤销主链通过。
- 未测量提交请购被拦截。
- 有价采购 3@100 金额 300；无价采购形成暂估应付；无价部分结算 5@120 金额 600。
- 未到场 preview 为空且 confirm 被拒；撤销结算后重新进入可结算集。
- 成本不完整时 `costComplete=false`，响应不输出伪利润；无价应付 API 暴露 `estimated=true`，Web/App 不把 0 元当成确定应付。

### 权限与跨企业

- Designer/PM：作业单/采购商业字段裁剪通过；结算 `page|info` 403；下单/结算确认 403。
- Boss/StoreManager/Finance：作业单与结算读权限保留。
- 跨企业 Boss：作业单/材料体检/助手 404；采购/结算/应付无企业 M 目标数据；供应商列表无临时供应商。

### 幂等、旧路线与助手

- 同幂等键同载荷重复下单只生成 1 张采购/1 条明细/1 条应付；同键异载荷 409。
- 已结算明细重复 confirm 被拒，preview 为空。
- `project/purchase/save|batch` 均 `canNotFoundHandler`；历史采购仍可查；材料计划工作台不再提供计划直采动作；App 旧直采路由空白。
- 50/50 黄金问题意图、证据/拒答、只读、审计状态通过；助手幂等返回同一 auditId；越界问题 REFUSAL；Designer 回答无成本金额。

## 5. 真实 UI 结果

### Web 1440×900

- `/finance/payables`：暂估行显示“待结算”，不显示 `¥0.00`；“安排付款”按钮 disabled；`innerWidth=scrollWidth=bodyScrollWidth=1440`，Console Errors 0。PASS。
- `/finance/material-ordering`：目标 Mixed 作业单行显示为 `瓷砖… · TILE-20261017-001 / 王芳… · 1 行 / 已到场`，**没有**显示“混合/部分已结算/成本待结算”；无横向溢出、Console Errors 0。

### App H5 390×844

- 作业单详情：显示“部分已结算”“已到场”和暂估/成本不完整说明；`innerWidth=scrollWidth=bodyScrollWidth=390`，Console Errors 0。PASS。
- 应付页：显示“待定价/暂估/待定价/待结算”；无提前付款按钮；无横向溢出，Console Errors 0。PASS。
- 旧直采路由：页面正文为空、无横向溢出。PASS。

## 6. 新缺陷

### B64-UI-WEB-SHEET-01（P2，OPEN）

- 问题：Web 主材下单页“已进入采购”的 Mixed 作业单行只显示履约状态“已到场”，未展示价格状态/成本待结算。
- 后端事实：同一单据 `priceState=Mixed`，App H5 正确显示“部分已结算”。
- 影响：Web 财务入口可能把“已到场”误读为价格/成本已收敛，尤其是多供应商拆单、部分结算后。
- 证据：`verification-results.json#B64-UI-WEB-SHEET-01`、`ui-metrics.json#web.ordering`、`screenshots/b64-web-material-ordering-mixed-1440x900.png`。
- 复现：执行本目录脚本重新造出 Priced 3@100 + Unpriced 5、部分到场、5@120 结算并撤销后，访问 Web `/finance/material-ordering` 读取目标行 DOM 文本。

## 7. 清理与证据

- 业务计数恢复：作业单 0、作业单明细 0、采购 18、采购明细 18、到场 18、结算 0、结算明细 0、应付 37、应付核销 0、供应商 2。
- 材料计划恢复 `ReadyToOrder`，quantity/net=5、unitPrice=88、cost=310、selectionId/arrivedAt=NULL。
- 项目临时 PM 恢复 `NULL`，临时供应商删除。
- 助手审计账本为追加式，保留本次黄金问题审计记录；未保存问题/回答正文，未保留密码、哈希、密钥或令牌。

证据文件：

- `summary.json`：机器可读结论、分类计数、B62 聚焦结果、缺陷与清理结果。
- `verification-results.json`：140 项逐条检查与实测值。
- `api-samples.json`：关键请求/响应摘要与清理前后计数。
- `ui-metrics.json`：Web/App 文本、按钮、viewport、横向溢出与 Console 指标。
- `screenshots/`：5 张真实渲染截图。
- `run-b64-verification.mjs`：可复跑的独立黑盒脚本；凭据只从环境变量读取。

## 8. 最终判断

- 独立验收结论：**FAIL**。
- B62-DEF-001：**CLOSED**。
- 是否满足进入用户最终 `ACCEPTED` 决策的独立证据条件：**否**；需先处置 `B64-UI-WEB-SHEET-01`（P2），再由用户决定是否接受该风险。
- 未闭环风险：Web Mixed 作业单行的成本状态可解释性；单店 StoreManager 账号无法独立验证同企业跨门店负向；50 个黄金问题审计行按追加账本保留。
