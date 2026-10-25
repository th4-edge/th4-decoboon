# EXPENSE-D3 独立验收记录（首轮 FAIL + 第二轮复验）

## 记录索引

| 轮次 | 结论 | 证据 |
| --- | --- | --- |
| 第一轮 | **FAIL**，P0=0 / P1=1 / P2=0，P1-003 OPEN | 本文件第 1 节起；`evidence/d3/independent-20261023/` |
| 第二轮 | **PASS / ACCEPTED**，P0=0 / P1=0 / P2=0，P1-003 CLOSED | 本文件“第二轮独立复验”起；`evidence/d3/independent-reacceptance-20261023/` |

> 第一轮原始记录完整保留；第二轮 PASS 不回溯改写首轮 FAIL。

## 第一轮独立验收（原始记录，保留）

| 项 | 内容 |
| --- | --- |
| 问题 | `PI-20261022-018` |
| 切片 | `EXPENSE-D3`：付款凭证来源快照与历史回读 |
| 验收类型 | 首轮独立黑盒验收；验收责任方不参与本切片研发实现 |
| 验收日期 | 2026-10-23 |
| 结论 | **FAIL** |
| 缺陷计数 | P0=0 / P1=1 / P2=0 |
| D3 是否可关闭 | **否** |
| PI-018 overall | **PENDING** |
| 后续门禁 | D4/D5 不启动；`GAP-070` 保持 OPEN；不发布 |

## 1. 独立快照与环境

- 完整源码快照：`/private/tmp/expense-d3-independent.BeBugm/full-backend`
- 源工作树 HEAD：`7cf83fa2b526156640ea8c227eb348ab81ca27d1`
- 源工作树 Tree：`0b58e9ba19798cf9863178ad560a8998047a5e0e`
- 快照包含源工作树未提交修改与未跟踪文件；源状态 483 项（219 修改、3 删除、257 未跟踪，另有状态项由 Git porcelain 组合表示）。
- 快照未纳入 `target/`、`dist/`、`logs/` 构建/日志产物。
- 源码 SHA-256 清单摘要：`bfd4b50ccb12975ddfc6be38cfaacb7c929f3cc8eb264c1427b680dc39d0dab9`。
- Java：Oracle JDK `22.0.1`，路径 `/Library/Java/JavaVirtualMachines/jdk-22.jdk/Contents/Home`。
- HTTP：真实 Spring Boot `RANDOM_PORT` + TCP，H2 `MODE=MySQL;DATABASE_TO_LOWER=TRUE`，`spring.jpa.open-in-view=false`，分布式锁关闭，本地隔离文件存储。
- 未连接 dev/test/prod 数据库，未发布，未进入 D4/D5。
- 独立 HTTP 源码位于快照 `acceptance-harness/`，未放入产品 `src/test`；最终源码 SHA-256：`d366a0f36f95a567540725c4688b177c47536aca43cca094edd6f8539ecc80b2`。

验收期间原工作区存在外部并发变动，本验收始终以所记录的独立快照为唯一验收对象。结束后对 D3 相关 8 个 Java 文件与 2 个迁移文件逐文件复核：9 项与快照一致，`VoucherContext.java` 在 18:48:41 被外部改动（内容针对不可解析 Expense 来源抛错，疑似 P1-003 候选修复），该改动不在本轮快照和最终 HTTP 结果中。必须冻结新快照后重新独立复验，不能据此回溯判定首轮 PASS。逐文件对照见 `evidence/d3/independent-20261023/backend/d3-relevant-file-hashes.txt`，候选差异见 `backend/voucher-context-post-acceptance-candidate.diff`。本验收本身未修改产品代码。

## 2. 后端与迁移结果

| 检查 | 结果 | 实测 |
| --- | --- | --- |
| Java 22 主源码 `clean compile` | PASS | RC=0；2,083 个主源码 Java；2,294 个 `.class` |
| 完整 `mvn -o -q clean test` | PASS | 264 suites / 1,075 tests / 0 failures / 0 errors / 0 skipped；RC=0 |
| 产品测试树边界 | PASS | 产品 `src/test` 265 个 Java 文件；独立验收源码命中 0 |
| 迁移 130 隔离 MySQL apply | PASS | MySQL 8.4.0，`--skip-networking`；三字段与联合索引均存在 |
| 迁移 130 rollback | PASS | 三字段和索引归零；`db_voucher_record_source_rollback_130` 存在并保留 2 条快照行；原 4 条凭证历史保留 |

