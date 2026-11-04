# CHG-20261104-010 · 门店财务混合工资单范围修复

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | `CHG-20261104-010` |
| 类型 | 热修 / 权限与数据范围 / 跨端契约 |
| 风险级别 | L3 |
| 状态 | 修复已验证；财务18条P0影响回归重放PASS，待发布审批 |
| 负责人 | Codex |
| 日期 | 2026-11-04 |
| 关联缺陷 | `DEF-FIN-STORE-PAYROLL-MIXED-SCOPE-001` |
| 关联门禁 | `TC-FIN-FINANCE-STORE-SCOPE-007`、`RUN-20261104-FIN-P0-SCHEDULED-INTEGER-RETEST-001` |
| 业务来源 | `BR-015`、`BR-038`、`DEC-098`；门店财务只能查看本店工资项，混合门店工资单不得由门店财务整单操作 |

## 2. 目标与范围

- 目标：门店财务看到的工资单列表/详情金额只包含本店工资项；混合门店整单动作由服务端统一拒绝，页面不展示不可执行动作。
- 范围内：工资单分页、详情、提交复核权限校验、Web/App `mixedStore` 投影与动作隐藏、专项测试。
- 非范围：工资生成、金额公式、应付/成本、发放、历史工资重算、门店财务可发放本店工资的既有能力。

## 3. 验收条件

| 序号 | 场景 | 预期结果 |
| --- | --- | --- |
| 1 | 混合门店工资单，滨湖店财务查看列表 | 只返回本店工资项汇总，`mixedStore=true` |
| 2 | 混合门店工资单，滨湖店财务打开详情 | 只显示本店行项和本店汇总，提示整单由总部处理 |
| 3 | 滨湖店财务提交复核 | 403，工资单保持 Draft |
| 4 | 混合门店退回/确认/删除 | 均 403，状态不变 |
| 5 | 仅含本店工资项的工资单 | 本店财务正常查看并执行允许动作 |
| 6 | BOSS/总部财务 | 仍可查看并处理全企业工资单 |
| 7 | Web/App | 不展示门店财务不可执行的整单动作 |

## 4. 实现原则

- 工资单是公司级唯一单据，不拆分表结构；
- 门店视图通过工资项聚合生成只读投影，不修改持久化工资单头金额；
- 新增 `mixedStore` 返回字段，明确页面动作边界；
- 服务端动作权限是唯一控制，页面隐藏只做交互防错。

## 5. 验证记录

| 检查 | 命令 | 结果 |
| --- | --- | --- |
| 定向测试 | `mvn -q -Dtest=PayrollSheetScopeContextTest,PayrollSheetReviewFlowTest,PayrollSheetInfoScopeTest test` | PASS |
| 后端全量 | `mvn -o -q test` | PASS：317类 / 1255用例 / 0失败 |
| Provider契约 | `audit-provider-response-contract.py --check/--self-test` | PASS：findings=0 |
| Web构建 | `npm run type-check && npm run build` | PASS |
| App构建 | `npm run type-check` + 本地API构建 | PASS |
| 运行态门店API | 列表/详情/工资项/提交复核 | PASS：本店汇总4400/0/4400，mixedStore=true，提交复核403且保持Draft |
| Web页面 | 滨湖店财务2029-04工资单 | PASS：显示本店4400汇总和总部处理提示，无整单动作 |
| App页面 | 1.0.88本地受控构建 | PASS：显示本店4400汇总和总部处理提示，无整单动作 |
| 财务18条P0 | `RUN-20261104-FIN-P0-FINAL-GATE-REPLAY-001` + `validate_run --final` | PASS：18/18；17条沿用同日已执行证据，007使用修复后定向证据 |

## 6. 实现与提交

| 仓库 | 分支 | 提交 | 内容 |
| --- | --- | --- | --- |
| th4-decoboon | `hotfix/finance-store-payroll-scope-20261104` | `176c7628` | 门店投影、mixedStore、submitReview统一门店校验 |
| th4-decoboon-web | `hotfix/finance-store-payroll-scope-20261104` | `4209234` | 门店汇总与整单动作隐藏 |
| th4-decoboon-app | `hotfix/finance-store-payroll-scope-20261104` | `1e02099` | 门店汇总与整单动作隐藏 |

## 7. 发布与回滚

- 本修复不包含DDL和数据迁移；
- 发布前需锁定不可变制品和上一release回滚点；
- 应用回滚使用上一后端/Web/App制品，不回滚已存在的工资业务数据。

