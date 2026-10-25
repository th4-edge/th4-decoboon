# EXPENSE-D2 研发证据

> 独立复验历史（2026-10-23）：初始 D2 6 项 FAIL、P1-001 轮 FAIL、P1-002 轮 FAIL，共三次独立 FAIL；第四次独立复验 `18/18 PASS / ACCEPTED`，P1-001 PASS、P1-002 CLOSED。历史 FAIL 保留，最终结论见 `03-acceptance-record-d2.md`。

| 项 | 内容 |
| --- | --- |
| 问题 | `PI-20261022-018` |
| 业务裁决 | `DEC-091` / `BR-057` / `T12-BIZ-20261022-012` |
| 日期 | 2026-10-23 |
| 状态 | `D2_PASS / EXPENSE-D2 ACCEPTED` |
| 结论 | 第三轮独立复验 18/18 PASS；P1-001 PASS、P1-002 CLOSED；PI-018 overall PENDING |

## 实现

- `Expense.payable` 关联唯一 `Payable(Expense)`。
- 审批通过自动创建应付，来源 `sourceType=Expense/sourceId=expenseId`。
- 金额、门店、团队、项目、负责人和收款员工继承费用事实。
- 部分付款保持 `Expense Approved`；付清后 `Expense Closed`。
- 付款复用 `Payment + PayableSettlement + Ledger(Expense)`；D2 不创建凭证。
- 费用应付禁止进入多应付合并付款。
- 财务/店长不能支付本人报销；BOSS 一人企业例外。
- App/Web 详情增加应付/已付/未付/付款状态，`pay` 动作接现有付款能力。

## 迁移

- `129-企业费用D2应付关联.sql`
- `rollback/129-企业费用D2应付关联-rollback.sql`
- `MIGRATION_END=129`

## 验证

| 命令/范围 | 结果 |
| --- | --- |
| `ExpenseLifecycleTest`（9 项） | PASS |
| 后端主代码隔离编译 | PASS |
| 唯一应付与来源字段 | PASS |
| 部分付款 200 / 付清 480 | PASS |
| 付清后 Expense Closed | PASS |
| Payment + Settlement + Ledger | PASS |
| 无 Voucher 副作用 | PASS |
| App type-check / build:mp-weixin | PASS |
| Web 隔离 type-check/build（排除无关 CatalogMaintenance 并发阻塞） | PASS |

## P1-001 修复（2026-10-23）

### 根因

`finance/project/report` 的 Provider 白名单包含 `Project.property`，但未注册 `Property` 的最小字段白名单；Jackson 在序列化房屋懒加载关联时继续访问 `Property.company`，因 `open-in-view=false` 无法初始化而输出只有键名、没有值的非法 JSON。

修复后同一响应又暴露出 `Payable.payeeEmployee` 的同类懒加载截断。其根因是项目财务详情使用的 `findByProjectOrderByDueDateAsc` 未预取收款员工，白名单模型虽已注册，但代理对象在事务外仍被默认序列化。

### 最终修改

- `finance/service/projects/report/support/Report.java`：增加 `Property` 白名单，只允许 `propertyId/community/address`。
- `finance/service/projects/page/support/Page.java`：列表使用同一房屋摘要白名单。
- `finance/context/FinanceReportContext.java` 与 `project/repository/ProjectRepository.java`：项目财务详情预取 `customer/property/manager/designer/quote`，避免事务外读取项目聚合关联。
- `finance/repository/PayableRepository.java`：项目财务详情预取 `payeeEmployee/property`。
- `finance/repository/ReceivableRepository.java`：项目财务详情预取 `property`。
- 未修改金额、成本、权限、状态机、DDL、迁移或业务规则。

### 复现证据

- HTTP：隔离 H2 + Undertow 随机端口，`open-in-view=false`，`finance/project/report` 与 `finance/project/page` 均返回合法 JSON。
- `project.property` 实际输出只含 `propertyId/address`（无 `community` 时按白名单省略）；`Company/Store/Team/Owner/Customer/createTime/updateTime` 均未输出。
- `payables[].payeeEmployee` 正确输出 `empId/name/role`，不再截断。
- JSON 证据：`evidence/d2/reaccept-p1-001/http-json-evidence.txt`。

