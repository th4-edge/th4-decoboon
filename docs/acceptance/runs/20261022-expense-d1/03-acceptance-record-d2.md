# EXPENSE-D2 独立验收记录（第三轮复验 / PASS）

| 项 | 内容 |
| --- | --- |
| 问题 | `PI-20261022-018` |
| 切片 | `EXPENSE-D2`：审批 → 唯一应付 → 部分/付清 → 关闭与隔离 |
| 业务裁决 | `DEC-091` / `BR-057` / `T12-BIZ-20261022-012` |
| 复验日期 | 2026-10-23 |
| 验收责任方 | 独立验收责任方（非研发实现方） |
| 完整快照 | `/private/tmp/expense-d2-reacceptance-round3.H50jW4/full-backend` |
| 候选提交 / Tree | `c77532c1ec18abffbc999951a3ca6d3c9ea5ed63` / `c035c3fd1f1535990a142c9723e9b91f8f26d26b` |
| 源码范围 | 2086 个主 Java 文件；262 个测试 Java 文件 |
| D2 HTTP | **18 tests / 18 PASS / 0 FAIL / 0 ERROR / 0 SKIP** |
| Java 22 全量 | **1069 tests / 0 failures / 0 errors / 0 skipped**，261 suites，RC=0 |
| 缺陷 | **P0=0，P1=0（P1-002 CLOSED），P2=0** |
| 结论 | **PASS（EXPENSE-D2 第三轮独立复验）** |
| 真实 UI/OSS | **BLOCKED**：未执行真实 Web/App 页面、真机/微信开发者工具或真实 OSS |
| D3 | 未进入 |
| D5 | `GAP-070` 仍 OPEN，保持 BLOCKED |


## 1. 第三轮独立性与环境

- 本轮未修改产品代码、权限、DDL、迁移、业务规则或产品测试，未连接 dev/test/prod 数据库，未使用真实 OSS，未发布。
- 使用完整独立快照；快照提交与 Tree 见上表。主 Java 2086 个、测试 Java 262 个；`src/**` 与当前候选逐文件一致，快照工作树为 CLEAN。
- HTTP：Spring Boot `RANDOM_PORT` + Undertow，真实 TCP `127.0.0.1`，业务动作全部经 `/api` Provider。
- 数据库：H2 2.4.240，`MODE=MySQL`、`ddl-auto=create-drop`、`open-in-view=false`。
- 并发锁：`th4.edge.components.lock.enabled=true`，隔离 Redis `127.0.0.1:6389`；本轮记录 436 次 `EVALSHA`，确认实际执行 Redisson 锁。
- 验收测试源运行后移出 `src/test`；全量测试前恢复仓库原有测试树，262 个测试 Java 文件中不含 D2 独立验收源码。
- 仓库既有 `DataScopeRequestFilterIntegrationTest` 的 `@Primary` Redisson 测试代理会被框架宽泛组件扫描带入；HTTP 验收时临时将该无关测试文件移出后再 clean 构建，运行结束后原样恢复。详细说明见证据 `12-test-harness-isolation.txt`，该环境隔离不属于 P1-002 或产品缺陷。

## 2. P1-002 复核：CLOSED

P1-002 原缺陷为 `finance/project/report` 在 `open-in-view=false` 下读取 `Customer.store` 懒加载，返回 `hibernateErrorCode`。第三轮独立复核结果：

| 主体 | transport HTTP | 业务结果 | 业务码 | `hibernateErrorCode` |
| --- | --- | --- | --- | --- |
| 本店店长 | 200 | 成功返回项目 | - | NONE |
| 本店门店财务 | 200 | 成功返回项目 | - | NONE |
| BOSS（企业范围） | 200 | 成功返回项目 | - | NONE |
| 总部财务（企业范围） | 200 | 成功返回项目 | - | NONE |
| 跨店店长 | 200 | 拒绝 | 404 | NONE |
| 跨店门店财务 | 200 | 拒绝 | 404 | NONE |
| 跨企业 BOSS | 200 | 拒绝 | 404 | NONE |
| 跨企业财务 | 200 | 拒绝 | 404 | NONE |

说明：框架错误以成功 transport + JSON 业务码返回；越权门禁的约定码为业务 `404`。8 个响应均无 `hibernateErrorCode`，也没有返回越权项目数据。

源码复核确认：

- `ProjectRepository.findScopeDetailByProjectIdAndCompany_ComId` 使用 `JOIN FETCH customer` 与 `left join fetch customer.store`，并固定 `projectId + company.comId`。
- `FinanceAccessContext.resolveProject` 在 `@Transactional(readOnly = true)` 内完成门店归属比较；门店范围仍只允许本店，BOSS/总部财务仍为企业范围。
- 未发现新增权限放宽、金额口径变化、DDL 或迁移变化。

## 3. 原 D2 验收矩阵（第三轮）