迁移字段核对：`source_type varchar(32) NULL`、`source_id bigint NULL`、`source_snapshot longtext NULL`；索引为 `(com_id, source_type, source_id)`。APPLY/ROLLBACK 各日志 stderr 均为 0 字节。

## 3. D3 黑盒验收矩阵

最终独立 HTTP 套件为 **11 tests / 10 PASS / 1 FAIL / 0 ERROR / 0 SKIP**。首轮和第二次执行的非产品失败来自独立夹具/断言修正，历史仍保留；最终稳定失败仅 P1-003。

| 编号 | 场景 | 结果 | 独立证据摘要 |
| --- | --- | --- | --- |
| D3-01 | 未付款费用不得生成付款凭证/副作用 | PASS | 业务错误；无 Voucher、Payment、Ledger 新增 |
| D3-02 | 已付款凭证来源与金额 | PASS | `sourceType=Expense`、`sourceId=expenseId`、凭证金额=Payment.amount，而非 Expense.amount |
| D3-03 | `source_snapshot` 必需事实 | PASS | 企业/编号/分类/归属/提交人/收款员工/审批版本/付款/模板版本/SHA-256 键存在且口径一致 |
| D3-04 | Direct 模板路由 | PASS | 标题“费用报销付款单”，快照 `templatePresetCode=P-EXPENSE` |
| D3-05 | Reimbursement 模板路由 | PASS | 标题“费用报销单”，快照 `templatePresetCode=E-REIMBURSEMENT` |
| D3-06 | TRAVEL 报销模板路由与项目快照 | PASS | 标题“差旅费报销单”，项目 ID/编号/名称进入快照 |
| D3-07 | 同一 paymentId 重打 | PASS | voucherId/voucherNo/sha256/对象路径不变；文件内容与 mtime 不变；仅短期签名 URL 的 expires/signature 变化 |
| D3-08 | 费用详情 payments/vouchers 回读 | PASS | Payment 与 Voucher 金额、paymentId、sourceType/sourceId、sha256 同源 |
| D3-09 | `voucher/page|report` 按 expenseId 过滤 | PASS | 两笔费用仅返回目标费用；报表 count/total/paymentAmount 一致 |
| D3-10 | 跨店、跨企业、错误角色 | PASS | build 返回 403/404；page/report/info 无泄露；响应无 `hibernateErrorCode`/懒加载异常 |
| D3-11 | 非法 Expense 来源拒绝 | **FAIL** | 声明 `sourceType=Expense` 但 `sourceId` 指向他企费用的付款仍成功发行普通“工资发放凭证” |
| D3-12 | 真实 Web/App/OSS 证据 | **BLOCKED** | 无真实登录页面/真机/微信开发者工具/真实 OSS 下载回读证据；不以构建产物冒充 |

## 4. P1-003：非法 Expense 来源未拒绝

| 项 | 内容 |
| --- | --- |
| 级别 | **P1** |
| 状态 | **OPEN** |
| 复现入口 | `document/voucher/build`，请求 `paymentId` |
| 前置 | Payment/Ledger 属企业 A；其 Payable 声明 `sourceType=Expense`，`sourceId` 指向企业 B 的费用事实 |
| 预期 | 在模板选择、渲染、存储前拒绝，返回业务 400/404/409，且不新增 VoucherRecord/文件 |
| 实际 | 接口 `success=true`，生成“工资发放凭证”，凭证无 Expense 来源快照，非法链被当作普通付款凭证发行 |
| 影响 | 非法/不可解析的 Expense 来源未被强制拒绝，可能生成错误业务凭证并形成可下载归档；本轮未观察到跨企业数据泄露或金额改写 |
| 代码定位 | `VoucherContext.build` 在约 180–184 行使用可空 Expense；`expenseForPayment` 在约 383–395 行对不可解析费用返回 null，调用方随后继续默认模板路径 |
| 恢复要求 | 当 Payable 声明 Expense 来源时，必须解析到同企业、同权限范围的 Expense，否则在任何模板/PDF/存储副作用前拒绝；补同企业不存在、跨企业、来源类型拼接回归 |

