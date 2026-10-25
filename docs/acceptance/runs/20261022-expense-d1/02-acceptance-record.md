# EXPENSE-D1 独立验收记录

| 项 | 内容 |
| --- | --- |
| 问题 | `PI-20261022-018` |
| 业务裁决 | `DEC-091` / `BR-057` / `ANS-BIZ-20261022-012` |
| 研发交接 | `T12-BIZ-20261022-012` |
| 验收日期 | 2026-10-23 |
| 验收责任方 | 独立验收责任方（非研发实现方） |
| 源码基线 | `dev` / `7cf83fa2b526156640ea8c227eb348ab81ca27d1`（工作区含并行未提交改动） |
| 结论 | **PASS（第二轮复验）** |
| 缺陷计数 | **当前 P0=0，P1=0，P2=0**；首轮历史缺陷见第 6 节 |
| 阻断原因 | 无未关闭 P0/P1/P2；真实 UI/OSS 环境仍为 BLOCKED |
| 真实 UI | **BLOCKED**：未取得独立真实后端浏览器/App 真机证据；Web 独立构建命令被中止，不以研发 Mock 或研发证据替代 |

> **当前最终结论（2026-10-23，EntityGraph 修复后）**：`PASS`。HTTP/DDL 独立验收 `P0=0 / P1=0 / P2=0`；原 14 项 HTTP 全部 PASS，6 个原缺陷全部 PASS。真实 Web/App、微信开发者工具/真机、真实 OSS 仍为 `BLOCKED`，不得伪造。详见文末“最终复验收敛”。

## 1. 独立性声明

- 本验收未修改产品代码、DDL、迁移、权限或业务语义，未提交 Git，未发布。
- 租户主数据通过隔离测试仓储准备；费用分类、草稿、提交、退回、重提、审批、拒绝、附件授权等业务动作全部经真实随机端口 HTTP `/api` Provider 发起，不以直接调用 Provider 作为唯一证据。
- 测试仅使用 JVM 内存 H2 和 `/tmp` 隔离 MySQL 8.4；未连接生产、开发或正式测试库，未使用生产账号或真实员工数据。
- 研发提供的测试不是本次唯一证据；独立测试源码、Surefire 原始报告和运行日志已保留。
- 验收发现缺陷后只记录，不实施修复。

## 2. 环境与测试数据

### 2.1 后端 HTTP 环境

- 方式：`@SpringBootTest(webEnvironment=RANDOM_PORT)` + Undertow，随机本机端口。
- 数据库：H2 `MODE=MySQL` 内存库，`ddl-auto=create-drop`，`open-in-view=false`。
- 网络：`TestRestTemplate` 真实 TCP HTTP 请求，登录后以 `token` 调用 Provider。
- 隔离副本：`/tmp/expense-d1-minrepo.go5BSB`，用于避开工作区并行构建清理 `target/classes`。
- 租户 A：总部、城北店、城南店；BOSS、总部财务、城北店长、城北财务、城北普通员工 A/B、城南店长、城南财务。
- 租户 B：他企负责人体系下普通员工，用于跨企业验证。
- 项目：企业 A 城北店项目 1 个；企业 B 项目 1 个。

### 2.2 数据库 DDL 环境

- MySQL `8.4.0`，独立临时数据目录和 socket，监听 `127.0.0.1:33307`。
- 先建立最小外键引用骨架，再原样执行 `docs/design/migrations/126-企业费用与员工报销D1.sql`。
- 再原样执行 `docs/design/migrations/rollback/126-企业费用与员工报销D1-rollback.sql`。

### 2.3 App/Web 环境

- App：当前工作区依赖、`npm run type-check`、`npm run build:mp-weixin`、导航/财务 IA/上传契约检查。
- Web：未取得本次最终独立构建结果；真实浏览器 + 真实后端未执行。
- 微信开发者工具/真机：不可用，未执行。
- 研发提供的 Mock 截图不作为真实 UI 验收证据。

## 3. 执行命令与结果

### 3.1 后端编译与独立 HTTP 测试

```bash
cd /tmp/expense-d1-minrepo.go5BSB
mvn -Dmaven.test.skip=true compile
javac -parameters \
  -cp "$REPO/target/classes:$(cat /tmp/expense-d1-classpath.txt)" \
  -d "$REPO/target/test-classes" \
  "$REPO/src/test/java/com/th4/edge/decoboon/finance/acceptance/ExpenseD1IndependentAcceptanceTest.java"
mvn -Dtest=com.th4.edge.decoboon.finance.acceptance.ExpenseD1IndependentAcceptanceTest surefire:test
```

