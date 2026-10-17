# B6.3 多供应商拆单价格投影修复

日期：2026-10-17
环境：本地 dev，后端 8091（当前工作树 B6.3）、MySQL `decoboon`、App H5 390×844。
性质：实现者自测，不是 B6.4 独立验收。

## 修复

- 新增 `MaterialSheetPriceProjectionContext`：按采购明细批量聚合作业单行/单价格状态；
- `material/sheet/info|page` 读取时从采购事实重算，不回写；
- 下单/结算/撤销写入后同步冗余投影，覆盖 Priced、Unpriced、Settled、Mixed 组合；
- 修正 `refreshSheetPriceState` 对 Mixed 的判定；
- 不改采购、应付、结算金额公式，不新增表/DDL。

## 实测

- 同一作业单行：3@100（Priced）+ 2（Unpriced）后，sheet/item 均为 `Mixed`；
- 2 到场并按 120 结算后，sheet/item 仍为 `Mixed`，purchasePrice=100、settlementPrice=null；
- 撤销结算后仍为 `Mixed`；
- App H5 390×844 真实页面显示“部分已结算”，无横向溢出，Console Errors 0；
- 截图：`screenshots/b63-app-sheet-mixed-390x844.png`。

## 边界

- 后端定向测试包含投影状态、下单混合、结算/撤销；全量测试见 `state.json` 记录；
- 临时作业单/采购/到场/结算已清理，材料计划恢复 ReadyToOrder，测试账号密码哈希恢复；
- 未执行正式发布；B6.4 独立复验后才能讨论 `ACCEPTED`。