## 5. App / Web 构建

| 端 | 结果 | 实测 |
| --- | --- | --- |
| App type-check | PASS | RC=0 |
| App `build:mp-weixin` | PASS | RC=0，`DONE Build complete` |
| Web type-check | PASS | RC=0 |
| Web `npm run build` / Vite build | FAIL | `ProjectExecutionView.vue` 引用未定义 Sass 变量 `$shadow-xs`；未修改产品样式或验收预期 |

Web 构建失败没有计入 D3 的 P0/P1/P2，因为它不是本轮 D3 专项用例的失败根因；但它是仓库级独立构建阻断，不能宣称 Web 构建通过。真实 Web/App/OSS 仍 `BLOCKED`。

## 6. 历史与门禁

- D2 第三轮 `PASS / ACCEPTED` 历史保持有效，不被本记录覆盖。
- D3 研发证据、研发迁移证据和本轮独立证据分别保留。
- D3 独立验收不通过，不得进入 D4/D5。
- `GAP-070` 保持 OPEN；未发布；PI-018 overall 仍 PENDING。

## 7. 证据路径

- 独立证据根目录：`docs/acceptance/runs/20261022-expense-d1/evidence/d3/independent-20261023/`
- 最终 HTTP 报告：`evidence/d3/independent-20261023/http-round3/report.txt`
- 最终 HTTP XML：`evidence/d3/independent-20261023/http-round3/TEST-acceptance.expense.d3.ExpenseD3IndependentHttpAcceptanceTest.xml`
- 独立源码：`evidence/d3/independent-20261023/harness/ExpenseD3IndependentHttpAcceptanceTest.java`
- 完整测试：`evidence/d3/independent-20261023/backend/full-clean-test.log.gz`、`full-clean-test-surefire-reports.tar.gz`
- 编译：`evidence/d3/independent-20261023/backend/java22-main-clean-compile-summary.txt`
- 迁移：`evidence/d3/independent-20261023/migration-130/`
- App/Web：`evidence/d3/independent-20261023/app/`、`evidence/d3/independent-20261023/web/`
- 证据清单摘要：`evidence/d3/independent-20261023/EVIDENCE-MANIFEST-SHA256.txt`

## 第二轮独立复验（2026-10-23）

| 项 | 内容 |
| --- | --- |
| 复验对象 | `EXPENSE-D3 / P1-003` 修复候选 |
| 验收类型 | 新快照独立黑盒复验 |
| 结论 | **PASS / ACCEPTED** |
| 缺陷计数 | P0=0 / P1=0 / P2=0 |
| P1-003 | **CLOSED** |
| D3 是否可关闭 | **是** |
| PI-018 overall | **PENDING**（D4~D6 未完成） |
| 后续门禁 | D4/D5 未启动；`GAP-070` OPEN；真实 Web/App/OSS BLOCKED；不发布 |

### 1. 新快照与环境

- 完整快照：`/private/tmp/expense-d3-reacceptance-round2.nUlJTV/full-backend`
- HEAD：`7cf83fa2b526156640ea8c227eb348ab81ca27d1`
- Tree：`0b58e9ba19798cf9863178ad560a8998047a5e0e`
- 快照前后产品源码清单一致；未复用首轮快照。
- 源码清单摘要：`98f44673f4d99ae4e7d1d4bcefb1a194ea408146a3d53af80f4b4fbc880ad94d`
- P1-003 候选 `VoucherContext.java` SHA-256：`2f38f20fca812e58622dad7953fd98bd9b272115d30b78e53488ff24e91ed2c4`
- 快照包含 4 个 `statistic/service/target` 包文件；仅排除仓库根 `/target`、`/dist`、`/logs`。
- Java 22.0.1；真实 Spring Boot `RANDOM_PORT` + TCP；H2 `MODE=MySQL`；`open-in-view=false`；本地隔离存储。
- 独立验收源码位于快照 `acceptance-harness/`，产品 `src/test` 中命中 0。

### 2. P1-003 专项结果

