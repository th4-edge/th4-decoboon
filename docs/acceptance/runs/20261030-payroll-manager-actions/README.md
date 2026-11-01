# 2026-10-30 店长本店薪酬动作权限

## 结论

- Domain：`payroll-labor`
- Batch：`PAY-MANAGER-ACTIONS-B8A`
- Decision：`DEC-097`
- Phase：`DEV_VERIFIED`
- 状态：`PENDING_REAL_STORE_MANAGER_UI`
- 未发布、未独立验收

## 已落地

- StoreManager 本店确认、退回、Draft 删除、员工工资/提成发放的动作角色已放开。
- 混合门店工资单由 `requireStoreItemsInStore` 拒绝，避免店长整单影响他店。
- Finance 仍不可最终确认、退回、删除；员工发放仍允许。
- BOSS 保持企业范围动作。
- App/Web 能力键和入口已对齐；生成工资单、薪酬方案、考勤、提成计提仍不放开给店长。
- B8B 作废仍受反向应付/成本、版本/唯一约束和 DDL 门禁。

## 验证

- 后端定向 16/16 PASS。
- App 契约/类型/UI/导航/文案/小程序构建/分包 PASS。
- Web 类型/生产构建/权限/门店/工作台 PASS。
- 全量测试在共享 target 并发清理后未可靠完成，见 `backend-verification.txt`。

## 待补

1. 受控 StoreManager 账号确认本店 Draft/Pending 工资单。
2. 受控 StoreManager 退回本店 Pending，原因回读。
3. 受控 StoreManager 删除本店 Draft，跨店和混合门店拒绝。
4. 受控 StoreManager 发放本店员工应付，验证核销、台账、工资条和幂等。
5. Finance 最终确认/退回/删除仍 403；BOSS 原动作不退化。
