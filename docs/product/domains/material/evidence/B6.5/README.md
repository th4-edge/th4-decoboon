# B6.5 Web 主材下单 Mixed 状态展示修复

日期：2026-10-17
环境：本地 dev 后端 8091、App H5 8080、Web 4175、MySQL `decoboon`。
性质：实现者自测，不是 B6.6 独立验收。

## 修复

- Web `FinanceMaterialOrderingView` 新增 `priceStateLabel` 映射；
- 待下单/已进入采购行显示服务端 `sheet.priceState`：
  - `Priced` → 已定价
  - `Unpriced` → 未定价 · 成本待结算
  - `Mixed` → 部分已结算 · 成本待结算
  - `Settled` → 已结算
- 增加价格状态 pill；不在前端推导价格或成本；
- 未改后端、权限、金额/成本公式，不做 DDL。

## 验证

- `npm run type-check`、`npm run build` PASS；
- 复用 B6.4 黑盒脚本（仅将输出指向 B6.5）：140/140 PASS；
- Web `/finance/material-ordering` 1440×900：目标 Mixed 行显示“部分已结算 · 成本待结算”；`innerWidth=scrollWidth=1440`，Console Errors 0；
- App H5 / Web 暂估应付与旧路线回归继续 PASS；
- 证据：`ui-metrics.json`、`verification-results.json`、`summary.json`、`screenshots/`。
