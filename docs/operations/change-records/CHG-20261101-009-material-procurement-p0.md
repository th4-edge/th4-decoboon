# CHG-20261101-009 材料采购 P0 验收

## 1. 基本信息

- 变更 ID：`CHG-20261101-009`
- 风险级别：`L1`
- 负责人：codex
- 状态：`ACCEPTANCE_PASS`
- RUN：`RUN-20261101-MAT-PROCURE-P0-001`

## 2. 范围

- `TC-MAT-DESIGNER-SHEET-002`
- `TC-MAT-BOUNDARY-QUANTITY-003`
- `TC-MAT-FINANCE-ORDER-004`

## 3. 结果

- 作业单来源：`sourceKind=Plan`，绑定材料计划 `242541620837220352`；
- 数量 13 超出可下单量 12，被 400 拒绝；合法数量 2 保存成功；
- 测量、提交请购和下单通过真实 Provider 完成；
- 采购单 `POb69125551f7cc1f275a014315ea0b202` 数量 2、金额 ¥240；
- 自动应付 ¥240，与采购单来源号一致；
- 同幂等键重复下单仍只有一张采购单和一条应付；
- Finance Web 主材下单和 App 应付账款均回读同一事实。

## 4. 证据

- `docs/acceptance/runs/RUN-20261101-MAT-PROCURE-P0-001/reports/final.md`
- `docs/acceptance/runs/RUN-20261101-MAT-PROCURE-P0-001/evidence/`
- `validate_run.py --final` PASS。

## 5. 未覆盖

材料到场/入库、实际成本、付款、材料结算、退货/换货和成本完整度未执行。
