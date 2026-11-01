# B8A · 店长本店薪酬动作权限扩展

## 状态

- 批次：`PAY-MANAGER-ACTIONS-B8A`
- 业务裁决：`DEC-097` / `ANS-BIZ-20261030-002` / `EV-429`
- 当前状态：`DEV_VERIFIED / PENDING_REAL_STORE_MANAGER_UI`
- DDL/迁移：无
- 作废：B8B 仍受反向应付/成本、版本/唯一约束和迁移门禁

## 用户裁决

店长在本店范围内获得工资单最终确认、退回草稿、Draft 删除和员工工资/提成发放权限。店长仍仅限本店；混合门店工资单不得整单操作；作废授权已取得但需单独技术门禁。

## 范围

- 后端：BOSS/StoreManager 确认、退回、Draft 删除；BOSS/Finance/StoreManager 员工发放。
- 数据范围：StoreManager 仅本店；混合门店工资单拒绝；跨店应付拒绝。
- App/Web：移除店长的只读按钮限制，显示其本店可执行动作。
- 不变：财务复核/支付能力；BOSS 企业范围；工资公式、成本、应付、工资条、幂等和审计。
- 不做：StoreManager 薪酬方案、考勤、提成计提、工资单生成；B8B 作废实现和 DDL。

## 实现结果

- 后端动作角色：确认/退回/删除使用 `requireConfirmActor` / `requireReturnActor` / `requireDeleteActor`；发放使用 `requirePaymentActor`。
- StoreManager 只能操作本店工资项全部覆盖的整单；混合门店由 `requireStoreItemsInStore` 拒绝。
- 删除统一走 `visibleSheets(current)`，不再按企业裸查 ID。
- App 增加店长 `confirm:payroll/pay:payroll` 能力；Draft 删除改为能力键门控。
- Web `payrollAccess` 允许店长确认/退回/删除/发放，但生成/复核仍不放开。
