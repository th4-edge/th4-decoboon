# 材料域 B6.6 · Web Mixed 状态聚焦独立复验

- 日期：2026-10-17
- 性质：独立黑盒与真实浏览器复验；不是 B6.5 实现者自测，不自行批准 `ACCEPTED`。
- 结论：**PASS**（157 / 157，0 FAIL，0 BLOCKED）。
- 环境：后端精确提交 `b66a33b`（8091）、App 精确提交 `46a5be4`（H5 8080）、Web 精确提交 `3ef4974`（4175）、MySQL `127.0.0.1:3306/decoboon`。
- 数据范围：仅本地 dev 企业 M；临时供应商、作业单、采购、到场、结算、应付全部清理，计划/供应商/项目经理恢复。

## 1. 独立性与边界

- 本任务只做黑盒 Provider 验证与真实浏览器验证，未修改任何实现代码。
- 只在本目录写入证据；未修改 `state.json`、`08-acceptance.md`、`10-reconciliation.md` 或 B6–B6.5 证据。
- 未执行 DDL/迁移、生产部署、正式小程序提审或发布。
- 未记录密码、密码哈希、密钥或真实令牌；账号只按角色记录。
- 首轮 Web 首次冷加载遇到 Vite `504 (Outdated Optimize Dep)`，同一问题仅重试 1 次（共 2 次），第二轮全部通过；该瞬态不是产品代码缺陷，记录于 `attempts.json`。

## 2. 必验结论

### 2.1 Web 1440×900 `/finance/material-ordering`

同一作业单行 8㎡ 的真实流程：

1. 供应商 A 下单 `Priced 3 @ 100`；
2. 临时供应商 B 下单 `Unpriced 5`；
3. 供应商 A 部分到场 `2/3`，供应商 B 全部到场 `5/5`；
4. 月结无价部分 `5 @ 120`（结算金额 600）；
5. 供应商 A 剩余 `1/3` 到场；
6. 撤销结算。

结果：

| 阶段 | 服务端 `priceState` | 页面文案 | 视觉状态 | 结果 |
| --- | --- | --- | --- | --- |
| 全有价 | `Priced` | `已定价` | `mo__price-state--priced`，绿色 pill | PASS |
| 全无价 | `Unpriced` | `未定价 · 成本待结算` | `mo__price-state--unpriced`，琥珀色 pill | PASS |
| 全部结算 | `Settled` | `已结算` | `mo__price-state--settled`，绿色 pill | PASS |
| 结算后 Mixed | `Mixed`，履约 `PartiallyArrived` | `部分已结算 · 成本待结算` | `mo__price-state--mixed`，琥珀色 pill | PASS |
| 撤销后 Mixed | `Mixed`，履约 `Arrived` | `部分已结算 · 成本待结算` + `已到场` | `mo__price-state--mixed`，琥珀色 pill | PASS |

关键点：

- Mixed 行同时显示价格状态和履约状态，不是只显示“已到场”。
- 页面文案、pill class、computed color/background 与 `material/sheet/page` API / 页面 XHR 的 `priceState` 全部一致。
- Web 源码审计：`priceState` 仅从服务端 `raw.priceState` 经 `SHEET_PRICE_STATE` 展示映射；未发现按 `purchasePrice` / `settlementPrice` 在前端推导价格状态。审计文件与 SHA-256 见 `environment.json`。
- 所有 Web 截图均为真实 `1440×900`；`innerWidth = scrollWidth = bodyScrollWidth = 1440`；Console Errors = 0。

### 2.2 B62-DEF-001 回归

23 / 23 聚焦检查 PASS：

- `sheet/info` 与 `sheet/page`：Priced、Unpriced、Mixed、Settled 四态同源；
- 不同供应商拆单后 `Mixed/Mixed`，`orderedQuantity=8`；
- 采购事实：`Priced` 3@100 + `Unpriced` 5；
- 部分到场 `PartiallyArrived + Mixed`；
- 无价部分结算 `5@120` 后仍 `Mixed`，不回退 `Unpriced`；
- 全部到场 `Arrived + Mixed`；
- 撤销结算后仍 `Mixed`，采购明细回到 `Priced + Unpriced`；
- 全有价、全无价、全部 Settled 三组基线投影均正确。

B62-DEF-001 状态：**CLOSED**。

### 2.3 其他回归

