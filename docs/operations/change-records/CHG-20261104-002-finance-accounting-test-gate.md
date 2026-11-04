# CHG-20261104-002 · 财务账务业务域专项测试门禁

## 1. 基本信息

| 字段 | 字段值 |
| --- | --- |
| 变更 ID | `CHG-20261104-002` |
| 类型 | 测试治理与发布门禁 |
| 风险级别 | L1 |
| 状态 | 门禁已扩展至表单生命周期；旧最终 RUN 标记 STALE，Web重开/切员工/切月份已通过，App/竞态/通用表单待执行 |
| 负责人 | Codex |
| 日期 | 2026-11-04 |
| 关联缺陷 | `CHG-20261104-001`、`EV-433`、考勤体系审计 `ATT-01~ATT-12` |
| 业务来源 | `BR-015`、`BR-038`、`BR-050`、`BR-055`、`BR-058`、`DEC-043`、`DEC-065`、`DEC-092`、`DEC-093`、`DEC-098`、`DEC-099` |

## 2. 目标与范围

- 目标：把工资单/考勤锁定缺陷抽象为财务账务域长期测试门禁，约束后续发布必须验证状态机、权限、金额、幂等、跨端和账务对账。
- 范围内：
  - 财务账务专项门禁规范；
  - 22 条机器可读 P0/P1 用例；
  - 双企业、多门店、工资单状态组合和金额边界夹具；
  - Provider/页面/业务规则校验；
  - 覆盖矩阵和汇总输出；
  - 用例契约负向自测。
- 非范围：
  - 不修改业务规则、金额公式、权限或生产数据；
  - 不在本变更中执行全部真实页面/真机用例；
  - 不替代独立验收和业务签字。

## 3. 强制门禁

- P0 用例 100% 通过；
- P1 无未接受阻断；
- 页面、Provider、DB/账务不变量证据齐全；
- App/Web/Provider 同源；
- 金额、应付、付款、成本、台账逐层勾稽；
- 跨企业、跨门店、跨团队权限正反验证；
- 并发、重复提交、失败恢复有证据。

## 4. 交付物

| 文件 | 用途 |
| --- | --- |
| `docs/testing/05-finance-accounting-test-gate.md` | 长期门禁规范 |
| `docs/testing/plans/TP-20261104-FIN-ACCOUNTING-GATE.md` | 执行计划 |
| `docs/testing/cases/finance-accounting-gate-v0.1.json` | 22 条机器可读用例 |
| `docs/testing/cases/finance-accounting-gate-v0.1.md` | 人工可读用例目录与明细 |
| `docs/testing/traceability/finance-accounting-gate-oracle.json` | 财务门禁规则 oracle |
| `docs/testing/data/finance-accounting-fixtures.md` | 组织、状态、金额和并发夹具 |
| `docs/testing/scripts/finance_gate.py` | 校验、矩阵和负向自测 |
| `docs/testing/traceability/finance-accounting-gate-matrix.csv` | 用例追溯矩阵 |
| `docs/testing/traceability/finance-accounting-gate-summary.json` | 覆盖汇总 |

## 5. 验证

| 检查 | 命令 | 结果 |
| --- | --- | --- |
| 财务门禁校验 | `python3 docs/testing/scripts/finance_gate.py validate` | PASS：22 条用例，14 条 P0，550 个 Provider |
| 门禁负向自测 | `python3 docs/testing/scripts/finance_gate.py self-test` | PASS：重复 ID、未知规则、未知 Provider 均被识别 |
| 用例 Schema | `jsonschema.validate` + `test-case.schema.json` | PASS：22/22 |
| 专项 RUN 创建 | `create_run.py --suite finance` | PASS：预检 22 条、P0/P1=14/8 |
| RUN 结构校验 | `validate_run.py <RUN>` | PASS：PLANNED 状态下 0 错误 |
| 既有测试资产校验 | `python3 docs/testing/scripts/test_assets.py validate` | PASS：44 条 V0.1 用例、6 个动作、74 条规则 |
| 既有测试资产自测 | `python3 docs/testing/scripts/test_assets.py self-test` | PASS：5 类负向门禁被识别 |

## 6. 发布影响

- 涉及财务域后续发布时，`finance_gate.py validate` 和 `self-test` 必须通过；
- P0 失败不得发布；
- P1 失败必须修复或取得业务负责人书面接受；
- 生产真实企业默认只读，破坏性用例只在专用测试租户执行。

## 7. 结论

开发侧门禁资产已建立并通过结构与负向校验。`RUN-20261104-FIN-P0-001` 已启动，第一批结果 `PASS=0 / FAIL=3 / BLOCKED=4 / NOT_RUN=7`；已确认状态机绕过、已确认考勤修改、门店财务跨店三个阻断缺陷。财务域不得标记通过，后续需继续执行并修复复验。
