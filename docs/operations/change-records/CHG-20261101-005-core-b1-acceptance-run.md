# CHG-20261101-005 核心 B1 独立验收

## 1. 基本信息

- 变更 ID：`CHG-20261101-005`
- 风险级别：`L1`
- 负责人：codex
- 状态：`ACCEPTANCE_PASS`
- 关联：`RUN-20261101-CORE-B1-001`
- 计划：`TP-20261101-V0.1`

## 2. 范围

只读执行 3 条用例：

- `TC-CRM-PERMISSION-STORE-005`
- `TC-CT-BOSS-PROJECT-002`
- `TC-WB-PM-GUIDANCE-002`

未执行报价接受、合同新建/生效写入、材料、验收或资金写入。

## 3. 结果

| 用例 | 结果 |
| --- | --- |
| 门店客户数据隔离 | PASS |
| 合同生效后的项目/阶段/材料/应收派生事实 | PASS |
| 项目经理工作台真实待办与项目上下文 | PASS |

汇总：`3 PASS / 0 FAIL / 0 BLOCKED / 0 NOT_RUN`。

## 4. 证据

- `docs/acceptance/runs/RUN-20261101-CORE-B1-001/reports/final.md`
- `docs/acceptance/runs/RUN-20261101-CORE-B1-001/evidence/`
- `validate_run.py --final`：PASS。

## 5. 结论

本批次只读 P0/P1 核心用例通过。下一步应进入受控写入批次，单独覆盖报价权限、合同签约/生效顺序和合同变更，不得把本批次结论外推到这些未执行用例。
