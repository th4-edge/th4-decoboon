# 冻结用例角色冲突

- 用例：`TC-FIN-FINANCE-RECEIPT-002`
- 用例声明角色：`Finance`
- 业务规则：`DEC-075/DEC-076` 规定财务不自动获得冲正高危权限；`DEC-065` 只将店长本店高危动作放开。
- App 权限矩阵：`act:receiptReverse` 仅 Boss、StoreManager 持有。
- 实测：Finance 调用 `finance/receipt/reverse` 返回 403；StoreManager 页面可冲正并完成回读。

结论：代码应按现有业务权限保留 Finance 拒绝；冻结用例需由测试资产责任方拆分“Finance 登记/超余额/幂等”和“Boss/店长冲正”两个角色步骤后，才能整条 PASS。
