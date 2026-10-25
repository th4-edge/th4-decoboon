# FIN-WRITE-D2 独立复验报告

- 结论：**PASS（仅 FIN-WRITE-D2）**
- 验收对象：`FIN-WRITE-D2` 按 ID 写入目标门店范围统一
- 业务依据：`ANS-BIZ-20261022-010`、`T12-BIZ-20261022-010`、`DEC-089`、`BR-055`
- 验收日期：2026-10-23
- 验收角色：独立复验；未修改实现、权限、DDL、迁移、测试或业务规则
- 验收环境：Java `22.0.1`，Maven `3.9.16`，空 `target`，H2 MySQL mode
- 独立快照：`/tmp/fin-write-d2-final-reaccept.9KSpAQ`

## 一、结论

`FIN-WRITE-D2` 的门店范围、来源归属、F1/F2/F3 修复、支付同键并发不变量和仓库全量测试门禁均通过独立复验。

上一轮的 FAIL 唯一阻断为全量测试未闭合。该阻断已由本次独立全量执行解除：

```text
test classes: 261
tests: 1056
failures: 0
errors: 0
skipped: 0
```

本结论只覆盖 `FIN-WRITE-D2`。`PI-20261022-016` 仍为 `DEV_IN_PROGRESS`；D1/D4 的 DDL/历史回填仍需用户单独确认，D5 不得提前关闭，也不代表已发布。

## 二、对收敛证据的核验

### 2.1 时间顺序核验

- `/tmp/zhuangdeqing-converge.JgGxh1/backend-full-rerun.log` 在 03:18 结束为 `1056 tests / 2 failures / 10 errors`，不是最终绿色结果。
- `PaymentIdempotencyTest` 在 03:19 被修改；`backend-full-final.log` 在 03:20 启动。
- `backend-full-final.log` 文件在 03:23:54 结束时没有 `Tests run/BUILD` 总结，不能单独作为全量通过证据。
- 因而本次不以开发自述或该日志文件名判定通过，而是对当前完整后端工作区重新做独立全量复跑。

### 2.2 当前源码一致性

当前工作区和收敛快照的以下关键文件 SHA-256 一致：

- `src/test/java/com/th4/edge/decoboon/finance/PaymentIdempotencyTest.java`
- `src/main/java/com/th4/edge/decoboon/finance/service/payments/save/support/Save.java`

`GoodsReceiptRepository` 的修复存在：

```java
List<GoodsReceipt> findByPurchase_Project_ProjectIdOrderByReceiptIdDesc(Long projectId);
```

该派生查询在独立全量测试中成功启动并通过。

## 三、独立全量执行

隔离副本包含当前 `pom.xml + src + config + data + docs/design + docs/operations + docs/product`，从空 `target` 开始执行：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-22.jdk/Contents/Home \
PATH=/Library/Java/JavaVirtualMachines/jdk-22.jdk/Contents/Home/bin:$PATH \
mvn -o -q clean test
```

Maven 进程返回码：`0`。Surefire 报告独立汇总：

```text
reports: 261
tests: 1056
failures: 0
errors: 0
skipped: 0
```

关键报告：

- `PaymentIdempotencyTest`：`8/8 PASS`
- `ReceiptRefundContextTest`：`2/2 PASS`
- `FinanceStoreScopeHttpAcceptanceTest`：通过
- `GoodsReceiptRepository` 相关测试：通过
- 全量测试中无失败或错误报告

## 四、PaymentIdempotencyTest 新断言复核

测试位置：`src/test/java/com/th4/edge/decoboon/finance/PaymentIdempotencyTest.java:200-228`

新断言不再依赖线程完成顺序，而是校验：

1. 同一幂等键最多成功返回同一 `paymentId`；
2. 数据库最终仅有一笔 Payment；
3. 数据库最终仅有一笔 Ledger；
4. 应付 `paidAmount` 只累计一次，为 `300.00`；
5. 若出现失败，只能有一个 409 幂等冲突，且不能有第二套副作用。

独立连续运行同一并发用例 3 次：`3/3 PASS`。

每次运行均观察到 `uk_fin_payment_company_idempotency` 唯一索引冲突，第二笔 Payment 插入被拦截并转换为 409；未发现重复 Payment、PayableSettlement 或 Ledger。该行为覆盖了“一个成功一个 409”和“两个请求返回同一 paymentId”两种合法调度。

## 五、F1/F2/F3 复核

### F1：员工工资/提成 Ledger 门店继承

- Payment 从来源应付继承 store/team/owner。
- Ledger 使用 source-aware 15 参数重载，传入 Payment 的 store/team/owner。
- B 店、项目为空的员工工资正向场景，Payment/Ledger 均为 B 店。

结论：PASS。

### F2：空门店历史来源保持

- source-aware 路径无条件覆盖 store/team；来源为空时保持 `null`。
- 空门店历史应付付款后，Payment/Ledger 均为 `null`。

结论：PASS。

### F3：退款单测门禁

- `ReceiptRefundContextTest` 使用 15 参数 `recordLedger` 验证。
- 独立执行：`2/2 PASS`。

结论：PASS。

## 六、D2 门禁矩阵

| 门禁 | 结论 | 证据 |
| --- | --- | --- |
| 应收/应付按目标 ID 数据范围写入 | PASS | 目标查询统一叠加 `byId.and(visible)`；全量回归通过 |
| 单张/多张付款门店范围与混店拒绝 | PASS | HTTP 负向与全量回归通过 |
| 员工工资/提成来源门店继承 | PASS | B 店、无项目正向断言 Payment/Ledger 同店 |
| 退款门店继承与单测门禁 | PASS | HTTP 负向 + `ReceiptRefundContextTest` 2/2 |
| 结算、质保释放、台账更新范围 | PASS | 目标对象范围查询；HTTP 负向通过 |
| 凭证来源流水范围与同店合并 | PASS | `VoucherContext` DataScopeSpecification；HTTP 400/403/404 |
| 跨企业/无权限角色 | PASS | 数据范围 Specification、`requireEditor` 与定向回归 |
| 失败无部分写入 | PASS | 事务边界 + 失败路径先校验后写入；全量与幂等回归通过 |
| 并发同键副作用 | PASS | 3 次专项 + 8/8 全类；唯一索引、单 Payment/Ledger/累计一次 |
| 全量测试 | PASS | 独立快照 1056/0/0/0 |

## 七、剩余人工门禁

以下不属于本次 D2 PASS，但仍是 PI-016 整体关闭前的人工门禁：

- FIN-WRITE-D1：来源字段、来源白名单与历史承接；涉及 DDL/迁移，需用户确认。
- FIN-WRITE-D4：期初资金门店字段与跨端页面；涉及 DDL/历史回填，需用户确认。
- FIN-WRITE-D5：跨企业、跨店、重复来源、越权、并发的组合验收；不得提前关闭。
- 真实 MySQL、真实页面/真机、生产发布与回滚演练未由本报告替代。

## 八、验收处置

- `FIN-WRITE-D2`：**PASS**
- `PI-20261022-016`：整体保持 `DEV_IN_PROGRESS`
- F1/F2/F3：**CLOSED**
- 真实重复付款/核销/流水：**未发现**
- D1/D4：等待用户单独确认
- D5：不得提前关闭
- 发布：**未执行，不得宣布发布**
