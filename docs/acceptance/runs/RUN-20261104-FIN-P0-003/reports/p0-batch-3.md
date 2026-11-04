# RUN-20261104-FIN-P0-003 · 财务跨域 P0

## 结果

`PASS=2 / FAIL=0 / BLOCKED=2 / NOT_RUN=0`

| 用例 | 结果 | 关键证据 |
| --- | --- | --- |
| 跨租户隔离 | BLOCKED | API/DB通过；页面证据待补 |
| 应收、收款、退款、冲正 | BLOCKED | API/DB通过；页面证据待补 |
| 材料、应付、付款、成本 | PASS | 材料采购单、应付1440、付款1440、材料成本1440 |
| 成本不完整与结算阻断 | PASS | costComplete=false，利润为空，确认结算按钮disabled |