| 类别 | PASS | FAIL | 说明 |
| --- | ---: | ---: | --- |
| 会话与实时数据前置 | 9 | 0 | 六角色登录/会话、ReadyToOrder 计划、供应商、临时第二供应商 |
| B6 主链 | 7 | 0 | 未测量提交拦截、结算 preview/confirm、撤销后重新可结算 |
| B6.6 聚焦 | 23 | 0 | B62-DEF-001 全量聚焦 |
| UI | 27 | 0 | Web/App 文案、pill、DOM 大小、Console、横向溢出、截图 |
| 幂等/重复 | 4 | 0 | 同键同载荷、同键异载荷 409、重复结算拒绝、助手幂等 |
| 金额/成本/到场边界 | 5 | 0 | 未到场 preview/confirm 拒绝、暂估应付、成本不完整利润 null |
| 角色权限 | 15 | 0 | Designer/PM 裁剪与 403；Boss/StoreManager/Finance 保留权限 |
| 跨企业 | 7 | 0 | 作业单/采购/结算/应付/供应商/体检/助手均无 M 数据 |
| 旧路线 | 4 | 0 | `project/purchase/save|batch` 不存在；历史采购可查；无计划直采动作 |
| 材料助手 | 52 | 0 | 50/50 黄金问题 + 越界拒答 + Designer 成本不可见 |
| 清理与恢复 | 4 | 0 | 业务表、供应商、计划逐列、项目经理恢复 |
| **合计** | **157** | **0** | |

补充：

- 50 个黄金问题 **50/50 PASS**，均返回预期意图、证据或明确拒答、`readOnly=true`、审计 `RECORDED`；
- 未到场结算：preview 为空，confirm 返回“尚未到场”；
- 暂估应付：无价应付 `estimated=true`，金额不伪装为 0 元确定应付；Web 显示“待结算”且“安排付款”禁用；App 显示“待定价/待结算”且无提前付款按钮；
- 旧路线：旧直采 Provider 均返回 `canNotFoundHandler`；App 旧路由空白且无横向溢出；
- 跨企业 Boss：作业单/体检/助手 404；采购/结算/应付/供应商无 M 企业目标数据。

### 2.4 App H5 390×844

- 作业单详情显示 `部分已结算`、`已到场`、暂估/成本不完整说明；
- 应付页显示 `待定价` / `待结算`，无提前付款按钮；
- 旧直采路由不可达/空白；
- 三个页面 `innerWidth = scrollWidth = bodyScrollWidth = 390`，Console Errors = 0；
- 截图尺寸经 `sips` 验证为 `390×844`。

## 3. DOM 大小与截图

`ui-metrics.json` 记录：

- Web 各状态页 `innerWidth=1440`、`innerHeight=900`、`scrollWidth=1440`、`bodyScrollWidth=1440`；
- Web DOM 节点数约 221，HTML 长度约 146,975–146,987 字符；
- App 作业单页 DOM 节点数 214，HTML 长度 155,422 字符；
- App 应付页 DOM 节点数 245，HTML 长度 171,140 字符；
- 所有正常业务页面 Console Errors = 0。

截图（均在本目录 `screenshots/`）：

- `b66-web-ordering-priced-1440x900.png`
- `b66-web-ordering-unpriced-1440x900.png`
- `b66-web-ordering-settled-1440x900.png`
- `b66-web-ordering-mixed-settled-1440x900.png`
- `b66-web-material-ordering-mixed-postcancel-1440x900.png`
- `b66-web-payables-estimated-1440x900.png`
- `b66-app-sheet-mixed-390x844.png`
- `b66-app-payables-estimated-390x844.png`
- `b66-app-old-route-unreachable-390x844.png`

## 4. 清理与恢复

清理前后计数一致：

| 表 | 清理前 | 清理后 |
| --- | ---: | ---: |
| `db_material_order_sheet` | 0 | 0 |
| `db_material_order_sheet_item` | 0 | 0 |
| `db_purchase_order` | 18 | 18 |
| `db_purchase_order_item` | 18 | 18 |
| `db_goods_receipt` | 18 | 18 |
| `db_material_settlement` | 0 | 0 |
| `db_material_settlement_item` | 0 | 0 |
| `db_finance_payable` | 37 | 37 |
| `db_partner` | 2 | 2 |

材料计划恢复 `ReadyToOrder`，`quantity/net=5`、`unitPrice=88`、`cost=310`、`selectionId/arrivedAt=NULL`；项目 `manager_id` 恢复原值 `NULL`；临时供应商已删除。独立复跑 SQL 复核同样得到上述结果。

## 5. 缺陷与风险

### 已关闭

- `B62-DEF-001`（P2，多供应商价格投影）：**CLOSED**，23/23 聚焦检查 PASS。
- `B64-UI-WEB-SHEET-01`（P2，Web Mixed 展示）：**CLOSED**，Web 1440×900 文案/pill/服务端状态/无溢出/Console 均 PASS。

