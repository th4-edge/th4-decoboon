# CHG-20261101-030 · 财务工资单草稿删除按数据范围

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | `CHG-20261101-030` |
| 类型 | 权限与数据范围修正 |
| 风险级别 | L3 |
| 状态 | 待验证 |
| 负责人 | Codex |
| 日期 | 2026-11-01 |
| 关联需求/业务裁决 | `DEC-098`、`EV-430`、`DEC-097`、`DEC-093`、`DEC-065`、`DEC-081` |

## 2. 目标与范围

- 目标：财务工资单 `Draft` 删除按财务数据范围开放，并与最终确认权限解耦。
- 范围内：总部财务全企业删除 Draft；门店财务仅本店删除工资项全部属于本店的 Draft；Web/App 删除入口同步。
- 非范围：不开放财务最终确认、退回、作废、企业治理；不改变工资计算、应付、成本、发放、工资条和金额公式。
- 受影响端：后端 / Web / App。

## 3. 业务与决策

- 是否涉及业务对象、状态、权限、金额、成本、利润、支付或数据范围：是。
- 业务事实来源与裁决编号：`DEC-098`、`EV-430`；财务数据范围沿用 `DEC-065`、`DEC-081`。
- 尚存 GAP 或假设置：无新增业务歧义；门店财务仅本店、混合门店整单拒绝由后端强制。

## 4. 验收条件

| 序号 | 场景/角色 | 前置条件 | 预期结果 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | 总部财务删除 Draft | 默认总店、企业数据范围 | 可删除无财务事实 Draft | 后端定向测试；Web 草稿详情显示删除入口 |
| 2 | 门店财务删除本店 Draft | 普通门店、Store 数据范围 | 可删除；仅能看到本店工资项 | 后端定向测试；真实多店独立验收待执行 |
| 3 | 门店财务操作混合门店工资单 | 含其他门店工资项 | 403，不删除任何记录 | `PayrollManagerActionAccessTest` |
| 4 | 财务点击删除草稿 | Web/App | 显示删除入口并二次确认 | Web 真实页面二次确认；App 契约与构建 |
| 5 | 财务尝试最终确认/退回 | Finance | 403，不扩大权限 | `PayrollConfirmRoleTest`、`PayrollManagerActionAccessTest` |
| 6 | 非 Draft/有财务事实 | Pending/Confirmed/已生成应付 | 409，拒绝物理删除 | `PayrollSheetDeleteGuardTest` |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | dev | 未提交 | `PayrollAccessContext`、`PayrollManagerActionAccessTest`、`PayrollItemPageScopeTest` |
| th4-decoboon-web | 工作区 | 未提交 | `payrollAccess.ts`、`FinanceEmployeeSalariesView.vue` |
| th4-decoboon-app | 工作区 | 未提交 | `permission.ts`、工资单列表/详情、薪酬闭环契约脚本 |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| 后端定向测试 | `mvn -Dtest=PayrollManagerActionAccessTest,PayrollItemPageScopeTest,PayrollSheetDeleteGuardTest,PayrollConfirmRoleTest,PayrollSheetReviewFlowTest test` | PASS，12/12 | `target/surefire-reports` |
| 后端全量测试 | `mvn test` | 1203 项，5 失败、1 错误；均为此前工作区既有基线 | `target/surefire-reports` |
| Web 类型检查/构建 | `npm run type-check`、`npm run build` | PASS | `dist/` |
| App 类型检查/构建 | `npm run type-check`、`npm run build:mp-weixin -- --mode development` | PASS | `dist/build/mp-weixin` |
| App 薪酬契约 | `node scripts/verify-payroll-closure-contract.mjs` | PASS | 脚本输出 |
| Provider 契约 | `python3 scripts/audit-provider-response-contract.py --check` | PASS：547 providers，0 findings | 脚本输出 |
| 真实页面/接口冒烟 | Web `/finance/employee-salaries`，吴濛濛（总部财务） | PASS：Draft 显示“删除草稿”，无“确认工资单”；取消二次确认未删数据 | `docs/acceptance/evidence/finance-payroll-draft-delete-20261101/README.md` |

- 未执行项及原因：门店财务真实多店页面黑盒与独立验收未执行。
- 已知失败及是否与本次相关：`StatisticStoreAggregateContextTest` 3、`DesignerQuoteStoreManagerScopeTest` 1、`ProviderSecurityAcceptanceTest` 1、`PayrollCompensationScopeTest` 1（error），均为变更前工作区已有失败；本次定向测试未使用这些路径。

## 7. 发布

| 字段 | 内容 |
| --- | --- |
| Release ID | 不适用（本地开发验收） |
| 制品路径/SHA-256 | `target/th4-decoboon-1.0.73-build.tar.gz` / `326bcec558fbe1cfa74ce34ce4acd0e514eb24ae8a50f9e86fe1ce0f9d43d97b` |
| 生产 release/版本 | 不适用 |
| 上一版本/回滚点 | 代码回退；本地数据库快照 |
| 健康检查 | PASS：本地后端 8091、Web 5174 可访问；吴濛濛总部财务页面加载真实工资单 |
| 发布后观察 | 不适用（未发布生产） |

- 数据库迁移：无。
- 配置/密钥变化：无。
- 本地备份：`data/backups/prod-import/20261101-213046/payroll-draft-delete-20261101/decoboon_prod_20261101-before-finance-draft-delete.sql.gz`，SHA-256 `ace9b67e0b270e6810b6036530662fb567d9b639b83ab22705c6f2f952abb7a5`。

## 8. 回滚

- 回滚条件：总部财务仍 403、门店财务跨店成功、财务获得最终确认/退回、混合门店 Draft 被删除。
- 回滚命令/步骤：回退本变更涉及的后端权限上下文、Web/App 删除入口和 App 能力矩阵；恢复数据库仅用于本地验收数据损坏。
- 回滚验证：财务确认/退回仍 403；BOSS/店长既有动作不变；门店财务跨店拒绝。
- 实际是否回滚：否。

## 9. 成本与结论

- 智能体运行/Token 摘要：未采集。
- 人工耗时：未采集。
- 一次通过：否（首次测试缺少 `Company` 导入，已修正）。
- 返工原因：测试夹具补充数据范围 mock 与导入。
- 最终结论：开发自测通过，待用户进行本地独立验收。

## 10. 复盘与沉淀

- 是否出现重复问题：否。
- 是否需要新增门禁：是，已更新 App 薪酬闭环契约脚本，分离 `confirm:payroll` 与 `delete:draftPayroll`。
- 候选 `EXP/ANT/PATH`：无。
- 后续动作与负责人：用户在 Web/App 使用吴濛濛账号验收总部财务 Draft 删除；门店财务另用多门店测试账号独立验收。
