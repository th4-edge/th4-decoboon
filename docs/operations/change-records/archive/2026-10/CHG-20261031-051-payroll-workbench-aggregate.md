# CHG-20261031-051 · 薪酬工作台聚合 SQL 化

## 1. 基本信息

| 字段 | 内容 |
| --- | --- |
| 变更 ID | `CHG-20261031-051` |
| 类型 | 后端读模型 / 聚合查询 / 性能优化 |
| 风险级别 | L2（薪酬金额统计口径与只读工作台查询，无 DDL/权限/状态机变化） |
| 状态 | DEV_VERIFIED / RENDER_AND_10X_PERF_PASS |
| 负责人 | codex |
| 日期 | 2026-10-31 |
| 关联需求/事故/业务裁决 | `CHG-20261031-050`、`ANS-BIZ-20261031-001`、`BR-015`、`BR-025`、`BR-031`、`DEC-036`、`DEC-037` |

## 2. 目标与范围

- 目标：把 `payroll/workbench/report` 从全量实体加载后 Java 统计改为 6 组有界聚合/投影查询，并修正负余额冲减问题。
- 范围内：考勤待处理数、工资单状态数、最近工资单、员工工资/提成应付、已释放未付提成、工资条状态。
- 非范围：工资公式、薪酬方案、考勤写动作、应付生成、发放、成本、权限、状态机、DDL、生产发布。
- 受影响端：后端；App/Web 响应字段不变，真实页面回读待补。

## 3. 业务与决策

- 用户 2026-10-31 确认按建议执行，形成 `ANS-BIZ-20261031-001`。
- 待处理考勤保持 `Pending/Disputed`；提成应付跨期间；未付金额逐条 `max(金额-已付,0)`；最近工资单按 `period DESC, sheetId DESC`。
- 业务事实来源：`BR-015`、`BR-025`、`BR-031`、`DEC-036`、`DEC-037`。
- 生产等价数据下的本地 MySQL 10×/20 并发压测和 Web/App 真实渲染回读已执行；待生产候选环境复测与独立视觉/业务验收。

## 4. 验收条件

| 序号 | 场景 | 预期结果 | 证据 |
| --- | --- | --- | --- |
| 1 | 同租户、门店、期间、数据 | 旧/新工作台字段逐项可解释 | HTTP 集成测试 |
| 2 | 存在负数未付金额 | 单条按 0，不冲减其他应付/提成 | `pendingEmployeePayableAmount=80.00` |
| 3 | 10× 应付/提成 | SQL 与实体加载数不增长 | 14 SQL / 2 entity loads |
| 4 | 权限/门店 | 企业/本店范围不变，本店成功、跨店 403 | `FinanceStoreScopeHttpAcceptanceTest` |
| 5 | 静态门禁 | 汇总聚合类纳入门禁，无 A1/A2 新违规 | `SummaryImplementationRulesTest` |

## 5. 实现与提交

| 仓库 | 分支 | 提交 | 主要文件/模块 |
| --- | --- | --- | --- |
| th4-decoboon | `dev` | 未提交工作区 | `PayrollWorkbenchContext.java`、`PayrollWorkbenchAggregateContext.java`、相关测试 |

## 6. 验证

| 检查 | 命令/页面 | 结果 | 证据位置 |
| --- | --- | --- | --- |
| 编译 | `mvn -q -DskipTests compile` | PASS | 命令输出 |
| 定向回归 | `mvn -q -Dtest=PayrollWorkbenchContextTest,FinanceStoreScopeHttpAcceptanceTest,SummaryImplementationRulesTest test` | PASS，33/0/0/0 | `docs/acceptance/runs/20261031-pay-ops-01/raw/targeted-tests.log` |
| 全量后端 | `mvn -q test` | PASS，1188/0/0/0 | `raw/backend-full-test.log.gz` |
| 改造前基线 | 真实 HTTP Provider + Hibernate statistics | 15 SQL / 22 entity loads / 493B | `raw/baseline-with-data.log` |
| 改造后 | 真实 HTTP Provider + Hibernate statistics | 14 SQL / 2 entity loads / 493B | `raw/after-with-data.log` |
| 10× 数据 | 50 条新增应付 + 50 条新增提成 | 14 SQL / 2 entity loads，不增长 | `raw/after-10x.log` |
| Provider 响应审计 | `python3 scripts/audit-provider-response-contract.py --check` | PASS，543 providers / 0 findings | 命令输出 |
| Web 真实页面 | Ego Lite，`/finance/payroll?period=2026-10` | PASS；¥1,600、考勤2、草稿1；console error 0 | `raw/web-current-readback.json` |
| Web 本地 MySQL 采样 | 浏览器 API 20 次 | P50 41.9ms / P95 66.1ms / max 71.2ms / 491B | `raw/web-current-readback.json` |
| Web 三档视口 | 1440/1280/1024 | 无横向溢出 | `raw/web-payroll-current-*.png` |
| App 原生页面 | 微信开发者工具 automator | 5/5 PASS；与 Web 同为 ¥1,600、考勤2、草稿1 | `raw/app-payops-check.json` |
| 视觉审查 | 截图 | `VISUAL_REVIEW_UNAVAILABLE`，模型无图片输入 | 三档截图 |
| 10× 顺序压测 | 3 次独立运行 ×30 请求 | P95/P99 中位 21.87/22.43ms，错误率 0 | `raw/mysql-10x-summary.json` |
| 10× 并发压测 | 3 次独立运行 ×20 并发×200 请求 | P95/P99 中位 28.19/29.75ms，max 31.78ms，974.26 RPS，错误率 0 | `raw/mysql-10x-summary.json` |
| 查询结构 | MySQL `Questions` 单请求增量 | 基线 22、10× 22 | `raw/mysql-10x-run1..3.json` |
| 测试数据清理 | 清理前/后 SQL | 全部目标记录 0 | `raw/cleanup-*.txt` |
| 差异检查 | `git diff --check` | PASS | 工作区 |

已知限制：聚合结构证据来自 H2；Web/App 页面回读与生产等价数据量的本地 MySQL 10×/20 并发压测已通过，但不等同生产机器规格、网络和并发，且尚无具备图片输入能力的独立视觉评审。

## 7. 发布

- Release ID：不适用
- 制品/SHA：不适用
- 数据库迁移：无
- 配置/密钥变化：无
- 发布结论：未发布

## 8. 回滚

- 回滚条件：金额统计或权限范围与已确认口径不一致。
- 回滚步骤：回退 `PayrollWorkbenchContext`、删除 `PayrollWorkbenchAggregateContext`、恢复对应测试；无数据库回滚。
- 回滚验证：旧工作台字段、门店权限和 22 实体基线可恢复。
- 实际是否回滚：否

## 9. 成本与结论

- 最终结论：聚合查询、Web/App 真实渲染回读、10× 本地 MySQL 并发压测均通过；待生产候选环境复测、独立视觉评审和独立业务验收。
- 下一步：在生产候选环境复测 P95/P99，并完成具备图片输入能力的独立视觉/业务验收。
