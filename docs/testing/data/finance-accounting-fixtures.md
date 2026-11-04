# 财务账务专项测试夹具

## FIX-FIN-ORG-01 双企业、多门店、多角色

- 企业 A：总部、门店 A1、门店 A2。
- 企业 B：用于跨租户拒绝。
- 角色：
  - `Boss-A`、`Finance-HQ-A`、`Finance-A1`、`Finance-A2`、`StoreManager-A1`、`Employee-A1/A2`、`Pm-A`、`Designer-A`。
  - `Boss-B`、`Finance-B`。

## FIX-FIN-PAYROLL-01 工资单状态组合

至少 8 名员工，覆盖：

- 有考勤且 Confirmed；
- 无考勤；
- Pending；
- Draft；
- Disputed；
- Rejected；
- InPayroll 且 Draft 工资单；
- InPayroll 且 Paid 工资单。

工资单至少覆盖：

- Draft；Pending；Confirmed；PartialPaid；Paid；Closed；Void（若实现）。

## FIX-FIN-MONEY-01 金额边界

- 0、两位小数、最大允许金额；
- 负向调整；
- 付款大于未付；
- 退款大于有效回款；
- 成本缺失；
- 实发为负；
- 应收/应付已结清和部分结清。

## FIX-FIN-COST-01 成本完整性

- 预计成本完整、实际成本缺失；
- 预计和实际均完整；
- 材料已到货但未结算；
- 人工成本未确认；
- 采购退货、换货、部分到场。

## FIX-FIN-CONCURRENCY-01 并发与幂等

- 同一工资单双击确认；
- 同一员工双击生成工资单；
- 同一应付双击付款；
- 同一收款重复冲正；
- 同一草稿在两个页面同时删除。

所有夹具必须提供重置脚本或数据恢复快照；生产数据不得作为破坏性夹具。
