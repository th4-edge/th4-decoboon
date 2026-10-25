# FIN-WRITE-D2 独立最终证据

- 日期：2026-10-23
- 结论：`PASS`（仅 FIN-WRITE-D2）
- 快照：`/tmp/fin-write-d2-final-reaccept.9KSpAQ`
- Java：22.0.1
- Maven：3.9.16
- 测试数据：H2 MySQL mode
- 方式：当前 `pom.xml + src + config + data + docs`，空 `target`

## 全量测试

```text
Surefire reports: 261
tests: 1056
failures: 0
errors: 0
skipped: 0
```

日志：`/tmp/fin-write-d2-final-reaccept.9KSpAQ/full-test-clean.log`
Maven 返回码：`0`

## 支付并发专项

`PaymentIdempotencyTest`：`8/8 PASS`。

`shouldSerializeConcurrentPaymentsWithTheSameClientKey` 连续 3 次独立运行：`3/3 PASS`。

断言覆盖：

- 同一幂等键最多返回同一 `paymentId`；
- 仅一笔 Payment；
- 仅一笔 Ledger；
- 应付 `paidAmount` 只累计一次；
- 若失败，仅允许一个 409 幂等冲突；
- 数据库唯一索引 `uk_fin_payment_company_idempotency` 拦截第二笔 Payment 插入。

## D2 输入修复核验

- `PaymentIdempotencyTest.java:212-228` 已改为调度无关的同一事实断言；
- `GoodsReceiptRepository` 派生查询已存在并在全量测试中加载；
- F1/F2/F3 定向回归均通过；
- 未发现真实重复付款、核销或流水。

## 边界

本证据只支持 FIN-WRITE-D2 独立验收 PASS。`PI-20261022-016` 整体仍为 `DEV_IN_PROGRESS`：D1/D4 的 DDL/历史回填待用户单独确认，D5 不提前关闭，未发布。