结果：

- 主代码隔离编译：PASS。
- 独立 HTTP 测试：`14` 项，`8 PASS / 6 FAIL / 0 ERROR / 0 SKIP`。
- 原始证据：
  - [Surefire 文本报告](evidence/independent/surefire-report.txt)
  - [Surefire XML 报告](evidence/independent/surefire-report.xml)
  - [完整运行日志](evidence/independent/surefire-run.log)
  - [独立测试源码](evidence/independent/ExpenseD1IndependentAcceptanceTest.java.txt)

工作区直接运行研发测试命令时，被并行基线 `CompanyPlanTemplateContextTest` 构造器参数不一致导致 testCompile 失败；该失败与 EXPENSE-D1 无关，因此使用隔离源码副本完成主代码编译和独立测试。

### 3.2 MySQL DDL 与回滚

执行结果：

- 迁移 `126`：PASS。
- 四表创建：`db_finance_expense`、`db_finance_expense_category`、`db_finance_expense_attachment`、`db_finance_expense_revision`。
- 关键字段核对通过：`amount DECIMAL(14,2)`、`description LONGTEXT NOT NULL`、`snapshot_json LONGTEXT NOT NULL`、`idempotency_key VARCHAR(128) NOT NULL`、`self_approved BIT(1) NOT NULL`。
- 关键索引均以 `com_id` 最左；企业费用单号、幂等键、分类编码、版本唯一约束存在。
- 回滚脚本：PASS；四张 D1 表删除，最小引用骨架保留。
- 未把迁移应用到 dev/test/prod。

### 3.3 App

| 检查 | 结果 |
| --- | --- |
| `npm run type-check` | PASS |
| `npm run build:mp-weixin` | PASS |
| `npm run check:navigation` | PASS |
| `npm run check:finance-ia` | PASS |
| `npm run check:upload` | PASS |
| `npm run check:client-copy` | FAIL：`src/subpackages/quote/pages/detail.vue:609` 研发型文案，非本次费用文件 |
| 微信开发者工具/真机真实角色走查 | BLOCKED |

### 3.4 Web

- 本次独立组合构建命令被中止，未取得最终独立结论。
- 未连接真实后端执行浏览器登录、列表、表单、审批与越权走查。
- 研发证据中的 `type-check/build` 通过仅作为研发自测，不转写为独立 PASS。

## 4. ACC-EXP-01～08 逐项结果

| 用例 | 结果 | 已验证 | 未通过/未覆盖 |
| --- | --- | --- | --- |
| ACC-EXP-01 分类初始化与停用校验 | **FAIL** | 六类基础分类首次初始化；重复读取不新增重复分类 | 企业财务可修改 `builtIn=true` 分类名称；分类停用后，已存在草稿仍可提交成功 |
| ACC-EXP-02 员工报销闭环 | **PASS** | 员工报销草稿、附件、提交 v1、店长审批、审批人/归属员工/附件可回读、`selfApproved=false`；D1 未新增 Payable/Payment/Ledger/VoucherRecord | 真实 App/Web 页面未验收 |
| ACC-EXP-03 退回重提版本 | **FAIL** | v1/v2 版本均保留，v1 快照附件可回读 | 重提后 `approvalNote` 被清空；版本快照不含退回人、退回时间、退回原因等审计事件，历史退回理由不可回读 |
| ACC-EXP-04 店长本人报销防自批 | **PASS** | 店长本人单据动作投影无审批；HTTP 自批被拒；BOSS 审批成功且 `selfApproved=false` | 无 |
| ACC-EXP-05 财务复核权限 | **PASS** | 门店财务可退回他人费用；财务审批被拒；财务不得退回本人费用 | 付款不属于 D1，未调用付款接口 |
| ACC-EXP-06 项目/企业归属与 `storeId=0` | **PASS** | 项目费用取项目门店；企业费用清空项目；提交后改金额被拒；普通员工/店长/门店财务使用 `storeId=0` 被拒；BOSS/总部财务可提交未归属门店；跨企业门店被拒 | D4 项目成本/手工零星成本防双计不在 D1 |
| ACC-EXP-07 附件、草稿与幂等 | **FAIL** | 无附件草稿可保存；提交缺证据被拒；补附件后提交成功；重复提交/重复审批不新增版本；`0.004` 金额处理存在缺陷；同一幂等键不同项目/说明被静默当作旧单 | 幂等键未覆盖项目、门店、归属员工、说明和附件；金额入库后变为 `0.00` 仍返回成功草稿 |
| ACC-EXP-08 数据范围与跨端 | **BLOCKED** | 后端 HTTP：本人、同店店长/财务、BOSS、总部财务可见；同店普通员工/异店店长/异店财务/跨企业不可见；跨企业项目不可用 | App/Web 真实同状态、同金额、同动作、同下一步未执行；跨企业附件另见 P0 |

