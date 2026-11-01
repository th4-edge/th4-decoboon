# CHG-20261030-002 · App 月度考勤应出勤天数对齐 Web

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261030-002 |
| 类型 | 需求 / 缺陷 |
| 风险级别 | L3 |
| 状态 | 待验证 |
| 负责人 | Codex |
| 日期 | 2026-10-30 |
| 关联需求/事故/业务裁决 | `USER-DEC-20261029-006`、`CHG-20261029-012`、`BR-038`、`DEC-045` |

## 2. 目标与范围

- 目标：App 月度考勤与 Web 使用同一规则，应出勤从薪酬方案派生，允许财务在 1～31 天内调整，并以考勤快照参与工资计算。
- 范围内：App 月度考勤新增/编辑页、App 薪酬闭环契约校验、薪酬域实施与对账文档。
- 非范围：薪酬方案维护、工资公式、考勤扣款系数、权限矩阵、状态机、员工发放、DDL 和迁移。
- 受影响端：App；后端和 Web 沿用 `CHG-20261029-012` 已发布能力。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：是，涉及考勤快照和工资计算输入。
- 业务事实来源与裁决编号：`USER-DEC-20261029-006`；沿用 `BR-038`、`DEC-045`。
- 尚存 GAP 或假设置：无；无生效薪酬方案时仍阻断考勤保存，不使用 22 兜底。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | App 财务新增月度考勤 | 员工当月有生效薪酬方案 | 应出勤自动带出方案值，输入框可编辑 | App 契约校验 PASS、类型检查 PASS |
| 2 | App 财务调整应出勤 | 已带出方案值 | 可改为 1～31 内整数并保存，回读为调整值 | 定向后端测试 PASS；App 契约校验 PASS |
| 3 | App 无生效方案 | 员工当月无方案 | 禁止保存并提示先配置方案，不显示 22 默认值 | App 契约校验 PASS；页面人工验收待执行 |
| 4 | App 工资计算 | 考勤已确认且工资单生成 | 使用考勤单保存的应出勤快照，不回写薪酬方案 | `PayrollAttendanceScheduledDaysTest`、`PayrollBuildContextScheduledDaysTest` PASS |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | `dev` | 待提交 | `docs/product/decisions/USER-DEC-20261029-006-月度考勤应出勤天数可调整.md`、`docs/product/domains/payroll-labor/` |
| th4-decoboon-app | `dev` | 待提交 | `src/subpackages/payroll/pages/attendance-edit.vue`、`scripts/verify-payroll-closure-contract.mjs` |
| th4-decoboon-web | `dev` | 沿用 `49f4b35` | `FinanceAttendanceDrawer.vue` 已发布 |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| App 验证汇总 | App type-check、契约、文案、UI 门禁 | PASS | `docs/acceptance/runs/20261030-payroll-attendance-app-align/app-verification.log` |
| 定向测试 | `mvn -q -Dtest=PayrollAttendanceScheduledDaysTest,PayrollAttendanceInfoScopeTest,PayrollBuildContextScheduledDaysTest,PayrollCalculationContextTest,PayrollConfirmCommissionDecoupledTest,PayrollSheetReviewFlowTest,PayrollEmployeePaymentAccessTest test` | PASS，17 tests / 0 failures / 0 errors / 0 skipped | `docs/acceptance/runs/20261030-payroll-attendance-app-align/backend-verification.txt` |
| 全量后端回归 | JDK 22 `mvn -q test` | PASS，1129 tests / 0 failures / 0 errors / 0 skipped | `docs/acceptance/runs/20261030-payroll-attendance-app-align/backend-verification.txt`；完整日志 `/tmp/chg-20261030-002-jdk22-full.log` |
| App 契约校验 | `node scripts/verify-payroll-closure-contract.mjs` | PASS | 本地命令输出 |
| App 类型检查/构建 | `npm run type-check`、`npm run build:mp-weixin` | PASS，小程序构建完成 | 本地命令输出 |
| App 文案/UI 规范 | `npm run check:client-copy`、`npm run check:ui` | PASS | 本地命令输出 |
| Web 类型检查回归 | `npm run type-check` | PASS | 本地命令输出 |
| 真实页面/接口冒烟 | App 月度考勤编辑页 | 待执行 | 验收记录 |
| 负向/越权/边界 | 无方案、0/32 天、工资项已生成 | 待执行 | 验收记录 |

- 未执行项及原因：未使用真实财务账号做 App 页面提交和回读；按试运营安全边界，避免污染真实薪酬数据。
- 已知失败及是否与本次相关：无。

## 7. 发布

| 字段 | 内容 |
| --- | --- |
| Release ID | 未发布 |
| 制品路径/SHA-256 | 未生成 |
| 生产 release/版本 | 未发布 |
| 上一版本/回滚点 | 沿用 `CHG-20261029-012`：后端 `/opt/decoboon/releases/20261027-172000`；Web `/opt/decoboon-web/releases/20261027-162200` |
| 健康检查 | 待发布 |
| 发布后观察 | 待发布 |

- 数据库迁移：无
- 配置/密钥变化：无

## 8. 回滚

- 回滚条件：App 保存的应出勤快照与 Web 不一致、无方案仍可保存、或工资计算结果异常。
- 回滚命令/步骤：回退 App 文件与契约脚本；后端和 Web 保持 `CHG-20261029-012` 已发布版本。
- 回滚验证：App 月度考勤恢复为只读派生，Web/后端考勤事实不受影响。
- 实际是否回滚：否

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集
- 人工耗时：未采集
- 一次通过：是
- 返工原因：无
- 最终结论：待真实页面验证后关闭

## 10. 复盘与沉淀

- 是否出现重复问题：是，App 与 Web 对同一应出勤字段曾分别采用只读和可编辑口径。
- 是否需要新增门禁：是，App 薪酬闭环契约增加 `scheduledDays` 可编辑、提交和 1～31 校验。
- 候选 `EXP/ANT/PATH`：派生默认值允许业务覆盖时，客户端必须明确提交覆盖快照，服务端不得再次回写方案。
- 后续动作与负责人：完成真实 App 页面验收后更新 `payroll-labor` 状态与对账矩阵。
