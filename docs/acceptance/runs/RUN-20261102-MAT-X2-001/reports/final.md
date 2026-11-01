# RUN-20261102-MAT-X2-001 验收报告

## 运行信息

- Plan：`TP-20261101-V0.1`
- Suite：`SUITE-CORE-CHAIN-V0.1`
- Oracle：`ZDQ-BIZ-20261101-V0.1` / `11995b9a88852dddb32315a07ecf15f8d515cb40`
- 后端提交：`256cc587dfea26d63fc4ec918caf66d671fe592b`
- Web 提交：`ec3da3bc8c7db34ddc2996e345e5c8cd495e09ad`
- App 提交：`f2fb91e0785bf953940257ce97e3ea84fc2ae808`（本轮无法合法登录）
- 环境：隔离后端 `8093`、干净 Web `5180`、隔离数据库 `decoboon_accept_20261101`
- 租户：`199500000000000001`
- 账号范围：Designer、Finance、Pm
- 数据前缀：`AUDIT-20261102-MATX2001-`

## 执行结果

| 总数 | PASS | FAIL | BLOCKED | NOT_RUN |
| --- | --- | --- | --- | --- |
| 4 | 0 | 1 | 3 | 0 |

### 逐例结论

| 用例 | 优先级 | 状态 | Web/Provider/DB 结论 | App 分支 |
| --- | --- | --- | --- | --- |
| `TC-MAT-DESIGNER-SHEET-002` | P0 | BLOCKED | 设计师从计划行建单、编辑、提交及事实回读通过 | `BLOCKED_IDENTITY` |
| `TC-MAT-BOUNDARY-QUANTITY-003` | P0 | BLOCKED | 上限10㎡保存通过；10.01㎡明确拒绝，无部分写入 | `BLOCKED_IDENTITY` |
| `TC-MAT-FINANCE-ORDER-004` | P0 | BLOCKED | 唯一采购与唯一应付；重复下单幂等；金额、数量和来源一致 | `BLOCKED_IDENTITY` |
| `TC-MAT-PM-ARRIVAL-005` | P1 | FAIL | 首批4/10㎡后作业单错误显示 `Arrived`，采购仍 `PartiallyReceived`，尾量入口消失 | `BLOCKED_IDENTITY` |

## 缺陷

| DEF | 优先级 | 状态 | 场景 | 证据 | 复验条件 |
| --- | --- | --- | --- | --- | --- |
| `DEF-20261102-005` | P1 | OPEN | 首批部分到场后作业单被错误标记已到场，尾量到场入口消失 | `evidence/TC-MAT-PM-ARRIVAL-005/03-after-first-arrival-4.png`、`04-provider-sheet-after-first-arrival.json`、`05-provider-purchases-after-first-arrival.json`、`07-page-after-refresh-no-tail-action.png`、`08-db-after-first-arrival.txt` | 新 RUN 先到4㎡仍为 PartiallyArrived 且可继续；再到6㎡后作业单/采购/计划全部到齐，应付唯一且成本880 |

## 覆盖与缺口

- 已覆盖：Designer 计划来源建单与提交；数量上下限；Finance 下单、采购/应付一致性及重复请求；Pm 首批到场与跨层对账。
- 失败：Pm 首批到场后 `MaterialOrderSheet=Arrived` 与 `PurchaseOrder=PartiallyReceived(4/10)`、`MaterialPlan=PartiallyArrived` 冲突，页面尾量动作消失。
- 阻塞：App/Owner 授权微信身份不可用，三条 Web 部分已通过的用例按跨端完整性记 `BLOCKED`，未冒充 `PASS`。
- 未闭环：尾量6㎡、全部到场终态、最终成本880、结算门禁和 App 同源回读均无法在本轮给出通过结论。
- 测试资产差异：Designer 当前实际入口为 `/workbench/material-sheets`，冻结 pageTargets 写为 `/finance/material-ordering`；建议后续校正资产，但本轮按角色可见入口执行并如实记录。
- 未执行：视觉人工评审、App 真机/弱网、生产环境验证。

## 结论

- 结论：`FAIL`
- 能证明什么：Web/Provider/DB 的设计师计划来源建单、10㎡边界拒绝、财务唯一采购/应付与幂等已形成可复核证据。
- 不能证明什么：不能证明 MAT-X2 已验收通过；App/Owner 未验证；到货尾量、最终成本与结算未完成验证。
- 残余风险：作业单与采购/计划状态冲突会让项目经理误以为材料已全部到场，并丢失尾量操作入口，可能阻断材料结算与成本完整判断。
- 下一步：以 `DEF-20261102-005` 建立修复单，修复状态推导后新建独立 RUN 复验；复验顺序为首批4㎡、尾量6㎡、超量拒绝、唯一应付、成本880、刷新回读；App 身份可用后再补跨端分支。
