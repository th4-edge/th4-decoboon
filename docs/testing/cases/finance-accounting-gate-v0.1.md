# 财务账务业务域专项用例 V0.1

- suiteId：`SUITE-FIN-ACCOUNTING-GATE-V0.1`
- oracleId：`ZDQ-FIN-20261104-V0.1`
- 用例数：26
- P0：17
- 生成命令：`python3 docs/testing/scripts/finance_gate.py validate`

| 用例 | 优先级 | 角色 | 端面 | 场景 |
| --- | --- | --- | --- | --- |
| TC-FIN-PAYROLL-ATTENDANCE-BLOCK-001 | P0 | Finance | Web, App, API | negative, boundary |
| TC-FIN-PAYROLL-LOCK-ATOMIC-002 | P0 | Finance | Web, API | happy, boundary |
| TC-FIN-PAYROLL-PENDING-NO-REBUILD-003 | P0 | Finance, Boss, StoreManager | Web, API | negative, permission, order |
| TC-FIN-PAYROLL-DRAFT-RESTORE-004 | P0 | Finance, Boss, StoreManager | Web, App, API | happy, recovery, order |
| TC-FIN-ATTENDANCE-STATE-BYPASS-005 | P0 | Finance, Boss, StoreManager | API, Web | negative, order, permission |
| TC-FIN-ATTENDANCE-CONFIRMED-EDIT-006 | P0 | Finance, StoreManager | Web, API | negative, order |
| TC-FIN-FINANCE-STORE-SCOPE-007 | P0 | Finance, StoreManager | Web, App, API | permission, negative |
| TC-FIN-ATTENDANCE-FORM-RESET-008 | P0 | Finance | Web | negative, recovery, boundary, order |
| TC-FIN-PAYROLL-ENUM-PARITY-009 | P1 | Finance, StoreManager, Employee | Web, App, API | parity, negative |
| TC-FIN-PAYROLL-REBUILD-CLEAN-010 | P1 | Finance | API | boundary, order |
| TC-FIN-SALARY-PAYABLE-ONCE-011 | P0 | Boss, StoreManager | API, Web | happy, concurrency |
| TC-FIN-SALARY-PAYMENT-IDEMPOTENT-012 | P0 | Finance, Boss, StoreManager | Web, App, API | happy, concurrency |
| TC-FIN-PAYSLIP-VISIBILITY-013 | P1 | Employee, Finance, Boss | Web, App, API | happy, permission, recovery |
| TC-FIN-PAID-HISTORY-IMMUTABLE-014 | P0 | Boss, Finance | API, Web | negative, boundary, order |
| TC-FIN-HISTORY-DATA-AUDIT-015 | P1 | Finance, Boss | API | boundary, recovery |
| TC-FIN-AR-RECEIPT-REFUND-016 | P0 | Finance, Boss, StoreManager | Web, App, API | happy, concurrency, boundary |
| TC-FIN-AP-MATERIAL-COST-017 | P0 | Finance, Boss, StoreManager, Pm | Web, App, API | happy, boundary, order |
| TC-FIN-EXPENSE-LEDGER-018 | P1 | Finance, Boss, Pm, Designer | Web, App, API | happy, permission, recovery |
| TC-FIN-COST-PROFIT-019 | P0 | Boss, Finance, StoreManager | Web, API | negative, boundary |
| TC-FIN-LEDGER-REPORT-PARITY-020 | P1 | Finance, Boss | Web, App, API | boundary, parity |
| TC-FIN-CROSS-TENANT-021 | P0 | Finance, Boss, StoreManager | API, Web, App | permission, negative |
| TC-FIN-VOUCHER-AUDIT-022 | P1 | Finance, Boss, StoreManager | Web, App, API | happy, negative, audit |
| TC-FIN-ATTENDANCE-FORM-RACE-023 | P0 | Finance | Web, App, API | negative, order, recovery, parity |
| TC-FIN-FORM-REOPEN-024 | P1 | Finance, Boss | Web, App | negative, recovery, boundary, parity |
| TC-FIN-ATTENDANCE-FORM-APP-025 | P0 | Finance, StoreManager | App | negative, recovery, boundary, parity |
| TC-FIN-COMMISSION-FORM-RESET-026 | P1 | Finance, Boss | Web, App | negative, order, recovery, parity |

