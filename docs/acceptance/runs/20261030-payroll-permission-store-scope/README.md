# 2026-10-30 薪酬入口权限与门店范围对齐

## 范围

- 变更记录：`CHG-20261030-004`
- App：员工提成列表的计提入口按 `edit:payroll` 门控
- Web：月度考勤抽屉、提成计提抽屉继承当前门店范围
- 非范围：后端角色矩阵、敏感字段裁剪、员工自助、作废和批量发放

## 实现结论

1. App 店长只有 `view:payroll`，不再看到“计提”入口；直接深链仍由页面守卫拦截。
2. Web 月度考勤分页请求携带 `storeId`，员工选择器绑定当前门店。
3. Web 提成计提抽屉的员工选择器绑定当前门店。
4. 后端继续以 `DataScope` / `StoreFilterContext` 为权限和范围兜底，不改角色矩阵。

## 验证

- 后端范围定向：`PayrollAttendanceInfoScopeTest`、`PayrollCompensationScopeTest`、`PayrollEmployeePaymentAccessTest` 6/6 PASS
- App 薪酬契约：PASS
- App type-check：PASS
- App `check:client-copy`：PASS
- App `check:ui`：PASS
- App `build:mp-weixin`：PASS
- Web type-check：PASS
- Web production build：PASS

## 未覆盖

- 真实店长账号在 App 确认无计提入口
- 真实财务/店长账号在 Web 单店范围检查考勤和提成员工候选
- 独立验收与发布

## 下一步

使用受控测试企业验证 App/Web 同角色、同门店的可执行动作和候选数据一致，再进入敏感字段和员工自助能力。