## 5. 额外安全验收

### SEC-ATT-01 跨企业附件引用

结果：**FAIL（P0）**。

复现：

1. 企业 B 员工通过 `file/private/grant` 上传 Expense 证据文件，得到对象路径 `companies/<企业B>/files/expense/...`。
2. 企业 A 普通员工创建费用单，把 `attachments[].fileUrl` 直接设为该企业 B 路径。
3. 费用单保存、提交均成功。
4. 企业 A 员工调用 `file/private/grant`，`businessId` 为自己的费用单，`paths` 为企业 B 路径。
5. 服务端返回成功，并签发企业 B 文件的私有下载 URL。

原始失败信息：

```text
foreign path must never be signed into another tenant's expense
Expecting blank but was:
/file/private/download?path=companies%2F<企业B>%2Ffiles%2Fexpense%2F...&expires=...&signature=...
```

根因边界：

- 费用保存只校验附件 URL 非空，没有验证文件上传归属、企业、项目、业务范围或上传人。
- Expense 文件授权只检查费用单本身是否可见，然后签名该费用单附件列表中客户端提供的任意 URL。
- 这违反 `DEC-091`“跨企业附件不可引用”和附件下载服务端授权要求。

最小修复建议：服务端签发/保存附件时应绑定上传凭证或对象键到 `companyId + scope + businessId`；费用保存不得接受任意字符串 URL；文件授权必须再次校验对象键所属企业和 Expense 业务范围。不得只依据费用单可见性签名路径。

## 6. 缺陷清单

| ID | 级别 | 缺陷 | 可复现结果 | 最小修复建议 |
| --- | --- | --- | --- | --- |
| EXP-D1-ACC-001 | **P0** | 跨企业附件路径可引用并被签名 | 企业 A 可把企业 B 文件路径挂到自己的费用单，并取得签名下载 URL | 服务端校验对象键企业/业务归属；客户端 URL 不得直接成为可签名白名单 |
| EXP-D1-ACC-002 | P1 | 平台基础分类可被企业改名 | `expense/categories/update` 成功把 `TRAVEL` 改名为“被篡改的差旅费”，`builtIn=true` 未阻止 | `builtIn=true` 禁止改名称/编码/说明等基线字段，仅允许明确允许的启停/排序 |
| EXP-D1-ACC-003 | P1 | 停用分类的旧草稿仍可提交 | 草稿保存后停用分类，提交返回成功并生成 v1 | 提交时重新校验分类 `active=true`，停用则拒绝并保留草稿 |
| EXP-D1-ACC-004 | P1 | 幂等键未覆盖完整业务意图 | 同一幂等键、不同项目或说明，第二次保存静默返回第一张单 | 幂等指纹至少覆盖项目、门店、归属员工、收款员工、分类、金额、日期、说明、附件；不一致返回 409 |
| EXP-D1-ACC-005 | P1 | 退回原因在重提后丢失，审批留痕不足 | 退回原因“补充行程单”在 v2 重提后读取为空；版本快照无退回事件 | 增加独立审批/退回事件或把状态、原因、操作人、时间写入不可变版本；重提不得覆盖历史原因 |
| EXP-D1-ACC-006 | P2 | 金额归一化后可变为 0 仍保存成功 | `0.004` 返回 `amount=0.0` 的 Draft | 保存/更新时先按 2 位小数归一化，再校验 `>0`；补 `@Digits(integer=12, fraction=2)` |

## 7. 通过项与风险

### 已通过

- 四表 DDL、实体映射、关键索引和回滚。
- 分类基础设施初始化幂等。
- 员工报销正常审批闭环。
- 店长自批、财务审批、财务自退等主要权限反向用例。
- 项目/企业归属、项目门店优先、`storeId=0` 企业范围规则。
- 缺证据提交、重复提交、重复审批不增加版本。
- 费用记录级 Self/Store/Company/跨企业数据范围。
- D1 未产生应付、付款、流水、凭证副作用。
- App 类型检查、小程序构建、导航、财务 IA、上传契约。

