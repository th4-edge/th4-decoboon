# CHG-20261030-004 · 薪酬入口权限与门店范围对齐

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | CHG-20261030-004 |
| 类型 | 缺陷 / 权限 |
| 风险级别 | L3 |
| 状态 | 待验证 |
| 负责人 | Codex |
| 日期 | 2026-10-30 |
| 关联需求/事故/业务裁决 | `BR-038`、`DEC-093`、`GAP-073`、`PI-20261023-001` |

## 2. 目标与范围

- 目标：App/Web 提成写入入口与后端 `edit:payroll` 权限一致；Web 月度考勤和提成计提使用当前门店上下文。
- 范围内：App 提成列表按钮门控、Web 考勤抽屉 `storeId`、Web 提成抽屉 `storeId`、对应 API 参数和契约门禁。
- 非范围：后端角色矩阵、工资公式、员工发放、员工自助、敏感字段裁剪、作废/批量/部分发放、DDL。
- 受影响端：App、Web。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：是，涉及薪酬写权限和门店范围。
- 业务事实来源与裁决编号：`BR-038`、`DEC-093`、`GAP-073`、`PI-20261023-001`。
- 尚存 GAP 或假设置：员工本人提成/考勤、店长个税明细边界仍待后续批次。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | App 店长打开员工提成 | 本店只读 | 不显示“计提”入口；直接深链仍被 `edit:payroll` 路由拦截 | App 契约、type-check、构建 |
| 2 | App 财务/BOSS 打开员工提成 | 有 `edit:payroll` | 显示计提入口并可进入 | App 契约、type-check |
| 3 | Web 财务在单店范围打开月度考勤 | 当前门店已选 | 考勤记录和员工选择器按当前门店加载 | Web type-check/build；后端范围测试 |
| 4 | Web 财务在单店范围计提提成 | 当前门店已选 | 员工选择器只展示当前门店员工；保存仍由服务端范围裁决 | Web type-check/build；后端范围测试 |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon-app | `dev` | 待提交 | `subpackages/payroll/pages/commission-list.vue`、薪酬契约脚本 |
| th4-decoboon-web | `dev` | 待提交 | `FinanceAttendanceDrawer.vue`、`FinanceCommissionDrawer.vue`、员工工资/提成页面、`domains/finance/api.ts` |
| th4-decoboon | `dev` | 不适用 | 仅文档和验证证据 |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| 后端范围测试 | `mvn -q -Dtest=PayrollAttendanceInfoScopeTest,PayrollCompensationScopeTest,PayrollEmployeePaymentAccessTest test` | PASS，6/6 | `docs/acceptance/runs/20261030-payroll-permission-store-scope/backend-verification.txt` |
| 后端全量回归（沿用同一后端代码） | JDK 22 `mvn -q test` | PASS，1130 tests / 0 failures / 0 errors / 0 skipped | `docs/acceptance/runs/20261030-payroll-employee-payment-align/backend-verification.txt` |
| App 契约 | `node scripts/verify-payroll-closure-contract.mjs` | PASS | `docs/acceptance/runs/20261030-payroll-permission-store-scope/frontend-verification.txt` |
| App type-check/构建 | `npm run type-check`、`npm run build:mp-weixin` | PASS | 同上 |
| Web type-check/构建 | `npm run type-check`、`npm run build` | PASS | 同上 |
| 真实页面 | App 店长/财务、Web 单店财务 | 待执行 | 验收记录 |

- 未执行项及原因：未使用真实店长/财务账号做单店接口和页面回读；需受控测试企业。
- 已知失败及是否与本次相关：无。

## 7. 发布

| 字段 | 内容 |
| --- | --- |
| Release ID | 未发布 |
| 制品路径/SHA-256 | 未生成 |
| 生产 release/版本 | 未发布 |
| 上一版本/回滚点 | 沿用 `CHG-20261030-003` 对应回滚点 |
| 健康检查 | 待发布 |
| 发布后观察 | 待发布 |

- 数据库迁移：无
- 配置/密钥变化：无

## 8. 回滚

- 回滚条件：App 店长仍能看到计提入口、Web 单店考勤/提成仍出现跨店员工或记录。
- 回滚命令/步骤：回退本 CHG 的 App/Web 文件；后端权限和薪酬事实不变。
- 回滚验证：原有薪酬查看与财务写权限恢复，服务端权限兜底保持。
- 实际是否回滚：否

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集
- 人工耗时：未采集
- 一次通过：待验证
- 返工原因：待验证
- 最终结论：待真实角色/门店页面验证后关闭

## 10. 复盘与沉淀

- 是否出现重复问题：是，前端入口曾显示但路由/后端不接受。
- 是否需要新增门禁：是，App 契约检查 `edit:payroll` 入口门控；Web 通过门店参数和构建门禁覆盖。
- 候选 `EXP/ANT/PATH`：前端按钮可见性必须由能力键驱动，不能按页面角色名称猜测。
- 后续动作与负责人：完成真实店长/财务门店验证后再处理敏感字段和员工自助。