| 编号 | 验收项 | 结果 | 说明 |
| --- | --- | --- | --- |
| D2-01 | 审批唯一应付、金额/归属继承 | PASS | 单次与重复审批均只有一张 `Payable(Expense)` |
| D2-02 | 部分付款与付清关闭 | PASS | 400 + 600 / 1000，`Expense Approved → Closed` |
| D2-03 | 单笔付款核销 | PASS | 单笔付款生成 1 条 `PayableSettlement` |
| D2-04 | 顺序幂等 | PASS | 同键重试返回同一 Payment；重复审批不新增应付 |
| D2-05 | 超付与付清后付款 | PASS | 超额和付清后再付均被拒绝 |
| D2-06 | 自付规则与 BOSS 例外 | PASS | 财务/店长自付被拒；BOSS 顺序操作成功 |
| D2-07 | 跨店、跨企业付款 | PASS | 返回业务 403/404，且不生成付款副作用 |
| D2-08 | 合并付款边界 | PASS | 费用应付禁止与其他应付合并付款 |
| D2-09 | Draft/异常费用应付付款门禁 | PASS | 返回 409/400，未生成 Payment |
| D2-10 | 并发审批 | PASS | 4 个并发审批请求仅生成 1 张应付 |
| D2-11 | 并发付款 | PASS | 400+400 对 500 应付至多成功 1 次，无超付 |
| D2-12 | P1-001 JSON/白名单/收款员工 | PASS | 合法 JSON；Property 白名单；PayeeEmployee 可读；page 同结构 |
| D2-13 | 项目财务隔离 | PASS | 原 17 项中的跨店/跨企业隔离通过；本店合法路径由 D2-14 补齐 |
| D2-14 | P1-002 门店/企业访问矩阵 | PASS | 本店店长/门店财务、BOSS/总部财务 200；跨店/跨企业业务 404；无数据库异常码 |

原 17 项与新增独立矩阵合计 **18/18 PASS**。逐项方法清单见 `evidence/d2/round3-independent-20261023/05-d2-suite-summary.txt`，原始日志见同目录 `04-d2-http-18.log`。

## 4. 全量、隔离与未覆盖

- Java 22.0.1 完整候选源码执行 `mvn -o clean test`：**261 suites / 1069 tests / 0 failures / 0 errors / 0 skipped**，BUILD SUCCESS，RC=0。
- 全量运行使用 262 个测试 Java 文件；D2 独立验收源码不在 `src/test`，未混入 1069 项测试。
- 真实 Web/App 页面、真机/微信开发者工具和真实 OSS 未执行，保持 **BLOCKED**。
- 本轮未执行 dev/test/prod DDL、迁移或发布冒烟。

## 5. 第三轮结论

`EXPENSE-D2` 第三轮独立复验为 **PASS / ACCEPTED**：原 D2 17 项与新增门店/企业访问矩阵 18/18 PASS，P1-001 保持 PASS，P1-002 CLOSED。

门禁结论：**`D2_PASS / EXPENSE-D2 ACCEPTED / PI-018 overall PENDING`**。D3 不启动；D5 仍受 `GAP-070` 阻塞；真实 Web/App/OSS 仍 `BLOCKED`；不发布。

---

# 第二轮历史记录（FAIL，保留）

## A.1 独立性与环境（第二轮历史）

- 本轮未修改产品代码、权限、DDL、迁移或业务规则，未连接 dev/test/prod 数据库，未发布。
- 使用完整独立快照，源码提交与 Tree 见上表；快照排除 `.git`、`target`、`logs` 等生成物后重新初始化并提交。
- HTTP：Spring Boot `RANDOM_PORT` + Undertow，真实 TCP `127.0.0.1`，业务动作全部经 `/api` Provider。
- 数据库：H2 2.4.240，`MODE=MySQL`、`ddl-auto=create-drop`、`open-in-view=false`。
- 并发锁：`th4.edge.components.lock.enabled=true`，隔离 Redis `127.0.0.1:6389`；Redis 命令统计记录 2074 次 `EVALSHA`，确认实际执行 Redisson 锁。
- 独立 HTTP 用例在临时测试树中运行；运行结束后恢复完整 1069 项测试树再执行全量测试。

## A.2 原 D2 验收矩阵（第二轮历史）

