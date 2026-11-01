# RUN-20261101-MAT-ARRIVAL-FIN-001 验收报告

## 运行信息

- Plan：`TP-20261101-V0.1`
- Suite：`SUITE-CORE-CHAIN-V0.1`
- Oracle：`ZDQ-BIZ-20261101-V0.1` / `11995b9a88852dddb32315a07ecf15f8d515cb40`
- 后端提交：`7b7dc5f0ed3795fbd6d13640f5dfb7f51b8c3dd8`
- Web 提交：`e40ca8baf6203d71f5d0375cc043d424a0aa5873`
- App 提交：`f2fb91e0785bf953940257ce97e3ea84fc2ae808`
- 环境：本地，昭青科技 `199500000000000001`
- 账号范围：Pm、Finance
- 项目：`242541620656865280`
- 数据前缀：`AUDIT-20261101-RUN20261101MATAR-`
- 执行时间：2026-11-01 15:49–16:05（Asia/Shanghai）

## 执行结果

| 总数 | PASS | FAIL | BLOCKED | NOT_RUN |
| --- | --- | --- | --- | --- |
| 3 | 2 | 1 | 0 | 0 |

| 用例 | 级别 | 结果 | 结论 |
| --- | --- | --- | --- |
| `TC-MAT-PM-ARRIVAL-005` | P1 | FAIL | 到场数量、状态、成本、应付账期均通过；App 无现场证据上传入口，`images=[]` |
| `TC-FIN-FINANCE-PAYABLE-003` | P0 | PASS | 应付、付款、核销、资金台账、供应商台账和 App/Web 回读一致；幂等与超额门禁通过 |
| `TC-FIN-FINANCE-COST-004` | P0 | PASS | 实际成本 ¥240、成本未完整、利润和毛利率为空；App/Web 金额到分 |

## 缺陷

| DEF | 优先级 | 状态 | 场景 | 证据 | 复验条件 |
| --- | --- | --- | --- | --- | --- |
| `DEF-20261101-003` | P2 | OPEN | App 材料到场缺少现场证据上传与展示 | `evidence/TC-MAT-PM-ARRIVAL-005/01-arrival-before-app.png`、`03-arrival-refreshed-app.png`、`11-arrival-provider-db-final.json` | App 可上传合成图片，Provider/DB 保存且刷新、重进、财务端回读一致 |

统一问题记录：`docs/product/issue-governance/issues/PI-20261101-002-App材料到场缺少现场证据上传与展示.md`。

## 页面、Provider、DB 对账

### 材料到场

- App 首次显示 `已到 0/2`，确认后刷新重进显示 `已到 2/2`；
- `db_goods_receipt` 1 条，数量 2，确认人为审计项目经理；
- `db_purchase_order` 与 `db_purchase_order_item` 均为 `Received`，累计 2；
- 材料计划为 `PartiallyArrived`，计划量 12、已下单/采购量 2、成本 ¥240，语义一致；
- 材料作业单为 `Arrived`，MaterialSheet 详情显示“材料已全部到场，等待财务结算”；
- 应付 `receiptDate=2026-11-01`、`dueDate=2026-12-01`；
- 到场页没有照片/附件控件，DB `db_goods_receipt_images` 为 0 条。

### 应付与付款

- 付款前：应付 ¥240、已付 ¥0、待付 ¥240、状态 `Pending`；
- 付款后：应付 ¥240、已付 ¥240、待付 ¥0、状态 `Paid`；
- 付款、核销、资金台账各 1 条，金额均为 ¥240，来源均为本批采购；
- 同幂等键重复提交返回原 `paymentId=242556961927725056`，未新增副作用；
- 新幂等键再次支付同一已结清应付被 400 拒绝；
- Web 付款记录和 App 付款记录显示同一笔 ¥240、同一交易号与供应商。

### 项目成本

- `actualCost=240`、`totalCost=240`、预计成本 `1760`；
- `costComplete=false`、`costIncompleteCount=2`；
- `profitAmount=null`、`grossMargin=null`；
- Web 显示“项目毛利 / 成本未完整”；App 利润位置无数字；
- 未使用 0 或预计成本伪造实际利润。

## 覆盖与缺口

- 已覆盖：PM App 到场确认、刷新重进、满量后重复点击、MaterialSheet 回读、PM Web 工作台回读、Finance Web 付款、App 付款记录、幂等、超额拒绝、项目成本与利润空值规则。
- 未单独覆盖：采购量仅 2，第一次页面动作即完成全量到场，没有单独执行“先 1㎡、再补 1㎡”的分段到场路径；该限制不改变本次终态对账，但不得把分段状态机完整覆盖计入 PASS。
- 现场证据链未通过：App 无上传入口，`GoodsReceipt.images` 为空。
- 本次未执行项目结算写入。观察到 Web/App 在无结算草稿时显示可用“生成结算”按钮，是否应提前禁用属于 `TC-FIN-FINANCE-SETTLEMENT-005` 范围，留给结算专项判定。
- 本次未执行内部编号隐藏专项。App 项目财务页可见项目号 `P1793516028568`，应由 `TC-XC-COPY-IDENTIFIER-003` 在跨切面批次统一判定。

## 结论

- 建议结论：`CONDITIONAL_PASS`
- 能证明：材料到货、采购终态、应付账期、付款核销、资金台账和项目实际成本已经形成同一条可对账事实；P0 财务用例通过，重复付款与伪利润风险未出现。
- 不能证明：到场现场证据链完整；不足数量分段到场的完整页面演进；项目结算门禁；内部编号隐藏。
- 残余风险：存在 1 个开放 P2 `DEF-20261101-003`，以及本报告列出的未覆盖专项。
- 下一步：先由研发修复 App 到场证据上传，或由用户明确接受该 P2 临时风险；修复后创建新 RUN 引用 `DEF-20261101-003` 复验。随后进入阶段验收/整改与业主身份专项。
