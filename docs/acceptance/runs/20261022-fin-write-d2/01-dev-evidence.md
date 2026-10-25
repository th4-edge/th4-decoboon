# FIN-WRITE-D2 开发自测证据

> 状态：`DEV_VERIFIED / FULL_TEST_PASS / INDEPENDENT_REACCEPTANCE_PASS`
> 独立验收：FIN-WRITE-D2 已 PASS；全量证据见 `05-final-independent-evidence.md`，结论见 `02-independent-acceptance.md`。
> 注意：D2 通过不代表 `PI-20261022-016` 整体闭环。

## 变更范围

- `finance/receivable/update`、`finance/payable/update`：按 ID 查询统一叠加数据范围；
- `finance/payment/save`：单张/多张应付按数据范围加载，多应付必须同门店；
- `finance/payment/employee/save`：员工工资/提成应付按数据范围加载，多应付必须同门店；
- `finance/refund/save`：原收款按数据范围加载，退款继承原收款门店/团队/负责人；
- `finance/settlement/update`、`finance/settlement/retention-release`：结算按数据范围加载；
- `finance/ledger/update`：手工流水按数据范围加载；
- `document/voucher/build`：来源流水按数据范围加载，多流水必须同门店；
- 付款、退款继承来源事实门店，避免企业范围操作人改写为当前账号门店；
- 新增 HTTP 验收覆盖跨店写路径和员工工资跨店发放。

## 验证环境

- 独立临时工作副本 `/private/tmp/fin-write-d2.0ej4j9`
- H2：`MODE=MySQL`
- 从空 `target` 开始编译并运行，不复用共享工作区编译产物
- 当前日期：2026-10-22

## 结果

| 验证 | 结果 | 证据 |
| --- | --- | --- |
| 主源码编译 | PASS | 干净副本测试生命周期内完成编译 |
| `FinanceStoreScopeHttpAcceptanceTest` | PASS 21/21 | `http-targeted-report.txt` |
| `FinanceStoreScopeTest` | PASS 10/10 | 开发运行报告 |
| `FinanceAccessContextTest` | PASS 4/4 | 开发运行报告 |
| `VoucherBuildAcceptanceTest` | PASS 6/6 | 开发运行报告 |
| `ReceiptRefundContextTest` | PASS 2/2 | `receipt-refund-report.txt` |
| `PaymentIdempotencyTest` | 时序波动 | 首轮 PASS 8/8；复跑并发同键出现两笔成功返回，详见 `payment-idempotency-report.txt` |
| D2 阻断项 F1/F2/F3 定向复验 | PASS | `FinanceStoreScopeHttpAcceptanceTest#idWritesRejectCrossStoreTargetsAndMixedStoreVouchers` 1/1；退款单测 2/2 |

## HTTP 验收重点

`FinanceStoreScopeHttpAcceptanceTest` 覆盖：

- 门店财务/店长修改他店应收、应付；
- 向应付付款、向原收款退款；
- 修改结算、释放质保；
- 更正手工流水；
- 生成他店流水凭证；
- 总部财务混合两家门店流水生成凭证；
- 员工工资/提成跨店发放；
- 门店候选、总部财务候选和既有门店范围回归。

## 独立验收阻断项复验

- F1 员工工资/提成付款 Ledger 门店继承：已加入无项目、B 店员工工资应付的正向 HTTP 断言，Payment 与 Ledger 均必须为 B 店；
- F2 空门店历史来源：已加入空门店应付付款断言，Payment 与 Ledger 均必须保持空门店；
- F3 退款单测签名：已更新为 15 参数 `recordLedger` 验证，`ReceiptRefundContextTest` 2/2 PASS。

## 风险与未闭环

1. `PaymentIdempotencyTest` 的并发同键用例存在时序波动：首轮 PASS，复跑出现两次成功返回。该结果不改变 D2 的门店范围结论，但必须由独立验收确认两次返回是否指向同一付款记录及是否存在真实并发风险。
2. D2 不包含来源字段/DDL 和白名单（D1），不包含店长凭证生成角色开放（D3），不包含跨端页面（D4）。
3. 共享工作区存在并行 `Expense`、报表和报价改造，不能在共享 `target` 上判断 D2 结果。
4. 无真实 MySQL、真实浏览器和真机证据；D2 当前仅为后端开发自测，独立验收尚未完成。
