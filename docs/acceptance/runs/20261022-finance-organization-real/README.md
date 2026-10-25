# PI-20261022-001 真实 App/Web 页面验收证据

- 日期：2026-10-23（Asia/Shanghai）
- 环境：本地 dev；后端 `8090`；隔离 MySQL `3307/decoboon_accept`；Web Vite `8085`；App H5 `8086`
- 数据：现有 dev 验收企业 `验收装企有限公司`（企业 `1001`，总店 `2001`、二店 `2002`）
- 账号：BOSS `13990000001`、总部财务 `13990000005`、二店财务 `13990000006`
- 边界：未访问生产；未修改金额/成本/权限语义；临时财务账号和测试业务数据在采集后清理

## 结论

真实页面验证为 **PASS**：

1. Web 总部财务全部门店视图为 `¥15,000.00`，下钻验收二店为 `¥5,000.00`。
2. Web 硬刷新 `?storeId=2002` 仍为 `¥5,000.00`，修复了首屏门店范围竞态。
3. Web 门店财务锁定验收二店，只显示 `¥5,000.00`，不能切换到全部门店。
4. Web 薪酬工作台在总部财务/二店财务视图均能看到 2026-10 工资单草稿 1 张；该工资单头归属总店、工资项归属二店。
5. Web 主材结算列表请求失败时显示“验收模拟结算列表失败”和“重新加载”，不再伪装空数据。
6. App 总部财务全部门店显示 `1.5万` 与两条流水；切换验收二店显示 `¥5,000` 且仅保留二店流水。
7. App 门店财务锁定验收二店，显示 `¥5,000`，无全部门店切换。
8. App 员工薪酬页在二店视图显示工资单草稿 1 张。
9. App 主材结算失败显示“可结算明细暂不可确认”和“重新加载”。
10. App/Web 同门店同口径金额一致：全部门店 `15,000`，验收二店 `5,000`。

## 截图

- `screenshots/web-hq-all.png`
- `screenshots/web-hq-store2-reload-fixed.png`
- `screenshots/web-store-finance-locked.png`
- `screenshots/web-payroll-store2-sheet-visible.png`
- `screenshots/web-store-finance-payroll-sheet.png`
- `screenshots/web-material-settlement-error.png`
- `screenshots/app-hq-all.png`
- `screenshots/app-hq-store2.png`
- `screenshots/app-store-finance-locked.png`
- `screenshots/app-payroll-store2-sheet-visible.png`
- `screenshots/app-store-finance-payroll-sheet.png`
- `screenshots/app-material-settlement-error.png`

## 数据夹具

- 临时财务账号：`3005`（总部财务，`2001`）、`3006`（二店财务，`2002`）。
- 临时流水：总店收入 `10,000.00`；二店收入 `5,000.00`。
- 临时工资单：表头 `9001` 归属总店；工资项 `9101` 归属二店；周期 `2026-10`。
- 失败态通过 Playwright 仅拦截 `material/settlement/page` 请求模拟 500，其他接口保持真实 dev 响应。

## 证据边界

本组证据证明本轮真实页面、真实登录态和同门店数字一致；不替代生产发布验收，不证明生产存量默认总店数据完全健康。

## 清理确认

采集完成后已删除临时财务账号 `3005/3006`、临时流水 `10,000/5,000`、临时工资单 `9001` 与工资项 `9101`；复查计数均为 0。现有 dev 数据集其他数据未修改。
