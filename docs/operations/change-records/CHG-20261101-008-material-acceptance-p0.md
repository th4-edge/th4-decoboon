# CHG-20261101-008 材料选材与员工验收权限 P0 验收

## 1. 基本信息

- 变更 ID：`CHG-20261101-008`
- 风险级别：`L1`
- 负责人：codex
- 状态：`CONDITIONAL_PASS`
- RUN：`RUN-20261101-MAT-ACC-P0-001`

## 2. 范围

- `TC-MAT-DESIGNER-SELECTION-001`
- `TC-ACC-PERMISSION-EMPLOYEE-002`

## 3. 结果

### 材料选材门禁 PASS

- 未选材建作业单被 400 拒绝；
- Plan 保持 `Planned`，无作业单；
- Web选材保存并交付后，Plan 转 `ReadyToOrder`；
- 随后成功生成 Draft 作业单 `1793517336944847641`。

### 员工代确认验收 PASS

- Boss、店长、财务、项目经理调用验收确认均 403；
- 验收保持 Pending，confirmed_at/confirmed_by 为空；
- PM Web 页面没有员工确认/驳回动作。

## 4. 限制

App选材页和员工验收页未在本 RUN 单独执行；Web/API/DB证据完整。结论为 `CONDITIONAL_PASS`，不能宣称材料/验收全端验收完成。

## 5. 证据

- `docs/acceptance/runs/RUN-20261101-MAT-ACC-P0-001/reports/final.md`
- `docs/acceptance/runs/RUN-20261101-MAT-ACC-P0-001/evidence/`
- `validate_run.py --final` PASS。
