# MAT-X2 到场状态生产热修验证

## 基线

- 来源：`origin/prod=449f086c`（v2026.11.01.2）
- 热修分支：`hotfix/mat-x2-arrival-state-20261102`
- 变更：CHG-20261102-009、CHG-20261102-010、CHG-20261102-011

## 验证结果

- 定向测试：29/29 PASS
  - MaterialGoodsReceiptFlowTest 8
  - MaterialSheetOrderFlowTest 15
  - MigrationNumberingTest 1
  - StatisticStoreAggregateContextTest 5
- 基线测试同步：10/10 PASS
  - SummaryImplementationRulesTest 7
  - ProviderSecurityAcceptanceTest 3
- 全量测试：971 tests / 0 failures / 0 errors / 0 skipped
- 迁移132：隔离克隆库迁移通过，回滚后列/索引数量均为0
- 构建：BUILD SUCCESS
- 制品 SHA-256：
  - boot.jar `eb9707c1dd8f8c373c9030c7917cf9ba0039314de71821a2faf0d3e53e651cd7`
  - build.tar.gz `683dd01ba9aea86c17f9036c6c5218ed1351541d819325d6ca554e6dca250028`
  - build.zip `fc33f1bd7480c365b822a6ae8367b33d67770eafe59670e4ecd7849d78021f4a`

## 结论

代码提交、全量测试和本机构建已通过；尚未推送、尚未生产迁移、尚未部署。生产执行需用户最终确认。