| 编号 | 验收项 | 结果 | 说明 |
| --- | --- | --- | --- |
| D2-01 | 审批唯一应付、金额/归属继承 | PASS | 单次与重复审批均只有一张 `Payable(Expense)` |
| D2-02 | 部分付款与付清关闭 | PASS | 400 + 600 / 1000，`Expense Approved → Closed` |
| D2-03 | 单笔付款核销 | PASS | 单笔付款生成 1 条 `PayableSettlement` |
| D2-04 | 顺序幂等 | PASS | 同键重试返回同一 Payment；重复审批不新增应付 |
| D2-05 | 超付与付清后付款 | PASS | 超额和付清后再付均被拒绝 |
| D2-06 | 自付规则与 BOSS 例外 | PASS | 财务/店长自付被拒；BOSS 顺序操作成功 |
| D2-07 | 跨店、跨企业付款 | PASS | 返回 403/404，且不生成付款副作用 |
| D2-08 | 合并付款边界 | PASS | 费用应付禁止与其他应付合并付款 |
| D2-09 | Draft/异常费用应付付款门禁 | PASS | 返回 409/400，未生成 Payment |
| D2-10 | 并发审批 | PASS | 4 个并发审批请求仅生成 1 张应付 |
| D2-11 | 并发付款 | PASS | 400+400 对 500 应付至多成功 1 次，无超付 |
| D2-12 | P1-001 JSON/白名单/收款员工 | PASS | 合法 JSON；Property 白名单；PayeeEmployee 可读；page 同结构 |
| D2-13 | 项目财务隔离 | **FAIL** | 店长门店范围访问项目财务详情返回 `hibernateErrorCode`，未按门禁返回 403/404 |

## A.3 P1-001 复核：PASS（第二轮历史）

- `finance/project/report` 返回合法 JSON，未再截断。
- `project.property` 严格只含 `propertyId/community/address` 允许字段；实际无社区时输出：
  `{"propertyId":"...","address":"独立验收小区 1 栋 101"}`。
- `Company/Store/Team/Owner/Customer/createTime/updateTime` 未出现在 `project.property`。
- `payables[].payeeEmployee` 可读：
  `{"empId":"...","name":"城北设计师A","role":{"name":"设计师","value":"Designer","key":"Designer"}}`。
- `finance/project/page` 的 `project.property` 使用同一最小白名单结构。

## A.4 P1-002：项目财务门店隔离返回数据库错误（第二轮历史，OPEN）

复现请求：店长以本店/他店身份调用 `finance/project/report`。实际响应：

```json
{"success":false,"errorCode":{"code":"hibernateErrorCode","message":"数据库操作错误:","system":false}}
```

关键堆栈：

```text
org.hibernate.LazyInitializationException: Could not initialize proxy [Customer] - no session
  at ... Customer$HibernateProxy.getStore
  at com.th4.edge.decoboon.finance.context.FinanceAccessContext.resolveProject(FinanceAccessContext.java:123)
  at com.th4.edge.decoboon.finance.context.FinanceReportContext.project(FinanceReportContext.java:328)
  at ... finance.service.projects.report.support.Report.call(Report.java:74)
```

影响：

- `open-in-view=false` 下，门店范围访问项目财务详情在读取 `Customer.store` 时发生懒加载异常。
- 异常发生在门店归属比较之前，因此合法本店访问同样会受影响；本轮直接复现的是跨店请求没有返回约定的 403/404。
- 该问题属于 D2“项目财务隔离”门禁，P1-002 保持 OPEN；修复前 D2 不得标记 PASS。

证据：[JSON 与堆栈摘录](evidence/d2/round2-independent-20261023/07-json-and-defect-evidence.txt)。

## A.5 全量与未覆盖（第二轮历史）

- Java 22 完整候选源码 `mvn -o clean test`：**1069 tests / 0 failures / 0 errors / 0 skipped**，261 suites，BUILD SUCCESS。
- 全量测试不覆盖本缺陷新增的“店长访问项目财务详情”路径，因此不能替代 D2 HTTP 门禁。
- 真实 Web/App/OSS 未执行，保持 **BLOCKED**；研发构建、Mock 截图或自动化 HTTP 不得冒充真实页面验收。
- 本轮未执行 dev/test/prod DDL、迁移或发布冒烟。

## A.6 历史结论保留（第二轮视角）

| 轮次 | 结果 | 说明 |
| --- | --- | --- |
| 第 1 轮独立验收 | **FAIL** | 13 项中 12 PASS / 1 FAIL；P1-001 为 `finance/project/report` JSON 截断 |
| 前序候选复验记录 | 未采信为本轮结论 | `evidence/d2/reaccept-p1-001-independent` 的 15+1 PASS 仅作为历史材料；未覆盖本项目财务隔离缺陷 |
| 第 2 轮独立复验（本轮） | **FAIL** | P1-001 PASS；P1-002 OPEN；D2 仍不可通过 |

## A.7 第二轮历史结论（保持 FAIL）

`EXPENSE-D2` 本轮独立复验结论为 **FAIL**：P1-001 已修复，但 P1-002 项目财务门店隔离仍返回数据库错误，D2 门禁不通过。

`PI-20261022-018` 整体保持 `PENDING`；不得进入 D3，不得发布，`EXPENSE-D5` 继续受 `GAP-070` 阻塞。真实 Web/App/OSS 保持 `BLOCKED`。
