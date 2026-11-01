# 2026-10-30 员工发放跨端上下文与幂等对齐

## 范围

- 变更记录：`CHG-20261030-003`
- 对象：员工工资/提成应付、员工发放、资金台账
- 非范围：工资公式、应付生成、角色权限、作废/退回、批量/部分发放、DDL

## 实现结论

1. Web 从工资单详情打开员工发放时，只加载当前工资单产生的 `PayrollItem` 应付。
2. Web 发放抽屉接收当前门店范围，不再只依赖客户端类型过滤。
3. Web 发放不再自动选中第一条应付，必须由用户明确选择。
4. App 发放补齐发放日期和备注输入，日期使用本地日期。
5. App 在失败重试时复用 `paymentIntentId`；只有切换应付、账户、日期或备注后才生成新意图。
6. 两端继续复用同一 Provider：`finance/payment/employee/page|save`。

## 验证

- 后端定向：`PayrollEmployeePaymentPageScopeTest`、`PayrollEmployeePaymentAccessTest` 2/2 PASS
- Java 22 全量：1130 tests / 0 failures / 0 errors / 0 skipped
- App payroll contract PASS
- App type-check PASS
- App `build:mp-weixin` PASS
- Web type-check PASS
- Web production build 与分页/文案/编号/品牌/壳层门禁 PASS

App 原始精简验证：`app-verification.log`  
Web 构建输出摘要：`frontend-verification.txt`

## 未覆盖

- 真实 Finance/BOSS 账号在 Web 工资单详情完成员工发放
- 真实 Finance/BOSS 账号在 App 完成发放并验证资金台账/工资条回读
- 独立验收和发布

## 下一步

使用受控测试企业完成 Web 和 App 同一员工应付发放，核对应付、付款、台账、工资条和幂等重试。