### 剩余风险

- P0 跨租户证据文件泄露风险使 D1 不能通过，也不能进入 D2。
- 分类、停用、幂等、退回审计和金额边界均涉及后续应付/成本来源；修复前进入 D2 会放大对账和成本错误。
- `EXPENSE-D5` 仍受 `GAP-070` 阻塞。
- 真实 Web 浏览器、App 真机/微信开发者工具、真实 OSS 签名和真实 MySQL dev/test 环境仍未覆盖。
- 工作区存在并行未提交改动及无关 testCompile 阻塞；本结论只对应本次独立测试时的源码快照。

## 8. 最终结论

**FAIL**。D1 的核心正常路径和主要权限门禁已通过，但存在 1 个 P0、4 个 P1、1 个 P2 缺陷；其中跨企业附件签名直接违反业务边界，必须阻断 D1 验收与后续 D2。真实 App/Web 跨端验收同时为 **BLOCKED**，不得以研发 Mock 截图或研发自测替代。

> 上述“不得启动 EXPENSE-D2”是首轮历史结论；第二轮修复后已解除，见文末最终复验收敛。

---

# 第二轮独立复验（2026-10-23）

> 本节是当前有效结论。首轮 FAIL 记录保留用于追踪修复前后差异。

## 9.1 复验范围与环境

- 同一独立验收责任方，未修改产品代码、DDL、权限或业务语义。
- 源码：当前 `dev` 工作区快照 `7cf83fa2b526156640ea8c227eb348ab81ca27d1` 上的未提交修复。
- 隔离源码副本：`/tmp/expense-d1-round2-repo.XDvfuc`。
- 真实 HTTP：随机端口 Undertow + H2 `MODE=MySQL`，`open-in-view=false`；主数据由隔离仓储准备，业务动作全部经 `/api` HTTP。
- DDL：临时 MySQL 8.4，端口 `33318`，仅执行迁移 126 与回滚。
- 真实 Web/App 浏览器、微信开发者工具/真机、真实 OSS：仍不可用，**BLOCKED**。
- 用户要求立即收敛后，研发侧 8 项补充回归未纳入本轮独立结论；独立 HTTP 已覆盖相应修复面。

## 9.2 最终 HTTP 结果

`16` 个独立 HTTP 测试方法：**15 PASS / 1 FAIL / 0 ERROR / 0 SKIP**。

- 原 14 项 HTTP：**13 PASS / 1 FAIL**。
- 新增 2 项独立覆盖：完整幂等指纹、跨项目附件与异常持久化路径不签名，均 PASS。
- 证据：
  - [第二轮 Surefire 文本报告](evidence/round2/surefire-report.txt)
  - [第二轮 Surefire XML](evidence/round2/surefire-report.xml)
  - [完整 HTTP 运行日志](evidence/round2/full-http-run.log)
  - [定向复测日志](evidence/round2/targeted-retest.log)
  - [第二轮独立测试源码](evidence/round2/ExpenseD1IndependentAcceptanceTest.java.txt)

## 9.3 原 14 项 HTTP 逐项状态

| 原测试/验收项 | 第二轮状态 | 说明 |
| --- | --- | --- |
| `accExp00` 四表与关键列 | PASS | H2 元数据含四表及新增 `returned_by_id/returned_at/return_reason` |
| `accExp01_defaults` 六类分类幂等初始化 | PASS | 两次读取均为 6 类，无重复 |
| `accExp01_builtin` 基础分类保护 | PASS | 名称/说明修改均拒绝；启停和排序允许 |
| `accExp01_disabled` 停用分类提交 | PASS | 草稿保存后停用分类，提交被拒 |
| `accExp02` 员工报销审批闭环 | PASS | 审批成功；D1 无应付/付款/流水/凭证副作用 |
| `accExp03` 退回重提与审计回读 | **FAIL** | v2 快照已有退回原因/人/时间，但详情 Provider 返回畸形 JSON，整个详情不可解析 |
| `accExp04` 店长本人报销防自批 | PASS | 自批拒绝，BOSS 审批成功 |
| `accExp05` 财务复核权限 | PASS | 财务可退回他人；不得审批；不得退回本人 |
| `accExp06` 项目/企业/storeId=0 | PASS | 项目证据路径、项目门店、企业/项目边界和门店范围均通过 |
| `accExp07_draft` 缺证据/重复提交 | PASS | 缺证据拒绝，补证据成功，重复提交不增版本 |
| `accExp07_idempotency` 原项目/说明幂等差异 | PASS | 不一致返回 409 |
| `accExp07_amount` 金额舍入 | PASS | `0.004` 归一化后为 0，保存被拒 |
| `accExp08` 数据范围 | PASS | Self/Store/Company/跨店/跨企业费用记录边界通过 |
| `accExpAttachmentForeign` 跨企业附件 | PASS | 保存被拒；异常路径无法取得签名 URL |

