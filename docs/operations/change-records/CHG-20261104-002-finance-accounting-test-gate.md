# CHG-20261104-002 · 财务账务业务域专项测试门禁

## 1. 基本信息

| 字段 | 字段值 |
| --- | --- |
| 变更 ID | `CHG-20261104-002` |
| 类型 | 测试治理与发布门禁 |
| 风险级别 | L1 |
| 状态 | 门禁已扩展至表单生命周期、真实交互证据和旧响应竞态；Web/App 定向执行完成，通用财务表单发现薪酬方案切员工污染，待修复 |
| 负责人 | Codex |
| 日期 | 2026-11-04 |
| 关联缺陷 | `CHG-20261104-001`、`EV-433`、考勤体系审计 `ATT-01~ATT-12`、`DEF-FIN-FORM-001`、`DEF-FIN-COMMISSION-FORM-001` |
| 业务来源 | `BR-015`、`BR-038`、`BR-050`、`BR-055`、`BR-058`、`DEC-043`、`DEC-065`、`DEC-092`、`DEC-093`、`DEC-098`、`DEC-099` |

## 2. 目标与范围

- 目标：把工资单/考勤锁定缺陷抽象为财务账务域长期测试门禁，约束后续发布必须验证状态机、权限、金额、幂等、跨端和账务对账。
- 范围内：
  - 财务账务专项门禁规范；
  - 26 条机器可读 P0/P1 用例（17 条 P0）；
  - 双企业、多门店、工资单状态组合和金额边界夹具；
  - Provider/页面/业务规则校验；
  - 覆盖矩阵和汇总输出；
  - 用例契约负向自测；
  - 页面交互证据不可替代规则；
  - 表单重开、切换对象和旧响应竞态强制用例；
  - Web/App 真实页面定向验证。
- 非范围：
  - 不修改业务规则、金额公式、权限或生产数据；
  - 不在本变更中执行全部真实页面/真机用例；
  - 不替代独立验收和业务签字。

## 3. 强制门禁

- P0 用例 100% 通过；
- P1 无未接受阻断；
- 页面、Provider、DB/账务不变量证据齐全；
- App/Web/Provider 同源；
- Web/App 页面 PASS 必须有真实交互步骤、状态回读和可解析 `page-result`，API/SQL 不能替代；
- 重开、切换员工/月份/项目/账户、旧响应竞态和最终提交对象一致性必须覆盖；
- 金额、应付、付款、成本、台账逐层勾稽；
- 跨企业、跨门店、跨团队权限正反验证；
- 并发、重复提交、失败恢复有证据。

## 4. 交付物

| 文件 | 用途 |
| --- | --- |
| `docs/testing/05-finance-accounting-test-gate.md` | 长期门禁规范 |
| `docs/testing/plans/TP-20261104-FIN-ACCOUNTING-GATE.md` | 执行计划 |
| `docs/testing/cases/finance-accounting-gate-v0.1.json` | 26 条机器可读用例 |
| `docs/testing/cases/finance-accounting-gate-v0.1.md` | 人工可读用例目录与明细 |
| `docs/testing/traceability/finance-accounting-gate-oracle.json` | 财务门禁规则 oracle |
| `docs/testing/data/finance-accounting-fixtures.md` | 组织、状态、金额和并发夹具 |
| `docs/testing/scripts/finance_gate.py` | 校验、矩阵和负向自测 |
| `docs/testing/traceability/finance-accounting-gate-matrix.csv` | 用例追溯矩阵 |
| `docs/testing/traceability/finance-accounting-gate-summary.json` | 覆盖汇总 |

## 5. 验证

| 检查 | 命令 | 结果 |
| --- | --- | --- |
| 财务门禁校验 | `python3 docs/testing/scripts/finance_gate.py validate` | PASS：26 条用例，17 条 P0，550 个 Provider |
| 门禁负向自测 | `python3 docs/testing/scripts/finance_gate.py self-test` | PASS：6 类负向门禁被识别，含缺少交互证据、缺少强制表单用例和端面目标不一致 |
| 用例 Schema | `jsonschema.validate` + `test-case.schema.json` | PASS：26/26 |
| Web RUN 校验 | `validate_run.py docs/acceptance/runs/RUN-20261104-FIN-FORM-WEB-001` | PASS：结构 0 错误；PASS=2 / FAIL=1 |
| App RUN 校验 | `validate_run.py docs/acceptance/runs/RUN-20261104-FIN-FORM-APP-001` | PASS：结构 0 错误；PASS=1 |
| RUN 页面门禁负向自测 | `validate_run.py --self-test` | PASS：API-only 不被判页面证据，截图/页面结果和交互回读可解析 |
| 既有测试资产校验 | `python3 docs/testing/scripts/test_assets.py validate` | PASS：44 条 V0.1 用例、6 个动作、74 条规则 |
| 既有测试资产自测 | `python3 docs/testing/scripts/test_assets.py self-test` | PASS：5 类负向门禁被识别 |

## 6. 发布影响

- 涉及财务域后续发布时，`finance_gate.py validate` 和 `self-test` 必须通过；
- P0 失败不得发布；
- P1 失败必须修复或取得业务负责人书面接受；
- 生产真实企业默认只读，破坏性用例只在专用测试租户执行。

## 7. 结论

开发侧门禁资产已建立并通过结构与负向校验。Web 已验证考勤重开/切员工/切月份、旧响应竞态以及费用/收款/付款重开；App 已验证考勤和费用重开、薪酬方案切员工。通用财务表单先后发现 `DEF-FIN-FORM-001` 和 `DEF-FIN-COMMISSION-FORM-001` 两个切换对象污染缺陷，均已修复，Web已发布生产，App 1.0.87开发版已上传。`RUN-20261104-FIN-P0-CURRENT-FINAL-001` 已覆盖全部17条P0，结果 `PASS=14 / FAIL=0 / BLOCKED=3`；最终门禁仍被App门店范围、App跨租户和App旧响应竞态三项页面证据阻断。