### 验证结果

| 命令/范围 | 结果 |
| --- | --- |
| Java 22 `mvn -o -Dmaven.test.skip=true clean compile` | PASS，2086 个主源码 |
| 隔离 D2 HTTP 全量 `ExpenseD2IndependentAcceptanceTest` | PASS，15/15，0 failure/error/skip |
| 新增列表同结构 HTTP 校验 | PASS，`finance/project/page.project.property` 白名单一致 |
| Java 22 隔离完整仓库 `mvn -o test` | PASS，1069 tests，0 failure/error/skip |

证据目录：`evidence/d2/reaccept-p1-001/`。

### 边界

- 本修复不涉及迁移，未重新执行 dev/test/prod DDL。
- 未执行真实 Web/App 浏览器、真机/微信开发者工具和真实 OSS；仍为 `BLOCKED`。
- 独立验收原 FAIL 记录保持不变；本轮只能标记研发候选和待复验。

## P1-002 修复（2026-10-23）

### 根因

`FinanceAccessContext.resolveProject` 在无事务边界下读取项目财务关联，门店范围主体访问 `finance/project/report` 时在 `project.customer.store` 或操作人门店作用域处触发 `LazyInitializationException`，被框架映射为 `hibernateErrorCode`，而不是按 DEC-065 返回本店数据或 403/404。

### 最终修改

- `project/repository/ProjectRepository.java`：新增 `findScopeDetailByProjectIdAndCompany_ComId`，`JOIN FETCH customer` 与 `customer.store`，查询同时固定 `comId`。
- `finance/context/FinanceAccessContext.java`：财务项目访问判定改用该查询，并将 `resolveProject` 置于 `@Transactional(readOnly = true)`，覆盖数据范围解析中的门店关联读取。
- 门店范围仍只比较当前门店与项目客户门店快照；BOSS/总部财务仍为企业范围；未新增或放宽任何权限。
- 本切片未修改金额、成本、状态机、DDL、迁移或业务规则。

### HTTP 访问矩阵

| 主体/场景 | 结果 |
| --- | --- |
| 本店店长 | `finance/project/report` 200，成功返回项目 |
| 本店门店财务 | `finance/project/report` 200，成功返回项目 |
| BOSS 企业范围 | `finance/project/report` 200，成功返回项目 |
| 总部财务企业范围 | `finance/project/report` 200，成功返回项目 |
| 跨店店长 | 404「项目不存在或无权访问」 |
| 跨企业 BOSS | 404「项目不存在或无权访问」 |
| 数据库异常码 | 无 `hibernateErrorCode` |

### 验证结果

| 命令/范围 | 结果 |
| --- | --- |
| Java 22 隔离 D2 HTTP（原 17 项 + 合法范围访问矩阵） | PASS，18/18，0 failure/error/skip |
| Java 22 `mvn -o -Dmaven.test.skip=true clean compile` | PASS，2086 个主源码 |
| Java 22 隔离完整仓库 `mvn -o -q clean test` | PASS，261 suites / 1069 tests / 0 / 0 / 0 |

证据：

- `evidence/d2/reaccept-p1-002/p1-002-fix.diff`
- `evidence/d2/reaccept-p1-002/access-matrix-summary.txt`
- `evidence/d2/reaccept-p1-002/d2-http-18-summary.txt`
- `evidence/d2/reaccept-p1-002/full-repository-test-count.txt`
- `evidence/d2/reaccept-p1-002/java22-compile.log`

## 未覆盖

- 独立复验 P1-002 修复及完整 D2 HTTP 用例。
- 独立 MySQL 迁移 129/回滚复验（本轮无迁移变更）。
- 真实 Web/App 浏览器/真机、真实 OSS。
- dev/test/prod 数据库迁移。
- 独立验收。

D2 已独立验收；D3 蓝图/计划已完成，迁移 130 与编码待用户确认。