## 9.4 原 6 个缺陷最终状态

| 原缺陷 | 第二轮状态 | 证据结论 |
| --- | --- | --- |
| 1. 跨企业附件可引用/签名 | **PASS（修复）** | 跨企业路径保存/提交被拒；异常持久化路径请求签名返回 404 |
| 1a. 跨项目附件 | **PASS（修复）** | 项目 A 费用引用项目 B 路径被拒，返回 400 |
| 2. `builtIn=true` 分类可改名 | **PASS（修复）** | 名称、说明修改均被拒；`active`、`sort` 可更新 |
| 3. 停用分类旧草稿可提交 | **PASS（修复）** | 提交时重新校验 `active=true`，停用后拒绝 |
| 4. 幂等指纹覆盖不足 | **PASS（修复）** | requester/project/store/owner/payee/category/amount/date/description/attachments 任一变化均返回 409 |
| 5. 退回人/时间/原因保留与 v2 回读 | **FAIL（部分修复）** | 数据库/快照已保存 `previousReturn`，但 `expense/expenses/info` JSON 在 `"returnedBy"}` 处损坏 |
| 6. 舍入后 <=0 金额 | **PASS（修复）** | `0.004` 拒绝，不再产生 `0.00` 草稿 |

## 9.5 当前缺陷

### EXP-D1-ACC-R2-001（P1）· 退回详情 JSON 畸形

复现：

1. 员工提交费用 v1。
2. 店长退回并填写“补充行程单”。
3. 员工补附件后重提，生成 v2。
4. 调用 `expense/expenses/info` 读取详情。

实际响应末尾：

```json
..."submittedAt":"2026-10-23 01:20:48","returnedBy"}]}
```

`returnedBy` 只有字段名，没有冒号和值，导致整个 JSON 不可解析。`snapshotJson` 中已经能看到：

```json
"previousReturn": {
  "reason": "补充行程单",
  "returnedBy": "<店长 empId>",
  "returnedAt": "2026-10-23 01:20:48"
}
```

因此数据落库和快照写入已完成，但外部 HTTP 详情不可回读，不满足“v2 快照可回读”。

证据一致性：`ExpenseRevisionRepository` 的版本查询只预取 `submittedBy`，而 `Info` 的 `@Properties` 对外暴露 `returnedBy`；`open-in-view=false` 下序列化懒关联导致畸形输出。

最小修复建议：在版本查询中预取 `returnedBy`，或在事务内显式组装审计字段，禁止对外序列化未初始化代理。修复后需只复验 `ACC-EXP-03` 与详情 JSON。

## 9.6 迁移 126 与回滚复验

结果：**PASS**。

- `db_finance_expense_revision` 新增：
  - `returned_by_id BIGINT NULL`
  - `returned_at DATETIME(6) NULL`
  - `return_reason LONGTEXT NULL`
- 外键 `fk_fin_expense_revision_returner` 指向 `db_tenant_employee(emp_id)`。
- 迁移 126 原样执行成功。
- 回滚脚本执行成功，四张 D1 表删除，引用骨架保留。
- 证据：
  - [DDL schema](evidence/round2/mysql-ddl-schema.out)
  - [回滚后表清单](evidence/round2/mysql-after-rollback.out)

## 9.7 当前结论与 D2 门禁

- 当前缺陷计数：**P0=0，P1=1，P2=0**。
- 原 6 个缺陷：5 个修复通过，1 个仍有外部回读失败。
- 真实 Web/App/OSS：**BLOCKED**，未伪造 UI 或签名证据。
- **不得进入 `EXPENSE-D2`**。必须先修复 `EXP-D1-ACC-R2-001` 并完成最小独立 HTTP 复验；其余已通过项无需重复扩展场景。

---

# 最终复验收敛（EntityGraph 修复后）

