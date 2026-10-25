# FIN-WRITE-D2 全量测试绿色复跑

- 日期：2026-10-23
- 目的：解除 `FIN-WRITE-D2` 与 `COST-D1` 共用的后端全量测试门禁
- 环境：完整隔离副本，Java 22.0.1，空 `target`，H2 MySQL mode
- 快照：`/tmp/full-test-d2-cost.AFPSq5/repo`
- 命令：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-22.jdk/Contents/Home \
PATH=/Library/Java/JavaVirtualMachines/jdk-22.jdk/Contents/Home/bin:$PATH \
mvn -o -q clean test
```

## 结果

```text
test classes: 253
tests: 1027
failures: 0
errors: 0
skipped: 0
exit: 0
```

## 本次解除的门禁项

1. `@EntityGraph` 超过 4 个属性的 A4 门禁失败：
   - 删除 `QuoteLineRepository` 未使用的宽查询；
   - 将费用详情/分页的大图谱拆为窄的主查询和人员查询，保留页面所需对象。
2. 客户旅程当前源码的 `hasSourceLines`、区域组和类型组断言已通过；此前的失败来自隔离快照中源码与测试不同步。
3. 费用生命周期测试 8/8 PASS，包含停用分类提交拒绝、项目费用附件归属和幂等意图校验。
4. `DecoboonClosedLoopTest` 与 `DesignLifecycleClosureTest` 改为通过合同接口显式传递平台基线模板：
   - 付款计划：`STANDARD_30_40_30`
   - 施工计划：`RESIDENTIAL_STANDARD_8`

## 定向证据

```text
SummaryImplementationRulesTest: 7/7 PASS
ExpenseLifecycleTest: 8/8 PASS
FinanceStoreScopeHttpAcceptanceTest: 22/22 PASS
ReceiptRefundContextTest: 2/2 PASS
PaymentIdempotencyTest: 8/8 PASS
DecoboonClosedLoopTest: 1/1 PASS
DesignLifecycleClosureTest: 1/1 PASS
```

## 结论边界

本次结果是开发侧全量门禁证据：`FIN-WRITE-D2` 原有的全量测试阻断已解除。`02-independent-acceptance.md` 中“功能复验 PASS”保持不变；该报告形成时的整体 FAIL 结论需要独立复验人基于本文件重新判定，开发侧不得自行把 D2 标成独立验收 PASS。

## 后续三端收敛全量（2026-10-23）

- 快照：`/tmp/zhuangdeqing-converge.JgGxh1`
- 后端：`261` 个测试类、`1056` tests、`0 failures`、`0 errors`、`0 skipped`。
- App：`verify:mp-weixin` PASS。
- Web：`type-check + build + actions/permissions/states/workspaces/page-system/forms/concepts/enums/metrics/store-scope` PASS。
- 统一证据：`docs/acceptance/runs/20261023-three-surface-convergence/README.md`。

## 独立最终复核（2026-10-23）

独立复验人未直接采用本文件的开发自述，已在当前完整后端工作区新快照 `/tmp/fin-write-d2-final-reaccept.9KSpAQ` 从空 `target` 复跑 Java 22 全量：

```text
Surefire reports: 261
tests: 1056
failures: 0
errors: 0
skipped: 0
```

`PaymentIdempotencyTest` 8/8，并发同键专项连续 3/3 PASS；F1/F2/F3 关闭。D2 独立验收结论：PASS。PI-016 整体仍为 DEV_IN_PROGRESS，D1/D4 与 D5 未关闭。
