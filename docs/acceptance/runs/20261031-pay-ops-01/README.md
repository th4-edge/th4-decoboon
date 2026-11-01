# PAY-OPS-01 薪酬工作台聚合 SQL 化

```text
AUDIT-ID: PAY-OPS-01
日期: 2026-10-31
状态: DEV_VERIFIED_RENDER_AND_10X_PERF_PASS_VISUAL_REVIEW_UNAVAILABLE
业务口径: ANS-BIZ-20261031-001 / BR-015 / BR-025 / BR-031
规范: DEC-036 / DEC-037 / docs/design/34-后端接口查询与业务编排强制规范.md
```

## 1. 目标

把 `payroll/workbench/report` 从“5 类实体全量加载后 Java 统计”改为固定聚合查询链：

- 考勤聚合；
- 工资单状态聚合；
- 最近工资单投影；
- 员工工资/提成应付聚合；
- 已释放未付提成聚合；
- 工资条状态聚合。

不改变响应字段、权限、门店范围或期间语义。

## 2. 业务口径

用户 2026-10-31 确认按建议执行：

1. 待处理考勤只统计 `Pending/Disputed`；
2. 员工提成待付金额跨期间累计；
3. 应付/提成未付金额逐条按 `max(金额-已付, 0)`，负余额不得冲减其他项；
4. 最近工资单按 `period DESC, sheetId DESC` 稳定排序。

## 3. 改造前后对比

同一 H2、同一租户、门店、期间和数据：

| 指标 | 改造前 | 改造后 | 结论 |
| --- | ---: | ---: | --- |
| prepared statement | 15 | 14 | 稳定 |
| 实体加载数 | 22 | 2 | -90.9% |
| 响应体 | 493B | 493B | 不变 |
| `pendingEmployeePayableAmount` | 60.00 | 80.00 | 修复负余额冲减 |
| 10× 应付/提成记录 SQL | — | 14 | 不增长 |
| 10× 应付/提成记录实体加载 | — | 2 | 不增长 |

空数据场景：

| 指标 | 结果 |
| --- | ---: |
| prepared statement | 13 |
| 响应体 | 485B |

说明：本节是 H2 下的实现结构证据；生产等价数据量与并发的本地 MySQL 结果见第 8 节，生产候选环境仍需复测。

## 4. 自动化验证

```text
定向：PayrollWorkbenchContextTest / FinanceStoreScopeHttpAcceptanceTest / SummaryImplementationRulesTest
结果：33/0/0/0
全量后端：1188 tests / 0 failures / 0 errors / 0 skipped
Provider 响应审计：providers=543 properties=538 allow=536 findings=0
门店负向：本店成功、跨店 403
git diff --check：PASS
```

## 5. 改动文件

- `src/main/java/com/th4/edge/decoboon/payroll/context/PayrollWorkbenchContext.java`
- `src/main/java/com/th4/edge/decoboon/payroll/context/PayrollWorkbenchAggregateContext.java`
- `src/test/java/com/th4/edge/decoboon/payroll/PayrollWorkbenchContextTest.java`
- `src/test/java/com/th4/edge/decoboon/arch/SummaryImplementationRulesTest.java`
- `src/test/java/com/th4/edge/decoboon/finance/FinanceStoreScopeHttpAcceptanceTest.java`

## 6. 证据

- `raw/baseline.log`
- `raw/baseline-with-data.log`
- `raw/after-with-data.log`
- `raw/after-10x.log`
- `raw/targeted-tests.log`
- `raw/final-targeted.log`
- `raw/backend-full-test.log.gz`
- `raw/provider-response-audit.txt`
- `raw/web-current-readback.json`
- `raw/web-payroll-current-*.png`
- `raw/app-payops-check.json`
- `raw/app-payroll-workbench.png`
- `raw/cleanup-before.txt`
- `raw/cleanup-after.txt`
- `raw/mysql-10x-summary.json`
- `raw/mysql-10x-run1.json`
- `raw/mysql-10x-run2.json`
- `raw/mysql-10x-run3.json`
- `raw/summary.txt`

## 7. 真实页面与本地 MySQL 回读（2026-10-31）

使用 `2026-10` 与 `2099-01` 两个隔离周期，在昭青科技测试租户通过正式 Provider 创建合成薪酬数据，验证后已清理。

### Web

- 浏览器：Ego Lite / Chromium
- 页面：`/finance/payroll?period=2026-10`
- 服务端结果：待处理考勤 2、工资单草稿 1、待发员工薪酬 ¥1,600.00
- 20 次同口径浏览器 API 采样：P50 41.9ms、P95 66.1ms、max 71.2ms
- 响应：HTTP 200、491B
- Console/unhandled error：0
- 1440/1280/1024 三档：`clientWidth == scrollWidth`，无页面级横向溢出
- 证据：`raw/web-current-readback.json`、`raw/web-payroll-current-*.png`

### App

- 运行端：微信开发者工具 + `miniprogram-automator`
- 页面：`subpackages/payroll/pages/workbench`
- 显示：`2026-10`、待发员工薪酬 `¥1,600`、待确认考勤 `2`、工资单草稿 `1`
- 5/5 功能检查 PASS
- 证据：`raw/app-payops-check.json`、`raw/app-payroll-workbench.png`

### 视觉边界

当前执行模型不支持图片输入，无法对截图进行视觉判断。因此：

- 真实渲染、DOM 数值、接口一致性和视口几何：PASS；
- 视觉审美、层级、对比度、细节质量：`VISUAL_REVIEW_UNAVAILABLE`；
- 不得将本批次表述为 UI 视觉验收通过。

### 数据清理

清理前：8 份薪酬方案、8 份考勤、2 张工资单、8 个工资项、4 条提成、4 条提成应付。
清理后：全部为 0。
证据：`raw/cleanup-before.txt`、`raw/cleanup-after.txt`。

## 8. 10× MySQL 并发压测（2026-10-31）

### 数据集

| 数据 | 基线 | 10× |
| --- | ---: | ---: |
| 员工 | 8 | 80 |
| 考勤 | 96 | 960 |
| 工资单 | 12 | 12 |
| 工资项 | 96 | 960 |
| 工资条 | 96 | 960 |
| 提成 | 96 | 960 |
| 应付 | 104 | 1,040 |

### 三次独立运行中位值

| 场景 | P50 | P95 | P99 | max | 错误率 |
| --- | ---: | ---: | ---: | ---: | ---: |
| 10× 顺序 30 请求/轮 | 13.54ms | 21.87ms | 22.43ms | 22.43ms | 0 |
| 10× 并发 20×200 请求/轮 | 19.25ms | 28.19ms | 29.75ms | 31.78ms | 0 |

- 并发吞吐中位：974.26 RPS
- 响应体：约 503B
- MySQL `Questions` 单请求增量：基线 22、10× 22，未随数据量增长
- 预算：核心读取 API P95 ≤500ms、P99 ≤1000ms，**PASS**
- 测后合成数据清理：0 残留

说明：这是本机 MySQL + 真实 Provider HTTP + 生产等价合成数据量的压测，不等同生产网络、机器规格和生产并发。生产环境发布前仍需在生产候选环境复测。

证据：`raw/mysql-10x-summary.json`、`raw/mysql-10x-run1..3.json`、`raw/mysql-10x-run1..3.log`。

## 9. 未完成

- 生产候选环境的最终并发复测；
- 具备图片输入能力的独立视觉评审；
- 独立业务验收；
- 生产发布与回滚演练。

因此当前只能声明 `DEV_VERIFIED + RENDER_PASS + 10X_PERF_PASS`，不得声明完整 UI 验收或已发布。