| 场景 | 期望 | 实测 |
| --- | --- | --- |
| sourceId 指向不存在费用 | 400/404/409，零副作用 | **404**；Voucher `0→0`；文件 `22→22` |
| sourceId 指向他企费用 | 400/404/409，零副作用 | **404**；Voucher `0→0`；文件 `25→25` |
| Expense 与 Payment 所属 Payable 不一致 | 400/404/409，零副作用 | **400**；Voucher `0→0`；文件 `27→27` |
| 合法 Expense 来源 | 仍成功发行费用凭证 | PASS；来源/金额/快照/历史重打原 11 项均通过 |

三种非法来源均未降级为普通付款凭证。代码修复在模板选择前解析并校验 Expense，因此上述调用未产生模板、PDF 或存储副作用。

### 3. HTTP 矩阵与计数

最终独立套件：**13 tests / 13 PASS / 0 FAIL / 0 ERROR / 0 SKIP**。

- 首轮原 D3 11 项：**11/11 PASS**，包括未付款零副作用、来源/金额/快照、`P-EXPENSE`/`E-REIMBURSEMENT`/`E-TRAVEL`、同付款重打、费用详情回读、`expenseId` 过滤、跨店/跨企业/错误角色。
- P1-003 新增分支：不存在来源、跨企业来源、Expense/Payable 不一致合计 **3/3 PASS**。
- 合法 Expense 来源未回归失败。
- 首跑唯一 ERROR 来自独立夹具试图创建第二个 Expense 来源 Payable，违反产品唯一约束；修正夹具后稳定通过，不计产品缺陷。

### 4. 后端与迁移

| 检查 | 实测 |
| --- | --- |
| Java 22 主源码 `clean compile` | PASS；2,087 Java / 2,298 class |
| 完整测试 attempt1 | FAIL；265 suites / 1,078 tests / 1 failure / 0 errors / 0 skipped |
| attempt1 失败归因 | `StoreDimensionBindingHttpAcceptanceTest` 时间字段 `computedAt` 秒值不同；非 D3 |
| attempt1 定向复跑 | PASS |
| 完整测试 attempt2 | PASS；265 suites / 1,078 tests / 0 failures / 0 errors / 0 skipped |
| 产品测试树独立源码 | 0 |
| 迁移 130 apply | PASS；三字段与 `(com_id,source_type,source_id)` 索引 |
| 迁移 130 rollback | PASS；源字段/索引归零，备份表保留 2 行，原 4 条凭证历史保留 |

### 5. App / Web

| 端 | 结果 |
| --- | --- |
| App type-check | PASS |
| App `build:mp-weixin` | PASS |
| Web type-check | PASS |
| Web `npm run build` | PASS |
| 真实 Web/App/OSS | **BLOCKED**，无真实登录页面、真机/微信开发者工具、真实 OSS 下载回读证据 |

### 6. 结论与门禁

- `P1-003 CLOSED`；首轮 FAIL 保留。
- `EXPENSE-D3 PASS / ACCEPTED`。
- D3 专项对 P0/P1/P2 的计数为 **0/0/0**。
- D4/D5 未进入；D5 仍受 `GAP-070` 阻塞。
- PI-018 overall 仍 PENDING。
- 真实 Web/App/OSS BLOCKED，不发布。
- 本复验未修改产品代码、权限、DDL、迁移、金额/成本规则或验收预期。

### 7. 证据路径

- 独立证据根目录：`docs/acceptance/runs/20261022-expense-d1/evidence/d3/independent-reacceptance-20261023/`
- 最终 HTTP：`evidence/d3/independent-reacceptance-20261023/http-run3-final/`
- 新快照与源码稳定性：`evidence/d3/independent-reacceptance-20261023/backend/`
- 完整测试：`evidence/d3/independent-reacceptance-20261023/backend/full-test-attempt1/`、`backend/full-test-attempt2/`
- 迁移：`evidence/d3/independent-reacceptance-20261023/migration-130/`
- App/Web：`evidence/d3/independent-reacceptance-20261023/app/`、`web/`
- 证据清单摘要：`f3b8a04d47f3b85eddd61da0069cc45ccb45ce565dbcfa335d8dd4553fab6547`
