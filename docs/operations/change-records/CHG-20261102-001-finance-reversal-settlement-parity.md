# CHG-20261102-001 · 财务收款冲正入口与结算成本投影修复

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261102-001 |
| 类型 | 缺陷修复 |
| 风险级别 | L3 |
| 状态 | `COMMITTED / DEFECT RETEST PASS / FULL P0 CASE BLOCKED` |
| 负责人 | codex |
| 日期 | 2026-11-02 |
| 关联需求/事故/业务裁决 | `DEF-20261102-001`、`DEF-20261102-002`、`BR-055`、`DEC-075`、`DEC-076`、`MET-002`、`DEC-092` |

## 2. 目标与范围

- 目标一：Web 收款记录为 BOSS/店长提供受控收款冲正入口，复用 `finance/receipt/reverse`。
- 目标二：`finance/settlement/page` 与 `finance/project/report` 使用同一成本完整性投影。
- 范围内：Web 收款记录与冲正抽屉、Web finance API/model/meta、后端结算分页。
- 非范围：不放开 Finance 高危冲正权限，不修改冲正状态机，不改变结算金额公式，不新增同义 Provider，不发布。

## 3. 业务与决策

- 收款冲正高危权限：BOSS 全企业、店长本店；Finance 保持拒绝，依据 `DEC-075/DEC-076`。
- 成本不完整时利润与毛利率为空，结算确认保持禁用，依据 `MET-002/DEC-092`。
- 冻结用例 `TC-FIN-FINANCE-RECEIPT-002` 把 Finance 声明为冲正执行角色，与业务规则冲突。测试资产需由责任方拆分角色步骤；代码不为测试通过错误放开 Finance。

## 4. 实现

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | dev | `64abe92e` | `finance/service/settlements/page/support/Page.java`、`FinanceSettlementPageCostProjectionTest.java` |
| th4-decoboon-web | dev | `a6e98ee` | `FinanceReceiptReversalDrawer.vue`、`FinanceReceiptsView.vue`、`domains/finance/api.ts`、`model.ts`、`meta.ts` |
| th4-decoboon-app | dev | 无改动 | App 已有冲正页与 Provider，保持同源 |

## 5. 验证

| 检查 | 结果 | 证据 |
| --- | --- | --- |
| 后端定向测试：结算分页、报价白名单、收款冲正 | PASS | `mvn -q -Dtest=FinanceSettlementPageCostProjectionTest,QuoteViewContractTest test`，干净提交 worktree 退出 0 |
| Web type-check | PASS | `npm run type-check` |
| Web type-check/build | PASS | 干净提交 worktree `npm run build` 退出 0，含 type-check、分页与客户端文案门禁 |
| Provider 响应契约 check + self-test | PASS，findings=0 | `audit-provider-response-contract.py` |
| 店长 Web 冲正入口与完整页面流程 | PASS | `RUN-20261102-FIN-FIX-RETEST-001/evidence/TC-FIN-FINANCE-RECEIPT-002/` |
| 冲正 DB/API 回读 | PASS：原收款 Reversed、反向台账¥100、应收待收恢复¥648.73 | 同上 `05-reversal-db.txt`、`06-reversal-api.json` |
| Finance 冲正权限负向 | PASS：403 | 同上 `07-finance-reverse-denied.json` |
| 结算分页与项目报告七项成本字段 | PASS：完全一致 | `RUN-20261102-FIN-FIX-RETEST-001/evidence/TC-FIN-FINANCE-SETTLEMENT-005/01-settlement-parity.json` |
| 原始 3 条 P0 用例全链路 | BLOCKED | 冻结用例角色冲突；App/业主授权微信身份不可用 |

## 6. 发布

- 已提交到本地 `dev`：后端 `64abe92e`、Web `a6e98ee`；未推送、未发布、未生成正式制品。
- 未修改数据库结构；验收仅使用隔离库 `decoboon_accept_20261101`。
- 仍不满足发布门禁：主工作区还有大量无关未提交变更，App/跨端 P0 未完成。

## 7. 回滚

- Web：回退新增抽屉、页面入口和 finance API/model/meta 改动。
- 后端：回退结算分页成本投影与对应测试。
- 实际是否回滚：否。

## 8. 结论

两个缺陷已完成缺陷级复验并标记 `VERIFIED`，对应最小补丁已进入本地 `dev` 提交。原始 P0 用例仍为 `BLOCKED`，不得宣称财务域完整验收通过或发布就绪。