### 本轮新缺陷

- 无产品缺陷。

### 环境瞬态（非产品缺陷）

- `ENV-B66-001`（P3，环境）：首轮 Web 冷加载出现 Vite `504 (Outdated Optimize Dep)`，导致首个 Priced 页 Console 非空；同一页面行文案/pill/API 检查仍通过。仅重试 1 次后全部 PASS。记录于 `attempts.json`。

### 未闭环风险/限制

- 单店 StoreManager 账号无法独立验证同企业跨门店负向；已覆盖跨企业/越权范围；
- 助手审计账本为追加式，黄金问题审计行保留；不保存问题/回答正文；
- H5 浏览器验证不等同于微信开发者工具/真机验证；本批未做正式小程序发布；
- `GAP-MAT-TIME-SEMANTICS` 仍为领域未决问题，与本批状态展示无关。

## 6. 是否满足进入用户最终 ACCEPTED 决策的独立证据条件

**是。** 本轮独立证据满足进入用户最终 `ACCEPTED` 决策的条件：

- B62-DEF-001 与 B64-UI-WEB-SHEET-01 均已独立关闭；
- Web 1440×900 的 Priced/Unpriced/Mixed/Settled 文案、视觉状态、服务端状态、DOM 尺寸、Console 与截图证据齐全；
- 主链、权限、幂等、到场门禁、暂估应付、50 黄金问题、旧路线、跨企业、App H5/Web UI 回归全部 PASS；
- 临时数据已清理，计划/供应商/项目经理已恢复。

但本复验**不自行批准** `ACCEPTED`；最终是否接受由用户决定。

## 7. 证据文件

| 文件 | 内容 |
| --- | --- |
| `summary.json` | 机器可读结论、分类计数、Web Mixed 逐态结果、缺陷状态、清理结果 |
| `verification-results.json` | 157 项逐条检查、期望/实测/证据 |
| `api-samples.json` | 关键 Provider 请求/响应摘要、主链事实、清理前后计数 |
| `ui-metrics.json` | Web/App DOM 文本、尺寸、pill 样式、Console、截图索引 |
| `environment.json` | 精确提交、端口、数据库、Web 源码审计与文件 SHA-256 |
| `attempts.json` | 两次尝试记录（首轮 Vite 504 瞬态、次轮全 PASS） |
| `run-b66-verification.mjs` | 可复跑独立黑盒脚本；凭据只从环境变量读取 |
| `screenshots/` | 9 张真实渲染截图（Web 1440×900，App 390×844） |

## 8. 实际命令摘要

```bash
# 精确提交与进程
git -C /tmp/th4-b66-backend-b66a33b rev-parse HEAD          # b66a33b
git -C /tmp/th4-b66-app-46a5be4 rev-parse HEAD              # 46a5be4
git -C /tmp/th4-b66-web-3ef4974 rev-parse HEAD              # 3ef4974
lsof -nP -iTCP:8091 -sTCP:LISTEN
lsof -nP -iTCP:8080 -sTCP:LISTEN
lsof -nP -iTCP:4175 -sTCP:LISTEN

# 登录与实时 Provider
curl -X POST http://127.0.0.1:8091/api/auth/session/password-login \
  -H 'Content-Type: application/json' -d '{"phone":"<Boss>","password":"<env>"}'

# 独立黑盒 + 真实浏览器（凭据/数据库口令仅通过环境变量传入，不落证据）
B66_PASSWORD='<env>' B66_DB_PASSWORD='<env>' \
B66_PHONE_BOSS='<env>' B66_PHONE_STORE='<env>' B66_PHONE_DESIGNER='<env>' \
B66_PHONE_PM='<env>' B66_PHONE_FINANCE='<env>' B66_PHONE_CROSS='<env>' \
node docs/product/domains/material/evidence/B6.6/run-b66-verification.mjs

# 独立清理复核
mysql -h127.0.0.1 -P3306 -uroot -p'<env>' -N -B decoboon -e \
  "SELECT COUNT(*) FROM db_material_order_sheet; SELECT ... FROM db_material_plan WHERE plan_id=235566303270666240;"
```

## 9. 最终判断

- 独立复验结论：**PASS**。
- 类别结果：157 PASS / 0 FAIL / 0 BLOCKED。
- 支持进入用户最终 `ACCEPTED` 决策：**是**（最终决定权在用户）。
- 不宣称：正式小程序发布、生产发布、全品类材料门禁。
