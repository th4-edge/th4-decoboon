# CHG-20261101-003 财务 P0 独立验收首批执行

## 1. 基本信息

- 变更 ID：`CHG-20261101-003`
- 风险级别：`L1`
- 负责人：codex
- 状态：`ACCEPTANCE_FAIL`
- 关联：`CHG-20261101-002`、`PI-20261101-001`

## 2. 目标与范围

- 目标：使用独立验收执行智能体执行首轮财务 P0 页面验收。
- 用例：`TC-FIN-FINANCE-COST-004`、`TC-FIN-CROSSSURFACE-PARITY-006`。
- 非范围：修复代码、全量验收、真实业主确认、真实收付款、发布。

## 3. 环境

- RUN：`RUN-20261101-FIN-P0-001`
- 环境：本地 `local-fixture`
- 测试企业：昭青科技 `199500000000000001`
- 项目：`242297299361857536`
- 端面：Web、App；角色：Boss、StoreManager、Finance。

## 4. 执行结果

| 用例 | 结果 | 结论 |
| --- | --- | --- |
| `TC-FIN-FINANCE-COST-004` | PASS | 成本不完整时利润为空，Web 结算确认禁用，API/DB 一致 |
| `TC-FIN-CROSSSURFACE-PARITY-006` | FAIL | 业务金额一致，但 App 结算按钮门禁与金额展示精度未与 Web 对齐 |

汇总：`2` 条执行，`1 PASS / 1 FAIL / 0 BLOCKED / 0 NOT_RUN`。

## 5. 缺陷

- `DEF-20261101-001 P1`：成本不完整时 App “确认结算”按钮未禁用，Web 已禁用。
- `DEF-20261101-002 P2`：App 金额取整到元，Web 精确到分；页面数据仍为精确值。

## 6. 证据

- `docs/acceptance/runs/RUN-20261101-FIN-P0-001/reports/final.md`
- `docs/acceptance/runs/RUN-20261101-FIN-P0-001/evidence/TC-FIN-FINANCE-COST-004/`
- `docs/acceptance/runs/RUN-20261101-FIN-P0-001/evidence/TC-FIN-CROSSSURFACE-PARITY-006/`
- `validate_run.py --final`：PASS，证据和缺陷引用完整。

## 7. 结论与下一步

`FAIL`：本轮为 P0 用例范围内失败，不能宣称财务跨端验收通过。下一步由研发修复 `PI-20261101-001`，之后创建新 `retest` RUN 复验；不得用开发自测替代页面复验。