## 用例明细

### TC-FIN-PAYROLL-ATTENDANCE-BLOCK-001 · 考勤未完整时禁止生成工资单

- 优先级：`P0`
- 规则：`BR-015`, `DEC-047`
- 夹具：`FIX-FIN-PAYROLL-01`
- Provider：`payroll/sheet/build`, `attendance/summary/page`
- 页面：`web:/finance/employee-salaries`, `app:/subpackages/payroll/pages/sheet-list`
- 预期页面：明确提示未确认人数；不显示虚假生成成功
- 预期状态：不创建工资单或工资项；不修改考勤状态
- 不变量：任一适用员工未确认即阻断；不得部分生成；错误可恢复
- 禁止结果：部分写入；提前锁定考勤；按0伪装完整

### TC-FIN-PAYROLL-LOCK-ATOMIC-002 · 全部考勤确认后生成工资单并锁定 InPayroll

- 优先级：`P0`
- 规则：`BR-015`, `DEC-047`
- 夹具：`FIX-FIN-PAYROLL-01`
- Provider：`payroll/sheet/build`, `payroll/item/page`, `attendance/summary/page`
- 页面：`web:/finance/employee-salaries`
- 预期页面：工资单为草稿；考勤显示已入工资
- 预期状态：每员工生成一条工资项；考勤全部 InPayroll
- 不变量：工资单/工资项/考勤同事务；无半成品
- 禁止结果：工资项存在但考勤 Confirmed；重复工资项；刷新漂移

### TC-FIN-PAYROLL-PENDING-NO-REBUILD-003 · 财务不能把 Pending 工资单重建为 Draft

- 优先级：`P0`
- 规则：`BR-038`, `DEC-043`, `DEC-093`
- 夹具：`FIX-FIN-PAYROLL-01`
- Provider：`payroll/sheet/build`, `payroll/sheet/update`, `payroll/sheet/info`
- 页面：`web:/finance/employee-salaries`, `app:/subpackages/payroll/pages/sheet-detail`
- 预期页面：财务无重建入口；退回只对 BOSS/店长可见
- 预期状态：财务重建拒绝且状态 Pending；退回后 Draft 并留原因
- 不变量：Pending 不得由制单动作重置；退回必须审计
- 禁止结果：财务绕过退回；无理由退回；Pending 被重算

### TC-FIN-PAYROLL-DRAFT-RESTORE-004 · 删除无财务事实草稿后恢复考勤

- 优先级：`P0`
- 规则：`DEC-043`, `DEC-098`
- 夹具：`FIX-FIN-PAYROLL-01`
- Provider：`payroll/sheet/delete`, `payroll/item/page`, `attendance/summary/info`, `payroll/sheet/build`
- 页面：`web:/finance/employee-salaries`, `app:/subpackages/payroll/pages/attendance-list`
- 预期页面：删除前显示影响；删除后无草稿；考勤可编辑
- 预期状态：工资项和工资单删除；考勤恢复 Confirmed；重生成只产生一套
- 不变量：删除同事务；无孤儿项/锁定考勤；有财务事实禁止删除
- 禁止结果：考勤仍锁定；旧工资项残留；重生成翻倍

### TC-FIN-ATTENDANCE-STATE-BYPASS-005 · 普通考勤接口不能直接写 InPayroll

- 优先级：`P0`
- 规则：`BR-038`, `DEC-047`
- 夹具：`FIX-FIN-PAYROLL-01`
- Provider：`attendance/summary/update`, `attendance/summary/info`, `payroll/sheet/build`
- 页面：`web:/finance/employee-salaries`, `app:/subpackages/payroll/pages/attendance-list`
- 预期页面：页面无非法动作入口
- 预期状态：非法请求拒绝；考勤保持原值
- 不变量：InPayroll 只由工资单生成产生；合法状态链封闭
- 禁止结果：接口直写 InPayroll；跳步确认；拒绝仍修改

### TC-FIN-ATTENDANCE-CONFIRMED-EDIT-006 · 已确认考勤修改后必须重新确认或阻断

