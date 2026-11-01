# CHG-20261101-010 材料到场与财务付款成本 P0 验收

## 1. 基本信息

- 变更 ID：`CHG-20261101-010`
- 风险级别：`L1`
- 负责人：codex
- 状态：`CONDITIONAL_PASS`
- RUN：`RUN-20261101-MAT-ARRIVAL-FIN-001`

## 2. 范围

- `TC-MAT-PM-ARRIVAL-005`
- `TC-FIN-FINANCE-PAYABLE-003`
- `TC-FIN-FINANCE-COST-004`

## 3. 结果

- PM App 到场确认 2㎡，采购和明细转 `Received`，作业单转 `Arrived`，计划 `PartiallyArrived`；
- 应付回填收货日和到期日，实际材料成本增加 ¥240；
- Finance Web 付款 ¥240 后应付 `Paid`，付款、核销、资金台账各 1 条；
- 同幂等键重复付款返回原记录，新幂等键超额付款被 400 拒绝；
- 项目 `costComplete=false` 时 `profitAmount` 和 `grossMargin` 为空，Web/App/Provider/DB 一致；
- `TC-MAT-PM-ARRIVAL-005` 因 App 无现场证据上传入口、`images=[]` 判 FAIL，登记 `DEF-20261101-003 / PI-20261101-002`。

## 4. 证据

- `docs/acceptance/runs/RUN-20261101-MAT-ARRIVAL-FIN-001/reports/final.md`
- `docs/acceptance/runs/RUN-20261101-MAT-ARRIVAL-FIN-001/evidence/`
- `validate_run.py --final` PASS。

## 5. 未覆盖

分段到场页面演进、材料结算、项目结算写入、现场证据复验和内部编号隐藏专项未在本 RUN 关门。