## 10.1 修复核对

`ExpenseRevisionRepository` 已将版本查询预取改为：

```java
@EntityGraph(attributePaths = {"submittedBy", "returnedBy"})
List<ExpenseRevision> findByCompany_ComIdAndExpense_ExpenseIdOrderByVersionDesc(...)
```

本轮只同步该实现到隔离副本，未修改工作区产品代码。

## 10.2 复验结果

### 完整回归

- 完整 16 项 HTTP 回归：`15 PASS / 1 FAIL`。
- 唯一失败不是产品 JSON 损坏，而是独立测试仍把 `snapshotJson.previousReturn.returnedBy` 按数字断言；实际该值按 JSON 字符串保存。
- 详情响应已恢复为合法 JSON，`returnedBy` 不再产生 `"returnedBy"}` 畸形字段。
- 原始 EntityGraph 复跑日志：[entitygraph-full-rerun.log](evidence/round2/entitygraph-full-rerun.log)

### 唯一失败项定向复测

修正独立测试断言后，仅复跑 `ACC-EXP-03`：

- `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
- `returnedBy` 正确返回店长 `empId`
- `returnedAt` 非空
- `returnReason = 补充行程单`
- v2 `snapshotJson.previousReturn` 可解析并含退回人、时间和原因
- 证据：
  - [ACC-EXP-03 复跑日志](evidence/round2/acc03-final.log)
  - [ACC-EXP-03 Surefire 文本报告](evidence/round2/acc03-surefire-report.txt)
  - [ACC-EXP-03 Surefire XML](evidence/round2/acc03-surefire-report.xml)
  - [最终独立测试源码](evidence/round2/ExpenseD1IndependentAcceptanceTest.final.java.txt)

组合最终结果：此前 15 项全部 PASS，唯一未完成项经断言修正后 PASS，因此：

- 原 14 项 HTTP：**14/14 PASS**
- 新增 2 项关键补强：**2/2 PASS**
- 原 6 个缺陷：**6/6 PASS**

## 10.3 原 14 项最终状态

| 验收项 | 最终状态 |
| --- | --- |
| 四表、关键列、新增退回字段 | PASS |
| 六类基础分类幂等初始化 | PASS |
| 基础分类仅允许启停/排序 | PASS |
| 停用分类提交拒绝 | PASS |
| 员工报销审批闭环，D1 无财务副作用 | PASS |
| 退回重提 v2、退回人/时间/原因与快照回读 | PASS |
| 店长本人报销防自批 | PASS |
| 财务复核权限与防自退 | PASS |
| 项目/企业归属及 `storeId=0` | PASS |
| 缺证据、补证据、重复提交 | PASS |
| 项目/说明幂等差异 | PASS |
| 舍入后 <=0 金额拒绝 | PASS |
| Self/Store/Company/跨店/跨企业数据范围 | PASS |
| 跨企业附件引用拒绝 | PASS |

## 10.4 6 个缺陷最终状态

| 原缺陷 | 最终状态 |
| --- | --- |
| 跨企业附件可引用/签名 | PASS（修复） |
| 跨项目附件引用、异常持久化路径不签名 | PASS（修复） |
| `builtIn=true` 分类名称/说明不可改，仅允许启停/排序 | PASS（修复） |
| 停用分类旧草稿提交拒绝 | PASS（修复） |
| 完整幂等指纹不一致返回 409 | PASS（修复） |
| 退回人/时间/原因保留且 v2 HTTP 可回读 | PASS（修复） |
| `0.004` 舍入后 <=0 拒绝 | PASS（修复） |

## 10.5 DDL 与回滚

- 迁移 126：PASS。
- `returned_by_id / returned_at / return_reason`：PASS。
- 退回人外键：PASS。
- 回滚：PASS。
- 未应用到 dev/test/prod。

## 10.6 最终风险与 D2 门禁

- 当前代码缺陷计数：**P0=0，P1=0，P2=0**。
- 后端 HTTP 与隔离 MySQL 范围内，EXPENSE-D1 独立验收 **PASS**。
- **可以进入 `EXPENSE-D2` 研发切片**。
- 真实 Web/App、微信开发者工具/真机、真实 OSS：**BLOCKED**。因此 D1 可以进入后续研发，但在正式发布或声称完整跨端闭环前，仍需补充真实 UI 与 OSS 证据。
- 本轮未修改产品代码、DDL、权限或业务语义，未提交 Git。