- 优先级：`P0`
- 规则：`BR-038`, `BR-015`
- 夹具：`FIX-FIN-PAYROLL-01`
- Provider：`attendance/summary/update`, `attendance/summary/info`, `payroll/sheet/build`
- 页面：`web:/finance/employee-salaries`
- 预期页面：提示撤销确认或阻断
- 预期状态：修改后必须重新确认；确认痕迹更新
- 不变量：确认责任和版本同步；工资项使用新快照
- 禁止结果：保留旧确认人；继续用旧考勤；无审计变更

### TC-FIN-FINANCE-STORE-SCOPE-007 · 门店财务不能操作其他门店薪酬对象

- 优先级：`P0`
- 规则：`DEC-065`, `DEC-098`, `DEC-099`
- 夹具：`FIX-FIN-ORG-01`
- Provider：`attendance/summary/update`, `payroll/sheet/delete`, `payroll/item/page`
- 页面：`web:/finance/employee-salaries`, `app:/subpackages/payroll/pages/attendance-list`
- 预期页面：A1只见本店；跨店提示无权限
- 预期状态：跨店更新失败且数据不变；总部财务保持全企业
- 不变量：范围只从登录员工解析；页面/主接口/依赖接口同权
- 禁止结果：伪造storeId；列表隐藏但详情成功；他店Draft被删除

### TC-FIN-ATTENDANCE-FORM-RESET-008 · 新建考勤重新打开及切换员工/月份不得继承上次输入

- 优先级：`P0`
- 规则：`BR-038`
- 夹具：`FIX-FIN-PAYROLL-01`
- Provider：`attendance/summary/page`, `payroll/compensation/info`, `attendance/summary/save`
- 页面：`web:/finance/employee-salaries`, `app:/subpackages/payroll/pages/attendance-edit`
- 预期页面：重新打开后员工为空、各天数为0、备注为空；切换员工后旧天数、备注、应出勤和员工上下文全部清除或重新水合；切换月份后不得保留上月的正常、请假、旷工、外勤、年假、调休和备注
- 预期状态：未保存值不得写入任何员工或月份；新员工只加载自己的薪酬方案应出勤天数；新月份重新加载目标月份方案，不沿用旧月份数据
- 不变量：新建表单生命周期必须从空状态开始；对象或月份身份变化必须清除所有依赖字段并重新加载默认值；旧上下文值不得被提交到新上下文
- 禁止结果：重新打开后仍显示上次录入；切换员工后仍保留上一员工天数；切换月份后仍保留上月备注或天数；应出勤沿用上一员工/月份

### TC-FIN-PAYROLL-ENUM-PARITY-009 · Web/App 必须正确识别 CamelCase 薪酬枚举

- 优先级：`P1`
- 规则：`BR-038`
- 夹具：`FIX-FIN-PAYROLL-01`
- Provider：`attendance/summary/page`, `payroll/sheet/page`, `payroll/commission/page`
- 页面：`web:/finance/employee-salaries`, `app:/subpackages/payroll/pages/attendance-list`, `app:/subpackages/payroll/pages/sheet-list`
- 预期页面：InPayroll显示已入工资；其他状态正确
- 预期状态：前端保留词内大写；动作匹配状态
- 不变量：禁止全量lowerCase；未知状态不得回落草稿
- 禁止结果：InPayroll显示草稿；已锁对象有按钮；跨端不同

### TC-FIN-PAYROLL-REBUILD-CLEAN-010 · 重算必须清理退出范围的旧工资项

- 优先级：`P1`
- 规则：`DEC-045`, `BR-015`
- 夹具：`FIX-FIN-PAYROLL-01`
- Provider：`payroll/sheet/build`, `payroll/item/page`, `payroll/sheet/info`
- 页面：`web:/finance/employee-salaries`
- 预期页面：只显示当前适用员工
- 预期状态：旧项删除或作废留痕；合计不含旧项
- 不变量：当前版本不留旧项；历史可追溯；金额可解释
- 禁止结果：旧项继续计入；历史静默丢失；新旧并存

### TC-FIN-SALARY-PAYABLE-ONCE-011 · 工资单确认只生成一次应付

- 优先级：`P0`
- 规则：`BR-015`, `DEC-093`, `DEC-097`
- 夹具：`FIX-FIN-PAYROLL-01`
- Provider：`payroll/sheet/update`, `payroll/item/page`, `finance/payment/employee/page`
- 页面：`web:/finance/employee-salaries`, `app:/subpackages/payroll/pages/sheet-detail`
- 预期页面：工资单已确认；重复确认无重复成功
- 预期状态：每员工一条应付；应付等于工资项汇总
- 不变量：确认/应付/成本同事务；幂等
- 禁止结果：重复应付；重复成本；金额不等

