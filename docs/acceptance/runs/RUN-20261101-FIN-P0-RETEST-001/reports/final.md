# RUN-20261101-FIN-P0-RETEST-001 独立复验报告

## 运行信息

- 原 RUN：`RUN-20261101-FIN-P0-001`
- Plan：`TP-20261101-V0.1`
- Oracle：`ZDQ-BIZ-20261101-V0.1`
- 环境：本地 `local-fixture`
- 项目：`242297299361857536`
- 复验缺陷：`DEF-20261101-001`、`DEF-20261101-002`

## 执行结果

| 总数 | PASS | FAIL | BLOCKED | NOT_RUN |
| ---: | ---: | ---: | ---: | ---: |
| 2 | 2 | 0 | 0 | 0 |

- `TC-FIN-FINANCE-COST-004`：PASS。App/Web成本不完整时利润为空，确认结算禁用。
- `TC-FIN-CROSSSURFACE-PARITY-006`：PASS。Boss、店长、财务Web与Finance App的合同/回款/待收/应付/已付/成本金额一致到分，确认结算控件状态一致。

## 缺陷复验

- `DEF-20261101-001`：VERIFIED。App确认结算按钮现为 `disabled=true`，显示“项目最终成本未完整，补齐成本后才能确认结算”。
- `DEF-20261101-002`：VERIFIED。App金额显示为 `¥2,495.75 / ¥1,148.05 / ¥6,347.70`，与Web精确值一致。

## 跨端对账

- 合同金额：Web/App `¥2,495.75`
- 累计回款：Web/App `¥1,148.05`
- 待收金额：Web/App `¥6,347.70`
- 应付成本：Web/App `¥3,440.00`
- 已付金额：Web/App `¥1,440.00`
- 项目总成本：Web/App `¥2,240.00`
- 成本完整：Web/App/API/DB `false`
- 利润与毛利率：App页面数据、API、DB均为空
- 结算状态：Web/App/API/DB `Draft`

## 结论

`PASS`

本结论只证明该两项P0用例及其原缺陷复验通过；不代表全部财务、所有角色、所有端或发布验收通过。正式发布仍需用户授权。
