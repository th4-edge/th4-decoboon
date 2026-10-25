# EXPENSE-D1 · 费用对象与审批研发任务

| 项 | 内容 |
| --- | --- |
| 关联问题 | `PI-20261022-018` |
| 业务裁决 | `DEC-091` / `BR-057` / `ANS-BIZ-20261022-012` / `T12-BIZ-20261022-012` |
| 蓝图 | `03a-expense-reimbursement-blueprint-v0.1.json` |
| 当前门禁 | `BLUEPRINT_READY / PLAN_READY` |
| 编码状态 | `NOT_AUTHORIZED` |
| 变更级别 | L3：DDL、权限、金额/成本语义 |
| 日期 | 2026-10-22 |

## 1. 本切片目标

完成费用/报销的独立业务事实和审批生命周期，使员工可在 App/Web 提交企业直付费用或员工报销，BOSS/店长可按范围审批，财务可按范围复核并退回补充。

本切片只形成 `Expense` 业务事实，不生成应付、不付款、不出凭证。审批通过后的成本确认是业务事实；D2 才接应付和付款。

## 2. 成功场景

```text
员工/经办人创建 Expense
-> 上传证据、填写分类/金额/日期/归属
-> 提交并冻结版本
-> BOSS/店长审批
   -> 退回：补充证据或修改 -> 新版本重提
   -> 拒绝：本次终态
   -> 通过：形成 Approved 费用事实
```

必须覆盖：

- 项目零星费用：项目归属，只进入对应项目成本；
- 员工垫付报销：收款员工=费用归属员工；
- 企业直付费用：项目为空，不得进入项目成本；
- 店长提交本人报销：不能自批，转 BOSS；
- 财务复核附件：可退回补充，不能替代业务审批；
- BOSS 一人企业：提交、审批可分动作留痕。

## 3. 明确不做

- 不创建 `Payable`、`Payment`、`Ledger` 或 `VoucherRecord`；
- 不修改 `ProjectCostContext`、`FinanceReportContext`、结算或利润公式；
- 不做预算、额度、多级审批、支出前申请、发票查验/认证/抵扣；
- 不迁移历史手工流水或费用应付；
- 不改 App/Web 其他业务模块。

## 4. 对象与实体规范

### 4.1 `Expense`

- 继承 `AuditModel`，类上使用 `@Model + @Note + @Entity + @Table`；
- `@Id + @SnowflakeId`，`@EntityListeners(SnowflakeEntityListener.class)`；
- 必含 `Company company`；
- `store/project/requester/owner/payeeEmployee/category/attachments/approvedBy` 使用对象关联，禁止裸 ID；
- 枚举使用 `Enumerable`；
- 长说明、审批意见和快照字段必须与迁移列长度一致，明确 `longtext`；
- 不写 `@JsonIgnore`，前后端字段按 Provider `@Properties` 裁剪。

### 4.2 字段契约

| 字段 | 类型/关联 | 必填 | 规则 |
| --- | --- | --- | --- |
| expenseId | Long | 是 | Snowflake 主键 |
| company | Company | 是 | 登录员工企业，不信任前端 |
| store | Store | 企业费用可为空 | 项目费用取项目门店；企业费用默认当前门店；仅 BOSS/总部财务可提交未归属门店（storeId=0） |
| project | Project | 否 | 非空=项目费用；空=企业费用 |
| requester | Employee | 是 | 实际提交人；代提交另存归属员工 |
| owner | Employee | 是 | 数据负责人；默认 requester |
| payeeEmployee | Employee | 报销必填 | 必须等于费用归属员工 |
| expenseNo | String | 是 | 企业内唯一、稳定 |
| category | ExpenseCategory | 是 | 同企业、启用 |
| paymentMode | `ExpensePaymentMode` | 是 | `Direct / Reimbursement` |
| amount | BigDecimal(14,2) | 是 | > 0，人民币 |
| expenseDate | LocalDate | 是 | 费用发生日 |
| description | String/text | 是 | 业务说明 |
| status | `ExpenseStatus` | 是 | 服务端状态机 |
| version | Integer | 是 | 提交版本 |
| submittedAt | LocalDateTime | 否 | 首次提交时间 |
| approvedBy/approvedAt/approvalNote | Employee/LocalDateTime/text | 否 | Approved 必填 |
| voidedBy/voidedAt/voidReason | Employee/LocalDateTime/text | 否 | Voided 必填 |
| sourceType/sourceId | String/Long | 否 | 继承来源；新费用可空 |
| idempotencyKey | String | 是 | 企业内唯一，防重复提交 |

### 4.3 `ExpenseCategory`

- 企业级一级分类，独立于 `LedgerCategory`；
- 字段：`categoryId/company/code/name/description/builtIn/status/sort`；
- 同企业 `code` 唯一；停用不删除历史引用；
- 平台基础分类通过初始化/复制提供，企业不能修改平台基线。

### 4.4 `ExpenseAttachment`

- 字段：`attachmentId/company/expense/revision/documentType/fileUrl/fileName/sha256/size/uploadedBy/uploadedAt`；
- 业务 Provider 只保存已上传文件引用，不新增本地直写上传通道；
- 同一修订至少一份附件；跨企业附件不可引用；
- 发票号码等可作为描述/元数据，但不建发票实体。

### 4.5 `ExpenseRevision`

- 每次首次提交和重新提交写入不可变快照；
- 字段：`revisionId/company/expense/version/snapshotJson/submittedBy/submittedAt`；
- 同企业同费用同版本唯一；
- 历史版本只读，不允许覆盖。

## 5. 状态机

