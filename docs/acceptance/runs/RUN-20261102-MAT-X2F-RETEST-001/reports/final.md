# RUN-20261102-MAT-X2F-RETEST-001 复验报告

## 运行信息

- 缺陷：`DEF-20261102-005`
- Plan：`TP-20261101-V0.1`
- Oracle：`ZDQ-BIZ-20261101-V0.1` / `11995b9a88852dddb32315a07ecf15f8d515cb40`
- 基础后端：`256cc587dfea26d63fc4ec918caf66d671fe592b`
- 修复补丁 SHA-256：`501163359a5e26e171af87e7ca36e297252aee247a4e428de9413b613b1370f4`
- Web：`ec3da3bc8c7db34ddc2996e345e5c8cd495e09ad`
- App：`f2fb91e0785bf953940257ce97e3ea84fc2ae808`
- 环境：隔离库 `decoboon_accept_20261101`、干净 baseline worktree、后端 `8093`、Web `5180`
- 数据前缀：`AUDIT-20261102-MATX2F001-`

## 执行结果

| 总数 | PASS | FAIL | BLOCKED | NOT_RUN |
| --- | --- | --- | --- | --- |
| 1 | 0 | 0 | 1 | 0 |

`TC-MAT-PM-ARRIVAL-005` 的 Web/API/DB 分支已通过；App 授权身份不可用，整条跨端用例仍为 `BLOCKED`，不能记 `PASS`。

## 缺陷复验

`DEF-20261102-005`：**Web/Backend/DB 分支 VERIFIED**。

- 修复前：首批4/10㎡后作业单直接 `Arrived`，采购仍 `PartiallyReceived`，尾量入口消失。
- 修复后：首批4/10㎡后作业单 `PartiallyArrived`，采购 `PartiallyReceived(4/10)`，页面继续提供确认到场；尾量6㎡后作业单/采购/计划全部 `Arrived`。
- 采购明细 `purchaseItemId=1793557575569359946` 已通过 `sheet_item_id=1793557537923163647` 追溯到具体作业单行；历史20㎡采购保持空关联，不会混入当前作业单。
- 超量7㎡被页面明确拒绝，DB仍为1条4㎡，无部分写入。
- 尾量后到场2条合计10㎡；应付仍唯一¥880、到期日2026-12-02；重复到场请求返回400且不新增receipt、应付或成本。
- Finance项目回读：`actualCost=2640`、`materialCost=2640`，等于同一MaterialPlan历史20㎡+本次10㎡、单价88；`MaterialPlan.cost=2640`。
- 迁移134在隔离库成功执行；独立克隆库完成迁移→回滚，回滚后列和索引数量均为0。

## 测试与构建

- 定向测试：24/24 PASS（`MaterialSheetOrderFlowTest` 15、`MaterialGoodsReceiptFlowTest` 8、`MigrationNumberingTest` 1）。
- Provider响应契约：`findings=0`。
- `mvn -DskipTests package`：BUILD SUCCESS。
- 干净 baseline + 本补丁全量测试：1121 tests，2 failures，均为既有 `StatisticStoreAggregateContextTest` 断言失败，与材料到场改动无关。
- 当前主工作区全量测试：1206 tests，4 failures + 1 error，包含既有脏工作区的 Quote/Payroll/Statistic/Security 基线问题。
- 因此：全量测试门禁仍为 `BLOCKED`，本轮不得进入发布。

## 覆盖与缺口

- 已覆盖：迁移134加列/索引/FK与回滚演练；新采购→作业单行关联；首批4㎡；超量7㎡拒绝；尾量6㎡；计划/采购/作业单/应付/成本对账；重复到场拒绝。
- 未覆盖：App授权微信身份、App真机/弱网、异常退换、结算确认、生产迁移与发布。
- 全量测试失败：`StatisticStoreAggregateContextTest.receiptAmountAndActiveProjectCountRespectStoreScope`、`unassignedCountsAndMonthQuoteCountsAreSqlSide`，为既有非材料测试。
- 版本管理：修复尚未提交；主工作区受132/133等其他未提交迁移和大量未提交改动影响，不能把混合目录冒充单一发布候选。

## 结论

- 结论：`BLOCKED`
- 能证明什么：`DEF-20261102-005` 的 Web/Provider/DB 状态与成本分支已修复并可复现通过。
- 不能证明什么：App 同源、全量测试全绿、生产可发布。
- 残余风险：App分支未验证；全量测试仍有2个既有statistic失败；132/133等前置未提交迁移未形成干净发布基线。
- 下一步：先把132/133等既有改动按各自CHG收口为干净基线，再提交MAT-X2F补丁；修复或隔离2个既有statistic失败后重跑全量测试；App身份可用后补跨端回读；生产迁移/发布需再次确认。
