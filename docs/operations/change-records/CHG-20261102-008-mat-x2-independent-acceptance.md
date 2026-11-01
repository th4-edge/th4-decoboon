# CHG-20261102-008 · MAT-X2 独立验收与 P1 缺陷登记

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261102-008 |
| 类型 | 运维 / 独立验收 |
| 风险级别 | L1 |
| 状态 | 待验证 |
| 负责人 | codex |
| 日期 | 2026-11-02 |
| 关联需求/事故/业务裁决 | `MAT-X2`、`BR-003/BR-009/BR-044/BR-045/DEC-072/DEC-092`、`DEF-20261102-005` |

## 2. 目标与范围

- 目标：在隔离数据库与干净构建上独立复验 MAT-X2 的设计师建单、数量边界、财务下单和 PM 到场主链；不以开发自测替代独立验收。
- 范围内：后端 `256cc587`、Web `ec3da3b`；Designer/Finance/Pm Web 页面、Provider 和隔离库对账。
- 非范围：代码修复、App 身份伪造、视觉人工评审、生产发布。
- 受影响端：后端 / Web（仅验收证据）；App 因授权身份不可用标 `BLOCKED_IDENTITY`。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：是
- 业务事实来源与裁决编号：`TC-MAT-DESIGNER-SHEET-002`、`TC-MAT-BOUNDARY-QUANTITY-003`、`TC-MAT-FINANCE-ORDER-004`、`TC-MAT-PM-ARRIVAL-005`；Oracle `ZDQ-BIZ-20261101-V0.1`
- 尚存 GAP 或假设置：App/Owner 授权微信身份不可用；Designer 冻结 pageTarget 与当前可见入口存在路由差异。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | Designer 从计划行建单并提交 | 材料计划可用、隔离库 | sourceKind=Plan，数量10㎡，提交后可回读 | `docs/acceptance/runs/RUN-20261102-MAT-X2-001/evidence/TC-MAT-DESIGNER-SHEET-002/` |
| 2 | Designer 数量边界 | 上限10㎡ | 10㎡保存通过，10.01㎡拒绝且不落库 | `docs/acceptance/runs/RUN-20261102-MAT-X2-001/evidence/TC-MAT-BOUNDARY-QUANTITY-003/` |
| 3 | Finance 下单与幂等 | 作业单 Submitted | 唯一采购与应付，重复请求不新增，金额均¥880 | `docs/acceptance/runs/RUN-20261102-MAT-X2-001/evidence/TC-MAT-FINANCE-ORDER-004/` |
| 4 | Pm 首批4/10㎡与尾量6㎡ | 采购 Ordered | 首批后 PartiallyArrived 且可继续，尾量后全部到场 | `docs/acceptance/runs/RUN-20261102-MAT-X2-001/evidence/TC-MAT-PM-ARRIVAL-005/` |
| 5 | App/Owner 同源回读 | 授权微信身份 | 与 Web/Provider 同一事实 | `BLOCKED_IDENTITY`，无证据 |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | dev（本地） | `256cc587` | MAT-X2 验收资产与证据 |
| th4-decoboon-web | dev（本地） | `ec3da3b` | MAT-X2A/X2B 页面 |
| th4-decoboon-app | 当前 | `f2fb91e` | App 分支未执行，身份不可用 |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| 冻结资产校验 | `python3 docs/testing/scripts/test_assets.py validate` | PASS | 预检记录 |
| 真实页面/接口/DB | `/workbench/material-sheets`、`/finance/material-ordering`、`/finance/payables` | 3 个 Web 分支通过，Pm 到场 FAIL | `RUN-20261102-MAT-X2-001` |
| 负向/边界 | 10.01㎡保存 | PASS_REJECTED，无部分写入 | `TC-MAT-BOUNDARY-QUANTITY-003` |
| 重复请求幂等 | 同一 `material/sheet/order` 请求重复提交 | PASS，采购/应付不增加 | `TC-MAT-FINANCE-ORDER-004` |
| 最终 RUN 校验 | `validate_run.py ... --final` | PASS，0 errors / 0 warnings | `reports/final.md` |
| App 真机 | 微信授权身份 | BLOCKED_IDENTITY | 无 |

- 未执行项及原因：App/真机/弱网因授权身份不可用；视觉人工评审未执行；尾量6㎡因页面状态缺陷无法执行。
- 已知失败及是否与本次相关：`DEF-20261102-005` P1，首批4/10㎡后作业单错误为 `Arrived`，采购为 `PartiallyReceived`，尾量入口消失。

## 7. 发布

| 字段 | 内容 |
| --- | --- |
| Release ID | 不适用 |
| 制品路径/SHA-256 | 不适用 |
| 生产 release/版本 | 不适用 |
| 上一版本/回滚点 | 不适用 |
| 健康检查 | 不适用 |
| 发布后观察 | 不适用 |

- 数据库迁移：无
- 配置/密钥变化：无

## 8. 回滚

- 回滚条件：不适用（仅隔离验收，无生产变更）
- 回滚命令/步骤：不适用
- 回滚验证：不适用
- 实际是否回滚：否

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集
- 人工耗时：未采集
- 一次通过：否
- 返工原因：Pm 首批到场后作业单状态与采购/计划事实冲突，尾量页面入口消失
- 最终结论：阻断，待修复 `DEF-20261102-005` 后新建 RUN 复验

## 10. 复盘与沉淀

- 是否出现重复问题：否（本轮首次发现该具体状态推导缺陷）
- 是否需要新增门禁：是，修复后至少补“同一材料计划存在历史采购/多作业单时，首批部分到场不得跨采购累计误判 Arrived”的负向回归
- 候选 `EXP/ANT/PATH`：到场状态必须按当前作业单与对应采购事实计算，禁止以计划全量历史累计量替代
- 后续动作与负责人：研发修复 `DEF-20261102-005`；独立验收新建 RUN 复验尾部全链路与 App 回读
