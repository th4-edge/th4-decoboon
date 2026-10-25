# H1 · 员工薪酬 P0 事故收口研发任务

## 状态

- 批次：`H1`
- 变更级别：L3（权限、金额确认链路）
- 编码授权：已由用户于 2026-10-23 确认
- 当前状态：`RECONCILED`（黑盒 PASS，独立签署待完成）
- DDL/迁移：无
- 生产发布：不在本任务自动执行

## 目标

在不改变业务规则和工资公式的前提下收口三个已裁决问题：

1. 工资单确认不再受提成完整度阻断；
2. Web 薪酬页面不再复用通用财务写权限；
3. 店长不能跨店读取考勤详情。

## H1-01 提成解耦回归

### 事实

- `EV-149` 记录 `e04a523` 已修复并生产发布；
- 当前 `dev/prod` 的 `PayrollSettlementContext.confirm` 仍包含提成阻断；
- 修复提交当前只存在于 `hotfix/payroll-confirm-boss`。

### 要求

- 先核对当前生产运行制品与 `prod` 源码，确认是否真实回归；
- 在 `dev` 重新落地批准逻辑，删除 `commissionComplete != true` 的确认阻断；
- 保留 `commissionComplete` 作为管理侧提示；
- 恢复或重写回归测试：提成未结清时 BOSS 可确认，财务仍不可确认；
- 不修改提成费用、应付或工资项计算。

### 验证

- 提成 `Accrued/Released/InPayment` 时 BOSS 可确认；
- 考勤未完整、工资项为空、实发为负仍阻断；
- Finance 确认仍被拒绝；
- 重复确认幂等。

## H1-02 Web 薪酬按钮权限

### 要求

- 薪酬方案页面只使用 `usePayrollAccess` 或同等薪酬能力判定；
- 员工工资页：薪酬方案入口用 `edit:payroll`；月度考勤用 `edit:attendance`；生成工资单用 `edit:payroll`；确认工资单用 `confirm:payroll`；员工发放本批不修改，等待 B3B；
- 员工提成页：计提提成用 `edit:payroll`；提成发放本批不修改；
- 薪酬工作台入口按页面能力裁剪，不因通用财务 `canWrite` 展示薪酬写动作；
- 后端仍是最终权限边界。

### 验证

- BOSS 按能力可见；
- Finance 可制单、可发放，不可最终确认；
- StoreManager 不可生成工资单、不可最终确认、不可计提提成；本店考勤入口保留；
- 不允许把 `edit:finance` 当作薪酬能力。

## H1-03 考勤详情门店范围

- `attendance/summary/info` 使用 `DataScopeSpecification` / `PayrollAccessContext.visible`；
- 店长和门店财务仅能读取本店记录；
- 他店已知 ID 返回 404；
- 补同店通过、跨店拒绝、跨企业拒绝测试。

## 明确不做

- 不补 Web 考勤编辑/删除；
- 不补工资单草稿删除、复核、作废；
- 不修改员工发放权限；
- 不修改工资公式、成本、应付、台账；
- 不做 DDL、迁移、生产数据修复；
- 不正式发布小程序。

## 交回材料

changed_files、定向与全量测试、Web type-check/build、权限矩阵证据、生产发布记录与源码差异结论、risks、acceptance_needed、learning_request。