### TC-FIN-SALARY-PAYMENT-IDEMPOTENT-012 · 员工工资付款只核销一次并生成唯一流水

- 优先级：`P0`
- 规则：`BR-015`, `DEC-093`
- 夹具：`FIX-FIN-MONEY-01`
- Provider：`finance/payment/employee/save`, `finance/payment/employee/page`, `finance/ledger/page`
- 页面：`web:/finance/employee-salaries`, `app:/subpackages/payroll/pages/payment-create`
- 预期页面：显示成功和余额；重复提交幂等
- 预期状态：应付已付准确；一条付款一套流水
- 不变量：不超付；同幂等键一次；台账同步
- 禁止结果：重复流水；超额付款；台账不一致

### TC-FIN-PAYSLIP-VISIBILITY-013 · 工资条只能在实际发放后对本人可见

- 优先级：`P1`
- 规则：`BR-015`, `DEC-093`
- 夹具：`FIX-FIN-PAYROLL-01`
- Provider：`payroll/payslip/my/page`, `payroll/payslip/info`, `finance/payment/employee/save`
- 页面：`web:/account/payslips`, `app:/subpackages/payroll/pages/my-payroll`
- 预期页面：发放前不可见；发放后只本人可见
- 预期状态：工资条等于已发工资项汇总；发布一次
- 不变量：支付成功后才发布；他人不可见；失败不可见
- 禁止结果：未发先可见；看他人工资条；重复发布

### TC-FIN-PAID-HISTORY-IMMUTABLE-014 · 已发工资只能下期调整不能反改

- 优先级：`P0`
- 规则：`BR-015`, `DEC-043`, `DEC-047`
- 夹具：`FIX-FIN-PAYROLL-01`
- Provider：`payroll/sheet/update`, `attendance/summary/update`, `payroll/commission/adjustment/save`, `finance/ledger/page`
- 页面：`web:/finance/employee-salaries`, `app:/subpackages/payroll/pages/adjustment-list`
- 预期页面：历史页只读；提示下期调整
- 预期状态：历史应付/付款/台账/工资条不变；调整可追溯
- 不变量：Paid禁止反改；差异下期调整
- 禁止结果：直接改已发工资；删除历史；重复冲减

### TC-FIN-HISTORY-DATA-AUDIT-015 · 历史工资项与考勤差异必须备份后受控修复

- 优先级：`P1`
- 规则：`BR-015`, `DEC-043`
- 夹具：`FIX-FIN-PAYROLL-01`
- Provider：`attendance/summary/page`, `payroll/item/page`, `payroll/sheet/info`
- 页面：`web:/finance/employee-salaries`
- 预期页面：报告列出月份和差异数
- 预期状态：只改目标状态；回填后差异0
- 不变量：先备份/演练；禁止改已发金额
- 禁止结果：无备份回填；把有工资项等同已发；回填后仍有差异

### TC-FIN-AR-RECEIPT-REFUND-016 · 应收、收款、退款和冲正金额守恒

- 优先级：`P0`
- 规则：`DEC-077`, `BR-050`, `DEC-099`
- 夹具：`FIX-FIN-MONEY-01`
- Provider：`finance/receivable/page`, `finance/receipt/save`, `finance/refund/save`, `finance/receipt/reverse`, `finance/ledger/page`
- 页面：`web:/finance/receivables`, `web:/finance/receipts`, `web:/finance/refunds`, `app:/subpackages/finance/pages/receivables`, `app:/subpackages/finance/pages/receipts`, `app:/subpackages/finance/pages/refunds`
- 预期页面：流水状态正确；余额/已收/退款/冲正摘要正确
- 预期状态：余额公式成立；冲正退款各一次；台账同源
- 不变量：退款不超有效收款；冲正不重复；流水不无痕删除
- 禁止结果：余额为负；冲正仍计有效；重复退款

### TC-FIN-AP-MATERIAL-COST-017 · 材料下单、应付、付款和成本分层勾稽

