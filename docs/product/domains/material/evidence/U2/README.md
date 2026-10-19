# U2 · App/Web 主材下单与结算工作台化

- 日期：2026-10-18
- 性质：UX 回归自测与真实 H5/浏览器渲染证据；不是独立验收
- 范围：
  - App `subpackages/project/pages/material-sheet-order.vue`
  - App `subpackages/finance/pages/material-settlement.vue`
  - Web `views/finance/FinanceMaterialOrderingView.vue`
  - Web `views/finance/FinanceMaterialSettlementView.vue`
- 环境：本地 dev，MySQL `127.0.0.1:3306/decoboon`，后端 `127.0.0.1:8090`，Web `127.0.0.1:8080`，App H5 `127.0.0.1:8081`
- 账号：Finance `19900000002`（本地测试密码）
- 数据：U2 作业单 `TILE-20261018-001` 走查后已 cancel → delete；结算候选使用本地既有 3 行演示采购明细

## 1. 页面契约

| 页面 | 首屏第一问 | 首屏第二问 | 唯一主行动 |
| --- | --- | --- | --- |
| App/Web 主材下单 | 待下单几张、可下单几行 | 供应商是否已选、几行未定价 | 确认下单 |
| App/Web 主材结算 | 可结算几行、几家供应商 | 未定价几行、待填结算价几行 | 结算本单（月结为次要） |

规则说明统一折叠到“为什么单价可留空？”/“结算规则”，不占首屏。

## 2. 改动

### App 主材下单

- 新增“本次下单”摘要：行数、供应商、未定价行数；
- 新增“为什么单价可留空？”折叠说明；
- 保留供应商、交付方式、要求到场、备注、行级数量/单价输入；
- 底部保留唯一主按钮“确认下单”。

### App 主材结算

- 页头改为“先填结算价，再按采购单或供应商确认”；
- 移除首屏大段结算规则，改为“可结算/未定价/待填结算价/供应商数”摘要；
- 新增“结算规则”折叠；
- “结算本单”提升为主按钮，“月结”保持次要动作。

### Web 主材下单

- 页头改为短句：先选作业单，再填供应商、数量和单价；
- 新增“待下单张数/可下单行数/已进入采购张数”概览；
- 待下单行增加显式“去下单”按钮，不再只靠整行点击；
- 选中作业单后显示“可下单行数/未定价行数”摘要；
- 长规则说明折叠到“为什么单价可留空？”。

### Web 主材结算

- 页头移除长规则，改为短句；
- 新增“行可结算/行未定价/行待填结算价/供应商数”摘要；
- 长规则折叠到“结算规则”；
- “结算本单”改为主按钮，“月结该供应商”保持次要动作。

## 3. 真实渲染结果

| 场景 | 结果 | 证据 |
| --- | --- | --- |
| App 下单 390×844 | PASS：摘要 1 行、未定价 1 行、帮助折叠、主按钮“确认下单” | `screenshots/u2-app-order-390x844.png` |
| App 下单 375×667 | PASS：无横向溢出 | `screenshots/u2-app-order-375x667.png` |
| App 结算 390×844 | PASS：可结算 3 行/未定价 0 行/待填 0 行/1 家供应商；规则折叠 | `screenshots/u2-app-settlement-390x844.png` |
| App 结算 375×667 | PASS：无横向溢出 | `screenshots/u2-app-settlement-375x667.png` |
| Web 下单 1440×900 | PASS：概览 1 张待下单/1 行可下单；行级“去下单” | `screenshots/u2-web-ordering-1440x900.png` |
| Web 下单选中 1440×900 | PASS：1 行可下单/1 行未定价；帮助折叠；主按钮“确认下单” | `screenshots/u2-web-ordering-selected-1440x900.png` |
| Web 下单 1024×768 | PASS：无横向溢出 | `screenshots/u2-web-ordering-1024x768.png` |
| Web 下单选中 1024×768 | PASS：无横向溢出 | `screenshots/u2-web-ordering-selected-1024x768.png` |
| Web 结算 1440×900 | PASS：3 行可结算/0 行未定价/1 家供应商；规则折叠 | `screenshots/u2-web-settlement-1440x900.png` |
| Web 结算 1024×768 | PASS：无横向溢出 | `screenshots/u2-web-settlement-1024x768.png` |

## 4. 指标

- App 390×844：`scrollWidth = innerWidth = 390`；下单页高 888，结算页高 958；
- App 375×667：`scrollWidth = innerWidth = 375`；下单页高 850，结算页高 927；
- Web 1440×900：`scrollWidth = innerWidth = 1440`；下单选中页高 1164，结算页高 993；
- Web 1024×768：`scrollWidth = innerWidth = 1024`；下单选中页高 1164，结算页高 1023；
- 业务 Console Errors：0；
- 首屏不再出现“同一采购明细只能结算一次/逐单优先、月结兜底”等长段规则。

## 5. 命令

App：

```bash
npm run type-check
npm run check:ui
npm run check:subpackages
npm run build:mp-weixin
```

Web：

```bash
npm run type-check
npm run build
```

全部 PASS。

## 6. 未闭环

- U3：老板项目材料风险页、助手技术语言、蓝图入口降级未执行；
- U4：真实设计师/财务/PM/老板的无培训可用性走查未执行；
- 本轮为真实 H5/浏览器自测，不等同于独立验收或真机验收。
