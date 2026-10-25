# FIN-WRITE-D2 全量测试尝试记录

- 日期：2026-10-23
- 目的：按权限/跨店高风险变更门禁尝试后端全量测试
- 环境：隔离临时副本，H2 MySQL mode，从空 `target` 编译
- 命令：`mvn -o test`
- 结果：中止于 `785` 个测试，`Failures: 5`、`Errors: 4`；全量门禁未通过

## 失败类

1. `OwnerClosureAcceptanceTest.shouldBackfillLegacyAcceptanceRoundsIdempotently`：临时副本未带 `docs/design/migrations`，`NoSuchFileException`。
2. `ProductionReleaseBaselineAcceptanceTest`：临时副本缺少发布运行手册导致文件读取失败。
3. `ProjectReportPublisherAccessTest`：并行源码存在旧包名导入，测试编译/运行异常。
4. `StoreDimensionBindingHttpAcceptanceTest.zeroStoreIdIsEquivalentToOmittedOnEveryRoute`：并行门店绑定改造回归。
5. `MigrationNumberingTest.shouldNotIntroduceNewDuplicateMigrationNumbers`：临时副本缺少迁移目录/并行迁移变更。
6. `MigrationSchemaParityTest.migrationDdlMustCoverEveryEntityColumn`：同上。
7. `SummaryImplementationRulesTest.entityGraphMustStayNarrow`：并行报表/投影改造。
8. `PayrollWorkbenchContextTest.shouldProjectLatestSheetWhenSelectedPeriodHasNoSheet`：并行薪酬工作台改造。

## 结论

- 上述失败均未指向本次 `FIN-WRITE-D2` 变更文件，但未做逐项基线对照，不能据此宣称全量测试通过。
- `FIN-WRITE-D2` 的功能/权限定向目标与补充回归 PASS，但全量测试门禁未通过，独立验收整体不能记为 PASS。
- 权限类高风险变更正式发布前，必须在无并行改动、包含完整 `docs/design/migrations` 的干净基线上重新执行全量测试；届时通过后才可重新申请独立验收，否则阻断发布。

## 后续复跑：全量门禁已通过（2026-10-23）

- 在完整隔离副本 `/tmp/full-test-d2-cost.AFPSq5/repo` 上执行 `mvn -o -q clean test`。
- 结果：`253` 个测试类、`1027` 个测试、`0 failures`、`0 errors`、`0 skipped`，退出码 `0`。
- 本轮同时清除了 A4 `@EntityGraph` 门禁、客户旅程统一报价投影、费用详情/分页预取和两个闭环测试的模板前置条件。
- 全量日志：`/tmp/full-test-d2-cost.AFPSq5/full-test-20261023-0137.log`。
- 结论：本文件此前的 `785 tests / 5 failures / 4 errors` 是历史失败尝试，已被本轮绿色结果覆盖；独立验收仍需重新判定。