- 优先级：`P0`
- 规则：`BR-058`, `DEC-092`, `BR-055`
- 夹具：`FIX-FIN-COST-01`
- Provider：`material/sheet/order`, `finance/payable/page`, `finance/payment/save`, `finance/project/report`
- 页面：`web:/finance/payables`, `web:/finance/projects`, `app:/subpackages/finance/pages/material-settlement`
- 预期页面：四层状态分别显示；成本不完整利润为空
- 预期状态：分层记录；成本不重复；退货有调整
- 不变量：付款等于成本；按0算利润；重复累计成本
- 禁止结果：material/sheet/order；finance/payable/page；finance/payment/save；finance/project/report

### TC-FIN-EXPENSE-LEDGER-018 · 费用审批、付款、台账和归属闭环

- 优先级：`P1`
- 规则：`DEC-089`, `BR-055`
- 夹具：`FIX-FIN-MONEY-01`
- Provider：`expense/expenses/save`, `expense/expenses/update`, `finance/payment/save`, `finance/ledger/page`
- 页面：`web:/finance/expenses`, `app:/subpackages/finance/pages/expenses`
- 预期页面：审批到待付到已付正确；归属正确
- 预期状态：付款只一次；金额与台账一致；不重复归集
- 不变量：未批先付；跨范围审批；重复付款
- 禁止结果：expense/expenses/save；expense/expenses/update；finance/payment/save；finance/ledger/page

### TC-FIN-COST-PROFIT-019 · 成本不完整时禁止结算和伪利润

- 优先级：`P0`
- 规则：`BR-058`, `DEC-092`, `MET-007`
- 夹具：`FIX-FIN-COST-01`
- Provider：`finance/project/report`, `finance/settlement/update`, `finance/summary/report`
- 页面：`web:/finance/projects`, `web:/finance/projects/:projectId`, `app:/subpackages/finance/pages/project`
- 预期页面：显示成本未完整；利润为空；结算阻断
- 预期状态：costComplete=false时利润/毛利率null；状态不变
- 不变量：禁止0成本；禁止跳过缺失结算
- 禁止结果：假利润；缺成本结算；口径不一致

### TC-FIN-LEDGER-REPORT-PARITY-020 · 列表、详情、汇总和台账必须同源

- 优先级：`P1`
- 规则：`MET-006`, `MET-007`, `BR-055`
- 夹具：`FIX-FIN-MONEY-01`
- Provider：`finance/summary/report`, `finance/ledger/report`, `finance/receivable/report`, `finance/payable/report`
- 页面：`web:/finance/workbench`, `web:/finance/ledger`, `app:/subpackages/finance/pages/ledger`
- 预期页面：同筛选同金额；分页可访问全量
- 预期状态：summary.count=totalElements；金额公式一致
- 不变量：不得当前页冒充全量；不得0掩盖缺失
- 禁止结果：工作台与台账不同；分页丢失；详情状态不同

### TC-FIN-CROSS-TENANT-021 · 财务对象不得跨企业或跨门店泄露

- 优先级：`P0`
- 规则：`DEC-065`, `DEC-099`
- 夹具：`FIX-FIN-ORG-01`
- Provider：`finance/receivable/page`, `finance/payment/page`, `payroll/sheet/info`, `payroll/item/page`
- 页面：`web:/finance/workbench`, `app:/subpackages/finance/pages/projects`
- 预期页面：无权限或不存在；不返回金额明细
- 预期状态：目标无副作用；范围从登录员工解析
- 不变量：禁止信任前端范围字段；查询写入同权
- 禁止结果：猜ID成功；列表隐藏详情成功；伪造归属

### TC-FIN-VOUCHER-AUDIT-022 · 高风险财务动作必须有凭证和审计闭环

- 优先级：`P1`
- 规则：`BR-006`, `OBJ-004`, `DEC-099`
- 夹具：`FIX-FIN-MONEY-01`
- Provider：`finance/receipt/info`, `finance/payment/page`, `finance/ledger/page`
- 页面：`web:/finance/vouchers`, `app:/subpackages/finance/pages/vouchers`
- 预期页面：凭证对象金额时间正确；原因和前后状态可见
- 预期状态：凭证只引用真实流水；高风险动作留痕
- 不变量：凭据金额不符；无原因作废；删除不可追溯
- 禁止结果：finance/receipt/info；finance/payment/page；finance/ledger/page