```text
Draft -> Submitted
Submitted -> Cancelled (requester withdraw)
Submitted -> Returned (BOSS/StoreManager)
Returned -> Submitted (new revision)
Submitted -> Approved (BOSS/StoreManager)
Submitted -> Rejected (BOSS/StoreManager)
```

D1 不实现 `Voided/Closed` 的付款副作用；枚举可预留，但不得伪造状态。

## 6. 权限与范围

| 动作 | BOSS | 店长 | 财务 | 普通员工 |
| --- | --- | --- | --- | --- |
| 查看 | Company | Store | 总部财务 Company；门店财务 Store | Self |
| 创建/编辑本人 | 允许 | 允许 | 允许 | 允许 |
| 代提交 | Company | Store | 禁止 | 禁止 |
| 提交/撤回 | 本人/代提交 | 本人/本店 | 本人 | 本人 |
| 审批 | Company | Store（非本人报销） | 禁止 | 禁止 |
| 退回/拒绝 | Company | Store（非本人报销） | 禁止 | 禁止 |
| 财务复核退回 | 允许 | 允许 | 本范围 | 禁止 |

- 角色从登录员工加载，禁止信任前端 `companyId/empId/storeId`。
- 数据范围使用 `DataPermission` 和服务端范围追加；详情/更新/删除使用 `DataScopeContext + DataScopeSpecification`。
- 财务不能审批本人费用；店长不能审批本人报销；普通员工不能审批。
- BOSS 一人企业自审必须写 `selfApproved=true` 和独立审计，不得把提交与审批合并。

## 7. 接口与 Provider 边界

按 AGENTS.md 固定能力名，D1 只允许以下 Provider 家族：

- `expense/expenses/page`：分页；
- `expense/expenses/info`：详情；
- `expense/expenses/save`：草稿创建；
- `expense/expenses/update`：草稿修改、提交、撤回、审批、退回、拒绝、重提；
- `expense/expenses/delete`：仅草稿且无提交版本；
- `expense/categories/page|save|update|delete`；
- 附件通过既有文件能力上传，业务侧只保存引用。

状态转换必须在 `update` 服务端根据当前状态、目标状态、角色和归属校验，不接受前端直接写 `done=true`。所有响应返回具体 `Expense` / `Page<Expense>`，不建 DTO/VO。

## 8. DDL 提案（未执行）

> 以下仅为研发评审方案，执行前必须用户确认、备份、迁移编号检查、回滚演练和全量测试。

### 8.1 表

- `db_finance_expense`
- `db_finance_expense_category`
- `db_finance_expense_attachment`
- `db_finance_expense_revision`

### 8.2 关键约束

- 每张表必须含 `com_id`；
- `db_finance_expense` 企业内 `expense_no` 唯一，企业内 `idempotency_key` 唯一；
- `db_finance_expense_revision` 企业内 `(expense_id, version)` 唯一；
- `db_finance_expense_category` 企业内 `code` 唯一；
- 费用表联合索引以 `com_id` 最左：
  - `(com_id, expense_id)`
  - `(com_id, store_id, status, created_at)`
  - `(com_id, requester_id, created_at)`
  - `(com_id, project_id, expense_date)`
  - `(com_id, payee_emp_id, created_at)`
- 金额使用 `decimal(14,2)`；
- 说明、审批意见、快照使用与 Java 字段一致的 `longtext`；
- 不建物理外键到 `com.th4.edge.platform`。

### 8.3 回滚

- 先停用 Provider 和前端入口；
- 保留已提交费用、修订和附件证据，不删除历史事实；
- 新建表可回滚删除前先导出/备份；
- 不在回滚中批量改写存量流水或应付。

## 9. App/Web 页面

| 页面 | App | Web |
| --- | --- | --- |
| 费用列表 | 本人/范围内列表，状态筛选 | 同对象字段和分页 |
| 创建/编辑 | 分类、金额、日期、项目/企业、支付方式、证据 | 相同字段与校验 |
| 详情 | 版本、证据、审批、退回原因、下一步 | 相同事实和动作 |
| 审批列表 | BOSS/店长范围内待办 | 同范围、同排序、同权限 |
| 财务复核 | 查看证据并退回补充 | 同对象和异常恢复 |

App 可相机优先；Web 必须提供文件上传等价闭环。两端不得只读后要求回 App。

## 10. 验收用例（D1）

- 员工：保存草稿、上传证据、提交；退回后修改重提；撤回；查看版本历史。
- 项目费用：选择项目；企业费用：不能带项目；项目/企业切换在提交后不可变。
- 员工报销：收款员工非本人或被停用员工 → 拒绝。
- 店长本人报销：店长审批自己 → 403；BOSS 审批 → 成功。
- 财务：审批他人费用 → 403；复核退回 → 成功；审批自己 → 403。
- 越权：跨企业、跨店、跨项目、员工查看他人 → 403/404。
- 幂等：重复提交、重复审批、重复退回 → 不新增第二版本/事件。
- 状态：非法跳转、已提交改金额、已审批再改单 → 拒绝。
- 跨端：App/Web 同字段、同状态、同金额、同错误文案、同下一步。
- 副作用：D1 不产生应付、付款、台账和凭证。

## 11. 当前阻塞与下一步

- `DDL_CONFIRMATION`：四张表、唯一约束、联合索引、长文本列需用户确认。
- `PERMISSION_CONFIRMATION`：财务不业务审批、财务不自审自付、店长不自批本人报销、BOSS 一人企业分动作留痕。
- `COST_CONFIRMATION`：审批通过是唯一成本确认事件；D1 不接应付/付款，D2 才接。
- 上述确认前：不建表、不写 Provider、不改权限或页面。
- 确认后：按本任务进入 D1 实现；实现完成交独立验收，不把研发自测写成验收通过。