### TC-FIN-ATTENDANCE-FORM-RACE-023 · 切换员工或月份后旧异步响应不得覆盖新表单

- 优先级：`P0`
- 规则：`BR-038`
- 夹具：`FIX-FIN-PAYROLL-01`
- Provider：`attendance/summary/page`, `payroll/compensation/info`, `attendance/summary/save`
- 页面：`web:/finance/employee-salaries`, `app:/subpackages/payroll/pages/attendance-edit`
- 预期页面：页面最终显示员工B和B的应出勤；旧响应不得把员工、天数、备注或应出勤恢复成A的数据
- 预期状态：请求版本或取消机制阻止旧响应写回；提交只使用当前选中的员工和月份
- 不变量：异步响应必须绑定请求上下文版本；上下文变化后旧响应只能被丢弃；页面显示和最终提交对象必须一致
- 禁止结果：旧响应覆盖新员工；旧月份方案覆盖新月份；页面显示B但提交A的empId；快速切换后出现混合字段

### TC-FIN-FORM-REOPEN-024 · 财务新建表单关闭重开和对象切换不得继承上次输入

- 优先级：`P1`
- 规则：`BR-055`, `DEC-089`
- 夹具：`FIX-FIN-ORG-01`
- Provider：`expense/expenses/save`, `finance/receipt/save`, `finance/payment/save`, `payroll/compensation/save`
- 页面：`web:/finance/expenses`, `web:/finance/receipts`, `web:/finance/payments`, `app:/subpackages/finance/pages/expense-edit`, `app:/subpackages/payroll/pages/compensation-edit`
- 预期页面：重新打开为空白新建状态；切换对象后旧金额、备注、归属和附件状态被清除；编辑既有对象时只水合该对象真实数据
- 预期状态：未保存数据不进入新对象；切换对象不保留旧对象ID；编辑状态和数据来源可区分
- 不变量：新建、编辑、取消、保存成功、保存失败必须有明确表单生命周期；对象切换必须清理依赖字段；旧请求不得覆盖新模式
- 禁止结果：重开后显示上一笔金额；切换员工后保留旧金额；切换项目后保留旧附件；新建提交引用旧对象ID

### TC-FIN-ATTENDANCE-FORM-APP-025 · App新建考勤重开与切换员工/月份不得继承上次输入

- 优先级：`P0`
- 规则：`BR-038`
- 夹具：`FIX-FIN-PAYROLL-01`
- Provider：`attendance/summary/page`, `payroll/compensation/info`, `attendance/summary/save`
- 页面：`app:/subpackages/payroll/pages/attendance-edit`, `app:/subpackages/payroll/pages/attendance-list`
- 预期页面：重开后页面为空状态；切换员工后旧天数、备注、应出勤全部重置；切换月份后旧月份数据不保留
- 预期状态：新对象不继承旧上下文；最终提交empId和month与页面当前选择一致
- 不变量：App表单生命周期与Web同源；页面、最终提交对象与服务端接收对象一致
- 禁止结果：重开保留上次输入；切员工保留旧天数；切月份保留旧备注；提交旧empId或month

### TC-FIN-COMMISSION-FORM-RESET-026 · 员工提成切换员工后不得继承上一员工金额和备注

- 优先级：`P1`
- 规则：`BR-055`, `DEC-089`
- 夹具：`FIX-FIN-PAYROLL-01`
- Provider：`payroll/commission/page`, `payroll/commission/info`, `payroll/commission/save`
- 页面：`web:/finance/employee-commissions`, `app:/subpackages/payroll/pages/commission-edit`
- 预期页面：切换员工后员工名更新，签约额、提成金额和备注清空，比例恢复默认值；页面显示对象、最终提交员工和接口接收empId一致；重新进入新建页为空状态
- 预期状态：新员工不继承上一员工金额、比例、提成金额和备注；最终提交empId仅为当前员工；不产生错误提成或下游工资事实
- 不变量：页面显示员工、最终提交员工和Provider接收员工一致；App与Web提成表单生命周期同源；切换对象后的金额字段不得来自旧上下文
- 禁止结果：切换员工后保留旧签约额；切换员工后保留旧比例或提成金额；切换员工后保留旧备注；以新empId提交上一员工输入

